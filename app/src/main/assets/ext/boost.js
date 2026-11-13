(function () {
  if (window.__sbBoost) return;
  window.__sbBoost = 1;
  // Spotify's class names are hashed per build, so select by data-testid / structure only.
  var GRID = 'div:has(> [data-testid="global-nav-bar"])';
  var css = `
    /* clip, not hidden: focusing a lyrics line scrolled the hidden-overflow document sideways by a few px */
    :root { --sb-nav: 56px; }
    /* bottom navigation: Inicio / Buscar / Biblioteca (they drive Spotify's own controls); the top bar keeps search + profile */
    #sb-lib-btn, [data-testid="home-button"] { display: none !important; }
    #sb-nav { position: fixed; left: 0; right: 0; bottom: 0; height: var(--sb-nav); z-index: 40; display: flex;
      background: #000; border-top: 1px solid #ffffff14; user-select: none; -webkit-user-select: none; }
    #sb-nav button { flex: 1; background: none; border: 0; color: #b3b3b3; display: flex; flex-direction: column;
      align-items: center; justify-content: center; gap: 2px; font: 600 11px/1 inherit; padding: 0; cursor: pointer; }
    #sb-nav button.on { color: #fff; }
    #sb-nav svg { width: 24px; height: 24px; fill: currentColor; }
    body:has(input:focus) #sb-nav, body.sb-np-open #sb-nav { display: none; } /* keyboard up / full-screen player */
    html, body, ${GRID} { min-width: 0 !important; overflow-x: clip !important; }
    ${GRID} {
      grid-template-areas: "top-banner" "global-nav" "main-view" "now-playing-bar" !important;
      grid-template-columns: minmax(0, 1fr) !important;
      grid-template-rows: auto auto minmax(0, 1fr) auto !important;
      height: 100dvh !important; min-height: 0 !important; padding: 0 0 var(--sb-nav) 0 !important; gap: 0 !important; box-sizing: border-box !important;
    }
    /* right panel (queue / now playing view) is unusable at phone width */
    #main-view + div, .LayoutResizer__resize-bar { display: none !important; }
    /* ...except while a panel is open (devices, queue, lyrics): body.sb-panel is set from the bar buttons' aria-pressed */
    body.sb-panel #main-view + div {
      display: block !important; position: fixed !important; z-index: 60; left: 0; right: 0; top: 56px; bottom: calc(96px + var(--sb-nav));
      width: auto !important; background: #121212; overflow: auto; scrollbar-width: none;
    }
    /* the wrapper chain from the sheet down to the panel content (not its internals): size to content, full width, in flow */
    body.sb-panel #main-view + div :is(div, aside):has(#Desktop_PanelContainer_Id),
    body.sb-panel #Desktop_PanelContainer_Id,
    body.sb-panel #Desktop_PanelContainer_Id > div,
    body.sb-panel #Desktop_PanelContainer_Id > div > div,
    body.sb-panel #Desktop_PanelContainer_Id > div > div > div,
    body.sb-panel #Desktop_PanelContainer_Id > div > div > div > div,
    body.sb-panel #Desktop_PanelContainer_Id > div > div > div > div > div {
      position: static !important; height: auto !important; min-height: 0 !important; max-height: none !important;
      width: 100% !important; min-width: 0 !important; max-width: 100% !important; overflow: visible !important;
      transform: none !important; box-sizing: border-box;
    }
    /* library becomes a drawer, toggled by the button added below */
    #Desktop_LeftSidebar_Id {
      display: none !important; position: fixed !important; z-index: 50; left: 0; top: 56px; bottom: calc(96px + var(--sb-nav));
      right: 0; width: auto !important; background: #121212; overflow: auto;
    }
    body.sb-lib #Desktop_LeftSidebar_Id { display: block !important; }
    /* top bar: drop the logo, let the rest scroll sideways instead of overflowing */
    [data-testid="global-nav-bar"] { overflow: auto hidden; scrollbar-width: none; gap: 4px; height: 56px !important; flex-wrap: nowrap !important; margin: 0 !important; }
    [data-testid="global-nav-bar"] a[href*="download"], [data-testid="global-nav-bar"] [data-testid="browse-button"], [data-testid="signup-button"] { display: none !important; }
    [data-testid="login-button"] { white-space: nowrap; }
    [data-testid="global-nav-bar"] > div:first-of-type { display: none !important; }
    /* sign-up / install banners */
    [data-testid="now-playing-bar"], [data-testid="signup-bar"] { min-height: 0 !important; max-width: 100% !important; overflow: hidden; box-sizing: border-box; }
    #main-view { min-width: 0 !important; border-radius: 0 !important; }
    /* tighter side/inner spacing (Spotify's own variable, 16px by default) */
    #main-view, #main-view * { --content-spacing: 12px !important; }
    /* Home hard-codes 40px of side padding on the child of its <section> */
    #main-view [data-testid="home-page"] > div { padding-inline: 12px !important; }
    /* the page's scroller allowed a few px of sideways drag (content 5px wider than the panel): vertical scroll only.
       Carousels keep their own horizontal scrollers. */
    #main-view div[data-overlayscrollbars-viewport], #main-view .os-viewport { overflow-x: hidden !important; overscroll-behavior-x: none; }
    #main-view .os-scrollbar-horizontal { display: none !important; }
    /* lyrics: Spotify mounts them in their own child of the app grid (not in the right panel). My one-column grid has no
       area for it, so it collapses to width 0: pin it as a sheet between the top bar and the player bar, and give every
       wrapper on the way down to the lines a real width. */
    body.sb-lyrics ${GRID} > div:has([data-testid="lyrics-line"]) {
      grid-area: auto !important; position: fixed !important; left: 0; right: 0; top: 56px; bottom: calc(96px + var(--sb-nav)); z-index: 65;
      width: auto !important; height: auto !important; overflow: auto !important; background: #121212; box-sizing: border-box;
    }
    body.sb-lyrics ${GRID} > div:has([data-testid="lyrics-line"]) div:has([data-testid="lyrics-line"]) {
      position: static !important; width: 100% !important; min-width: 0 !important; max-width: 100% !important;
      height: auto !important; transform: none !important; overflow: visible !important; box-sizing: border-box;
    }
    body.sb-lyrics [data-testid="lyrics-line"] { width: auto !important; padding: 0 20px; box-sizing: border-box; }
    /* the panel's overlay scrollbars are empty flex siblings of the content and would split the row: the sheet scrolls natively */
    body.sb-panel #main-view + div .os-scrollbar { display: none !important; }
    /* player bar: [cover+title | prev play next] on top, progress underneath, extras (lyrics, volume...) dropped */
    div:has(> [data-testid="now-playing-bar"]) { margin: 0 !important; padding: 0 !important; }
    div:has(> [data-testid="now-playing-bar"]), [data-testid="now-playing-bar"] { min-width: 0 !important; width: 100% !important; box-sizing: border-box; }
    [data-testid="now-playing-bar"] > div {
      min-width: 0 !important;
      flex-wrap: wrap !important; width: 100% !important; height: auto !important;
      padding: 4px 8px !important; box-sizing: border-box; gap: 0 !important;
    }
    [data-testid="now-playing-bar"] > div > :nth-child(1) { flex: 1 1 0 !important; min-width: 0 !important; width: auto !important; }
    [data-testid="now-playing-bar"] > div > :nth-child(2), [data-testid="player-controls"] { display: contents !important; }
    [data-testid="general-controls"] { flex: none !important; width: auto !important; }
    [data-testid="general-controls"] button:not([data-testid]), [data-testid="control-button-repeat"] { display: none !important; }
    [data-testid="player-controls"] > :nth-child(2) { flex: 1 1 100% !important; order: 3; width: 100% !important; }
    /* right block: keep only "Conectar a un dispositivo" (3rd item), drop lyrics, queue and volume */
    [data-testid="now-playing-bar"] > div > :nth-child(3) { display: flex !important; flex: none !important; width: auto !important; min-width: 0 !important; }
    [data-testid="now-playing-bar"] > div > :nth-child(3) > div { min-width: 0 !important; width: auto !important; }
    [data-testid="now-playing-bar"] > div > :nth-child(3) > div > :not(:nth-child(3)) { display: none !important; }

    /* expanded now-playing sheet (built by boost.js) */
    #sb-np { display: none; position: fixed; inset: 0; z-index: 70; flex-direction: column; box-sizing: border-box;
      padding: 8px 22px 18px; height: 100dvh; background: linear-gradient(#303030, #121212 60%); color: #fff;
      font-family: inherit; user-select: none; -webkit-user-select: none; }
    body.sb-np-open #sb-np { display: flex; }
    #sb-np button { background: none; border: 0; color: #fff; padding: 0; cursor: pointer; display: inline-flex;
      align-items: center; justify-content: center; opacity: .85; }
    #sb-np button.on { color: #1db954; opacity: 1; }
    #sb-np button:disabled { opacity: .3; }
    #sb-np svg { width: 26px; height: 26px; fill: currentColor; }
    #sb-np .sb-top { display: flex; align-items: center; justify-content: space-between; height: 44px; font-size: 13px; font-weight: 700; }
    #sb-np .sb-top button { width: 40px; height: 40px; }
    #sb-np .sb-cover { flex: 1 1 auto; min-height: 0; display: flex; align-items: center; justify-content: center; }
    #sb-np .sb-cover img { width: 100%; max-height: 100%; aspect-ratio: 1; object-fit: cover; border-radius: 8px; background: #282828;
      box-shadow: 0 10px 34px #0009; }
    #sb-np .sb-meta { display: flex; align-items: center; gap: 12px; margin-top: 16px; }
    #sb-np .sb-meta > div { flex: 1; min-width: 0; }
    #sb-np .sb-title { font-size: 21px; font-weight: 800; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
    #sb-np .sb-artist { font-size: 15px; color: #b3b3b3; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; margin-top: 2px; }
    #sb-np input[type=range] { width: 100%; margin: 14px 0 0; accent-color: #fff; height: 28px; }
    #sb-np .sb-times { display: flex; justify-content: space-between; font-size: 12px; color: #b3b3b3; margin-top: -4px; }
    #sb-np .sb-ctl { display: flex; align-items: center; justify-content: space-between; margin-top: 14px; }
    #sb-np .sb-ctl button { width: 52px; height: 52px; }
    #sb-np .sb-ctl button svg { width: 30px; height: 30px; }
    #sb-np .sb-ctl button.sb-play { width: 68px; height: 68px; border-radius: 50%; background: #fff; color: #000; opacity: 1; }
    #sb-np .sb-ctl button.sb-play svg { width: 34px; height: 34px; }
    #sb-np .sb-bot { display: flex; justify-content: space-around; margin-top: 12px; }
    #sb-np .sb-bot button { width: 56px; height: 44px; }
    #sb-lib-btn {
      flex: none; width: 40px; height: 40px; border: 0; border-radius: 50%; margin: 0 4px;
      background: #1f1f1f; color: #fff; font-size: 20px; line-height: 40px; text-align: center;
    }
  `;
  function apply() {
    var h = document.head || document.documentElement;
    if (!h) return false;
    var m = document.querySelector('meta[name=viewport]');
    if (!m) { m = document.createElement('meta'); m.name = 'viewport'; h.appendChild(m); }
    m.content = 'width=device-width, initial-scale=1, viewport-fit=cover';
    var s = document.createElement('style');
    s.id = '__sb_css'; s.textContent = css; h.appendChild(s);
    return true;
  }
  // The SPA rebuilds its DOM on navigation, so (re)attach the library button when the nav bar shows up.
  function addBtn() {
    var bar = document.querySelector('[data-testid="global-nav-bar"]');
    if (!bar || document.getElementById('sb-lib-btn')) return;
    var b = document.createElement('button');
    b.id = 'sb-lib-btn'; b.textContent = '☰'; b.setAttribute('aria-label', 'Biblioteca');
    b.onclick = function () { document.body.classList.toggle('sb-lib'); };
    bar.insertBefore(b, bar.firstChild);
  }
  // Social logins don't work in an embedded WebView (Google answers 403 disallowed_useragent), so hide them on
  // Spotify's login pages. Spotify's own links are /login/<provider>; text is the fallback.
  function hideSocial() {
    if (!/(^|\.)accounts\.spotify\.com$/.test(location.hostname)) return;
    document.querySelectorAll('a, button, [role=button]').forEach(function (e) {
      if (e.style.display === 'none') return;
      var t = (e.textContent || '').trim();
      if (/\/login\/(google|facebook|apple)\b/.test(e.getAttribute('href') || '') ||
          (t.length < 50 && /google|facebook|apple/i.test(t + ' ' + (e.getAttribute('aria-label') || '')))) {
        e.style.display = 'none';
      }
    });
  }
  function onMutate() { addBtn(); hideSocial(); }

  // ---- expanded "now playing" sheet: mirrors the bar's state, drives the bar's own buttons
  var IC = {
    play: '<svg viewBox="0 0 24 24"><path d="M8 5v14l11-7z"/></svg>',
    pause: '<svg viewBox="0 0 24 24"><path d="M6 5h4v14H6zM14 5h4v14h-4z"/></svg>',
    prev: '<svg viewBox="0 0 24 24"><path d="M6 6h2v12H6zM9.5 12L18 18V6z"/></svg>',
    next: '<svg viewBox="0 0 24 24"><path d="M16 6h2v12h-2zM6 18l8.5-6L6 6z"/></svg>',
    shuf: '<svg viewBox="0 0 24 24"><path d="M10.6 9.2L5.4 4 4 5.4l5.2 5.2zM14.5 4l2.5 2.5L4 19.6 5.4 21 18.5 7.9 21 10.5V4zM14.8 13.4l-1.4 1.4 3.6 3.6-2.5 2.5H21v-6.5l-2.5 2.5z"/></svg>',
    rep: '<svg viewBox="0 0 24 24"><path d="M7 7h10v3l4-4-4-4v3H5v6h2zM17 17H7v-3l-4 4 4 4v-3h12v-6h-2z"/></svg>',
    down: '<svg viewBox="0 0 24 24"><path d="M7.4 8.6L12 13.2l4.6-4.6L18 10l-6 6-6-6z"/></svg>',
    queue: '<svg viewBox="0 0 24 24"><path d="M3 6h13v2H3zM3 11h13v2H3zM3 16h8v2H3zM18 11v6.3a2.5 2.5 0 1 0 2 2.4V14h2v-3z"/></svg>',
    lyr: '<svg viewBox="0 0 24 24"><path d="M4 4h16v13H8l-4 4zM7 8v2h10V8zm0 4v2h7v-2z"/></svg>',
    dev: '<svg viewBox="0 0 24 24"><path d="M4 6h10v2H4v8h6v2H4a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2zm12 4h6a1 1 0 0 1 1 1v9a1 1 0 0 1-1 1h-6a1 1 0 0 1-1-1v-9a1 1 0 0 1 1-1zm3 2.5a1.5 1.5 0 1 0 0 3 1.5 1.5 0 0 0 0-3zm0 4a2 2 0 1 0 0 4 2 2 0 0 0 0-4z"/></svg>',
    like: '<svg viewBox="0 0 24 24"><path d="M12 21s-7-4.6-9.3-9A5.4 5.4 0 0 1 12 6.1 5.4 5.4 0 0 1 21.3 12C19 16.4 12 21 12 21z"/></svg>'
  };
  function q1(sel, root) { return (root || document).querySelector(sel); }
  function origs() {
    var b = q1('[data-testid="now-playing-bar"]'); if (!b) return null;
    var gc = q1('[data-testid="general-controls"]', b), btns = [].slice.call(b.querySelectorAll('button'));
    return {
      b: b, play: q1('[data-testid="control-button-playpause"]', b), prev: q1('[data-testid="control-button-skip-back"]', b),
      next: q1('[data-testid="control-button-skip-forward"]', b), repeat: q1('[data-testid="control-button-repeat"]', b),
      shuffle: gc && gc.querySelector('button:not([data-testid])'),
      queue: q1('[data-testid="control-button-queue"]', b), lyrics: q1('[data-testid="lyrics-button"]', b),
      connect: btns.filter(function (x) { return /dispositivo|device/i.test(x.getAttribute('aria-label') || ''); })[0],
      like: btns.filter(function (x) { return !x.dataset.testid && /^(true|false)$/.test(x.getAttribute('aria-checked') || '') &&
        /playlist|gusta|liked|saved|guardad|agregar|quitar|add to|remove from/i.test(x.getAttribute('aria-label') || ''); })[0],
      range: q1('[data-testid="playback-progressbar"] input[type=range]', b),
      pos: q1('[data-testid="playback-position"]', b), dur: q1('[data-testid="playback-duration"]', b),
      title: q1('[data-testid="context-item-link"]', b), artist: q1('[data-testid="context-item-info-artist"]', b),
      img: q1('[data-testid="now-playing-widget"] img', b)
    };
  }
  function hasNpUi() { return !!q1('#sb-np'); }
  function buildNp() {
    if (hasNpUi() || !document.body) return;
    var d = document.createElement('div'); d.id = 'sb-np';
    d.innerHTML =
      '<div class="sb-top"><button data-a="close" aria-label="Cerrar">' + IC.down + '</button><span>En reproducción</span><span style="width:40px"></span></div>' +
      '<div class="sb-cover"><img alt=""></div>' +
      '<div class="sb-meta"><div><div class="sb-title"></div><div class="sb-artist"></div></div><button data-a="like" aria-label="Me gusta">' + IC.like + '</button></div>' +
      '<input type="range" min="0" max="1" value="0" step="1000"><div class="sb-times"><span class="sb-pos">0:00</span><span class="sb-dur">0:00</span></div>' +
      '<div class="sb-ctl"><button data-a="shuffle" aria-label="Aleatorio">' + IC.shuf + '</button><button data-a="prev" aria-label="Anterior">' + IC.prev + '</button>' +
      '<button data-a="play" class="sb-play" aria-label="Reproducir">' + IC.play + '</button><button data-a="next" aria-label="Siguiente">' + IC.next + '</button>' +
      '<button data-a="repeat" aria-label="Repetir">' + IC.rep + '</button></div>' +
      '<div class="sb-bot"><button data-a="connect" aria-label="Dispositivos">' + IC.dev + '</button><button data-a="lyrics" aria-label="Letra">' + IC.lyr + '</button><button data-a="queue" aria-label="Cola">' + IC.queue + '</button></div>';
    document.body.appendChild(d);
    var dragging = false, seek = q1('input[type=range]', d);
    seek.addEventListener('pointerdown', function () { dragging = true; });
    seek.addEventListener('change', function () {
      var o = origs(); dragging = false; if (!o || !o.range) return;
      Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'value').set.call(o.range, seek.value);
      o.range.dispatchEvent(new Event('input', { bubbles: true })); o.range.dispatchEvent(new Event('change', { bubbles: true }));
    });
    d.addEventListener('click', function (e) {
      var btn = e.target.closest && e.target.closest('button[data-a]'); if (!btn) return;
      var o = origs(), a = btn.dataset.a; if (!o) return;
      if (a === 'close') return closeNp();
      if (a === 'connect' || a === 'lyrics' || a === 'queue') { closeNp(); setTimeout(function () { if (o[a]) o[a].click(); }, 120); return; }
      if (o[a]) o[a].click();
      setTimeout(syncNp, 150);
    });
    d._dragging = function () { return dragging; };
  }
  function syncNp() {
    var d = q1('#sb-np'), o = origs(); if (!d || !o || !document.body.classList.contains('sb-np-open')) return;
    var img = d.querySelector('.sb-cover img'), src = o.img ? (o.img.currentSrc || o.img.src) : '';
    src = src.replace(/(ab67616d0000)[0-9a-f]{4}/, '$1b273'); // same album art, 640px
    if (src && img.dataset.s !== src) { img.src = src; img.dataset.s = src; }
    d.querySelector('.sb-title').textContent = o.title ? o.title.textContent : '';
    d.querySelector('.sb-artist').textContent = o.artist ? o.artist.textContent : '';
    var playing = o.play && /pausar|pause/i.test(o.play.getAttribute('aria-label') || '');
    var pb = d.querySelector('[data-a=play]'); if (pb.dataset.p !== String(playing)) { pb.dataset.p = String(playing); pb.innerHTML = playing ? IC.pause : IC.play; }
    var seek = q1('input[type=range]', d);
    if (o.range && !d._dragging()) { seek.max = o.range.max; seek.value = o.range.value; }
    d.querySelector('.sb-pos').textContent = o.pos ? o.pos.textContent : '0:00';
    d.querySelector('.sb-dur').textContent = o.dur ? o.dur.textContent : '0:00';
    d.querySelector('[data-a=shuffle]').classList.toggle('on', !!(o.shuffle && /desactivar|disable|turn off/i.test(o.shuffle.getAttribute('aria-label') || '')));
    var rp = o.repeat ? o.repeat.getAttribute('aria-checked') : 'false';
    d.querySelector('[data-a=repeat]').classList.toggle('on', rp === 'true' || rp === 'mixed');
    d.querySelector('[data-a=like]').classList.toggle('on', !!(o.like && o.like.getAttribute('aria-checked') === 'true'));
    d.querySelector('[data-a=like]').style.visibility = o.like ? 'visible' : 'hidden';
    ['lyrics', 'queue', 'connect'].forEach(function (k) { d.querySelector('[data-a=' + k + ']').disabled = !o[k] || !!o[k].disabled; });
  }
  // Android Back: the Gecko app talks to us through a native port (its canGoBack ignores pushState entries); the plain
  // WebView app can rely on history entries instead.
  var native = null;
  try {
    if (typeof browser !== 'undefined' && browser.runtime && browser.runtime.connect) {
      native = browser.runtime.connect({ name: 'sb' });  // -> background page -> app (content scripts can't use native messaging)
      native.onMessage.addListener(function (m) { if (m && m.back) closeTop(); });
    }
  } catch (e) { native = null; }
  function overlayOpen() { var c = document.body.classList; return c.contains('sb-np-open') || c.contains('sb-panel') || c.contains('sb-lyrics'); }
  var lastReported = null;
  function report() {
    var o = overlayOpen(); if (!native || o === lastReported) return; lastReported = o;
    try { native.postMessage({ overlay: o }); } catch (e) {}
  }
  function openNp() {
    buildNp(); if (!hasNpUi() || !origs()) return;
    document.body.classList.add('sb-np-open');
    if (!native) { try { history.pushState({ sbnp: 1 }, ''); } catch (e) {} }
    syncNp(); report();
  }
  function closeNp() {
    if (!native && history.state && history.state.sbnp) history.back(); else document.body.classList.remove('sb-np-open');
    report();
  }
  // Back with an overlay up: close the topmost one (sheet first, then the devices/queue/lyrics panel)
  function closeTop() {
    if (document.body.classList.contains('sb-np-open')) { document.body.classList.remove('sb-np-open'); }
    else if (document.body.classList.contains('sb-panel') || document.body.classList.contains('sb-lyrics')) {
      var o = origs(); if (o) [o.queue, o.lyrics, o.connect].forEach(function (b) { if (b && b.getAttribute('aria-pressed') === 'true') b.click(); });
    }
    setTimeout(report, 300);
  }
  window.addEventListener('popstate', function () { document.body.classList.remove('sb-np-open'); });
  // tap the cover/title in the bar -> expand (the like button and the other bar buttons keep working)
  document.addEventListener('click', function (e) {
    var t = e.target; if (!t.closest || !t.closest('[data-testid="now-playing-widget"]')) return;
    var b = t.closest('button'); if (b && !/cover-art-button/.test(b.dataset.testid || '')) return;
    e.preventDefault(); e.stopPropagation(); openNp();
  }, true);
  setInterval(syncNp, 500);
  // ---- bottom navigation
  var NAV = [
    ['home', 'Inicio', '<svg viewBox="0 0 24 24"><path d="M12 3 2 12h3v8h5v-6h4v6h5v-8h3z"/></svg>'],
    ['search', 'Buscar', '<svg viewBox="0 0 24 24"><path d="M10.5 3a7.5 7.5 0 0 1 5.9 12.1l4.3 4.3-1.4 1.4-4.3-4.3A7.5 7.5 0 1 1 10.5 3zm0 2a5.5 5.5 0 1 0 0 11 5.5 5.5 0 0 0 0-11z"/></svg>'],
    ['library', 'Biblioteca', '<svg viewBox="0 0 24 24"><path d="M4 4h2v16H4zM8 4h2v16H8zM12.4 5.2l1.9-.6 4.6 15.3-1.9.6z"/></svg>']
  ];
  function buildNav() {
    if (q1('#sb-nav') || !document.body) return;
    var n = document.createElement('div'); n.id = 'sb-nav';
    n.innerHTML = NAV.map(function (i) { return '<button data-n="' + i[0] + '">' + i[2] + '<span>' + i[1] + '</span></button>'; }).join('');
    document.body.appendChild(n);
    n.addEventListener('click', function (e) {
      var b = e.target.closest && e.target.closest('button[data-n]'); if (!b) return;
      var k = b.dataset.n, cl = document.body.classList;
      if (overlayOpen()) closeTop();                     // close lyrics / queue / devices first
      if (k === 'library') { cl.toggle('sb-lib'); return; }
      cl.remove('sb-lib');
      if (k === 'home') { var h = q1('[data-testid="home-button"]'); if (h) h.click(); }
      else {
        var i = q1('[data-testid="search-icon"]'); if (i) (i.closest('button, a, [role=button]') || i).click();
        setTimeout(function () { var f = q1('[data-testid="search-input"]'); if (f) f.focus(); }, 250);
      }
    });
  }
  var navPath = location.pathname;
  function syncNav() {
    var n = q1('#sb-nav'); if (!n) return;
    if (location.pathname !== navPath) { navPath = location.pathname; document.body.classList.remove('sb-lib'); } // picked something: close the library
    var p = location.pathname, cl = document.body.classList;
    var cur = cl.contains('sb-lib') ? 'library' : /\/search(\/|$)/.test(p) ? 'search' : /^\/(intl-[a-z-]+\/)?$/.test(p) ? 'home' : '';
    [].forEach.call(n.querySelectorAll('button'), function (b) { b.classList.toggle('on', b.dataset.n === cur); });
  }
  // Spotify keeps the library in a collapsed icon rail unless its own toggle ("Abre Tu biblioteca") is pressed
  var lastExpand = 0;
  function expandLibrary() {
    var lib = q1('#Desktop_LeftSidebar_Id'); if (!lib || !document.body.classList.contains('sb-lib')) return;
    var t = lib.querySelector('button');
    if (t && /^(abre|open|expand|ampl)/i.test(t.getAttribute('aria-label') || '') && Date.now() - lastExpand > 1500) { lastExpand = Date.now(); t.click(); }
  }
  setInterval(function () { buildNav(); syncNav(); expandLibrary(); }, 400);
  // right panel (devices / queue / lyrics) is shown as a sheet only while its bar button is pressed
  setInterval(function () {
    var o = origs(); if (!o) return;
    var on = function (x) { return !!x && x.getAttribute('aria-pressed') === 'true'; };
    document.body.classList.toggle('sb-panel', on(o.queue) || on(o.connect));
    document.body.classList.toggle('sb-lyrics', on(o.lyrics));
    report();
  }, 400);
  if (!apply()) document.addEventListener('DOMContentLoaded', apply);
  new MutationObserver(onMutate).observe(document, { childList: true, subtree: true });
})();
