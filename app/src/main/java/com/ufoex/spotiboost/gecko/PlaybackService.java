package com.ufoex.spotiboost.gecko;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.Bitmap;
import android.graphics.drawable.Icon;
import android.media.MediaMetadata;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;

/**
 * Keeps the music going with the screen off / app in the background.
 * Android mutes (AppOps opControlAudio) and then pauses the audio of an app that has no foreground service, so this
 * service holds one for as long as Gecko reports an active media session, shows the lock-screen / notification
 * controls, and holds the CPU and Wi-Fi awake while playing. State comes from MainActivity's Gecko MediaSession
 * delegate (Spotify's own navigator.mediaSession); commands go back through the same Gecko MediaSession.
 */
public class PlaybackService extends Service {
    private static final String CH = "playback";
    private static final int ID = 1;
    private static final String A_PREV = "prev", A_TOGGLE = "toggle", A_NEXT = "next";
    private static final long IDLE_STOP_MS = 10 * 60 * 1000; // paused this long -> drop the notification and stop

    // Written on the main thread by MainActivity's delegate, read here (also main thread).
    static org.mozilla.geckoview.MediaSession gecko;
    static String title = "", artist = "", album = "";
    static Bitmap art;
    static boolean playing;
    static long features;
    static double pos, dur, rate = 1;

    private static PlaybackService instance;
    private MediaSession session;
    private PowerManager.WakeLock cpu;
    private WifiManager.WifiLock wifi;
    private boolean foreground;
    private final Handler idle = new Handler(Looper.getMainLooper());
    private final Runnable stopIfIdle = () -> { if (!playing) { stopForeground(STOP_FOREGROUND_REMOVE); stopSelf(); } };

    /** Called whenever the Gecko media state changes. */
    static void poke(Context c) {
        if (instance != null) instance.render();
        else if (gecko != null) c.startForegroundService(new Intent(c, PlaybackService.class));
    }

    @Override
    public void onCreate() {
        instance = this;
        getSystemService(NotificationManager.class).createNotificationChannel(
                new NotificationChannel(CH, getString(R.string.playback_channel), NotificationManager.IMPORTANCE_LOW));
        session = new MediaSession(this, "Spotiboost");
        session.setCallback(new MediaSession.Callback() {
            @Override public void onPlay() { if (gecko != null) gecko.play(); }
            @Override public void onPause() { if (gecko != null) gecko.pause(); }
            @Override public void onSkipToNext() { if (gecko != null) gecko.nextTrack(); }
            @Override public void onSkipToPrevious() { if (gecko != null) gecko.previousTrack(); }
            @Override public void onSeekTo(long ms) { if (gecko != null) gecko.seekTo(ms / 1000.0, false); }
        });
        cpu = getSystemService(PowerManager.class).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "spotiboost:play");
        // ponytail: FULL_HIGH_PERF is deprecated on API 34+ but still the one that keeps Wi-Fi up with the screen off
        wifi = getSystemService(WifiManager.class).createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "spotiboost:play");
    }

    @Override
    public int onStartCommand(Intent i, int flags, int startId) {
        String a = i == null ? null : i.getAction();
        if (gecko != null && a != null) {
            if (A_TOGGLE.equals(a)) { if (playing) gecko.pause(); else gecko.play(); }
            else if (A_NEXT.equals(a)) gecko.nextTrack();
            else if (A_PREV.equals(a)) gecko.previousTrack();
        }
        render();
        return START_NOT_STICKY;
    }

    private PendingIntent act(String a, int code) {
        return PendingIntent.getService(this, code, new Intent(this, PlaybackService.class).setAction(a),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private void render() {
        if (gecko == null) { // page has no active media session any more
            stopForeground(STOP_FOREGROUND_REMOVE);
            stopSelf();
            return;
        }
        long actions = PlaybackState.ACTION_PLAY | PlaybackState.ACTION_PAUSE | PlaybackState.ACTION_PLAY_PAUSE;
        if ((features & org.mozilla.geckoview.MediaSession.Feature.NEXT_TRACK) != 0) actions |= PlaybackState.ACTION_SKIP_TO_NEXT;
        if ((features & org.mozilla.geckoview.MediaSession.Feature.PREVIOUS_TRACK) != 0) actions |= PlaybackState.ACTION_SKIP_TO_PREVIOUS;
        if ((features & org.mozilla.geckoview.MediaSession.Feature.SEEK_TO) != 0) actions |= PlaybackState.ACTION_SEEK_TO;
        long durMs = Double.isFinite(dur) ? (long) (dur * 1000) : -1;
        session.setMetadata(new MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_TITLE, title).putString(MediaMetadata.METADATA_KEY_ARTIST, artist)
                .putString(MediaMetadata.METADATA_KEY_ALBUM, album).putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, art)
                .putLong(MediaMetadata.METADATA_KEY_DURATION, durMs).build());
        session.setPlaybackState(new PlaybackState.Builder().setActions(actions)
                .setState(playing ? PlaybackState.STATE_PLAYING : PlaybackState.STATE_PAUSED,
                        Double.isFinite(pos) ? (long) (pos * 1000) : 0, playing ? (float) rate : 0f).build());
        session.setActive(true);

        Notification n = new Notification.Builder(this, CH)
                .setSmallIcon(R.drawable.ic_notif).setContentTitle(title).setContentText(artist).setLargeIcon(art)
                .setContentIntent(PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_IMMUTABLE))
                .setOngoing(playing).setOnlyAlertOnce(true).setVisibility(Notification.VISIBILITY_PUBLIC)
                .addAction(new Notification.Action.Builder(Icon.createWithResource(this, android.R.drawable.ic_media_previous), getString(R.string.media_prev), act(A_PREV, 1)).build())
                .addAction(new Notification.Action.Builder(Icon.createWithResource(this,
                        playing ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play), getString(playing ? R.string.media_pause : R.string.media_play), act(A_TOGGLE, 2)).build())
                .addAction(new Notification.Action.Builder(Icon.createWithResource(this, android.R.drawable.ic_media_next), getString(R.string.media_next), act(A_NEXT, 3)).build())
                .setStyle(new Notification.MediaStyle().setMediaSession(session.getSessionToken()).setShowActionsInCompactView(0, 1, 2))
                .build();
        if (!foreground) { startForeground(ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK); foreground = true; }
        else getSystemService(NotificationManager.class).notify(ID, n);

        if (playing) { if (!cpu.isHeld()) cpu.acquire(); if (!wifi.isHeld()) wifi.acquire(); }
        else { if (cpu.isHeld()) cpu.release(); if (wifi.isHeld()) wifi.release(); }
        idle.removeCallbacks(stopIfIdle);
        if (!playing) idle.postDelayed(stopIfIdle, IDLE_STOP_MS);
    }

    @Override
    public void onTaskRemoved(Intent root) { // swiped away from recents: stop the music like any player
        if (gecko != null) gecko.pause();
        stopSelf();
    }

    @Override
    public void onDestroy() {
        idle.removeCallbacks(stopIfIdle);
        if (cpu.isHeld()) cpu.release();
        if (wifi.isHeld()) wifi.release();
        session.release();
        instance = null;
    }

    @Override public IBinder onBind(Intent i) { return null; }
}
