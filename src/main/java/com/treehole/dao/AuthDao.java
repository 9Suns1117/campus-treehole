package com.treehole.dao;

import com.treehole.common.DBUtil;
import com.treehole.pojo.TreeholeUser;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class AuthDao {
    private static boolean avatarColumnChecked = false;

    public TreeholeUser getUserByUsername(String username) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        TreeholeUser user = null;
        try {
            conn = DBUtil.getConnection();
            if (conn == null) return null;
            ensureUserColumns(conn);
            String sql = "SELECT * FROM user WHERE username = ? AND is_deleted = 0";
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, username);
            rs = pstmt.executeQuery();
            if (rs.next()) {
                user = mapRowToUser(rs);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            DBUtil.close(conn, pstmt, rs);
        }
        return user;
    }

    public TreeholeUser getUserById(Long userId) {
        if (userId == null) return null;
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        TreeholeUser user = null;
        try {
            conn = DBUtil.getConnection();
            if (conn == null) return null;
            ensureUserColumns(conn);
            String sql = "SELECT * FROM user WHERE user_id = ? AND is_deleted = 0";
            pstmt = conn.prepareStatement(sql);
            pstmt.setLong(1, userId);
            rs = pstmt.executeQuery();
            if (rs.next()) {
                user = mapRowToUser(rs);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            DBUtil.close(conn, pstmt, rs);
        }
        return user;
    }

    public List<TreeholeUser> getAllUsers() {
        List<TreeholeUser> users = new ArrayList<>();
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = DBUtil.getConnection();
            if (conn == null) return users;
            ensureUserColumns(conn);
            String sql = "SELECT * FROM user WHERE is_deleted = 0 ORDER BY role DESC, create_time DESC";
            pstmt = conn.prepareStatement(sql);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                TreeholeUser user = mapRowToUser(rs);
                user.setPassword(null);
                users.add(user);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            DBUtil.close(conn, pstmt, rs);
        }
        return users;
    }

    public boolean insertUser(TreeholeUser user) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = DBUtil.getConnection();
            if (conn == null) return false;
            ensureUserColumns(conn);
            String sql = "INSERT INTO user (username, password, nickname, role, status, is_deleted) VALUES (?, ?, ?, ?, 1, 0)";
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, user.getUsername());
            pstmt.setString(2, user.getPassword());
            pstmt.setString(3, user.getNickname());
            pstmt.setInt(4, user.getRole());
            return pstmt.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            DBUtil.close(conn, pstmt, null);
        }
    }

    public boolean updateUserStatus(Long userId, Integer status) {
        return updateUserFlag(userId, "status", status);
    }

    public boolean updateUserMuteStatus(Long userId, Integer muteStatus) {
        return updateUserMuteStatus(userId, muteStatus, null);
    }

    public boolean updateUserMuteStatus(Long userId, Integer muteStatus, Date muteUntil) {
        if (userId == null || muteStatus == null || (muteStatus != 0 && muteStatus != 1)) return false;
        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = DBUtil.getConnection();
            if (conn == null) return false;
            ensureUserColumns(conn);
            String sql = "UPDATE user SET mute_status = ?, mute_until = ? WHERE user_id = ? AND is_deleted = 0";
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, muteStatus);
            if (muteStatus == 1 && muteUntil != null) {
                pstmt.setTimestamp(2, new Timestamp(muteUntil.getTime()));
            } else {
                pstmt.setNull(2, java.sql.Types.TIMESTAMP);
            }
            pstmt.setLong(3, userId);
            return pstmt.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            DBUtil.close(conn, pstmt, null);
        }
    }

    private boolean updateUserFlag(Long userId, String columnName, Integer value) {
        if (userId == null || value == null || (value != 0 && value != 1)) return false;
        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = DBUtil.getConnection();
            if (conn == null) return false;
            ensureUserColumns(conn);
            String sql = "UPDATE user SET " + columnName + " = ? WHERE user_id = ? AND is_deleted = 0";
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, value);
            pstmt.setLong(2, userId);
            return pstmt.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            DBUtil.close(conn, pstmt, null);
        }
    }

    public boolean updatePassword(Long userId, String passwordHash) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = DBUtil.getConnection();
            String sql = "UPDATE user SET password = ? WHERE user_id = ?";
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, passwordHash);
            pstmt.setLong(2, userId);
            return pstmt.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            DBUtil.close(conn, pstmt, null);
        }
    }

    public boolean updateAvatarUrl(Long userId, String avatarUrl) {
        if (userId == null) return false;
        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = DBUtil.getConnection();
            if (conn == null) return false;
            ensureUserColumns(conn);
            String sql = "UPDATE user SET avatar_url = ? WHERE user_id = ? AND is_deleted = 0";
            pstmt = conn.prepareStatement(sql);
            if (avatarUrl == null || avatarUrl.trim().isEmpty()) {
                pstmt.setNull(1, java.sql.Types.LONGVARCHAR);
            } else {
                pstmt.setString(1, avatarUrl.trim());
            }
            pstmt.setLong(2, userId);
            return pstmt.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            DBUtil.close(conn, pstmt, null);
        }
    }

    public boolean updateProfilePublic(Long userId, Integer profilePublic) {
        if (userId == null) return false;
        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = DBUtil.getConnection();
            if (conn == null) return false;
            ensureUserColumns(conn);
            String sql = "UPDATE user SET profile_public = ? WHERE user_id = ? AND is_deleted = 0";
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, profilePublic != null && profilePublic == 0 ? 0 : 1);
            pstmt.setLong(2, userId);
            return pstmt.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            DBUtil.close(conn, pstmt, null);
        }
    }

    public boolean deleteUser(Long userId) {
        if (userId == null) return false;
        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = DBUtil.getConnection();
            if (conn == null) return false;
            String sql = "UPDATE user SET is_deleted = 1, status = 0 WHERE user_id = ?";
            pstmt = conn.prepareStatement(sql);
            pstmt.setLong(1, userId);
            return pstmt.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            DBUtil.close(conn, pstmt, null);
        }
    }

    private TreeholeUser mapRowToUser(ResultSet rs) throws Exception {
        TreeholeUser user = new TreeholeUser();
        user.setUserId(rs.getLong("user_id"));
        user.setUsername(rs.getString("username"));
        user.setPassword(rs.getString("password"));
        user.setNickname(rs.getString("nickname"));
        user.setRole(rs.getInt("role"));
        user.setAvatarUrl(rs.getString("avatar_url"));
        user.setProfilePublic(getIntIfExists(rs, "profile_public", 1));
        user.setStatus(rs.getInt("status"));
        user.setMuteStatus(getIntIfExists(rs, "mute_status", 0));
        user.setMuteUntil(getTimestampIfExists(rs, "mute_until"));
        user.setCreateTime(rs.getTimestamp("create_time"));
        user.setIsDeleted(rs.getInt("is_deleted"));
        return user;
    }

    private int getIntIfExists(ResultSet rs, String column, int fallback) {
        try {
            return rs.getInt(column);
        } catch (SQLException e) {
            return fallback;
        }
    }

    private Date getTimestampIfExists(ResultSet rs, String column) {
        try {
            return rs.getTimestamp(column);
        } catch (SQLException e) {
            return null;
        }
    }

    private void ensureUserColumns(Connection conn) {
        if (!hasColumn(conn, "user", "avatar_url")) {
            addColumn(conn, "ALTER TABLE user ADD COLUMN avatar_url LONGTEXT");
        } else if (!avatarColumnChecked) {
            avatarColumnChecked = true;
            addColumn(conn, "ALTER TABLE user MODIFY COLUMN avatar_url LONGTEXT");
        }
        if (!hasColumn(conn, "user", "mute_status")) {
            addColumn(conn, "ALTER TABLE user ADD COLUMN mute_status INT DEFAULT 0");
        }
        if (!hasColumn(conn, "user", "mute_until")) {
            addColumn(conn, "ALTER TABLE user ADD COLUMN mute_until DATETIME");
        }
        if (!hasColumn(conn, "user", "profile_public")) {
            addColumn(conn, "ALTER TABLE user ADD COLUMN profile_public INT DEFAULT 1");
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

    private void addColumn(Connection conn, String sql) {
        Statement stmt = null;
        try {
            stmt = conn.createStatement();
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            if (!isDuplicateColumnError(e)) {
                e.printStackTrace();
            }
        } finally {
            try {
                if (stmt != null) stmt.close();
            } catch (SQLException ignored) {
            }
        }
    }

    private boolean isDuplicateColumnError(SQLException e) {
        String message = e.getMessage() == null ? "" : e.getMessage().toLowerCase();
        return e.getErrorCode() == 1060 || message.contains("duplicate column");
    }
}
