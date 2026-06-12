package com.englishstudy.backend.service;

public interface LogService {

    void save(String module, String action, String detail);

    void saveByUser(Long userId, String username, String module, String action, String detail);
}
