package com.englishstudy.backend.service;

import com.englishstudy.backend.request.AdminRequests;
import com.englishstudy.backend.request.FeedbackRequest;
import java.util.List;
import java.util.Map;

public interface FeedbackService {

    List<Map<String, Object>> listMine();

    Map<String, Object> create(FeedbackRequest request);

    List<Map<String, Object>> listAll();

    Map<String, Object> reply(Long id, AdminRequests.FeedbackReplyRequest request);
}
