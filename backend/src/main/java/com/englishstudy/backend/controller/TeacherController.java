package com.englishstudy.backend.controller;

import com.englishstudy.backend.common.ApiResponse;
import com.englishstudy.backend.request.ExamRequests;
import com.englishstudy.backend.request.HomeworkRequests;
import com.englishstudy.backend.request.QaRequests;
import com.englishstudy.backend.request.ResourceRequest;
import com.englishstudy.backend.service.DashboardService;
import com.englishstudy.backend.service.ExamService;
import com.englishstudy.backend.service.HomeworkService;
import com.englishstudy.backend.service.NotificationService;
import com.englishstudy.backend.service.OllamaService;
import com.englishstudy.backend.service.QaService;
import com.englishstudy.backend.service.ResourceService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/teacher")
public class TeacherController {

    private final DashboardService dashboardService;
    private final ResourceService resourceService;
    private final ExamService examService;
    private final QaService qaService;
    private final HomeworkService homeworkService;
    private final NotificationService notificationService;
    private final OllamaService ollamaService;

    public TeacherController(DashboardService dashboardService,
                             ResourceService resourceService,
                             ExamService examService,
                             QaService qaService,
                             HomeworkService homeworkService,
                             NotificationService notificationService,
                             OllamaService ollamaService) {
        this.dashboardService = dashboardService;
        this.resourceService = resourceService;
        this.examService = examService;
        this.qaService = qaService;
        this.homeworkService = homeworkService;
        this.notificationService = notificationService;
        this.ollamaService = ollamaService;
    }

    @GetMapping("/dashboard")
    public ApiResponse<Map<String, Object>> dashboard() {
        return ApiResponse.success(dashboardService.teacherDashboard());
    }

    @GetMapping("/notifications")
    public ApiResponse<List<Map<String, Object>>> notifications() {
        return ApiResponse.success(notificationService.listForCurrentUser());
    }

    @PostMapping("/ai/advice")
    public ApiResponse<Map<String, Object>> aiTeachingAdvice() {
        return ApiResponse.success("AI教学建议已生成", ollamaService.teacherTeachingAdvice());
    }

    @GetMapping("/resources")
    public ApiResponse<List<Map<String, Object>>> resources() {
        return ApiResponse.success(resourceService.listOwn());
    }

    @PostMapping("/resources")
    public ApiResponse<Map<String, Object>> createResource(@RequestBody ResourceRequest request) {
        return ApiResponse.success("资源发布成功", resourceService.create(request));
    }

    @PostMapping("/resources/upload")
    public ApiResponse<Map<String, Object>> uploadResource(@RequestParam("file") MultipartFile file,
                                                           @RequestParam(value = "title", required = false) String title,
                                                           @RequestParam(value = "description", required = false) String description,
                                                           @RequestParam(value = "type", required = false) String type,
                                                           @RequestParam(value = "visibility", required = false) String visibility,
                                                           @RequestParam(value = "className", required = false) String className) {
        return ApiResponse.success("文件上传成功", resourceService.upload(file, title, description, type, visibility, className));
    }

    @PutMapping("/resources/{id}")
    public ApiResponse<Map<String, Object>> updateResource(@PathVariable Long id, @RequestBody ResourceRequest request) {
        return ApiResponse.success("资源更新成功", resourceService.update(id, request));
    }

