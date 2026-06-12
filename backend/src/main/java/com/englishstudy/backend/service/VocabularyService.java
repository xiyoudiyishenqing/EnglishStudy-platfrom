package com.englishstudy.backend.service;

import com.englishstudy.backend.request.VocabularyRequests;

import java.util.Map;

public interface VocabularyService {

    Map<String, Object> listWords(String mode);

    Map<String, Object> review(VocabularyRequests.WordReviewRequest request);

    void resetMine();
}
