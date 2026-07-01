(function () {
  "use strict";

  const DRAFT_KEY = "campus-treehole-draft-v2";
  const EDIT_POST_KEY = "campus-treehole-edit-post-v1";
  const PREVIEW_POSTS_KEY = "campus-treehole-preview-posts-v1";
  const INTERACTION_SEEN_PREFIX = "campus-treehole-interactions-seen";

  let loginUserRaw = localStorage.getItem("loginUser");
  let loginUser = safelyParse(loginUserRaw, null);

  const categoryAccent = {
    "日常": "#1d7485",
    "心情": "#7567d9",
    "求助": "#e85f4d",
    "失物": "#d99a20",
    "表白": "#31835c",
  };

  const seedPosts = [
    {
      id: "seed-1",
      title: "图书馆闭馆前的灯",
      body: "今天在三楼靠窗的位置写完了实验报告。出门时看到有人把旁边座位的草稿纸叠得很整齐，突然觉得期末周也没有那么冷。",
      category: "日常",
      mood: "微光",
      alias: "匿名同学",
      authorUsername: "user",
      tags: ["图书馆", "期末", "晚风"],
      likes: 18,
      likedBy: ["momo", "night-reader", "小鹿"],
      hugs: 9,
      huggedBy: ["小鹿", "晚风"],
      reports: 0,
      reportedBy: [],
      auditStatus: 1,
      createdAt: minutesAgo(28),
      replies: [
        { id: "r-1", body: "三楼窗边真的很适合回血。", alias: "匿名同学", auditStatus: 1, createdAt: minutesAgo(12) },
      ],
    },
    {
      id: "seed-2",
      title: "想问问转专业面试",
      body: "有没有学长学姐参加过计算机方向的转专业面试？我准备了项目和高数，但还是有点没底。想听听真实经验。",
      category: "求助",
      mood: "需要倾听",
      alias: "小岛",
      authorUsername: "user",
      tags: ["转专业", "面试", "经验"],
      likes: 7,
      likedBy: ["momo"],
      hugs: 22,
      huggedBy: ["momo", "路过同学", "星星"],
      reports: 0,
      reportedBy: [],
      auditStatus: 1,
      createdAt: minutesAgo(74),
      replies: [],
    },
  ];

  const state = {
    posts: [],
    activeFilter: "全部",
    query: "",
    sort: "newest",
    selectedMood: "微光",
    openReplyId: null,
    serverReady: false,
    myPosts: [],
    myPostsLoaded: false,
    selectedMedia: [],
    previewPosts: safelyParse(localStorage.getItem(PREVIEW_POSTS_KEY), []),
    editingPostId: null,
  };

  const dom = {
    form: document.getElementById("postForm"),
    title: document.getElementById("postTitle"),
    body: document.getElementById("postBody"),
    category: document.getElementById("postCategory"),
    alias: document.getElementById("postAlias"),
    tags: document.getElementById("postTags"),
    media: document.getElementById("postMedia"),
    mediaPreview: document.getElementById("mediaPreview"),
    counter: document.getElementById("charCounter"),
    clearDraft: document.getElementById("clearDraftBtn"),
    moodButtons: Array.from(document.querySelectorAll(".mood")),
    filterButtons: Array.from(document.querySelectorAll(".filter")),
    search: document.getElementById("searchInput"),
    sort: document.getElementById("sortSelect"),
    feed: document.getElementById("feed"),
    toast: document.getElementById("toast"),
    statTotal: document.getElementById("statTotal"),
    statToday: document.getElementById("statToday"),
    statWarmth: document.getElementById("statWarmth"),
    sceneCount: document.getElementById("sceneCount"),
    tagCloud: document.getElementById("tagCloud"),
    currentUserText: document.getElementById("currentUserText"),
    logoutBtn: document.getElementById("logoutBtn"),
    adminAuditLink: document.getElementById("adminAuditLink"),
    profileOpenBtn: document.getElementById("profileOpenBtn"),
    interactionAlert: document.getElementById("interactionAlert"),
    interactionTitle: document.getElementById("interactionTitle"),
    interactionText: document.getElementById("interactionText"),
    interactionStats: document.getElementById("interactionStats"),
    interactionAckBtn: document.getElementById("interactionAckBtn"),
    interactionProfileBtn: document.getElementById("interactionProfileBtn"),
    aiOpenBtn: document.getElementById("aiOpenBtn"),
    aiCardBtn: document.getElementById("aiCardBtn"),
    aiModal: document.getElementById("aiModal"),
    aiCloseBtn: document.getElementById("aiCloseBtn"),
    aiMessages: document.getElementById("aiChatMessages"),
    aiForm: document.getElementById("aiChatForm"),
    aiInput: document.getElementById("aiChatInput"),
  };

  const auth = {
    modal: document.getElementById("authModal"),
    registerModal: document.getElementById("registerModal"),
    loginUserBtn: document.getElementById("loginUserBtn"),
    adminLoginBtn: document.getElementById("adminLoginBtn"),
    registerBtn: document.getElementById("registerBtn"),
    closeBtn: document.getElementById("authCloseBtn"),
    registerCloseBtn: document.getElementById("registerCloseBtn"),
    submitLoginBtn: document.getElementById("submitLoginBtn"),
    submitRegisterBtn: document.getElementById("submitRegisterBtn"),
    authTitle: document.getElementById("authTitle"),
    loginRole: document.getElementById("loginRole"),
    loginUsername: document.getElementById("loginUsername"),
    loginPassword: document.getElementById("loginPassword"),
    registerUsername: document.getElementById("registerUsername"),
    registerNickname: document.getElementById("registerNickname"),
    registerPassword: document.getElementById("registerPassword"),
    authTip: document.getElementById("authTip"),
    registerTip: document.getElementById("registerTip"),
  };

  init();

  function init() {
    renderLoginUser();
    restoreDraft();
    bindEvents();
    syncCurrentUser();
    loadPosts();
    if (loginUser) loadMinePosts();
  }

  function api(path) {
    const contextPath = window.CONTEXT_PATH || "";
    return contextPath + path;
  }

  function safelyParse(raw, fallback) {
    try {
      return raw ? JSON.parse(raw) : fallback;
    } catch (error) {
      return fallback;
    }
  }

  function minutesAgo(minutes) {
    return new Date(Date.now() - minutes * 60 * 1000).toISOString();
  }

  function syncCurrentUser() {
    fetch(api("/auth/current"), { credentials: "same-origin" })
      .then((res) => res.json())
      .then((data) => {
        if (data.success && data.user) {
          loginUser = data.user;
          localStorage.setItem("loginUser", JSON.stringify(data.user));
          loadMinePosts();
        } else {
          loginUser = null;
          state.myPosts = [];
          state.myPostsLoaded = false;
          localStorage.removeItem("loginUser");
        }
        renderLoginUser();
      })
      .catch(() => {
        renderLoginUser();
      });
  }

  function loadPosts() {
    fetch(api("/api/posts/"), { credentials: "same-origin" })
      .then((res) => res.json())
      .then((data) => {
        state.serverReady = true;
        state.posts = normalizePosts(getPreviewPosts().concat(Array.isArray(data) ? data : []));
        render();
      })
      .catch((err) => {
        console.warn("树洞数据读取失败，已使用默认数据。", err);
        state.serverReady = false;
        state.posts = normalizePosts(getPreviewPosts().concat(seedPosts));
        render();
      });
  }

  function loadMinePosts() {
    if (!loginUser) {
      state.myPosts = [];
      state.myPostsLoaded = false;
      renderPersonalSurfaces();
      return;
    }

    fetch(api("/api/posts/mine"), { credentials: "same-origin" })
      .then((res) => res.json())
      .then((data) => {
        if (Array.isArray(data)) {
          state.myPosts = normalizePosts(getPreviewPostsByCurrentUser().concat(data));
          state.myPostsLoaded = true;
        } else {
          state.myPosts = getPublicPostsByCurrentUser();
          state.myPostsLoaded = false;
        }
        renderPersonalSurfaces();
      })
      .catch(() => {
        state.myPosts = getPublicPostsByCurrentUser();
        state.myPostsLoaded = false;
        renderPersonalSurfaces();
      });
  }

  function normalizePosts(posts) {
    return posts.map((post) => ({
      ...post,
      tags: Array.isArray(post.tags) ? post.tags : [],
      likedBy: Array.isArray(post.likedBy) ? post.likedBy : [],
      huggedBy: Array.isArray(post.huggedBy) ? post.huggedBy : [],
      reportedBy: Array.isArray(post.reportedBy) ? post.reportedBy : [],
      replies: Array.isArray(post.replies) ? post.replies.filter((reply) => reply.auditStatus === undefined || reply.auditStatus === 1) : [],
      likes: Number(post.likes || 0),
      hugs: Number(post.hugs || 0),
      reports: Number(post.reports || 0),
      auditStatus: post.auditStatus === undefined ? 1 : Number(post.auditStatus),
      media: normalizeMedia(post.media),
    }));
  }

  function getPreviewPosts() {
    return Array.isArray(state.previewPosts) ? state.previewPosts : [];
  }

  function getPreviewPostsByCurrentUser() {
    if (!loginUser) return [];
    return getPreviewPosts().filter((post) => post.authorUsername === loginUser.username);
  }

  function isPreviewResponse(data) {
    return !!(data && typeof data.message === "string" && data.message.includes("预览模式"));
  }

  function savePreviewPost(post) {
    if (!loginUser) return null;
    const previewPost = {
      ...post,
      id: `preview-local-${Date.now()}`,
      authorUsername: loginUser.username,
      likes: 0,
      likedBy: [],
      hugs: post.mood === "需要倾听" ? 1 : 0,
      huggedBy: post.mood === "需要倾听" ? [loginUser.username] : [],
      reports: 0,
      reportedBy: [],
      replies: [],
      auditStatus: 1,
      isPinned: 0,
      createdAt: new Date().toISOString(),
      media: normalizeMedia(post.media),
    };
    const nextPosts = [previewPost].concat(getPreviewPosts()).slice(0, 12);
    state.previewPosts = nextPosts;
    try {
      localStorage.setItem(PREVIEW_POSTS_KEY, JSON.stringify(nextPosts));
    } catch (err) {
      console.warn("预览帖子媒体较大，已仅保留当前会话展示。", err);
      const textOnlyPosts = nextPosts.map((item) => ({ ...item, media: [] }));
      try {
        localStorage.setItem(PREVIEW_POSTS_KEY, JSON.stringify(textOnlyPosts));
      } catch (ignored) {
      }
    }
    return previewPost;
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

  function renderLoginUser() {
    const authActions = document.getElementById("authActions");
    const userActions = document.getElementById("userActions");
    if (!loginUser) {
      if (authActions) authActions.style.display = "flex";
      if (userActions) userActions.style.display = "none";
      if (dom.currentUserText) dom.currentUserText.textContent = "游客模式";
      if (dom.adminAuditLink) dom.adminAuditLink.style.display = "none";
      renderPersonalSurfaces();
      return;
    }

    if (authActions) authActions.style.display = "none";
    if (userActions) userActions.style.display = "flex";

    const roleName = Number(loginUser.role) === 2 ? "管理员" : "普通用户";
    const name = loginUser.username;
    if (dom.currentUserText) dom.currentUserText.textContent = `${roleName}：${name}`;
    if (dom.adminAuditLink) dom.adminAuditLink.style.display = Number(loginUser.role) === 2 ? "inline-flex" : "none";
    renderPersonalSurfaces();
  }

  function bindEvents() {
    if (dom.form) dom.form.addEventListener("submit", handlePostSubmit);
    if (dom.logoutBtn) dom.logoutBtn.addEventListener("click", logout);
    if (dom.interactionAckBtn) dom.interactionAckBtn.addEventListener("click", markInteractionsSeen);
    if (dom.aiOpenBtn) dom.aiOpenBtn.addEventListener("click", openAiModal);
    if (dom.aiCardBtn) dom.aiCardBtn.addEventListener("click", openAiModal);
    if (dom.aiCloseBtn) dom.aiCloseBtn.addEventListener("click", closeAiModal);
    if (dom.aiForm) dom.aiForm.addEventListener("submit", handleAiSubmit);
    if (dom.body) dom.body.addEventListener("input", updateCounter);
    if (dom.clearDraft) dom.clearDraft.addEventListener("click", clearDraft);
    if (dom.media) dom.media.addEventListener("change", handleMediaChange);
    if (dom.mediaPreview) dom.mediaPreview.addEventListener("click", handleMediaPreviewClick);

    [dom.title, dom.body, dom.category, dom.alias, dom.tags].forEach((field) => {
      if (!field) return;
      field.addEventListener("input", saveDraft);
      field.addEventListener("change", saveDraft);
    });

    dom.moodButtons.forEach((button) => {
      button.addEventListener("click", () => {
        state.selectedMood = button.dataset.mood;
        dom.moodButtons.forEach((item) => item.classList.toggle("active", item === button));
        saveDraft();
      });
    });

    dom.filterButtons.forEach((button) => {
      button.addEventListener("click", () => {
        state.activeFilter = button.dataset.filter;
        dom.filterButtons.forEach((item) => item.classList.toggle("active", item === button));
        render();
      });
    });

    if (dom.search) {
      dom.search.addEventListener("input", () => {
        state.query = dom.search.value.trim().toLowerCase();
        render();
      });
    }

    if (dom.sort) {
      dom.sort.addEventListener("change", () => {
        state.sort = dom.sort.value;
        render();
      });
    }

    if (dom.feed) {
      dom.feed.addEventListener("click", handleFeedClick);
      dom.feed.addEventListener("submit", handleReplySubmit);
    }

    if (auth.loginUserBtn) {
      auth.loginUserBtn.addEventListener("click", () => openLoginModal(1));
    }
    if (auth.adminLoginBtn) {
      auth.adminLoginBtn.addEventListener("click", () => openLoginModal(2));
    }
    if (auth.registerBtn) {
      auth.registerBtn.addEventListener("click", () => {
        auth.registerTip.textContent = "";
        auth.registerModal.classList.add("show");
        auth.registerUsername.focus();
      });
    }
    if (auth.closeBtn) auth.closeBtn.addEventListener("click", () => auth.modal.classList.remove("show"));
    if (auth.registerCloseBtn) auth.registerCloseBtn.addEventListener("click", () => auth.registerModal.classList.remove("show"));
    if (auth.submitLoginBtn) auth.submitLoginBtn.addEventListener("click", submitLogin);
    if (auth.submitRegisterBtn) auth.submitRegisterBtn.addEventListener("click", submitRegister);

    [dom.aiModal].forEach((modal) => {
      if (!modal) return;
      modal.addEventListener("click", (event) => {
        if (event.target === modal) modal.classList.remove("show");
      });
    });

    document.addEventListener("keydown", (event) => {
      if (event.key !== "Escape") return;
      closeAiModal();
      if (auth.modal) auth.modal.classList.remove("show");
      if (auth.registerModal) auth.registerModal.classList.remove("show");
    });

    [auth.loginUsername, auth.loginPassword].forEach((input) => {
      if (!input) return;
      input.addEventListener("keydown", (event) => {
        if (event.key === "Enter") submitLogin();
      });
    });
    [auth.registerUsername, auth.registerNickname, auth.registerPassword].forEach((input) => {
      if (!input) return;
      input.addEventListener("keydown", (event) => {
        if (event.key === "Enter") submitRegister();
      });
    });
  }

  function openLoginModal(role) {
    auth.authTitle.textContent = role === 2 ? "管理员登录" : "用户登录";
    auth.loginRole.value = String(role);
    auth.authTip.textContent = "";
    auth.modal.classList.add("show");
    auth.loginUsername.focus();
  }

  function submitLogin() {
    const username = auth.loginUsername.value.trim();
    const password = auth.loginPassword.value.trim();
    const role = Number(auth.loginRole.value);

    if (!username || !password) {
      auth.authTip.textContent = "用户名和密码不能为空";
      return;
    }

    setButtonLoading(auth.submitLoginBtn, true, "登录中…");
    fetch(api("/auth/login"), {
      method: "POST",
      credentials: "same-origin",
      headers: { "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8" },
      body: toFormBody({ username, password, role }),
    })
      .then((res) => res.json())
      .then((data) => {
        if (data.success) {
          loginUser = data.user;
          localStorage.setItem("loginUser", JSON.stringify(data.user));
          auth.loginPassword.value = "";
          renderLoginUser();
          loadMinePosts();
          auth.modal.classList.remove("show");
          showToast(Number(loginUser.role) === 2 ? "管理员登录成功，可从顶部进入审核台。" : "登录成功，可以发布树洞啦。");
        } else {
          auth.authTip.textContent = data.message || "用户名、密码或入口类型错误";
        }
      })
      .catch(() => {
        auth.authTip.textContent = "网络错误，请稍后再试";
      })
      .finally(() => setButtonLoading(auth.submitLoginBtn, false, "立即登录"));
  }

  function submitRegister() {
    const username = auth.registerUsername.value.trim();
    const nickname = auth.registerNickname.value.trim();
    const password = auth.registerPassword.value.trim();

    if (!username || !nickname || !password) {
      auth.registerTip.textContent = "用户名、昵称和密码不能为空";
      return;
    }
    if (!/^[A-Za-z0-9_]{3,20}$/.test(username)) {
      auth.registerTip.textContent = "用户名需为 3-20 位字母、数字或下划线";
      return;
    }
    if (password.length < 6) {
      auth.registerTip.textContent = "密码至少 6 位";
      return;
    }
    if (nickname.length > 12) {
      auth.registerTip.textContent = "昵称最多 12 个字";
      return;
    }

    setButtonLoading(auth.submitRegisterBtn, true, "注册中…");
    fetch(api("/auth/register"), {
      method: "POST",
      credentials: "same-origin",
      headers: { "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8" },
      body: toFormBody({ username, password, nickname }),
    })
      .then((res) => res.json())
      .then((data) => {
        if (data.success) {
          auth.registerModal.classList.remove("show");
          auth.registerPassword.value = "";
          auth.registerNickname.value = "";
          auth.loginUsername.value = username;
          openLoginModal(1);
          showToast("注册成功，请登录。");
        } else {
          auth.registerTip.textContent = data.message || "用户名已存在";
        }
      })
      .catch(() => {
        auth.registerTip.textContent = "网络错误，请稍后再试";
      })
      .finally(() => setButtonLoading(auth.submitRegisterBtn, false, "注册账号"));
  }

  function logout() {
    fetch(api("/auth/logout"), { method: "POST", credentials: "same-origin" }).finally(() => {
      localStorage.removeItem("loginUser");
      loginUser = null;
      location.href = api("/index.jsp");
    });
  }

  function toFormBody(data) {
    return Object.keys(data)
      .map((key) => `${encodeURIComponent(key)}=${encodeURIComponent(data[key] == null ? "" : data[key])}`)
      .join("&");
  }

  function setButtonLoading(button, loading, text) {
    if (!button) return;
    button.disabled = loading;
    button.textContent = text;
  }

  function handlePostSubmit(event) {
    event.preventDefault();

    if (!loginUser) {
      showToast("游客模式仅支持浏览，发布树洞请先登录。");
      openLoginModal(1);
      return;
    }

    const body = dom.body.value.trim();
    const title = dom.title.value.trim();

    if (body.length < 8) {
      showToast("再多写一点点，至少 8 个字。");
      dom.body.focus();
      return;
    }

    const post = {
      id: state.editingPostId || undefined,
      title: title || "匿名碎碎念",
      body,
      category: dom.category.value,
      mood: state.selectedMood,
      alias: dom.alias.value.trim() || "匿名同学",
      tags: parseTags(dom.tags.value),
      media: state.selectedMedia,
    };

    const submitBtn = dom.form.querySelector("button[type='submit']");
    setButtonLoading(submitBtn, true, "提交中…");
    fetch(api(state.editingPostId ? "/api/posts/resubmit" : "/api/posts/publish"), {
      method: "POST",
      credentials: "same-origin",
      headers: { "Content-Type": "application/json;charset=UTF-8" },
      body: JSON.stringify(post),
    })
      .then((res) => res.json())
      .then((data) => {
        if (data.success) {
          if (isPreviewResponse(data)) savePreviewPost(post);
          dom.form.reset();
          state.selectedMedia = [];
          renderMediaPreview();
          state.selectedMood = "微光";
          dom.moodButtons.forEach((button) => button.classList.toggle("active", button.dataset.mood === "微光"));
          localStorage.removeItem(DRAFT_KEY);
          localStorage.removeItem(EDIT_POST_KEY);
          state.editingPostId = null;
          updateCounter();
          loadPosts();
          loadMinePosts();
          showToast(isPreviewResponse(data) ? "预览模式：已添加到本地广场。" : data.message || "已提交，等待管理员审核。通过后会出现在广场。");
        } else {
          showToast(data.message || "发布失败");
        }
      })
      .catch(() => showToast("网络错误，请稍后再试"))
      .finally(() => setButtonLoading(submitBtn, false, state.editingPostId ? "重新提交" : "提交审核"));
  }

  function handleFeedClick(event) {
    const button = event.target.closest("button[data-action]");
    const userAliasElement = event.target.closest(".user-alias");

    if (userAliasElement) {
      const targetAuthor = userAliasElement.dataset.author;
      if (targetAuthor) {
        state.query = `author:${targetAuthor}`;
        dom.search.value = `author:${targetAuthor}`;
        render();
      }
      return;
    }

    if (!button) return;

    if (!loginUser) {
      showToast("游客模式仅支持浏览，操作请先登录。");
      openLoginModal(1);
      return;
    }

    const card = event.target.closest(".hole-card");
    if (!card) return;

    const post = state.posts.find((item) => item.id === card.dataset.id);
    if (!post) return;

    const action = button.dataset.action;
    if (action === "reply") {
      state.openReplyId = state.openReplyId === post.id ? null : post.id;
      render();
      return;
    }

    if (action === "pin") {
      if (!loginUser || Number(loginUser.role) !== 2) return;
      requestPinFromFeed(post, Number(button.dataset.next));
      return;
    }

    if (!["like", "hug", "report"].includes(action)) return;
    const wasActive = action === "like" ? hasUserAction(post.likedBy) : action === "hug" ? hasUserAction(post.huggedBy) : hasUserAction(post.reportedBy);

    fetch(api("/api/posts/action"), {
      method: "POST",
      credentials: "same-origin",
      headers: { "Content-Type": "application/json;charset=UTF-8" },
      body: JSON.stringify({ postId: post.id, action }),
    })
      .then((res) => res.json())
      .then((data) => {
        if (!data.success) {
          showToast(data.message || "操作失败");
          return;
        }
        const nextActive = typeof data.active === "boolean" ? data.active : !wasActive;
        const tips = {
          like: nextActive ? "这份心意被看见了。" : "已取消点赞。",
          hug: nextActive ? "送出一个抱抱。" : "已取消抱抱。",
          report: nextActive ? "已记录举报，达到次数后会交给管理员复核。" : "已取消举报。",
        };
        showToast(tips[action]);
        loadPosts();
        loadMinePosts();
      })
      .catch(() => showToast("网络错误，请稍后再试"));
  }

  function handleReplySubmit(event) {
    if (!event.target.matches(".reply-form")) return;
    event.preventDefault();

    if (!loginUser) {
      showToast("游客模式仅支持浏览，操作请先登录。");
      openLoginModal(1);
      return;
    }

    const form = event.target;
    const input = form.querySelector("input");
    const body = input.value.trim();

    if (body.length < 2) {
      showToast("回应再写清楚一点。");
      return;
    }

    const submitBtn = form.querySelector("button");
    setButtonLoading(submitBtn, true, "发送中…");
    fetch(api("/api/posts/reply"), {
      method: "POST",
      credentials: "same-origin",
      headers: { "Content-Type": "application/json;charset=UTF-8" },
      body: JSON.stringify({ postId: form.dataset.id, body }),
    })
      .then((res) => res.json())
      .then((data) => {
        if (data.success) {
          input.value = "";
          showToast(data.message || "回应已提交AI审核。通过后会展示在树洞下。");
          loadPosts();
        } else {
          showToast(data.message || "回应失败");
        }
      })
      .catch(() => showToast("网络错误，请稍后再试"))
      .finally(() => setButtonLoading(submitBtn, false, "发送"));
  }

  function hasUserAction(users) {
    return !!(loginUser && Array.isArray(users) && users.includes(loginUser.username));
  }

  function requestPinFromFeed(post, pinned) {
    fetch(api("/api/admin/post/pin"), {
      method: "POST",
      credentials: "same-origin",
      headers: { "Content-Type": "application/json;charset=UTF-8" },
      body: JSON.stringify({ id: post.id, pinned }),
    })
      .then((res) => res.json())
      .then((data) => {
        showToast(data.message || (data.success ? "置顶状态已更新" : "置顶操作失败"));
        if (data.success) {
          loadPosts();
          loadMinePosts();
        }
      })
      .catch(() => showToast("网络错误，请稍后再试"));
  }

  function render() {
    if (!dom.feed) return;
    const visiblePosts = getVisiblePosts();
    dom.feed.innerHTML = visiblePosts.length ? visiblePosts.map(renderPost).join("") : renderEmptyState();
    renderStats(visiblePosts);
    renderTags();
    renderPersonalSurfaces();
  }

  function getVisiblePosts() {
    const query = state.query;
    let authorQuery = null;
    let normalQuery = query;

    if (query && query.startsWith("author:")) {
      authorQuery = query.substring(7);
      normalQuery = null;
    } else if (query && query.startsWith("alias:")) {
      authorQuery = query.substring(6);
      normalQuery = null;
    }

    const posts = state.posts.filter((post) => {
      const matchesApproved = post.auditStatus === undefined || Number(post.auditStatus) === 1;
      const matchesFilter = state.activeFilter === "全部" || post.category === state.activeFilter;

      let matchesQuery = true;
      if (authorQuery) {
        matchesQuery = post.authorUsername === authorQuery || post.alias === authorQuery;
      } else if (normalQuery) {
        const haystack = [post.title, post.body, post.category, post.mood, post.alias, ...(post.tags || [])]
          .join(" ")
          .toLowerCase();
        matchesQuery = haystack.includes(normalQuery);
      }
      return matchesApproved && matchesFilter && matchesQuery;
    });

    return posts.sort((a, b) => {
      const pinnedDiff = Number(b.isPinned || 0) - Number(a.isPinned || 0);
      if (pinnedDiff !== 0) return pinnedDiff;
      if (state.sort === "popular") return Number(b.likes || 0) + Number(b.hugs || 0) - (Number(a.likes || 0) + Number(a.hugs || 0));
      if (state.sort === "warmest") return Number(b.hugs || 0) - Number(a.hugs || 0);
      return new Date(b.createdAt) - new Date(a.createdAt);
    });
  }

  function renderPost(post) {
    const accent = categoryAccent[post.category] || categoryAccent["日常"];
    const isFolded = false;
    const replies = Array.isArray(post.replies) ? post.replies : [];

    const hasLiked = loginUser && Array.isArray(post.likedBy) && post.likedBy.includes(loginUser.username);
    const hasHugged = loginUser && Array.isArray(post.huggedBy) && post.huggedBy.includes(loginUser.username);
    const hasReported = loginUser && Array.isArray(post.reportedBy) && post.reportedBy.includes(loginUser.username);

    if (isFolded) {
      return `
        <article class="hole-card reported" data-id="${escapeHtml(post.id)}" style="--accent:${accent}">
          <div class="folded-message">
            这条内容已因多次举报被折叠。
            <button class="action-btn danger ${hasReported ? "is-active" : ""}" type="button" data-action="report">继续举报 ${post.reports}</button>
          </div>
        </article>
      `;
    }

    return `
      <article class="hole-card" data-id="${escapeHtml(post.id)}" style="--accent:${accent}">
        <div class="post-inner">
          <div class="post-meta">
            <div class="post-meta-left">
              <span class="category-pill">${escapeHtml(post.category || "日常")}</span>
              <span class="mood-pill">${escapeHtml(post.mood || "微光")}</span>
              ${Number(post.isPinned || 0) === 1 ? `<span class="mood-pill pinned-pill">置顶</span>` : ""}
              <span class="user-alias" title="点击筛选该作者" data-author="${escapeHtml(post.authorUsername || post.alias || "匿名同学")}">${escapeHtml(post.alias || "匿名同学")}</span>
            </div>
            <time datetime="${escapeHtml(post.createdAt)}">${timeAgo(post.createdAt)}</time>
          </div>

          <h3>${escapeHtml(post.title || "匿名碎碎念")}</h3>
          <p class="post-content">${escapeHtml(post.body || "")}</p>

          ${renderMediaGrid(post.media)}
          ${(post.tags || []).length ? `<div class="tag-row">${post.tags.map((tag) => `<span class="tag">#${escapeHtml(tag)}</span>`).join("")}</div>` : ""}

          <div class="post-actions">
            <button class="action-btn ${hasLiked ? "is-active" : ""}" type="button" data-action="like" aria-label="喜欢">
              <span aria-hidden="true">♡</span><span>${Number(post.likes || 0)}</span>
            </button>
            <button class="action-btn ${hasHugged ? "is-active" : ""}" type="button" data-action="hug" aria-label="抱抱">
              <span aria-hidden="true">◎</span><span>${Number(post.hugs || 0)}</span>
            </button>
            <button class="action-btn ${state.openReplyId === post.id ? "is-active" : ""}" type="button" data-action="reply" aria-label="回应">
              <span aria-hidden="true">↩</span><span>${replies.length}</span>
            </button>
            <button class="action-btn danger ${hasReported ? "is-active" : ""}" type="button" data-action="report" aria-label="举报">
              <span aria-hidden="true">!</span><span>${Number(post.reports || 0)}</span>
            </button>
            ${loginUser && Number(loginUser.role) === 2 ? `<button class="action-btn pin" type="button" data-action="pin" data-next="${Number(post.isPinned || 0) === 1 ? "0" : "1"}">${Number(post.isPinned || 0) === 1 ? "取消置顶" : "置顶"}</button>` : ""}
          </div>

          ${state.openReplyId === post.id ? renderReplyBox(post) : ""}
        </div>
      </article>
    `;
  }

  function renderReplyBox(post) {
    const replies = Array.isArray(post.replies) ? post.replies : [];
    const replyList = replies.length
      ? replies
          .map(
            (reply) => `
              <div class="reply-item">
                <b class="user-alias" title="点击筛选该作者" data-author="${escapeHtml(reply.authorUsername || reply.alias || "匿名同学")}">${escapeHtml(reply.alias || "匿名同学")} · ${timeAgo(reply.createdAt)}</b>
                <span>${escapeHtml(reply.body || "")}</span>
              </div>
            `,
          )
          .join("")
      : `<div class="reply-item"><span>还没有回应。</span></div>`;

    return `
      <div class="reply-box">
        ${replyList}
        <form class="reply-form" data-id="${escapeHtml(post.id)}">
          <input maxlength="120" placeholder="写一句回应，AI审核通过后展示" />
          <button type="submit">发送</button>
        </form>
      </div>
    `;
  }

  function renderEmptyState() {
    return `<div class="empty-state">没有匹配的树洞。换个分区或关键词看看。</div>`;
  }

  function renderMediaGrid(media) {
    const items = normalizeMedia(media);
    if (!items.length) return "";
    return `
      <div class="post-media-grid">
        ${items.map(renderMediaItem).join("")}
      </div>
    `;
  }

  function renderMediaItem(item) {
    if (item.type === "video") {
      return `
        <figure class="post-media-item">
          <video src="${escapeHtml(item.url)}" controls preload="metadata"></video>
        </figure>
      `;
    }
    return `
      <figure class="post-media-item">
        <img src="${escapeHtml(item.url)}" alt="${escapeHtml(item.name || "树洞图片")}" loading="lazy" />
      </figure>
    `;
  }

  function renderStats(visiblePosts) {
    if (!dom.statTotal) return;
    const today = new Date().toDateString();
    const approvedPosts = state.posts.filter((post) => post.auditStatus === undefined || Number(post.auditStatus) === 1);
    const todayCount = approvedPosts.filter((post) => new Date(post.createdAt).toDateString() === today).length;
    const warmth = approvedPosts.reduce((sum, post) => sum + Number(post.hugs || 0), 0);

    dom.statTotal.textContent = approvedPosts.length;
    dom.statToday.textContent = todayCount;
    dom.statWarmth.textContent = warmth;
    dom.sceneCount.textContent = visiblePosts.length;
  }

  function renderTags() {
    if (!dom.tagCloud) return;
    const counts = new Map();
    state.posts
      .filter((post) => post.auditStatus === undefined || Number(post.auditStatus) === 1)
      .forEach((post) => {
        (post.tags || []).forEach((tag) => counts.set(tag, (counts.get(tag) || 0) + 1));
      });

    const tags = Array.from(counts.entries()).sort((a, b) => b[1] - a[1]).slice(0, 10);

    dom.tagCloud.innerHTML = tags.length
      ? tags.map(([tag, count]) => `<button class="tag" type="button" data-tag="${escapeHtml(tag)}">#${escapeHtml(tag)} ${count}</button>`).join("")
      : `<span class="soft-text">暂无标签。</span>`;

    dom.tagCloud.querySelectorAll("button").forEach((button) => {
      button.addEventListener("click", () => {
        dom.search.value = button.dataset.tag;
        state.query = button.dataset.tag.toLowerCase();
        render();
      });
    });
  }

  function getPublicPostsByCurrentUser() {
    if (!loginUser) return [];
    return state.posts.filter((post) => post.authorUsername === loginUser.username);
  }

  function getMyPosts() {
    if (!loginUser) return [];
    return state.myPostsLoaded ? state.myPosts : getPublicPostsByCurrentUser();
  }

  function getDisplayName() {
    if (!loginUser) return "未登录";
    return loginUser.nickname || loginUser.username || "匿名同学";
  }

  function getAvatarText() {
    const name = getDisplayName();
    return name ? name.slice(0, 1).toUpperCase() : "你";
  }

  function getRoleName() {
    if (!loginUser) return "游客";
    return Number(loginUser.role) === 2 ? "管理员" : "普通用户";
  }

  function getOtherInteractionCount(post, totalKey, listKey) {
    const total = Number(post[totalKey] || 0);
    const users = Array.isArray(post[listKey]) ? post[listKey] : [];
    if (!loginUser) return total;
    if (users.length) return users.filter((name) => name !== loginUser.username).length;
    return post.authorUsername === loginUser.username ? total : 0;
  }

  function getPersonalSummary() {
    if (!loginUser) {
      return {
        posts: [],
        postCount: 0,
        approvedCount: 0,
        pendingCount: 0,
        rejectedCount: 0,
        likesReceived: 0,
        hugsReceived: 0,
        replyCount: 0,
        totalInteractions: 0,
      };
    }

    const posts = getMyPosts();
    const likesReceived = posts.reduce((sum, post) => sum + getOtherInteractionCount(post, "likes", "likedBy"), 0);
    const hugsReceived = posts.reduce((sum, post) => sum + getOtherInteractionCount(post, "hugs", "huggedBy"), 0);
    const replyCount = posts.reduce((sum, post) => sum + (Array.isArray(post.replies) ? post.replies.length : 0), 0);

    return {
      posts,
      postCount: posts.length,
      approvedCount: posts.filter((post) => Number(post.auditStatus) === 1).length,
      pendingCount: posts.filter((post) => Number(post.auditStatus) === 0).length,
      rejectedCount: posts.filter((post) => Number(post.auditStatus) === 2).length,
      likesReceived,
      hugsReceived,
      replyCount,
      totalInteractions: likesReceived + hugsReceived,
    };
  }

  function renderPersonalSurfaces() {
    const summary = getPersonalSummary();
    renderInteractionAlert(summary);
  }

  function renderInteractionAlert(summary) {
    if (!dom.interactionAlert) return;
    if (!loginUser || !summary.totalInteractions) {
      dom.interactionAlert.hidden = true;
      return;
    }

    const seen = getSeenInteractionTotal();
    const newTotal = Math.max(0, summary.totalInteractions - seen);

    dom.interactionAlert.hidden = false;
    dom.interactionAlert.classList.toggle("is-new", newTotal > 0);
    dom.interactionAlert.classList.toggle("is-soft", newTotal === 0);
    dom.interactionTitle.textContent = newTotal > 0 ? `你收到了 ${newTotal} 个新的回应` : "你的树洞正在被看见";
    dom.interactionText.textContent = `你发布的 ${summary.postCount} 条树洞，收到了 ${summary.likesReceived} 个喜欢和 ${summary.hugsReceived} 个抱抱。`;
    dom.interactionStats.innerHTML = `
      <span><b>${summary.likesReceived}</b><i>喜欢</i></span>
      <span><b>${summary.hugsReceived}</b><i>抱抱</i></span>
      <span><b>${summary.replyCount}</b><i>回应</i></span>
    `;
    if (dom.interactionAckBtn) {
      dom.interactionAckBtn.style.display = newTotal > 0 ? "inline-flex" : "none";
    }
  }

  function getInteractionSeenKey() {
    return `${INTERACTION_SEEN_PREFIX}:${loginUser ? loginUser.username : "guest"}`;
  }

  function getSeenInteractionTotal() {
    if (!loginUser) return 0;
    const raw = localStorage.getItem(getInteractionSeenKey());
    const snapshot = safelyParse(raw, null);
    return snapshot && typeof snapshot.total === "number" ? snapshot.total : 0;
  }

  function markInteractionsSeen() {
    if (!loginUser) return;
    const summary = getPersonalSummary();
    localStorage.setItem(
      getInteractionSeenKey(),
      JSON.stringify({
        likes: summary.likesReceived,
        hugs: summary.hugsReceived,
        total: summary.totalInteractions,
        at: new Date().toISOString(),
      }),
    );
    renderPersonalSurfaces();
    showToast("提醒已收下，新的喜欢和抱抱会继续出现在这里。");
  }

  function openAiModal() {
    if (!dom.aiModal) return;
    dom.aiModal.classList.add("show");
    ensureAiWelcome();
    if (dom.aiInput) dom.aiInput.focus();
  }

  function closeAiModal() {
    if (dom.aiModal) dom.aiModal.classList.remove("show");
  }

  function ensureAiWelcome() {
    if (!dom.aiMessages || dom.aiMessages.children.length) return;
    const name = loginUser ? getDisplayName() : "同学";
    appendAiMessage("assistant", `${name}，晚上好。我在这里，可以帮你整理心情、润色树洞，也可以先安静地听你说。`);
  }

  function handleAiSubmit(event) {
    event.preventDefault();
    if (!dom.aiInput) return;
    const text = dom.aiInput.value.trim();
    if (!text) return;

    const history = getAiHistory();
    appendAiMessage("user", text);
    dom.aiInput.value = "";
    appendAiMessage("assistant", "正在接入 AI...");
    fetch(api("/api/ai/chat"), {
      method: "POST",
      credentials: "same-origin",
      headers: { "Content-Type": "application/json;charset=UTF-8" },
      body: JSON.stringify({ message: text, history }),
    })
      .then((res) => res.json())
      .then((data) => {
        removeLastAiThinking();
        appendAiMessage("assistant", data.success ? data.reply : data.message || "AI 暂时没有接通。");
      })
      .catch(() => {
        removeLastAiThinking();
        appendAiMessage("assistant", "网络有点不稳，稍后再试一次。");
      });
  }

  function appendAiMessage(role, text) {
    if (!dom.aiMessages) return;
    dom.aiMessages.insertAdjacentHTML(
      "beforeend",
      `<div class="ai-message ${role}"><span>${escapeHtml(text)}</span></div>`,
    );
    dom.aiMessages.scrollTop = dom.aiMessages.scrollHeight;
  }

  function removeLastAiThinking() {
    if (!dom.aiMessages) return;
    const last = dom.aiMessages.lastElementChild;
    if (last && last.classList.contains("assistant") && last.textContent.includes("正在接入")) {
      last.remove();
    }
  }

  function getAiHistory() {
    if (!dom.aiMessages) return [];
    return Array.from(dom.aiMessages.querySelectorAll(".ai-message"))
      .map((node) => ({
        role: node.classList.contains("user") ? "user" : "assistant",
        content: node.textContent.trim(),
      }))
      .filter((item) => item.content && !item.content.includes("正在接入"))
      .slice(-10);
  }

  function buildAiReply(text) {
    const lower = text.toLowerCase();
    if (text.includes("标题") || text.includes("树洞") || text.includes("发布")) {
      return "可以先抓住最真实的一句话，再把细节放在后面。标题不用太用力，像“今天有点低电量”或者“想把这件小事放一下”就已经很像你。";
    }
    if (text.includes("难过") || text.includes("焦虑") || text.includes("烦") || text.includes("累")) {
      return "我先把这份重量接住。你可以不用立刻解决它，先说说最卡住你的那一小段，我们再一起把它拆小一点。";
    }
    if (text.includes("考试") || text.includes("学习") || text.includes("作业") || lower.includes("deadline")) {
      return "我们可以先做一个很轻的排序：最急的一件、最能在二十分钟内推进的一件、最需要求助的一件。你把清单丢给我，我帮你排。";
    }
    return "我听见了。你可以继续说，我会帮你把混在一起的情绪、事实和下一步慢慢分开。";
  }

  function handleMediaChange(event) {
    const files = Array.from(event.target.files || []);
    if (!files.length) return;

    const remaining = Math.max(0, 4 - state.selectedMedia.length);
    if (!remaining) {
      showToast("最多添加 4 个图片或视频。");
      dom.media.value = "";
      return;
    }

    const picked = files.slice(0, remaining);
    const readers = picked.map(readMediaFile);

    Promise.all(readers)
      .then((items) => {
        const validItems = items.filter(Boolean);
        state.selectedMedia = state.selectedMedia.concat(validItems).slice(0, 4);
        renderMediaPreview();
        if (files.length > remaining) showToast("最多添加 4 个媒体文件，已自动保留前几个。");
      })
      .finally(() => {
        dom.media.value = "";
      });
  }

  function readMediaFile(file) {
    const isImage = file.type && file.type.startsWith("image/");
    const isVideo = file.type && file.type.startsWith("video/");
    if (!isImage && !isVideo) {
      showToast("只支持图片或视频文件。");
      return Promise.resolve(null);
    }
    if (file.size > 2 * 1024 * 1024) {
      showToast(`${file.name} 超过 2MB，已跳过。`);
      return Promise.resolve(null);
    }

    return new Promise((resolve) => {
      const reader = new FileReader();
      reader.onload = () => {
        resolve({
          type: isVideo ? "video" : "image",
          url: reader.result,
          name: file.name,
        });
      };
      reader.onerror = () => {
        showToast(`${file.name} 读取失败。`);
        resolve(null);
      };
      reader.readAsDataURL(file);
    });
  }

  function renderMediaPreview() {
    if (!dom.mediaPreview) return;
    dom.mediaPreview.innerHTML = state.selectedMedia.length
      ? state.selectedMedia
          .map(
            (item, index) => `
              <div class="media-preview-item">
                ${item.type === "video" ? `<video src="${escapeHtml(item.url)}" muted preload="metadata"></video>` : `<img src="${escapeHtml(item.url)}" alt="${escapeHtml(item.name || "预览图片")}" />`}
                <button type="button" data-media-remove="${index}" aria-label="移除媒体">×</button>
              </div>
            `,
          )
          .join("")
      : "";
  }

  function handleMediaPreviewClick(event) {
    const button = event.target.closest("button[data-media-remove]");
    if (!button) return;
    const index = Number(button.dataset.mediaRemove);
    if (Number.isNaN(index)) return;
    state.selectedMedia.splice(index, 1);
    renderMediaPreview();
  }

  function updateCounter() {
    if (!dom.counter || !dom.body) return;
    dom.counter.textContent = `${dom.body.value.length} / 520`;
    saveDraft();
  }

  function saveDraft() {
    if (!dom.title || !dom.body) return;
    const draft = {
      title: dom.title.value,
      body: dom.body.value,
      category: dom.category.value,
      alias: dom.alias.value,
      tags: dom.tags.value,
      mood: state.selectedMood,
    };
    localStorage.setItem(DRAFT_KEY, JSON.stringify(draft));
  }

  function restoreDraft() {
    if (!dom.title || !dom.body) return;
    try {
      const editRaw = localStorage.getItem(EDIT_POST_KEY);
      if (editRaw) {
        const editPost = JSON.parse(editRaw);
        state.editingPostId = editPost.id || null;
        dom.title.value = editPost.title || "";
        dom.body.value = editPost.body || "";
        dom.category.value = editPost.category || "日常";
        dom.alias.value = editPost.alias || "";
        dom.tags.value = Array.isArray(editPost.tags) ? editPost.tags.join(" ") : editPost.tags || "";
        state.selectedMood = editPost.mood || "微光";
        state.selectedMedia = normalizeMedia(editPost.media);
        dom.moodButtons.forEach((button) => button.classList.toggle("active", button.dataset.mood === state.selectedMood));
        renderMediaPreview();
        const submitBtn = dom.form ? dom.form.querySelector("button[type='submit']") : null;
        if (submitBtn) submitBtn.lastChild.textContent = "重新提交";
        updateCounter();
        showToast("已载入被驳回的树洞，可以修改后重新提交。");
        return;
      }

      const raw = localStorage.getItem(DRAFT_KEY);
      if (!raw) {
        updateCounter();
        return;
      }

      const draft = JSON.parse(raw);
      dom.title.value = draft.title || "";
      dom.body.value = draft.body || "";
      dom.category.value = draft.category || "日常";
      dom.alias.value = draft.alias || "";
      dom.tags.value = draft.tags || "";
      state.selectedMood = draft.mood || "微光";
      dom.moodButtons.forEach((button) => button.classList.toggle("active", button.dataset.mood === state.selectedMood));
      updateCounter();
    } catch (error) {
      console.warn("草稿读取失败。", error);
      updateCounter();
    }
  }

  function clearDraft() {
    dom.form.reset();
    state.selectedMedia = [];
    renderMediaPreview();
    state.selectedMood = "微光";
    state.editingPostId = null;
    dom.moodButtons.forEach((button) => button.classList.toggle("active", button.dataset.mood === "微光"));
    localStorage.removeItem(DRAFT_KEY);
    localStorage.removeItem(EDIT_POST_KEY);
    updateCounter();
    showToast("草稿已清空。");
  }

  function parseTags(value) {
    return Array.from(
      new Set(
        value
          .split(/\s+/)
          .map((tag) => tag.replace(/^#/, "").trim())
          .filter(Boolean),
      ),
    ).slice(0, 6);
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
    if (!dom.toast) return;
    dom.toast.textContent = message;
    dom.toast.classList.add("show");
    window.clearTimeout(showToast.timer);
    showToast.timer = window.setTimeout(() => {
      dom.toast.classList.remove("show");
    }, 2400);
  }
})();
