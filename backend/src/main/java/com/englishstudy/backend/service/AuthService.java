package com.englishstudy.backend.service;

import com.englishstudy.backend.context.CurrentUser;
import com.englishstudy.backend.dto.AuthDTO;
import com.englishstudy.backend.vo.LoginVO;
import com.englishstudy.backend.vo.UserVO;

public interface AuthService {

    UserVO register(AuthDTO.RegisterDTO request);

    LoginVO login(AuthDTO.LoginDTO request);

    CurrentUser parseToken(String token);

    UserVO currentProfile();

    UserVO updateProfile(AuthDTO.ProfileUpdateDTO request);

    void updatePassword(AuthDTO.PasswordUpdateDTO request);
}
