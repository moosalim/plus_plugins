// Copyright 2017 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package dev.fluttercommunity.plus.androidalarmmanager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.PowerManager;
import android.util.Log;

/**
 * Receives alarm broadcasts from the system AlarmManager and triggers:
 * 1. Sets an alarm flag for Flutter to detect
 * 2. Launches the app to foreground
 * 3. Processes the alarm callback
 */
public class AlarmBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = "AlarmBroadcastReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "Alarm broadcast received");

        // Step 1: Set the alarm flag so Flutter can detect which alarm fired
        AlarmFlagManager.set(context, intent);

        // Step 2: Acquire wake lock to ensure device stays awake
        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(
            PowerManager.FULL_WAKE_LOCK |
            PowerManager.ACQUIRE_CAUSES_WAKEUP |
            PowerManager.ON_AFTER_RELEASE,
            "AlarmBroadcastReceiver:AlarmWakeLock"
        );

        // Step 3: Get the app's launch intent
        Intent startIntent = context
            .getPackageManager()
            .getLaunchIntentForPackage(context.getPackageName());

        if (startIntent != null) {
            // Set flags to bring app to foreground
            startIntent.setFlags(
                Intent.FLAG_ACTIVITY_REORDER_TO_FRONT |
                Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
            );
        }

        // Step 4: Acquire wake lock for 3 minutes (enough time for alarm to be handled)
        wakeLock.acquire(3 * 60 * 1000L);

        // Step 5: Launch the app
        if (startIntent != null) {
            try {
                context.startActivity(startIntent);
                Log.i(TAG, "App launched successfully");
            } catch (Exception e) {
                Log.e(TAG, "Failed to launch app", e);
            }
        } else {
            Log.e(TAG, "Launch intent is null, cannot start app");
        }

        // Step 6: Process the alarm in background (original plugin behavior)
        AlarmService.enqueueAlarmProcessing(context, intent);

        // Step 7: Release wake lock
        wakeLock.release();

        // Step 8: Close system dialogs (for older Android versions)
        if (Build.VERSION.SDK_INT < 31) {
            context.sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
        }
    }
}