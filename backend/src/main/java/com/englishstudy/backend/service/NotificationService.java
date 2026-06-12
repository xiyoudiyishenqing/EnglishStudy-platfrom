package com.englishstudy.backend.service;

import com.englishstudy.backend.request.AdminRequests;
import java.util.List;
import java.util.Map;

public interface NotificationService {

    List<Map<String, Object>> listForCurrentUser();

    List<Map<String, Object>> listAll();

    Map<String, Object> create(AdminRequests.NotificationCreateRequest request);

    void delete(Long id);
}
