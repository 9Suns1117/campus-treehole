(function () {
  "use strict";

  let loginUser = null;
  let chatTimer = null;

  const state = {
    listeners: [],
    profile: null,
    incoming: [],
    outgoing: [],
    query: "",
    requestPanel: "incoming",
    chatRequestId: null,
    chatMessages: [],
  };

  const dom = {
    userText: document.getElementById("listenerUserText"),
    listenerCount: document.getElementById("listenerCount"),
    requestCount: document.getElementById("requestCount"),
    thanksCount: document.getElementById("thanksCount"),
    heroCount: document.getElementById("listenerHeroCount"),
    applyForm: document.getElementById("listenerApplyForm"),
    reason: document.getElementById("listenerReason"),
    bio: document.getElementById("listenerBio"),
    topics: document.getElementById("listenerTopics"),
    availableTime: document.getElementById("listenerAvailableTime"),
    statusTitle: document.getElementById("listenerStatusTitle"),
    statusText: document.getElementById("listenerStatusText"),
    search: document.getElementById("listenerSearch"),
    list: document.getElementById("listenerList"),
    openIncoming: document.getElementById("openIncomingRequests"),
    openOutgoing: document.getElementById("openOutgoingRequests"),
    requestsModal: document.getElementById("listenerRequestsModal"),
    requestsClose: document.getElementById("listenerRequestsClose"),
    requestsTitle: document.getElementById("listenerRequestsTitle"),
    requestsEyebrow: document.getElementById("listenerRequestsEyebrow"),
    requestsList: document.getElementById("listenerRequestsList"),
    modal: document.getElementById("listenerModal"),
    modalClose: document.getElementById("listenerModalClose"),
    requestForm: document.getElementById("listenerRequestForm"),
    targetUsername: document.getElementById("listenerTargetUsername"),
    requestTopic: document.getElementById("requestTopic"),
    requestMessage: document.getElementById("requestMessage"),
    requestMode: document.getElementById("requestMode"),
    chatModal: document.getElementById("listenerChatModal"),
    chatClose: document.getElementById("listenerChatClose"),
    chatTitle: document.getElementById("listenerChatTitle"),
    chatMeta: document.getElementById("listenerChatMeta"),
    chatMessages: document.getElementById("listenerChatMessages"),
    chatForm: document.getElementById("listenerChatForm"),
    chatInput: document.getElementById("listenerChatInput"),
    toast: document.getElementById("toast"),
  };

  init();

  function api(path) {
    return (window.CONTEXT_PATH || "") + path;
  }

  function init() {
    bindEvents();
    fetch(api("/auth/current"), { credentials: "same-origin" })
      .then((res) => res.json())
      .then((data) => {
        loginUser = data.success ? data.user : null;
        renderUser();
        return loadAll();
      })
      .catch(() => {
        loginUser = null;
        renderUser();
        loadAll();
      });
  }

  function bindEvents() {
    if (dom.applyForm) dom.applyForm.addEventListener("submit", submitApplication);
    if (dom.search) {
      dom.search.addEventListener("input", () => {
        state.query = dom.search.value.trim().toLowerCase();
        renderListeners();
      });
    }
    if (dom.list) dom.list.addEventListener("click", handleListenerClick);
    if (dom.openIncoming) dom.openIncoming.addEventListener("click", () => openRequestPanel("incoming"));
    if (dom.openOutgoing) dom.openOutgoing.addEventListener("click", () => openRequestPanel("outgoing"));
    if (dom.requestsList) dom.requestsList.addEventListener("click", handleRequestPanelClick);
    if (dom.requestsClose) dom.requestsClose.addEventListener("click", closeRequestsModal);
    if (dom.requestsModal) {
      dom.requestsModal.addEventListener("click", (event) => {
        if (event.target === dom.requestsModal) closeRequestsModal();
      });
    }
    if (dom.modalClose) dom.modalClose.addEventListener("click", closeRequestModal);
    if (dom.modal) {
      dom.modal.addEventListener("click", (event) => {
        if (event.target === dom.modal) closeRequestModal();
      });
    }
    if (dom.requestForm) dom.requestForm.addEventListener("submit", submitRequest);
    if (dom.chatClose) dom.chatClose.addEventListener("click", closeChat);
    if (dom.chatModal) {
      dom.chatModal.addEventListener("click", (event) => {
        if (event.target === dom.chatModal) closeChat();
      });
    }
    if (dom.chatForm) dom.chatForm.addEventListener("submit", submitChatMessage);
  }

  function loadAll() {
    return Promise.all([
      fetch(api("/api/listener/plaza"), { credentials: "same-origin" }).then((res) => res.json()).catch(() => ({ listeners: [] })),
      fetch(api("/api/listener/me"), { credentials: "same-origin" }).then((res) => res.json()).catch(() => ({ success: false })),
    ]).then(([plaza, me]) => {
      state.listeners = Array.isArray(plaza.listeners) ? plaza.listeners : [];
      state.profile = me.profile || null;
      state.incoming = Array.isArray(me.incoming) ? me.incoming : [];
      state.outgoing = Array.isArray(me.outgoing) ? me.outgoing : [];
      render();
    });
  }

  function render() {
    renderStats();
    renderApplication();
    renderListeners();
    renderRequestPanel();
  }

  function renderUser() {
    if (!dom.userText) return;
    dom.userText.textContent = loginUser ? `${roleLabel(loginUser)}：${loginUser.nickname || loginUser.username}` : "游客";
  }

  function renderStats() {
    const thanks = state.listeners.reduce((sum, item) => sum + Number(item.thanksCount || 0), 0);
    if (dom.listenerCount) dom.listenerCount.textContent = state.listeners.length;
    if (dom.requestCount) dom.requestCount.textContent = state.incoming.length + state.outgoing.length;
    if (dom.thanksCount) dom.thanksCount.textContent = thanks;
    if (dom.heroCount) dom.heroCount.textContent = state.listeners.length;
  }

  function renderApplication() {
    const profile = state.profile;
    const status = profile ? Number(profile.status) : -1;
    if (dom.statusTitle) {
      dom.statusTitle.textContent = status === 1 ? "已成为倾听者" : status === 0 ? "申请待审核" : status === 2 ? "申请被驳回" : "尚未申请";
      dom.statusTitle.className = `audit-status ${status === 1 ? "ok" : status === 2 ? "reject" : "pending"}`;
    }
    if (dom.statusText) {
      dom.statusText.textContent = status === 1
        ? `已倾听 ${profile.listenCount || 0} 次，收到 ${profile.thanksCount || 0} 张感谢卡，温柔值 ${profile.warmth || 0}。`
        : status === 0
          ? "你的申请正在等待管理员审核。"
          : status === 2
            ? `审核说明：${profile.auditReason || "暂未填写"}，你可以修改后重新提交。`
            : "填写申请理由、擅长话题和可在线时间后提交审核。";
    }
    if (profile) {
      if (dom.reason) dom.reason.value = profile.reason || "";
      if (dom.bio) dom.bio.value = profile.bio || "";
      if (dom.topics) dom.topics.value = profile.topics || "";
      if (dom.availableTime) dom.availableTime.value = profile.availableTime || "";
    }
  }

  function renderListeners() {
    if (!dom.list) return;
    const query = state.query;
    const listeners = query
      ? state.listeners.filter((item) => [item.nickname, item.username, item.bio, item.topics, item.availableTime].join(" ").toLowerCase().includes(query))
      : state.listeners;
    dom.list.innerHTML = listeners.length ? listeners.map(renderListenerCard).join("") : emptyState("暂时还没有通过审核的倾听者。");
  }

  function renderListenerCard(item) {
    return `
      <article class="hole-card listener-card" data-username="${escapeHtml(item.username)}">
        <div class="post-inner">
          <div class="audit-card-head">
            <div class="user-card-title">
              <span class="profile-avatar small">${avatarText(item)}</span>
              <div>
                <h3>${escapeHtml(item.nickname || item.username)}</h3>
                <p>@${escapeHtml(item.username)} · 月光倾听者</p>
              </div>
            </div>
            <span class="audit-status ok">温柔值 ${Number(item.warmth || 0)}</span>
          </div>
          <p class="post-content">${escapeHtml(item.bio || item.reason || "愿意认真接住一些难说的话。")}</p>
          <div class="tag-row">${splitTags(item.topics).map((tag) => `<span class="tag">#${escapeHtml(tag)}</span>`).join("")}</div>
          <div class="user-admin-grid">
            <span><b>${Number(item.listenCount || 0)}</b><i>已倾听</i></span>
            <span><b>${Number(item.thanksCount || 0)}</b><i>感谢卡</i></span>
            <span><b>${escapeHtml(item.availableTime || "未填写")}</b><i>在线时间</i></span>
          </div>
          <div class="audit-actions">
            <button class="auth-btn admin" type="button" data-action="request">找 TA 说说</button>
          </div>
        </div>
      </article>
    `;
  }

  function openRequestPanel(type) {
    if (!loginUser) return showToast("请先登录后查看请求");
    state.requestPanel = type === "outgoing" ? "outgoing" : "incoming";
    renderRequestPanel();
    openModal(dom.requestsModal);
  }

  function renderRequestPanel() {
    if (!dom.requestsList) return;
    const incoming = state.requestPanel !== "outgoing";
    const requests = incoming ? state.incoming : state.outgoing;
    if (dom.requestsTitle) dom.requestsTitle.textContent = incoming ? "我收到的请求" : "我发出的请求";
    if (dom.requestsEyebrow) dom.requestsEyebrow.textContent = incoming ? "倾听工作台" : "我的倾听请求";
    dom.requestsList.innerHTML = requests.length ? requests.map((item) => renderRequestCard(item, incoming)).join("") : emptyState(incoming ? "还没有发给你的倾听请求。" : "你还没有发出倾听请求。");
  }

  function renderRequestCard(item, incoming) {
    const name = incoming ? item.requesterNickname || item.requesterUsername : item.listenerNickname || item.listenerUsername;
    const canChat = Number(item.status) !== 2;
    return `
      <article class="listener-request-card" data-id="${item.id}">
        <span class="audit-status ${requestStatusClass(item.status)}">${requestStatusText(item.status)}</span>
        <h4>${escapeHtml(item.topic || "想说说话")}</h4>
        <p>${escapeHtml(item.message || "")}</p>
        <p class="audit-meta-line">${incoming ? "来自" : "发给"}：${escapeHtml(name || "同学")} · ${escapeHtml(item.responseMode || "只想被倾听")}</p>
        ${item.replyText ? `<div class="audit-reason">倾听者回复：${escapeHtml(item.replyText)}</div>` : ""}
        <div class="audit-actions">
          ${incoming ? incomingActions(item) : outgoingActions(item)}
          ${canChat ? `<button class="action-btn ai" data-action="chat">打开对话</button>` : ""}
        </div>
      </article>
    `;
  }

  function incomingActions(item) {
    const status = Number(item.status);
    if (status === 0) return `<button class="action-btn approve" data-action="accept">接单</button><button class="action-btn ghost" data-action="reject">拒绝</button>`;
    if (status === 1 || status === 3) return `<button class="action-btn approve" data-action="complete">完成倾听</button>`;
    return "";
  }

  function outgoingActions(item) {
    if (Number(item.status) === 4 && Number(item.thanksSent || 0) !== 1) return `<button class="auth-btn admin" data-action="thanks">送感谢卡</button>`;
    return Number(item.thanksSent || 0) === 1 ? `<span class="audit-status ok">已送感谢卡</span>` : "";
  }

  function submitApplication(event) {
    event.preventDefault();
    if (!loginUser) return showToast("请先登录后再申请");
    post("/api/listener/apply", {
      reason: value(dom.reason),
      bio: value(dom.bio),
      topics: value(dom.topics),
      availableTime: value(dom.availableTime),
    }).then((data) => {
      showToast(data.message || "已提交");
      loadAll();
    });
  }

  function handleListenerClick(event) {
    const button = event.target.closest("[data-action='request']");
    if (!button) return;
    if (!loginUser) return showToast("请先登录后再发起倾听请求");
    const card = button.closest("[data-username]");
    if (dom.targetUsername) dom.targetUsername.value = card.dataset.username;
    openModal(dom.modal);
  }

  function submitRequest(event) {
    event.preventDefault();
    post("/api/listener/request", {
      listenerUsername: value(dom.targetUsername),
      topic: value(dom.requestTopic),
      message: value(dom.requestMessage),
      responseMode: value(dom.requestMode),
    }).then((data) => {
      showToast(data.message || "已送达");
      if (data.success) {
        if (dom.requestForm) dom.requestForm.reset();
        closeRequestModal();
        loadAll();
      }
    });
  }

  function handleRequestPanelClick(event) {
    const button = event.target.closest("[data-action]");
    if (!button) return;
    const card = button.closest("[data-id]");
    const id = card ? card.dataset.id : "";
    const action = button.dataset.action;
    if (action === "chat") return openChat(id);
    if (action === "thanks") return sendThanks(id);
    const statusMap = { accept: 1, reject: 2, complete: 4 };
    if (statusMap[action] !== undefined) updateRequest(id, statusMap[action]);
  }

  function updateRequest(id, status) {
    post("/api/listener/request/status", { id, status }).then((data) => {
      showToast(data.message || "操作完成");
      loadAll();
    });
  }

  function sendThanks(id) {
    post("/api/listener/thanks", { id }).then((data) => {
      showToast(data.message || "已送出");
      loadAll();
    });
  }

  function openChat(id) {
    state.chatRequestId = id;
    const request = state.incoming.concat(state.outgoing).find((item) => String(item.id) === String(id));
    if (dom.chatTitle) dom.chatTitle.textContent = request ? request.topic || "月光小窗" : "月光小窗";
    if (dom.chatMeta && request) {
      const other = request.listenerUsername === (loginUser && loginUser.username)
        ? request.requesterNickname || request.requesterUsername
        : request.listenerNickname || request.listenerUsername;
      dom.chatMeta.textContent = `和 ${other || "同学"} 的倾听对话 · ${requestStatusText(request.status)}`;
    }
    openModal(dom.chatModal);
    loadMessages();
    clearInterval(chatTimer);
    chatTimer = setInterval(loadMessages, 3000);
    if (dom.chatInput) dom.chatInput.focus();
  }

  function closeChat() {
    clearInterval(chatTimer);
    chatTimer = null;
    state.chatRequestId = null;
    closeModal(dom.chatModal);
  }

  function loadMessages() {
    if (!state.chatRequestId || !dom.chatMessages) return;
    fetch(api(`/api/listener/messages?id=${encodeURIComponent(state.chatRequestId)}`), { credentials: "same-origin" })
      .then((res) => res.json())
      .then((data) => {
        state.chatMessages = Array.isArray(data.messages) ? data.messages : [];
        renderMessages();
      })
      .catch(() => {});
  }

  function renderMessages() {
    if (!dom.chatMessages) return;
    dom.chatMessages.innerHTML = state.chatMessages.length
      ? state.chatMessages.map((item) => {
          const mine = loginUser && item.senderUsername === loginUser.username;
          return `<div class="listener-chat-bubble ${mine ? "mine" : ""}">
            <span>${escapeHtml(item.senderNickname || item.senderUsername || "同学")}</span>
            <p>${escapeHtml(item.message || "")}</p>
          </div>`;
        }).join("")
      : `<div class="empty-state">还没有消息，先说一句吧。</div>`;
    dom.chatMessages.scrollTop = dom.chatMessages.scrollHeight;
  }

  function submitChatMessage(event) {
    event.preventDefault();
    const message = value(dom.chatInput);
    if (!state.chatRequestId || !message) return;
    post("/api/listener/message", { id: state.chatRequestId, message }).then((data) => {
      if (!data.success) return showToast(data.message || "发送失败");
      if (dom.chatInput) dom.chatInput.value = "";
      loadMessages();
    });
  }

  function post(path, body) {
    return fetch(api(path), {
      method: "POST",
      credentials: "same-origin",
      headers: { "Content-Type": "application/json;charset=UTF-8" },
      body: JSON.stringify(body || {}),
    }).then((res) => res.json());
  }

  function openModal(modal) {
    if (!modal) return;
    modal.classList.add("show");
    modal.setAttribute("aria-hidden", "false");
  }

  function closeModal(modal) {
    if (!modal) return;
    modal.classList.remove("show");
    modal.setAttribute("aria-hidden", "true");
  }

  function closeRequestModal() {
    closeModal(dom.modal);
  }

  function closeRequestsModal() {
    closeModal(dom.requestsModal);
  }

  function splitTags(value) {
    return String(value || "").split(/[,\s，、]+/).filter(Boolean).slice(0, 6);
  }

  function avatarText(item) {
    return escapeHtml((item.nickname || item.username || "月").slice(0, 1));
  }

  function requestStatusText(status) {
    return ["待接单", "已接单", "已拒绝", "对话中", "已完成"][Number(status)] || "未知";
  }

  function requestStatusClass(status) {
    return Number(status) === 2 ? "reject" : Number(status) === 0 ? "pending" : "ok";
  }

  function roleLabel(user) {
    return Number(user.role) === 2 ? "管理员" : "普通用户";
  }

  function value(input) {
    return input ? String(input.value || "").trim() : "";
  }

  function emptyState(text) {
    return `<div class="empty-state">${escapeHtml(text)}</div>`;
  }

  function escapeHtml(value) {
    return String(value == null ? "" : value).replace(/[&<>"']/g, (char) => ({
      "&": "&amp;",
      "<": "&lt;",
      ">": "&gt;",
      '"': "&quot;",
      "'": "&#39;",
    }[char]));
  }

  function showToast(message) {
    if (!dom.toast) return;
    dom.toast.textContent = message;
    dom.toast.classList.add("show");
    setTimeout(() => dom.toast.classList.remove("show"), 1800);
  }
})();
