package com.englishstudy.backend.controller;

import com.englishstudy.backend.common.ApiResponse;
import com.englishstudy.backend.request.ForumRequests;
import com.englishstudy.backend.service.ForumService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/student/forum")
public class ForumController {

    private final ForumService forumService;

    public ForumController(ForumService forumService) {
        this.forumService = forumService;
    }

    @GetMapping("/posts")
    public ApiResponse<List<Map<String, Object>>> posts(@RequestParam(required = false) String keyword) {
        return ApiResponse.success(forumService.listPosts(keyword));
    }

    @PostMapping("/posts")
    public ApiResponse<Map<String, Object>> createPost(@RequestBody ForumRequests.PostCreateRequest request) {
        return ApiResponse.success("帖子已发布", forumService.createPost(request));
    }

    @GetMapping("/posts/{id}")
    public ApiResponse<Map<String, Object>> postDetail(@PathVariable Long id) {
        return ApiResponse.success(forumService.getPost(id));
    }

    @DeleteMapping("/posts/{id}")
    public ApiResponse<Void> deletePost(@PathVariable Long id) {
        forumService.deletePost(id);
        return ApiResponse.success("帖子已删除", null);
    }

    @PostMapping("/posts/{id}/replies")
    public ApiResponse<Map<String, Object>> createReply(@PathVariable Long id, @RequestBody ForumRequests.ReplyCreateRequest request) {
        return ApiResponse.success("回复已发表", forumService.createReply(id, request));
    }

    @DeleteMapping("/replies/{id}")
    public ApiResponse<Void> deleteReply(@PathVariable Long id) {
        forumService.deleteReply(id);
        return ApiResponse.success("回复已删除", null);
    }
}
