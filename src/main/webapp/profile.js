(function () {
  "use strict";

  let loginUser = null;
  const state = {
    posts: [],
  };

  const dom = {
    userText: document.getElementById("profileUserText"),
    adminAuditLink: document.getElementById("profileAdminAuditLink"),
    logoutBtn: document.getElementById("profileLogoutBtn"),
    statPosts: document.getElementById("profileStatPosts"),
    statLikes: document.getElementById("profileStatLikes"),
    statHugs: document.getElementById("profileStatHugs"),
    heroCount: document.getElementById("profileHeroCount"),
    avatar: document.getElementById("profileAvatar"),
    accountList: document.getElementById("profileAccountList"),
    scoreboard: document.getElementById("profileScoreboard"),
    posts: document.getElementById("profilePosts"),
    toast: document.getElementById("toast"),
  };

  init();

  function init() {
    bindEvents();
    syncCurrentUser();
  }

  function api(path) {
    return (window.CONTEXT_PATH || "") + path;
  }

  function bindEvents() {
    if (dom.logoutBtn) dom.logoutBtn.addEventListener("click", logout);
  }

  function syncCurrentUser() {
    fetch(api("/auth/current"), { credentials: "same-origin" })
      .then((res) => res.json())
      .then((data) => {
        if (!data.success || !data.user) {
          showToast("请先登录后查看个人空间。");
          window.setTimeout(() => (location.href = api("/index.jsp")), 900);
          return;
        }
        loginUser = data.user;
        localStorage.setItem("loginUser", JSON.stringify(data.user));
        renderUserHeader();
        loadMinePosts();
      })
      .catch(() => {
        showToast("登录状态读取失败，请重新登录。");
        window.setTimeout(() => (location.href = api("/index.jsp")), 900);
      });
  }

  function loadMinePosts() {
    dom.posts.innerHTML = `<div class="profile-empty">正在读取你的树洞…</div>`;
    fetch(api("/api/posts/mine"), { credentials: "same-origin" })
      .then((res) => res.json())
      .then((data) => {
        state.posts = normalizePosts(Array.isArray(data) ? data : []);
        render();
      })
      .catch(() => {
        dom.posts.innerHTML = `<div class="profile-empty">读取失败，请稍后再试。</div>`;
      });
  }

  function renderUserHeader() {
    const roleName = Number(loginUser.role) === 2 ? "管理员" : "普通用户";
    dom.userText.textContent = `${roleName}：${loginUser.username}`;
    dom.avatar.textContent = getAvatarText();
    if (dom.adminAuditLink) dom.adminAuditLink.style.display = Number(loginUser.role) === 2 ? "inline-flex" : "none";
  }

  function render() {
    const summary = getSummary();
    renderStats(summary);
    renderAccount();
    renderPosts(summary.posts);
  }

  function renderStats(summary) {
    dom.statPosts.textContent = summary.postCount;
    dom.statLikes.textContent = summary.likesReceived;
    dom.statHugs.textContent = summary.hugsReceived;
    dom.heroCount.textContent = summary.postCount;
    dom.scoreboard.innerHTML = `
      <span><b>${summary.postCount}</b><i>发布</i></span>
      <span><b>${summary.likesReceived}</b><i>喜欢</i></span>
      <span><b>${summary.hugsReceived}</b><i>抱抱</i></span>
      <span><b>${summary.replyCount}</b><i>回应</i></span>
    `;
  }

  function renderAccount() {
    const accountItems = [
      ["用户名", `@${loginUser.username}`],
      ["昵称", loginUser.nickname || "未设置"],
      ["身份", Number(loginUser.role) === 2 ? "管理员" : "普通用户"],
      ["账号编号", loginUser.userId == null ? "暂无" : `#${loginUser.userId}`],
      ["账号状态", Number(loginUser.status) === 0 ? "已禁用" : "正常"],
      ["头像", loginUser.avatarUrl ? "已设置" : "默认头像"],
      ["创建时间", formatDate(loginUser.createTime)],
    ];

    dom.accountList.innerHTML = accountItems
      .map(([label, value]) => `<div><span>${escapeHtml(label)}</span><strong>${escapeHtml(value)}</strong></div>`)
      .join("");
  }

  function renderPosts(posts) {
    if (!posts.length) {
      dom.posts.innerHTML = `<div class="profile-empty">还没有自己的树洞。回到广场写下一句，这里会替你收好它。</div>`;
      return;
    }
    dom.posts.innerHTML = posts.map(renderProfilePost).join("");
  }

  function renderProfilePost(post) {
    const status = Number(post.auditStatus);
    const statusText = status === 1 ? "已通过" : status === 2 ? "已驳回" : "待审核";
    const statusClass = status === 1 ? "ok" : status === 2 ? "reject" : "pending";
    const likes = getOtherInteractionCount(post, "likes", "likedBy");
    const hugs = getOtherInteractionCount(post, "hugs", "huggedBy");
    const replies = Array.isArray(post.replies) ? post.replies.length : 0;

    return `
      <article class="profile-post-card">
        <div class="profile-post-head">
          <span class="profile-status ${statusClass}">${statusText}</span>
          <time datetime="${escapeHtml(post.createdAt)}">${timeAgo(post.createdAt)}</time>
        </div>
        <strong>${escapeHtml(post.title || "匿名碎碎念")}</strong>
        <p>${escapeHtml(post.body || "")}</p>
        ${renderMediaGrid(post.media)}
        <div class="profile-post-meta">
          <span>${escapeHtml(post.category || "日常")}</span>
          <span>${escapeHtml(post.mood || "微光")}</span>
          <span>${likes} 喜欢</span>
          <span>${hugs} 抱抱</span>
          <span>${replies} 回应</span>
        </div>
        ${post.auditReason ? `<div class="audit-reason">审核说明：${escapeHtml(post.auditReason)}</div>` : ""}
      </article>
    `;
  }

  function getSummary() {
    const posts = state.posts;
    const likesReceived = posts.reduce((sum, post) => sum + getOtherInteractionCount(post, "likes", "likedBy"), 0);
    const hugsReceived = posts.reduce((sum, post) => sum + getOtherInteractionCount(post, "hugs", "huggedBy"), 0);
    const replyCount = posts.reduce((sum, post) => sum + (Array.isArray(post.replies) ? post.replies.length : 0), 0);

    return {
      posts,
      postCount: posts.length,
      likesReceived,
      hugsReceived,
      replyCount,
    };
  }

  function getOtherInteractionCount(post, totalKey, listKey) {
    const total = Number(post[totalKey] || 0);
    const users = Array.isArray(post[listKey]) ? post[listKey] : [];
    if (users.length) return users.filter((name) => name !== loginUser.username).length;
    return post.authorUsername === loginUser.username ? total : 0;
  }

  function normalizePosts(posts) {
    return posts.map((post) => ({
      ...post,
      tags: Array.isArray(post.tags) ? post.tags : [],
      likedBy: Array.isArray(post.likedBy) ? post.likedBy : [],
      huggedBy: Array.isArray(post.huggedBy) ? post.huggedBy : [],
      reportedBy: Array.isArray(post.reportedBy) ? post.reportedBy : [],
      replies: Array.isArray(post.replies) ? post.replies : [],
      likes: Number(post.likes || 0),
      hugs: Number(post.hugs || 0),
      reports: Number(post.reports || 0),
      auditStatus: post.auditStatus === undefined ? 1 : Number(post.auditStatus),
      media: normalizeMedia(post.media),
    }));
  }

  function normalizeMedia(media) {
    if (!Array.isArray(media)) return [];
    return media
      .filter((item) => item && typeof item.url === "string" && typeof item.type === "string")
      .map((item) => ({
        type: item.type.startsWith("video") ? "video" : "image",
        url: item.url,
        name: item.name || "",
      }))
      .slice(0, 4);
  }

  function renderMediaGrid(media) {
    const items = normalizeMedia(media);
    if (!items.length) return "";
    return `
      <div class="post-media-grid">
        ${items
          .map((item) =>
            item.type === "video"
              ? `<figure class="post-media-item"><video src="${escapeHtml(item.url)}" controls preload="metadata"></video></figure>`
              : `<figure class="post-media-item"><img src="${escapeHtml(item.url)}" alt="${escapeHtml(item.name || "树洞图片")}" loading="lazy" /></figure>`,
          )
          .join("")}
      </div>
    `;
  }

  function getAvatarText() {
    const name = loginUser.nickname || loginUser.username || "你";
    return name.slice(0, 1).toUpperCase();
  }

  function logout() {
    fetch(api("/auth/logout"), { method: "POST", credentials: "same-origin" }).finally(() => {
      localStorage.removeItem("loginUser");
      location.href = api("/index.jsp");
    });
  }

  function timeAgo(value) {
    if (!value) return "刚刚";
    const diff = Date.now() - new Date(value).getTime();
    const minute = 60 * 1000;
    const hour = 60 * minute;
    const day = 24 * hour;
    if (Number.isNaN(diff) || diff < minute) return "刚刚";
    if (diff < hour) return `${Math.floor(diff / minute)} 分钟前`;
    if (diff < day) return `${Math.floor(diff / hour)} 小时前`;
    return `${Math.floor(diff / day)} 天前`;
  }

  function formatDate(value) {
    if (!value) return "暂无";
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return "暂无";
    return date.toLocaleDateString("zh-CN", {
      year: "numeric",
      month: "2-digit",
      day: "2-digit",
    });
  }

  function escapeHtml(value) {
    return String(value == null ? "" : value)
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;")
      .replace(/"/g, "&quot;")
      .replace(/'/g, "&#039;");
  }

  function showToast(message) {
    dom.toast.textContent = message;
    dom.toast.classList.add("show");
    window.clearTimeout(showToast.timer);
    showToast.timer = window.setTimeout(() => dom.toast.classList.remove("show"), 2400);
  }
})();
