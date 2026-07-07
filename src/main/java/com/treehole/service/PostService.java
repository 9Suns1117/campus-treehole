package com.treehole.service;

import com.treehole.pojo.Post;
import com.treehole.pojo.Reply;

import java.util.List;

public interface PostService {
    List<Post> getAllPosts();
    Post getPostById(String id);
    List<Post> getPostsByAuthor(String username);
    List<Post> getInteractedPostsByUser(String username);
    boolean publishPost(Post post);
    boolean resubmitPost(Post post);
    boolean actionPost(String postId, String action, String username);
    boolean addReply(Reply reply);
    boolean actionReply(String replyId, String action, String username);

    /** 对单条待审评论调用AI审核。 */
    boolean aiAuditReply(String id, String auditedBy);

    /** 批量对待审评论调用AI审核，返回处理数量。 */
    int aiAuditPendingReplies(String auditedBy);

    List<Post> getAuditPosts(Integer status);
    List<Reply> getAuditReplies(Integer status);
    boolean auditPost(String id, Integer status, String reason, String auditedBy);
    boolean auditReply(String id, Integer status, String reason, String auditedBy);
    boolean deletePost(String id);
    boolean deleteReply(String id);
    boolean pinPost(String id, Integer pinned, String operatedBy);
}
