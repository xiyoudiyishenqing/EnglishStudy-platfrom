package com.englishstudy.backend.controller;

import com.englishstudy.backend.common.ApiResponse;
import com.englishstudy.backend.request.AdminRequests;
import com.englishstudy.backend.service.AdminService;
import com.englishstudy.backend.service.FeedbackService;
import com.englishstudy.backend.service.NotificationService;
import com.englishstudy.backend.service.ResourceService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;
    private final NotificationService notificationService;
    private final FeedbackService feedbackService;
    private final ResourceService resourceService;

    public AdminController(AdminService adminService,
                           NotificationService notificationService,
                           FeedbackService feedbackService,
                           ResourceService resourceService) {
        this.adminService = adminService;
        this.notificationService = notificationService;
        this.feedbackService = feedbackService;
        this.resourceService = resourceService;
    }

    @GetMapping("/dashboard")
    public ApiResponse<Map<String, Object>> dashboard() {
        return ApiResponse.success(adminService.dashboard());
    }

    @GetMapping("/users")
    public ApiResponse<List<Map<String, Object>>> users() {
        return ApiResponse.success(adminService.listUsers());
    }

    @PostMapping("/users")
    public ApiResponse<Map<String, Object>> createUser(@RequestBody AdminRequests.UserCreateRequest request) {
        return ApiResponse.success("用户创建成功", adminService.createUser(request));
    }

    @PutMapping("/users/{id}")
    public ApiResponse<Map<String, Object>> updateUser(@PathVariable Long id, @RequestBody AdminRequests.UserUpdateRequest request) {
        return ApiResponse.success("用户更新成功", adminService.updateUser(id, request));
    }

    @PutMapping("/users/{id}/reset-password")
    public ApiResponse<Void> resetPassword(@PathVariable Long id) {
        adminService.resetPassword(id);
        return ApiResponse.success("密码已重置为123456", null);
    }

    @GetMapping("/notifications")
    public ApiResponse<List<Map<String, Object>>> notifications() {
        return ApiResponse.success(notificationService.listAll());
    }

    @PostMapping("/notifications")
    public ApiResponse<Map<String, Object>> createNotification(@RequestBody AdminRequests.NotificationCreateRequest request) {
        return ApiResponse.success("公告发布成功", notificationService.create(request));
    }

    @DeleteMapping("/notifications/{id}")
    public ApiResponse<Void> deleteNotification(@PathVariable Long id) {
        notificationService.delete(id);
        return ApiResponse.success("公告删除成功", null);
    }

    @GetMapping("/feedback")
    public ApiResponse<List<Map<String, Object>>> feedback() {
        return ApiResponse.success(feedbackService.listAll());
    }

    @PutMapping("/feedback/{id}/reply")
    public ApiResponse<Map<String, Object>> replyFeedback(@PathVariable Long id, @RequestBody AdminRequests.FeedbackReplyRequest request) {
        return ApiResponse.success("反馈处理成功", feedbackService.reply(id, request));
    }

    @GetMapping("/logs")
    public ApiResponse<List<Map<String, Object>>> logs() {
        return ApiResponse.success(adminService.logs());
    }

    @GetMapping("/configs")
    public ApiResponse<List<Map<String, Object>>> configs() {
        return ApiResponse.success(adminService.configs());
    }

    @PutMapping("/configs/{id}")
    public ApiResponse<Map<String, Object>> updateConfig(@PathVariable Long id, @RequestBody AdminRequests.ConfigUpdateRequest request) {
        return ApiResponse.success("配置更新成功", adminService.updateConfig(id, request));
    }

    @PostMapping("/backup")
    public ApiResponse<Map<String, Object>> backup() {
        return ApiResponse.success("已登记一次备份", adminService.registerBackup());
    }

    @GetMapping("/report")
    public ApiResponse<Map<String, Object>> report() {
        return ApiResponse.success(adminService.report());
    }

    @GetMapping("/resources")
    public ApiResponse<List<Map<String, Object>>> resources(@RequestParam(required = false) String keyword) {
        return ApiResponse.success(resourceService.listAll(keyword));
    }

    @PutMapping("/resources/{id}/audit")
    public ApiResponse<Map<String, Object>> auditResource(@PathVariable Long id,
                                                          @RequestBody AdminRequests.ResourceAuditRequest request) {
        return ApiResponse.success("资源审核状态已更新", resourceService.audit(id, request.getAuditStatus()));
    }

    @PutMapping("/resources/{id}/online-status")
    public ApiResponse<Map<String, Object>> updateResourceOnlineStatus(@PathVariable Long id,
                                                                       @RequestBody AdminRequests.ResourceOnlineStatusRequest request) {
        return ApiResponse.success("资源上下架状态已更新", resourceService.updateOnlineStatus(id, request.getOnlineStatus()));
    }
}
