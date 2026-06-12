package com.englishstudy.backend.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.englishstudy.backend.common.BusinessException;
import com.englishstudy.backend.constant.RoleConstants;
import com.englishstudy.backend.constant.StatusConstants;
import com.englishstudy.backend.entity.StudyPlan;
import com.englishstudy.backend.mapper.StudyPlanMapper;
import com.englishstudy.backend.request.StudyPlanRequest;
import com.englishstudy.backend.service.BaseService;
import com.englishstudy.backend.service.LogService;
import com.englishstudy.backend.service.StudyPlanService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class StudyPlanServiceImpl extends BaseService implements StudyPlanService {

    private final StudyPlanMapper studyPlanMapper;
    private final LogService logService;

    public StudyPlanServiceImpl(StudyPlanMapper studyPlanMapper, LogService logService) {
        this.studyPlanMapper = studyPlanMapper;
        this.logService = logService;
    }

    @Override
    public List<Map<String, Object>> listMine() {
        requireRole(RoleConstants.STUDENT);
        return studyPlanMapper.selectList(Wrappers.<StudyPlan>lambdaQuery()
                        .eq(StudyPlan::getStudentId, currentUser().getUserId())
                        .orderByDesc(StudyPlan::getCreatedAt))
                .stream()
                .map(this::toView)
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> create(StudyPlanRequest request) {
        requireRole(RoleConstants.STUDENT);
        if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
            throw new BusinessException("计划标题不能为空");
        }
        StudyPlan studyPlan = new StudyPlan();
        studyPlan.setStudentId(currentUser().getUserId());
        studyPlan.setTitle(request.getTitle().trim());
        studyPlan.setTargetContent(trimToNull(request.getTargetContent()));
        studyPlan.setProgress(request.getProgress() == null ? 0 : normalizeProgress(request.getProgress()));
        studyPlan.setStatus(request.getStatus() == null || request.getStatus().trim().isEmpty() ? StatusConstants.TODO : request.getStatus().trim());
        studyPlan.setDueDate(parseDate(request.getDueDate()));
        markForInsert(studyPlan);
        studyPlanMapper.insert(studyPlan);
        logService.save("学习计划", "新增计划", studyPlan.getTitle());
        return toView(studyPlan);
    }

    @Override
    public Map<String, Object> update(Long id, StudyPlanRequest request) {
        requireRole(RoleConstants.STUDENT);
        StudyPlan studyPlan = studyPlanMapper.selectById(id);
        if (studyPlan == null) {
            throw new BusinessException("学习计划不存在");
        }
        if (!studyPlan.getStudentId().equals(currentUser().getUserId())) {
            throw new BusinessException("只能修改自己的学习计划");
        }
        if (request.getTitle() != null && !request.getTitle().trim().isEmpty()) {
            studyPlan.setTitle(request.getTitle().trim());
        }
        studyPlan.setTargetContent(trimToNull(request.getTargetContent()));
        if (request.getProgress() != null) {
            studyPlan.setProgress(normalizeProgress(request.getProgress()));
        }
        if (request.getStatus() != null && !request.getStatus().trim().isEmpty()) {
            studyPlan.setStatus(request.getStatus().trim());
        }
        if (request.getDueDate() != null) {
            studyPlan.setDueDate(parseDate(request.getDueDate()));
        }
        markForUpdate(studyPlan);
        studyPlanMapper.updateById(studyPlan);
        logService.save("学习计划", "更新计划", studyPlan.getTitle());
        return toView(studyPlan);
    }

    @Override
    public void delete(Long id) {
        requireRole(RoleConstants.STUDENT);
        StudyPlan studyPlan = studyPlanMapper.selectById(id);
        if (studyPlan == null) {
            throw new BusinessException("学习计划不存在");
        }
        if (!studyPlan.getStudentId().equals(currentUser().getUserId())) {
            throw new BusinessException("只能删除自己的学习计划");
        }
        studyPlanMapper.deleteById(id);
        logService.save("学习计划", "删除计划", studyPlan.getTitle());
    }

    private Map<String, Object> toView(StudyPlan item) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", item.getId());
        view.put("title", item.getTitle());
        view.put("targetContent", item.getTargetContent());
        view.put("progress", item.getProgress());
        view.put("status", item.getStatus());
        view.put("dueDate", item.getDueDate());
        view.put("createdAt", item.getCreatedAt());
        return view;
    }

    private Integer normalizeProgress(Integer progress) {
        if (progress < 0) {
            return 0;
        }
        return Math.min(progress, 100);
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return LocalDate.parse(value.trim());
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
