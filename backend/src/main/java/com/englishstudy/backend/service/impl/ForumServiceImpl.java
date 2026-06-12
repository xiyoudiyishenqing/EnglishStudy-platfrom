package com.englishstudy.backend.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.englishstudy.backend.common.BusinessException;
import com.englishstudy.backend.constant.RoleConstants;
import com.englishstudy.backend.entity.ForumPost;
import com.englishstudy.backend.entity.ForumReply;
import com.englishstudy.backend.mapper.ForumPostMapper;
import com.englishstudy.backend.mapper.ForumReplyMapper;
import com.englishstudy.backend.request.ForumRequests;
import com.englishstudy.backend.service.BaseService;
import com.englishstudy.backend.service.ForumService;
import com.englishstudy.backend.service.LogService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class ForumServiceImpl extends BaseService implements ForumService {

    private final ForumPostMapper forumPostMapper;
    private final ForumReplyMapper forumReplyMapper;
    private final LogService logService;

    public ForumServiceImpl(ForumPostMapper forumPostMapper,
                            ForumReplyMapper forumReplyMapper,
                            LogService logService) {
        this.forumPostMapper = forumPostMapper;
        this.forumReplyMapper = forumReplyMapper;
        this.logService = logService;
    }

    @Override
    public List<Map<String, Object>> listPosts(String keyword) {
        requireRole(RoleConstants.STUDENT);
        String className = requireCurrentClassName();
        String safeKeyword = keyword == null ? "" : keyword.trim().toLowerCase();
        return forumPostMapper.selectList(Wrappers.<ForumPost>lambdaQuery()
                        .eq(ForumPost::getClassName, className)
                        .orderByDesc(ForumPost::getLastReplyAt)
                        .orderByDesc(ForumPost::getCreatedAt))
                .stream()
                .filter(post -> safeKeyword.isEmpty()
                        || containsIgnoreCase(post.getTitle(), safeKeyword)
                        || containsIgnoreCase(post.getContent(), safeKeyword)
                        || containsIgnoreCase(post.getStudentName(), safeKeyword))
                .map(this::toPostView)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public Map<String, Object> createPost(ForumRequests.PostCreateRequest request) {
        requireRole(RoleConstants.STUDENT);
        if (isBlank(request.getTitle()) || isBlank(request.getContent())) {
            throw new BusinessException("帖子标题和内容不能为空");
        }
        ForumPost post = new ForumPost();
        post.setStudentId(currentUser().getUserId());
        post.setStudentName(currentUser().getRealName());
        post.setClassName(requireCurrentClassName());
        post.setTitle(request.getTitle().trim());
        post.setContent(request.getContent().trim());
        post.setReplyCount(0);
        markForInsert(post);
        forumPostMapper.insert(post);
        post.setLastReplyAt(post.getCreatedAt());
        markForUpdate(post);
        forumPostMapper.updateById(post);
        logService.save("班级论坛", "发布帖子", post.getTitle());
        return toPostView(post);
    }

    @Override
    public Map<String, Object> getPost(Long postId) {
        requireRole(RoleConstants.STUDENT);
        ForumPost post = requireVisiblePost(postId);
        return toPostDetailView(post);
    }

    @Override
    @Transactional
    public void deletePost(Long postId) {
        requireRole(RoleConstants.STUDENT);
        ForumPost post = requireVisiblePost(postId);
        if (!post.getStudentId().equals(currentUser().getUserId())) {
            throw new BusinessException("只能删除自己发布的帖子");
        }
        forumReplyMapper.delete(Wrappers.<ForumReply>lambdaQuery().eq(ForumReply::getPostId, postId));
        forumPostMapper.deleteById(postId);
        logService.save("班级论坛", "删除帖子", post.getTitle());
    }

    @Override
    @Transactional
    public Map<String, Object> createReply(Long postId, ForumRequests.ReplyCreateRequest request) {
        requireRole(RoleConstants.STUDENT);
        if (isBlank(request.getContent())) {
            throw new BusinessException("回复内容不能为空");
        }
        ForumPost post = requireVisiblePost(postId);
        ForumReply reply = new ForumReply();
        reply.setPostId(postId);
        reply.setStudentId(currentUser().getUserId());
        reply.setStudentName(currentUser().getRealName());
        reply.setContent(request.getContent().trim());
        markForInsert(reply);
        forumReplyMapper.insert(reply);
        refreshPostStats(postId);
        logService.save("班级论坛", "发表回复", post.getTitle());
        return toReplyView(reply);
    }

    @Override
    @Transactional
    public void deleteReply(Long replyId) {
        requireRole(RoleConstants.STUDENT);
        ForumReply reply = forumReplyMapper.selectById(replyId);
        if (reply == null) {
            throw new BusinessException("回复不存在");
        }
        ForumPost post = requireVisiblePost(reply.getPostId());
        if (!reply.getStudentId().equals(currentUser().getUserId())) {
            throw new BusinessException("只能删除自己发布的回复");
        }
        forumReplyMapper.deleteById(replyId);
        refreshPostStats(post.getId());
        logService.save("班级论坛", "删除回复", post.getTitle());
    }

    private ForumPost requireVisiblePost(Long postId) {
        ForumPost post = forumPostMapper.selectById(postId);
        if (post == null) {
            throw new BusinessException("帖子不存在");
        }
        if (!sameClassName(post.getClassName(), requireCurrentClassName())) {
            throw new BusinessException("只能访问本班级论坛帖子");
        }
        return post;
    }

    private void refreshPostStats(Long postId) {
        List<ForumReply> replies = forumReplyMapper.selectList(Wrappers.<ForumReply>lambdaQuery()
                .eq(ForumReply::getPostId, postId)
                .orderByAsc(ForumReply::getCreatedAt));
        ForumPost post = forumPostMapper.selectById(postId);
        if (post == null) {
            return;
        }
        post.setReplyCount(replies.size());
        post.setLastReplyAt(replies.stream()
                .map(ForumReply::getCreatedAt)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(null));
        markForUpdate(post);
        forumPostMapper.updateById(post);
    }

    private Map<String, Object> toPostView(ForumPost post) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", post.getId());
        view.put("studentId", post.getStudentId());
        view.put("studentName", post.getStudentName());
        view.put("className", post.getClassName());
        view.put("title", post.getTitle());
        view.put("content", post.getContent());
        view.put("replyCount", post.getReplyCount() == null ? 0 : post.getReplyCount());
        view.put("lastReplyAt", post.getLastReplyAt());
        view.put("createdAt", post.getCreatedAt());
        view.put("updatedAt", post.getUpdatedAt());
        view.put("isMine", post.getStudentId() != null && post.getStudentId().equals(currentUser().getUserId()));
        return view;
    }

    private Map<String, Object> toPostDetailView(ForumPost post) {
        Map<String, Object> view = toPostView(post);
        view.put("replies", forumReplyMapper.selectList(Wrappers.<ForumReply>lambdaQuery()
                        .eq(ForumReply::getPostId, post.getId())
                        .orderByAsc(ForumReply::getCreatedAt))
                .stream()
                .map(this::toReplyView)
                .collect(Collectors.toList()));
        return view;
    }

    private Map<String, Object> toReplyView(ForumReply reply) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", reply.getId());
        view.put("postId", reply.getPostId());
        view.put("studentId", reply.getStudentId());
        view.put("studentName", reply.getStudentName());
        view.put("content", reply.getContent());
        view.put("createdAt", reply.getCreatedAt());
        view.put("updatedAt", reply.getUpdatedAt());
        view.put("isMine", reply.getStudentId() != null && reply.getStudentId().equals(currentUser().getUserId()));
        return view;
    }

    private boolean containsIgnoreCase(String value, String keyword) {
        if (value == null) {
            return false;
        }
        return value.toLowerCase().contains(keyword);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
