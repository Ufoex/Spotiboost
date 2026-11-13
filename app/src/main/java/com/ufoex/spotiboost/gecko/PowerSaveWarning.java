package com.ufoex.spotiboost.gecko;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.PowerManager;
import android.provider.Settings;

/**
 * Battery saver throttles background work and can cut the music when the screen is off. An app cannot turn it off
 * itself (that needs WRITE_SECURE_SETTINGS, which only adb can grant), so while the app is visible and battery saver is
 * on, ask the user to turn it off and jump straight to the system screen for it. "Ahora no" is remembered until the
 * mode is switched off and on again, so it does not nag.
 */
final class PowerSaveWarning extends BroadcastReceiver {
    private final Activity activity;
    private AlertDialog dialog;
    private boolean declined;

    PowerSaveWarning(Activity activity) { this.activity = activity; }

    /** Call from onStart: start reacting to changes and check right now. */
    void start() {
        activity.registerReceiver(this, new IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED), Context.RECEIVER_NOT_EXPORTED);
        check();
    }

    /** Call from onStop. */
    void stop() {
        activity.unregisterReceiver(this);
        if (dialog != null) dialog.dismiss();
    }

    @Override public void onReceive(Context c, Intent i) { check(); }

    private void check() {
        if (!activity.getSystemService(PowerManager.class).isPowerSaveMode()) {
            declined = false; // forget "ahora no" once it is off, so a later activation warns again
            if (dialog != null) dialog.dismiss();
            return;
        }
        if (declined || dialog != null || activity.isFinishing() || activity.isDestroyed()) return;
        dialog = new AlertDialog.Builder(activity, android.R.style.Theme_Material_Dialog_Alert)
                .setTitle("Ahorro de energía activado")
                .setMessage("Puede cortar o pausar la música cuando se apaga la pantalla o la app queda en segundo plano. "
                        + "Desáctivalo para que no se interrumpa.")
                .setPositiveButton("Abrir ajustes", (d, w) -> activity.startActivity(new Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS)))
                .setNegativeButton("Ahora no", (d, w) -> declined = true)
                .setOnDismissListener(d -> dialog = null)
                .show();
    }
}
