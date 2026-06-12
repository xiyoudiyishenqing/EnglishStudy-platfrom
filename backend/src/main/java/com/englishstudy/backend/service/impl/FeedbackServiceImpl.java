package com.englishstudy.backend.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.englishstudy.backend.common.BusinessException;
import com.englishstudy.backend.constant.RoleConstants;
import com.englishstudy.backend.constant.StatusConstants;
import com.englishstudy.backend.entity.UserFeedback;
import com.englishstudy.backend.mapper.UserFeedbackMapper;
import com.englishstudy.backend.request.AdminRequests;
import com.englishstudy.backend.request.FeedbackRequest;
import com.englishstudy.backend.service.BaseService;
import com.englishstudy.backend.service.FeedbackService;
import com.englishstudy.backend.service.LogService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class FeedbackServiceImpl extends BaseService implements FeedbackService {

    private final UserFeedbackMapper userFeedbackMapper;
    private final LogService logService;

    public FeedbackServiceImpl(UserFeedbackMapper userFeedbackMapper, LogService logService) {
        this.userFeedbackMapper = userFeedbackMapper;
        this.logService = logService;
    }

    @Override
    public List<Map<String, Object>> listMine() {
        return userFeedbackMapper.selectList(Wrappers.<UserFeedback>lambdaQuery()
                        .eq(UserFeedback::getUserId, currentUser().getUserId())
                        .orderByDesc(UserFeedback::getCreatedAt))
                .stream()
                .map(this::toView)
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> create(FeedbackRequest request) {
        if (request.getContent() == null || request.getContent().trim().isEmpty()) {
            throw new BusinessException("反馈内容不能为空");
        }
        UserFeedback userFeedback = new UserFeedback();
        userFeedback.setUserId(currentUser().getUserId());
        userFeedback.setUsername(currentUser().getUsername());
        userFeedback.setRole(currentUser().getRole());
        userFeedback.setContent(request.getContent().trim());
        userFeedback.setStatus(StatusConstants.PENDING);
        markForInsert(userFeedback);
        userFeedbackMapper.insert(userFeedback);
        logService.save("意见反馈", "提交反馈", "提交新反馈");
        return toView(userFeedback);
    }

    @Override
    public List<Map<String, Object>> listAll() {
        requireRole(RoleConstants.ADMIN);
        return userFeedbackMapper.selectList(Wrappers.<UserFeedback>lambdaQuery()
                        .orderByDesc(UserFeedback::getCreatedAt))
                .stream()
                .map(this::toView)
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> reply(Long id, AdminRequests.FeedbackReplyRequest request) {
        requireRole(RoleConstants.ADMIN);
        UserFeedback userFeedback = userFeedbackMapper.selectById(id);
        if (userFeedback == null) {
            throw new BusinessException("反馈不存在");
        }
        userFeedback.setReplyContent(request.getReplyContent());
        userFeedback.setStatus(StatusConstants.RESOLVED);
        userFeedback.setHandledAt(LocalDateTime.now());
        markForUpdate(userFeedback);
        userFeedbackMapper.updateById(userFeedback);
        logService.save("意见反馈", "处理反馈", "反馈ID:" + id);
        return toView(userFeedback);
    }

    private Map<String, Object> toView(UserFeedback item) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", item.getId());
        view.put("userId", item.getUserId());
        view.put("username", item.getUsername());
        view.put("role", item.getRole());
        view.put("content", item.getContent());
        view.put("status", item.getStatus());
        view.put("replyContent", item.getReplyContent());
        view.put("handledAt", item.getHandledAt());
        view.put("createdAt", item.getCreatedAt());
        return view;
    }
}
