<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!doctype html>
<html lang="zh-CN">
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>校屿树洞 · Campus Treehole</title>
    <link rel="preconnect" href="https://fonts.googleapis.com" />
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin />
    <link
      href="https://fonts.googleapis.com/css2?family=Cormorant+Garamond:wght@400;500;600&family=Noto+Serif+SC:wght@400;500;600;700&family=Noto+Sans+SC:wght@300;400;500;700&display=swap"
      rel="stylesheet"
    />
    <link rel="stylesheet" href="${pageContext.request.contextPath}/styles-entry-portal.css?v=20260702-cinematic-entry" />
  </head>
  <body class="entry-body">
    <main class="entry-portal" aria-labelledby="entryTitle">
      <div class="entry-bg" aria-hidden="true"></div>
      <div class="entry-grain" aria-hidden="true"></div>

      <header class="entry-nav" aria-label="入口导航">
        <a class="entry-brand" href="${pageContext.request.contextPath}/index.jsp" aria-label="校屿树洞入口">
          <span class="entry-brand-mark">树</span>
          <span>
            <strong>校屿树洞</strong>
            <em>CAMPUS TREEHOLE</em>
          </span>
        </a>
        <span class="entry-nav-note">EST. 2026</span>
      </header>

      <section class="entry-stage">
        <div class="entry-copy">
          <p class="entry-kicker">CAMPUS WHISPER · QUIET PORTAL</p>
          <h1 id="entryTitle" class="entry-title">
            <span>树洞</span>
            <span>今晚</span>
            <span>为你打开</span>
          </h1>
          <p class="entry-subtitle">
            每一句心事，都会被温柔聆听。这里不用逞强，只需要慢慢说。
          </p>
        </div>

        <a class="entry-cta" href="${pageContext.request.contextPath}/plaza.jsp" aria-label="进入校屿树洞">
          <span>进入校屿树洞</span>
          <i aria-hidden="true">→</i>
        </a>
      </section>

      <footer class="entry-footer" aria-label="入口说明">
        <span>ANONYMOUS · CAMPUS · MOONLIGHT</span>
        <span>静谧倾诉，温暖回应</span>
      </footer>
    </main>

    <script>
      (function () {
        var portal = document.querySelector(".entry-portal");
        var bg = document.querySelector(".entry-bg");
        if (!portal || !bg) return;
        portal.addEventListener("pointermove", function (event) {
          var x = event.clientX / window.innerWidth - 0.5;
          var y = event.clientY / window.innerHeight - 0.5;
          bg.style.transform = "scale(1.045) translate(" + (-x * 14) + "px, " + (-y * 10) + "px)";
          portal.style.setProperty("--mx", (event.clientX / window.innerWidth * 100).toFixed(2) + "%");
          portal.style.setProperty("--my", (event.clientY / window.innerHeight * 100).toFixed(2) + "%");
        });
      })();
    </script>
  </body>
</html>
