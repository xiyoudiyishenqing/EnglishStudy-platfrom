package com.englishstudy.backend.request;

public class VocabularyRequests {

    public static class WordReviewRequest {
        private Long wordId;

        private Boolean known;

        public Long getWordId() {
            return wordId;
        }

        public void setWordId(Long wordId) {
            this.wordId = wordId;
        }

        public Boolean getKnown() {
            return known;
        }

        public void setKnown(Boolean known) {
            this.known = known;
        }
    }
}