    @DeleteMapping("/resources/{id}")
    public ApiResponse<Void> deleteResource(@PathVariable Long id) {
        resourceService.delete(id);
        return ApiResponse.success("资源删除成功", null);
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

    @GetMapping("/exams")
    public ApiResponse<List<Map<String, Object>>> exams() {
        return ApiResponse.success(examService.listTeacherExams());
    }

    @GetMapping("/exam-questions")
    public ApiResponse<List<Map<String, Object>>> examQuestions() {
        return ApiResponse.success(examService.listQuestionBank());
    }

    @PostMapping("/exam-questions")
    public ApiResponse<Map<String, Object>> createExamQuestion(@RequestBody ExamRequests.QuestionItem request) {
        return ApiResponse.success("题目已入库", examService.createQuestion(request));
    }

    @PostMapping("/exam-questions/audio")
    public ApiResponse<Map<String, Object>> uploadExamQuestionAudio(@RequestParam("file") MultipartFile file) {
        return ApiResponse.success("听力音频已上传", examService.uploadQuestionAudio(file));
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

    @PutMapping("/exam-questions/{id}")
    public ApiResponse<Map<String, Object>> updateExamQuestion(@PathVariable Long id, @RequestBody ExamRequests.QuestionItem request) {
        return ApiResponse.success("题目已更新", examService.updateQuestion(id, request));
    }

    @DeleteMapping("/exam-questions/{id}")
    public ApiResponse<Void> deleteExamQuestion(@PathVariable Long id) {
        examService.deleteQuestion(id);
        return ApiResponse.success("题目已删除", null);
    }

    @PostMapping("/exams")
    public ApiResponse<Map<String, Object>> createExam(@RequestBody ExamRequests.ExamUpsertRequest request) {
        return ApiResponse.success("考试创建成功", examService.createExam(request));
    }

    @PutMapping("/exams/{id}")
    public ApiResponse<Map<String, Object>> updateExam(@PathVariable Long id, @RequestBody ExamRequests.ExamUpsertRequest request) {
        return ApiResponse.success("考试更新成功", examService.updateExam(id, request));
    }

    @DeleteMapping("/exams/{id}")
    public ApiResponse<Void> deleteExam(@PathVariable Long id) {
        examService.deleteExam(id);
        return ApiResponse.success("考试删除成功", null);
    }

    @GetMapping("/records")
    public ApiResponse<List<Map<String, Object>>> records() {
        return ApiResponse.success(examService.listTeacherRecords());
    }

    @PutMapping("/records/{id}/feedback")
    public ApiResponse<Map<String, Object>> feedbackRecord(@PathVariable Long id, @RequestBody ExamRequests.ExamFeedbackRequest request) {
        return ApiResponse.success("反馈成功", examService.giveFeedback(id, request));
    }

    @GetMapping("/homeworks")
    public ApiResponse<List<Map<String, Object>>> homeworks() {
        return ApiResponse.success(homeworkService.listForTeacher());
    }

    @PostMapping("/homeworks")
    public ApiResponse<Map<String, Object>> createHomework(@RequestBody HomeworkRequests.HomeworkUpsertRequest request) {
        return ApiResponse.success("作业发布成功", homeworkService.create(request));
    }

    @PutMapping("/homeworks/{id}")
    public ApiResponse<Map<String, Object>> updateHomework(@PathVariable Long id,
                                                           @RequestBody HomeworkRequests.HomeworkUpsertRequest request) {
        return ApiResponse.success("作业更新成功", homeworkService.update(id, request));
    }

    @DeleteMapping("/homeworks/{id}")
    public ApiResponse<Void> deleteHomework(@PathVariable Long id) {
        homeworkService.delete(id);
        return ApiResponse.success("作业删除成功", null);
    }

    @GetMapping("/homeworks/{id}/submissions")
    public ApiResponse<List<Map<String, Object>>> homeworkSubmissions(@PathVariable Long id) {
        return ApiResponse.success(homeworkService.listSubmissions(id));
    }

    @PutMapping("/homework-submissions/{id}/grade")
    public ApiResponse<Map<String, Object>> gradeHomeworkSubmission(@PathVariable Long id,
                                                                    @RequestBody HomeworkRequests.GradeRequest request) {
        return ApiResponse.success("作业批改成功", homeworkService.gradeSubmission(id, request));
    }

    @GetMapping("/qa")
    public ApiResponse<List<Map<String, Object>>> qaList() {
        return ApiResponse.success(qaService.listAllQuestionsForTeacher());
    }

    @PostMapping("/qa/{id}/answer")
    public ApiResponse<Map<String, Object>> answer(@PathVariable Long id, @RequestBody QaRequests.AnswerCreateRequest request) {
        return ApiResponse.success("答疑成功", qaService.answerQuestion(id, request));
    }
}
