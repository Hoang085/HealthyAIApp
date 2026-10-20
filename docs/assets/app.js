/* =========================================================================
   HealthMate — tài liệu kỹ thuật
   Điều khiển chung: sidebar, mục lục, theme, tìm kiếm, nút copy code.
   Chạy hoàn toàn offline (không CDN, không fetch).
   ========================================================================= */

(function () {
  "use strict";

  /* ----------------------------------------------------------- cấu trúc site */

  var SITE = [
    { path: "index.html",        icon: "🏠", title: "Tổng quan",                 short: "Tổng quan" },
    { path: "architecture.html", icon: "🏗️", title: "Kiến trúc & luồng dữ liệu", short: "Kiến trúc" },
    { path: "data.html",         icon: "🗄️", title: "Tầng dữ liệu & bảo mật",    short: "Dữ liệu" },
    { path: "logic-sensors.html",icon: "📈", title: "Thuật toán: bước chân · giấc ngủ · nhịp tim", short: "Thuật toán cảm biến" },
    { path: "logic-ai.html",     icon: "🧠", title: "Logic: Rule Engine & AI",    short: "Rule Engine & AI" },
    { path: "ui.html",           icon: "📱", title: "Giao diện & điều hướng",     short: "Giao diện" },
    { path: "background.html",   icon: "⚙️", title: "Chạy nền & phân quyền",     short: "Chạy nền" },
    { path: "testing.html",      icon: "🧪", title: "Kiểm thử & chất lượng",      short: "Kiểm thử" },
    { path: "files.html",        icon: "🗂️", title: "Bản đồ mã nguồn",            short: "Bản đồ mã nguồn" },
    { path: "classes.html",      icon: "📚", title: "Danh mục lớp & trách nhiệm", short: "Danh mục lớp" }
  ];

  var current = document.documentElement.getAttribute("data-page") || "index.html";
  var currentIndex = -1;
  for (var si = 0; si < SITE.length; si++) {
    if (SITE[si].path === current) { currentIndex = si; }
  }

  /* ---------------------------------------------------------------- theme */

  /**
   * Khoá lưu lựa chọn theme. Đổi tên khoá (v1 → v2) là cố ý: bản trước mặc định theo
   * `prefers-color-scheme` nên máy đang bật Dark Mode đã lưu "dark" vào localStorage.
   * Đổi khoá để lựa chọn cũ không đè lên mặc định MỚI của site.
   */
  var THEME_KEY = "healthyai-docs-theme-v2";

  function applyTheme(theme) {
    document.documentElement.setAttribute("data-theme", theme);
    try { localStorage.setItem(THEME_KEY, theme); } catch (e) { /* ignore */ }
    var btn = document.querySelector(".theme-toggle");
    if (btn) { btn.textContent = theme === "dark" ? "☀️ Nền sáng" : "🌙 Nền tối"; }
  }

  function initTheme() {
    var saved = null;
    try { saved = localStorage.getItem(THEME_KEY); } catch (e) { /* ignore */ }
    // Mặc định NỀN TRẮNG. Chỉ dùng nền tối khi người dùng tự bấm nút đổi theme
    // (không còn theo `prefers-color-scheme` của hệ điều hành).
    applyTheme(saved === "dark" ? "dark" : "light");
  }

  /* ---------------------------------------------------------------- sidebar */

  function buildSidebar() {
    var host = document.getElementById("sidebar");
    if (!host) { return; }

    var html = "";
    html += '<div class="brand">';
    html += '  <span class="brand__logo">🌿</span>';
    html += '  <span><span class="brand__name">HealthMate</span>';
    html += '  <span class="brand__sub">Tài liệu kỹ thuật hệ thống</span></span>';
    html += '</div>';

    html += '<div class="sidebar__search">';
    html += '  <input id="nav-search" type="search" placeholder="Tìm mục trong trang…" autocomplete="off">';
    html += '</div>';

    html += '<nav><ul class="nav">';
    for (var i = 0; i < SITE.length; i++) {
      var item = SITE[i];
      var active = item.path === current ? " is-active" : "";
      html += '<li>';
      html += '<a class="nav__link' + active + '" href="' + item.path + '" data-path="' + item.path + '">';
      html += '<span class="nav__icon">' + item.icon + '</span><span>' + item.title + '</span>';
      html += '</a>';
      html += '<ul class="nav__sub' + (item.path === current ? " is-open" : "") + '" id="sub-' + i + '"></ul>';
      html += '</li>';
    }
    html += '</ul></nav>';

    html += '<div class="sidebar__foot">';
    html += '  Android · Kotlin · Jetpack Compose<br>';
    html += '  Clean Architecture / MVVM<br>';
    html += '  <button class="theme-toggle" type="button">🌙 Nền tối</button>';
    html += '</div>';

    host.innerHTML = html;

    var toggle = host.querySelector(".theme-toggle");
    if (toggle) { toggle.addEventListener("click", function () { applyTheme(toggleTheme()); }); }
  }

  function toggleTheme() {
    var now = document.documentElement.getAttribute("data-theme");
    return now === "dark" ? "light" : "dark";
  }

  /* --------------------------------------------------------------- mục lục */

  function slugify(text) {
    var s = text.toLowerCase();
    s = s.replace(/[àáạảãâầấậẩẫăằắặẳẵ]/g, "a")
         .replace(/[èéẹẻẽêềếệểễ]/g, "e")
         .replace(/[ìíịỉĩ]/g, "i")
         .replace(/[òóọỏõôồốộổỗơờớợởỡ]/g, "o")
         .replace(/[ùúụủũưừứựửữ]/g, "u")
         .replace(/[ỳýỵỷỹ]/g, "y")
         .replace(/đ/g, "d")
         .replace(/[^a-z0-9\s\-]/g, "")
         .replace(/\s+/g, "-")
         .replace(/-+/g, "-")
         .replace(/^-|-$/g, "");
    return s || "muc";
  }

  var headingIndex = [];

  function buildToc() {
    var article = document.querySelector("article");
    var toc = document.getElementById("toc");
    if (!article || !toc) { return; }

    var heads = article.querySelectorAll("h2, h3");
    var used = {};
    var html = '<div class="toc__title">Trong trang này</div><ul>';
    headingIndex = [];

    for (var i = 0; i < heads.length; i++) {
      var h = heads[i];
      var text = h.textContent.replace(/\s+/g, " ").trim();
      var id = h.id;
      if (!id) {
        id = slugify(text);
        while (used[id]) { id = id + "-x"; }
        h.id = id;
      }
      used[id] = true;

      var anchor = document.createElement("a");
      anchor.className = "anchor";
      anchor.href = "#" + id;
      anchor.textContent = "#";
      anchor.setAttribute("aria-label", "Liên kết tới mục này");
      h.appendChild(anchor);

      var level = h.tagName === "H3" ? "toc-h3" : "toc-h2";
      html += '<li class="' + level + '"><a href="#' + id + '" data-target="' + id + '">' + text + '</a></li>';
      headingIndex.push({ id: id, el: h, text: text });
    }

    html += "</ul>";
    toc.innerHTML = html;
  }

  /* ------------------------------------------------------------ scroll spy */

  function initScrollSpy() {
    if (!headingIndex.length) { return; }

    var links = {};
    var anchors = document.querySelectorAll("#toc a[data-target]");
    for (var i = 0; i < anchors.length; i++) {
      links[anchors[i].getAttribute("data-target")] = anchors[i];
    }

    function update() {
      var best = null;
      for (var j = 0; j < headingIndex.length; j++) {
        var rect = headingIndex[j].el.getBoundingClientRect();
        if (rect.top <= 90) { best = headingIndex[j].id; }
      }
      if (!best) { best = headingIndex[0].id; }
      for (var key in links) {
        if (Object.prototype.hasOwnProperty.call(links, key)) {
          links[key].classList.toggle("is-active", key === best);
        }
      }
    }

    var ticking = false;
    window.addEventListener("scroll", function () {
      if (ticking) { return; }
      ticking = true;
      window.requestAnimationFrame(function () { update(); ticking = false; });
    }, { passive: true });
    window.addEventListener("resize", update);
    update();
  }

  /* -------------------------------------------------------------- tìm kiếm */

  function initSearch() {
    var input = document.getElementById("nav-search");
    if (!input) { return; }

    input.addEventListener("input", function () {
      var q = input.value.trim().toLowerCase();
      var subItems = document.querySelectorAll(".nav__sub a");

      for (var i = 0; i < subItems.length; i++) {
        var a = subItems[i];
        var text = a.textContent.toLowerCase();
        var match = !q || text.indexOf(q) !== -1;
        a.parentNode.style.display = match ? "" : "none";
      }
      if (q) {
        var subs = document.querySelectorAll(".nav__sub");
        for (var k = 0; k < subs.length; k++) { subs[k].classList.add("is-open"); }
      }
    });

    input.addEventListener("keydown", function (ev) {
      if (ev.key === "Enter") {
        var first = document.querySelector(".nav__sub a:not([style*='display: none'])");
        if (first) { window.location.hash = first.getAttribute("href"); }
      }
      if (ev.key === "Escape") { input.value = ""; input.dispatchEvent(new Event("input")); }
    });
  }

  /* ---------------------------------------------------------- copy code btn */

  function initCopyButtons() {
    var pres = document.querySelectorAll("pre");
    for (var i = 0; i < pres.length; i++) {
      (function (pre) {
        var btn = document.createElement("button");
        btn.className = "copy-btn";
        btn.type = "button";
        btn.textContent = "Copy";
        btn.addEventListener("click", function () {
          var text = pre.querySelector("code");
          text = text ? text.textContent : pre.textContent;
          if (navigator.clipboard && navigator.clipboard.writeText) {
            navigator.clipboard.writeText(text).then(function () {
              btn.textContent = "Đã copy";
              setTimeout(function () { btn.textContent = "Copy"; }, 1400);
            }, function () { btn.textContent = "Không copy được"; });
          }
        });
        pre.appendChild(btn);
      })(pres[i]);
    }
  }

  /* ---------------------------------------------------------------- pager */

  function buildPager() {
    var host = document.getElementById("pager");
    if (!host || currentIndex < 0) { return; }
    var html = "";
    var prev = SITE[currentIndex - 1];
    var next = SITE[currentIndex + 1];
    if (prev) {
      html += '<a class="prev" href="' + prev.path + '"><span class="pager__label">← Trước</span>' + prev.title + '</a>';
    }
    if (next) {
      html += '<a class="next" href="' + next.path + '"><span class="pager__label">Tiếp →</span>' + next.title + '</a>';
    }
    host.innerHTML = html;
  }

  /* ----------------------------------------------------------------- khởi chạy */

  function boot() {
    initTheme();
    buildSidebar();
    buildToc();
    // Render danh sách mục con cho sidebar từ các heading vừa tạo id
    var sub = document.getElementById("sub-" + currentIndex);
    if (sub) {
      var out = "";
      for (var i = 0; i < headingIndex.length; i++) {
        if (headingIndex[i].el.tagName === "H2") {
          out += '<li><a href="#' + headingIndex[i].id + '">' + headingIndex[i].text + '</a></li>';
        }
      }
      sub.innerHTML = out;
    }
    initScrollSpy();
    initSearch();
    initCopyButtons();
    buildPager();
  }

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", boot);
  } else {
    boot();
  }
})();
