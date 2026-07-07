(function () {
  "use strict";

  const EDIT_POST_KEY = "campus-treehole-edit-post-v1";

  let loginUser = null;
  const state = {
    posts: [],
    interactedPosts: [],
    activeTab: "mine",
    viewedUsername: new URLSearchParams(location.search).get("user") || "",
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
    avatarBtn: document.getElementById("profileAvatarBtn"),
    avatarInput: document.getElementById("profileAvatarInput"),
    accountMeta: document.getElementById("profileAccountMeta"),
    accountList: document.getElementById("profileAccountList"),
    privacySetting: document.getElementById("profilePrivacySetting"),
    profilePublicToggle: document.getElementById("profilePublicToggle"),
    scoreboard: document.getElementById("profileScoreboard"),
    posts: document.getElementById("profilePosts"),
    tabButtons: Array.from(document.querySelectorAll("[data-profile-tab]")),
    toast: document.getElementById("toast"),
  };

  init();

  function init() {
    const target = new URLSearchParams(location.search).get("user");
    if (target) {
      state.viewedUsername = target.trim();
      sessionStorage.setItem("campus-treehole-profile-target", state.viewedUsername);
    } else {
      sessionStorage.removeItem("campus-treehole-profile-target");
    }
    bindEvents();
    syncCurrentUser();
  }

  function api(path) {
    return (window.CONTEXT_PATH || "") + path;
  }

  function bindEvents() {
    if (dom.logoutBtn) dom.logoutBtn.addEventListener("click", logout);
    if (dom.avatar) dom.avatar.addEventListener("click", pickAvatar);
    if (dom.avatarBtn) dom.avatarBtn.addEventListener("click", pickAvatar);
    if (dom.avatarInput) dom.avatarInput.addEventListener("change", handleAvatarChange);
    if (dom.profilePublicToggle) dom.profilePublicToggle.addEventListener("click", toggleProfilePublic);
    if (dom.posts) dom.posts.addEventListener("click", handlePostListClick);
    dom.tabButtons.forEach((button) => {
      button.addEventListener("click", () => {
        state.activeTab = button.dataset.profileTab || "mine";
        dom.tabButtons.forEach((item) => item.classList.toggle("active", item === button));
        renderPosts();
      });
    });
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
        if (isViewingPublicProfile()) {
          renderPublicHeader(state.viewedUsername);
          loadAuthorPosts(state.viewedUsername);
        } else {
          renderUserHeader();
          loadMinePosts();
          loadInteractedPosts();
        }
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

  function loadInteractedPosts() {
    fetch(api("/api/posts/interacted"), { credentials: "same-origin" })
      .then((res) => res.json())
      .then((data) => {
        state.interactedPosts = normalizePosts(Array.isArray(data) ? data : []);
        if (state.activeTab === "interacted") renderPosts();
      })
      .catch(() => {
        state.interactedPosts = [];
      });
  }

  function loadAuthorPosts(username) {
    dom.tabButtons.forEach((button) => {
      button.classList.toggle("active", button.dataset.profileTab === "mine");
      if (button.dataset.profileTab === "interacted") button.style.display = "none";
    });
    state.activeTab = "mine";
    dom.posts.innerHTML = `<div class="profile-empty">正在读取公开主页…</div>`;
    fetch(api(`/api/posts/author?username=${encodeURIComponent(username)}`), { credentials: "same-origin" })
      .then((res) => res.json())
      .then((data) => {
        state.posts = normalizePosts(Array.isArray(data) ? data : []);
        renderPublicHeader(username);
        render();
      })
      .catch(() => {
        dom.posts.innerHTML = `<div class="profile-empty">公开主页读取失败，请稍后再试。</div>`;
      });
  }

  function isViewingPublicProfile() {
    return !!state.viewedUsername;
  }

  function renderPublicHeader(username) {
    if (dom.userText) dom.userText.textContent = `主页：${username}`;
    if (dom.adminAuditLink) dom.adminAuditLink.style.display = Number(loginUser.role) === 2 ? "inline-flex" : "none";
    if (dom.accountMeta) dom.accountMeta.textContent = `@${username} · 公开主页`;
    if (dom.privacySetting) dom.privacySetting.style.display = "none";
    if (dom.avatarBtn) dom.avatarBtn.style.display = "none";
    if (dom.avatar) dom.avatar.textContent = username.slice(0, 1).toUpperCase();
  }

  function renderUserHeader() {
    const roleName = Number(loginUser.role) === 2 ? "管理员" : "普通用户";
    dom.userText.textContent = `${roleName}：${loginUser.username}`;
    renderAvatar();
    if (dom.accountMeta) {
      dom.accountMeta.textContent = `${loginUser.nickname || loginUser.username} · @${loginUser.username} · ${roleName}`;
    }
    if (dom.adminAuditLink) dom.adminAuditLink.style.display = Number(loginUser.role) === 2 ? "inline-flex" : "none";
    renderPrivacySetting();
  }

  function render() {
    const summary = getSummary();
    renderStats(summary);
    renderAccount();
    renderPosts();
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
    if (!dom.accountList) return;
    if (isViewingPublicProfile()) {
      const accountItems = [
        ["用户名", `@${state.viewedUsername}`],
        ["身份", "公开主页"],
        ["发布", `${state.posts.length}`],
      ];
      dom.accountList.innerHTML = accountItems
        .map(([label, value]) => `<div><span>${escapeHtml(label)}</span><strong>${escapeHtml(value)}</strong></div>`)
        .join("");
      return;
    }
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

  function renderPosts() {
    const posts = state.activeTab === "interacted" ? state.interactedPosts : state.posts;
    if (!posts.length) {
      dom.posts.innerHTML =
        state.activeTab === "interacted"
          ? `<div class="profile-empty">还没有点赞或抱抱过的树洞。遇到被你接住的内容，它会出现在这里。</div>`
          : `<div class="profile-empty">还没有自己的树洞。回到广场写下一句，这里会替你收好它。</div>`;
      return;
    }
    dom.posts.innerHTML = posts.map((post) => renderProfilePost(post, state.activeTab)).join("");
  }

  function renderProfilePost(post, mode) {
    const status = Number(post.auditStatus);
    const statusText = status === 1 ? "已通过" : status === 2 ? "已驳回" : "待审核";
    const statusClass = status === 1 ? "ok" : status === 2 ? "reject" : "pending";
    const likes = getOtherInteractionCount(post, "likes", "likedBy");
    const hugs = getOtherInteractionCount(post, "hugs", "huggedBy");
    const replies = Array.isArray(post.replies) ? post.replies.length : 0;
    const interactionMarks = mode === "interacted" ? renderInteractionMarks(post) : "";

    return `
      <article class="profile-post-card" data-post-id="${escapeHtml(post.id)}">
        <div class="profile-post-head">
          <span class="profile-status ${statusClass}">${mode === "interacted" ? "互动过" : statusText}</span>
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
        ${interactionMarks}
        ${post.auditReason ? `<div class="audit-reason">审核说明：${escapeHtml(post.auditReason)}</div>` : ""}
        ${mode === "mine" && status === 2 ? `<div class="profile-post-actions"><button class="auth-btn admin" type="button" data-edit-post="${escapeHtml(post.id)}">重新编辑</button></div>` : ""}
      </article>
    `;
  }

  function renderInteractionMarks(post) {
    const marks = [];
    if (Array.isArray(post.likedBy) && post.likedBy.includes(loginUser.username)) marks.push("点过赞");
    if (Array.isArray(post.huggedBy) && post.huggedBy.includes(loginUser.username)) marks.push("抱抱过");
    return marks.length ? `<div class="profile-post-meta is-interaction">${marks.map((mark) => `<span>${mark}</span>`).join("")}</div>` : "";
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
      isPinned: Number(post.isPinned || 0),
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

  function renderAvatar() {
    if (!dom.avatar) return;
    if (loginUser && loginUser.avatarUrl) {
      dom.avatar.innerHTML = `<img src="${escapeHtml(loginUser.avatarUrl)}" alt="当前头像" />`;
    } else {
      dom.avatar.textContent = getAvatarText();
    }
  }

  function isProfilePublic() {
    return !loginUser || loginUser.profilePublic === undefined || Number(loginUser.profilePublic) !== 0;
  }

  function renderPrivacySetting() {
    if (!dom.privacySetting || !dom.profilePublicToggle) return;
    dom.privacySetting.style.display = isViewingPublicProfile() ? "none" : "flex";
    const enabled = isProfilePublic();
    dom.profilePublicToggle.classList.toggle("is-off", !enabled);
    dom.profilePublicToggle.textContent = enabled ? "已公开" : "不公开";
    dom.profilePublicToggle.setAttribute("aria-pressed", String(enabled));
  }

  function toggleProfilePublic() {
    if (!loginUser || isViewingPublicProfile()) return;
    const nextValue = isProfilePublic() ? 0 : 1;
    fetch(api("/auth/profile-visibility"), {
      method: "POST",
      credentials: "same-origin",
      headers: { "Content-Type": "application/json;charset=UTF-8" },
      body: JSON.stringify({ profilePublic: nextValue }),
    })
      .then((res) => res.json())
      .then((data) => {
        if (!data.success) {
          showToast(data.message || "个人主页设置更新失败");
          return;
        }
        loginUser = data.user || { ...loginUser, profilePublic: nextValue };
        localStorage.setItem("loginUser", JSON.stringify(loginUser));
        renderUserHeader();
        renderAccount();
        showToast(data.message || "个人主页设置已更新");
      })
      .catch(() => showToast("网络错误，请稍后再试"));
  }

  function pickAvatar() {
    if (dom.avatarInput) dom.avatarInput.click();
  }

  function handleAvatarChange(event) {
    const file = event.target.files && event.target.files[0];
    if (!file) return;
    if (!file.type || !file.type.startsWith("image/")) {
      showToast("头像只支持图片文件。");
      dom.avatarInput.value = "";
      return;
    }
    if (file.size > 1500 * 1024) {
      showToast("头像图片不能超过 1.5MB。");
      dom.avatarInput.value = "";
      return;
    }

    const reader = new FileReader();
    reader.onload = () => updateAvatar(reader.result);
    reader.onerror = () => showToast("头像读取失败，请换一张图片。");
    reader.readAsDataURL(file);
  }

  function updateAvatar(avatarUrl) {
    fetch(api("/auth/avatar"), {
      method: "POST",
      credentials: "same-origin",
      headers: { "Content-Type": "application/json;charset=UTF-8" },
      body: JSON.stringify({ avatarUrl }),
    })
      .then((res) => res.json())
      .then((data) => {
        if (!data.success) {
          showToast(data.message || "头像更新失败");
          return;
        }
        loginUser = data.user || { ...loginUser, avatarUrl };
        localStorage.setItem("loginUser", JSON.stringify(loginUser));
        renderUserHeader();
        showToast("头像已更新。");
      })
      .catch(() => showToast("网络错误，请稍后再试"))
      .finally(() => {
        if (dom.avatarInput) dom.avatarInput.value = "";
      });
  }

  function handlePostListClick(event) {
    if (isViewingPublicProfile()) return;
    const button = event.target.closest("[data-edit-post]");
    if (!button) return;
    const post = state.posts.find((item) => item.id === button.dataset.editPost);
    if (!post) return;
    localStorage.setItem(
      EDIT_POST_KEY,
      JSON.stringify({
        id: post.id,
        title: post.title || "",
        body: post.body || "",
        category: post.category || "日常",
        mood: post.mood || "微光",
        alias: post.alias || "匿名同学",
        tags: post.tags || [],
        media: normalizeMedia(post.media),
      }),
    );
    showToast("已放回发布框，正在前往编辑。");
    window.setTimeout(() => {
      location.href = api("/index.jsp");
    }, 500);
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
