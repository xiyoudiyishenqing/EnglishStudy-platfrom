package com.englishstudy.backend.service;

import com.englishstudy.backend.request.ForumRequests;

import java.util.List;
import java.util.Map;

public interface ForumService {

    List<Map<String, Object>> listPosts(String keyword);

    Map<String, Object> createPost(ForumRequests.PostCreateRequest request);

    Map<String, Object> getPost(Long postId);

    void deletePost(Long postId);

    Map<String, Object> createReply(Long postId, ForumRequests.ReplyCreateRequest request);

    void deleteReply(Long replyId);
}
