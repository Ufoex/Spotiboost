// Runs in the page's own context (injected by boost.js). Finds Spotify's Platform object through React's tree
// (Spicetify patches the bundle for the same object) and gets rid of audio ads, like the adblockify extension does.
(function () {
  if (window.__sbPage) return; window.__sbPage = 1;
  const log = (...a) => console.log('SBPAGE', ...a);
  function findPlatform() {
    const rootEl = [document.body, ...document.body.children].find(e => Object.keys(e).some(k => k.startsWith('__reactContainer$')));
    if (!rootEl) return null;
    const stack = [rootEl[Object.keys(rootEl).find(k => k.startsWith('__reactContainer$'))]];
    for (let n = 0; stack.length && n < 300000; n++) {
      const f = stack.pop(), p = f.memoizedProps;
      if (p && p.platform && p.platform.version) return p.platform;
      if (f.sibling) stack.push(f.sibling);
      if (f.child) stack.push(f.child);
    }
    return null;
  }
  const played = new Set(), play = HTMLMediaElement.prototype.play;
  HTMLMediaElement.prototype.play = function () { played.add(this); return play.apply(this, arguments); };
  const wait = setInterval(() => {
    const p = findPlatform();
    if (!p) return;
    clearInterval(wait);
    const reg = p.getRegistry();
    const player = reg.resolve([...reg._map.keys()].find(k => k.description === 'PlayerAPI'));
    const isAd = () => { const it = player._state && player._state.item; return !!it && (it.type === 'ad' || (it.metadata && it.metadata.is_advertisement === 'true')); };
    // Skipping an ad is refused by the server and the in-stream API ignores it, so play it out silently and at once.
    const touched = new Set();
    let since = 0;
    log('ready v' + p.version);
    setInterval(() => {
      if (!isAd()) {
        if (since) { log('ad over after', Date.now() - since, 'ms'); since = 0; }
        touched.forEach(m => { m.muted = false; m.playbackRate = 1; }); touched.clear();
        return;
      }
      if (!since) { since = Date.now(); log('AD', player._state.item.uri); }
      [...played].filter(m => /adstudio/.test(m.currentSrc)).forEach(m => {
        if (!touched.has(m)) { touched.add(m); log('muting ad media, duration', m.duration); }
        m.muted = true; m.playbackRate = 16;
        if (m.duration > 0 && isFinite(m.duration) && m.currentTime < m.duration - 0.3) m.currentTime = m.duration - 0.2;
      });
    }, 250);
  }, 500);
})();
