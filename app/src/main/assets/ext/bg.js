// Relay between the page's content script and the Android app (native messaging is only allowed from here):
//   content --{overlay:bool}--> app     app --{back:true}--> content
let content = null, nat = null;
function app() {
  if (!nat) {
    nat = browser.runtime.connectNative('browser');
    nat.onMessage.addListener(m => { if (content) content.postMessage(m); });
    nat.onDisconnect.addListener(() => { nat = null; });
  }
  return nat;
}
browser.runtime.onConnect.addListener(p => {
  content = p;
  p.onMessage.addListener(m => app().postMessage(m));
  p.onDisconnect.addListener(() => { if (content === p) { content = null; if (nat) nat.postMessage({ overlay: false }); } });
});
