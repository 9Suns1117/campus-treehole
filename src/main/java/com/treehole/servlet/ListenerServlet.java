package com.treehole.servlet;

import com.alibaba.fastjson.JSON;
import com.treehole.dao.ListenerDao;
import com.treehole.pojo.ListenerProfile;
import com.treehole.pojo.ListenerRequest;
import com.treehole.pojo.TreeholeUser;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

@WebServlet("/api/listener/*")
public class ListenerServlet extends HttpServlet {
    private final ListenerDao listenerDao = new ListenerDao();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        String path = request.getPathInfo();
        TreeholeUser user = currentUser(request);

        if ("/plaza".equals(path)) {
            writeJson(response, ok("查询成功", "listeners", listenerDao.getApprovedProfiles()));
        } else if ("/me".equals(path)) {
            if (user == null) {
                writeJson(response, fail("请先登录"));
                return;
            }
            Map<String, Object> result = ok("查询成功");
            result.put("profile", listenerDao.getProfileByUsername(user.getUsername()));
            result.put("incoming", listenerDao.getRequestsForListener(user.getUsername()));
            result.put("outgoing", listenerDao.getRequestsByRequester(user.getUsername()));
            writeJson(response, result);
        } else if ("/messages".equals(path)) {
            if (user == null) {
                writeJson(response, fail("请先登录"));
                return;
            }
            Map<String, Object> result = ok("查询成功");
            result.put("messages", listenerDao.getMessages(parseLong(request.getParameter("id")), user.getUsername()));
            writeJson(response, result);
        } else {
            writeJson(response, fail("接口不存在"));
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        String path = request.getPathInfo();
        TreeholeUser user = currentUser(request);
        if (user == null) {
            writeJson(response, fail("请先登录"));
            return;
        }

        Map<String, Object> body = parseBody(request);
        boolean success;
        if ("/apply".equals(path)) {
            ListenerProfile profile = new ListenerProfile();
            profile.setUsername(user.getUsername());
            profile.setReason(limit(getString(body, "reason"), 500));
            profile.setBio(limit(getString(body, "bio"), 500));
            profile.setTopics(limit(getString(body, "topics"), 255));
            profile.setAvailableTime(limit(getString(body, "availableTime"), 255));
            if (isBlank(profile.getReason()) || isBlank(profile.getTopics()) || isBlank(profile.getAvailableTime())) {
                writeJson(response, fail("请填写申请理由、擅长话题和可在线时间"));
                return;
            }
            success = listenerDao.upsertApplication(profile);
            writeJson(response, success ? ok("申请已提交，等待管理员审核") : fail("申请提交失败"));
        } else if ("/request".equals(path)) {
            ListenerRequest listenerRequest = new ListenerRequest();
            listenerRequest.setListenerUsername(getString(body, "listenerUsername"));
            listenerRequest.setRequesterUsername(user.getUsername());
            listenerRequest.setTopic(limit(getString(body, "topic"), 120));
            listenerRequest.setMessage(limit(getString(body, "message"), 2000));
            listenerRequest.setResponseMode(limit(getString(body, "responseMode"), 50));
            if (isBlank(listenerRequest.getListenerUsername()) || isBlank(listenerRequest.getTopic()) || isBlank(listenerRequest.getMessage())) {
                writeJson(response, fail("请填写想聊的话题和想说的话"));
                return;
            }
            ListenerProfile profile = listenerDao.getProfileByUsername(listenerRequest.getListenerUsername());
            if (profile == null || profile.getStatus() == null || profile.getStatus() != 1) {
                writeJson(response, fail("该倾听者暂不可接收请求"));
                return;
            }
            success = listenerDao.createRequest(listenerRequest);
            writeJson(response, success ? ok("倾听请求已送达") : fail("倾听请求提交失败"));
        } else if ("/request/status".equals(path)) {
            Long id = getLong(body, "id");
            Integer status = getInteger(body, "status");
            String replyText = limit(getString(body, "replyText"), 2000);
            ListenerProfile profile = listenerDao.getProfileByUsername(user.getUsername());
            if (profile == null || profile.getStatus() == null || profile.getStatus() != 1) {
                writeJson(response, fail("你还不是已通过审核的倾听者"));
                return;
            }
            success = listenerDao.updateRequestStatus(id, user.getUsername(), status, replyText);
            writeJson(response, success ? ok("操作成功") : fail("操作失败"));
        } else if ("/thanks".equals(path)) {
            success = listenerDao.sendThanks(getLong(body, "id"), user.getUsername());
            writeJson(response, success ? ok("感谢卡已送出，温柔值已增加") : fail("感谢卡发送失败或已经送过"));
        } else if ("/message".equals(path)) {
            Long id = getLong(body, "id");
            String message = limit(getString(body, "message"), 1000);
            if (isBlank(message)) {
                writeJson(response, fail("请先写下要发送的内容"));
                return;
            }
            success = listenerDao.createMessage(id, user.getUsername(), message);
            writeJson(response, success ? ok("消息已发送") : fail("消息发送失败，请检查请求状态"));
        } else {
            writeJson(response, fail("接口不存在"));
        }
    }

    private TreeholeUser currentUser(HttpServletRequest request) {
        Object user = request.getSession().getAttribute("loginUser");
        return user instanceof TreeholeUser ? (TreeholeUser) user : null;
    }

    private Map<String, Object> parseBody(HttpServletRequest request) throws IOException {
        String text = readBody(request);
        if (isBlank(text)) return new HashMap<>();
        Map<String, Object> map = JSON.parseObject(text, Map.class);
        return map == null ? new HashMap<>() : map;
    }

    private String readBody(HttpServletRequest request) throws IOException {
        StringBuilder sb = new StringBuilder();
        BufferedReader reader = request.getReader();
        String line;
        while ((line = reader.readLine()) != null) sb.append(line);
        return sb.toString();
    }

    private String getString(Map<String, Object> map, String key) {
        Object value = map == null ? null : map.get(key);
        return value == null ? "" : String.valueOf(value).trim();
    }

    private Integer getInteger(Map<String, Object> map, String key) {
        Object value = map == null ? null : map.get(key);
        if (value instanceof Number) return ((Number) value).intValue();
        try {
            return value == null ? null : Integer.parseInt(String.valueOf(value));
        } catch (Exception e) {
            return null;
        }
    }

    private Long getLong(Map<String, Object> map, String key) {
        Object value = map == null ? null : map.get(key);
        if (value instanceof Number) return ((Number) value).longValue();
        try {
            return value == null ? null : Long.parseLong(String.valueOf(value));
        } catch (Exception e) {
            return null;
        }
    }

    private Long parseLong(String value) {
        try {
            return value == null ? null : Long.parseLong(value);
        } catch (Exception e) {
            return null;
        }
    }

    private String limit(String value, int max) {
        if (value == null) return "";
        return value.length() <= max ? value : value.substring(0, max);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
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

    private void writeJson(HttpServletResponse response, Object data) throws IOException {
        response.setContentType("application/json;charset=utf-8");
        PrintWriter out = response.getWriter();
        out.write(JSON.toJSONString(data));
        out.flush();
        out.close();
    }
}
