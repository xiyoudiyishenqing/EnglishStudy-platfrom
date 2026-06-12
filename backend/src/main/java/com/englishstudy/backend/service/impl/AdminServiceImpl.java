package com.englishstudy.backend.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.englishstudy.backend.common.BusinessException;
import com.englishstudy.backend.constant.RoleConstants;
import com.englishstudy.backend.constant.StatusConstants;
import com.englishstudy.backend.entity.*;
import com.englishstudy.backend.mapper.*;
import com.englishstudy.backend.request.AdminRequests;
import com.englishstudy.backend.service.AdminService;
import com.englishstudy.backend.service.BaseService;
import com.englishstudy.backend.service.LogService;
import com.englishstudy.backend.util.PasswordUtil;
import com.englishstudy.backend.vo.UserVO;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AdminServiceImpl extends BaseService implements AdminService {

    private final UserMapper userMapper;
    private final ResourceMapper resourceMapper;
    private final ExamMapper examMapper;
    private final ExamRecordMapper examRecordMapper;
    private final UserFeedbackMapper userFeedbackMapper;
    private final QaQuestionMapper qaQuestionMapper;
    private final OperationLogMapper operationLogMapper;
    private final SystemConfigMapper systemConfigMapper;
    private final LogService logService;

    public AdminServiceImpl(UserMapper userMapper,
                            ResourceMapper resourceMapper,
                            ExamMapper examMapper,
                            ExamRecordMapper examRecordMapper,
                            UserFeedbackMapper userFeedbackMapper,
                            QaQuestionMapper qaQuestionMapper,
                            OperationLogMapper operationLogMapper,
                            SystemConfigMapper systemConfigMapper,
                            LogService logService) {
        this.userMapper = userMapper;
        this.resourceMapper = resourceMapper;
        this.examMapper = examMapper;
        this.examRecordMapper = examRecordMapper;
        this.userFeedbackMapper = userFeedbackMapper;
        this.qaQuestionMapper = qaQuestionMapper;
        this.operationLogMapper = operationLogMapper;
        this.systemConfigMapper = systemConfigMapper;
        this.logService = logService;
    }

    @Override
    public Map<String, Object> dashboard() {
        requireRole(RoleConstants.ADMIN);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("userCount", userMapper.selectCount(null));
        result.put("studentCount", countUsersByRole(RoleConstants.STUDENT));
        result.put("teacherCount", countUsersByRole(RoleConstants.TEACHER));
        result.put("resourceCount", resourceMapper.selectCount(null));
        result.put("examCount", examMapper.selectCount(null));
        result.put("examRecordCount", examRecordMapper.selectCount(null));
        result.put("pendingFeedbackCount", userFeedbackMapper.selectCount(Wrappers.<UserFeedback>lambdaQuery()
                .eq(UserFeedback::getStatus, StatusConstants.PENDING)));
        result.put("openQuestionCount", qaQuestionMapper.selectCount(Wrappers.<QaQuestion>lambdaQuery()
                .eq(QaQuestion::getStatus, StatusConstants.OPEN)));
        result.put("recentLogs", operationLogMapper.selectList(Wrappers.<OperationLog>lambdaQuery()
                        .orderByDesc(OperationLog::getCreatedAt)
                        .last("limit 8"))
                .stream()
                .map(this::logView)
                .collect(Collectors.toList()));
        result.put("report", report());
        return result;
    }

    @Override
    public List<Map<String, Object>> listUsers() {
        requireRole(RoleConstants.ADMIN);
        return userMapper.selectList(Wrappers.<User>lambdaQuery().orderByDesc(User::getCreatedAt))
                .stream()
                .map(this::userView)
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> createUser(AdminRequests.UserCreateRequest request) {
        requireRole(RoleConstants.ADMIN);
        if (isBlank(request.getUsername()) || isBlank(request.getPassword()) || isBlank(request.getRealName())) {
            throw new BusinessException("用户名、密码和姓名不能为空");
        }
        User existingUser = userMapper.selectOne(Wrappers.<User>lambdaQuery()
                .eq(User::getUsername, request.getUsername().trim())
                .last("limit 1"));
        if (existingUser != null) {
            throw new BusinessException("用户名已存在");
        }

        User user = new User();
        user.setUsername(request.getUsername().trim());
        user.setPassword(PasswordUtil.encode(request.getPassword().trim()));
        user.setRealName(request.getRealName().trim());
        user.setRole(normalizeRole(request.getRole()));
        user.setStatus(normalizeStatus(request.getStatus()));
        user.setEmail(trimToNull(request.getEmail()));
        user.setClassName(normalizeClassName(user.getRole(), request.getClassName()));
        user.setBio(trimToNull(request.getBio()));
        markForInsert(user);
        userMapper.insert(user);
        logService.save("用户权限", "创建用户", user.getUsername());
        return userView(user);
    }

    @Override
    public Map<String, Object> updateUser(Long userId, AdminRequests.UserUpdateRequest request) {
        requireRole(RoleConstants.ADMIN);
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (request.getRole() != null && !request.getRole().trim().isEmpty()) {
            user.setRole(normalizeRole(request.getRole()));
        }
        if (request.getStatus() != null && !request.getStatus().trim().isEmpty()) {
            user.setStatus(normalizeStatus(request.getStatus()));
        }
        if (request.getRealName() != null && !request.getRealName().trim().isEmpty()) {
            user.setRealName(request.getRealName().trim());
        }
        user.setEmail(trimToNull(request.getEmail()));
        user.setClassName(normalizeClassName(user.getRole(), request.getClassName()));
        markForUpdate(user);
        userMapper.updateById(user);
        logService.save("用户权限", "更新用户", user.getUsername());
        return userView(user);
    }

    @Override
    public void resetPassword(Long userId) {
        requireRole(RoleConstants.ADMIN);
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        user.setPassword(PasswordUtil.encode("123456"));
        markForUpdate(user);
        userMapper.updateById(user);
        logService.save("用户权限", "重置密码", user.getUsername());
    }

    @Override
    public List<Map<String, Object>> logs() {
        requireRole(RoleConstants.ADMIN);
        return operationLogMapper.selectList(Wrappers.<OperationLog>lambdaQuery()
                        .orderByDesc(OperationLog::getCreatedAt)
                        .last("limit 50"))
                .stream()
                .map(this::logView)
                .collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> configs() {
        requireRole(RoleConstants.ADMIN);
        List<SystemConfig> configs = systemConfigMapper.selectList(Wrappers.<SystemConfig>lambdaQuery()
                .orderByDesc(SystemConfig::getCreatedAt));
        if (configs.isEmpty()) {
            initDefaultConfigs();
            configs = systemConfigMapper.selectList(Wrappers.<SystemConfig>lambdaQuery()
                    .orderByDesc(SystemConfig::getCreatedAt));
        }
        return configs.stream().map(this::configView).collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> updateConfig(Long id, AdminRequests.ConfigUpdateRequest request) {
        requireRole(RoleConstants.ADMIN);
        SystemConfig config = systemConfigMapper.selectById(id);
        if (config == null) {
            throw new BusinessException("配置不存在");
        }
        config.setConfigValue(request.getConfigValue());
        markForUpdate(config);
        systemConfigMapper.updateById(config);
        logService.save("系统参数", "更新参数", config.getConfigKey());
        return configView(config);
    }

    @Override
    public Map<String, Object> registerBackup() {
        requireRole(RoleConstants.ADMIN);
        SystemConfig config = systemConfigMapper.selectOne(Wrappers.<SystemConfig>lambdaQuery()
                .eq(SystemConfig::getConfigKey, "last_backup_time")
                .last("limit 1"));
        if (config == null) {
            config = new SystemConfig();
            config.setConfigKey("last_backup_time");
            config.setConfigValue("");
            config.setDescription("最近一次手动备份时间");
            markForInsert(config);
            systemConfigMapper.insert(config);
        }
        config.setConfigValue(LocalDateTime.now().toString());
        markForUpdate(config);
        systemConfigMapper.updateById(config);
        logService.save("系统维护", "登记备份", config.getConfigValue());
        return configView(config);
    }

    @Override
    public Map<String, Object> report() {
        requireRole(RoleConstants.ADMIN);
        Map<String, Object> report = new LinkedHashMap<>();
        List<Map<String, Object>> roleData = new ArrayList<>();
        roleData.add(chartItem("学生", countUsersByRole(RoleConstants.STUDENT)));
        roleData.add(chartItem("教师", countUsersByRole(RoleConstants.TEACHER)));
        roleData.add(chartItem("管理员", countUsersByRole(RoleConstants.ADMIN)));
        report.put("roleDistribution", roleData);
        report.put("resourceCount", resourceMapper.selectCount(null));
        report.put("examCount", examMapper.selectCount(null));
        report.put("recordCount", examRecordMapper.selectCount(null));
        report.put("pendingFeedbackCount", userFeedbackMapper.selectCount(Wrappers.<UserFeedback>lambdaQuery()
                .eq(UserFeedback::getStatus, StatusConstants.PENDING)));
        report.put("answeredQuestionCount", qaQuestionMapper.selectCount(Wrappers.<QaQuestion>lambdaQuery()
                .eq(QaQuestion::getStatus, StatusConstants.ANSWERED)));
        return report;
    }

    private Long countUsersByRole(String role) {
        return userMapper.selectCount(Wrappers.<User>lambdaQuery().eq(User::getRole, role));
    }

    private String normalizeRole(String role) {
        String normalized = trimToNull(role);
        if (normalized == null) {
            return RoleConstants.STUDENT;
        }
        normalized = normalized.toUpperCase();
        if (!RoleConstants.STUDENT.equals(normalized)
                && !RoleConstants.TEACHER.equals(normalized)
                && !RoleConstants.ADMIN.equals(normalized)) {
            throw new BusinessException("用户角色仅支持 STUDENT、TEACHER 或 ADMIN");
        }
        return normalized;
    }

    private String normalizeStatus(String status) {
        String normalized = trimToNull(status);
        if (normalized == null) {
            return StatusConstants.ACTIVE;
        }
        normalized = normalized.toUpperCase();
        if (!StatusConstants.ACTIVE.equals(normalized) && !StatusConstants.DISABLED.equals(normalized)) {
            throw new BusinessException("账号状态仅支持 ACTIVE 或 DISABLED");
        }
        return normalized;
    }

    private String normalizeClassName(String role, String className) {
        String normalized = trimToNull(className);
        if ((RoleConstants.STUDENT.equals(role) || RoleConstants.TEACHER.equals(role)) && normalized == null) {
            throw new BusinessException("学生和教师账号必须分配班级");
        }
        return RoleConstants.ADMIN.equals(role) ? null : normalized;
    }

    private void initDefaultConfigs() {
        insertConfig("open_register", "true", "是否开放学生注册");
        insertConfig("max_plan_count", "20", "单个学生建议学习计划数量");
        insertConfig("last_backup_time", "", "最近一次手动备份时间");
    }

    private void insertConfig(String key, String value, String description) {
        SystemConfig config = new SystemConfig();
        config.setConfigKey(key);
        config.setConfigValue(value);
        config.setDescription(description);
        markForInsert(config);
        systemConfigMapper.insert(config);
    }

    private Map<String, Object> userView(User user) {
        UserVO vo = UserVO.from(user);
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", vo.getId());
        view.put("username", vo.getUsername());
        view.put("realName", vo.getRealName());
        view.put("role", vo.getRole());
        view.put("status", vo.getStatus());
        view.put("email", vo.getEmail());
        view.put("className", vo.getClassName());
        view.put("bio", vo.getBio());
        view.put("createdAt", vo.getCreatedAt());
        return view;
    }

    private Map<String, Object> logView(OperationLog log) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", log.getId());
        row.put("userId", log.getUserId());
        row.put("username", log.getUsername());
        row.put("module", log.getModule());
        row.put("action", log.getAction());
        row.put("detail", log.getDetail());
        row.put("createdAt", log.getCreatedAt());
        return row;
    }

    private Map<String, Object> configView(SystemConfig item) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", item.getId());
        view.put("configKey", item.getConfigKey());
        view.put("configValue", item.getConfigValue());
        view.put("description", item.getDescription());
        view.put("updatedAt", item.getUpdatedAt());
        return view;
    }

    private Map<String, Object> chartItem(String name, Object value) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("name", name);
        item.put("value", value);
        return item;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
