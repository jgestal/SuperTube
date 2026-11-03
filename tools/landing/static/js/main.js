// SuperTube landing: small progressive enhancements. The page works without JS.
(function () {
  "use strict";
  var doc = document.documentElement;
  doc.classList.add("js");
  // ?shot: everything visible at once, without animations (for screenshots)
  var reduce = window.matchMedia("(prefers-reduced-motion: reduce)").matches || /[?&]shot/.test(location.search);

  /**
   * Hero simulator: the TV app in miniature. The focus moves over the cards and the focused video becomes the
   * backdrop (like the real app); Enter or a tap opens a player that skips the sponsor segment. It plays by itself
   * until the visitor takes over with the arrows, a click, a tap or a swipe.
   */
  function initSim() {
    var sim = document.getElementById("sim");
    if (!sim) return;
    var track = sim.querySelector(".sim-track");
    var cards = Array.prototype.slice.call(track.querySelectorAll(".sim-card"));
    var layers = sim.querySelectorAll(".sim-bg img");
    var player = sim.querySelector(".sim-player");
    var pImg = player.querySelector("img"), pTitle = player.querySelector(".sim-info b");
    var progress = player.querySelector(".sim-progress"), toast = player.querySelector(".sim-toast");
    var lite = document.getElementById("sim-lite");
    var focus = -1, layer = 0, bdTimer = 0, demoTimer = 0, idleTimer = 0, playTimer = 0, demoStep = 0, user = false;

    // Second row (decorative), like the shelves of the app
    var ghost = sim.querySelector(".sim-row").cloneNode(true);
    ghost.classList.add("ghost");
    ghost.setAttribute("aria-hidden", "true");
    ghost.querySelectorAll("button").forEach(function (b) { b.tabIndex = -1; b.classList.remove("focus"); });
    var gt = ghost.querySelector(".sim-track");
    for (var k = gt.children.length - 1; k >= 0; k--) gt.appendChild(gt.children[k]);
    sim.appendChild(ghost);

    // Real clock, like the app's corner clock
    var clock = sim.querySelector(".sim-clock");
    function tick() {
      var d = new Date();
      clock.textContent = d.toLocaleTimeString(document.documentElement.lang, { hour: "2-digit", minute: "2-digit" });
    }
    tick();
    setInterval(tick, 20000);

    function backdrop(src) {
      clearTimeout(bdTimer);
      if (lite.checked) return;
      // Small delay as in the app: fast moves don't load every image
      bdTimer = setTimeout(function () {
        var next = layers[1 - layer];
        next.src = src;
        layers[layer].classList.remove("on");
        next.classList.add("on");
        layer = 1 - layer;
        sim.classList.add("has-bd");
      }, reduce ? 0 : 280);
    }

    function setFocus(i) {
      i = Math.max(0, Math.min(cards.length - 1, i));
      if (i === focus) return;
      if (focus >= 0) cards[focus].classList.remove("focus");
      focus = i;
      cards[i].classList.add("focus");
      // Leanback keeps the focused card near the left edge once the row scrolls
      var shift = Math.max(0, i - 1) * (cards[0].offsetWidth + parseFloat(getComputedStyle(track).columnGap || 0));
      track.style.transform = "translateX(" + -shift + "px)";
      backdrop(cards[i].getAttribute("data-img"));
    }

    function open() {
      if (focus < 0) return;
      clearTimeout(playTimer);
      pImg.src = cards[focus].getAttribute("data-img");
      pTitle.textContent = cards[focus].getAttribute("data-title");
      toast.classList.remove("show");
      player.hidden = false;
      var p = 0, skipped = false;
      progress.style.setProperty("--p", "0%");
      (function run() {
        p += lite.checked ? 2.5 : 1.2;
        if (!skipped && p >= 24) { p = 40; skipped = true; toast.classList.add("show"); } // SponsorBlock jumps over the green segment
        progress.style.setProperty("--p", Math.min(p, 100) + "%");
        if (p < 100) playTimer = setTimeout(run, 60);
        else if (!user) playTimer = setTimeout(close, 900);
      })();
      if (user) sim.focus({ preventScroll: true });
    }

    function close() {
      clearTimeout(playTimer);
      player.hidden = true;
    }

    // Automatic demo: walk the row, open a video, come back
    function demo() {
      clearTimeout(demoTimer);
      if (user || reduce) return;
      demoTimer = setTimeout(function () {
        if (document.hidden || !inView) { demo(); return; }
        if (!player.hidden) { demo(); return; }
        demoStep++;
        if (demoStep % 5 === 0) open();
        else setFocus(focus + 1 >= cards.length ? 0 : focus + 1);
        demo();
      }, demoStep % 5 === 4 ? 1600 : 2300);
    }

    function takeOver() {
      user = true;
      sim.classList.add("user");
      clearTimeout(demoTimer);
      clearTimeout(idleTimer);
      idleTimer = setTimeout(function () { user = false; sim.classList.remove("user"); close(); demo(); }, 15000);
    }

    sim.addEventListener("keydown", function (ev) {
      var k = ev.key;
      if (["ArrowRight", "ArrowLeft", "ArrowUp", "ArrowDown", "Enter", " ", "Escape", "Backspace"].indexOf(k) < 0) return;
      ev.preventDefault();
      takeOver();
      if (!player.hidden) { if (k === "Escape" || k === "Backspace" || k === "ArrowLeft") close(); return; }
      if (k === "ArrowRight") setFocus(focus + 1);
      else if (k === "ArrowLeft") setFocus(focus - 1);
      else if (k === "Enter" || k === " ") open();
    });
    cards.forEach(function (c, i) {
      c.tabIndex = -1;
      c.addEventListener("click", function () {
        takeOver();
        if (i === focus) open(); else { setFocus(i); if (matchMedia("(pointer: coarse)").matches) open(); }
      });
    });
    player.querySelector(".sim-back").addEventListener("click", function () { takeOver(); close(); });

    // Horizontal swipe on touch screens moves the focus
    var x0 = null;
    sim.addEventListener("pointerdown", function (ev) { x0 = ev.clientX; });
    sim.addEventListener("pointerup", function (ev) {
      if (x0 === null) return;
      var dx = ev.clientX - x0;
      x0 = null;
      if (Math.abs(dx) > 40 && player.hidden) { takeOver(); setFocus(focus + (dx < 0 ? 1 : -1)); }
    });

    lite.addEventListener("change", function () {
      sim.classList.toggle("lite", lite.checked);
      if (lite.checked) { sim.classList.remove("has-bd"); layers.forEach(function (l) { l.classList.remove("on"); }); }
      else if (focus >= 0) backdrop(cards[focus].getAttribute("data-img"));
    });

    // Only animate while the hero is on screen
    var inView = true;
    if ("IntersectionObserver" in window) {
      new IntersectionObserver(function (en) { inView = en[0].isIntersecting; }).observe(sim);
    }
    window.addEventListener("resize", function () { var f = focus; focus = -1; cards[f] && cards[f].classList.remove("focus"); setFocus(f); });

    setFocus(0);
    demo();
  }

  document.addEventListener("DOMContentLoaded", function () {
    // Scroll reveal
    var items = document.querySelectorAll(".reveal");
    if ("IntersectionObserver" in window && !reduce) {
      var io = new IntersectionObserver(function (entries) {
        entries.forEach(function (en) {
          if (en.isIntersecting) { en.target.classList.add("in"); io.unobserve(en.target); }
        });
      }, { rootMargin: "0px 0px -8% 0px", threshold: 0.08 });
      items.forEach(function (el, i) { el.style.transitionDelay = (i % 3) * 80 + "ms"; io.observe(el); });
    } else {
      items.forEach(function (el) { el.classList.add("in"); });
    }

    initSim();

    // Subtle 3D tilt of the devices following the pointer
    var stage = document.querySelector(".stage");
    if (stage && !reduce && window.matchMedia("(pointer: fine)").matches) {
      var hero = document.querySelector(".hero");
      hero.addEventListener("pointermove", function (ev) {
        var r = hero.getBoundingClientRect();
        var x = (ev.clientX - r.left) / r.width - 0.5;
        var y = (ev.clientY - r.top) / r.height - 0.5;
        stage.style.setProperty("--rx", (x * 10).toFixed(2));
        stage.style.setProperty("--ry", (y * 8).toFixed(2));
      });
      hero.addEventListener("pointerleave", function () {
        stage.style.setProperty("--rx", 0);
        stage.style.setProperty("--ry", 0);
      });
    }

    // Spotlight that follows the pointer on the feature cards
    document.querySelectorAll(".feat").forEach(function (card) {
      card.addEventListener("pointermove", function (ev) {
        var r = card.getBoundingClientRect();
        card.style.setProperty("--mx", ev.clientX - r.left + "px");
        card.style.setProperty("--my", ev.clientY - r.top + "px");
      });
    });

    // Gallery tabs (WAI-ARIA tabs with arrow keys)
    var tabs = Array.prototype.slice.call(document.querySelectorAll('[role="tab"]'));
    function select(tab) {
      tabs.forEach(function (t) {
        var on = t === tab;
        t.setAttribute("aria-selected", on);
        t.tabIndex = on ? 0 : -1;
        document.getElementById(t.getAttribute("aria-controls")).hidden = !on;
      });
    }
    tabs.forEach(function (t, i) {
      t.addEventListener("click", function () { select(t); });
      t.addEventListener("keydown", function (ev) {
        if (ev.key !== "ArrowRight" && ev.key !== "ArrowLeft") return;
        var n = tabs[(i + (ev.key === "ArrowRight" ? 1 : tabs.length - 1)) % tabs.length];
        select(n); n.focus();
      });
    });

    // Lightbox
    var lb = document.querySelector(".lightbox");
    if (lb && typeof lb.showModal === "function") {
      var img = lb.querySelector("img"), cap = lb.querySelector("figcaption"), list = [], idx = 0;
      function show(i) {
        idx = (i + list.length) % list.length;
        img.src = list[idx].getAttribute("data-full");
        cap.textContent = img.alt = list[idx].getAttribute("data-cap");
      }
      document.querySelectorAll(".shot button").forEach(function (b) {
        b.addEventListener("click", function () {
          list = Array.prototype.slice.call(b.closest(".rail").querySelectorAll("button"));
          show(list.indexOf(b));
          lb.showModal();
        });
      });
      lb.querySelector(".lb-close").addEventListener("click", function () { lb.close(); });
      lb.querySelector(".lb-prev").addEventListener("click", function () { show(idx - 1); });
      lb.querySelector(".lb-next").addEventListener("click", function () { show(idx + 1); });
      lb.addEventListener("click", function (ev) { if (ev.target === lb) lb.close(); });
      lb.addEventListener("keydown", function (ev) {
        if (ev.key === "ArrowRight") show(idx + 1);
        if (ev.key === "ArrowLeft") show(idx - 1);
      });
    }

    // Suggest the visitor's language once (no automatic redirect: better for search engines and links)
    var main = document.getElementById("main");
    if (!main || !main.dataset.langs) return;
    var store;
    try { store = window.localStorage; } catch (e) { store = null; }
    try { if (store && store.getItem("st-lang-dismissed")) return; } catch (e) { return; }
    var langs = JSON.parse(main.dataset.langs), here = main.dataset.lang;
    var want = (navigator.language || "en").slice(0, 2).toLowerCase();
    if (want === here || !langs[want]) return;
    var bar = document.createElement("div");
    bar.className = "suggest";
    bar.setAttribute("lang", want);
    bar.innerHTML = '<a href="' + langs[want].href + '">' + langs[want].name + ' →</a><button type="button" aria-label="×">×</button>';
    bar.querySelector("button").addEventListener("click", function () {
      bar.classList.remove("show");
      try { store && store.setItem("st-lang-dismissed", "1"); } catch (e) { /* private mode */ }
    });
    document.body.appendChild(bar);
    setTimeout(function () { bar.classList.add("show"); }, 1200);
  });
})();
