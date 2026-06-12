package com.englishstudy.backend.service;

import com.englishstudy.backend.request.StudyPlanRequest;
import java.util.List;
import java.util.Map;

public interface StudyPlanService {

    List<Map<String, Object>> listMine();

    Map<String, Object> create(StudyPlanRequest request);

    Map<String, Object> update(Long id, StudyPlanRequest request);

    void delete(Long id);
}
