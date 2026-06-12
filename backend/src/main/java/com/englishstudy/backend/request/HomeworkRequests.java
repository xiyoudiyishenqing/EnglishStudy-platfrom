package com.englishstudy.backend.request;

public class HomeworkRequests {

    public static class HomeworkUpsertRequest {
        private String title;
        private String description;
        private String className;
        private String dueTime;
        private String status;

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

        public String getClassName() {
            return className;
        }

        public void setClassName(String className) {
            this.className = className;
        }

        public String getDueTime() {
            return dueTime;
        }

        public void setDueTime(String dueTime) {
            this.dueTime = dueTime;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }

    public static class SubmissionRequest {
        private String content;

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }
    }

    public static class GradeRequest {
        private Integer score;
        private String teacherFeedback;

        public Integer getScore() {
            return score;
        }

        public void setScore(Integer score) {
            this.score = score;
        }

        public String getTeacherFeedback() {
            return teacherFeedback;
        }

        public void setTeacherFeedback(String teacherFeedback) {
            this.teacherFeedback = teacherFeedback;
        }
    }
}
