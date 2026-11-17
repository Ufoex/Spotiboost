package com.ufoex.spotiboost.gecko;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.FrameLayout;


import org.mozilla.geckoview.GeckoResult;
import org.mozilla.geckoview.GeckoRuntime;
import org.mozilla.geckoview.GeckoRuntimeSettings;
import org.mozilla.geckoview.GeckoSession;
import org.mozilla.geckoview.GeckoSessionSettings;
import org.mozilla.geckoview.GeckoView;
import org.mozilla.geckoview.MediaSession;
import org.mozilla.geckoview.WebExtension;

public class MainActivity extends Activity {
    private static GeckoRuntime runtime; // one per process
    private static org.mozilla.geckoview.GeckoResult<WebExtension> extension;
    private GeckoSession session;
    private boolean canGoBack;
    private FrameLayout root;
    private boolean fullScreen;         // the page asked for element fullscreen (e.g. Spotify's lyrics fullscreen button)
    private volatile boolean overlay;   // a sheet (now playing / devices / queue) is up in the page
    private WebExtension.Port port;
    private PowerSaveWarning powerSave;
    private boolean askingNotifications;   // a system permission request is in flight
    private AlertDialog notificationsDialog;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        powerSave = new PowerSaveWarning(this);
        GeckoView view = new GeckoView(this);
        root = new FrameLayout(this);
        root.setBackgroundColor(0xFF121212);
        view.setAutofillEnabled(true);   // Android's autofill service (Google / Bitwarden...) fills saved logins
        root.addView(view);
        setContentView(root);
        root.setOnApplyWindowInsetsListener((v, in) -> {
            if (fullScreen) v.setPadding(0, 0, 0, 0);   // real fullscreen: the page draws under the status bar / camera cutout
            else v.setPadding(in.getSystemWindowInsetLeft(), in.getSystemWindowInsetTop(),
                    in.getSystemWindowInsetRight(), in.getSystemWindowInsetBottom());
            return in;
        });

