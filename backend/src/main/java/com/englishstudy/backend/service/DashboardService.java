package com.englishstudy.backend.service;

import java.util.Map;

public interface DashboardService {

    Map<String, Object> studentDashboard();

    Map<String, Object> teacherDashboard();

    Map<String, Object> byRole(String role);
}
