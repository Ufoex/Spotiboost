package com.ufoex.spotiboost.gecko;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Looks at the newest GitHub release once per app start and, if it is newer than this build, offers to download and
 * install it (same flow as Faceboost): the APK goes to the cache and is handed to the system installer. The update only
 * installs if it is signed with the same key as the installed app, which is what the release workflow guarantees.
 */
final class UpdateChecker {
    private static final String LATEST = "https://api.github.com/repos/Ufoex/Spotiboost/releases/latest";
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
                if (!isNewer(version, current)) return;
                String apk = apkUrl(j.getJSONArray("assets"));
                if (apk == null) return;
                String notes = notes(j.optString("body"));
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

    /** Release body up to the first "---" line: the workflow puts install/signing boilerplate after it. */
    static String notes(String body) {
        int i = body.indexOf("\n---");
        return (i < 0 ? body : body.substring(0, i)).replace("\r", "").trim();
    }

    /** Numeric, component by component: 0.10.0 > 0.9.0. Anything after "-" (like "-dev") is ignored. */
    static boolean isNewer(String remote, String local) {
        String[] r = remote.split("-")[0].split("\\."), l = local.split("-")[0].split("\\.");
        for (int i = 0; i < Math.max(r.length, l.length); i++) {
            int x = i < r.length ? num(r[i]) : 0, y = i < l.length ? num(l[i]) : 0;
            if (x != y) return x > y;
        }
        return false;
    }

    private static int num(String s) {
        try { return Integer.parseInt(s); } catch (NumberFormatException e) { return 0; }
    }

    private static void dialog(Activity a, String version, String current, String apk, String notes) {
        String msg = "Spotiboost " + version + " está disponible (tienes " + current + ")."
                + (notes.isEmpty() ? "" : "\n\n" + notes);
        AlertDialog d = new AlertDialog.Builder(a, android.R.style.Theme_Material_Dialog_Alert)
                .setTitle("Actualización disponible")
                .setMessage(msg)
                .setPositiveButton("Descargar e instalar", null)   // set below: must not auto-dismiss while downloading
                .setNegativeButton("Más tarde", null)
                .show();
        d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            // First time Android needs the user to allow this app to install APKs.
            if (!a.getPackageManager().canRequestPackageInstalls()) {
                a.startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + a.getPackageName())));
                Toast.makeText(a, "Permite instalar apps desde Spotiboost y vuelve a abrirla", Toast.LENGTH_LONG).show();
                d.dismiss();
                return;
            }
            d.setCancelable(false);
            d.setMessage("Descargando…");
            d.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
            d.getButton(AlertDialog.BUTTON_NEGATIVE).setEnabled(false);
            new Thread(() -> {
                boolean ok = download(a, apk);
                a.runOnUiThread(() -> {
                    d.dismiss();
                    if (!ok) {
                        Toast.makeText(a, "No se pudo descargar; se abre en el navegador", Toast.LENGTH_LONG).show();
                        a.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(apk)));
                    }
                });
            }).start();
        });
    }

    /** Downloads the APK and hands it to the system installer. */
    private static boolean download(Activity a, String url) {
        try {
            File f = new File(a.getCacheDir(), "updates/update.apk");
            f.getParentFile().mkdirs();
            HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();   // follows GitHub's redirect to the CDN
            c.setConnectTimeout(10000);
            c.setReadTimeout(30000);
            if (c.getResponseCode() != 200) return false;
            try (InputStream in = c.getInputStream(); FileOutputStream out = new FileOutputStream(f)) {
                byte[] buf = new byte[65536];
                for (int n; (n = in.read(buf)) > 0; ) out.write(buf, 0, n);
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
