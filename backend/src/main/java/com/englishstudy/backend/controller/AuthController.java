package com.englishstudy.backend.controller;

import com.englishstudy.backend.common.ApiResponse;
import com.englishstudy.backend.dto.AuthDTO;
import com.englishstudy.backend.service.AuthService;
import com.englishstudy.backend.vo.LoginVO;
import com.englishstudy.backend.vo.UserVO;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ApiResponse<UserVO> register(@RequestBody AuthDTO.RegisterDTO request) {
        return ApiResponse.success("注册成功", authService.register(request));
    }

    @PostMapping("/login")
    public ApiResponse<LoginVO> login(@RequestBody AuthDTO.LoginDTO request) {
        return ApiResponse.success("登录成功", authService.login(request));
    }

    @GetMapping("/me")
    public ApiResponse<UserVO> me() {
        return ApiResponse.success(authService.currentProfile());
    }

    @PutMapping("/profile")
    public ApiResponse<UserVO> updateProfile(@RequestBody AuthDTO.ProfileUpdateDTO request) {
        return ApiResponse.success("资料更新成功", authService.updateProfile(request));
    }

    @PutMapping("/password")
    public ApiResponse<Void> updatePassword(@RequestBody AuthDTO.PasswordUpdateDTO request) {
        authService.updatePassword(request);
        return ApiResponse.success("密码修改成功", null);
    }
}
