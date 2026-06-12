package com.englishstudy.backend.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.englishstudy.backend.common.BusinessException;
import com.englishstudy.backend.constant.RoleConstants;
import com.englishstudy.backend.constant.StatusConstants;
import com.englishstudy.backend.context.CurrentUser;
import com.englishstudy.backend.context.UserContext;
import com.englishstudy.backend.dto.AuthDTO;
import com.englishstudy.backend.entity.User;
import com.englishstudy.backend.mapper.UserMapper;
import com.englishstudy.backend.security.JwtUtil;
import com.englishstudy.backend.service.AuthService;
import com.englishstudy.backend.service.BaseService;
import com.englishstudy.backend.service.LogService;
import com.englishstudy.backend.util.PasswordUtil;
import com.englishstudy.backend.vo.LoginVO;
import com.englishstudy.backend.vo.UserVO;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl extends BaseService implements AuthService {

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final LogService logService;

    public AuthServiceImpl(UserMapper userMapper, JwtUtil jwtUtil, LogService logService) {
        this.userMapper = userMapper;
        this.jwtUtil = jwtUtil;
        this.logService = logService;
    }

    @Override
    public UserVO register(AuthDTO.RegisterDTO request) {
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
        user.setEmail(trimToNull(request.getEmail()));
        user.setRole(RoleConstants.STUDENT);
        user.setClassName(normalizeClassName(user.getRole(), request.getClassName()));
        user.setStatus(StatusConstants.ACTIVE);
        markForInsert(user);
        userMapper.insert(user);
        logService.saveByUser(user.getId(), user.getUsername(), "认证", "学生注册", "新学生账号注册");
        return UserVO.from(user);
    }

    @Override
    public LoginVO login(AuthDTO.LoginDTO request) {
        User user = userMapper.selectOne(Wrappers.<User>lambdaQuery()
                .eq(User::getUsername, request.getUsername())
                .last("limit 1"));
        if (user == null || !PasswordUtil.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }
        if (!StatusConstants.ACTIVE.equals(user.getStatus())) {
            throw new BusinessException("当前账号不可用，请联系管理员");
        }
        CurrentUser currentUser = new CurrentUser(
                user.getId(),
                user.getUsername(),
                user.getRealName(),
                user.getRole(),
                user.getClassName()
        );
        String token = jwtUtil.generate(currentUser);
        logService.saveByUser(user.getId(), user.getUsername(), "认证", "用户登录", "登录系统");
        return new LoginVO(token, UserVO.from(user), jwtUtil.expireAtMillis());
    }

    @Override
    public CurrentUser parseToken(String token) {
        return jwtUtil.parse(token);
    }

    @Override
    public UserVO currentProfile() {
        return UserVO.from(getCurrentUserEntity());
    }

    @Override
    public UserVO updateProfile(AuthDTO.ProfileUpdateDTO request) {
        User user = getCurrentUserEntity();
        if (!isBlank(request.getRealName())) {
            user.setRealName(request.getRealName().trim());
        }
        user.setEmail(trimToNull(request.getEmail()));
        if (RoleConstants.ADMIN.equals(user.getRole())) {
            user.setClassName(null);
        }
        user.setBio(trimToNull(request.getBio()));
        markForUpdate(user);
        userMapper.updateById(user);
        CurrentUser currentUser = UserContext.get();
        if (currentUser != null) {
            currentUser.setRealName(user.getRealName());
            currentUser.setClassName(user.getClassName());
        }
        logService.save("个人中心", "更新资料", "更新个人资料");
        return UserVO.from(user);
    }

    @Override
    public void updatePassword(AuthDTO.PasswordUpdateDTO request) {
        if (isBlank(request.getOldPassword()) || isBlank(request.getNewPassword())) {
            throw new BusinessException("旧密码和新密码不能为空");
        }
        User user = getCurrentUserEntity();
        if (!PasswordUtil.matches(request.getOldPassword(), user.getPassword())) {
            throw new BusinessException("旧密码不正确");
        }
        user.setPassword(PasswordUtil.encode(request.getNewPassword().trim()));
        markForUpdate(user);
        userMapper.updateById(user);
        logService.save("个人中心", "修改密码", "修改登录密码");
    }

    private User getCurrentUserEntity() {
        User user = userMapper.selectById(currentUser().getUserId());
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return user;
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

    private String normalizeClassName(String role, String className) {
        String normalized = trimToNull(className);
        if (RoleConstants.STUDENT.equals(role) && normalized == null) {
            throw new BusinessException("学生账号必须填写班级");
        }
        return RoleConstants.ADMIN.equals(role) ? null : normalized;
    }
}
