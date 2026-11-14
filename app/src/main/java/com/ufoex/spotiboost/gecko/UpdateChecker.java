package com.ufoex.spotiboost.gecko;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.InsetDrawable;
import android.net.Uri;
import android.provider.Settings;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;
import android.text.style.URLSpan;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;
import java.util.function.IntConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Looks at the newest GitHub release once per app start and, if it is newer than this build, offers to download and
 * install it (same flow as Faceboost): the APK goes to the cache and is handed to the system installer, and the
 * dialog shows the release notes in the user's language. The update only installs if it is signed with the same key
 * as the installed app, which is what the release workflow guarantees.
 */
final class UpdateChecker {
    private static final String LATEST = "https://api.github.com/repos/Ufoex/Spotiboost/releases/latest";
    private static final int ACCENT = 0xFF1DB954;
    // `**bold**`, `code`, `[text](https://link)` and bare https:// URLs of one notes line
    private static final Pattern INLINE = Pattern.compile(
            "\\*\\*(.+?)\\*\\*|`(.+?)`|\\[(.+?)]\\((https://[^)\\s]+)\\)|(https://[^\\s)]*[^\\s).,;:])");
    private static boolean checked;   // once per process, not per Activity recreation

    private UpdateChecker() {}

    static void check(Activity a) {
        // A debug build has another application id: installing the release APK would not update it, it would add a second app.
        if ((a.getApplicationInfo().flags & android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0 || checked) return;
        checked = true;
        new Thread(() -> {
            try {
                HttpURLConnection c = (HttpURLConnection) new URL(LATEST).openConnection();
                c.setConnectTimeout(8000);
                c.setReadTimeout(8000);
                c.setRequestProperty("Accept", "application/vnd.github+json");
                if (c.getResponseCode() != 200) return;
                JSONObject j = new JSONObject(read(c.getInputStream()));
                String version = j.getString("tag_name").replaceFirst("^v", "");
                String current = a.getPackageManager().getPackageInfo(a.getPackageName(), 0).versionName;
                if (!UpdateLogic.isNewer(version, current)) return;
                String apk = apkUrl(j.getJSONArray("assets"));
                if (apk == null) return;
                String notes = UpdateLogic.notes(j.optString("body"), Locale.getDefault().getLanguage());
                a.runOnUiThread(() -> { if (!a.isFinishing() && !a.isDestroyed()) dialog(a, version, current, apk, notes); });
            } catch (Exception ignored) { /* offline, rate limited, malformed: no update prompt */ }
        }).start();
    }

    /** First .apk asset (the release also carries the .sha256 file). */
    private static String apkUrl(JSONArray assets) throws Exception {
        for (int i = 0; i < assets.length(); i++) {
            JSONObject o = assets.getJSONObject(i);
            if (o.getString("name").endsWith(".apk")) return o.getString("browser_download_url");
        }
        return null;
    }

    /** The few markdown shapes the notes use: `## heading`, `- bullet`, plain paragraphs; inline bold, code and links. */
    static CharSequence render(String notes) {
        SpannableStringBuilder out = new SpannableStringBuilder();
        for (String line : notes.split("\n")) {
            if (line.trim().isEmpty()) continue;
            if (out.length() > 0) out.append('\n');
            int start = out.length();
            if (line.startsWith("#")) {
                inline(out, line.replaceFirst("^#+\\s*", ""));
                out.setSpan(new StyleSpan(Typeface.BOLD), start, out.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                out.setSpan(new ForegroundColorSpan(ACCENT), start, out.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                out.setSpan(new RelativeSizeSpan(1.1f), start, out.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else if (line.matches("^[-*]\\s+.*")) {
                out.append("• ");
                inline(out, line.replaceFirst("^[-*]\\s+", ""));
            } else {
                inline(out, line.trim());
            }
        }
        return out;
    }

    private static void inline(SpannableStringBuilder out, String text) {
        Matcher m = INLINE.matcher(text);
        int last = 0;
        while (m.find()) {
            out.append(text, last, m.start());
            int s = out.length();
            if (m.group(1) != null) {
                out.append(m.group(1));
                out.setSpan(new StyleSpan(Typeface.BOLD), s, out.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else if (m.group(2) != null) {
                out.append(m.group(2));
                out.setSpan(new TypefaceSpan("monospace"), s, out.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else if (m.group(3) != null) {
                out.append(m.group(3));
                out.setSpan(new URLSpan(m.group(4)), s, out.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else {
                out.append(m.group(5));
                out.setSpan(new URLSpan(m.group(5)), s, out.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            last = m.end();
        }
        out.append(text, last, text.length());
    }

    private static void dialog(Activity a, String version, String current, String apk, String notes) {
        View v = a.getLayoutInflater().inflate(R.layout.dialog_update, null);
        TextView title = v.findViewById(R.id.up_title), desc = v.findViewById(R.id.up_desc), body = v.findViewById(R.id.up_notes);
        View scroll = v.findViewById(R.id.up_scroll);
        ProgressBar bar = v.findViewById(R.id.up_bar);
        Button later = v.findViewById(R.id.up_later), install = v.findViewById(R.id.up_install);
        title.setText(R.string.update_available_title);
        desc.setText(a.getString(R.string.update_available_desc, version, current));
        body.setText(render(notes));
        body.setMovementMethod(LinkMovementMethod.getInstance());   // links in the notes
        scroll.setVisibility(notes.isEmpty() ? View.GONE : View.VISIBLE);
        // long notes scroll instead of pushing the buttons off screen
        int max = a.getResources().getDisplayMetrics().heightPixels * 2 / 5;
        scroll.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            if (scroll.getHeight() > max) { scroll.getLayoutParams().height = max; scroll.requestLayout(); }
        });
        AlertDialog d = new AlertDialog.Builder(a).setView(v).create();
        // the card draws its own rounded background; the transparent inset keeps it off the screen edges
        int margin = (int) (16 * a.getResources().getDisplayMetrics().density);
        d.getWindow().setBackgroundDrawable(new InsetDrawable(new ColorDrawable(0), margin));
        later.setOnClickListener(x -> d.dismiss());
        install.setOnClickListener(x -> {
            // First time Android needs the user to allow this app to install APKs.
            if (!a.getPackageManager().canRequestPackageInstalls()) {
                a.startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + a.getPackageName())));
                Toast.makeText(a, R.string.update_allow_install, Toast.LENGTH_LONG).show();
                d.dismiss();
                return;
            }
            d.setCancelable(false);
            scroll.setVisibility(View.GONE);
            desc.setText(R.string.update_downloading);
            install.setEnabled(false);
            install.setAlpha(0.4f);
            later.setEnabled(false);
            bar.setIndeterminate(true);   // until the size is known
            bar.setVisibility(View.VISIBLE);
            new Thread(() -> {
                boolean ok = download(a, apk, pct -> a.runOnUiThread(() -> {
                    bar.setIndeterminate(false);
                    bar.setProgress(pct);
                    desc.setText(a.getString(R.string.update_downloading) + " " + pct + "%");
                }));
                a.runOnUiThread(() -> {
                    d.dismiss();
                    if (!ok) {
                        Toast.makeText(a, R.string.update_failed, Toast.LENGTH_LONG).show();
                        a.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(apk)));
                    }
                });
            }).start();
        });
        d.show();
    }

    /** Downloads the APK (reporting whole percents) and hands it to the system installer. */
    private static boolean download(Activity a, String url, IntConsumer progress) {
        try {
            File f = new File(a.getCacheDir(), "updates/update.apk");
            f.getParentFile().mkdirs();
            HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();   // follows GitHub's redirect to the CDN
            c.setConnectTimeout(10000);
            c.setReadTimeout(30000);
            if (c.getResponseCode() != 200) return false;
            long total = c.getContentLengthLong(), done = 0;
            int shown = -1;
            try (InputStream in = c.getInputStream(); FileOutputStream out = new FileOutputStream(f)) {
                byte[] buf = new byte[65536];
                for (int n; (n = in.read(buf)) > 0; ) {
                    out.write(buf, 0, n);
                    done += n;
                    int pct = total > 0 ? (int) (done * 100 / total) : -1;
                    if (pct != shown && pct >= 0) { shown = pct; progress.accept(pct); }
                }
            }
            Uri uri = FileProvider.getUriForFile(a, a.getPackageName() + ".fileprovider", f);
            a.startActivity(new Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/vnd.android.package-archive")
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static String read(InputStream in) throws Exception {
        try (InputStream i = in) { return new String(i.readAllBytes(), "UTF-8"); }
    }
}
