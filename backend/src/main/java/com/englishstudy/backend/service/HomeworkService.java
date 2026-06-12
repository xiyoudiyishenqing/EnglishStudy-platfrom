package com.englishstudy.backend.service;

import com.englishstudy.backend.request.HomeworkRequests;
import java.util.List;
import java.util.Map;

public interface HomeworkService {

    List<Map<String, Object>> listForStudent();

    List<Map<String, Object>> listForTeacher();

    Map<String, Object> create(HomeworkRequests.HomeworkUpsertRequest request);

    Map<String, Object> update(Long id, HomeworkRequests.HomeworkUpsertRequest request);

    void delete(Long id);

    Map<String, Object> submit(Long homeworkId, HomeworkRequests.SubmissionRequest request);

    List<Map<String, Object>> listSubmissions(Long homeworkId);

    Map<String, Object> gradeSubmission(Long submissionId, HomeworkRequests.GradeRequest request);
}
