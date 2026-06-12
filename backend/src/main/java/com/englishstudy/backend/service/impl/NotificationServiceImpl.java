package com.englishstudy.backend.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.englishstudy.backend.common.BusinessException;
import com.englishstudy.backend.constant.RoleConstants;
import com.englishstudy.backend.entity.Notification;
import com.englishstudy.backend.mapper.NotificationMapper;
import com.englishstudy.backend.request.AdminRequests;
import com.englishstudy.backend.service.BaseService;
import com.englishstudy.backend.service.LogService;
import com.englishstudy.backend.service.NotificationService;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class NotificationServiceImpl extends BaseService implements NotificationService {

    private final NotificationMapper notificationMapper;
    private final LogService logService;

    public NotificationServiceImpl(NotificationMapper notificationMapper, LogService logService) {
        this.notificationMapper = notificationMapper;
        this.logService = logService;
    }

    @Override
    public List<Map<String, Object>> listForCurrentUser() {
        String role = currentUser().getRole();
        return notificationMapper.selectList(Wrappers.<Notification>lambdaQuery()
                        .orderByDesc(Notification::getCreatedAt))
                .stream()
                .filter(item -> RoleConstants.ALL.equals(item.getTargetRole()) || role.equals(item.getTargetRole()))
                .map(this::toView)
                .collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> listAll() {
        requireRole(RoleConstants.ADMIN);
        return notificationMapper.selectList(Wrappers.<Notification>lambdaQuery()
                        .orderByDesc(Notification::getCreatedAt))
                .stream()
                .map(this::toView)
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> create(AdminRequests.NotificationCreateRequest request) {
        requireRole(RoleConstants.ADMIN);
        if (isBlank(request.getTitle()) || isBlank(request.getContent()) || isBlank(request.getTargetRole())) {
            throw new BusinessException("公告标题、内容和目标角色不能为空");
        }
        Notification notification = new Notification();
        notification.setTitle(request.getTitle().trim());
        notification.setContent(request.getContent().trim());
        notification.setTargetRole(request.getTargetRole().trim());
        notification.setCreatedById(currentUser().getUserId());
        notification.setCreatedByName(currentUser().getRealName());
        markForInsert(notification);
        notificationMapper.insert(notification);
        logService.save("系统公告", "发布公告", notification.getTitle());
        return toView(notification);
    }

    @Override
    public void delete(Long id) {
        requireRole(RoleConstants.ADMIN);
        Notification notification = notificationMapper.selectById(id);
        if (notification == null) {
            throw new BusinessException("公告不存在");
        }
        notificationMapper.deleteById(id);
        logService.save("系统公告", "删除公告", notification.getTitle());
    }

    private Map<String, Object> toView(Notification item) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", item.getId());
        view.put("title", item.getTitle());
        view.put("content", item.getContent());
        view.put("targetRole", item.getTargetRole());
        view.put("createdByName", item.getCreatedByName());
        view.put("createdAt", item.getCreatedAt());
        return view;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
