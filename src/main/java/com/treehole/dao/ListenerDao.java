package com.treehole.dao;

import com.treehole.common.DBUtil;
import com.treehole.pojo.ListenerProfile;
import com.treehole.pojo.ListenerRequest;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ListenerDao {

    public void ensureStorage() {
        Connection conn = null;
        try {
            conn = DBUtil.getConnection();
            if (conn != null) ensureTables(conn);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            DBUtil.close(conn, null, null);
        }
    }

    public ListenerProfile getProfileByUsername(String username) {
        if (isBlank(username)) return null;
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = DBUtil.getConnection();
            if (conn == null) return null;
            ensureTables(conn);
            String sql = "SELECT lp.*, u.nickname, u.avatar_url FROM listener_profile lp LEFT JOIN user u ON lp.username = u.username WHERE lp.username = ?";
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, username);
            rs = pstmt.executeQuery();
            return rs.next() ? mapProfile(rs) : null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            DBUtil.close(conn, pstmt, rs);
        }
    }

    public List<ListenerProfile> getApprovedProfiles() {
        return getProfilesByStatus(1);
    }

    public List<ListenerProfile> getProfilesByStatus(Integer status) {
        List<ListenerProfile> profiles = new ArrayList<>();
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = DBUtil.getConnection();
            if (conn == null) return profiles;
            ensureTables(conn);
            String sql = "SELECT lp.*, u.nickname, u.avatar_url FROM listener_profile lp LEFT JOIN user u ON lp.username = u.username" +
                    (status == null ? "" : " WHERE lp.status = ?") +
                    " ORDER BY lp.status ASC, lp.warmth DESC, lp.created_at DESC";
            pstmt = conn.prepareStatement(sql);
            if (status != null) pstmt.setInt(1, status);
            rs = pstmt.executeQuery();
            while (rs.next()) profiles.add(mapProfile(rs));
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            DBUtil.close(conn, pstmt, rs);
        }
        return profiles;
    }

    public boolean upsertApplication(ListenerProfile profile) {
        if (profile == null || isBlank(profile.getUsername())) return false;
        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = DBUtil.getConnection();
            if (conn == null) return false;
            ensureTables(conn);
            String sql = "INSERT INTO listener_profile (username, reason, bio, topics, available_time, status, audit_reason, created_at, updated_at) " +
                    "VALUES (?, ?, ?, ?, ?, 0, NULL, NOW(), NOW()) " +
                    "ON DUPLICATE KEY UPDATE reason = VALUES(reason), bio = VALUES(bio), topics = VALUES(topics), available_time = VALUES(available_time), status = 0, audit_reason = NULL, updated_at = NOW()";
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, profile.getUsername());
            pstmt.setString(2, profile.getReason());
            pstmt.setString(3, profile.getBio());
            pstmt.setString(4, profile.getTopics());
            pstmt.setString(5, profile.getAvailableTime());
            return pstmt.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            DBUtil.close(conn, pstmt, null);
        }
    }

    public boolean auditApplication(Long id, Integer status, String reason, String adminUsername) {
        if (id == null || status == null || (status != 1 && status != 2)) return false;
        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = DBUtil.getConnection();
            if (conn == null) return false;
            ensureTables(conn);
            String sql = "UPDATE listener_profile SET status = ?, audit_reason = ?, audited_by = ?, audited_at = NOW(), updated_at = NOW() WHERE id = ?";
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, status);
            pstmt.setString(2, reason);
            pstmt.setString(3, adminUsername);
            pstmt.setLong(4, id);
            return pstmt.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            DBUtil.close(conn, pstmt, null);
        }
    }

    public boolean createRequest(ListenerRequest request) {
        if (request == null || isBlank(request.getListenerUsername()) || isBlank(request.getRequesterUsername())) return false;
        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = DBUtil.getConnection();
            if (conn == null) return false;
            ensureTables(conn);
            String sql = "INSERT INTO listener_request (listener_username, requester_username, topic, message, response_mode, status, thanks_sent, created_at) VALUES (?, ?, ?, ?, ?, 0, 0, NOW())";
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, request.getListenerUsername());
            pstmt.setString(2, request.getRequesterUsername());
            pstmt.setString(3, request.getTopic());
            pstmt.setString(4, request.getMessage());
            pstmt.setString(5, request.getResponseMode());
            return pstmt.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            DBUtil.close(conn, pstmt, null);
        }
    }

    public List<ListenerRequest> getRequestsForListener(String username) {
        return getRequests("lr.listener_username = ?", username);
    }

    public List<ListenerRequest> getRequestsByRequester(String username) {
        return getRequests("lr.requester_username = ?", username);
    }

    private List<ListenerRequest> getRequests(String where, String username) {
        List<ListenerRequest> requests = new ArrayList<>();
        if (isBlank(username)) return requests;
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = DBUtil.getConnection();
            if (conn == null) return requests;
            ensureTables(conn);
            String sql = "SELECT lr.*, lu.nickname AS listener_nickname, ru.nickname AS requester_nickname " +
                    "FROM listener_request lr " +
                    "LEFT JOIN user lu ON lr.listener_username = lu.username " +
                    "LEFT JOIN user ru ON lr.requester_username = ru.username " +
                    "WHERE " + where + " ORDER BY lr.created_at DESC";
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, username);
            rs = pstmt.executeQuery();
            while (rs.next()) requests.add(mapRequest(rs));
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            DBUtil.close(conn, pstmt, rs);
        }
        return requests;
    }

    public boolean updateRequestStatus(Long id, String listenerUsername, Integer status, String replyText) {
        if (id == null || isBlank(listenerUsername) || status == null) return false;
        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = DBUtil.getConnection();
            if (conn == null) return false;
            ensureTables(conn);
            String timeColumn = status == 1 ? "accepted_at" : status == 3 ? "replied_at" : status == 4 ? "completed_at" : null;
            String sql = "UPDATE listener_request SET status = ?, reply_text = COALESCE(?, reply_text)" +
                    (timeColumn == null ? "" : ", " + timeColumn + " = NOW()") +
                    " WHERE id = ? AND listener_username = ?";
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, status);
            if (replyText == null) pstmt.setNull(2, java.sql.Types.LONGVARCHAR); else pstmt.setString(2, replyText);
            pstmt.setLong(3, id);
            pstmt.setString(4, listenerUsername);
            boolean success = pstmt.executeUpdate() > 0;
            if (success && status == 4) incrementListenCount(conn, listenerUsername);
            return success;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            DBUtil.close(conn, pstmt, null);
        }
    }

    public boolean sendThanks(Long id, String requesterUsername) {
        if (id == null || isBlank(requesterUsername)) return false;
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = DBUtil.getConnection();
            if (conn == null) return false;
            ensureTables(conn);
            String query = "SELECT listener_username FROM listener_request WHERE id = ? AND requester_username = ? AND status = 4 AND thanks_sent = 0";
            pstmt = conn.prepareStatement(query);
            pstmt.setLong(1, id);
            pstmt.setString(2, requesterUsername);
            rs = pstmt.executeQuery();
            if (!rs.next()) return false;
            String listenerUsername = rs.getString("listener_username");
            rs.close();
            pstmt.close();

            pstmt = conn.prepareStatement("UPDATE listener_request SET thanks_sent = 1 WHERE id = ?");
            pstmt.setLong(1, id);
            boolean success = pstmt.executeUpdate() > 0;
            if (success) incrementThanks(conn, listenerUsername);
            return success;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            DBUtil.close(conn, pstmt, rs);
        }
    }

    public List<Map<String, Object>> getMessages(Long requestId, String username) {
        List<Map<String, Object>> messages = new ArrayList<>();
        if (requestId == null || isBlank(username)) return messages;
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = DBUtil.getConnection();
            if (conn == null) return messages;
            ensureTables(conn);
            if (!canAccessRequest(conn, requestId, username)) return messages;
            String sql = "SELECT lm.*, u.nickname AS sender_nickname " +
                    "FROM listener_message lm LEFT JOIN user u ON lm.sender_username = u.username " +
                    "WHERE lm.request_id = ? ORDER BY lm.created_at ASC, lm.id ASC";
            pstmt = conn.prepareStatement(sql);
            pstmt.setLong(1, requestId);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                Map<String, Object> item = new HashMap<>();
                item.put("id", rs.getLong("id"));
                item.put("requestId", rs.getLong("request_id"));
                item.put("senderUsername", rs.getString("sender_username"));
                item.put("senderNickname", rs.getString("sender_nickname"));
                item.put("message", rs.getString("message"));
                item.put("createdAt", toDate(rs.getTimestamp("created_at")));
                messages.add(item);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            DBUtil.close(conn, pstmt, rs);
        }
        return messages;
    }

    public boolean createMessage(Long requestId, String senderUsername, String message) {
        if (requestId == null || isBlank(senderUsername) || isBlank(message)) return false;
        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = DBUtil.getConnection();
            if (conn == null) return false;
            ensureTables(conn);
            if (!canAccessRequest(conn, requestId, senderUsername)) return false;
            String sql = "INSERT INTO listener_message (request_id, sender_username, message, created_at) VALUES (?, ?, ?, NOW())";
            pstmt = conn.prepareStatement(sql);
            pstmt.setLong(1, requestId);
            pstmt.setString(2, senderUsername);
            pstmt.setString(3, message);
            boolean success = pstmt.executeUpdate() > 0;
            if (success) markRequestChatting(conn, requestId);
            return success;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            DBUtil.close(conn, pstmt, null);
        }
    }

    private boolean canAccessRequest(Connection conn, Long requestId, String username) throws SQLException {
        try (PreparedStatement pstmt = conn.prepareStatement("SELECT id FROM listener_request WHERE id = ? AND (listener_username = ? OR requester_username = ?) AND status <> 2")) {
            pstmt.setLong(1, requestId);
            pstmt.setString(2, username);
            pstmt.setString(3, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    private void markRequestChatting(Connection conn, Long requestId) throws SQLException {
        try (PreparedStatement pstmt = conn.prepareStatement("UPDATE listener_request SET status = 3, replied_at = COALESCE(replied_at, NOW()) WHERE id = ? AND status = 1")) {
            pstmt.setLong(1, requestId);
            pstmt.executeUpdate();
        }
    }

    private void incrementListenCount(Connection conn, String username) throws SQLException {
        try (PreparedStatement pstmt = conn.prepareStatement("UPDATE listener_profile SET listen_count = listen_count + 1, warmth = warmth + 2, updated_at = NOW() WHERE username = ?")) {
            pstmt.setString(1, username);
            pstmt.executeUpdate();
        }
    }

    private void incrementThanks(Connection conn, String username) throws SQLException {
        try (PreparedStatement pstmt = conn.prepareStatement("UPDATE listener_profile SET thanks_count = thanks_count + 1, warmth = warmth + 5, updated_at = NOW() WHERE username = ?")) {
            pstmt.setString(1, username);
            pstmt.executeUpdate();
        }
    }

    private ListenerProfile mapProfile(ResultSet rs) throws SQLException {
        ListenerProfile profile = new ListenerProfile();
        profile.setId(rs.getLong("id"));
        profile.setUsername(rs.getString("username"));
        profile.setNickname(rs.getString("nickname"));
        profile.setAvatarUrl(rs.getString("avatar_url"));
        profile.setReason(rs.getString("reason"));
        profile.setBio(rs.getString("bio"));
        profile.setTopics(rs.getString("topics"));
        profile.setAvailableTime(rs.getString("available_time"));
        profile.setStatus(rs.getInt("status"));
        profile.setAuditReason(rs.getString("audit_reason"));
        profile.setAuditedBy(rs.getString("audited_by"));
        profile.setAuditedAt(toDate(rs.getTimestamp("audited_at")));
        profile.setListenCount(rs.getInt("listen_count"));
        profile.setThanksCount(rs.getInt("thanks_count"));
        profile.setWarmth(rs.getInt("warmth"));
        profile.setCreatedAt(toDate(rs.getTimestamp("created_at")));
        profile.setUpdatedAt(toDate(rs.getTimestamp("updated_at")));
        return profile;
    }

    private ListenerRequest mapRequest(ResultSet rs) throws SQLException {
        ListenerRequest request = new ListenerRequest();
        request.setId(rs.getLong("id"));
        request.setListenerUsername(rs.getString("listener_username"));
        request.setListenerNickname(rs.getString("listener_nickname"));
        request.setRequesterUsername(rs.getString("requester_username"));
        request.setRequesterNickname(rs.getString("requester_nickname"));
        request.setTopic(rs.getString("topic"));
        request.setMessage(rs.getString("message"));
        request.setResponseMode(rs.getString("response_mode"));
        request.setStatus(rs.getInt("status"));
        request.setReplyText(rs.getString("reply_text"));
        request.setThanksSent(rs.getInt("thanks_sent"));
        request.setCreatedAt(toDate(rs.getTimestamp("created_at")));
        request.setAcceptedAt(toDate(rs.getTimestamp("accepted_at")));
        request.setRepliedAt(toDate(rs.getTimestamp("replied_at")));
        request.setCompletedAt(toDate(rs.getTimestamp("completed_at")));
        return request;
    }

    private Date toDate(Timestamp timestamp) {
        return timestamp == null ? null : new Date(timestamp.getTime());
    }

    private void ensureTables(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS listener_profile (" +
                    "id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                    "username VARCHAR(50) NOT NULL UNIQUE," +
                    "reason VARCHAR(500)," +
                    "bio VARCHAR(500)," +
                    "topics VARCHAR(255)," +
                    "available_time VARCHAR(255)," +
                    "status INT DEFAULT 0," +
                    "audit_reason VARCHAR(255)," +
                    "audited_by VARCHAR(50)," +
                    "audited_at DATETIME," +
                    "listen_count INT DEFAULT 0," +
                    "thanks_count INT DEFAULT 0," +
                    "warmth INT DEFAULT 0," +
                    "created_at DATETIME DEFAULT CURRENT_TIMESTAMP," +
                    "updated_at DATETIME DEFAULT CURRENT_TIMESTAMP," +
                    "is_deleted INT DEFAULT 0)");
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS listener_request (" +
                    "id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                    "listener_username VARCHAR(50) NOT NULL," +
                    "requester_username VARCHAR(50) NOT NULL," +
                    "topic VARCHAR(120)," +
                    "message TEXT," +
                    "response_mode VARCHAR(50)," +
                    "status INT DEFAULT 0," +
                    "reply_text TEXT," +
                    "thanks_sent INT DEFAULT 0," +
                    "created_at DATETIME DEFAULT CURRENT_TIMESTAMP," +
                    "accepted_at DATETIME," +
                    "replied_at DATETIME," +
                    "completed_at DATETIME)");
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS listener_message (" +
                    "id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                    "request_id BIGINT NOT NULL," +
                    "sender_username VARCHAR(50) NOT NULL," +
                    "message TEXT NOT NULL," +
                    "created_at DATETIME DEFAULT CURRENT_TIMESTAMP," +
                    "INDEX idx_listener_message_request (request_id)," +
                    "INDEX idx_listener_message_sender (sender_username))");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
