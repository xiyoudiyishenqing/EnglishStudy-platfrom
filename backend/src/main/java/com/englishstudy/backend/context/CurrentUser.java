package com.englishstudy.backend.context;

public class CurrentUser {

    private Long userId;
    private String username;
    private String realName;
    private String role;
    private String className;

    public CurrentUser() {
    }

    public CurrentUser(Long userId, String username, String realName, String role) {
        this(userId, username, realName, role, null);
    }

    public CurrentUser(Long userId, String username, String realName, String role, String className) {
        this.userId = userId;
        this.username = username;
        this.realName = realName;
        this.role = role;
        this.className = className;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRealName() {
        return realName;
    }

    public void setRealName(String realName) {
        this.realName = realName;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }
}
