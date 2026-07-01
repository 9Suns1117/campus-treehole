package com.treehole.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.treehole.pojo.AiAuditResult;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

public class AiCommentAuditService {

    private static final String DEFAULT_PROMPT =
            "你是校园匿名树洞评论审核员。请判断用户评论是否适合公开展示。" +
            "需要拦截：辱骂、人身攻击、色情低俗、暴力威胁、违法违规、泄露隐私、广告引流、歧视仇恨、骚扰、恶意引战、自伤自杀煽动等。" +
            "正常校园交流、善意调侃、学习生活求助、普通情绪表达可以通过。" +
            "只返回一个JSON对象，不要返回Markdown，不要额外解释。" +
            "JSON格式：{\"pass\":true或false,\"reason\":\"20字以内中文原因\",\"risk\":\"normal/abuse/privacy/sexual/violence/illegal/ad/harassment/self_harm/other\"}";

    private final Properties config = new Properties();

    public AiCommentAuditService() {
        try (InputStream in = AiCommentAuditService.class.getClassLoader().getResourceAsStream("ai-audit.properties")) {
            if (in != null) {
                config.load(in);
                System.out.println("[AI审核] 已读取 ai-audit.properties");
            } else {
                System.out.println("[AI审核] 未找到 ai-audit.properties，请确认文件在 src/main/resources 下");
            }
        } catch (Exception e) {
            System.out.println("[AI审核] 读取配置失败：" + e.getMessage());
            e.printStackTrace();
        }
    }

    public AiAuditResult auditReply(String replyBody) {
        if (replyBody == null || replyBody.trim().isEmpty()) {
            return AiAuditResult.rejected("评论为空", "other");
        }

        boolean enabled = Boolean.parseBoolean(getConfig("AI_AUDIT_ENABLED", "ai.audit.enabled", "true"));
        if (!enabled) {
            return AiAuditResult.pending("AI审核未启用");
        }

        String apiUrl = normalizeApiUrl(getConfig("AI_AUDIT_API_URL", "ai.audit.apiUrl", ""));
        String apiKey = getConfig("AI_AUDIT_API_KEY", "ai.audit.apiKey", "");
        String model = getConfig("AI_AUDIT_MODEL", "ai.audit.model", "gemini-3-flash");
        String systemPrompt = getConfig("AI_AUDIT_PROMPT", "ai.audit.prompt", DEFAULT_PROMPT);

        if (apiUrl.isEmpty() || apiKey.isEmpty() || apiKey.contains("替换") || apiKey.contains("你的API")) {
            return AiAuditResult.pending("AI接口未配置");
        }

        try {
            System.out.println("[AI审核] 请求模型=" + model + "，地址=" + apiUrl);
            String response = callChatApi(apiUrl, apiKey, model, systemPrompt, replyBody);
            return parseAuditResult(response);
        } catch (Exception e) {
            String msg = compact(e.getMessage(), 90);
            System.out.println("[AI审核] 调用失败：" + msg);
            e.printStackTrace();
            return AiAuditResult.pending("AI异常：" + msg);
        }
    }

    private String normalizeApiUrl(String value) {
        if (value == null) return "";
        String url = value.trim();
        if (url.endsWith("/")) url = url.substring(0, url.length() - 1);
        if (url.endsWith("/chat/completions")) return url;
        if (url.endsWith("/v1")) return url + "/chat/completions";
        return url + "/v1/chat/completions";
    }

    private String callChatApi(String apiUrl, String apiKey, String model, String systemPrompt, String replyBody) throws Exception {
        JSONObject payload = new JSONObject();
        payload.put("model", model);
        payload.put("temperature", 0);

        JSONArray messages = new JSONArray();
        JSONObject systemMessage = new JSONObject();
        systemMessage.put("role", "system");
        systemMessage.put("content", systemPrompt);
        messages.add(systemMessage);

        JSONObject userMessage = new JSONObject();
        userMessage.put("role", "user");
        userMessage.put("content", "请审核这条评论：" + trim(replyBody, 600));
        messages.add(userMessage);
        payload.put("messages", messages);

        URL url = new URL(apiUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setConnectTimeout(12000);
        conn.setReadTimeout(30000);
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
        conn.setRequestProperty("Authorization", "Bearer " + apiKey);

        byte[] body = payload.toJSONString().getBytes(StandardCharsets.UTF_8);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(body);
        }

        int code = conn.getResponseCode();
        InputStream stream = code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream();
        String text = readStream(stream);
        System.out.println("[AI审核] HTTP=" + code + "，返回=" + compact(text, 500));
        if (code < 200 || code >= 300) {
            throw new RuntimeException("HTTP " + code + " " + compact(text, 180));
        }
        return text;
    }

    private AiAuditResult parseAuditResult(String apiResponse) {
        try {
            JSONObject root = JSON.parseObject(apiResponse);
            JSONArray choices = root.getJSONArray("choices");
            if (choices == null || choices.isEmpty()) {
                return AiAuditResult.pending("AI返回无choices");
            }

            JSONObject first = choices.getJSONObject(0);
            JSONObject message = first.getJSONObject("message");
            String content = message == null ? first.getString("text") : message.getString("content");
            if (content == null || content.trim().isEmpty()) {
                return AiAuditResult.pending("AI返回内容为空");
            }

            JSONObject decision = extractJsonObject(content);
            Boolean pass = decision.getBoolean("pass");
            if (pass == null) {
                String value = decision.getString("decision");
                pass = "pass".equalsIgnoreCase(value) || "approve".equalsIgnoreCase(value) || "approved".equalsIgnoreCase(value) || "通过".equals(value);
            }
            if (pass == null) {
                return AiAuditResult.pending("AI返回缺少pass字段");
            }

            String reason = decision.getString("reason");
            String risk = decision.getString("risk");
            return pass ? AiAuditResult.approved(reason, risk) : AiAuditResult.rejected(reason, risk);
        } catch (Exception e) {
            return AiAuditResult.pending("AI解析失败：" + compact(e.getMessage(), 50));
        }
    }

    private JSONObject extractJsonObject(String text) {
        String cleaned = text.trim();
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.replaceFirst("^```json", "").replaceFirst("^```", "");
            int end = cleaned.lastIndexOf("```");
            if (end >= 0) cleaned = cleaned.substring(0, end);
            cleaned = cleaned.trim();
        }
        int start = cleaned.indexOf('{');
        int end = cleaned.lastIndexOf('}');
        if (start >= 0 && end > start) {
            cleaned = cleaned.substring(start, end + 1);
        }
        return JSON.parseObject(cleaned);
    }

    private String getConfig(String envKey, String propertyKey, String defaultValue) {
        String envValue = System.getenv(envKey);
        if (envValue != null && !envValue.trim().isEmpty()) return envValue.trim();
        String propValue = config.getProperty(propertyKey);
        if (propValue != null && !propValue.trim().isEmpty()) return propValue.trim();
        return defaultValue;
    }

    private String readStream(InputStream stream) throws Exception {
        if (stream == null) return "";
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
        }
        return sb.toString();
    }

    private String trim(String value, int maxLength) {
        String text = value == null ? "" : value.trim();
        return text.length() <= maxLength ? text : text.substring(0, maxLength);
    }

    private String compact(String value, int maxLength) {
        if (value == null) return "";
        String text = value.replace('\n', ' ').replace('\r', ' ').trim();
        return text.length() <= maxLength ? text : text.substring(0, maxLength) + "...";
    }
}
