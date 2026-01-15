// Copyright 2019 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.
package dev.fluttercommunity.plus.androidalarmmanager

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.util.Log

class AlarmBroadcastReceiver : BroadcastReceiver() {
  companion object {
    private const val TAG = "AlarmBroadcastReceiver"
  }

  /**
   * Invoked by the OS when a timer goes off.
   *
   * Modified to:
   * 1. Set an alarm flag for Flutter to detect which alarm fired
   * 2. Launch the app to foreground
   * 3. Process the alarm callback via AlarmService
   *
   * The associated timer was registered in [AlarmService].
   *
   * In Android, timer notifications require a [BroadcastReceiver] as the artifact that is
   * notified when the timer goes off. This method handles the alarm by setting a flag,
   * launching the app, and offloading work to [AlarmService.enqueueAlarmProcessing].
   *
   * This method is the beginning of an execution path that will eventually execute a desired
   * Dart callback function, as registered by the Dart side of the android_alarm_manager plugin.
   */
  override fun onReceive(context: Context, intent: Intent) {
    Log.i(TAG, "Alarm broadcast received")

    // Step 1: Set the alarm flag so Flutter can detect which alarm fired
    AlarmFlagManager.set(context, intent)

    // Step 2: Acquire wake lock to ensure device stays awake
    val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    val wakeLock = powerManager.newWakeLock(
      PowerManager.FULL_WAKE_LOCK or
      PowerManager.ACQUIRE_CAUSES_WAKEUP or
      PowerManager.ON_AFTER_RELEASE,
      "AlarmBroadcastReceiver:AlarmWakeLock"
    )

    // Step 3: Get the app's launch intent
    val startIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)

    startIntent?.let {
      // Set flags to bring app to foreground
      it.flags = (Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                  Intent.FLAG_ACTIVITY_NEW_TASK or
                  Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
    }

    // Step 4: Acquire wake lock for 3 minutes (enough time for alarm to be handled)
    wakeLock.acquire(3 * 60 * 1000L)

    // Step 5: Launch the app
    startIntent?.let {
      try {
        context.startActivity(it)
        Log.i(TAG, "App launched successfully")
      } catch (e: Exception) {
        Log.e(TAG, "Failed to launch app", e)
      }
    } ?: run {
      Log.e(TAG, "Launch intent is null, cannot start app")
    }

    // Step 6: Process the alarm in background (original plugin behavior)
    AlarmService.enqueueAlarmProcessing(context, intent)

    // Step 7: Release wake lock
    wakeLock.release()

    // Step 8: Close system dialogs (for older Android versions)
    if (Build.VERSION.SDK_INT < 31) {
      context.sendBroadcast(Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS))
    }
  }
}