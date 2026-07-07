<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!doctype html>
<html lang="zh-CN">
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>月光倾听者 · Campus Whisper</title>
    <script>
      (function () {
        var savedTheme = localStorage.getItem("campus-treehole-theme-v1");
        document.documentElement.dataset.theme = savedTheme === "nature" ? "nature" : "blue";
        if (savedTheme === "nature") document.documentElement.classList.add("theme-nature");
      })();
    </script>
    <link rel="preconnect" href="https://fonts.googleapis.com" />
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin />
    <link
      href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&family=JetBrains+Mono:wght@400;500&family=Noto+Sans+SC:wght@300;400;500;700&display=swap"
      rel="stylesheet"
    />
    <link rel="stylesheet" href="${pageContext.request.contextPath}/styles-blue-hour-image-corrected.css?v=20260702-listener-flow" />
    <link id="blueHourThemeStyles" rel="stylesheet" href="${pageContext.request.contextPath}/styles-blue-hour-premium.css?v=20260701-blue-hour-premium" />
    <link id="natureThemeStyles" rel="stylesheet" href="${pageContext.request.contextPath}/styles-nature-distilled.css?v=20260701-nature-distilled" />
    <script>
      (function () {
        var savedTheme = localStorage.getItem("campus-treehole-theme-v1");
        if (savedTheme !== "bluehour") document.getElementById("blueHourThemeStyles").disabled = true;
        if (savedTheme !== "nature") document.getElementById("natureThemeStyles").disabled = true;
      })();
    </script>
  </head>
  <body>
    <div class="app-shell listener-shell">
      <header class="topbar">
        <div class="topbar-row topbar-row--top">
          <a class="brand" href="${pageContext.request.contextPath}/plaza.jsp" aria-label="返回校屿树洞">
            <span class="brand-mark" aria-hidden="true">○</span>
            <span class="brand-text">
              <span class="brand-kicker">MOONLIGHT LISTENERS</span>
              <strong>月光倾听者</strong>
            </span>
          </a>
          <div class="user-actions" style="display:flex;align-items:center;gap:12px;flex-wrap:wrap;">
            <span id="listenerUserText" style="font-size:14px;font-weight:500;color:var(--ink-soft);">正在读取账号…</span>
            <a class="auth-btn ghost" href="${pageContext.request.contextPath}/plaza.jsp">返回广场</a>
            <a class="auth-btn ghost" href="${pageContext.request.contextPath}/profile.jsp">个人空间</a>
          </div>
        </div>
        <div class="topbar-row topbar-row--stats">
          <div class="top-stats">
            <span><b id="listenerCount">0</b><i>倾听者</i></span>
            <span><b id="requestCount">0</b><i>我的请求</i></span>
            <span><b id="thanksCount">0</b><i>感谢卡</i></span>
          </div>
        </div>
      </header>

      <main class="listener-page listener-entry-page">
        <section class="campus-scene listener-hero listener-entry-hero">
          <span class="scene-tag">MOONLIGHT</span>
          <p class="scene-eyebrow">只做同伴陪伴和情绪支持，不替代专业心理咨询</p>
          <strong class="scene-count" id="listenerHeroCount">0</strong>
          <span class="scene-foot">人在低谷时，也可以被温柔接住</span>
        </section>

        <section class="listener-entry-grid">
          <section class="compose-panel listener-apply-card">
            <div class="panel-head">
              <div>
                <p class="eyebrow">申请入口</p>
                <h1>申请成为月光倾听者</h1>
              </div>
            </div>
            <form class="composer" id="listenerApplyForm">
              <label>
                <span>申请理由</span>
                <textarea id="listenerReason" rows="4" maxlength="500" placeholder="为什么想成为倾听者？"></textarea>
              </label>
              <label>
                <span>简介</span>
                <textarea id="listenerBio" rows="3" maxlength="500" placeholder="给大家一点安全感，比如你的倾听方式"></textarea>
              </label>
              <label>
                <span>擅长话题</span>
                <input id="listenerTopics" maxlength="120" placeholder="如：考研 焦虑 人际关系 失眠" />
              </label>
              <label>
                <span>可在线时间</span>
                <input id="listenerAvailableTime" maxlength="120" placeholder="如：周一到周五 20:00-22:00" />
              </label>
              <button class="primary-btn" type="submit">提交申请</button>
            </form>
            <div class="notice listener-notice">
              <strong>边界提醒</strong>
              <span>倾听者只提供同伴陪伴和情绪支持，不提供专业心理咨询；遇到危机请联系现实中可信任的人或专业机构。</span>
            </div>
          </section>

          <section class="mini-section listener-entry-card">
            <p class="eyebrow">平台入口</p>
            <h2>进入月光倾听者平台</h2>
            <p>查看已通过审核的倾听者，发起倾听请求，也可以在成为倾听者后处理收到的请求。</p>
            <div class="listener-entry-status">
              <span class="audit-status pending" id="listenerStatusTitle">尚未申请</span>
              <p id="listenerStatusText">提交申请后，管理员会在审核台处理。</p>
            </div>
            <a class="primary-btn listener-platform-link" href="${pageContext.request.contextPath}/listener_plaza.jsp">进入平台</a>
          </section>
        </section>
      </main>
    </div>

    <div class="toast" id="toast" role="status" aria-live="polite"></div>
    <script>window.CONTEXT_PATH = "${pageContext.request.contextPath}";</script>
    <script src="${pageContext.request.contextPath}/listener.js?v=20260702-listener-flow"></script>
  </body>
</html>
