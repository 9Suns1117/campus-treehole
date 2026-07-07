package com.treehole.servlet;

import com.alibaba.fastjson.JSON;
import com.treehole.dao.AuthDao;
import com.treehole.dao.ListenerDao;
import com.treehole.pojo.TreeholeUser;
import com.treehole.service.PostService;
import com.treehole.service.impl.PostServiceImpl;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@WebServlet("/api/admin/*")
public class AdminServlet extends HttpServlet {

    private PostService postService = new PostServiceImpl();
    private AuthDao authDao = new AuthDao();
    private ListenerDao listenerDao = new ListenerDao();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        TreeholeUser admin = currentAdmin(request);
        if (admin == null) {
            writeJson(response, fail("无管理员权限"));
            return;
        }

        String pathInfo = request.getPathInfo();
        Integer status = parseStatus(request.getParameter("status"));
        if ("/posts".equals(pathInfo)) {
            writeJson(response, ok("查询成功", "posts", postService.getAuditPosts(status)));
        } else if ("/replies".equals(pathInfo)) {
            writeJson(response, ok("查询成功", "replies", postService.getAuditReplies(status)));
        } else if ("/users".equals(pathInfo)) {
            writeJson(response, ok("查询成功", "users", safeUsers(authDao.getAllUsers())));
        } else if ("/listeners".equals(pathInfo)) {
            writeJson(response, ok("查询成功", "listeners", listenerDao.getProfilesByStatus(status)));
        } else {
            writeJson(response, fail("接口不存在"));
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        TreeholeUser admin = currentAdmin(request);
        if (admin == null) {
            writeJson(response, fail("无管理员权限"));
            return;
        }

        String pathInfo = request.getPathInfo();
        Map<String, Object> body = parseBody(request);
        String id = getString(body, "id");
        String reason = getString(body, "reason");
        Integer status = getInteger(body, "status");
        Long userId = getLong(body, "userId");
        Integer muted = getInteger(body, "muted");
        Integer durationMinutes = getInteger(body, "durationMinutes");
        Integer pinned = getInteger(body, "pinned");
        Long listenerId = getLong(body, "listenerId");

        boolean success;
        if ("/post/audit".equals(pathInfo)) {
            success = postService.auditPost(id, status, reason, admin.getUsername());
        } else if ("/post/pin".equals(pathInfo)) {
            success = postService.pinPost(id, pinned, admin.getUsername());
        } else if ("/reply/audit".equals(pathInfo)) {
            success = postService.auditReply(id, status, reason, admin.getUsername());
        } else if ("/reply/ai-audit".equals(pathInfo)) {
            success = postService.aiAuditReply(id, admin.getUsername());
        } else if ("/replies/ai-audit-pending".equals(pathInfo)) {
            int count = postService.aiAuditPendingReplies(admin.getUsername());
            Map<String, Object> result = ok("AI审核完成，已处理 " + count + " 条评论");
            result.put("count", count);
            writeJson(response, result);
            return;
        } else if ("/post/delete".equals(pathInfo)) {
            success = postService.deletePost(id);
        } else if ("/reply/delete".equals(pathInfo)) {
            success = postService.deleteReply(id);
        } else if ("/user/status".equals(pathInfo)) {
            success = updateUserStatus(admin, userId, status);
        } else if ("/user/mute".equals(pathInfo)) {
            success = updateUserMuteStatus(admin, userId, muted, durationMinutes);
        } else if ("/user/delete".equals(pathInfo)) {
            success = deleteUser(admin, userId);
        } else if ("/listener/audit".equals(pathInfo)) {
            success = listenerDao.auditApplication(listenerId, status, reason, admin.getUsername());
        } else {
            writeJson(response, fail("接口不存在"));
            return;
        }

        writeJson(response, success ? ok("操作成功") : fail("操作失败"));
    }

    private TreeholeUser currentAdmin(HttpServletRequest request) {
        Object user = request.getSession().getAttribute("loginUser");
        if (!(user instanceof TreeholeUser)) return null;
        TreeholeUser loginUser = (TreeholeUser) user;
        return loginUser.getRole() != null && loginUser.getRole() == 2 ? loginUser : null;
    }

    private Integer parseStatus(String value) {
        if (value == null || value.trim().isEmpty() || "all".equals(value)) return null;
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            return null;
        }
    }

    private String getString(Map<String, Object> map, String key) {
        Object value = map == null ? null : map.get(key);
        return value == null ? null : String.valueOf(value).trim();
    }

    private Integer getInteger(Map<String, Object> map, String key) {
        Object value = map == null ? null : map.get(key);
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).intValue();
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception e) {
            return null;
        }
    }

    private Long getLong(Map<String, Object> map, String key) {
        Object value = map == null ? null : map.get(key);
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).longValue();
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (Exception e) {
            return null;
        }
    }

    private boolean updateUserStatus(TreeholeUser admin, Long userId, Integer status) {
        if (status == null || (status != 0 && status != 1)) return false;
        TreeholeUser target = authDao.getUserById(userId);
        if (target == null || isSelf(admin, target)) return false;
        return authDao.updateUserStatus(userId, status);
    }

    private boolean updateUserMuteStatus(TreeholeUser admin, Long userId, Integer muted, Integer durationMinutes) {
        if (muted == null || (muted != 0 && muted != 1)) return false;
        TreeholeUser target = authDao.getUserById(userId);
        if (target == null || isSelf(admin, target)) return false;
        Date muteUntil = null;
        if (muted == 1 && durationMinutes != null && durationMinutes > 0) {
            muteUntil = new Date(System.currentTimeMillis() + durationMinutes.longValue() * 60L * 1000L);
        }
        return authDao.updateUserMuteStatus(userId, muted, muteUntil);
    }

    private boolean deleteUser(TreeholeUser admin, Long userId) {
        TreeholeUser target = authDao.getUserById(userId);
        if (target == null || isSelf(admin, target)) return false;
        return authDao.deleteUser(userId);
    }

    private boolean isSelf(TreeholeUser admin, TreeholeUser target) {
        return admin != null && target != null && admin.getUserId() != null && admin.getUserId().equals(target.getUserId());
    }

    private List<TreeholeUser> safeUsers(List<TreeholeUser> users) {
        if (users != null) {
            for (TreeholeUser user : users) {
                if (user != null) user.setPassword(null);
            }
        }
        return users;
    }

    private Map<String, Object> ok(String message) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", message);
        return result;
    }

    private Map<String, Object> ok(String message, String key, Object data) {
        Map<String, Object> result = ok(message);
        result.put(key, data);
        return result;
    }

    private Map<String, Object> fail(String message) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        result.put("message", message);
        return result;
    }

    private Map<String, Object> parseBody(HttpServletRequest request) throws IOException {
        String text = readBody(request);
        if (text == null || text.trim().isEmpty()) {
            return new HashMap<>();
        }
        Map<String, Object> map = JSON.parseObject(text, Map.class);
        return map == null ? new HashMap<>() : map;
    }

    private String readBody(HttpServletRequest request) throws IOException {
        StringBuilder sb = new StringBuilder();
        BufferedReader reader = request.getReader();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        return sb.toString();
    }

    private void writeJson(HttpServletResponse response, Object data) throws IOException {
        response.setContentType("application/json;charset=utf-8");
        PrintWriter out = response.getWriter();
        out.write(JSON.toJSONString(data));
        out.flush();
        out.close();
    }
}
