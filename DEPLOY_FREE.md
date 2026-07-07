# 免费部署建议

这个项目是 JSP/Servlet + MySQL，不适合只用 GitHub Pages、Cloudflare Pages 这类静态托管。推荐用免费云主机运行 Docker。

## 推荐方案：Oracle Cloud Always Free

优点：
- 可以免费创建长期运行的云主机。
- 适合运行 Tomcat + MySQL 这种完整后端项目。
- 使用 Docker Compose 后，项目迁移和协作比较简单。

限制：
- 需要自己注册 Oracle Cloud 账号。
- 通常需要手机号、银行卡验证。
- 需要把 8080 或 80 端口放通。

## 部署步骤

1. 创建一台 Ubuntu 云主机。
2. 安装 Docker 和 Docker Compose。
3. 克隆项目：

```bash
git clone https://github.com/9Suns1117/campus-treehole.git
cd campus-treehole
```

4. 创建环境变量文件：

```bash
cp .env.example .env
```

5. 编辑 `.env`，填入数据库密码和 AI 审核 API：

```bash
MYSQL_ROOT_PASSWORD=change-this-password
APP_PORT=8080
AI_AUDIT_ENABLED=true
AI_AUDIT_API_URL=https://your-api-host
AI_AUDIT_API_KEY=your-api-key
AI_AUDIT_MODEL=gemini-3-flash
```

6. 启动：

```bash
docker compose up -d --build
```

7. 访问：

```text
http://服务器公网IP:8080
```

## 注意

- 不要把真实的 AI API Key 提交到 GitHub。
- 如果要绑定域名，可以后续加 Nginx 和 HTTPS。
- MySQL 数据保存在 Docker volume `mysql-data` 中，重启容器不会丢失。
