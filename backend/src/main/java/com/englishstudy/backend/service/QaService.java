package com.englishstudy.backend.service;

import com.englishstudy.backend.request.QaRequests;
import java.util.List;
import java.util.Map;

public interface QaService {

    List<Map<String, Object>> listStudentQuestions();

    Map<String, Object> createQuestion(QaRequests.QuestionCreateRequest request);

    List<Map<String, Object>> listAllQuestionsForTeacher();

    Map<String, Object> answerQuestion(Long questionId, QaRequests.AnswerCreateRequest request);
}
