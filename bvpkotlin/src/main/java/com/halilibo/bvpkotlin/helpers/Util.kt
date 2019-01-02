package com.halilibo.bvpkotlin.helpers

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.net.Uri
import androidx.annotation.AttrRes
import java.util.*
import java.util.concurrent.TimeUnit

object Util {
    fun getDurationString(durationMs: Long, negativePrefix: Boolean): String {
        val hours = TimeUnit.MILLISECONDS.toHours(durationMs)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMs)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(durationMs)

        return if (hours > 0) {
            java.lang.String.format(Locale.getDefault(), "%s%02d:%02d:%02d",
                    if (negativePrefix) "-" else "",
                    hours,
                    minutes - TimeUnit.HOURS.toMinutes(hours),
                    seconds - TimeUnit.MINUTES.toSeconds(minutes))
        } else java.lang.String.format(Locale.getDefault(), "%s%02d:%02d",
                if (negativePrefix) "-" else "",
                minutes,
                seconds - TimeUnit.MINUTES.toSeconds(minutes)
        )
    }

    fun isColorDark(color: Int): Boolean {
        val darkness = 1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255
        return darkness >= 0.5
    }

    fun adjustAlpha(color: Int, factor: Float): Int {
        val alpha = Math.round(Color.alpha(color) * factor)
        val red = Color.red(color)
        val green = Color.green(color)
        val blue = Color.blue(color)
        return Color.argb(alpha, red, green, blue)
    }

    fun resolveColor(context: Context, @AttrRes attr: Int): Int {
        return resolveColor(context, attr, 0)
    }

    fun resolveColor(context: Context, @AttrRes attr: Int, fallback: Int): Int {
        val a = context.theme.obtainStyledAttributes(intArrayOf(attr))
        try {
            return a.getColor(0, fallback)
        } finally {
            a.recycle()
        }
    }

    fun resolveDrawable(context: Context, @AttrRes attr: Int): Drawable? {
        return resolveDrawable(context, attr, null)
    }

    private fun resolveDrawable(context: Context, @AttrRes attr: Int, fallback: Drawable?): Drawable? {
        val a = context.theme.obtainStyledAttributes(intArrayOf(attr))
        try {
            var d = a.getDrawable(0)
            if (d == null && fallback != null)
                d = fallback
            return d
        } finally {
            a.recycle()
        }
    }

    fun getScreenWidth(context: Context): Int {
        return context.resources.displayMetrics.widthPixels
    }

    fun isRemotePath(path: Uri): Boolean {
        return "http" == path.scheme || "https" == path.scheme
    }

    fun secondsToDuration(seconds: Int): String {
        return java.lang.String.format(Locale.getDefault(), "%02d:%02d:%02d",
                seconds / 3600,
                seconds % 3600 / 60,
                seconds % 60
        )
    }
}