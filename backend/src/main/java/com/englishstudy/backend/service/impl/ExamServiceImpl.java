package com.englishstudy.backend.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.englishstudy.backend.common.BusinessException;
import com.englishstudy.backend.constant.RoleConstants;
import com.englishstudy.backend.entity.Exam;
import com.englishstudy.backend.entity.ExamPaperQuestion;
import com.englishstudy.backend.entity.ExamQuestion;
import com.englishstudy.backend.entity.ExamRecord;
import com.englishstudy.backend.entity.ExamSession;
import com.englishstudy.backend.mapper.ExamMapper;
import com.englishstudy.backend.mapper.ExamPaperQuestionMapper;
import com.englishstudy.backend.mapper.ExamQuestionMapper;
import com.englishstudy.backend.mapper.ExamRecordMapper;
import com.englishstudy.backend.mapper.ExamSessionMapper;
import com.englishstudy.backend.request.ExamRequests;
import com.englishstudy.backend.service.BaseService;
import com.englishstudy.backend.service.ExamService;
import com.englishstudy.backend.service.LogService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ExamServiceImpl extends BaseService implements ExamService {

    private static final String DEFAULT_EXAM_TYPE = "大学四级";
    private static final String DEFAULT_QUESTION_TYPE = "SINGLE_CHOICE";
    private static final String LISTENING_QUESTION_TYPE = "LISTENING_CHOICE";

    private final ExamMapper examMapper;
    private final ExamQuestionMapper examQuestionMapper;
    private final ExamPaperQuestionMapper examPaperQuestionMapper;
    private final ExamRecordMapper examRecordMapper;
    private final ExamSessionMapper examSessionMapper;
    private final LogService logService;
    private final ObjectMapper objectMapper;

    @Value("${app.upload-dir}")
    private String uploadDir;

    public ExamServiceImpl(ExamMapper examMapper,
                           ExamQuestionMapper examQuestionMapper,
                           ExamPaperQuestionMapper examPaperQuestionMapper,
                           ExamRecordMapper examRecordMapper,
                           ExamSessionMapper examSessionMapper,
                           LogService logService,
                           ObjectMapper objectMapper) {
        this.examMapper = examMapper;
        this.examQuestionMapper = examQuestionMapper;
        this.examPaperQuestionMapper = examPaperQuestionMapper;
        this.examRecordMapper = examRecordMapper;
        this.examSessionMapper = examSessionMapper;
        this.logService = logService;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<Map<String, Object>> listPublishedExams() {
        requireRole(RoleConstants.STUDENT);
        String className = requireCurrentClassName();
        return examMapper.selectList(Wrappers.<Exam>lambdaQuery()
                        .eq(Exam::getPublished, true)
                        .eq(Exam::getClassName, className)
                        .orderByDesc(Exam::getCreatedAt))
                .stream()
                .map(this::toSimpleView)
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> studentExamDetail(Long examId) {
        requireRole(RoleConstants.STUDENT);
        Exam exam = requireExam(examId);
        if (!Boolean.TRUE.equals(exam.getPublished())) {
            throw new BusinessException("该考试暂未发布");
        }
        validateStudentExamClass(exam);
        ensureExamNotCompleted(examId);
        return toDetailView(exam, false);
    }

    @Override
    public Map<String, Object> getSession(Long examId) {
        requireRole(RoleConstants.STUDENT);
        validateStudentExamClass(requireExam(examId));
        ensureExamNotCompleted(examId);
        ExamSession session = findSession(examId, currentUser().getUserId());
        if (session == null) {
            Map<String, Object> empty = new LinkedHashMap<>();
            empty.put("examId", examId);
            empty.put("answers", new ArrayList<>());
            empty.put("remainingSeconds", null);
            empty.put("status", "NEW");
            return empty;
        }
        return toSessionView(session);
    }

    @Override
    public Map<String, Object> saveSession(Long examId, ExamRequests.ExamSessionRequest request) {
        requireRole(RoleConstants.STUDENT);
        Exam exam = requireExam(examId);
        if (!Boolean.TRUE.equals(exam.getPublished())) {
            throw new BusinessException("该考试暂未发布");
        }
        validateStudentExamClass(exam);
        ensureExamNotCompleted(examId);
        ExamSession session = findSession(examId, currentUser().getUserId());
        if (session == null) {
            session = new ExamSession();
            session.setExamId(examId);
            session.setStudentId(currentUser().getUserId());
            session.setStatus("IN_PROGRESS");
            session.setRemainingSeconds(defaultRemainingSeconds(exam, request));
            session.setAnswersJson(writeJson(request.getAnswers() == null ? new ArrayList<>() : request.getAnswers()));
            markForInsert(session);
            examSessionMapper.insert(session);
            return toSessionView(session);
        }
        session.setAnswersJson(writeJson(request.getAnswers() == null ? new ArrayList<>() : request.getAnswers()));
        session.setRemainingSeconds(defaultRemainingSeconds(exam, request));
        session.setStatus("IN_PROGRESS");
        markForUpdate(session);
        examSessionMapper.updateById(session);
        return toSessionView(session);
    }

    @Override
    @Transactional
    public Map<String, Object> submitExam(Long examId, ExamRequests.ExamSubmitRequest request) {
        requireRole(RoleConstants.STUDENT);
        Exam exam = requireExam(examId);
        if (!Boolean.TRUE.equals(exam.getPublished())) {
            throw new BusinessException("该考试暂未发布");
        }
        validateStudentExamClass(exam);
        ensureExamNotCompleted(examId);
        List<PaperQuestion> paperQuestions = listPaperQuestions(examId);
        Map<Long, ExamRequests.AnswerItem> answerMap = new LinkedHashMap<>();
        if (request.getAnswers() != null) {
            for (ExamRequests.AnswerItem item : request.getAnswers()) {
                answerMap.put(item.getQuestionId(), item);
            }
        }

        int score = 0;
        List<Map<String, Object>> answerResult = new ArrayList<>();
        for (PaperQuestion paperQuestion : paperQuestions) {
            ExamQuestion question = paperQuestion.getQuestion();
            ExamRequests.AnswerItem answerItem = answerMap.get(question.getId());
            String answer = answerItem == null ? "" : answerItem.getAnswer();
            int questionScore = paperQuestion.getScore();
            boolean correct = isAnswerCorrect(question.getQuestionType(), question.getCorrectAnswer(), answer);
            if (correct) {
                score += questionScore;
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("questionId", question.getId());
            row.put("questionType", question.getQuestionType());
            row.put("answer", answer);
            row.put("correctAnswer", question.getCorrectAnswer());
            row.put("correct", correct);
            row.put("score", correct ? questionScore : 0);
            answerResult.add(row);
        }

        ExamRecord examRecord = new ExamRecord();
        examRecord.setExamId(exam.getId());
        examRecord.setExamTitle(exam.getTitle());
        examRecord.setStudentId(currentUser().getUserId());
        examRecord.setStudentName(currentUser().getRealName());
        examRecord.setClassName(requireCurrentClassName());
        examRecord.setScore(score);
        examRecord.setTotalScore(exam.getTotalScore());
        examRecord.setAnswersJson(writeJson(answerResult));
        examRecord.setSubmittedAt(LocalDateTime.now());
        examRecordMapper.insert(examRecord);

        ExamSession session = findSession(examId, currentUser().getUserId());
        if (session != null) {
            session.setStatus("SUBMITTED");
            session.setRemainingSeconds(0);
            markForUpdate(session);
            examSessionMapper.updateById(session);
        }
        logService.save("考试中心", "提交考试", exam.getTitle());
        return toRecordView(examRecord);
    }

    @Override
    public List<Map<String, Object>> listStudentRecords() {
        requireRole(RoleConstants.STUDENT);
        return examRecordMapper.selectList(Wrappers.<ExamRecord>lambdaQuery()
                        .eq(ExamRecord::getStudentId, currentUser().getUserId())
                        .orderByDesc(ExamRecord::getSubmittedAt))
                .stream()
                .map(this::toRecordView)
                .collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> listTeacherExams() {
        requireRole(RoleConstants.TEACHER);
        String className = requireCurrentClassName();
        return examMapper.selectList(Wrappers.<Exam>lambdaQuery()
                        .eq(Exam::getTeacherId, currentUser().getUserId())
                        .eq(Exam::getClassName, className)
                        .orderByDesc(Exam::getCreatedAt))
                .stream()
                .map(exam -> toDetailView(exam, true))
                .collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> listQuestionBank() {
        requireRole(RoleConstants.TEACHER);
        return examQuestionMapper.selectList(Wrappers.<ExamQuestion>lambdaQuery()
                        .orderByAsc(ExamQuestion::getExamType)
                        .orderByAsc(ExamQuestion::getQuestionType)
                        .orderByDesc(ExamQuestion::getId))
                .stream()
                .map(question -> toQuestionView(question, true, question.getSortOrder(), defaultScore(question.getScore())))
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> createQuestion(ExamRequests.QuestionItem request) {
        requireRole(RoleConstants.TEACHER);
        ExamQuestion question = new ExamQuestion();
        fillQuestion(question, request);
        examQuestionMapper.insert(question);
        logService.save("题库管理", "新增题目", question.getContent());
        return toQuestionView(question, true, question.getSortOrder(), defaultScore(question.getScore()));
    }

    @Override
    public Map<String, Object> updateQuestion(Long questionId, ExamRequests.QuestionItem request) {
        requireRole(RoleConstants.TEACHER);
        ExamQuestion question = requireQuestion(questionId);
        fillQuestion(question, request);
        examQuestionMapper.updateById(question);
        logService.save("题库管理", "修改题目", question.getContent());
        return toQuestionView(question, true, question.getSortOrder(), defaultScore(question.getScore()));
    }

    @Override
    @Transactional
    public void deleteQuestion(Long questionId) {
        requireRole(RoleConstants.TEACHER);
        ExamQuestion question = requireQuestion(questionId);
        Long usedCount = examPaperQuestionMapper.selectCount(Wrappers.<ExamPaperQuestion>lambdaQuery()
                .eq(ExamPaperQuestion::getQuestionId, questionId));
        if (usedCount != null && usedCount > 0) {
            question.setEnabled(false);
            examQuestionMapper.updateById(question);
        } else {
            examQuestionMapper.deleteById(questionId);
        }
        logService.save("题库管理", "删除题目", question.getContent());
    }

    @Override
    public Map<String, Object> uploadQuestionAudio(MultipartFile file) {
        requireRole(RoleConstants.TEACHER);
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择要上传的听力音频");
        }
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            throw new BusinessException("音频文件名不能为空");
        }
        String extension = getFileExtension(originalFilename).toLowerCase();
        if (!".mp3".equals(extension) && !".wav".equals(extension) && !".m4a".equals(extension) && !".ogg".equals(extension)) {
            throw new BusinessException("听力音频只支持 mp3、wav、m4a、ogg");
        }
        try {
            Path uploadRoot = Paths.get(uploadDir);
            Files.createDirectories(uploadRoot);
            String storedName = System.currentTimeMillis() + "_exam_audio_" + UUID.randomUUID().toString().replace("-", "") + extension;
            Path target = uploadRoot.resolve(storedName);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("audioFileName", originalFilename);
            result.put("audioStoredName", storedName);
            result.put("audioContentType", normalizeAudioContentType(file.getContentType(), originalFilename));
            result.put("audioFileSize", file.getSize());
            logService.save("题库管理", "上传听力音频", originalFilename);
            return result;
        } catch (IOException exception) {
            throw new BusinessException("听力音频上传失败");
        }
    }

    @Override
    public Resource loadQuestionAudio(Long questionId) {
        ExamQuestion question = requireQuestionAudioViewable(questionId);
        Path filePath = resolveQuestionAudioPath(question);
        FileSystemResource resource = new FileSystemResource(filePath.toFile());
        if (!resource.exists()) {
            throw new BusinessException("听力音频文件不存在");
        }
        return resource;
    }

    @Override
    public MediaType loadQuestionAudioMediaType(Long questionId) {
        ExamQuestion question = requireQuestionAudioViewable(questionId);
        return parseMediaType(question.getAudioContentType());
    }

    @Override
    public long loadQuestionAudioSize(Long questionId) {
        ExamQuestion question = requireQuestionAudioViewable(questionId);
        try {
            return Files.size(resolveQuestionAudioPath(question));
        } catch (IOException exception) {
            throw new BusinessException("读取听力音频失败");
        }
    }

    @Override
    @Transactional
    public Map<String, Object> createExam(ExamRequests.ExamUpsertRequest request) {
        requireRole(RoleConstants.TEACHER);
        String examType = normalizeExamType(request.getType());
        int questionCount = normalizeQuestionCount(request.getQuestionCount());
        List<ExamQuestion> selectedQuestions = selectRandomQuestions(examType, questionCount);

        Exam exam = new Exam();
        fillExam(exam, request, selectedQuestions);
        exam.setTeacherId(currentUser().getUserId());
        exam.setTeacherName(currentUser().getRealName());
        exam.setClassName(requireCurrentClassName());
        markForInsert(exam);
        examMapper.insert(exam);
        savePaperQuestions(exam.getId(), selectedQuestions);
        logService.save("考试管理", "新增考试", exam.getTitle());
        return toDetailView(requireExam(exam.getId()), true);
    }

    @Override
    @Transactional
    public Map<String, Object> updateExam(Long examId, ExamRequests.ExamUpsertRequest request) {
        requireRole(RoleConstants.TEACHER);
        Exam exam = requireExam(examId);
        if (!currentUser().getUserId().equals(exam.getTeacherId())) {
            throw new BusinessException("只能修改自己创建的考试");
        }
        if (!sameClassName(exam.getClassName(), requireCurrentClassName())) {
            throw new BusinessException("只能修改本班考试");
        }
        String examType = normalizeExamType(request.getType());
        int questionCount = normalizeQuestionCount(request.getQuestionCount());
        List<ExamQuestion> selectedQuestions = selectRandomQuestions(examType, questionCount);

        fillExam(exam, request, selectedQuestions);
        exam.setClassName(requireCurrentClassName());
        markForUpdate(exam);
        examMapper.updateById(exam);
        examPaperQuestionMapper.delete(Wrappers.<ExamPaperQuestion>lambdaQuery().eq(ExamPaperQuestion::getExamId, examId));
        savePaperQuestions(examId, selectedQuestions);
        logService.save("考试管理", "修改考试", exam.getTitle());
        return toDetailView(requireExam(examId), true);
    }

    @Override
    @Transactional
    public void deleteExam(Long examId) {
        requireRole(RoleConstants.TEACHER);
        Exam exam = requireExam(examId);
        if (!currentUser().getUserId().equals(exam.getTeacherId())) {
            throw new BusinessException("只能删除自己创建的考试");
        }
        if (!sameClassName(exam.getClassName(), requireCurrentClassName())) {
            throw new BusinessException("只能删除本班考试");
        }
        examPaperQuestionMapper.delete(Wrappers.<ExamPaperQuestion>lambdaQuery().eq(ExamPaperQuestion::getExamId, examId));
        examSessionMapper.delete(Wrappers.<ExamSession>lambdaQuery().eq(ExamSession::getExamId, examId));
        examMapper.deleteById(examId);
        logService.save("考试管理", "删除考试", exam.getTitle());
    }

    @Override
    public List<Map<String, Object>> listTeacherRecords() {
        requireRole(RoleConstants.TEACHER);
        String className = requireCurrentClassName();
        List<Long> examIds = examMapper.selectList(Wrappers.<Exam>lambdaQuery()
                        .eq(Exam::getTeacherId, currentUser().getUserId())
                        .eq(Exam::getClassName, className))
                .stream()
                .map(Exam::getId)
                .collect(Collectors.toList());
        if (examIds.isEmpty()) {
            return new ArrayList<>();
        }
        return examRecordMapper.selectList(Wrappers.<ExamRecord>lambdaQuery()
                        .in(ExamRecord::getExamId, examIds)
                        .orderByDesc(ExamRecord::getSubmittedAt))
                .stream()
                .filter(record -> sameClassName(record.getClassName(), className))
                .map(this::toRecordView)
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> giveFeedback(Long recordId, ExamRequests.ExamFeedbackRequest request) {
        requireRole(RoleConstants.TEACHER);
        String className = requireCurrentClassName();
        ExamRecord record = examRecordMapper.selectById(recordId);
        if (record == null) {
            throw new BusinessException("成绩记录不存在");
        }
        Exam exam = requireExam(record.getExamId());
        if (!currentUser().getUserId().equals(exam.getTeacherId())) {
            throw new BusinessException("只能反馈自己考试的成绩");
        }
        if (!sameClassName(exam.getClassName(), className)) {
            throw new BusinessException("只能反馈本班考试的成绩");
        }
        if (!sameClassName(record.getClassName(), className)) {
            throw new BusinessException("只能反馈本班学生的成绩");
        }
        record.setTeacherFeedback(request.getTeacherFeedback());
        examRecordMapper.updateById(record);
        logService.save("教学评估", "成绩反馈", record.getExamTitle());
        return toRecordView(record);
    }

    private Exam requireExam(Long examId) {
        Exam exam = examMapper.selectById(examId);
        if (exam == null) {
            throw new BusinessException("考试不存在");
        }
        return exam;
    }

    private ExamQuestion requireQuestion(Long questionId) {
        ExamQuestion question = examQuestionMapper.selectById(questionId);
        if (question == null) {
            throw new BusinessException("题目不存在");
        }
        return question;
    }

    private ExamSession findSession(Long examId, Long studentId) {
        return examSessionMapper.selectOne(Wrappers.<ExamSession>lambdaQuery()
                .eq(ExamSession::getExamId, examId)
                .eq(ExamSession::getStudentId, studentId)
                .last("limit 1"));
    }

    private ExamRecord findStudentRecord(Long examId, Long studentId) {
        return examRecordMapper.selectOne(Wrappers.<ExamRecord>lambdaQuery()
                .eq(ExamRecord::getExamId, examId)
                .eq(ExamRecord::getStudentId, studentId)
                .orderByDesc(ExamRecord::getSubmittedAt)
                .last("limit 1"));
    }

    private void ensureExamNotCompleted(Long examId) {
        if (findStudentRecord(examId, currentUser().getUserId()) != null) {
            throw new BusinessException("该试卷已完成，不能重复考试");
        }
    }

    private ExamQuestion requireQuestionAudioViewable(Long questionId) {
        ExamQuestion question = requireQuestion(questionId);
        if (question.getAudioStoredName() == null || question.getAudioStoredName().trim().isEmpty()) {
            throw new BusinessException("该题目未上传听力音频");
        }
        String role = currentUser().getRole();
        if (RoleConstants.TEACHER.equals(role)) {
            return question;
        }
        if (RoleConstants.STUDENT.equals(role)) {
            String className = requireCurrentClassName();
            Long count = examPaperQuestionMapper.selectCount(Wrappers.<ExamPaperQuestion>lambdaQuery()
                    .eq(ExamPaperQuestion::getQuestionId, questionId)
                    .inSql(ExamPaperQuestion::getExamId,
                            "select id from exams where published = 1 and class_name = '" + escapeSql(className) + "'"));
            if (count != null && count > 0) {
                return question;
            }
        }
        throw new BusinessException("没有访问听力音频的权限");
    }

    private List<PaperQuestion> listPaperQuestions(Long examId) {
        List<ExamPaperQuestion> paperQuestions = examPaperQuestionMapper.selectList(Wrappers.<ExamPaperQuestion>lambdaQuery()
                .eq(ExamPaperQuestion::getExamId, examId)
                .orderByAsc(ExamPaperQuestion::getSortOrder));
        if (paperQuestions.isEmpty()) {
            return listLegacyQuestions(examId);
        }
        List<Long> questionIds = paperQuestions.stream()
                .map(ExamPaperQuestion::getQuestionId)
                .collect(Collectors.toList());
        Map<Long, ExamQuestion> questionMap = examQuestionMapper.selectBatchIds(questionIds).stream()
                .collect(Collectors.toMap(ExamQuestion::getId, question -> question, (left, right) -> left));
        return paperQuestions.stream()
                .filter(paperQuestion -> questionMap.containsKey(paperQuestion.getQuestionId()))
                .map(paperQuestion -> new PaperQuestion(questionMap.get(paperQuestion.getQuestionId()), paperQuestion))
                .collect(Collectors.toList());
    }

    private List<PaperQuestion> listLegacyQuestions(Long examId) {
        return examQuestionMapper.selectList(Wrappers.<ExamQuestion>lambdaQuery()
                        .eq(ExamQuestion::getExamId, examId)
                        .orderByAsc(ExamQuestion::getSortOrder))
                .stream()
                .map(question -> {
                    ExamPaperQuestion paperQuestion = new ExamPaperQuestion();
                    paperQuestion.setExamId(examId);
                    paperQuestion.setQuestionId(question.getId());
                    paperQuestion.setScore(defaultScore(question.getScore()));
                    paperQuestion.setSortOrder(question.getSortOrder());
                    return new PaperQuestion(question, paperQuestion);
                })
                .collect(Collectors.toList());
    }

    private Integer defaultRemainingSeconds(Exam exam, ExamRequests.ExamSessionRequest request) {
        if (request.getRemainingSeconds() != null) {
            return Math.max(request.getRemainingSeconds(), 0);
        }
        return exam.getDurationMinutes() * 60;
    }

    private Path resolveQuestionAudioPath(ExamQuestion question) {
        Path uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
        Path filePath = uploadRoot.resolve(question.getAudioStoredName()).normalize();
        if (!filePath.startsWith(uploadRoot)) {
            throw new BusinessException("听力音频路径非法");
        }
        return filePath;
    }

    private MediaType parseMediaType(String contentType) {
        if (contentType == null || contentType.trim().isEmpty()) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
        try {
            return MediaType.parseMediaType(contentType);
        } catch (Exception exception) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    private String normalizeAudioContentType(String contentType, String filename) {
        String normalized = trimToNull(contentType);
        if (normalized != null) {
            return normalized;
        }
        String extension = getFileExtension(filename).toLowerCase();
        if (".mp3".equals(extension)) {
            return "audio/mpeg";
        }
        if (".wav".equals(extension)) {
            return "audio/wav";
        }
        if (".m4a".equals(extension)) {
            return "audio/mp4";
        }
        if (".ogg".equals(extension)) {
            return "audio/ogg";
        }
        return MediaType.APPLICATION_OCTET_STREAM_VALUE;
    }

    private String getFileExtension(String filename) {
        int index = filename.lastIndexOf(".");
        return index < 0 ? "" : filename.substring(index);
    }

    private void fillExam(Exam exam, ExamRequests.ExamUpsertRequest request, List<ExamQuestion> selectedQuestions) {
        if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
            throw new BusinessException("考试标题不能为空");
        }
        exam.setTitle(request.getTitle().trim());
        exam.setDescription(request.getDescription() == null ? null : request.getDescription().trim());
        exam.setType(normalizeExamType(request.getType()));
        exam.setQuestionCount(selectedQuestions.size());
        exam.setDurationMinutes(request.getDurationMinutes() == null ? 30 : Math.max(request.getDurationMinutes(), 1));
        exam.setAutoSubmit(request.getAutoSubmit() == null || request.getAutoSubmit());
        exam.setPublished(request.getPublished() == null || request.getPublished());
        exam.setTotalScore(selectedQuestions.stream()
                .map(ExamQuestion::getScore)
                .mapToInt(this::defaultScore)
                .sum());
    }

    private void fillQuestion(ExamQuestion question, ExamRequests.QuestionItem request) {
        if (request.getContent() == null || request.getContent().trim().isEmpty()) {
            throw new BusinessException("题干不能为空");
        }
        String questionType = normalizeQuestionType(request.getQuestionType());
        String correctAnswer = request.getCorrectAnswer() == null ? "" : request.getCorrectAnswer().trim();
        if (correctAnswer.isEmpty()) {
            throw new BusinessException("标准答案不能为空");
        }
        question.setExamId(request.getExamId());
        question.setExamType(normalizeExamType(request.getExamType()));
        question.setQuestionType(questionType);
        question.setContent(request.getContent().trim());
        question.setOptionA(trimToNull(request.getOptionA()));
        question.setOptionB(trimToNull(request.getOptionB()));
        question.setOptionC(trimToNull(request.getOptionC()));
        question.setOptionD(trimToNull(request.getOptionD()));
        if ("TRUE_FALSE".equals(questionType)) {
            question.setOptionA(question.getOptionA() == null ? "正确" : question.getOptionA());
            question.setOptionB(question.getOptionB() == null ? "错误" : question.getOptionB());
        }
        if (isChoiceQuestion(questionType) && (question.getOptionA() == null || question.getOptionB() == null)) {
            throw new BusinessException("选择题至少需要填写 A、B 两个选项");
        }
        question.setCorrectAnswer(correctAnswer);
        question.setAnalysis(trimToNull(request.getAnalysis()));
        question.setAudioFileName(trimToNull(request.getAudioFileName()));
        question.setAudioStoredName(trimToNull(request.getAudioStoredName()));
        question.setAudioContentType(trimToNull(request.getAudioContentType()));
        question.setAudioFileSize(request.getAudioFileSize());
        if (LISTENING_QUESTION_TYPE.equals(questionType) && question.getAudioStoredName() == null) {
            throw new BusinessException("听力题需要上传音频文件");
        }
        question.setScore(request.getScore() == null ? 10 : Math.max(request.getScore(), 1));
        question.setEnabled(request.getEnabled() == null || request.getEnabled());
    }

    private List<ExamQuestion> selectRandomQuestions(String examType, int questionCount) {
        List<ExamQuestion> candidates = examQuestionMapper.selectList(Wrappers.<ExamQuestion>lambdaQuery()
                .eq(ExamQuestion::getExamType, examType)
                .eq(ExamQuestion::getEnabled, true));
        if (candidates.size() < questionCount) {
            throw new BusinessException("题库中“" + examType + "”题目不足，当前仅 " + candidates.size() + " 道，无法组 " + questionCount + " 道题");
        }
        Collections.shuffle(candidates);
        return candidates.subList(0, questionCount);
    }

    private void savePaperQuestions(Long examId, List<ExamQuestion> questions) {
        int sort = 1;
        for (ExamQuestion question : questions) {
            ExamPaperQuestion paperQuestion = new ExamPaperQuestion();
            paperQuestion.setExamId(examId);
            paperQuestion.setQuestionId(question.getId());
            paperQuestion.setScore(defaultScore(question.getScore()));
            paperQuestion.setSortOrder(sort++);
            examPaperQuestionMapper.insert(paperQuestion);
        }
    }

    private Map<String, Object> toSimpleView(Exam exam) {
        Map<String, Object> view = new LinkedHashMap<>();
        ExamRecord record = RoleConstants.STUDENT.equals(currentUser().getRole())
                ? findStudentRecord(exam.getId(), currentUser().getUserId())
                : null;
        view.put("id", exam.getId());
        view.put("title", exam.getTitle());
        view.put("description", exam.getDescription());
        view.put("type", exam.getType());
        view.put("questionCount", exam.getQuestionCount());
        view.put("teacherName", exam.getTeacherName());
        view.put("className", exam.getClassName());
        view.put("durationMinutes", exam.getDurationMinutes());
        view.put("autoSubmit", exam.getAutoSubmit());
        view.put("totalScore", exam.getTotalScore());
        view.put("published", exam.getPublished());
        view.put("createdAt", exam.getCreatedAt());
        view.put("completed", record != null);
        view.put("score", record == null ? null : record.getScore());
        view.put("submittedAt", record == null ? null : record.getSubmittedAt());
        return view;
    }

    private Map<String, Object> toDetailView(Exam exam, boolean includeCorrectAnswer) {
        List<PaperQuestion> questions = listPaperQuestions(exam.getId());
        Map<String, Object> view = toSimpleView(exam);
        view.put("questionCount", questions.size());
        view.put("questions", questions.stream()
                .map(paperQuestion -> toQuestionView(
                        paperQuestion.getQuestion(),
                        includeCorrectAnswer,
                        paperQuestion.getSortOrder(),
                        paperQuestion.getScore()))
                .collect(Collectors.toList()));
        view.put("session", RoleConstants.STUDENT.equals(currentUser().getRole()) ? getSession(exam.getId()) : null);
        return view;
    }

    private Map<String, Object> toQuestionView(ExamQuestion question,
                                               boolean includeCorrectAnswer,
                                               Integer sortOrder,
                                               Integer score) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", question.getId());
        view.put("examId", question.getExamId());
        view.put("examType", question.getExamType());
        view.put("questionType", normalizeQuestionType(question.getQuestionType()));
        view.put("questionTypeLabel", questionTypeLabel(question.getQuestionType()));
        view.put("content", question.getContent());
        view.put("optionA", question.getOptionA());
        view.put("optionB", question.getOptionB());
        view.put("optionC", question.getOptionC());
        view.put("optionD", question.getOptionD());
        view.put("analysis", question.getAnalysis());
        view.put("audioFileName", question.getAudioFileName());
        view.put("audioStoredName", includeCorrectAnswer ? question.getAudioStoredName() : null);
        view.put("audioContentType", question.getAudioContentType());
        view.put("audioFileSize", question.getAudioFileSize());
        view.put("hasAudio", question.getAudioStoredName() != null && !question.getAudioStoredName().trim().isEmpty());
        view.put("audioUrl", question.getAudioStoredName() == null ? null : "/api/student/exam-questions/" + question.getId() + "/audio");
        view.put("score", score);
        view.put("sortOrder", sortOrder);
        view.put("enabled", question.getEnabled() == null || question.getEnabled());
        if (includeCorrectAnswer) {
            view.put("correctAnswer", question.getCorrectAnswer());
        }
        return view;
    }

    private Map<String, Object> toRecordView(ExamRecord record) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", record.getId());
        view.put("examId", record.getExamId());
        view.put("examTitle", record.getExamTitle());
        view.put("studentId", record.getStudentId());
        view.put("studentName", record.getStudentName());
        view.put("className", record.getClassName());
        view.put("score", record.getScore());
        view.put("totalScore", record.getTotalScore());
        view.put("answers", readJson(record.getAnswersJson()));
        view.put("teacherFeedback", record.getTeacherFeedback());
        view.put("submittedAt", record.getSubmittedAt());
        return view;
    }

    private Map<String, Object> toSessionView(ExamSession session) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", session.getId());
        view.put("examId", session.getExamId());
        view.put("studentId", session.getStudentId());
        view.put("answers", readJson(session.getAnswersJson()));
        view.put("remainingSeconds", session.getRemainingSeconds());
        view.put("status", session.getStatus());
        view.put("updatedAt", session.getUpdatedAt());
        return view;
    }

    private boolean isAnswerCorrect(String questionType, String correctAnswer, String answer) {
        String normalizedType = normalizeQuestionType(questionType);
        if ("MULTIPLE_CHOICE".equals(normalizedType)) {
            return normalizeMultiAnswer(correctAnswer).equals(normalizeMultiAnswer(answer));
        }
        return normalizeTextAnswer(correctAnswer).equals(normalizeTextAnswer(answer));
    }

    private void validateStudentExamClass(Exam exam) {
        if (!sameClassName(exam.getClassName(), requireCurrentClassName())) {
            throw new BusinessException("只能参加本班考试");
        }
    }

    private String normalizeMultiAnswer(String answer) {
        if (answer == null) {
            return "";
        }
        return Arrays.stream(answer.toUpperCase()
                        .replace("，", ",")
                        .replace("、", ",")
                        .replace(";", ",")
                        .split("[,\\s]+"))
                .map(String::trim)
                .filter(item -> !item.isEmpty())
                .distinct()
                .sorted()
                .collect(Collectors.joining(","));
    }

    private String normalizeTextAnswer(String answer) {
        return answer == null ? "" : answer.trim().replaceAll("\\s+", " ").toUpperCase();
    }

    private boolean isChoiceQuestion(String questionType) {
        String normalizedType = normalizeQuestionType(questionType);
        return "SINGLE_CHOICE".equals(normalizedType)
                || "MULTIPLE_CHOICE".equals(normalizedType)
                || LISTENING_QUESTION_TYPE.equals(normalizedType);
    }

    private String normalizeExamType(String type) {
        String value = trimToNull(type);
        return value == null ? DEFAULT_EXAM_TYPE : value;
    }

    private String normalizeQuestionType(String questionType) {
        String value = trimToNull(questionType);
        if (value == null) {
            return DEFAULT_QUESTION_TYPE;
        }
        switch (value.toUpperCase()) {
            case "单选":
            case "单选题":
            case "SINGLE":
            case "SINGLE_CHOICE":
                return "SINGLE_CHOICE";
            case "听力":
            case "听力题":
            case "听力选择":
            case "听力选择题":
            case "LISTENING":
            case "LISTENING_CHOICE":
                return LISTENING_QUESTION_TYPE;
            case "多选":
            case "多选题":
            case "MULTIPLE":
            case "MULTIPLE_CHOICE":
                return "MULTIPLE_CHOICE";
            case "判断":
            case "判断题":
            case "TRUE_FALSE":
                return "TRUE_FALSE";
            case "填空":
            case "填空题":
            case "FILL_BLANK":
                return "FILL_BLANK";
            case "简答":
            case "简答题":
            case "SHORT_ANSWER":
                return "SHORT_ANSWER";
            default:
                return value;
        }
    }

    private String questionTypeLabel(String questionType) {
        switch (normalizeQuestionType(questionType)) {
            case "LISTENING_CHOICE":
                return "听力题";
            case "MULTIPLE_CHOICE":
                return "多选题";
            case "TRUE_FALSE":
                return "判断题";
            case "FILL_BLANK":
                return "填空题";
            case "SHORT_ANSWER":
                return "简答题";
            case "SINGLE_CHOICE":
            default:
                return "单选题";
        }
    }

    private int normalizeQuestionCount(Integer questionCount) {
        return questionCount == null ? 5 : Math.max(questionCount, 1);
    }

    private int defaultScore(Integer score) {
        return score == null ? 10 : Math.max(score, 0);
    }

    private String trimToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

    private String escapeSql(String value) {
        return value == null ? "" : value.replace("'", "''");
    }

    private String writeJson(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException exception) {
            throw new BusinessException("答案保存失败");
        }
    }

    private Object readJson(String json) {
        if (json == null || json.trim().isEmpty()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(json, List.class);
        } catch (Exception exception) {
            return new ArrayList<>();
        }
    }

    private static class PaperQuestion {
        private final ExamQuestion question;
        private final ExamPaperQuestion paperQuestion;

        private PaperQuestion(ExamQuestion question, ExamPaperQuestion paperQuestion) {
            this.question = question;
            this.paperQuestion = paperQuestion;
        }

        private ExamQuestion getQuestion() {
            return question;
        }

        private Integer getScore() {
            return paperQuestion.getScore() == null ? question.getScore() : paperQuestion.getScore();
        }

        private Integer getSortOrder() {
            return paperQuestion.getSortOrder();
        }
    }
}
