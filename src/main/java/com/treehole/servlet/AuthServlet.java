package com.treehole.servlet;

import com.alibaba.fastjson.JSON;
import com.treehole.pojo.TreeholeUser;
import com.treehole.service.AuthService;
import com.treehole.service.impl.AuthServiceImpl;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

@WebServlet("/auth/*")
public class AuthServlet extends HttpServlet {

    private AuthService authService = new AuthServiceImpl();
    private com.treehole.dao.AuthDao authDao = new com.treehole.dao.AuthDao();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        String pathInfo = request.getPathInfo();
        if ("/current".equals(pathInfo)) {
            current(request, response);
        } else {
            writeJson(response, fail("接口不存在"));
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        String pathInfo = request.getPathInfo();

        if ("/login".equals(pathInfo)) {
            login(request, response);
        } else if ("/register".equals(pathInfo)) {
            register(request, response);
        } else if ("/logout".equals(pathInfo)) {
            logout(request, response);
        } else if ("/avatar".equals(pathInfo)) {
            updateAvatar(request, response);
        } else if ("/profile-visibility".equals(pathInfo)) {
            updateProfileVisibility(request, response);
        } else {
            writeJson(response, fail("接口不存在"));
        }
    }

    private void login(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String username = trim(request.getParameter("username"));
        String password = trim(request.getParameter("password"));
        Integer role = parseRole(request.getParameter("role"));

        if (username.isEmpty() || password.isEmpty()) {
            writeJson(response, fail("用户名和密码不能为空"));
            return;
        }
        if (role == null || (role != 1 && role != 2)) {
            writeJson(response, fail("登录入口不正确"));
            return;
        }

        TreeholeUser user = authService.login(username, password, role);
        if (user != null) {
            request.getSession(true).setAttribute("loginUser", user);
            Map<String, Object> result = ok("登录成功");
            result.put("user", safeUser(user));
            writeJson(response, result);
        } else {
            writeJson(response, fail("用户名、密码或入口类型错误"));
        }
    }

    private void register(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String username = trim(request.getParameter("username"));
        String nickname = trim(request.getParameter("nickname"));
        String password = trim(request.getParameter("password"));

        if (nickname.isEmpty()) {
            writeJson(response, fail("昵称不能为空"));
            return;
        }
        if (!username.matches("^[A-Za-z0-9_]{3,20}$")) {
            writeJson(response, fail("用户名需为 3-20 位字母、数字或下划线"));
            return;
        }
        if (password.length() < 6 || password.length() > 32) {
            writeJson(response, fail("密码长度需为 6-32 位"));
            return;
        }
        if (nickname.length() > 12) {
            writeJson(response, fail("昵称最多 12 个字"));
            return;
        }

        boolean success = authService.register(username, nickname, password);
        writeJson(response, success ? ok("注册成功，请登录") : fail("用户名已存在"));
    }

    private void current(HttpServletRequest request, HttpServletResponse response) throws IOException {
        TreeholeUser user = (TreeholeUser) request.getSession().getAttribute("loginUser");
        Map<String, Object> result = new HashMap<>();
        result.put("success", user != null);
        if (user != null) {
            result.put("user", safeUser(user));
        }
        writeJson(response, result);
    }

    private void logout(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);
        if (session != null) session.invalidate();
        writeJson(response, ok("已退出登录"));
    }

    private void updateAvatar(HttpServletRequest request, HttpServletResponse response) throws IOException {
        TreeholeUser user = (TreeholeUser) request.getSession().getAttribute("loginUser");
        if (user == null) {
            writeJson(response, fail("请先登录后再更换头像"));
            return;
        }

        Map<String, Object> body = JSON.parseObject(readBody(request), Map.class);
        Object avatarValue = body == null ? null : body.get("avatarUrl");
        String avatarUrl = avatarValue == null ? "" : trim(String.valueOf(avatarValue));
        if (!avatarUrl.isEmpty() && !avatarUrl.startsWith("data:image/")) {
            writeJson(response, fail("头像只支持图片文件"));
            return;
        }
        if (avatarUrl.length() > 1500 * 1024) {
            writeJson(response, fail("头像图片不能超过 1.5MB"));
            return;
        }

        boolean success = authDao.updateAvatarUrl(user.getUserId(), avatarUrl);
        if (success) {
            user.setAvatarUrl(avatarUrl.isEmpty() ? null : avatarUrl);
            request.getSession(true).setAttribute("loginUser", user);
            Map<String, Object> result = ok("头像已更新");
            result.put("user", safeUser(user));
            writeJson(response, result);
        } else {
            writeJson(response, fail("头像更新失败"));
        }
    }

    private void updateProfileVisibility(HttpServletRequest request, HttpServletResponse response) throws IOException {
        TreeholeUser user = (TreeholeUser) request.getSession().getAttribute("loginUser");
        if (user == null) {
            writeJson(response, fail("请先登录后再修改个人主页设置"));
            return;
        }

        Map<String, Object> body = JSON.parseObject(readBody(request), Map.class);
        Object value = body == null ? null : body.get("profilePublic");
        int profilePublic = Boolean.FALSE.equals(value) || "0".equals(String.valueOf(value)) || "false".equalsIgnoreCase(String.valueOf(value)) ? 0 : 1;
        boolean success = authDao.updateProfilePublic(user.getUserId(), profilePublic);
        if (success) {
            user.setProfilePublic(profilePublic);
            request.getSession(true).setAttribute("loginUser", user);
            Map<String, Object> result = ok(profilePublic == 1 ? "个人主页已公开" : "个人主页已设为不公开");
            result.put("user", safeUser(user));
            writeJson(response, result);
        } else {
            writeJson(response, fail("个人主页设置更新失败"));
        }
    }

    private Integer parseRole(String roleText) {
        try {
            return Integer.parseInt(roleText);
        } catch (Exception e) {
            return null;
        }
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private String readBody(HttpServletRequest request) throws IOException {
        StringBuilder sb = new StringBuilder();
        java.io.BufferedReader reader = request.getReader();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        return sb.toString();
    }

    private Map<String, Object> safeUser(TreeholeUser user) {
        Map<String, Object> map = new HashMap<>();
        map.put("userId", user.getUserId());
        map.put("username", user.getUsername());
        map.put("nickname", user.getNickname());
        map.put("role", user.getRole());
        map.put("avatarUrl", user.getAvatarUrl());
        map.put("profilePublic", user.getProfilePublic() == null ? 1 : user.getProfilePublic());
        map.put("status", user.getStatus());
        map.put("muteStatus", user.getMuteStatus());
        map.put("muteUntil", user.getMuteUntil());
        map.put("createTime", user.getCreateTime());
        return map;
    }

    private Map<String, Object> ok(String message) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", message);
        return result;
    }

    private Map<String, Object> fail(String message) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        result.put("message", message);
        return result;
    }

    private void writeJson(HttpServletResponse response, Object data) throws IOException {
        response.setContentType("application/json;charset=utf-8");
        PrintWriter out = response.getWriter();
        out.write(JSON.toJSONString(data));
        out.flush();
        out.close();
    }
}
