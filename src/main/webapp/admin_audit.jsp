<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!doctype html>
<html lang="zh-CN">
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>校屿树洞 · 管理员审核台</title>
    <link rel="preconnect" href="https://fonts.googleapis.com" />
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin />
    <link
      href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&family=JetBrains+Mono:wght@400;500&family=Noto+Sans+SC:wght@300;400;500;700&display=swap"
      rel="stylesheet"
    />
    <link rel="stylesheet" href="${pageContext.request.contextPath}/styles-blue-hour-image-corrected.css?v=20260701-profile-ai" />
  </head>
  <body>
    <div class="app-shell admin-shell">
      <header class="topbar">
        <div class="topbar-row topbar-row--top">
          <a class="brand" href="${pageContext.request.contextPath}/index.jsp" aria-label="返回校屿树洞首页">
            <span class="brand-mark" aria-hidden="true">◇</span>
            <span class="brand-text">
              <span class="brand-kicker">CAMPUS WHISPER · REVIEW DESK</span>
              <strong>管理员审核台</strong>
            </span>
          </a>

          <div class="user-actions" style="display: flex; align-items: center; gap: 12px; flex-wrap: wrap;">
            <span id="adminUserText" style="font-size: 14px; font-weight: 500; color: var(--ink-soft);">正在验证权限…</span>
            <a class="auth-btn ghost" href="${pageContext.request.contextPath}/index.jsp">返回广场</a>
            <a class="auth-btn ghost" href="${pageContext.request.contextPath}/profile.jsp">个人空间</a>
            <button class="auth-btn admin" id="adminRefreshBtn" type="button">刷新队列</button>
            <button class="auth-btn ghost" id="adminLogoutBtn" type="button">退出登录</button>
          </div>
        </div>

        <div class="topbar-row topbar-row--stats">
          <div class="top-stats" aria-label="审核概览">
            <span><b id="auditPendingCount">0</b><i>待审核</i></span>
            <span><b id="auditApprovedCount">0</b><i>已通过</i></span>
            <span><b id="auditRejectedCount">0</b><i>已驳回</i></span>
            <span><b id="userTotalCount">0</b><i>用户</i></span>
            <span><b id="userLimitedCount">0</b><i>限制中</i></span>
          </div>
        </div>
      </header>

      <main class="admin-workspace">
        <section class="campus-scene audit-hero" role="img" aria-label="审核台氛围图">
          <span class="scene-tag">ADMIN · BLUE HOUR</span>
          <p class="scene-eyebrow">守住夜色里的边界</p>
          <strong class="scene-count" id="auditHeroCount">0</strong>
          <span class="scene-foot">条内容 · 等待确认</span>
        </section>

        <section class="audit-panel" aria-labelledby="auditTitle">
          <div class="feed-head audit-head">
            <div>
              <p class="eyebrow">审核队列</p>
              <h1 id="auditTitle">树洞、评论与用户管理</h1>
            </div>

            <label class="search-box" aria-label="搜索审核内容">
              <span aria-hidden="true">⌕</span>
              <input id="auditSearchInput" placeholder="搜索内容、作者、标题、分区或用户" />
            </label>
          </div>

          <div class="toolbar audit-toolbar">
            <div class="filter-tabs audit-tabs" aria-label="审核对象切换">
              <button class="filter active" type="button" data-tab="posts">树洞审核</button>
              <button class="filter" type="button" data-tab="replies">评论审核</button>
              <button class="filter" type="button" data-tab="users">用户管理</button>
              <button class="filter ai-audit-trigger" id="aiAuditRepliesBtn" type="button">AI审核待审评论</button>
            </div>

            <label class="sort-select">
              <span>状态</span>
              <select id="auditStatusSelect">
                <option value="0">待审核</option>
                <option value="all">全部</option>
                <option value="1">已通过</option>
                <option value="2">已驳回</option>
              </select>
            </label>
          </div>

          <div class="audit-list" id="postAuditList" aria-live="polite"></div>
          <div class="audit-list" id="replyAuditList" aria-live="polite" style="display:none;"></div>
          <div class="audit-list user-audit-list" id="userAuditList" aria-live="polite" style="display:none;"></div>
        </section>
      </main>
    </div>

    <div class="toast" id="toast" role="status" aria-live="polite"></div>

    <script>
      window.CONTEXT_PATH = "${pageContext.request.contextPath}";
    </script>
    <script src="${pageContext.request.contextPath}/admin-audit.js?v=20260701-profile-ai"></script>
  </body>
</html>
