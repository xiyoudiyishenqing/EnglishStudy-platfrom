package com.englishstudy.backend.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.englishstudy.backend.common.BusinessException;
import com.englishstudy.backend.constant.RoleConstants;
import com.englishstudy.backend.constant.StatusConstants;
import com.englishstudy.backend.entity.Homework;
import com.englishstudy.backend.entity.HomeworkSubmission;
import com.englishstudy.backend.mapper.HomeworkMapper;
import com.englishstudy.backend.mapper.HomeworkSubmissionMapper;
import com.englishstudy.backend.request.HomeworkRequests;
import com.englishstudy.backend.service.BaseService;
import com.englishstudy.backend.service.HomeworkService;
import com.englishstudy.backend.service.LogService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class HomeworkServiceImpl extends BaseService implements HomeworkService {

    private final HomeworkMapper homeworkMapper;
    private final HomeworkSubmissionMapper submissionMapper;
    private final LogService logService;

    public HomeworkServiceImpl(HomeworkMapper homeworkMapper,
                               HomeworkSubmissionMapper submissionMapper,
                               LogService logService) {
        this.homeworkMapper = homeworkMapper;
        this.submissionMapper = submissionMapper;
        this.logService = logService;
    }

    @Override
    public List<Map<String, Object>> listForStudent() {
        requireRole(RoleConstants.STUDENT);
        String className = requireCurrentClassName();
        return homeworkMapper.selectList(Wrappers.<Homework>lambdaQuery()
                        .eq(Homework::getStatus, StatusConstants.PUBLISHED)
                        .eq(Homework::getClassName, className)
                        .orderByDesc(Homework::getCreatedAt))
                .stream()
                .map(homework -> homeworkView(homework, findMySubmission(homework.getId())))
                .collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> listForTeacher() {
        requireRole(RoleConstants.TEACHER);
        String className = requireCurrentClassName();
        return homeworkMapper.selectList(Wrappers.<Homework>lambdaQuery()
                        .eq(Homework::getTeacherId, currentUser().getUserId())
                        .eq(Homework::getClassName, className)
                        .orderByDesc(Homework::getCreatedAt))
                .stream()
                .map(homework -> homeworkView(homework, null))
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> create(HomeworkRequests.HomeworkUpsertRequest request) {
        requireRole(RoleConstants.TEACHER);
        Homework homework = new Homework();
        fillHomework(homework, request);
        homework.setTeacherId(currentUser().getUserId());
        homework.setTeacherName(currentUser().getRealName());
        markForInsert(homework);
        homeworkMapper.insert(homework);
        logService.save("班级作业", "发布作业", homework.getTitle());
        return homeworkView(homework, null);
    }

    @Override
    public Map<String, Object> update(Long id, HomeworkRequests.HomeworkUpsertRequest request) {
        requireRole(RoleConstants.TEACHER);
        Homework homework = requireHomework(id);
        validateTeacherOwner(homework);
        fillHomework(homework, request);
        markForUpdate(homework);
        homeworkMapper.updateById(homework);
        logService.save("班级作业", "更新作业", homework.getTitle());
        return homeworkView(homework, null);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        requireRole(RoleConstants.TEACHER);
        Homework homework = requireHomework(id);
        validateTeacherOwner(homework);
        submissionMapper.delete(Wrappers.<HomeworkSubmission>lambdaQuery().eq(HomeworkSubmission::getHomeworkId, id));
        homeworkMapper.deleteById(id);
        logService.save("班级作业", "删除作业", homework.getTitle());
    }

    @Override
    public Map<String, Object> submit(Long homeworkId, HomeworkRequests.SubmissionRequest request) {
        requireRole(RoleConstants.STUDENT);
        Homework homework = requireHomework(homeworkId);
        if (!StatusConstants.PUBLISHED.equals(homework.getStatus())) {
            throw new BusinessException("作业暂未发布");
        }
        if (!sameClassName(homework.getClassName(), requireCurrentClassName())) {
            throw new BusinessException("只能提交本班作业");
        }
        HomeworkSubmission submission = findMySubmission(homeworkId);
        if (submission == null) {
            submission = new HomeworkSubmission();
            submission.setHomeworkId(homeworkId);
            submission.setStudentId(currentUser().getUserId());
            submission.setStudentName(currentUser().getRealName());
            submission.setStatus(StatusConstants.SUBMITTED);
            submission.setSubmittedAt(LocalDateTime.now());
            markForInsert(submission);
            submission.setContent(trimToNull(request.getContent()));
            submissionMapper.insert(submission);
        } else {
            submission.setContent(trimToNull(request.getContent()));
            submission.setStatus(StatusConstants.SUBMITTED);
            submission.setSubmittedAt(LocalDateTime.now());
            markForUpdate(submission);
            submissionMapper.updateById(submission);
        }
        logService.save("班级作业", "提交作业", homework.getTitle());
        return submissionView(submission);
    }

    @Override
    public List<Map<String, Object>> listSubmissions(Long homeworkId) {
        requireRole(RoleConstants.TEACHER);
        Homework homework = requireHomework(homeworkId);
        validateTeacherOwner(homework);
        return submissionMapper.selectList(Wrappers.<HomeworkSubmission>lambdaQuery()
                        .eq(HomeworkSubmission::getHomeworkId, homeworkId)
                        .orderByDesc(HomeworkSubmission::getSubmittedAt))
                .stream()
                .map(this::submissionView)
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> gradeSubmission(Long submissionId, HomeworkRequests.GradeRequest request) {
        requireRole(RoleConstants.TEACHER);
        HomeworkSubmission submission = submissionMapper.selectById(submissionId);
        if (submission == null) {
            throw new BusinessException("作业提交记录不存在");
        }
        Homework homework = requireHomework(submission.getHomeworkId());
        validateTeacherOwner(homework);
        submission.setScore(request.getScore());
        submission.setTeacherFeedback(trimToNull(request.getTeacherFeedback()));
        submission.setStatus(StatusConstants.GRADED);
        submission.setGradedAt(LocalDateTime.now());
        markForUpdate(submission);
        submissionMapper.updateById(submission);
        logService.save("班级作业", "批改作业", homework.getTitle());
        return submissionView(submission);
    }

    private Homework requireHomework(Long id) {
        Homework homework = homeworkMapper.selectById(id);
        if (homework == null) {
            throw new BusinessException("作业不存在");
        }
        return homework;
    }

    private HomeworkSubmission findMySubmission(Long homeworkId) {
        return submissionMapper.selectOne(Wrappers.<HomeworkSubmission>lambdaQuery()
                .eq(HomeworkSubmission::getHomeworkId, homeworkId)
                .eq(HomeworkSubmission::getStudentId, currentUser().getUserId())
                .last("limit 1"));
    }

    private void validateTeacherOwner(Homework homework) {
        if (!homework.getTeacherId().equals(currentUser().getUserId())) {
            throw new BusinessException("只能操作自己发布的作业");
        }
        if (!sameClassName(homework.getClassName(), requireCurrentClassName())) {
            throw new BusinessException("只能操作本班作业");
        }
    }

    private void fillHomework(Homework homework, HomeworkRequests.HomeworkUpsertRequest request) {
        if (isBlank(request.getTitle())) {
            throw new BusinessException("作业标题不能为空");
        }
        homework.setTitle(request.getTitle().trim());
        homework.setDescription(trimToNull(request.getDescription()));
        homework.setClassName(requireCurrentClassName());
        homework.setDueTime(parseDateTime(request.getDueTime()));
        homework.setStatus(isBlank(request.getStatus()) ? StatusConstants.PUBLISHED : request.getStatus().trim());
    }

    private Map<String, Object> homeworkView(Homework homework, HomeworkSubmission mySubmission) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", homework.getId());
        view.put("title", homework.getTitle());
        view.put("description", homework.getDescription());
        view.put("className", homework.getClassName());
        view.put("teacherId", homework.getTeacherId());
        view.put("teacherName", homework.getTeacherName());
        view.put("dueTime", homework.getDueTime());
        view.put("status", homework.getStatus());
        view.put("createdAt", homework.getCreatedAt());
        view.put("submissionCount", submissionMapper.selectCount(Wrappers.<HomeworkSubmission>lambdaQuery()
                .eq(HomeworkSubmission::getHomeworkId, homework.getId())));
        view.put("mySubmission", mySubmission == null ? null : submissionView(mySubmission));
        return view;
    }

    private Map<String, Object> submissionView(HomeworkSubmission submission) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", submission.getId());
        view.put("homeworkId", submission.getHomeworkId());
        view.put("studentId", submission.getStudentId());
        view.put("studentName", submission.getStudentName());
        view.put("content", submission.getContent());
        view.put("score", submission.getScore());
        view.put("teacherFeedback", submission.getTeacherFeedback());
        view.put("status", submission.getStatus());
        view.put("submittedAt", submission.getSubmittedAt());
        view.put("gradedAt", submission.getGradedAt());
        return view;
    }

    private LocalDateTime parseDateTime(String value) {
        if (isBlank(value)) {
            return null;
        }
        String normalized = value.trim().replace("T", " ");
        if (normalized.length() == 16) {
            normalized = normalized + ":00";
        }
        return LocalDateTime.parse(normalized, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
