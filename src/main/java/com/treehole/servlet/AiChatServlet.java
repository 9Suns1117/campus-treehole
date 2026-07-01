package com.treehole.servlet;

import com.alibaba.fastjson.JSON;
import com.treehole.service.impl.AiGatewayService;

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

@WebServlet("/api/ai/chat")
public class AiChatServlet extends HttpServlet {
    private final AiGatewayService aiGatewayService = new AiGatewayService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        Map<String, Object> result = new HashMap<>();
        try {
            Map<String, String> body = JSON.parseObject(readBody(request), Map.class);
            String message = body == null ? "" : body.get("message");
            if (message == null || message.trim().isEmpty()) {
                result.put("success", false);
                result.put("message", "消息不能为空");
            } else {
                result.put("success", true);
                result.put("reply", aiGatewayService.chat(message));
            }
        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "AI 暂时没有接通");
        }
        writeJson(response, result);
    }

    private String readBody(HttpServletRequest request) throws IOException {
        StringBuilder sb = new StringBuilder();
        BufferedReader reader = request.getReader();
        String line;
        while ((line = reader.readLine()) != null) sb.append(line);
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
