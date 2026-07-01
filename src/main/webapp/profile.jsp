<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!doctype html>
<html lang="zh-CN">
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>校屿树洞 · 个人空间</title>
    <link rel="preconnect" href="https://fonts.googleapis.com" />
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin />
    <link
      href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&family=JetBrains+Mono:wght@400;500&family=Noto+Sans+SC:wght@300;400;500;700&display=swap"
      rel="stylesheet"
    />
    <link rel="stylesheet" href="${pageContext.request.contextPath}/styles-blue-hour-image-corrected.css" />
  </head>
  <body>
    <div class="app-shell profile-shell">
      <header class="topbar">
        <div class="topbar-row topbar-row--top">
          <a class="brand" href="${pageContext.request.contextPath}/index.jsp" aria-label="返回校屿树洞首页">
            <span class="brand-mark" aria-hidden="true">○</span>
            <span class="brand-text">
              <span class="brand-kicker">CAMPUS WHISPER · PERSONAL SPACE</span>
              <strong>个人空间</strong>
            </span>
          </a>

          <div class="user-actions" style="display: flex; align-items: center; gap: 12px; flex-wrap: wrap;">
            <span id="profileUserText" style="font-size: 14px; font-weight: 500; color: var(--ink-soft);">正在读取账号…</span>
            <a class="auth-btn ghost" href="${pageContext.request.contextPath}/index.jsp">返回广场</a>
            <a class="auth-btn admin" id="profileAdminAuditLink" href="${pageContext.request.contextPath}/admin_audit.jsp" style="display:none;">审核台</a>
            <button class="auth-btn ghost" id="profileLogoutBtn" type="button">退出登录</button>
          </div>
        </div>

        <div class="topbar-row topbar-row--stats">
          <div class="top-stats" aria-label="个人空间概览">
            <span><b id="profileStatPosts">0</b><i>发布</i></span>
            <span><b id="profileStatLikes">0</b><i>收到喜欢</i></span>
            <span><b id="profileStatHugs">0</b><i>收到抱抱</i></span>
          </div>
        </div>
      </header>

      <main class="profile-page">
        <section class="campus-scene profile-hero" role="img" aria-label="个人空间氛围图">
          <span class="scene-tag">MY TREEHOLE</span>
          <p class="scene-eyebrow">把发出的心事收好</p>
          <strong class="scene-count" id="profileHeroCount">0</strong>
          <span class="scene-foot">条树洞 · 记录中</span>
        </section>

        <section class="profile-page-grid">
          <section class="profile-account-card">
            <div class="profile-page-head">
              <div>
                <p class="eyebrow">账号信息</p>
                <h1 id="profilePageTitle">个人空间</h1>
              </div>
              <div class="profile-avatar large" id="profileAvatar">你</div>
            </div>
            <div class="account-list" id="profileAccountList"></div>
          </section>

          <section class="profile-overview-card">
            <div class="profile-page-head">
              <div>
                <p class="eyebrow">我的树洞</p>
                <h2>发布记录</h2>
              </div>
            </div>
            <div class="profile-scoreboard" id="profileScoreboard">
              <span><b>0</b><i>发布</i></span>
              <span><b>0</b><i>喜欢</i></span>
              <span><b>0</b><i>抱抱</i></span>
              <span><b>0</b><i>回应</i></span>
            </div>
            <div class="profile-post-list page-list" id="profilePosts" aria-live="polite"></div>
          </section>
        </section>
      </main>
    </div>

    <div class="toast" id="toast" role="status" aria-live="polite"></div>

    <script>
      window.CONTEXT_PATH = "${pageContext.request.contextPath}";
    </script>
    <script src="${pageContext.request.contextPath}/profile.js"></script>
  </body>
</html>
