package com.englishstudy.backend.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.englishstudy.backend.common.BusinessException;
import com.englishstudy.backend.constant.RoleConstants;
import com.englishstudy.backend.constant.StatusConstants;
import com.englishstudy.backend.entity.Exam;
import com.englishstudy.backend.entity.ExamRecord;
import com.englishstudy.backend.entity.Homework;
import com.englishstudy.backend.entity.HomeworkSubmission;
import com.englishstudy.backend.entity.QaQuestion;
import com.englishstudy.backend.entity.ResourceItem;
import com.englishstudy.backend.entity.User;
import com.englishstudy.backend.mapper.ExamMapper;
import com.englishstudy.backend.mapper.ExamRecordMapper;
import com.englishstudy.backend.mapper.HomeworkMapper;
import com.englishstudy.backend.mapper.HomeworkSubmissionMapper;
import com.englishstudy.backend.mapper.QaQuestionMapper;
import com.englishstudy.backend.mapper.ResourceMapper;
import com.englishstudy.backend.mapper.UserMapper;
import com.englishstudy.backend.service.BaseService;
import com.englishstudy.backend.service.OllamaService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class OllamaServiceImpl extends BaseService implements OllamaService {

    private final RestTemplate restTemplate;
    private final UserMapper userMapper;
    private final ResourceMapper resourceMapper;
    private final ExamMapper examMapper;
    private final ExamRecordMapper examRecordMapper;
    private final HomeworkMapper homeworkMapper;
    private final HomeworkSubmissionMapper homeworkSubmissionMapper;
    private final QaQuestionMapper qaQuestionMapper;

    @Value("${ollama.base-url}")
    private String baseUrl;

    @Value("${ollama.model}")
    private String model;

    public OllamaServiceImpl(RestTemplate restTemplate,
                             UserMapper userMapper,
                             ResourceMapper resourceMapper,
                             ExamMapper examMapper,
                             ExamRecordMapper examRecordMapper,
                             HomeworkMapper homeworkMapper,
                             HomeworkSubmissionMapper homeworkSubmissionMapper,
                             QaQuestionMapper qaQuestionMapper) {
        this.restTemplate = restTemplate;
        this.userMapper = userMapper;
        this.resourceMapper = resourceMapper;
        this.examMapper = examMapper;
        this.examRecordMapper = examRecordMapper;
        this.homeworkMapper = homeworkMapper;
        this.homeworkSubmissionMapper = homeworkSubmissionMapper;
        this.qaQuestionMapper = qaQuestionMapper;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> explainWord(String query) {
        if (query == null || query.trim().isEmpty()) {
            throw new BusinessException("请输入要查询的单词或短语");
        }
        String prompt = "你是大学生英语学习平台中的AI词典助手。请针对用户输入的英文单词、短语或句子，"
                + "使用简洁清晰的中文给出：1. 中文释义 2. 词性或用法 3. 一个英文例句和中文翻译 4. 学习建议。"
                + "请直接输出自然语言，不要输出JSON。用户输入：" + query.trim();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("query", query.trim());
        result.put("answer", generate(prompt));
        return result;
    }

    @Override
    public Map<String, Object> teacherTeachingAdvice() {
        requireRole(RoleConstants.TEACHER);
        String className = requireCurrentClassName();
        Map<String, Object> data = collectTeacherData(className);
        String prompt = "你是大学英语课程的AI教学分析助手。请基于以下教师本班教学数据，"
                + "用中文输出一份面向任课教师的教学建议。要求："
                + "1. 先简要概括班级教学现状；"
                + "2. 分析成绩、作业、答疑、资源建设中可能存在的问题；"
                + "3. 给出3到5条可执行的后续教学建议；"
                + "4. 语言务实，适合教师直接参考，不要输出JSON。\n"
                + "班级数据如下：\n" + buildTeacherDataText(data);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("className", className);
        result.put("generatedAt", java.time.LocalDateTime.now());
        result.put("data", data);
        result.put("answer", generate(prompt));
        return result;
    }

    @SuppressWarnings("unchecked")
    private String generate(String prompt) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("model", model);
        request.put("prompt", prompt);
        request.put("stream", false);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> response;
        try {
            response = restTemplate.postForObject(baseUrl + "/api/generate", new HttpEntity<>(request, headers), Map.class);
        } catch (Exception exception) {
            throw new BusinessException("调用本地Ollama失败，请确认模型服务已启动");
        }
        if (response == null || response.get("response") == null) {
            throw new BusinessException("Ollama返回结果为空");
        }
        return String.valueOf(response.get("response")).trim();
    }

    private Map<String, Object> collectTeacherData(String className) {
        Long teacherId = currentUser().getUserId();
        List<Exam> exams = examMapper.selectList(Wrappers.<Exam>lambdaQuery()
                .eq(Exam::getTeacherId, teacherId)
                .eq(Exam::getClassName, className));
        List<Long> examIds = exams.stream().map(Exam::getId).collect(Collectors.toList());
        List<ExamRecord> records = examIds.isEmpty()
                ? java.util.Collections.emptyList()
                : examRecordMapper.selectList(Wrappers.<ExamRecord>lambdaQuery()
                        .in(ExamRecord::getExamId, examIds)
                        .eq(ExamRecord::getClassName, className));

        List<Homework> homeworks = homeworkMapper.selectList(Wrappers.<Homework>lambdaQuery()
                .eq(Homework::getTeacherId, teacherId)
                .eq(Homework::getClassName, className));
        List<Long> homeworkIds = homeworks.stream().map(Homework::getId).collect(Collectors.toList());
        List<HomeworkSubmission> submissions = homeworkIds.isEmpty()
                ? java.util.Collections.emptyList()
                : homeworkSubmissionMapper.selectList(Wrappers.<HomeworkSubmission>lambdaQuery()
                        .in(HomeworkSubmission::getHomeworkId, homeworkIds));

        long studentCount = number(userMapper.selectCount(Wrappers.<User>lambdaQuery()
                .eq(User::getRole, RoleConstants.STUDENT)
                .eq(User::getClassName, className)));
        long resourceCount = number(resourceMapper.selectCount(Wrappers.<ResourceItem>lambdaQuery()
                .eq(ResourceItem::getCreatorId, teacherId)
                .eq(ResourceItem::getClassName, className)));
        long qaOpenCount = number(qaQuestionMapper.selectCount(Wrappers.<QaQuestion>lambdaQuery()
                .eq(QaQuestion::getClassName, className)
                .eq(QaQuestion::getStatus, StatusConstants.OPEN)));
        long qaAnsweredCount = number(qaQuestionMapper.selectCount(Wrappers.<QaQuestion>lambdaQuery()
                .eq(QaQuestion::getClassName, className)
                .eq(QaQuestion::getStatus, StatusConstants.ANSWERED)));

        double averageScore = records.stream()
                .filter(record -> record.getScore() != null)
                .mapToInt(ExamRecord::getScore)
                .average()
                .orElse(0);
        int minScore = records.stream()
                .filter(record -> record.getScore() != null)
                .mapToInt(ExamRecord::getScore)
                .min()
                .orElse(0);
        int maxScore = records.stream()
                .filter(record -> record.getScore() != null)
                .mapToInt(ExamRecord::getScore)
                .max()
                .orElse(0);
        long lowScoreCount = records.stream()
                .filter(record -> record.getScore() != null && record.getTotalScore() != null && record.getTotalScore() > 0)
                .filter(record -> record.getScore() * 100.0 / record.getTotalScore() < 60)
                .count();
        double averageHomeworkScore = submissions.stream()
                .filter(submission -> submission.getScore() != null)
                .mapToInt(HomeworkSubmission::getScore)
                .average()
                .orElse(0);
        long gradedCount = submissions.stream()
                .filter(submission -> StatusConstants.GRADED.equals(submission.getStatus()))
                .count();

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("studentCount", studentCount);
        data.put("resourceCount", resourceCount);
        data.put("examCount", exams.size());
        data.put("examRecordCount", records.size());
        data.put("averageScore", round1(averageScore));
        data.put("minScore", minScore);
        data.put("maxScore", maxScore);
        data.put("lowScoreCount", lowScoreCount);
        data.put("homeworkCount", homeworks.size());
        data.put("homeworkSubmissionCount", submissions.size());
        data.put("gradedHomeworkCount", gradedCount);
        data.put("averageHomeworkScore", round1(averageHomeworkScore));
        data.put("openQuestionCount", qaOpenCount);
        data.put("answeredQuestionCount", qaAnsweredCount);
        return data;
    }

    private String buildTeacherDataText(Map<String, Object> data) {
        return "学生人数：" + data.get("studentCount") + "\n"
                + "教师发布资源数：" + data.get("resourceCount") + "\n"
                + "考试数量：" + data.get("examCount") + "\n"
                + "考试参与记录数：" + data.get("examRecordCount") + "\n"
                + "考试平均分：" + data.get("averageScore") + "\n"
                + "最低分：" + data.get("minScore") + "\n"
                + "最高分：" + data.get("maxScore") + "\n"
                + "低于60%得分记录数：" + data.get("lowScoreCount") + "\n"
                + "作业数量：" + data.get("homeworkCount") + "\n"
                + "作业提交数：" + data.get("homeworkSubmissionCount") + "\n"
                + "已批改作业数：" + data.get("gradedHomeworkCount") + "\n"
                + "作业平均分：" + data.get("averageHomeworkScore") + "\n"
                + "待答疑问题数：" + data.get("openQuestionCount") + "\n"
                + "已答疑问题数：" + data.get("answeredQuestionCount") + "\n";
    }

    private long number(Long value) {
        return value == null ? 0 : value;
    }

    private double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
