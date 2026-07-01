package com.treehole.servlet;

import com.alibaba.fastjson.JSON;
import com.treehole.dao.AuthDao;
import com.treehole.pojo.AiAuditResult;
import com.treehole.pojo.Post;
import com.treehole.pojo.Reply;
import com.treehole.pojo.TreeholeUser;
import com.treehole.service.PostService;
import com.treehole.service.impl.AiGatewayService;
import com.treehole.service.impl.PostServiceImpl;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@WebServlet("/api/posts/*")
public class PostServlet extends HttpServlet {

    private PostService postService = new PostServiceImpl();
    private AuthDao authDao = new AuthDao();
    private AiGatewayService aiGatewayService = new AiGatewayService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        String pathInfo = request.getPathInfo();
        if (pathInfo == null || "/".equals(pathInfo)) {
            List<Post> posts = postService.getAllPosts();
            writeJson(response, posts);
        } else if ("/mine".equals(pathInfo)) {
            TreeholeUser user = currentUser(request);
            if (user == null) {
                writeJson(response, fail("请先登录后再查看个人空间"));
                return;
            }
            writeJson(response, postService.getPostsByAuthor(user.getUsername()));
        } else {
            writeJson(response, fail("接口不存在"));
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        String pathInfo = request.getPathInfo();

        if ("/publish".equals(pathInfo)) {
            publishPost(request, response);
        } else if ("/action".equals(pathInfo)) {
            actionPost(request, response);
        } else if ("/reply".equals(pathInfo)) {
            addReply(request, response);
        } else {
            writeJson(response, fail("接口不存在"));
        }
    }

