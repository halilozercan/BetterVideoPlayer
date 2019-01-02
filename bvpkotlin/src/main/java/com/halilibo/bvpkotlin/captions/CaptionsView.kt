package com.halilibo.bvpkotlin.captions

import android.content.Context
import android.graphics.Color
import android.media.MediaPlayer
import android.net.Uri
import android.text.Html
import android.util.AttributeSet
import android.util.Log
import androidx.annotation.RawRes
import androidx.appcompat.widget.AppCompatTextView
import com.halilibo.bvpkotlin.helpers.Util
import java.io.*
import java.net.MalformedURLException
import java.net.URL
import java.util.*

class CaptionsView @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = android.R.attr.textViewStyle
) : AppCompatTextView(context, attrs, defStyleAttr), Runnable {
    private var player: MediaPlayer? = null
    private var track: TreeMap<Long, Line>? = null
    private var mimeType: SubMime? = null

    private var captionsViewLoadListener: CaptionsViewLoadListener? = null

    interface CaptionsViewLoadListener {
        fun onCaptionLoadSuccess(path: String?, resId: Int)
        fun onCaptionLoadFailed(error: Throwable, path: String?, resId: Int)
    }

    override fun run() {
        player?.let { player ->
            text = Html.fromHtml(getTimedText(player.currentPosition.toLong()))
        }
        postDelayed(this, UPDATE_INTERVAL.toLong())
    }

    private fun getTimedText(currentPosition: Long): String {
        var result = ""
        track?.let { track ->
            for ((key, value) in track) {
                if (currentPosition < key) break
                if (currentPosition < value.to) result = value.text
            }
        }
        return result
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        postDelayed(this, 300)
        this.setShadowLayer(6f, 6f, 6f, Color.BLACK)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        removeCallbacks(this)
    }

    fun setPlayer(player: MediaPlayer) {
        this.player = player
    }

    fun setCaptionsViewLoadListener(listener: CaptionsViewLoadListener?) {
        this.captionsViewLoadListener = listener
    }

    fun setCaptionsSource(@RawRes ResID: Int, mime: SubMime) {
        this.mimeType = mime
        track = getSubtitleFile(ResID)
    }

    fun setCaptionsSource(path: Uri?, mime: SubMime) {
        this.mimeType = mime
        if (path == null) {
            track = TreeMap()
            return
        }
        if (Util.isRemotePath(path)) {
            try {
                val url = URL(path.toString())
                getSubtitleFile(url)
            } catch (e: MalformedURLException) {
                captionsViewLoadListener?.onCaptionLoadFailed(e, path.toString(), 0)
                e.printStackTrace()
            } catch (e: NullPointerException) {
                captionsViewLoadListener?.onCaptionLoadFailed(e, path.toString(), 0)
                e.printStackTrace()
            }

        } else {
            track = getSubtitleFile(path.toString())
        }

    }

    private enum class TrackParseState {
        NEW_TRACK,
        PARSED_CUE,
        PARSED_TIME
    }

    private fun getSubtitleFile(path: String): TreeMap<Long, Line>? {
        var inputStream: InputStream? = null
        try {
            inputStream = FileInputStream(File(path))
            val tracks = parse(inputStream, mimeType)
            captionsViewLoadListener?.onCaptionLoadSuccess(path, 0)
            return tracks
        } catch (e: Exception) {
            captionsViewLoadListener?.onCaptionLoadFailed(e, path, 0)
            e.printStackTrace()
        } finally {
            inputStream?.let {
                try {
                    it.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }

            }
        }
        return null
    }

    private fun getSubtitleFile(resId: Int): TreeMap<Long, Line>? {
        var inputStream: InputStream? = null
        try {
            inputStream = resources.openRawResource(resId)
            val result = parse(inputStream, mimeType)
            captionsViewLoadListener?.onCaptionLoadSuccess(null, resId)
            return result
        } catch (e: Exception) {
            captionsViewLoadListener?.onCaptionLoadFailed(e, null, resId)
            e.printStackTrace()
        } finally {
            inputStream?.let {
                try {
                    it.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }

            }
        }
        return null
    }

    private fun getSubtitleFile(url: URL) {
        val downloader = DownloadFile(context.cacheDir, object : DownloadCallback {
            override fun onDownload(file: File) {
                try {
                    track = getSubtitleFile(file.path)
                } catch (e: Exception) {
                    captionsViewLoadListener?.onCaptionLoadFailed(e, url.toString(), 0)
                    // Possibility of download returning 500
                }

            }

            override fun onFail(e: Exception) {
                Log.d(TAG, e.message)
                captionsViewLoadListener?.onCaptionLoadFailed(e, url.toString(), 0)
            }
        })
        Log.d(TAG, "url: " + url.toString())
        downloader.execute(url.toString(), "subtitle.srt")
    }

    class Line {
        internal var from: Long = 0
        internal var to: Long = 0
        internal var text: String = ""

        constructor(from: Long, to: Long, text: String) {
            this.from = from
            this.to = to
            this.text = text
        }

        constructor(from: Long, to: Long) {
            this.from = from
            this.to = to
        }

        fun setText(text: String) {
            this.text = text
        }
    }

    enum class SubMime {
        SUBRIP, WEBVTT
    }

    companion object {
        private const val TAG = "SubtitleView"
        private const val LINE_BREAK = "<br/>"
        private const val DEBUG = false
        private const val UPDATE_INTERVAL = 50

        /////////////Utility Methods:
        //Based on https://github.com/sannies/mp4parser/
        //Apache 2.0 Licence at: https://github.com/sannies/mp4parser/blob/master/LICENSE

        @Throws(IOException::class)
        fun parse(`in`: InputStream, mime: SubMime?): TreeMap<Long, Line> {
            if (mime == SubMime.SUBRIP) {
                return parseSrt(`in`)
            } else if (mime == SubMime.WEBVTT) {
                return parseVtt(`in`)
            }

            return parseSrt(`in`)
        }

        @Throws(IOException::class)
        fun parseSrt(`is`: InputStream): TreeMap<Long, Line> {
            val r = LineNumberReader(InputStreamReader(`is`, "UTF-8"))
            val track = TreeMap<Long, Line>()
            var lineEntry: String?
            val textStringBuilder = StringBuilder()
            var line: Line? = null
            var state = TrackParseState.NEW_TRACK
            var lineNumber = 0

            lineEntry = r.readLine()
            while (lineEntry != null) {
                lineNumber++
                if (state == TrackParseState.NEW_TRACK) {
                    // Try to parse the cue number.
                    if (lineEntry.isEmpty()) {
                        // empty string, move along.
                    } else if (isInteger(lineEntry)) {
                        // We've reached a new cue.
                        state = TrackParseState.PARSED_CUE
                        if (line != null && textStringBuilder.isNotEmpty()) {
                            // Add the previous track.
                            val lineText = textStringBuilder.toString()
                            line.setText(lineText.substring(0, lineText.length - LINE_BREAK.length))
                            addTrack(track, line)
                            line = null
                            textStringBuilder.setLength(0) // reset the string builder
                        }
                    } else {
                        if (textStringBuilder.isNotEmpty()) {
                            // Support invalid formats which have line spaces between text.
                            textStringBuilder.append(lineEntry).append(LINE_BREAK)
                        }
                        // Be lenient, just log the error and move along.
                        Log.w(TAG, "No cue number found at line: $lineNumber")
                    }
                }
                else if (state == TrackParseState.PARSED_CUE) {
                    // Try to parse the time codes.
                    val times = lineEntry.split("-->".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                    if (times.size == 2) {
                        val startTime = parseSrt(times[0])
                        val endTime = parseSrt(times[1])
                        line = Line(startTime, endTime)
                        state = TrackParseState.PARSED_TIME
                    }
                    // Handle invalid formats gracefully. Better to have some subtitle than none.
                    Log.w(TAG, "No time-code found at line: $lineNumber")
                }
                else if (state == TrackParseState.PARSED_TIME) {
                    // Try to parse the text.
                    if (!lineEntry.isEmpty()) {
                        textStringBuilder.append(lineEntry).append(LINE_BREAK)
                    } else {
                        state = TrackParseState.NEW_TRACK
                    }
                }
                lineEntry = r.readLine()
            }

            if (line != null && textStringBuilder.isNotEmpty()) {
                // Add the final track.
                val lineText = textStringBuilder.toString()
                line.setText(lineText.substring(0, lineText.length - LINE_BREAK.length))
                addTrack(track, line)
            }

            return track
        }

        private fun addTrack(track: TreeMap<Long, Line>, line: Line) {
            track[line.from] = line
        }

        private fun isInteger(s: String): Boolean {
            if (s.isEmpty()) return false
            for (i in 0 until s.length) {
                if (i == 0 && s[i] == '-') {
                    return if (s.length == 1)
                        false
                    else
                        continue
                }
                if (Character.digit(s[i], 10) < 0) return false
            }
            return true
        }

        private fun parseSrt(`in`: String): Long {
            val timeSections = `in`.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            val secondAndMillisecond = timeSections[2].split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            val hours = java.lang.Long.parseLong(timeSections[0].trim { it <= ' ' })
            val minutes = java.lang.Long.parseLong(timeSections[1].trim { it <= ' ' })
            val seconds = java.lang.Long.parseLong(secondAndMillisecond[0].trim { it <= ' ' })
            val millies = java.lang.Long.parseLong(secondAndMillisecond[1].trim { it <= ' ' })

            return hours * 60 * 60 * 1000 + minutes * 60 * 1000 + seconds * 1000 + millies

        }

        @Throws(IOException::class)
        fun parseVtt(`is`: InputStream): TreeMap<Long, Line> {
            val r = LineNumberReader(InputStreamReader(`is`, "UTF-8"))
            val track = TreeMap<Long, Line>()
            r.readLine() // Read first WEBVTT FILE cue
            r.readLine() // Empty line after cue
            var timeString: String?
            timeString = r.readLine()
            while (timeString != null)
            /*Read cue number*/ {
                var lineString = ""
                var s: String?
                s = r.readLine()
                while (!(s == null || s.trim().isEmpty())) {
                    lineString += s + LINE_BREAK
                    s = r.readLine()
                }
                // Remove final line-break at the end of the string
                lineString = lineString.substring(0, lineString.length - LINE_BREAK.length)

                val times = timeString.split(" --> ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                if (times.size == 2) {
                    val startTime = parseVtt(times[0])
                    val endTime = parseVtt(times[1])
                    track[startTime] = Line(startTime, endTime, lineString)
                }
                timeString = r.readLine()
            }
            return track
        }

        private fun parseVtt(`in`: String): Long {
            val timeUnits = `in`.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            val hoursAvailable = timeUnits.size == 3
            return if (hoursAvailable) {
                val secondAndMillisecond = timeUnits[2].split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                val hours = java.lang.Long.parseLong(timeUnits[0].trim { it <= ' ' })
                val minutes = java.lang.Long.parseLong(timeUnits[1].trim { it <= ' ' })
                val seconds = java.lang.Long.parseLong(secondAndMillisecond[0].trim { it <= ' ' })
                val millies = java.lang.Long.parseLong(secondAndMillisecond[1].trim { it <= ' ' })
                hours * 60 * 60 * 1000 + minutes * 60 * 1000 + seconds * 1000 + millies
            } else {
                val secondAndMillisecond = timeUnits[1].split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                val minutes = java.lang.Long.parseLong(timeUnits[0].trim { it <= ' ' })
                val seconds = java.lang.Long.parseLong(secondAndMillisecond[0].trim { it <= ' ' })
                val millies = java.lang.Long.parseLong(secondAndMillisecond[1].trim { it <= ' ' })
                minutes * 60 * 1000 + seconds * 1000 + millies
            }

        }
    }
}