// prototype diagnostics -> logcat (GeckoConsole): Widevine, autoplay policy, UA
(function () {
  if (window.__sbDiag) return; window.__sbDiag = 1;
  const log = (...a) => console.log('SBDIAG', ...a);
  log('ua', navigator.userAgent);
  try { log('autoplayPolicy(mediaelement)', navigator.getAutoplayPolicy('mediaelement')); } catch (e) { log('policy err', e.message); }
  navigator.requestMediaKeySystemAccess('com.widevine.alpha',
    [{ initDataTypes: ['cenc'], audioCapabilities: [{ contentType: 'audio/mp4;codecs="mp4a.40.2"' }] }])
    .then(a => log('widevine OK', a.keySystem), e => log('widevine FAIL', e.name, e.message));
  // --- layout measurements (fixed, no input channel): logged on every page change, for tuning boost.js
  function measure(tag) {
    const mv = document.querySelector('#main-view'); if (!mv) return;
    const r = e => { const b = e.getBoundingClientRect(); return Math.round(b.x) + ',' + Math.round(b.y) + ' ' + Math.round(b.width) + 'x' + Math.round(b.height); };
    const cs = (e, ...p) => p.map(k => k + '=' + getComputedStyle(e)[k]).join(' ');
    const out = ['LAYOUT ' + tag + ' ' + location.pathname + ' vw=' + innerWidth + ' docSW=' + document.documentElement.scrollWidth];
    out.push('main-view ' + r(mv) + ' ' + cs(mv, 'margin', 'padding', 'borderRadius', 'backgroundColor'));
    const chain = []; let e = mv;
    for (let i = 0; i < 6 && e; i++) { e = e.firstElementChild; if (e) chain.push((e.className.toString().split(' ')[0] || e.tagName) + ' ' + r(e) + ' pad=' + getComputedStyle(e).padding + ' bg=' + getComputedStyle(e).backgroundColor.replace(/ /g, '') + ' br=' + getComputedStyle(e).borderRadius); }
    out.push('chain: ' + chain.join(' > '));
    const sn = mv.querySelector('.main-view-container__scroll-node, [data-overlayscrollbars-viewport]') || mv;
    out.push('scroll-node ' + r(sn) + ' scrollW=' + sn.scrollWidth + ' clientW=' + sn.clientWidth + ' ' + cs(sn, 'overflowX', 'overflowY'));
    const wide = [...mv.querySelectorAll('*')].map(x => [x, x.getBoundingClientRect()]).filter(([x, b]) => b.width > innerWidth + 2 || b.right > innerWidth + 2)
      .sort((a, b) => b[1].width - a[1].width).slice(0, 5)
      .map(([x, b]) => (x.dataset.testid || x.className.toString().split(' ')[0] || x.tagName) + ' ' + Math.round(b.x) + '..' + Math.round(b.right) + ' w=' + Math.round(b.width) + ' ' + cs(x, 'display', 'minWidth', 'gridTemplateColumns').slice(0, 90));
    out.push('wider than viewport: ' + (wide.join(' | ') || 'none'));
    const tl = mv.querySelector('[data-testid="track-list"], [role="grid"]');
    if (tl) { const row = tl.querySelector('[role="row"][aria-rowindex]') || tl.querySelector('[role="row"]'); out.push('tracklist ' + r(tl) + ' ' + cs(tl, 'minWidth', 'width') + (row ? ' | row ' + r(row) + ' ' + cs(row, 'display', 'gridTemplateColumns', 'minWidth').slice(0, 110) : '')); }
    // horizontal overflow measured against the panel itself (the viewport can scroll sideways by a few px)
    const vp = [...mv.querySelectorAll('[data-overlayscrollbars-viewport], .os-viewport, .main-view-container__scroll-node')].map(x => (x.className.toString().split(' ')[0] || x.tagName) + ' sw=' + x.scrollWidth + ' cw=' + x.clientWidth + ' ox=' + getComputedStyle(x).overflowX + ' oy=' + getComputedStyle(x).overflowY);
    out.push('viewports: ' + vp.join(' | '));
    const mvr = mv.getBoundingClientRect();
    const over = [...mv.querySelectorAll('*')].map(x => [x, x.getBoundingClientRect()]).filter(([x, b]) => b.width > 0 && (b.right > mvr.right + 1 || b.left < mvr.left - 1))
      .sort((a, b) => (b[1].right - mvr.right) - (a[1].right - mvr.right)).slice(0, 5)
      .map(([x, b]) => (x.dataset.testid || x.className.toString().split(' ')[0] || x.tagName) + ' ' + Math.round(b.left - mvr.left) + '..' + Math.round(b.right - mvr.left) + ' of ' + Math.round(mvr.width));
    out.push('overflow vs panel: ' + (over.join(' | ') || 'none'));
    const root = getComputedStyle(document.documentElement), mc = getComputedStyle(mv);
    out.push('vars: ' + ['--content-spacing', '--content-max-width', '--shelf-fixed-width', '--encore-spacing-base'].map(k => k + '=' + (mc.getPropertyValue(k) || root.getPropertyValue(k)).trim()).join(' '));
    const hs = [...mv.querySelectorAll('.os-scrollbar-horizontal, [class*=os-scrollbar-horizontal]')].map(x => x.className.toString().split(' ').slice(0, 3).join('.') + ' ' + r(x) + ' disp=' + getComputedStyle(x).display);
    out.push('h-scrollbars: ' + (hs.join(' | ') || 'none'));
    // which ancestors of the first content tile add side padding/margin
    const tile = mv.querySelector('a[href*="/playlist/"], a[href*="/album/"], [data-testid="track-list"], [data-testid="top-sentinel"] ~ *');
    if (tile) { const ch = []; let x = tile; while (x && x !== mv.parentElement) { const c = getComputedStyle(x); const pl = parseFloat(c.paddingLeft) + parseFloat(c.marginLeft), pr = parseFloat(c.paddingRight) + parseFloat(c.marginRight);
      if (pl || pr) ch.push((x.dataset.testid || x.className.toString().split(' ')[0] || x.tagName) + ' L=' + pl + ' R=' + pr + ' [' + Math.round(x.getBoundingClientRect().left) + '..' + Math.round(x.getBoundingClientRect().right) + ']'); x = x.parentElement; }
      out.push('side spacing (inner->outer): ' + ch.join(' < '));
      let y = tile; while (y && y !== mv.parentElement) { const c = getComputedStyle(y); if (parseFloat(c.paddingLeft) >= 30) { const up = []; let z = y.parentElement; for (let k = 0; k < 4 && z; k++, z = z.parentElement) up.push(z.tagName + (z.dataset.testid ? '[' + z.dataset.testid + ']' : '') + (z.id ? '#' + z.id : ''));
        out.push('padded el: ' + y.tagName + ' testid=' + (y.dataset.testid || '-') + ' attrs=' + [...y.attributes].map(a => a.name).filter(n => n !== 'class').join(',') + ' kids=' + y.children.length + ' firstKidTestid=' + ((y.firstElementChild || {dataset: {}}).dataset.testid || '-') + ' parents=' + up.join('<') + ' paddingInline=' + c.paddingInline + ' cssText-ish=' + (y.getAttribute('style') || '')); break; } y = y.parentElement; } }
    out.forEach(l => log(l));
  }
  let lastPath = '';
  setInterval(() => { if (location.pathname !== lastPath && document.querySelector('#main-view')) { lastPath = location.pathname; setTimeout(() => measure('t+3s'), 3000); setTimeout(() => measure('t+7s'), 7000); } }, 1500);
  // --- lyrics view: what is inside while it is open (fixed logging only)
  let lyLog = 0;
  setInterval(() => {
    const btn = document.querySelector('[data-testid="lyrics-button"]');
    if (!btn || btn.getAttribute('aria-pressed') !== 'true' || lyLog++ > 4) { if (!btn || btn.getAttribute('aria-pressed') !== 'true') lyLog = 0; return; }
    const c = document.querySelector('.Root__lyrics-cinema, #lyrics-cinema, [data-testid="lyrics-cinema"]');
    const rr = e => { const b = e.getBoundingClientRect(); return Math.round(b.x) + ',' + Math.round(b.y) + ' ' + Math.round(b.width) + 'x' + Math.round(b.height); };
    if (!c) { log('LYRICS pressed but no lyrics-cinema element; body classes=' + document.body.className); return; }
    const cs = getComputedStyle(c);
    log('LYRICS cinema ' + rr(c) + ' disp=' + cs.display + ' pos=' + cs.position + ' bg=' + cs.backgroundColor + ' vis=' + cs.visibility + ' op=' + cs.opacity + ' kids=' + c.children.length + ' textLen=' + c.innerText.length + ' text=' + JSON.stringify(c.innerText.slice(0, 160)));
    const kids = [...c.querySelectorAll('*')].filter(e => e.children.length === 0 && (e.innerText || '').trim()).slice(0, 4).map(e => e.tagName + ' ' + rr(e) + ' color=' + getComputedStyle(e).color + ' op=' + getComputedStyle(e).opacity + ' "' + e.innerText.trim().slice(0, 30) + '"');
    log('LYRICS leaves: ' + (kids.join(' | ') || 'none'));
    const ln = document.querySelector('[data-testid="lyrics-line"], [class*="lyrics-line"]');
    if (ln) { const ch = []; let x = ln; for (let i = 0; i < 14 && x && x !== document.body; i++, x = x.parentElement) { const cs2 = getComputedStyle(x);
      ch.push((x.id ? '#' + x.id : (x.dataset.testid ? '[' + x.dataset.testid + ']' : (x.className.toString().split(' ')[0] || x.tagName))) + ' ' + rr(x) + ' ' + cs2.display + (cs2.position !== 'static' ? ' ' + cs2.position : '') + (cs2.overflow !== 'visible' ? ' ov=' + cs2.overflow : '')); }
      log('LYRICS chain (line->body): ' + ch.join(' < ')); log('LYRICS line text=' + JSON.stringify(ln.innerText.slice(0, 50)) + ' color=' + getComputedStyle(ln).color); }
    const cn = [...document.querySelectorAll('[class*="lyrics"], [data-testid*="lyrics"]')].slice(0, 8).map(e => (e.dataset.testid || e.className.toString().split(' ')[0]) + ' ' + rr(e) + ' ' + getComputedStyle(e).display);
    log('LYRICS els: ' + cn.join(' | '));
  }, 1500);
  // --- library drawer: which controls can expand the collapsed rail (fixed logging only)
  let libLog = 0;
  setInterval(() => {
    if (!document.body.classList.contains('sb-lib')) { libLog = 0; return; }
    if (libLog++ > 1) return;
    const sb = document.querySelector('#Desktop_LeftSidebar_Id'); if (!sb) { log('LIB no sidebar'); return; }
    const b = [...sb.querySelectorAll('button, [role=button], [role=radio], [role=switch]')].slice(0, 14)
      .map(x => (x.dataset.testid || '-') + '|' + (x.getAttribute('aria-label') || x.innerText || '').trim().slice(0, 40) + '|' + (x.getAttribute('aria-pressed') || x.getAttribute('aria-checked') || x.getAttribute('aria-expanded') || ''));
    log('LIB buttons: ' + b.join(' ;; '));
    const r = sb.getBoundingClientRect(); const rows = sb.querySelectorAll('[role=row], li, [data-encore-id="listRow"]').length;
    log('LIB sidebar ' + Math.round(r.width) + 'x' + Math.round(r.height) + ' rows=' + rows + ' cls=' + sb.className.toString().split(' ').slice(0, 2).join('.') + ' cols=' + getComputedStyle(sb.parentElement).gridTemplateColumns);
    const g = document.querySelector('[data-testid="root"], .Root, body > div'); const v = {};
    ['--left-sidebar-width', '--left-sidebar-min-width', '--panel-width'].forEach(k => v[k] = (getComputedStyle(document.documentElement).getPropertyValue(k) || getComputedStyle(sb.parentElement).getPropertyValue(k)).trim());
    log('LIB vars: ' + JSON.stringify(v));
  }, 800);
})();
