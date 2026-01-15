// Copyright 2024 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.
package dev.fluttercommunity.plus.androidalarmmanager

import android.content.Context
import android.content.Intent

/**
 * Manages alarm flags using SharedPreferences.
 * 
 * When an alarm fires, this manager stores the alarm ID in SharedPreferences
 * so that the Flutter side can detect which alarm was triggered.
 */
object AlarmFlagManager {
  private const val FLUTTER_SHARED_PREFERENCE_KEY = "FlutterSharedPreferences"
  private const val ALARM_FLAG_KEY = "flutter.alarmFlagKey"

  /**
   * Sets the alarm flag with the alarm ID from the intent.
   * 
   * @param context The application context
   * @param intent The intent containing the alarm ID
   */
  fun set(context: Context, intent: Intent) {
    val alarmId = intent.getIntExtra("id", -1)
    
    val prefs = context.getSharedPreferences(FLUTTER_SHARED_PREFERENCE_KEY, 0)
    prefs.edit().putLong(ALARM_FLAG_KEY, alarmId.toLong()).apply()
  }
}