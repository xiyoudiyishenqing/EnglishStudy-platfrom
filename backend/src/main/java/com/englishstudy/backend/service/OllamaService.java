package com.englishstudy.backend.service;

import java.util.Map;

public interface OllamaService {

    Map<String, Object> explainWord(String query);

    Map<String, Object> teacherTeachingAdvice();
}
