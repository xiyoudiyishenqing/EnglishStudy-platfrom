package com.englishstudy.backend.request;

import java.util.List;

public class ExamRequests {

    public static class ExamUpsertRequest {
        private String title;
        private String description;
        private String type;
        private Integer questionCount;
        private Integer durationMinutes;
        private Boolean autoSubmit;
        private Boolean published;

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public Integer getQuestionCount() {
            return questionCount;
        }

        public void setQuestionCount(Integer questionCount) {
            this.questionCount = questionCount;
        }

        public Integer getDurationMinutes() {
            return durationMinutes;
        }

        public void setDurationMinutes(Integer durationMinutes) {
            this.durationMinutes = durationMinutes;
        }

        public Boolean getAutoSubmit() {
            return autoSubmit;
        }

        public void setAutoSubmit(Boolean autoSubmit) {
            this.autoSubmit = autoSubmit;
        }

        public Boolean getPublished() {
            return published;
        }

        public void setPublished(Boolean published) {
            this.published = published;
        }
    }

    public static class QuestionItem {
        private Long id;
        private Long examId;
        private String examType;
        private String questionType;
        private String content;
        private String optionA;
        private String optionB;
        private String optionC;
        private String optionD;
        private String correctAnswer;
        private String analysis;
        private String audioFileName;
        private String audioStoredName;
        private String audioContentType;
        private Long audioFileSize;
        private Integer score;
        private Boolean enabled;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public Long getExamId() {
            return examId;
        }

        public void setExamId(Long examId) {
            this.examId = examId;
        }

        public String getExamType() {
            return examType;
        }

        public void setExamType(String examType) {
            this.examType = examType;
        }

        public String getQuestionType() {
            return questionType;
        }

        public void setQuestionType(String questionType) {
            this.questionType = questionType;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public String getOptionA() {
            return optionA;
        }

        public void setOptionA(String optionA) {
            this.optionA = optionA;
        }

        public String getOptionB() {
            return optionB;
        }

        public void setOptionB(String optionB) {
            this.optionB = optionB;
        }

        public String getOptionC() {
            return optionC;
        }

        public void setOptionC(String optionC) {
            this.optionC = optionC;
        }

        public String getOptionD() {
            return optionD;
        }

        public void setOptionD(String optionD) {
            this.optionD = optionD;
        }

        public String getCorrectAnswer() {
            return correctAnswer;
        }

        public void setCorrectAnswer(String correctAnswer) {
            this.correctAnswer = correctAnswer;
        }

        public String getAnalysis() {
            return analysis;
        }

        public void setAnalysis(String analysis) {
            this.analysis = analysis;
        }

        public String getAudioFileName() {
            return audioFileName;
        }

        public void setAudioFileName(String audioFileName) {
            this.audioFileName = audioFileName;
        }

        public String getAudioStoredName() {
            return audioStoredName;
        }

        public void setAudioStoredName(String audioStoredName) {
            this.audioStoredName = audioStoredName;
        }

        public String getAudioContentType() {
            return audioContentType;
        }

        public void setAudioContentType(String audioContentType) {
            this.audioContentType = audioContentType;
        }

        public Long getAudioFileSize() {
            return audioFileSize;
        }

        public void setAudioFileSize(Long audioFileSize) {
            this.audioFileSize = audioFileSize;
        }

        public Integer getScore() {
            return score;
        }

        public void setScore(Integer score) {
            this.score = score;
        }

        public Boolean getEnabled() {
            return enabled;
        }

        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }
    }

    public static class ExamSubmitRequest {
        private List<AnswerItem> answers;

        public List<AnswerItem> getAnswers() {
            return answers;
        }

        public void setAnswers(List<AnswerItem> answers) {
            this.answers = answers;
        }
    }

    public static class ExamSessionRequest {
        private List<AnswerItem> answers;
        private Integer remainingSeconds;

        public List<AnswerItem> getAnswers() {
            return answers;
        }

        public void setAnswers(List<AnswerItem> answers) {
            this.answers = answers;
        }

        public Integer getRemainingSeconds() {
            return remainingSeconds;
        }

        public void setRemainingSeconds(Integer remainingSeconds) {
            this.remainingSeconds = remainingSeconds;
        }
    }

    public static class AnswerItem {
        private Long questionId;
        private String answer;

        public Long getQuestionId() {
            return questionId;
        }

        public void setQuestionId(Long questionId) {
            this.questionId = questionId;
        }

        public String getAnswer() {
            return answer;
        }

        public void setAnswer(String answer) {
            this.answer = answer;
        }
    }

    public static class ExamFeedbackRequest {
        private String teacherFeedback;

        public String getTeacherFeedback() {
            return teacherFeedback;
        }

        public void setTeacherFeedback(String teacherFeedback) {
            this.teacherFeedback = teacherFeedback;
        }
    }
}
