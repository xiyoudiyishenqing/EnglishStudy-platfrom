package com.englishstudy.backend.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.englishstudy.backend.common.BusinessException;
import com.englishstudy.backend.constant.RoleConstants;
import com.englishstudy.backend.constant.StatusConstants;
import com.englishstudy.backend.entity.*;
import com.englishstudy.backend.mapper.*;
import com.englishstudy.backend.service.BaseService;
import com.englishstudy.backend.service.DashboardService;
import com.englishstudy.backend.service.NotificationService;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DashboardServiceImpl extends BaseService implements DashboardService {

    private final ResourceMapper resourceMapper;
    private final StudyPlanMapper studyPlanMapper;
    private final ExamMapper examMapper;
    private final ExamRecordMapper examRecordMapper;
    private final QaQuestionMapper qaQuestionMapper;
    private final UserMapper userMapper;
    private final HomeworkMapper homeworkMapper;
    private final HomeworkSubmissionMapper homeworkSubmissionMapper;
    private final NotificationService notificationService;

    public DashboardServiceImpl(ResourceMapper resourceMapper,
                                StudyPlanMapper studyPlanMapper,
                                ExamMapper examMapper,
                                ExamRecordMapper examRecordMapper,
                                QaQuestionMapper qaQuestionMapper,
                                UserMapper userMapper,
                                HomeworkMapper homeworkMapper,
                                HomeworkSubmissionMapper homeworkSubmissionMapper,
                                NotificationService notificationService) {
        this.resourceMapper = resourceMapper;
        this.studyPlanMapper = studyPlanMapper;
        this.examMapper = examMapper;
        this.examRecordMapper = examRecordMapper;
        this.qaQuestionMapper = qaQuestionMapper;
        this.userMapper = userMapper;
        this.homeworkMapper = homeworkMapper;
        this.homeworkSubmissionMapper = homeworkSubmissionMapper;
        this.notificationService = notificationService;
    }

    @Override
    public Map<String, Object> studentDashboard() {
        requireRole(RoleConstants.STUDENT);
        String className = requireCurrentClassName();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("resourceCount", resourceMapper.selectList(Wrappers.<ResourceItem>lambdaQuery()
                        .eq(ResourceItem::getAuditStatus, StatusConstants.APPROVED)
                        .eq(ResourceItem::getOnlineStatus, StatusConstants.PUBLISHED))
                .stream()
                .filter(item -> StatusConstants.PUBLIC.equals(item.getVisibility())
                        || (StatusConstants.CLASS.equals(item.getVisibility()) && sameClassName(className, item.getClassName())))
                .filter(item -> !RoleConstants.TEACHER.equals(item.getCreatorRole()) || sameClassName(className, item.getClassName()))
                .count());
        result.put("planCount", studyPlanMapper.selectCount(Wrappers.<StudyPlan>lambdaQuery()
                .eq(StudyPlan::getStudentId, currentUser().getUserId())));
        result.put("recordCount", examRecordMapper.selectCount(Wrappers.<ExamRecord>lambdaQuery()
                .eq(ExamRecord::getStudentId, currentUser().getUserId())));
        result.put("examCount", examMapper.selectList(Wrappers.<Exam>lambdaQuery()
                        .eq(Exam::getPublished, true)
                        .eq(Exam::getClassName, className))
                .stream()
                .filter(exam -> {
                    Long completedCount = examRecordMapper.selectCount(Wrappers.<ExamRecord>lambdaQuery()
                            .eq(ExamRecord::getExamId, exam.getId())
                            .eq(ExamRecord::getStudentId, currentUser().getUserId()));
                    return completedCount == null || completedCount == 0;
                })
                .count());
        result.put("notifications", notificationService.listForCurrentUser().stream().limit(5).collect(Collectors.toList()));
        return result;
    }

    @Override
    public Map<String, Object> teacherDashboard() {
        requireRole(RoleConstants.TEACHER);
        String className = requireCurrentClassName();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("resourceCount", resourceMapper.selectCount(Wrappers.<ResourceItem>lambdaQuery()
                .eq(ResourceItem::getCreatorId, currentUser().getUserId())
                .eq(ResourceItem::getClassName, className)));
        result.put("examCount", examMapper.selectCount(Wrappers.<Exam>lambdaQuery()
                .eq(Exam::getTeacherId, currentUser().getUserId())
                .eq(Exam::getClassName, className)));
        result.put("qaCount", qaQuestionMapper.selectCount(Wrappers.<QaQuestion>lambdaQuery()
                .eq(QaQuestion::getClassName, className)
                .eq(QaQuestion::getStatus, StatusConstants.OPEN)));
        result.put("studentCount", userMapper.selectCount(Wrappers.<User>lambdaQuery()
                .eq(User::getRole, RoleConstants.STUDENT)
                .eq(User::getClassName, className)));
        result.put("homeworkCount", homeworkMapper.selectCount(Wrappers.<Homework>lambdaQuery()
                .eq(Homework::getTeacherId, currentUser().getUserId())
                .eq(Homework::getClassName, className)));
        result.put("submittedHomeworkCount", homeworkSubmissionMapper.selectCount(Wrappers.<HomeworkSubmission>lambdaQuery()
                .inSql(HomeworkSubmission::getHomeworkId,
                        "select id from homeworks where teacher_id = " + currentUser().getUserId()
                                + " and class_name = '" + escapeSql(className) + "'")));
        result.put("gradedHomeworkCount", homeworkSubmissionMapper.selectCount(Wrappers.<HomeworkSubmission>lambdaQuery()
                .eq(HomeworkSubmission::getStatus, StatusConstants.GRADED)
                .inSql(HomeworkSubmission::getHomeworkId,
                        "select id from homeworks where teacher_id = " + currentUser().getUserId()
                                + " and class_name = '" + escapeSql(className) + "'")));
        result.put("examParticipation", examRecordMapper.selectCount(Wrappers.<ExamRecord>lambdaQuery()
                .inSql(ExamRecord::getExamId,
                        "select id from exams where teacher_id = " + currentUser().getUserId()
                                + " and class_name = '" + escapeSql(className) + "'")
                .eq(ExamRecord::getClassName, className)));
        result.put("averageScore", calculateAverageScore());
        result.put("notifications", notificationService.listForCurrentUser().stream().limit(5).collect(Collectors.toList()));
        return result;
    }

    @Override
    public Map<String, Object> byRole(String role) {
        if (RoleConstants.STUDENT.equals(role)) {
            return studentDashboard();
        }
        if (RoleConstants.TEACHER.equals(role)) {
            return teacherDashboard();
        }
        throw new BusinessException("不支持的仪表盘角色");
    }

    private double calculateAverageScore() {
        String className = requireCurrentClassName();
        return examRecordMapper.selectList(Wrappers.<ExamRecord>lambdaQuery()
                        .inSql(ExamRecord::getExamId,
                                "select id from exams where teacher_id = " + currentUser().getUserId()
                                        + " and class_name = '" + escapeSql(className) + "'"))
                .stream()
                .filter(record -> sameClassName(className, record.getClassName()))
                .filter(record -> record.getScore() != null)
                .mapToInt(ExamRecord::getScore)
                .average()
                .orElse(0);
    }

    private String escapeSql(String value) {
        return value == null ? "" : value.replace("'", "''");
    }
}