        boolean debuggable = (getApplicationInfo().flags & android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0;
        if (runtime == null) {
            // Prefs via config file (this GeckoView has no runtime setters for them):
            //  - media.autoplay.default=0: the player calls play() after a network round-trip, with no user gesture left.
            //  - dom.serviceWorkers.enabled=false: Spotify's service worker re-fetches cover art with a User-Agent header;
            //    Gecko (unlike Chrome) sends it, which forces a CORS preflight that i.scdn.co rejects -> every cover fails.
            String cfg = new java.io.File(getFilesDir(), "geckoview-config.yaml").getAbsolutePath();
            try {
                java.nio.file.Files.write(java.nio.file.Paths.get(cfg),
                        "prefs:\n  media.autoplay.default: 0\n  media.autoplay.blocking_policy: 0\n  dom.serviceWorkers.enabled: false\n".getBytes());
            } catch (java.io.IOException e) { throw new RuntimeException(e); }
            runtime = GeckoRuntime.create(this, new GeckoRuntimeSettings.Builder()
                    .configFilePath(cfg)
                    .consoleOutput(debuggable)            // page console.* -> logcat (GeckoConsole)
                    .remoteDebuggingEnabled(debuggable)
                    .aboutConfigEnabled(debuggable)
                    .build());
            // The extension carries boost.js (CSS) and diag.js (logcat diagnostics).
            extension = runtime.getWebExtensionController()
                    .ensureBuiltIn("resource://android/assets/ext/", "boost@spotiboost.local");
        }

        // Desktop UA (mobile UA gets the "install the app" wall) but honour the viewport meta that boost.js injects.
        session = new GeckoSession(new GeckoSessionSettings.Builder()
                .userAgentMode(GeckoSessionSettings.USER_AGENT_MODE_DESKTOP)
                .viewportMode(GeckoSessionSettings.VIEWPORT_MODE_MOBILE)
                .build());
        // Without this delegate Gecko ignores requestFullscreen(): hide the system bars while the page is fullscreen.
        session.setContentDelegate(new GeckoSession.ContentDelegate() {
            @Override
            @SuppressWarnings("deprecation")
            public void onFullScreen(GeckoSession s, boolean on) {
                fullScreen = on;
                getWindow().getDecorView().setSystemUiVisibility(on
                        ? android.view.View.SYSTEM_UI_FLAG_FULLSCREEN | android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                          | android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                          | android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        : android.view.View.SYSTEM_UI_FLAG_VISIBLE);
                // without this the notch / camera cutout stays a black bar in portrait
                android.view.WindowManager.LayoutParams lp = getWindow().getAttributes();
                lp.layoutInDisplayCutoutMode = on ? android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                        : android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT;
                getWindow().setAttributes(lp);
                root.requestApplyInsets();
            }
        });
        session.setNavigationDelegate(new GeckoSession.NavigationDelegate() {
            @Override
            public void onCanGoBack(GeckoSession s, boolean can) { canGoBack = can; }
        });
        // Gecko refuses Widevine (EME) and autoplay until the embedding app approves them. Only for Spotify's own origins.
        session.setPermissionDelegate(new GeckoSession.PermissionDelegate() {
            @Override
            public GeckoResult<Integer> onContentPermissionRequest(GeckoSession s, ContentPermission perm) {
                boolean spotify = perm.uri != null && perm.uri.contains("spotify.com");
                // DRM, plus audible/inaudible autoplay: the player calls play() after a network round-trip, so Gecko's
                // autoplay policy would otherwise reject it (no user gesture left).
                boolean ok = perm.permission == PERMISSION_MEDIA_KEY_SYSTEM_ACCESS
                        || perm.permission == PERMISSION_AUTOPLAY_AUDIBLE
                        || perm.permission == PERMISSION_AUTOPLAY_INAUDIBLE;
                int answer = spotify && ok ? ContentPermission.VALUE_ALLOW : ContentPermission.VALUE_DENY;
                android.util.Log.i("SBPERM", "perm=" + perm.permission + " uri=" + perm.uri + " current=" + perm.value + " -> " + answer);
                return GeckoResult.fromValue(answer);
            }
        });
        // Page <-> app channel (content script -> background page -> here): the page tells us when a sheet is open so Back
        // can close it instead of leaving the app.
        extension.accept(ext -> ext.setMessageDelegate(new WebExtension.MessageDelegate() {
            @Override
            public void onConnect(WebExtension.Port p) {
                port = p;
                android.util.Log.i("SBPORT", "connected");
                p.setDelegate(new WebExtension.PortDelegate() {
                    @Override
                    public void onPortMessage(Object message, WebExtension.Port from) {
                        if (message instanceof org.json.JSONObject) overlay = ((org.json.JSONObject) message).optBoolean("overlay");
                        android.util.Log.i("SBPORT", "overlay=" + overlay);
                    }

                    @Override
                    public void onDisconnect(WebExtension.Port from) { if (port == from) { port = null; overlay = false; } }
                });
            }
        }, "browser"));
        // Gecko mirrors Spotify's navigator.mediaSession here: that drives the foreground service (background playback),
        // the notification / lock-screen controls and, in the other direction, play / pause / next / previous.
        session.setMediaSessionDelegate(new MediaSession.Delegate() {
            @Override public void onActivated(GeckoSession s, MediaSession m) { PlaybackService.gecko = m; PlaybackService.poke(MainActivity.this); }
            @Override public void onDeactivated(GeckoSession s, MediaSession m) {
                if (PlaybackService.gecko == m) { PlaybackService.gecko = null; PlaybackService.playing = false; PlaybackService.poke(MainActivity.this); }
            }
            @Override public void onMetadata(GeckoSession s, MediaSession m, MediaSession.Metadata md) {
                PlaybackService.title = md.title == null ? "" : md.title;
                PlaybackService.artist = md.artist == null ? "" : md.artist;
                PlaybackService.album = md.album == null ? "" : md.album;
                PlaybackService.art = null;
                if (md.artwork != null) md.artwork.getBitmap(512).accept(b -> { PlaybackService.art = b; PlaybackService.poke(MainActivity.this); }, e -> { });
                PlaybackService.poke(MainActivity.this);
            }
            @Override public void onFeatures(GeckoSession s, MediaSession m, long f) { PlaybackService.features = f; PlaybackService.poke(MainActivity.this); }
            @Override public void onPlay(GeckoSession s, MediaSession m) {
                PlaybackService.playing = true;
                session.setPriorityHint(GeckoSession.PRIORITY_HIGH); // keep the content process from being demoted in the background
                PlaybackService.poke(MainActivity.this);
            }
            @Override public void onPause(GeckoSession s, MediaSession m) {
                PlaybackService.playing = false;
                session.setPriorityHint(GeckoSession.PRIORITY_DEFAULT);
                PlaybackService.poke(MainActivity.this);
            }
            @Override public void onStop(GeckoSession s, MediaSession m) { onPause(s, m); }
            @Override public void onPositionState(GeckoSession s, MediaSession m, MediaSession.PositionState ps) {
                PlaybackService.pos = ps.position; PlaybackService.dur = ps.duration; PlaybackService.rate = ps.playbackRate;
                PlaybackService.poke(MainActivity.this);
            }
        });
        session.open(runtime);
        UpdateChecker.check(this);
        view.setSession(session);
        session.loadUri("https://open.spotify.com/");
    }

    // Ask every time the app comes to the front without the notification permission (Android 13+): without it the
    // foreground-service notification and its lock-screen controls stay hidden. The system dialog only shows twice;
    // after that requestPermissions() returns "denied" at once, so fall back to our own dialog that opens the settings.
    @Override
    protected void onResume() {
        super.onResume();
        if (Build.VERSION.SDK_INT < 33 || askingNotifications || notificationsDialog != null
                || checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) return;
        askingNotifications = true;
        requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 1);
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(code, permissions, results);
        askingNotifications = false;
        if (code != 1 || (results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED)) return;
        if (shouldShowRequestPermissionRationale(android.Manifest.permission.POST_NOTIFICATIONS)) return; // the system will ask again
        notificationsDialog = new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
                .setTitle(R.string.notif_off_title)
                .setMessage(R.string.notif_off_msg)
                .setPositiveButton(R.string.open_settings, (d, w) -> startActivity(new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName())))
                .setNegativeButton(R.string.not_now, null)
                .setOnDismissListener(d -> notificationsDialog = null)
                .show();
    }

    @Override
    protected void onStart() {
        super.onStart();
        powerSave.start();
    }

    @Override
    protected void onStop() {
        powerSave.stop();
        super.onStop();
    }

    @Override
    public void onBackPressed() {
        if (fullScreen) session.exitFullScreen();
        else if (overlay && port != null) {
            try { port.postMessage(new org.json.JSONObject().put("back", true)); } catch (org.json.JSONException ignored) {}
        } else if (canGoBack) session.goBack(); else super.onBackPressed();
    }
}
