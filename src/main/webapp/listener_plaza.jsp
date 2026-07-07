<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!doctype html>
<html lang="zh-CN">
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>月光倾听者广场 · Campus Whisper</title>
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
          <a class="brand" href="${pageContext.request.contextPath}/listener.jsp" aria-label="返回月光倾听者入口">
            <span class="brand-mark" aria-hidden="true">○</span>
            <span class="brand-text">
              <span class="brand-kicker">LISTENER PLAZA</span>
              <strong>倾听者广场</strong>
            </span>
          </a>
          <div class="user-actions" style="display:flex;align-items:center;gap:12px;flex-wrap:wrap;">
            <span id="listenerUserText" style="font-size:14px;font-weight:500;color:var(--ink-soft);">正在读取账号…</span>
            <a class="auth-btn ghost" href="${pageContext.request.contextPath}/listener.jsp">申请入口</a>
            <a class="auth-btn ghost" href="${pageContext.request.contextPath}/plaza.jsp">返回广场</a>
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

      <main class="listener-page listener-plaza-page">
        <section class="campus-scene listener-hero">
          <span class="scene-tag">PEER SUPPORT</span>
          <p class="scene-eyebrow">找一个愿意接住你的人，说一会儿</p>
          <strong class="scene-count" id="listenerHeroCount">0</strong>
          <span class="scene-foot">同伴陪伴 · 情绪支持 · 非专业心理咨询</span>
        </section>

        <section class="feed-panel listener-main-panel">
          <div class="feed-head listener-plaza-head">
            <div>
              <p class="eyebrow">倾听者广场</p>
              <h1>今晚想找谁听听？</h1>
            </div>
            <label class="search-box">
              <span aria-hidden="true">⌕</span>
              <input id="listenerSearch" placeholder="搜索昵称、简介或话题" />
            </label>
          </div>
          <div class="listener-plaza-actions">
            <button class="auth-btn ghost" id="openIncomingRequests" type="button">我收到的请求</button>
            <button class="auth-btn ghost" id="openOutgoingRequests" type="button">我发出的请求</button>
          </div>
          <div class="listener-list" id="listenerList" aria-live="polite"></div>
        </section>
      </main>
    </div>

    <div class="auth-modal" id="listenerModal" aria-hidden="true">
      <div class="auth-card listener-dialog" role="dialog" aria-modal="true" aria-labelledby="listenerModalTitle">
        <button class="modal-close" id="listenerModalClose" type="button" aria-label="关闭">×</button>
        <p class="eyebrow">倾听请求</p>
        <h2 id="listenerModalTitle">找 TA 说说</h2>
        <form class="auth-form" id="listenerRequestForm">
          <input id="listenerTargetUsername" type="hidden" />
          <label>
            <span>想聊的话题</span>
            <input id="requestTopic" maxlength="80" placeholder="比如：最近压力很大" />
          </label>
          <label>
            <span>想说的话</span>
            <textarea id="requestMessage" rows="5" maxlength="1000" placeholder="可以先写一点你愿意说的部分"></textarea>
          </label>
          <label>
            <span>期望回应方式</span>
            <select id="requestMode">
              <option value="只想被倾听">只想被倾听</option>
              <option value="希望给建议">希望给建议</option>
              <option value="想要鼓励">想要鼓励</option>
            </select>
          </label>
          <button class="primary-btn" type="submit">送出请求</button>
        </form>
      </div>
    </div>

    <div class="auth-modal" id="listenerRequestsModal" aria-hidden="true">
      <div class="auth-card listener-dialog listener-requests-dialog" role="dialog" aria-modal="true" aria-labelledby="listenerRequestsTitle">
        <button class="modal-close" id="listenerRequestsClose" type="button" aria-label="关闭">×</button>
        <p class="eyebrow" id="listenerRequestsEyebrow">请求列表</p>
        <h2 id="listenerRequestsTitle">我收到的请求</h2>
        <div class="listener-request-list listener-modal-list" id="listenerRequestsList"></div>
      </div>
    </div>

    <div class="auth-modal" id="listenerChatModal" aria-hidden="true">
      <div class="auth-card listener-dialog listener-chat-dialog" role="dialog" aria-modal="true" aria-labelledby="listenerChatTitle">
        <button class="modal-close" id="listenerChatClose" type="button" aria-label="关闭">×</button>
        <p class="eyebrow">实时对话</p>
        <h2 id="listenerChatTitle">月光小窗</h2>
        <p class="soft-text" id="listenerChatMeta"></p>
        <div class="listener-chat-messages" id="listenerChatMessages" aria-live="polite"></div>
        <form class="listener-chat-form" id="listenerChatForm">
          <input id="listenerChatInput" maxlength="800" placeholder="写一句想说的话…" />
          <button class="primary-btn" type="submit">发送</button>
        </form>
      </div>
    </div>

    <div class="toast" id="toast" role="status" aria-live="polite"></div>
    <script>window.CONTEXT_PATH = "${pageContext.request.contextPath}";</script>
    <script src="${pageContext.request.contextPath}/listener.js?v=20260702-listener-flow"></script>
  </body>
</html>
