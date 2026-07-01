(function () {
  "use strict";

  let loginUser = null;
  const state = {
    posts: [],
    replies: [],
    users: [],
    activeTab: "posts",
    status: "0",
    query: "",
  };

  const dom = {
    adminUserText: document.getElementById("adminUserText"),
    refreshBtn: document.getElementById("adminRefreshBtn"),
    logoutBtn: document.getElementById("adminLogoutBtn"),
    pendingCount: document.getElementById("auditPendingCount"),
    approvedCount: document.getElementById("auditApprovedCount"),
    rejectedCount: document.getElementById("auditRejectedCount"),
    userTotalCount: document.getElementById("userTotalCount"),
    userLimitedCount: document.getElementById("userLimitedCount"),
    heroCount: document.getElementById("auditHeroCount"),
    searchInput: document.getElementById("auditSearchInput"),
    statusSelect: document.getElementById("auditStatusSelect"),
    tabs: Array.from(document.querySelectorAll(".audit-tabs .filter[data-tab]")),
    aiAuditRepliesBtn: document.getElementById("aiAuditRepliesBtn"),
    postList: document.getElementById("postAuditList"),
    replyList: document.getElementById("replyAuditList"),
    userList: document.getElementById("userAuditList"),
    toast: document.getElementById("toast"),
  };

  init();

  function init() {
    bindEvents();
    checkAdmin().then((ok) => {
      if (ok) loadAll();
    });
  }

  function api(path) {
    return (window.CONTEXT_PATH || "") + path;
  }

  function bindEvents() {
    if (dom.refreshBtn) dom.refreshBtn.addEventListener("click", loadAll);
    if (dom.logoutBtn) dom.logoutBtn.addEventListener("click", logout);
    if (dom.aiAuditRepliesBtn) dom.aiAuditRepliesBtn.addEventListener("click", aiAuditPendingReplies);
    if (dom.searchInput) {
      dom.searchInput.addEventListener("input", () => {
        state.query = dom.searchInput.value.trim().toLowerCase();
        render();
      });
    }
    if (dom.statusSelect) {
      dom.statusSelect.addEventListener("change", () => {
        state.status = dom.statusSelect.value;
        loadAll();
      });
    }
    dom.tabs.forEach((button) => {
      button.addEventListener("click", () => {
        state.activeTab = button.dataset.tab;
        state.query = "";
        if (dom.searchInput) dom.searchInput.value = "";
        dom.tabs.forEach((item) => item.classList.toggle("active", item === button));
        render();
      });
    });
    if (dom.postList) dom.postList.addEventListener("click", handleActionClick);
    if (dom.replyList) dom.replyList.addEventListener("click", handleActionClick);
    if (dom.userList) dom.userList.addEventListener("click", handleUserActionClick);
  }

  function checkAdmin() {
    return fetch(api("/auth/current"), { credentials: "same-origin" })
      .then((res) => res.json())
      .then((data) => {
        if (!data.success || !data.user || Number(data.user.role) !== 2) {
          showToast("请使用管理员账号登录后进入审核台。");
          setTimeout(() => (location.href = api("/index.jsp")), 900);
          return false;
        }
        loginUser = data.user;
        localStorage.setItem("loginUser", JSON.stringify(data.user));
        if (dom.adminUserText) dom.adminUserText.textContent = `管理员：${data.user.username}`;
        return true;
      })
      .catch(() => {
        showToast("权限校验失败，请重新登录。");
        setTimeout(() => (location.href = api("/index.jsp")), 900);
        return false;
      });
  }

  function loadAll() {
    const statusParam = encodeURIComponent(state.status);
    if (dom.postList) dom.postList.innerHTML = loadingState("正在读取树洞审核队列...");
    if (dom.replyList) dom.replyList.innerHTML = loadingState("正在读取评论审核队列...");
    if (dom.userList) dom.userList.innerHTML = loadingState("正在读取用户列表...");

    Promise.all([
      fetch(api(`/api/admin/posts?status=${statusParam}`), { credentials: "same-origin" }).then((res) => res.json()),
      fetch(api(`/api/admin/replies?status=${statusParam}`), { credentials: "same-origin" }).then((res) => res.json()),
      fetch(api("/api/admin/posts?status=all"), { credentials: "same-origin" }).then((res) => res.json()),
      fetch(api("/api/admin/replies?status=all"), { credentials: "same-origin" }).then((res) => res.json()),
      fetch(api("/api/admin/users"), { credentials: "same-origin" }).then((res) => res.json()).catch(() => ({ success: false, users: [] })),
    ])
      .then(([postData, replyData, allPostData, allReplyData, userData]) => {
        if (!postData.success || !replyData.success) {
          showToast(postData.message || replyData.message || "读取失败");
          return;
        }
        state.posts = Array.isArray(postData.posts) ? postData.posts : [];
        state.replies = Array.isArray(replyData.replies) ? replyData.replies : [];
        state.users = Array.isArray(userData.users) ? userData.users : [];
        renderStats([...(allPostData.posts || []), ...(allReplyData.replies || [])], state.users);
        render();
      })
      .catch(() => {
        if (dom.postList) dom.postList.innerHTML = emptyState("审核队列读取失败，请检查接口或登录状态。");
        if (dom.replyList) dom.replyList.innerHTML = emptyState("审核队列读取失败，请检查接口或登录状态。");
        if (dom.userList) dom.userList.innerHTML = emptyState("用户列表读取失败，请检查接口或登录状态。");
      });
  }

  function renderStats(items, users) {
    const pending = items.filter((item) => Number(item.auditStatus) === 0).length;
    const approved = items.filter((item) => Number(item.auditStatus) === 1).length;
    const rejected = items.filter((item) => Number(item.auditStatus) === 2).length;
    const limited = users.filter((user) => Number(user.status) !== 1 || Number(user.muteStatus || 0) === 1).length;
    if (dom.pendingCount) dom.pendingCount.textContent = pending;
    if (dom.approvedCount) dom.approvedCount.textContent = approved;
    if (dom.rejectedCount) dom.rejectedCount.textContent = rejected;
    if (dom.userTotalCount) dom.userTotalCount.textContent = users.length;
    if (dom.userLimitedCount) dom.userLimitedCount.textContent = limited;
    if (dom.heroCount) dom.heroCount.textContent = state.activeTab === "users" ? limited : pending;
  }

  function render() {
    const isPosts = state.activeTab === "posts";
    const isReplies = state.activeTab === "replies";
    const isUsers = state.activeTab === "users";
    if (dom.postList) dom.postList.style.display = isPosts ? "grid" : "none";
    if (dom.replyList) dom.replyList.style.display = isReplies ? "grid" : "none";
    if (dom.userList) dom.userList.style.display = isUsers ? "grid" : "none";
    if (dom.statusSelect) dom.statusSelect.closest(".sort-select").style.display = isUsers ? "none" : "inline-flex";
    if (dom.aiAuditRepliesBtn) dom.aiAuditRepliesBtn.style.display = isUsers ? "none" : "inline-flex";
    if (dom.heroCount) {
      const limited = state.users.filter((user) => Number(user.status) !== 1 || Number(user.muteStatus || 0) === 1).length;
      const pending = state.posts.concat(state.replies).filter((item) => Number(item.auditStatus) === 0).length;
      dom.heroCount.textContent = isUsers ? limited : pending;
    }

    if (isPosts) {
      const posts = filterItems(state.posts, (post) => [post.title, post.body, post.category, post.mood, post.alias, post.authorUsername, ...(post.tags || [])]);
      dom.postList.innerHTML = posts.length ? posts.map(renderPostCard).join("") : emptyState("当前没有符合条件的树洞。");
    } else if (isReplies) {
      const replies = filterItems(state.replies, (reply) => [reply.body, reply.alias, reply.authorUsername, reply.postTitle, reply.postCategory]);
      dom.replyList.innerHTML = replies.length ? replies.map(renderReplyCard).join("") : emptyState("当前没有符合条件的评论。");
    } else {
    const users = filterItems(state.users, (user) => [user.username, user.nickname, roleText(user.role), userStatusText(user.status), disabledAwareMuteText(user), muteUntilText(user.muteUntil)]);
      dom.userList.innerHTML = users.length ? users.map(renderUserCard).join("") : emptyState("当前没有符合条件的用户。");
    }
  }

  function filterItems(items, fieldFactory) {
    if (!state.query) return items;
    return items.filter((item) => fieldFactory(item).join(" ").toLowerCase().includes(state.query));
  }

  function renderPostCard(post) {
    const status = statusInfo(post.auditStatus);
    const pinned = Number(post.isPinned || 0) === 1;
    const tags = Array.isArray(post.tags) && post.tags.length
      ? `<div class="tag-row">${post.tags.map((tag) => `<span class="tag">#${escapeHtml(tag)}</span>`).join("")}</div>`
      : "";
    return `
      <article class="audit-card ${pinned ? "is-pinned" : ""}" data-type="post" data-id="${escapeHtml(post.id)}" data-pinned="${pinned ? "1" : "0"}">
        <div class="audit-card-head">
          <div class="post-meta-left">
            <span class="category-pill">${escapeHtml(post.category || "日常")}</span>
            <span class="mood-pill">${escapeHtml(post.mood || "微光")}</span>
            ${pinned ? `<span class="audit-status pinned">置顶</span>` : ""}
            <span class="audit-status ${status.className}">${status.text}</span>
          </div>
          <time>${formatTime(post.createdAt)}</time>
        </div>
        <h3>${escapeHtml(post.title || "匿名碎碎念")}</h3>
        <p class="post-content">${escapeHtml(post.body || "")}</p>
        ${renderMediaGrid(post.media)}
        ${tags}
        <div class="audit-meta-line">作者：${escapeHtml(post.alias || "匿名同学")} / ${escapeHtml(post.authorUsername || "-")}　举报：${Number(post.reports || 0)}　评论：${Array.isArray(post.replies) ? post.replies.length : 0}</div>
        ${post.auditReason ? `<div class="audit-reason">驳回原因：${escapeHtml(post.auditReason)}</div>` : ""}
        ${renderActions(post.auditStatus, "post", pinned)}
      </article>
    `;
  }

  function renderReplyCard(reply) {
    const status = statusInfo(reply.auditStatus);
    return `
      <article class="audit-card" data-type="reply" data-id="${escapeHtml(reply.id)}">
        <div class="audit-card-head">
          <div class="post-meta-left">
            <span class="category-pill">评论</span>
            <span class="mood-pill">${escapeHtml(reply.postCategory || "树洞")}</span>
            <span class="audit-status ${status.className}">${status.text}</span>
          </div>
          <time>${formatTime(reply.createdAt)}</time>
        </div>
        <h3>回应于：${escapeHtml(reply.postTitle || reply.postId || "未知树洞")}</h3>
        <p class="post-content">${escapeHtml(reply.body || "")}</p>
        <div class="audit-meta-line">评论者：${escapeHtml(reply.alias || "匿名同学")} / ${escapeHtml(reply.authorUsername || "-")}</div>
        ${reply.auditReason ? `<div class="audit-reason">驳回原因：${escapeHtml(reply.auditReason)}</div>` : ""}
        ${renderActions(reply.auditStatus, "reply", false)}
      </article>
    `;
  }

  function renderUserCard(user) {
    const disabled = Number(user.status) !== 1;
    const muted = Number(user.muteStatus || 0) === 1;
    const self = loginUser && Number(loginUser.userId) === Number(user.userId);
    return `
      <article class="audit-card user-card ${disabled ? "is-disabled" : ""} ${muted ? "is-muted" : ""}" data-user-id="${escapeHtml(user.userId)}">
        <div class="audit-card-head">
          <div class="user-card-title">
            <span class="profile-avatar small">${escapeHtml((user.nickname || user.username || "匿").slice(0, 1))}</span>
            <div>
              <h3>${escapeHtml(user.nickname || user.username || "匿名同学")}</h3>
              <p>@${escapeHtml(user.username || "-")} · ${escapeHtml(roleText(user.role))}</p>
            </div>
          </div>
          <div class="post-meta-left">
            <span class="audit-status ${disabled ? "reject" : "ok"}">${escapeHtml(userStatusText(user.status))}</span>
            ${disabled ? "" : `<span class="audit-status ${muted ? "pending" : "ok"}">${escapeHtml(muteText(user.muteStatus, user.muteUntil))}</span>`}
          </div>
        </div>
        <div class="user-admin-grid">
          <span><b>${escapeHtml(user.userId || "-")}</b><i>账号编号</i></span>
          <span><b>${formatTime(user.createTime)}</b><i>注册时间</i></span>
          <span><b>${muted && !disabled ? escapeHtml(muteUntilText(user.muteUntil)) : self ? "当前管理员" : "可管理"}</b><i>${muted && !disabled ? "禁言期限" : "操作范围"}</i></span>
        </div>
        <div class="audit-actions">
          <button class="action-btn danger" type="button" data-user-action="status" data-next="${disabled ? "1" : "0"}" ${self ? "disabled" : ""}>${disabled ? "解除封号" : "封号"}</button>
          ${disabled ? "" : `<button class="action-btn ghost" type="button" data-user-action="mute" data-next="${muted ? "0" : "1"}" ${self ? "disabled" : ""}>${muted ? "解除禁言" : "禁言"}</button>`}
        </div>
      </article>
    `;
  }

  function renderActions(auditStatus, type, pinned) {
    const status = Number(auditStatus);
    const isReply = type === "reply";
    return `
      <div class="audit-actions">
        ${isReply && status === 0 ? `<button class="action-btn ai" type="button" data-action="aiAudit">AI审核</button>` : ""}
        ${status !== 1 ? `<button class="action-btn approve" type="button" data-action="approve">通过</button>` : ""}
        ${status !== 2 ? `<button class="action-btn danger" type="button" data-action="reject">驳回</button>` : ""}
        ${!isReply && status === 1 ? `<button class="action-btn pin" type="button" data-action="pin" data-next="${pinned ? "0" : "1"}">${pinned ? "取消置顶" : "置顶"}</button>` : ""}
        <button class="action-btn ghost" type="button" data-action="delete">删除</button>
      </div>
    `;
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

  function handleActionClick(event) {
    const button = event.target.closest("button[data-action]");
    if (!button) return;
    const card = event.target.closest(".audit-card");
    if (!card) return;

    const type = card.dataset.type;
    const id = card.dataset.id;
    const action = button.dataset.action;

    if (action === "aiAudit") {
      if (type !== "reply") return;
      requestAction("reply", "ai-audit", { id });
      return;
    }

    if (action === "delete") {
      if (!confirm("确定删除这条内容吗？删除后不会在前台和审核台显示。")) return;
      requestAction(type, "delete", { id });
      return;
    }

    if (action === "approve") {
      requestAction(type, "audit", { id, status: 1, reason: "" });
      return;
    }

    if (action === "reject") {
      const reason = prompt("请输入驳回原因（可为空）：", "内容不适合展示");
      if (reason === null) return;
      requestAction(type, "audit", { id, status: 2, reason });
      return;
    }

    if (action === "pin") {
      requestAction("post", "pin", { id, pinned: Number(button.dataset.next) });
    }
  }

  function handleUserActionClick(event) {
    const button = event.target.closest("button[data-user-action]");
    if (!button || button.disabled) return;
    const card = event.target.closest(".user-card");
    if (!card) return;
    const userId = Number(card.dataset.userId);
    const next = Number(button.dataset.next);
    if (button.dataset.userAction === "status") {
      requestAction("user", "status", { userId, status: next });
    } else if (button.dataset.userAction === "mute") {
      if (next === 1) {
        const durationMinutes = askMuteDuration();
        if (durationMinutes === null) return;
        requestAction("user", "mute", { userId, muted: next, durationMinutes });
      } else {
        requestAction("user", "mute", { userId, muted: next, durationMinutes: 0 });
      }
    }
  }

  function askMuteDuration() {
    const answer = prompt("选择禁言时长：1=30分钟，2=2小时，3=24小时，4=永久；也可以直接输入分钟数。", "1");
    if (answer === null) return null;
    const text = answer.trim();
    if (text === "1") return 30;
    if (text === "2") return 120;
    if (text === "3") return 1440;
    if (text === "4") return 0;
    const minutes = Number(text);
    if (!Number.isFinite(minutes) || minutes < 0) {
      showToast("禁言时长格式不正确");
      return null;
    }
    return Math.round(minutes);
  }

  function aiAuditPendingReplies() {
    if (!confirm("确定调用 AI 审核所有待审评论吗？")) return;
    setButtonLoading(dom.aiAuditRepliesBtn, true, "AI审核中...");
    fetch(api("/api/admin/replies/ai-audit-pending"), {
      method: "POST",
      credentials: "same-origin",
      headers: { "Content-Type": "application/json;charset=UTF-8" },
      body: JSON.stringify({}),
    })
      .then((res) => res.json())
      .then((data) => {
        showToast(data.message || (data.success ? "AI审核完成" : "AI审核失败"));
        if (data.success) loadAll();
      })
      .catch(() => showToast("AI审核接口调用失败"))
      .finally(() => setButtonLoading(dom.aiAuditRepliesBtn, false, "AI审核待审评论"));
  }

  function setButtonLoading(button, loading, text) {
    if (!button) return;
    button.disabled = loading;
    button.textContent = text;
  }

  function requestAction(type, action, body) {
    fetch(api(`/api/admin/${type}/${action}`), {
      method: "POST",
      credentials: "same-origin",
      headers: { "Content-Type": "application/json;charset=UTF-8" },
      body: JSON.stringify(body),
    })
      .then((res) => res.json())
      .then((data) => {
        showToast(data.message || (data.success ? "操作成功" : "操作失败"));
        if (data.success) loadAll();
      })
      .catch(() => showToast("网络错误，请稍后再试"));
  }

  function logout() {
    fetch(api("/auth/logout"), { method: "POST", credentials: "same-origin" }).finally(() => {
      localStorage.removeItem("loginUser");
      location.href = api("/index.jsp");
    });
  }

  function statusInfo(value) {
    const status = Number(value);
    if (status === 1) return { text: "已通过", className: "ok" };
    if (status === 2) return { text: "已驳回", className: "reject" };
    return { text: "待审核", className: "pending" };
  }

  function roleText(value) {
    return Number(value) === 2 ? "管理员" : "普通用户";
  }

  function userStatusText(value) {
    return Number(value) === 1 ? "正常" : "已封号";
  }

  function muteText(value, muteUntil) {
    if (Number(value) !== 1) return "可发言";
    return muteUntil ? "已禁言至 " + formatShortTime(muteUntil) : "永久禁言";
  }

  function disabledAwareMuteText(user) {
    return Number(user.status) === 1 ? muteText(user.muteStatus, user.muteUntil) : "";
  }

  function muteUntilText(value) {
    return value ? formatShortTime(value) : "永久";
  }

  function loadingState(text) {
    return `<div class="empty-state">${escapeHtml(text)}</div>`;
  }

  function emptyState(text) {
    return `<div class="empty-state">${escapeHtml(text)}</div>`;
  }

  function formatTime(value) {
    if (!value) return "刚刚";
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return "刚刚";
    const y = date.getFullYear();
    const m = String(date.getMonth() + 1).padStart(2, "0");
    const d = String(date.getDate()).padStart(2, "0");
    const hh = String(date.getHours()).padStart(2, "0");
    const mm = String(date.getMinutes()).padStart(2, "0");
    return `${y}-${m}-${d} ${hh}:${mm}`;
  }

  function formatShortTime(value) {
    if (!value) return "永久";
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return "永久";
    const m = String(date.getMonth() + 1).padStart(2, "0");
    const d = String(date.getDate()).padStart(2, "0");
    const hh = String(date.getHours()).padStart(2, "0");
    const mm = String(date.getMinutes()).padStart(2, "0");
    return `${m}-${d} ${hh}:${mm}`;
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
    if (!dom.toast) return;
    dom.toast.textContent = message;
    dom.toast.classList.add("show");
    window.clearTimeout(showToast.timer);
    showToast.timer = window.setTimeout(() => dom.toast.classList.remove("show"), 2400);
  }
})();
