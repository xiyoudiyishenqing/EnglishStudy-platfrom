package com.englishstudy.backend.controller;

import com.englishstudy.backend.common.ApiResponse;
import com.englishstudy.backend.request.*;
import com.englishstudy.backend.service.*;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/student")
public class StudentController {

    private final DashboardService dashboardService;
    private final ResourceService resourceService;
    private final StudyPlanService studyPlanService;
    private final ExamService examService;
    private final NotificationService notificationService;
    private final FeedbackService feedbackService;
    private final QaService qaService;
    private final OllamaService ollamaService;
    private final HomeworkService homeworkService;
    private final VocabularyService vocabularyService;

    public StudentController(DashboardService dashboardService,
                             ResourceService resourceService,
                             StudyPlanService studyPlanService,
                             ExamService examService,
                             NotificationService notificationService,
                             FeedbackService feedbackService,
                             QaService qaService,
                             OllamaService ollamaService,
                             HomeworkService homeworkService,
                             VocabularyService vocabularyService) {
        this.dashboardService = dashboardService;
        this.resourceService = resourceService;
        this.studyPlanService = studyPlanService;
        this.examService = examService;
        this.notificationService = notificationService;
        this.feedbackService = feedbackService;
        this.qaService = qaService;
        this.ollamaService = ollamaService;
        this.homeworkService = homeworkService;
        this.vocabularyService = vocabularyService;
    }

    @GetMapping("/dashboard")
    public ApiResponse<Map<String, Object>> dashboard() {
        return ApiResponse.success(dashboardService.studentDashboard());
    }

    @GetMapping("/resources")
    public ApiResponse<List<Map<String, Object>>> resources(@RequestParam(required = false) String keyword) {
        return ApiResponse.success(resourceService.listAll(keyword));
    }

    @GetMapping("/resources/{id}/file")
    public ResponseEntity<Resource> resourceFile(@PathVariable Long id) {
        Resource resource = resourceService.loadFileResource(id);
        return ResponseEntity.ok()
                .contentType(resourceService.loadFileMediaType(id))
                .contentLength(resourceService.loadFileSize(id))
                .header(HttpHeaders.CACHE_CONTROL, "no-cache")
                .body(resource);
    }

    @GetMapping("/resources/{id}/download")
    public ResponseEntity<Resource> downloadResource(@PathVariable Long id) {
        return resourceService.downloadFile(id);
    }

    @GetMapping("/plans")
    public ApiResponse<List<Map<String, Object>>> plans() {
        return ApiResponse.success(studyPlanService.listMine());
    }

    @PostMapping("/plans")
    public ApiResponse<Map<String, Object>> createPlan(@RequestBody StudyPlanRequest request) {
        return ApiResponse.success("新增成功", studyPlanService.create(request));
    }

    @PutMapping("/plans/{id}")
    public ApiResponse<Map<String, Object>> updatePlan(@PathVariable Long id, @RequestBody StudyPlanRequest request) {
        return ApiResponse.success("更新成功", studyPlanService.update(id, request));
    }

    @DeleteMapping("/plans/{id}")
    public ApiResponse<Void> deletePlan(@PathVariable Long id) {
        studyPlanService.delete(id);
        return ApiResponse.success("删除成功", null);
    }

    @GetMapping("/exams")
    public ApiResponse<List<Map<String, Object>>> exams() {
        return ApiResponse.success(examService.listPublishedExams());
    }

    @GetMapping("/exams/{id}")
    public ApiResponse<Map<String, Object>> examDetail(@PathVariable Long id) {
        return ApiResponse.success(examService.studentExamDetail(id));
    }

    @GetMapping("/exam-questions/{id}/audio")
    public ResponseEntity<Resource> examQuestionAudio(@PathVariable Long id) {
        Resource resource = examService.loadQuestionAudio(id);
        return ResponseEntity.ok()
                .contentType(examService.loadQuestionAudioMediaType(id))
                .contentLength(examService.loadQuestionAudioSize(id))
                .header(HttpHeaders.CACHE_CONTROL, "no-cache")
                .body(resource);
    }

    @GetMapping("/exams/{id}/session")
    public ApiResponse<Map<String, Object>> examSession(@PathVariable Long id) {
        return ApiResponse.success(examService.getSession(id));
    }

    @PostMapping("/exams/{id}/session")
    public ApiResponse<Map<String, Object>> saveExamSession(@PathVariable Long id, @RequestBody ExamRequests.ExamSessionRequest request) {
        return ApiResponse.success("答题进度已保存", examService.saveSession(id, request));
    }

    @PostMapping("/exams/{id}/submit")
    public ApiResponse<Map<String, Object>> submitExam(@PathVariable Long id, @RequestBody ExamRequests.ExamSubmitRequest request) {
        return ApiResponse.success("交卷成功", examService.submitExam(id, request));
    }

    @GetMapping("/records")
    public ApiResponse<List<Map<String, Object>>> records() {
        return ApiResponse.success(examService.listStudentRecords());
    }

    @GetMapping("/vocabulary")
    public ApiResponse<Map<String, Object>> vocabulary(@RequestParam(required = false) String mode) {
        return ApiResponse.success(vocabularyService.listWords(mode));
    }

    @PostMapping("/vocabulary/review")
    public ApiResponse<Map<String, Object>> reviewVocabulary(@RequestBody VocabularyRequests.WordReviewRequest request) {
        return ApiResponse.success("学习记录已更新", vocabularyService.review(request));
    }

    @DeleteMapping("/vocabulary/records")
    public ApiResponse<Void> resetVocabulary() {
        vocabularyService.resetMine();
        return ApiResponse.success("学习记录已重置", null);
    }

    @GetMapping("/homeworks")
    public ApiResponse<List<Map<String, Object>>> homeworks() {
        return ApiResponse.success(homeworkService.listForStudent());
    }

    @PostMapping("/homeworks/{id}/submit")
    public ApiResponse<Map<String, Object>> submitHomework(@PathVariable Long id,
                                                           @RequestBody HomeworkRequests.SubmissionRequest request) {
        return ApiResponse.success("作业已提交", homeworkService.submit(id, request));
    }

    @GetMapping("/notifications")
    public ApiResponse<List<Map<String, Object>>> notifications() {
        return ApiResponse.success(notificationService.listForCurrentUser());
    }

    @GetMapping("/feedback")
    public ApiResponse<List<Map<String, Object>>> feedbackList() {
        return ApiResponse.success(feedbackService.listMine());
    }

    @PostMapping("/feedback")
    public ApiResponse<Map<String, Object>> createFeedback(@RequestBody FeedbackRequest request) {
        return ApiResponse.success("反馈已提交", feedbackService.create(request));
    }

    @GetMapping("/qa")
    public ApiResponse<List<Map<String, Object>>> qaList() {
        return ApiResponse.success(qaService.listStudentQuestions());
    }

    @PostMapping("/qa")
    public ApiResponse<Map<String, Object>> createQuestion(@RequestBody QaRequests.QuestionCreateRequest request) {
        return ApiResponse.success("问题已提交", qaService.createQuestion(request));
    }

    @PostMapping("/ai-dictionary")
    public ApiResponse<Map<String, Object>> aiDictionary(@RequestBody AiDictionaryRequest request) {
        return ApiResponse.success(ollamaService.explainWord(request.getQuery()));
    }
}
