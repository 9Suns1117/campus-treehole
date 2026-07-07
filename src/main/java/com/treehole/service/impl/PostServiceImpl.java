
package com.treehole.service.impl;
import com.treehole.dao.PostDao;
import com.treehole.pojo.Post;
import com.treehole.pojo.Reply;
import com.treehole.pojo.AiAuditResult;
import com.treehole.service.PostService;

import java.util.Date;
import java.util.ArrayList;
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
    public List<Post> getInteractedPostsByUser(String username) {
        return postDao.getInteractedPostsByUser(username);
    }

    @Override
    public boolean publishPost(Post post) {
        if (!isValidPost(post)) return false;
        post.setAuditStatus(isValidAuditStatus(post.getAuditStatus()) ? post.getAuditStatus() : 0);
        post.setIsDeleted(0);
        return postDao.insertPost(post);
    }

    @Override
    public boolean resubmitPost(Post post) {
        if (!isValidPost(post)) return false;
        if (post.getId() == null || post.getId().trim().isEmpty()) return false;
        if (post.getAuthorUsername() == null || post.getAuthorUsername().trim().isEmpty()) return false;
        if (!isValidAuditStatus(post.getAuditStatus())) return false;

        Post existing = postDao.getPostById(post.getId());
        if (existing == null) return false;
        if (existing.getAuthorUsername() == null || !existing.getAuthorUsername().equals(post.getAuthorUsername())) return false;
        if (existing.getAuditStatus() == null || existing.getAuditStatus() != 2) return false;

        post.setIsDeleted(0);
        return postDao.updatePostForResubmission(post);
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
            postDao.auditPost(postId, 0, "Multiple reports, pending admin review", "system");
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
        if (reply.getParentReplyId() != null && !reply.getParentReplyId().trim().isEmpty()) {
            Reply parentReply = postDao.getReplyById(reply.getParentReplyId());
            if (parentReply == null || parentReply.getAuditStatus() == null || parentReply.getAuditStatus() != 1) return false;
            if (parentReply.getPostId() == null || !parentReply.getPostId().equals(reply.getPostId())) return false;
            if (parentReply.getParentReplyId() != null && !parentReply.getParentReplyId().trim().isEmpty()) return false;
            reply.setParentReplyId(parentReply.getId());
        } else {
            reply.setParentReplyId(null);
        }

        AiAuditResult result = aiCommentAuditService.auditReply(reply.getBody());
        reply.setAuditStatus(result.getStatus());
        reply.setAuditReason(result.getReason());
        reply.setAuditedBy(result.getStatus() == 0 ? null : "AI");
        reply.setAuditedAt(result.getStatus() == 0 ? null : new Date());
        reply.setIsDeleted(0);
        reply.setLikes(0);
        reply.setLikedBy(new ArrayList<String>());
        reply.setHugs(0);
        reply.setHuggedBy(new ArrayList<String>());
        reply.setReports(0);
        reply.setReportedBy(new ArrayList<String>());
        return postDao.insertReply(reply);
    }

    @Override
    public boolean actionReply(String replyId, String action, String username) {
        Reply reply = postDao.getReplyById(replyId);
        if (reply == null || reply.getAuditStatus() == null || reply.getAuditStatus() != 1) return false;
        if (reply.getLikedBy() == null) reply.setLikedBy(new ArrayList<String>());
        if (reply.getHuggedBy() == null) reply.setHuggedBy(new ArrayList<String>());
        if (reply.getReportedBy() == null) reply.setReportedBy(new ArrayList<String>());
        if (reply.getLikes() == null) reply.setLikes(0);
        if (reply.getHugs() == null) reply.setHugs(0);
        if (reply.getReports() == null) reply.setReports(0);

        boolean updated = false;
        if ("like".equals(action)) {
            if (reply.getLikedBy().contains(username)) {
                reply.getLikedBy().remove(username);
                reply.setLikes(Math.max(0, reply.getLikes() - 1));
            } else {
                reply.getLikedBy().add(username);
                reply.setLikes(reply.getLikes() + 1);
            }
            updated = true;
        } else if ("hug".equals(action)) {
            if (reply.getHuggedBy().contains(username)) {
                reply.getHuggedBy().remove(username);
                reply.setHugs(Math.max(0, reply.getHugs() - 1));
            } else {
                reply.getHuggedBy().add(username);
                reply.setHugs(reply.getHugs() + 1);
            }
            updated = true;
        } else if ("report".equals(action)) {
            if (reply.getReportedBy().contains(username)) {
                reply.getReportedBy().remove(username);
                reply.setReports(Math.max(0, reply.getReports() - 1));
            } else {
                reply.getReportedBy().add(username);
                reply.setReports(reply.getReports() + 1);
            }
            updated = true;
        }

        boolean saved = updated && postDao.updateReplyStats(reply);
        if (saved && "report".equals(action) && reply.getReports() != null && reply.getReports() >= 3) {
            postDao.auditReply(replyId, 0, "Multiple reports, pending admin review", "system");
        }
        return saved;
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

    private boolean isValidPost(Post post) {
        if (post == null) return false;
        if (post.getBody() == null || post.getBody().trim().length() < 8) return false;
        if (post.getBody().length() > 520) return false;
        return post.getTitle() == null || post.getTitle().length() <= 36;
    }
}
