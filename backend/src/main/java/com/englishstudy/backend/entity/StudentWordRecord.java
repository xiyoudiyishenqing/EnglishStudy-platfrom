package com.englishstudy.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.englishstudy.backend.common.BaseEntity;

import java.time.LocalDateTime;

@TableName("student_word_records")
public class StudentWordRecord extends BaseEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long studentId;

    private Long wordId;

    private String status;

    private Integer reviewCount;

    private Integer knownCount;

    private Integer unknownCount;

    private String lastResult;

    private LocalDateTime lastReviewedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getStudentId() {
        return studentId;
    }

    public void setStudentId(Long studentId) {
        this.studentId = studentId;
    }

    public Long getWordId() {
        return wordId;
    }

    public void setWordId(Long wordId) {
        this.wordId = wordId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getReviewCount() {
        return reviewCount;
    }

    public void setReviewCount(Integer reviewCount) {
        this.reviewCount = reviewCount;
    }

    public Integer getKnownCount() {
        return knownCount;
    }

    public void setKnownCount(Integer knownCount) {
        this.knownCount = knownCount;
    }

    public Integer getUnknownCount() {
        return unknownCount;
    }

    public void setUnknownCount(Integer unknownCount) {
        this.unknownCount = unknownCount;
    }

    public String getLastResult() {
        return lastResult;
    }

    public void setLastResult(String lastResult) {
        this.lastResult = lastResult;
    }

    public LocalDateTime getLastReviewedAt() {
        return lastReviewedAt;
    }

    public void setLastReviewedAt(LocalDateTime lastReviewedAt) {
        this.lastReviewedAt = lastReviewedAt;
    }
}
