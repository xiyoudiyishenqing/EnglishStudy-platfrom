package com.englishstudy.backend.vo;

public class LoginVO {

    private String token;
    private UserVO user;
    private Long expireAt;

    public LoginVO() {
    }

    public LoginVO(String token, UserVO user, Long expireAt) {
        this.token = token;
        this.user = user;
        this.expireAt = expireAt;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public UserVO getUser() {
        return user;
    }

    public void setUser(UserVO user) {
        this.user = user;
    }

    public Long getExpireAt() {
        return expireAt;
    }

    public void setExpireAt(Long expireAt) {
        this.expireAt = expireAt;
    }
}
