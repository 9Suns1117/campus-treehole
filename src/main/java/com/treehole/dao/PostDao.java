package com.treehole.dao;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.treehole.common.DBUtil;
import com.treehole.pojo.Post;
import com.treehole.pojo.Reply;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PostDao {

    /** 前台广场：只展示审核通过且未删除的树洞。 */
    public List<Post> getAllPosts() {
        List<Post> posts = new ArrayList<>();
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = DBUtil.getConnection();
            if (conn == null) return posts;
            boolean hasPinColumn = ensurePostPinColumn(conn);
            ensureReplyInteractionColumns(conn);
            String sql = "SELECT * FROM post WHERE is_deleted = 0 AND audit_status = 1 ORDER BY " + (hasPinColumn ? "is_pinned DESC, " : "") + "created_at DESC";
            pstmt = conn.prepareStatement(sql);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                Post post = mapRowToPost(rs);
                post.setReplies(getRepliesByPostId(post.getId(), conn, false));
                posts.add(post);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            DBUtil.close(conn, pstmt, rs);
        }
        return posts;
    }

    /** 个人空间：展示当前用户自己发布的全部未删除树洞。 */
    public List<Post> getPostsByAuthor(String username) {
        List<Post> posts = new ArrayList<>();
        if (username == null || username.trim().isEmpty()) return posts;

        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = DBUtil.getConnection();
            ensureReplyInteractionColumns(conn);
            String sql = "SELECT * FROM post WHERE is_deleted = 0 AND author_username = ? ORDER BY created_at DESC";
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, username);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                Post post = mapRowToPost(rs);
                post.setReplies(getRepliesByPostId(post.getId(), conn, true));
                posts.add(post);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            DBUtil.close(conn, pstmt, rs);
        }
        return posts;
    }

    /** 个人空间：当前用户点赞或抱抱过的已通过树洞。 */
    public List<Post> getInteractedPostsByUser(String username) {
        List<Post> posts = new ArrayList<>();
        if (username == null || username.trim().isEmpty()) return posts;

        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = DBUtil.getConnection();
            if (conn == null) return posts;
            boolean hasPinColumn = ensurePostPinColumn(conn);
            ensureReplyInteractionColumns(conn);
            String sql = "SELECT * FROM post WHERE is_deleted = 0 AND audit_status = 1 ORDER BY " + (hasPinColumn ? "is_pinned DESC, " : "") + "created_at DESC";
            pstmt = conn.prepareStatement(sql);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                Post post = mapRowToPost(rs);
                boolean liked = post.getLikedBy() != null && post.getLikedBy().contains(username);
                boolean hugged = post.getHuggedBy() != null && post.getHuggedBy().contains(username);
                if (liked || hugged) {
                    post.setReplies(getRepliesByPostId(post.getId(), conn, false));
                    posts.add(post);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            DBUtil.close(conn, pstmt, rs);
        }
        return posts;
    }

    /** 管理员审核台：可看待审、通过、驳回的树洞。status 为 null 表示全部。 */
    public List<Post> getAuditPosts(Integer status) {
        List<Post> posts = new ArrayList<>();
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = DBUtil.getConnection();
            if (conn == null) return posts;
            boolean hasPinColumn = ensurePostPinColumn(conn);
            ensureReplyInteractionColumns(conn);
            String sql = "SELECT * FROM post WHERE is_deleted = 0" + (status == null ? "" : " AND audit_status = ?") + " ORDER BY " + (hasPinColumn ? "is_pinned DESC, " : "") + "created_at DESC";
            pstmt = conn.prepareStatement(sql);
            if (status != null) pstmt.setInt(1, status);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                Post post = mapRowToPost(rs);
                post.setReplies(getRepliesByPostId(post.getId(), conn, true));
                posts.add(post);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            DBUtil.close(conn, pstmt, rs);
        }
        return posts;
    }

    public Post getPostById(String id) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = DBUtil.getConnection();
            ensureReplyInteractionColumns(conn);
            String sql = "SELECT * FROM post WHERE id = ? AND is_deleted = 0";
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, id);
            rs = pstmt.executeQuery();
            if (rs.next()) {
                Post post = mapRowToPost(rs);
                post.setReplies(getRepliesByPostId(post.getId(), conn, true));
                return post;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            DBUtil.close(conn, pstmt, rs);
        }
        return null;
    }

    private List<Reply> getRepliesByPostId(String postId, Connection conn, boolean includeAllAuditStatus) {
        List<Reply> replies = new ArrayList<>();
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            ensureReplyInteractionColumns(conn);
            String sql = "SELECT r.*, p.title AS post_title, p.category AS post_category, parent.alias AS parent_alias FROM reply r " +
                    "LEFT JOIN post p ON r.post_id = p.id " +
                    "LEFT JOIN reply parent ON r.parent_reply_id = parent.id " +
                    "WHERE r.post_id = ? AND r.is_deleted = 0" +
                    (includeAllAuditStatus ? "" : " AND r.audit_status = 1") +
                    " ORDER BY r.created_at ASC";
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, postId);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                replies.add(mapRowToReply(rs));
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (rs != null) rs.close();
                if (pstmt != null) pstmt.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return replies;
    }

    public List<Reply> getAuditReplies(Integer status) {
        List<Reply> replies = new ArrayList<>();
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = DBUtil.getConnection();
            ensureReplyInteractionColumns(conn);
            String sql = "SELECT r.*, p.title AS post_title, p.category AS post_category, parent.alias AS parent_alias FROM reply r " +
                    "LEFT JOIN post p ON r.post_id = p.id " +
                    "LEFT JOIN reply parent ON r.parent_reply_id = parent.id " +
                    "WHERE r.is_deleted = 0" +
                    (status == null ? "" : " AND r.audit_status = ?") +
                    " ORDER BY r.created_at DESC";
            pstmt = conn.prepareStatement(sql);
            if (status != null) pstmt.setInt(1, status);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                replies.add(mapRowToReply(rs));
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            DBUtil.close(conn, pstmt, rs);
        }
        return replies;
    }

    public Reply getReplyById(String id) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = DBUtil.getConnection();
            ensureReplyInteractionColumns(conn);
            String sql = "SELECT r.*, p.title AS post_title, p.category AS post_category, parent.alias AS parent_alias FROM reply r " +
                    "LEFT JOIN post p ON r.post_id = p.id " +
                    "LEFT JOIN reply parent ON r.parent_reply_id = parent.id " +
                    "WHERE r.id = ? AND r.is_deleted = 0";
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, id);
            rs = pstmt.executeQuery();
            if (rs.next()) {
                return mapRowToReply(rs);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            DBUtil.close(conn, pstmt, rs);
        }
        return null;
    }

    private Post mapRowToPost(ResultSet rs) throws Exception {
        Post post = new Post();
        post.setId(rs.getString("id"));
        post.setTitle(rs.getString("title"));
        post.setBody(rs.getString("body"));
        post.setCategory(rs.getString("category"));
        post.setMood(rs.getString("mood"));
        post.setAlias(rs.getString("alias"));
        post.setAuthorUsername(rs.getString("author_username"));
        post.setTags(parseStringList(rs.getString("tags")));
        post.setMedia(parseMediaList(getStringIfExists(rs, "media")));
        post.setLikes(rs.getInt("likes"));
        post.setLikedBy(parseStringList(rs.getString("liked_by")));
        post.setHugs(rs.getInt("hugs"));
        post.setHuggedBy(parseStringList(rs.getString("hugged_by")));
        post.setReports(rs.getInt("reports"));
        post.setReportedBy(parseStringList(rs.getString("reported_by")));
        post.setCreatedAt(rs.getTimestamp("created_at"));
        post.setAuditStatus(rs.getInt("audit_status"));
        post.setAuditReason(rs.getString("audit_reason"));
        post.setAuditedBy(rs.getString("audited_by"));
        post.setAuditedAt(rs.getTimestamp("audited_at"));
        post.setIsDeleted(rs.getInt("is_deleted"));
        post.setIsPinned(getIntIfExists(rs, "is_pinned", 0));
        return post;
    }

    private Reply mapRowToReply(ResultSet rs) throws Exception {
        Reply reply = new Reply();
        reply.setId(rs.getString("id"));
        reply.setPostId(rs.getString("post_id"));
        reply.setParentReplyId(getStringIfExists(rs, "parent_reply_id"));
        reply.setBody(rs.getString("body"));
        reply.setCreatedAt(rs.getTimestamp("created_at"));
        reply.setAuthorUsername(rs.getString("author_username"));
        reply.setAlias(rs.getString("alias"));
        reply.setLikes(getIntIfExists(rs, "likes", 0));
        reply.setLikedBy(parseStringList(getStringIfExists(rs, "liked_by")));
        reply.setHugs(getIntIfExists(rs, "hugs", 0));
        reply.setHuggedBy(parseStringList(getStringIfExists(rs, "hugged_by")));
        reply.setReports(getIntIfExists(rs, "reports", 0));
        reply.setReportedBy(parseStringList(getStringIfExists(rs, "reported_by")));
        reply.setAuditStatus(rs.getInt("audit_status"));
        reply.setAuditReason(rs.getString("audit_reason"));
        reply.setAuditedBy(rs.getString("audited_by"));
        reply.setAuditedAt(rs.getTimestamp("audited_at"));
        reply.setIsDeleted(rs.getInt("is_deleted"));
        reply.setPostTitle(getStringIfExists(rs, "post_title"));
        reply.setPostCategory(getStringIfExists(rs, "post_category"));
        reply.setParentAlias(getStringIfExists(rs, "parent_alias"));
        return reply;
    }

    private String getStringIfExists(ResultSet rs, String column) {
        try {
            return rs.getString(column);
        } catch (SQLException e) {
            return null;
        }
    }

    private int getIntIfExists(ResultSet rs, String column, int fallback) {
        try {
            return rs.getInt(column);
        } catch (SQLException e) {
            return fallback;
        }
    }

    private List<String> parseStringList(String json) {
        if (json == null || json.trim().isEmpty()) return new ArrayList<>();
        List<String> list = JSON.parseObject(json, new TypeReference<List<String>>(){});
        return list == null ? new ArrayList<>() : list;
    }

    private List<Map<String, String>> parseMediaList(String json) {
        if (json == null || json.trim().isEmpty()) return new ArrayList<>();
        List<Map<String, String>> list = JSON.parseObject(json, new TypeReference<List<Map<String, String>>>(){});
        return list == null ? new ArrayList<>() : list;
    }

    public boolean insertPost(Post post) {
        Connection conn = null;
        try {
            conn = DBUtil.getConnection();
            if (conn == null) return false;
            ensurePostMediaColumn(conn);
            return insertPostWithMedia(conn, post);
        } catch (SQLException e) {
            if (isMediaColumnError(e)) {
                try {
                    return insertPostWithoutMedia(conn, post);
                } catch (Exception fallbackException) {
                    fallbackException.printStackTrace();
                }
            } else {
                e.printStackTrace();
            }
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            DBUtil.close(conn, null, null);
        }
    }

    private boolean insertPostWithMedia(Connection conn, Post post) throws SQLException {
        PreparedStatement pstmt = null;
        try {
            String sql = "INSERT INTO post (id, title, body, category, mood, alias, author_username, tags, media, likes, liked_by, hugs, hugged_by, reports, reported_by, created_at, audit_status, audit_reason, audited_by, audited_at, is_deleted) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0)";
            pstmt = conn.prepareStatement(sql);
            bindInsertPost(pstmt, post, true);
            return pstmt.executeUpdate() > 0;
        } finally {
            DBUtil.close(null, pstmt, null);
        }
    }

    private boolean insertPostWithoutMedia(Connection conn, Post post) throws SQLException {
        if (conn == null) return false;
        PreparedStatement pstmt = null;
        try {
            String sql = "INSERT INTO post (id, title, body, category, mood, alias, author_username, tags, likes, liked_by, hugs, hugged_by, reports, reported_by, created_at, audit_status, audit_reason, audited_by, audited_at, is_deleted) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0)";
            pstmt = conn.prepareStatement(sql);
            bindInsertPost(pstmt, post, false);
            return pstmt.executeUpdate() > 0;
        } finally {
            DBUtil.close(null, pstmt, null);
        }
    }

    private void bindInsertPost(PreparedStatement pstmt, Post post, boolean includeMedia) throws SQLException {
        int index = 1;
        pstmt.setString(index++, post.getId());
        pstmt.setString(index++, post.getTitle());
        pstmt.setString(index++, post.getBody());
        pstmt.setString(index++, post.getCategory());
        pstmt.setString(index++, post.getMood());
        pstmt.setString(index++, post.getAlias());
        pstmt.setString(index++, post.getAuthorUsername());
        pstmt.setString(index++, JSON.toJSONString(post.getTags() == null ? new ArrayList<>() : post.getTags()));
        if (includeMedia) {
            pstmt.setString(index++, JSON.toJSONString(post.getMedia() == null ? new ArrayList<>() : post.getMedia()));
        }
        pstmt.setInt(index++, post.getLikes() == null ? 0 : post.getLikes());
        pstmt.setString(index++, JSON.toJSONString(post.getLikedBy() == null ? new ArrayList<>() : post.getLikedBy()));
        pstmt.setInt(index++, post.getHugs() == null ? 0 : post.getHugs());
        pstmt.setString(index++, JSON.toJSONString(post.getHuggedBy() == null ? new ArrayList<>() : post.getHuggedBy()));
        pstmt.setInt(index++, post.getReports() == null ? 0 : post.getReports());
        pstmt.setString(index++, JSON.toJSONString(post.getReportedBy() == null ? new ArrayList<>() : post.getReportedBy()));
        pstmt.setTimestamp(index++, new java.sql.Timestamp(post.getCreatedAt().getTime()));
        pstmt.setInt(index++, post.getAuditStatus() == null ? 0 : post.getAuditStatus());
        bindAuditFields(pstmt, index, post);
    }

    public boolean updatePostForResubmission(Post post) {
        Connection conn = null;
        try {
            conn = DBUtil.getConnection();
            if (conn == null) return false;
            ensurePostMediaColumn(conn);
            ensurePostPinColumn(conn);
            return updatePostForResubmissionWithMedia(conn, post);
        } catch (SQLException e) {
            if (isMediaColumnError(e)) {
                try {
                    return updatePostForResubmissionWithoutMedia(conn, post);
                } catch (Exception fallbackException) {
                    fallbackException.printStackTrace();
                }
            } else {
                e.printStackTrace();
            }
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            DBUtil.close(conn, null, null);
        }
    }

    private boolean updatePostForResubmissionWithMedia(Connection conn, Post post) throws SQLException {
        PreparedStatement pstmt = null;
        try {
            String sql = "UPDATE post SET title = ?, body = ?, category = ?, mood = ?, alias = ?, tags = ?, media = ?, reports = 0, reported_by = ?, created_at = ?, audit_status = ?, audit_reason = ?, audited_by = ?, audited_at = ?, is_pinned = 0 " +
                    "WHERE id = ? AND author_username = ? AND is_deleted = 0";
            pstmt = conn.prepareStatement(sql);
            bindResubmitPost(pstmt, post, true);
            return pstmt.executeUpdate() > 0;
        } finally {
            DBUtil.close(null, pstmt, null);
        }
    }

    private boolean updatePostForResubmissionWithoutMedia(Connection conn, Post post) throws SQLException {
        if (conn == null) return false;
        PreparedStatement pstmt = null;
        try {
            String sql = "UPDATE post SET title = ?, body = ?, category = ?, mood = ?, alias = ?, tags = ?, reports = 0, reported_by = ?, created_at = ?, audit_status = ?, audit_reason = ?, audited_by = ?, audited_at = ?, is_pinned = 0 " +
                    "WHERE id = ? AND author_username = ? AND is_deleted = 0";
            pstmt = conn.prepareStatement(sql);
            bindResubmitPost(pstmt, post, false);
            return pstmt.executeUpdate() > 0;
        } finally {
            DBUtil.close(null, pstmt, null);
        }
    }

    private void bindResubmitPost(PreparedStatement pstmt, Post post, boolean includeMedia) throws SQLException {
        int index = 1;
        pstmt.setString(index++, post.getTitle());
        pstmt.setString(index++, post.getBody());
        pstmt.setString(index++, post.getCategory());
        pstmt.setString(index++, post.getMood());
        pstmt.setString(index++, post.getAlias());
        pstmt.setString(index++, JSON.toJSONString(post.getTags() == null ? new ArrayList<>() : post.getTags()));
        if (includeMedia) {
            pstmt.setString(index++, JSON.toJSONString(post.getMedia() == null ? new ArrayList<>() : post.getMedia()));
        }
        pstmt.setString(index++, JSON.toJSONString(new ArrayList<>()));
        pstmt.setTimestamp(index++, new java.sql.Timestamp(post.getCreatedAt().getTime()));
        pstmt.setInt(index++, post.getAuditStatus() == null ? 0 : post.getAuditStatus());
        bindAuditFields(pstmt, index, post);
        index += 3;
        pstmt.setString(index++, post.getId());
        pstmt.setString(index, post.getAuthorUsername());
    }

    private void bindAuditFields(PreparedStatement pstmt, int index, Post post) throws SQLException {
        if (post.getAuditReason() == null || post.getAuditReason().trim().isEmpty()) {
            pstmt.setNull(index++, Types.VARCHAR);
        } else {
            pstmt.setString(index++, post.getAuditReason().trim());
        }
        if (post.getAuditedBy() == null || post.getAuditedBy().trim().isEmpty()) {
            pstmt.setNull(index++, Types.VARCHAR);
        } else {
            pstmt.setString(index++, post.getAuditedBy().trim());
        }
        if (post.getAuditedAt() == null) {
            pstmt.setNull(index, Types.TIMESTAMP);
        } else {
            pstmt.setTimestamp(index, new java.sql.Timestamp(post.getAuditedAt().getTime()));
        }
    }

    private void ensurePostMediaColumn(Connection conn) {
        if (hasColumn(conn, "post", "media")) return;
        Statement stmt = null;
        try {
            stmt = conn.createStatement();
            stmt.executeUpdate("ALTER TABLE post ADD COLUMN media LONGTEXT");
        } catch (SQLException e) {
            if (!isDuplicateColumnError(e)) {
                e.printStackTrace();
            }
        } finally {
            closeStatement(stmt);
        }
    }

    private boolean ensurePostPinColumn(Connection conn) {
        if (hasColumn(conn, "post", "is_pinned")) return true;
        Statement stmt = null;
        try {
            stmt = conn.createStatement();
            stmt.executeUpdate("ALTER TABLE post ADD COLUMN is_pinned INT DEFAULT 0");
            return true;
        } catch (SQLException e) {
            if (!isDuplicateColumnError(e)) {
                e.printStackTrace();
            }
            return isDuplicateColumnError(e) || hasColumn(conn, "post", "is_pinned");
        } finally {
            closeStatement(stmt);
        }
    }

    private void ensureReplyInteractionColumns(Connection conn) {
        if (conn == null) return;
        ensureColumn(conn, "reply", "parent_reply_id", "VARCHAR(50)");
        ensureColumn(conn, "reply", "likes", "INT DEFAULT 0");
        ensureColumn(conn, "reply", "liked_by", "JSON");
        ensureColumn(conn, "reply", "hugs", "INT DEFAULT 0");
        ensureColumn(conn, "reply", "hugged_by", "JSON");
        ensureColumn(conn, "reply", "reports", "INT DEFAULT 0");
        ensureColumn(conn, "reply", "reported_by", "JSON");
    }

    private void ensureColumn(Connection conn, String tableName, String columnName, String definition) {
        if (hasColumn(conn, tableName, columnName)) return;
        Statement stmt = null;
        try {
            stmt = conn.createStatement();
            stmt.executeUpdate("ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + definition);
        } catch (SQLException e) {
            if (!isDuplicateColumnError(e)) {
                e.printStackTrace();
            }
        } finally {
            closeStatement(stmt);
        }
    }

    private boolean hasColumn(Connection conn, String tableName, String columnName) {
        ResultSet rs = null;
        try {
            rs = conn.getMetaData().getColumns(conn.getCatalog(), null, tableName, columnName);
            if (rs.next()) return true;
            rs.close();
            rs = conn.getMetaData().getColumns(conn.getCatalog(), null, tableName.toUpperCase(), columnName.toUpperCase());
            return rs.next();
        } catch (SQLException e) {
            return false;
        } finally {
            try {
                if (rs != null) rs.close();
            } catch (SQLException ignored) {
            }
        }
    }

    private boolean isDuplicateColumnError(SQLException e) {
        String message = e.getMessage() == null ? "" : e.getMessage().toLowerCase();
        return e.getErrorCode() == 1060 || message.contains("duplicate column");
    }

    private boolean isMediaColumnError(SQLException e) {
        String message = e.getMessage() == null ? "" : e.getMessage().toLowerCase();
        return message.contains("media") || e.getErrorCode() == 1054;
    }

    private void closeStatement(Statement stmt) {
        try {
            if (stmt != null) stmt.close();
        } catch (SQLException ignored) {
        }
    }

    public boolean updatePostStats(Post post) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = DBUtil.getConnection();
            String sql = "UPDATE post SET likes = ?, liked_by = ?, hugs = ?, hugged_by = ?, reports = ?, reported_by = ? WHERE id = ? AND is_deleted = 0";
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, post.getLikes());
            pstmt.setString(2, JSON.toJSONString(post.getLikedBy()));
            pstmt.setInt(3, post.getHugs());
            pstmt.setString(4, JSON.toJSONString(post.getHuggedBy()));
            pstmt.setInt(5, post.getReports());
            pstmt.setString(6, JSON.toJSONString(post.getReportedBy()));
            pstmt.setString(7, post.getId());
            return pstmt.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            DBUtil.close(conn, pstmt, null);
        }
    }

    public boolean pinPost(String id, Integer pinned) {
        if (id == null || id.trim().isEmpty() || pinned == null || (pinned != 0 && pinned != 1)) return false;
        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = DBUtil.getConnection();
            if (conn == null) return false;
            ensurePostPinColumn(conn);
            String sql = "UPDATE post SET is_pinned = ? WHERE id = ? AND is_deleted = 0 AND audit_status = 1";
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, pinned);
            pstmt.setString(2, id);
            return pstmt.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            DBUtil.close(conn, pstmt, null);
        }
    }

    public boolean insertReply(Reply reply) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = DBUtil.getConnection();
            ensureReplyInteractionColumns(conn);
            String sql = "INSERT INTO reply (id, post_id, parent_reply_id, body, created_at, author_username, alias, likes, liked_by, hugs, hugged_by, reports, reported_by, audit_status, audit_reason, audited_by, audited_at, is_deleted) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0)";
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, reply.getId());
            pstmt.setString(2, reply.getPostId());
            if (reply.getParentReplyId() == null || reply.getParentReplyId().trim().isEmpty()) {
                pstmt.setNull(3, Types.VARCHAR);
            } else {
                pstmt.setString(3, reply.getParentReplyId().trim());
            }
            pstmt.setString(4, reply.getBody());
            pstmt.setTimestamp(5, new java.sql.Timestamp(reply.getCreatedAt().getTime()));
            pstmt.setString(6, reply.getAuthorUsername());
            pstmt.setString(7, reply.getAlias());
            pstmt.setInt(8, reply.getLikes() == null ? 0 : reply.getLikes());
            pstmt.setString(9, JSON.toJSONString(reply.getLikedBy() == null ? new ArrayList<>() : reply.getLikedBy()));
            pstmt.setInt(10, reply.getHugs() == null ? 0 : reply.getHugs());
            pstmt.setString(11, JSON.toJSONString(reply.getHuggedBy() == null ? new ArrayList<>() : reply.getHuggedBy()));
            pstmt.setInt(12, reply.getReports() == null ? 0 : reply.getReports());
            pstmt.setString(13, JSON.toJSONString(reply.getReportedBy() == null ? new ArrayList<>() : reply.getReportedBy()));
            pstmt.setInt(14, reply.getAuditStatus() == null ? 0 : reply.getAuditStatus());
            if (reply.getAuditReason() == null || reply.getAuditReason().trim().isEmpty()) {
                pstmt.setNull(15, Types.VARCHAR);
            } else {
                pstmt.setString(15, reply.getAuditReason().trim());
            }
            if (reply.getAuditedBy() == null || reply.getAuditedBy().trim().isEmpty()) {
                pstmt.setNull(16, Types.VARCHAR);
            } else {
                pstmt.setString(16, reply.getAuditedBy().trim());
            }
            if (reply.getAuditedAt() == null) {
                pstmt.setNull(17, Types.TIMESTAMP);
            } else {
                pstmt.setTimestamp(17, new java.sql.Timestamp(reply.getAuditedAt().getTime()));
            }
            return pstmt.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            DBUtil.close(conn, pstmt, null);
        }
    }

    public boolean updateReplyStats(Reply reply) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = DBUtil.getConnection();
            ensureReplyInteractionColumns(conn);
            String sql = "UPDATE reply SET likes = ?, liked_by = ?, hugs = ?, hugged_by = ?, reports = ?, reported_by = ? WHERE id = ? AND is_deleted = 0";
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, reply.getLikes() == null ? 0 : reply.getLikes());
            pstmt.setString(2, JSON.toJSONString(reply.getLikedBy() == null ? new ArrayList<>() : reply.getLikedBy()));
            pstmt.setInt(3, reply.getHugs() == null ? 0 : reply.getHugs());
            pstmt.setString(4, JSON.toJSONString(reply.getHuggedBy() == null ? new ArrayList<>() : reply.getHuggedBy()));
            pstmt.setInt(5, reply.getReports() == null ? 0 : reply.getReports());
            pstmt.setString(6, JSON.toJSONString(reply.getReportedBy() == null ? new ArrayList<>() : reply.getReportedBy()));
            pstmt.setString(7, reply.getId());
            return pstmt.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            DBUtil.close(conn, pstmt, null);
        }
    }

    public boolean auditPost(String id, Integer status, String reason, String auditedBy) {
        return auditContent("post", id, status, reason, auditedBy);
    }

    public boolean auditReply(String id, Integer status, String reason, String auditedBy) {
        return auditContent("reply", id, status, reason, auditedBy);
    }

    private boolean auditContent(String tableName, String id, Integer status, String reason, String auditedBy) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = DBUtil.getConnection();
            String sql = "UPDATE " + tableName + " SET audit_status = ?, audit_reason = ?, audited_by = ?, audited_at = NOW() WHERE id = ? AND is_deleted = 0";
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, status);
            if (reason == null || reason.trim().isEmpty()) {
                pstmt.setNull(2, Types.VARCHAR);
            } else {
                pstmt.setString(2, reason.trim());
            }
            pstmt.setString(3, auditedBy);
            pstmt.setString(4, id);
            return pstmt.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            DBUtil.close(conn, pstmt, null);
        }
    }

    public boolean deletePost(String id) {
        if (id == null || id.trim().isEmpty()) return false;
        Connection conn = null;
        PreparedStatement deleteReplies = null;
        PreparedStatement deletePost = null;
        try {
            conn = DBUtil.getConnection();
            if (conn == null) return false;
            conn.setAutoCommit(false);
            deleteReplies = conn.prepareStatement("DELETE FROM reply WHERE post_id = ?");
            deleteReplies.setString(1, id);
            deleteReplies.executeUpdate();
            deletePost = conn.prepareStatement("DELETE FROM post WHERE id = ?");
            deletePost.setString(1, id);
            boolean success = deletePost.executeUpdate() > 0;
            conn.commit();
            return success;
        } catch (Exception e) {
            try {
                if (conn != null) conn.rollback();
            } catch (SQLException rollbackException) {
                rollbackException.printStackTrace();
            }
            e.printStackTrace();
            return false;
        } finally {
            try {
                if (conn != null) conn.setAutoCommit(true);
            } catch (SQLException ignored) {
            }
            DBUtil.close(null, deleteReplies, null);
            DBUtil.close(conn, deletePost, null);
        }
    }

    public boolean deleteReply(String id) {
        if (id == null || id.trim().isEmpty()) return false;
        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = DBUtil.getConnection();
            String sql = "DELETE FROM reply WHERE id = ?";
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, id);
            return pstmt.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            DBUtil.close(conn, pstmt, null);
        }
    }
}
