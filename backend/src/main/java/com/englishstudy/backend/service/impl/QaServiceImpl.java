package com.englishstudy.backend.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.englishstudy.backend.common.BusinessException;
import com.englishstudy.backend.constant.RoleConstants;
import com.englishstudy.backend.constant.StatusConstants;
import com.englishstudy.backend.entity.QaAnswer;
import com.englishstudy.backend.entity.QaQuestion;
import com.englishstudy.backend.entity.ResourceItem;
import com.englishstudy.backend.mapper.QaAnswerMapper;
import com.englishstudy.backend.mapper.QaQuestionMapper;
import com.englishstudy.backend.mapper.ResourceMapper;
import com.englishstudy.backend.request.QaRequests;
import com.englishstudy.backend.service.BaseService;
import com.englishstudy.backend.service.LogService;
import com.englishstudy.backend.service.QaService;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class QaServiceImpl extends BaseService implements QaService {

    private final QaQuestionMapper qaQuestionMapper;
    private final QaAnswerMapper qaAnswerMapper;
    private final ResourceMapper resourceMapper;
    private final LogService logService;

    public QaServiceImpl(QaQuestionMapper qaQuestionMapper,
                         QaAnswerMapper qaAnswerMapper,
                         ResourceMapper resourceMapper,
                         LogService logService) {
        this.qaQuestionMapper = qaQuestionMapper;
        this.qaAnswerMapper = qaAnswerMapper;
        this.resourceMapper = resourceMapper;
        this.logService = logService;
    }

    @Override
    public List<Map<String, Object>> listStudentQuestions() {
        requireRole(RoleConstants.STUDENT);
        return qaQuestionMapper.selectList(Wrappers.<QaQuestion>lambdaQuery()
                        .eq(QaQuestion::getStudentId, currentUser().getUserId())
                        .orderByDesc(QaQuestion::getCreatedAt))
                .stream()
                .map(this::toView)
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> createQuestion(QaRequests.QuestionCreateRequest request) {
        requireRole(RoleConstants.STUDENT);
        if (request.getResourceId() == null) {
            throw new BusinessException("请选择要答疑的学习资源");
        }
        if (isBlank(request.getTitle()) || isBlank(request.getContent())) {
            throw new BusinessException("问题标题和内容不能为空");
        }
        String className = requireCurrentClassName();
        ResourceItem resource = requireStudentVisibleResource(request.getResourceId(), className);
        QaQuestion question = new QaQuestion();
        question.setStudentId(currentUser().getUserId());
        question.setStudentName(currentUser().getRealName());
        question.setClassName(className);
        question.setResourceId(resource.getId());
        question.setResourceTitle(resource.getTitle());
        question.setCourseTitle(resource.getTitle());
        question.setTitle(request.getTitle().trim());
        question.setContent(request.getContent().trim());
        question.setStatus(StatusConstants.OPEN);
        markForInsert(question);
        qaQuestionMapper.insert(question);
        logService.save("在线答疑", "提出问题", question.getTitle());
        return toView(question);
    }

    @Override
    public List<Map<String, Object>> listAllQuestionsForTeacher() {
        requireRole(RoleConstants.TEACHER);
        String className = requireCurrentClassName();
        return qaQuestionMapper.selectList(Wrappers.<QaQuestion>lambdaQuery()
                        .eq(QaQuestion::getClassName, className)
                        .orderByDesc(QaQuestion::getCreatedAt))
                .stream()
                .map(this::toView)
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> answerQuestion(Long questionId, QaRequests.AnswerCreateRequest request) {
        requireRole(RoleConstants.TEACHER);
        QaQuestion question = qaQuestionMapper.selectById(questionId);
        if (question == null) {
            throw new BusinessException("问题不存在");
        }
        if (!sameClassName(question.getClassName(), requireCurrentClassName())) {
            throw new BusinessException("只能回复本班学生的问题");
        }
        if (isBlank(request.getContent())) {
            throw new BusinessException("答疑内容不能为空");
        }
        QaAnswer answer = new QaAnswer();
        answer.setQuestionId(questionId);
        answer.setTeacherId(currentUser().getUserId());
        answer.setTeacherName(currentUser().getRealName());
        answer.setContent(request.getContent().trim());
        markForInsert(answer);
        qaAnswerMapper.insert(answer);
        question.setStatus(StatusConstants.ANSWERED);
        markForUpdate(question);
        qaQuestionMapper.updateById(question);
        logService.save("在线答疑", "教师答疑", question.getTitle());
        return toView(question);
    }

    private Map<String, Object> toView(QaQuestion question) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", question.getId());
        view.put("studentId", question.getStudentId());
        view.put("studentName", question.getStudentName());
        view.put("className", question.getClassName());
        view.put("resourceId", question.getResourceId());
        view.put("resourceTitle", question.getResourceTitle());
        view.put("courseTitle", question.getCourseTitle());
        view.put("title", question.getTitle());
        view.put("content", question.getContent());
        view.put("status", question.getStatus());
        view.put("createdAt", question.getCreatedAt());
        view.put("answers", qaAnswerMapper.selectList(Wrappers.<QaAnswer>lambdaQuery()
                        .eq(QaAnswer::getQuestionId, question.getId())
                        .orderByAsc(QaAnswer::getCreatedAt))
                .stream()
                .map(this::answerView)
                .collect(Collectors.toList()));
        return view;
    }

    private Map<String, Object> answerView(QaAnswer answer) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", answer.getId());
        view.put("teacherId", answer.getTeacherId());
        view.put("teacherName", answer.getTeacherName());
        view.put("content", answer.getContent());
        view.put("createdAt", answer.getCreatedAt());
        return view;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private ResourceItem requireStudentVisibleResource(Long resourceId, String className) {
        ResourceItem resource = resourceMapper.selectById(resourceId);
        if (resource == null) {
            throw new BusinessException("学习资源不存在");
        }
        if (!StatusConstants.APPROVED.equals(resource.getAuditStatus())
                || !StatusConstants.PUBLISHED.equals(resource.getOnlineStatus())) {
            throw new BusinessException("该学习资源暂不可提问");
        }
        if (RoleConstants.TEACHER.equals(resource.getCreatorRole())
                && !sameClassName(className, resource.getClassName())) {
            throw new BusinessException("只能对本班资源发起答疑");
        }
        if (StatusConstants.CLASS.equals(resource.getVisibility())
                && !sameClassName(className, resource.getClassName())) {
            throw new BusinessException("只能对本班可见资源发起答疑");
        }
        return resource;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
