<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!doctype html>
<html lang="zh-CN">
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>校屿树洞 · Campus Whisper</title>
    <link rel="preconnect" href="https://fonts.googleapis.com" />
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin />
    <script>
      (function () {
        var savedTheme = localStorage.getItem("campus-treehole-theme-v1");
        document.documentElement.dataset.theme = savedTheme === "nature" ? "nature" : "blue";
        if (savedTheme === "nature") {
          document.documentElement.classList.add("theme-nature");
        }
      })();
    </script>
    <link
      href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&family=JetBrains+Mono:wght@400;500&family=Noto+Sans+SC:wght@300;400;500;700&display=swap"
      rel="stylesheet"
    />
    <link rel="stylesheet" href="${pageContext.request.contextPath}/styles-blue-hour-image-corrected.css?v=20260702-xhs-inline-replies" />
    <link id="blueHourThemeStyles" rel="stylesheet" href="${pageContext.request.contextPath}/styles-blue-hour-premium.css?v=20260701-blue-hour-premium" />
    <link id="natureThemeStyles" rel="stylesheet" href="${pageContext.request.contextPath}/styles-nature-distilled.css?v=20260701-nature-distilled" />
    <script>
      (function () {
        var savedTheme = localStorage.getItem("campus-treehole-theme-v1");
        if (savedTheme !== "bluehour") {
          document.getElementById("blueHourThemeStyles").disabled = true;
        }
        if (savedTheme !== "nature") {
          document.getElementById("natureThemeStyles").disabled = true;
        }
      })();
    </script>
  </head>
  <body>
    <div class="app-shell">
      <header class="topbar">
        <div class="topbar-row topbar-row--top">
          <a class="brand" href="${pageContext.request.contextPath}/plaza.jsp" aria-label="校屿树洞广场">
            <span class="brand-mark" aria-hidden="true">○</span>
            <span class="brand-text">
              <span class="brand-kicker">CAMPUS WHISPER · EST. 2026</span>
              <strong>校屿树洞</strong>
            </span>
          </a>

          <div class="auth-actions" id="authActions">
            <button class="auth-btn" id="loginUserBtn" type="button">用户登录</button>
            <button class="auth-btn ghost" id="registerBtn" type="button">注册</button>
            <button class="auth-btn admin" id="adminLoginBtn" type="button">管理员入口</button>
          </div>

          <div class="user-actions" id="userActions" style="display: none; align-items: center; gap: 12px; flex-wrap: wrap;">
            <span id="currentUserText" style="font-size: 14px; font-weight: 500; color: var(--ink-soft);"></span>
            <a class="auth-btn ghost" id="profileOpenBtn" href="${pageContext.request.contextPath}/profile.jsp">个人空间</a>
            <a class="auth-btn ghost" href="${pageContext.request.contextPath}/listener.jsp">月光倾听者</a>
            <button class="auth-btn" id="aiOpenBtn" type="button">AI 伙伴</button>
            <button class="auth-btn ghost theme-toggle" id="themeToggleBtn" type="button" title="切换主题">蓝调</button>
            <a class="auth-btn admin" id="adminAuditLink" href="${pageContext.request.contextPath}/admin_audit.jsp" style="display:none;">审核台</a>
            <button class="auth-btn ghost" id="logoutBtn" type="button">退出登录</button>
          </div>
        </div>

        <div class="topbar-row topbar-row--stats">
          <div class="top-stats" aria-label="树洞概览">
            <span><b id="statTotal">0</b><i>条心事</i></span>
            <span><b id="statToday">0</b><i>今日新增</i></span>
            <span><b id="statWarmth">0</b><i>次抱抱</i></span>
          </div>
        </div>
      </header>

      <main class="workspace">
        <section class="compose-panel" aria-labelledby="composeTitle">
          <div class="panel-head">
            <div>
              <p class="eyebrow">匿名入口</p>
              <h1 id="composeTitle">把今晚放进树洞</h1>
            </div>
            <button class="ghost-icon" id="clearDraftBtn" type="button" aria-label="清空草稿" title="清空草稿">×</button>
          </div>

          <form class="composer" id="postForm">
            <label>
              <span>标题</span>
              <input id="postTitle" maxlength="36" placeholder="比如：图书馆三楼的晚风" />
            </label>

            <label>
              <span>内容</span>
              <textarea id="postBody" rows="8" maxlength="520" placeholder="写下想说的话。"></textarea>
            </label>

            <div class="media-uploader">
              <div>
                <span>图片 / 视频</span>
                <p>可添加图片或视频，最多 4 个文件，单个不超过 2MB。</p>
              </div>
              <label class="media-pick-btn">
                <input id="postMedia" type="file" accept="image/*,video/*" multiple />
                添加媒体
              </label>
            </div>
            <div class="media-preview" id="mediaPreview" aria-live="polite"></div>

            <div class="form-grid">
              <label>
                <span>分区</span>
                <select id="postCategory">
                  <option value="日常">日常</option>
                  <option value="心情">心情</option>
                  <option value="求助">求助</option>
                  <option value="失物">失物</option>
                  <option value="表白">表白</option>
                </select>
              </label>

              <label>
                <span>署名</span>
                <select id="postAlias">
                  <option value="nickname">使用昵称</option>
                  <option value="anonymous">匿名同学</option>
                </select>
              </label>
            </div>

            <label>
              <span>标签</span>
              <input id="postTags" maxlength="46" placeholder="用空格分开，如 考研 食堂 社团" />
            </label>

            <div class="mood-picker" aria-label="心情选择">
              <button type="button" class="mood active" data-mood="微光">微光</button>
              <button type="button" class="mood" data-mood="晴朗">晴朗</button>
              <button type="button" class="mood" data-mood="低电量">低电量</button>
              <button type="button" class="mood" data-mood="需要倾听">需要倾听</button>
            </div>

            <div class="composer-footer">
              <span id="charCounter">0 / 520</span>
              <button class="primary-btn" type="submit">
                <span aria-hidden="true">＋</span>
                提交审核
              </button>
            </div>
          </form>

          <div class="notice">
            <strong>温柔但不失边界。</strong>
            <span>发布和回应均会进入审核队列。不要公开姓名、学号、电话、宿舍号等隐私信息。</span>
          </div>
        </section>

        <section class="feed-panel" aria-labelledby="feedTitle">
          <div class="campus-scene" role="img" aria-label="今晚的树洞">
            <span class="scene-tag">ISSUE · NIGHTLY</span>
            <p class="scene-eyebrow">今晚的树洞</p>
            <strong class="scene-count" id="sceneCount">0</strong>
            <span class="scene-foot">条心事 · 正在发光</span>
          </div>

          <section class="interaction-alert" id="interactionAlert" hidden aria-live="polite">
            <div class="interaction-copy">
              <span class="interaction-badge">FOR YOU</span>
              <div>
                <h2 id="interactionTitle">你的树洞正在被看见</h2>
                <p id="interactionText">登录后，这里会显示别人给你帖子留下的喜欢和抱抱。</p>
              </div>
            </div>
            <div class="interaction-side">
              <div class="interaction-stats" id="interactionStats"></div>
              <div class="interaction-actions">
                <button class="auth-btn ghost" id="interactionAckBtn" type="button">收下提醒</button>
                <a class="auth-btn admin" id="interactionProfileBtn" href="${pageContext.request.contextPath}/profile.jsp">查看个人空间</a>
              </div>
            </div>
          </section>

          <div class="feed-head">
            <div>
              <p class="eyebrow">广场</p>
              <h2 id="feedTitle">新鲜心事</h2>
            </div>

            <label class="search-box" aria-label="搜索树洞">
              <span aria-hidden="true">⌕</span>
              <input id="searchInput" placeholder="搜索标题、内容或标签" />
            </label>
          </div>

          <div class="toolbar">
            <div class="filter-tabs" aria-label="分区筛选">
              <button class="filter active" type="button" data-filter="全部">全部</button>
              <button class="filter" type="button" data-filter="日常">日常</button>
              <button class="filter" type="button" data-filter="心情">心情</button>
              <button class="filter" type="button" data-filter="求助">求助</button>
              <button class="filter" type="button" data-filter="失物">失物</button>
              <button class="filter" type="button" data-filter="表白">表白</button>
            </div>

            <label class="sort-select">
              <span>排序</span>
              <select id="sortSelect">
                <option value="newest">最新</option>
                <option value="warmest">最暖</option>
                <option value="popular">最热</option>
              </select>
            </label>
          </div>

          <div class="feed" id="feed" aria-live="polite"></div>
        </section>

        <aside class="insight-panel" aria-label="树洞侧栏">
          <section class="mini-section ai-entry-card ai-entry-card--primary">
            <p class="eyebrow">AI 伙伴</p>
            <h3>把难说的话先说给 AI 听</h3>
            <p class="soft-text">可以帮你整理心情，也可以先安静地听你说一会儿。</p>
            <button class="side-entry-btn ai" id="aiCardBtn" type="button">开始聊天</button>
          </section>

          <section class="mini-section">
            <p class="eyebrow">热门标签</p>
            <div class="tag-cloud" id="tagCloud"></div>
          </section>

          <section class="mini-section">
            <p class="eyebrow">轻声提醒</p>
            <ul class="quiet-list">
              <li>游客可以浏览，登录后才能发布、回应、点赞和举报。</li>
              <li>新树洞和评论会先由 AI 审核，通过后才会出现在广场。</li>
              <li>被举报多次后，会由管理员进行审核。</li>
            </ul>
          </section>
        </aside>
      </main>
    </div>

    <div class="auth-modal" id="authModal">
      <div class="auth-card">
        <button class="auth-close" id="authCloseBtn" type="button">×</button>
        <h2 id="authTitle">用户登录</h2>
        <input type="hidden" id="loginRole" value="1" />
        <label>
          <span>用户名</span>
          <input id="loginUsername" autocomplete="username" placeholder="请输入用户名" />
        </label>
        <label>
          <span>密码</span>
          <input id="loginPassword" type="password" autocomplete="current-password" placeholder="请输入密码" />
        </label>
        <button class="primary-btn" id="submitLoginBtn" type="button">立即登录</button>
        <p class="auth-tip" id="authTip"></p>
      </div>
    </div>

    <div class="auth-modal" id="registerModal">
      <div class="auth-card">
        <button class="auth-close" id="registerCloseBtn" type="button">×</button>
        <h2>用户注册</h2>
        <label>
          <span>用户名</span>
          <input id="registerUsername" autocomplete="username" required placeholder="3-20位字母、数字或下划线" />
        </label>
        <label>
          <span>昵称</span>
          <input id="registerNickname" maxlength="12" required placeholder="请输入昵称" />
        </label>
        <label>
          <span>密码</span>
          <input id="registerPassword" type="password" autocomplete="new-password" placeholder="至少 6 位" />
        </label>
        <button class="primary-btn" id="submitRegisterBtn" type="button">注册账号</button>
        <p class="auth-tip" id="registerTip"></p>
      </div>
    </div>

    <div class="auth-modal" id="aiModal">
      <div class="ai-chat-dialog" role="dialog" aria-modal="true" aria-labelledby="aiDialogTitle">
        <button class="auth-close" id="aiCloseBtn" type="button">×</button>
        <div class="ai-chat-head">
          <div>
            <p class="eyebrow">AI COMPANION</p>
            <h2 id="aiDialogTitle">树洞 AI 伙伴</h2>
          </div>
          <span class="ai-status">在线倾听</span>
        </div>
        <div class="ai-chat-body" id="aiChatMessages" aria-live="polite"></div>
        <form class="ai-chat-form" id="aiChatForm">
          <input id="aiChatInput" maxlength="240" placeholder="写下想聊的事，比如：帮我整理这段心情" />
          <button class="primary-btn" type="submit">发送</button>
        </form>
      </div>
    </div>

    <div class="auth-modal" id="postDetailModal">
      <div class="post-detail-dialog" role="dialog" aria-modal="true" aria-labelledby="postDetailTitle">
        <button class="auth-close" id="postDetailCloseBtn" type="button">×</button>
        <div class="post-detail-content" id="postDetailContent"></div>
      </div>
    </div>

    <div class="toast" id="toast" role="status" aria-live="polite"></div>

    <script>
      window.CONTEXT_PATH = "${pageContext.request.contextPath}";
    </script>
    <script src="${pageContext.request.contextPath}/app.js?v=20260703-nested-reply-actions"></script>
  </body>
</html>
