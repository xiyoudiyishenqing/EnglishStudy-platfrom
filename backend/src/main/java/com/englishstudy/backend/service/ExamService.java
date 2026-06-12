package com.englishstudy.backend.service;

import com.englishstudy.backend.request.ExamRequests;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface ExamService {

    List<Map<String, Object>> listPublishedExams();

    Map<String, Object> studentExamDetail(Long examId);

    Map<String, Object> saveSession(Long examId, ExamRequests.ExamSessionRequest request);

    Map<String, Object> getSession(Long examId);

    Map<String, Object> submitExam(Long examId, ExamRequests.ExamSubmitRequest request);

    List<Map<String, Object>> listStudentRecords();

    List<Map<String, Object>> listTeacherExams();

    List<Map<String, Object>> listQuestionBank();

    Map<String, Object> createQuestion(ExamRequests.QuestionItem request);

    Map<String, Object> updateQuestion(Long questionId, ExamRequests.QuestionItem request);

    void deleteQuestion(Long questionId);

    Map<String, Object> uploadQuestionAudio(MultipartFile file);

    Resource loadQuestionAudio(Long questionId);

    MediaType loadQuestionAudioMediaType(Long questionId);

    long loadQuestionAudioSize(Long questionId);

    Map<String, Object> createExam(ExamRequests.ExamUpsertRequest request);

    Map<String, Object> updateExam(Long examId, ExamRequests.ExamUpsertRequest request);

    void deleteExam(Long examId);

    List<Map<String, Object>> listTeacherRecords();

    Map<String, Object> giveFeedback(Long recordId, ExamRequests.ExamFeedbackRequest request);
}
