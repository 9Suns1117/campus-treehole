package com.treehole.service.impl;

import com.treehole.dao.PostDao;
import com.treehole.pojo.Post;
import com.treehole.pojo.Reply;
import com.treehole.pojo.AiAuditResult;
import com.treehole.service.PostService;

import java.util.Date;
import java.util.List;

public class PostServiceImpl implements PostService {

    private PostDao postDao = new PostDao();
    private AiCommentAuditService aiCommentAuditService = new AiCommentAuditService();

    @Override
    public List<Post> getAllPosts() {
        return postDao.getAllPosts();
    }

    @Override
    public Post getPostById(String id) {
        return postDao.getPostById(id);
    }

    @Override
    public List<Post> getPostsByAuthor(String username) {
        return postDao.getPostsByAuthor(username);
    }

    @Override
    public boolean publishPost(Post post) {
        if (post == null) return false;
        if (post.getBody() == null || post.getBody().trim().length() < 8) return false;
        if (post.getBody().length() > 520) return false;
        if (post.getTitle() != null && post.getTitle().length() > 36) return false;
        post.setAuditStatus(post.getAuditStatus() != null && post.getAuditStatus() == 1 ? 1 : 0);
        post.setIsDeleted(0);
        return postDao.insertPost(post);
    }

    @Override
    public boolean actionPost(String postId, String action, String username) {
        Post post = postDao.getPostById(postId);
        if (post == null || post.getAuditStatus() == null || post.getAuditStatus() != 1) return false;

        boolean updated = false;

        if ("like".equals(action)) {
            if (post.getLikedBy().contains(username)) {
                post.getLikedBy().remove(username);
                post.setLikes(Math.max(0, post.getLikes() - 1));
            } else {
                post.getLikedBy().add(username);
                post.setLikes(post.getLikes() + 1);
            }
            updated = true;
        } else if ("hug".equals(action)) {
            if (post.getHuggedBy().contains(username)) {
                post.getHuggedBy().remove(username);
                post.setHugs(Math.max(0, post.getHugs() - 1));
            } else {
                post.getHuggedBy().add(username);
                post.setHugs(post.getHugs() + 1);
            }
            updated = true;
        } else if ("report".equals(action)) {
            if (post.getReportedBy().contains(username)) {
                post.getReportedBy().remove(username);
                post.setReports(Math.max(0, post.getReports() - 1));
            } else {
                post.getReportedBy().add(username);
                post.setReports(post.getReports() + 1);
            }
            updated = true;
        }

        boolean saved = updated && postDao.updatePostStats(post);
        if (saved && "report".equals(action) && post.getReports() != null && post.getReports() >= 3) {
            postDao.auditPost(postId, 0, "多次举报，待管理员复核", "system");
        }
        return saved;
    }

    @Override
    public boolean addReply(Reply reply) {
        if (reply == null) return false;
        if (reply.getBody() == null || reply.getBody().trim().length() < 2) return false;
        if (reply.getBody().length() > 120) return false;
        Post parent = postDao.getPostById(reply.getPostId());
        if (parent == null || parent.getAuditStatus() == null || parent.getAuditStatus() != 1) return false;

        AiAuditResult result = aiCommentAuditService.auditReply(reply.getBody());
        reply.setAuditStatus(result.getStatus());
        reply.setAuditReason(result.getReason());
        reply.setAuditedBy(result.getStatus() == 0 ? null : "AI");
        reply.setAuditedAt(result.getStatus() == 0 ? null : new Date());
        reply.setIsDeleted(0);
        return postDao.insertReply(reply);
    }

    @Override
    public boolean aiAuditReply(String id, String auditedBy) {
        Reply reply = postDao.getReplyById(id);
        if (reply == null || reply.getIsDeleted() == null || reply.getIsDeleted() != 0) return false;
        AiAuditResult result = aiCommentAuditService.auditReply(reply.getBody());
        if (result.getStatus() == 0) {
            return postDao.auditReply(id, 0, result.getReason(), auditedBy == null ? "AI" : auditedBy);
        }
        return postDao.auditReply(id, result.getStatus(), result.getReason(), "AI");
    }

    @Override
    public int aiAuditPendingReplies(String auditedBy) {
        List<Reply> replies = postDao.getAuditReplies(0);
        int count = 0;
        for (Reply reply : replies) {
            if (reply != null && aiAuditReply(reply.getId(), auditedBy)) {
                count++;
            }
        }
        return count;
    }

    @Override
    public List<Post> getAuditPosts(Integer status) {
        return postDao.getAuditPosts(status);
    }

    @Override
    public List<Reply> getAuditReplies(Integer status) {
        return postDao.getAuditReplies(status);
    }

    @Override
    public boolean auditPost(String id, Integer status, String reason, String auditedBy) {
        if (!isValidAuditStatus(status)) return false;
        return postDao.auditPost(id, status, reason, auditedBy);
    }

    @Override
    public boolean auditReply(String id, Integer status, String reason, String auditedBy) {
        if (!isValidAuditStatus(status)) return false;
        return postDao.auditReply(id, status, reason, auditedBy);
    }

    @Override
    public boolean deletePost(String id) {
        return postDao.deletePost(id);
    }

    @Override
    public boolean deleteReply(String id) {
        return postDao.deleteReply(id);
    }

    @Override
    public boolean pinPost(String id, Integer pinned, String operatedBy) {
        if (pinned == null || (pinned != 0 && pinned != 1)) return false;
        return postDao.pinPost(id, pinned);
    }

    private boolean isValidAuditStatus(Integer status) {
        return status != null && (status == 0 || status == 1 || status == 2);
    }
}