    private void publishPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Map<String, Object> result = new HashMap<>();
        TreeholeUser user = currentUser(request);
        if (user == null) {
            writeJson(response, fail("请先登录后再发布"));
            return;
        }
        TreeholeUser activeUser = activeUser(user);
        if (activeUser == null) {
            writeJson(response, fail("账号已被封禁，无法发布"));
            return;
        }
        if (isMuted(activeUser)) {
            writeJson(response, fail("账号已被禁言，无法发布"));
            return;
        }
        try {
            String json = readBody(request);
            Post post = JSON.parseObject(json, Post.class);
            boolean adminPost = activeUser.getRole() != null && activeUser.getRole() == 2;
            post.setId("hole-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8));
            post.setAuthorUsername(activeUser.getUsername());
            post.setAlias(normalizeAlias(post.getAlias()));
            post.setCreatedAt(new Date());
            post.setMedia(normalizeMedia(post.getMedia()));
            post.setLikes(0);
            post.setLikedBy(new ArrayList<String>());
            post.setHugs("需要倾听".equals(post.getMood()) ? 1 : 0);
            post.setHuggedBy(new ArrayList<String>());
            post.setReports(0);
            post.setReportedBy(new ArrayList<String>());
            post.setReplies(new ArrayList<Reply>());
            post.setAuditStatus(adminPost ? 1 : 0);
            post.setAuditedBy(adminPost ? activeUser.getUsername() : null);
            post.setAuditedAt(adminPost ? new Date() : null);
            post.setIsDeleted(0);

            if (!adminPost) {
                AiAuditResult auditResult = aiGatewayService.auditPost(post.getTitle(), post.getBody(), post.getTags() == null ? "" : post.getTags().toString());
                if (auditResult != null && auditResult.getStatus() != null && auditResult.getStatus() == 2) {
                    result.put("success", false);
                    result.put("message", "AI审核未通过：" + (auditResult.getReason() == null ? "内容不适合展示" : auditResult.getReason()));
                    writeJson(response, result);
                    return;
                }
                if (auditResult != null && auditResult.getStatus() != null && auditResult.getStatus() == 1) {
                    post.setAuditStatus(1);
                    post.setAuditReason(auditResult.getReason());
                    post.setAuditedBy("AI");
                    post.setAuditedAt(new Date());
                } else {
                    post.setAuditStatus(0);
                    post.setAuditReason(auditResult == null ? "等待管理员复核" : auditResult.getReason());
                }
            }

            boolean success = postService.publishPost(post);
            result.put("success", success);
            result.put("message", success ? (post.getAuditStatus() != null && post.getAuditStatus() == 1 ? "发布成功，已显示在广场" : "发布成功，等待管理员审核") : "发布失败，请检查内容长度或稍后重试");
            result.put("post", post);
        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "发布失败");
        }
        writeJson(response, result);
    }

    private void actionPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        TreeholeUser user = currentUser(request);
        if (user == null) {
            writeJson(response, fail("请先登录后再操作"));
            return;
        }
        TreeholeUser activeUser = activeUser(user);
        if (activeUser == null) {
            writeJson(response, fail("账号已被封禁，无法操作"));
            return;
        }
        Map<String, Object> result = new HashMap<>();
        try {
            String json = readBody(request);
            Map<String, String> map = JSON.parseObject(json, Map.class);
            String postId = map.get("postId");
            String action = map.get("action");
            boolean success = postService.actionPost(postId, action, activeUser.getUsername());
            result.put("success", success);
            result.put("message", success ? "操作成功" : "操作失败");
            if (success) {
                Post updatedPost = postService.getPostById(postId);
                result.put("active", actionActive(updatedPost, action, activeUser.getUsername()));
                result.put("likes", updatedPost == null ? 0 : updatedPost.getLikes());
                result.put("hugs", updatedPost == null ? 0 : updatedPost.getHugs());
                result.put("reports", updatedPost == null ? 0 : updatedPost.getReports());
            }
        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "操作失败");
        }
        writeJson(response, result);
    }

    private void addReply(HttpServletRequest request, HttpServletResponse response) throws IOException {
        TreeholeUser user = currentUser(request);
        if (user == null) {
            writeJson(response, fail("请先登录后再回应"));
            return;
        }
        TreeholeUser activeUser = activeUser(user);
        if (activeUser == null) {
            writeJson(response, fail("账号已被封禁，无法回应"));
            return;
        }
        if (isMuted(activeUser)) {
            writeJson(response, fail("账号已被禁言，无法回应"));
            return;
        }
        Map<String, Object> result = new HashMap<>();
        try {
            String json = readBody(request);
            Reply reply = JSON.parseObject(json, Reply.class);
            reply.setId("reply-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8));
            reply.setCreatedAt(new Date());
            reply.setAuthorUsername(activeUser.getUsername());
            reply.setAlias(activeUser.getNickname() == null || activeUser.getNickname().trim().isEmpty() ? activeUser.getUsername() : activeUser.getNickname());
            reply.setAuditStatus(0);
            reply.setIsDeleted(0);

            boolean success = postService.addReply(reply);
            result.put("success", success);
            result.put("message", success ? replySubmitMessage(reply) : "回应失败，请检查内容长度");
            result.put("reply", reply);
        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "回应失败");
        }
        writeJson(response, result);
    }

    private String replySubmitMessage(Reply reply) {
        if (reply == null || reply.getAuditStatus() == null) return "回应成功，等待AI审核";
        if (reply.getAuditStatus() == 1) return "回应成功，AI审核已通过";
        if (reply.getAuditStatus() == 2) return "回应已提交，但AI审核未通过：" + (reply.getAuditReason() == null ? "内容不适合展示" : reply.getAuditReason());
        return "回应成功，AI审核暂未完成，已转入待审核队列";
    }

    private TreeholeUser currentUser(HttpServletRequest request) {
        Object user = request.getSession().getAttribute("loginUser");
        return user instanceof TreeholeUser ? (TreeholeUser) user : null;
    }

    private TreeholeUser activeUser(TreeholeUser sessionUser) {
        if (sessionUser == null || sessionUser.getUsername() == null) return null;
        TreeholeUser freshUser = authDao.getUserByUsername(sessionUser.getUsername());
        if (freshUser == null || freshUser.getStatus() == null || freshUser.getStatus() != 1) return null;
        return freshUser;
    }

    private boolean isMuted(TreeholeUser user) {
        if (user == null || user.getMuteStatus() == null || user.getMuteStatus() != 1) return false;
        if (user.getMuteUntil() != null && user.getMuteUntil().before(new Date())) {
            authDao.updateUserMuteStatus(user.getUserId(), 0, null);
            return false;
        }
        return true;
    }

    private boolean actionActive(Post post, String action, String username) {
        if (post == null || username == null) return false;
        if ("like".equals(action)) return post.getLikedBy() != null && post.getLikedBy().contains(username);
        if ("hug".equals(action)) return post.getHuggedBy() != null && post.getHuggedBy().contains(username);
        if ("report".equals(action)) return post.getReportedBy() != null && post.getReportedBy().contains(username);
        return false;
    }

    private List<Map<String, String>> normalizeMedia(List<Map<String, String>> media) {
        List<Map<String, String>> normalized = new ArrayList<>();
        if (media == null) return normalized;
        for (Map<String, String> item : media) {
            if (item == null || normalized.size() >= 4) break;
            String type = item.get("type");
            String url = item.get("url");
            if (!"image".equals(type) && !"video".equals(type)) continue;
            if (url == null || url.length() > 3_000_000) continue;
            if ("image".equals(type) && !url.startsWith("data:image/")) continue;
            if ("video".equals(type) && !url.startsWith("data:video/")) continue;

            Map<String, String> clean = new HashMap<>();
            clean.put("type", type);
            clean.put("url", url);
            clean.put("name", trimName(item.get("name")));
            normalized.add(clean);
        }
        return normalized;
    }

    private String trimName(String name) {
        if (name == null) return "";
        String trimmed = name.trim();
        return trimmed.length() > 80 ? trimmed.substring(0, 80) : trimmed;
    }

    private String normalizeAlias(String alias) {
        if (alias == null || alias.trim().isEmpty()) return "匿名同学";
        return alias.trim().length() > 12 ? alias.trim().substring(0, 12) : alias.trim();
    }

    private Map<String, Object> fail(String message) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        result.put("message", message);
        return result;
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
