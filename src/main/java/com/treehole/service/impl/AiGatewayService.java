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

public class AiGatewayService {
    private static final String DEFAULT_AUDIT_PROMPT =
            "你是校园匿名树洞内容审核员。判断内容是否适合公开展示。需要拦截辱骂、人身攻击、色情低俗、暴力威胁、违法违规、泄露隐私、广告引流、歧视仇恨、骚扰、恶意引战、自伤自杀煽动等。正常校园交流、学习生活求助、普通情绪表达可以通过。只返回 JSON：{\"pass\":true 或 false,\"reason\":\"20字以内中文原因\",\"risk\":\"normal/abuse/privacy/sexual/violence/illegal/ad/harassment/self_harm/other\"}";

    private static final String DEFAULT_CHAT_PROMPT =
            "你是校园树洞里的 AI 伙伴。你要温柔、克制、真诚，帮助同学整理心情、润色树洞、拆解困扰。不要替用户做危险决定；遇到自伤、自杀、暴力、违法等高风险内容时，先安抚并建议联系现实中的可信任的人或专业帮助。回答用简洁中文。";

    private final Properties config = new Properties();

    public AiGatewayService() {
        try (InputStream in = AiGatewayService.class.getClassLoader().getResourceAsStream("ai-audit.properties")) {
            if (in != null) config.load(in);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public AiAuditResult auditPost(String title, String body, String tags) {
        String content = "标题：" + trim(title, 80) + "\n内容：" + trim(body, 900) + "\n标签：" + trim(tags, 120);
        return auditText(content);
    }

    public AiAuditResult auditText(String text) {
        if (text == null || text.trim().isEmpty()) return AiAuditResult.rejected("内容为空", "other");
        if (!isEnabled()) return AiAuditResult.pending("AI审核未启用");
        if (!hasApiConfig()) return AiAuditResult.pending("AI接口未配置");
        try {
            String response = callChatApi(getApiUrl(), getApiKey(), getModel(), getAuditPrompt(), "请审核这条树洞内容：\n" + trim(text, 1200), 0);
            return parseAuditResult(response);
        } catch (Exception e) {
            return AiAuditResult.pending("AI异常：" + compact(e.getMessage(), 50));
        }
    }

    public String chat(String message) {
        return chat(message, null);
    }

    public String chat(String message, JSONArray history) {
        if (message == null || message.trim().isEmpty()) return "你可以先写下一句话，我会接住它。";
        if (!hasApiConfig()) return "AI 接口还没有配置好。请先检查 ai-audit.properties 里的 API 地址、Key 和模型。";
        try {
            String response = callChatApi(getApiUrl(), getApiKey(), getModel(), buildChatMessages(getChatPrompt(), history, message), 0.7);
            return parseChatContent(response);
        } catch (Exception e) {
            return "AI 暂时没有接通：" + compact(e.getMessage(), 60);
        }
    }

    private boolean isEnabled() {
        return Boolean.parseBoolean(getConfig("AI_AUDIT_ENABLED", "ai.audit.enabled", "true"));
    }

    private boolean hasApiConfig() {
        String apiUrl = getApiUrl();
        String apiKey = getApiKey();
        return !apiUrl.isEmpty() && !apiKey.isEmpty() && !apiKey.contains("替换") && !apiKey.contains("你的API");
    }

    private String getApiUrl() {
        return normalizeApiUrl(getConfig("AI_AUDIT_API_URL", "ai.audit.apiUrl", ""));
    }

    private String getApiKey() {
        return getConfig("AI_AUDIT_API_KEY", "ai.audit.apiKey", "");
    }

    private String getModel() {
        return getConfig("AI_AUDIT_MODEL", "ai.audit.model", "gemini-3-flash");
    }

    private String getAuditPrompt() {
        return getConfig("AI_AUDIT_PROMPT", "ai.audit.prompt", DEFAULT_AUDIT_PROMPT);
    }

    private String getChatPrompt() {
        return getConfig("AI_CHAT_PROMPT", "ai.chat.prompt", DEFAULT_CHAT_PROMPT);
    }

    private String normalizeApiUrl(String value) {
        if (value == null) return "";
        String url = value.trim();
        if (url.endsWith("/")) url = url.substring(0, url.length() - 1);
        if (url.endsWith("/chat/completions")) return url;
        if (url.endsWith("/v1")) return url + "/chat/completions";
        return url + "/v1/chat/completions";
    }

    private String callChatApi(String apiUrl, String apiKey, String model, String systemPrompt, String userText, double temperature) throws Exception {
        JSONArray messages = new JSONArray();
        JSONObject systemMessage = new JSONObject();
        systemMessage.put("role", "system");
        systemMessage.put("content", systemPrompt);
        messages.add(systemMessage);

        JSONObject userMessage = new JSONObject();
        userMessage.put("role", "user");
        userMessage.put("content", userText);
        messages.add(userMessage);

        return callChatApi(apiUrl, apiKey, model, messages, temperature);
    }

    private String callChatApi(String apiUrl, String apiKey, String model, JSONArray messages, double temperature) throws Exception {
        JSONObject payload = new JSONObject();
        payload.put("model", model);
        payload.put("temperature", temperature);
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
        if (code < 200 || code >= 300) throw new RuntimeException("HTTP " + code + " " + compact(text, 180));
        return text;
    }

    private JSONArray buildChatMessages(String systemPrompt, JSONArray history, String currentMessage) {
        JSONArray messages = new JSONArray();
        JSONObject systemMessage = new JSONObject();
        systemMessage.put("role", "system");
        systemMessage.put("content", systemPrompt + "\n你可以阅读前文上下文，但不要编造用户没有说过的事实。");
        messages.add(systemMessage);

        if (history != null) {
            int start = Math.max(0, history.size() - 10);
            for (int i = start; i < history.size(); i++) {
                JSONObject item = history.getJSONObject(i);
                if (item == null) continue;
                String role = item.getString("role");
                String content = item.getString("content");
                if (!"user".equals(role) && !"assistant".equals(role)) continue;
                if (content == null || content.trim().isEmpty()) continue;
                JSONObject message = new JSONObject();
                message.put("role", role);
                message.put("content", trim(content, 800));
                messages.add(message);
            }
        }

        JSONObject userMessage = new JSONObject();
        userMessage.put("role", "user");
        userMessage.put("content", trim(currentMessage, 1200));
        messages.add(userMessage);
        return messages;
    }

    private AiAuditResult parseAuditResult(String apiResponse) {
        try {
            String content = parseChatContent(apiResponse);
            JSONObject decision = extractJsonObject(content);
            Boolean pass = decision.getBoolean("pass");
            if (pass == null) {
                String value = decision.getString("decision");
                pass = "pass".equalsIgnoreCase(value) || "approve".equalsIgnoreCase(value) || "approved".equalsIgnoreCase(value) || "通过".equals(value);
            }
            if (pass == null) return AiAuditResult.pending("AI返回缺少pass字段");
            String reason = decision.getString("reason");
            String risk = decision.getString("risk");
            return pass ? AiAuditResult.approved(reason, risk) : AiAuditResult.rejected(reason, risk);
        } catch (Exception e) {
            return AiAuditResult.pending("AI解析失败：" + compact(e.getMessage(), 50));
        }
    }

    private String parseChatContent(String apiResponse) {
        JSONObject root = JSON.parseObject(apiResponse);
        JSONArray choices = root.getJSONArray("choices");
        if (choices == null || choices.isEmpty()) return "";
        JSONObject first = choices.getJSONObject(0);
        JSONObject message = first.getJSONObject("message");
        String content = message == null ? first.getString("text") : message.getString("content");
        return content == null ? "" : content.trim();
    }

    private JSONObject extractJsonObject(String text) {
        String cleaned = text == null ? "" : text.trim();
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.replaceFirst("^```json", "").replaceFirst("^```", "");
            int end = cleaned.lastIndexOf("```");
            if (end >= 0) cleaned = cleaned.substring(0, end);
            cleaned = cleaned.trim();
        }
        int start = cleaned.indexOf('{');
        int end = cleaned.lastIndexOf('}');
        if (start >= 0 && end > start) cleaned = cleaned.substring(start, end + 1);
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
