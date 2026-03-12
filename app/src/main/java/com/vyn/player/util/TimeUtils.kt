package com.vyn.player.util

import java.util.Locale
import java.util.concurrent.TimeUnit

object TimeUtils {

    fun formatDuration(durationMs: Long): String {
        val hours = TimeUnit.MILLISECONDS.toHours(durationMs)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMs) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(durationMs) % 60

        return if (hours > 0) {
            String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%d:%02d", minutes, seconds)
        }
    }

    fun formatDurationShort(durationMs: Long): String {
        val totalMinutes = TimeUnit.MILLISECONDS.toMinutes(durationMs)
        return if (totalMinutes >= 60) {
            val hours = totalMinutes / 60
            val mins = totalMinutes % 60
            "${hours}h ${mins}m"
        } else {
            "${totalMinutes}m"
        }
    }
}
