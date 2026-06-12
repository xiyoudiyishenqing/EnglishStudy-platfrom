package com.englishstudy.backend.service;

import com.englishstudy.backend.request.AdminRequests;
import java.util.List;
import java.util.Map;

public interface AdminService {

    Map<String, Object> dashboard();

    List<Map<String, Object>> listUsers();

    Map<String, Object> createUser(AdminRequests.UserCreateRequest request);

    Map<String, Object> updateUser(Long userId, AdminRequests.UserUpdateRequest request);

    void resetPassword(Long userId);

    List<Map<String, Object>> logs();

    List<Map<String, Object>> configs();

    Map<String, Object> updateConfig(Long id, AdminRequests.ConfigUpdateRequest request);

    Map<String, Object> registerBackup();

    Map<String, Object> report();
}
