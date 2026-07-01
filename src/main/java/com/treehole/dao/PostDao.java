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
            String sql = "SELECT r.*, p.title AS post_title, p.category AS post_category FROM reply r " +
                    "LEFT JOIN post p ON r.post_id = p.id " +
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
            String sql = "SELECT r.*, p.title AS post_title, p.category AS post_category FROM reply r " +
                    "LEFT JOIN post p ON r.post_id = p.id " +
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
            String sql = "SELECT r.*, p.title AS post_title, p.category AS post_category FROM reply r " +
                    "LEFT JOIN post p ON r.post_id = p.id " +
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
        reply.setBody(rs.getString("body"));
        reply.setCreatedAt(rs.getTimestamp("created_at"));
        reply.setAuthorUsername(rs.getString("author_username"));
        reply.setAlias(rs.getString("alias"));
        reply.setAuditStatus(rs.getInt("audit_status"));
        reply.setAuditReason(rs.getString("audit_reason"));
        reply.setAuditedBy(rs.getString("audited_by"));
        reply.setAuditedAt(rs.getTimestamp("audited_at"));
        reply.setIsDeleted(rs.getInt("is_deleted"));
        reply.setPostTitle(getStringIfExists(rs, "post_title"));
        reply.setPostCategory(getStringIfExists(rs, "post_category"));
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
            String sql = "INSERT INTO post (id, title, body, category, mood, alias, author_username, tags, media, likes, liked_by, hugs, hugged_by, reports, reported_by, created_at, audit_status, is_deleted) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0)";
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
            String sql = "INSERT INTO post (id, title, body, category, mood, alias, author_username, tags, likes, liked_by, hugs, hugged_by, reports, reported_by, created_at, audit_status, is_deleted) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0)";
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
        pstmt.setInt(index, post.getAuditStatus() == null ? 0 : post.getAuditStatus());
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
            String sql = "INSERT INTO reply (id, post_id, body, created_at, author_username, alias, audit_status, audit_reason, audited_by, audited_at, is_deleted) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0)";
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, reply.getId());
            pstmt.setString(2, reply.getPostId());
            pstmt.setString(3, reply.getBody());
            pstmt.setTimestamp(4, new java.sql.Timestamp(reply.getCreatedAt().getTime()));
            pstmt.setString(5, reply.getAuthorUsername());
            pstmt.setString(6, reply.getAlias());
            pstmt.setInt(7, reply.getAuditStatus() == null ? 0 : reply.getAuditStatus());
            if (reply.getAuditReason() == null || reply.getAuditReason().trim().isEmpty()) {
                pstmt.setNull(8, Types.VARCHAR);
            } else {
                pstmt.setString(8, reply.getAuditReason().trim());
            }
            if (reply.getAuditedBy() == null || reply.getAuditedBy().trim().isEmpty()) {
                pstmt.setNull(9, Types.VARCHAR);
            } else {
                pstmt.setString(9, reply.getAuditedBy().trim());
            }
            if (reply.getAuditedAt() == null) {
                pstmt.setNull(10, Types.TIMESTAMP);
            } else {
                pstmt.setTimestamp(10, new java.sql.Timestamp(reply.getAuditedAt().getTime()));
            }
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
        return logicalDelete("post", id);
    }

    public boolean deleteReply(String id) {
        return logicalDelete("reply", id);
    }

    private boolean logicalDelete(String tableName, String id) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = DBUtil.getConnection();
            String sql = "UPDATE " + tableName + " SET is_deleted = 1 WHERE id = ?";
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
