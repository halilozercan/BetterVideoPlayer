package com.halilibo.bettervideoplayer.subtitle;

import android.content.Context;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.support.annotation.RawRes;
import android.support.v7.widget.AppCompatTextView;
import android.text.Html;
import android.util.AttributeSet;
import android.util.Log;

import com.halilibo.bettervideoplayer.HelperMethods;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.TreeMap;

public class CaptionsView extends AppCompatTextView implements Runnable{
    private static final String TAG = "SubtitleView";
    private static final String LINE_BREAK = "<br/>";
    private static final boolean DEBUG = false;
    private static final int UPDATE_INTERVAL = 50;
    private MediaPlayer player;
    private TreeMap<Long, Line> track;
    private CMime mimeType;

    private CaptionsViewLoadListener captionsViewLoadListener;

    public interface CaptionsViewLoadListener {
        void onCaptionLoadSuccess(@Nullable String path, int resId);
        void onCaptionLoadFailed(Throwable error, @Nullable String path, int resId);
    }

    public CaptionsView(Context context) {
        super(context);
    }

    public CaptionsView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CaptionsView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void run() {
        if (player !=null && track!= null){
            int seconds = player.getCurrentPosition() / 1000;
            setText(Html.fromHtml(
                    // If debug mode is on, subtitle is shown with timing
                    (DEBUG?"[" + HelperMethods.secondsToDuration(seconds) + "] ":"")
                    + getTimedText(player.getCurrentPosition())));
        }
        postDelayed(this, UPDATE_INTERVAL);
    }

    private String getTimedText(long currentPosition) {
        String result = "";
        for(Map.Entry<Long, Line> entry: track.entrySet()){
            if (currentPosition < entry.getKey()) break;
            if (currentPosition < entry.getValue().to) result = entry.getValue().text;
        }
        return result;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        postDelayed(this, 300);
        this.setShadowLayer(6,6,6, Color.BLACK);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        removeCallbacks(this);
    }
    public void setPlayer(MediaPlayer player) {
        this.player = player;
    }

    public void setCaptionsViewLoadListener(CaptionsViewLoadListener listener) {
        this.captionsViewLoadListener = listener;
    }

    public void setCaptionsSource(@RawRes int ResID, CMime mime){
        this.mimeType = mime;
        track = getSubtitleFile(ResID);

    }

    public void setCaptionsSource(@Nullable Uri path, CMime mime){
        this.mimeType = mime;
        if(path == null){
            track = new TreeMap<>();
            return;
        }
        if (HelperMethods.isRemotePath(path)) {
            try {
                URL url = new URL(path.toString());
                getSubtitleFile(url);
            } catch (MalformedURLException | NullPointerException e) {
                if(captionsViewLoadListener != null) {
                    captionsViewLoadListener.onCaptionLoadFailed(e, path.toString(), 0);
                }
                e.printStackTrace();
            }
        } else {
            track = getSubtitleFile(path.toString());
        }

    }

    /////////////Utility Methods:
    //Based on https://github.com/sannies/mp4parser/
    //Apache 2.0 Licence at: https://github.com/sannies/mp4parser/blob/master/LICENSE

    public static TreeMap<Long, Line> parse(InputStream in, CMime mime) throws IOException {
        if(mime == CMime.SUBRIP){
            return parseSrt(in);
        }
        else if(mime == CMime.WEBVTT){
            return parseVtt(in);
        }

        return parseSrt(in);
    }

    private enum TrackParseState {
        NEW_TRACK,
        PARSED_CUE,
        PARSED_TIME,
    }

    public static TreeMap<Long, Line> parseSrt(InputStream is) throws IOException {
        LineNumberReader r = new LineNumberReader(new InputStreamReader(is, "UTF-8"));
        TreeMap<Long, Line> track = new TreeMap<>();
        String lineEntry;
        StringBuilder textStringBuilder = new StringBuilder();
        Line line = null;
        TrackParseState state = TrackParseState.NEW_TRACK;
        int lineNumber = 0;
        while ((lineEntry = r.readLine()) != null){
            lineNumber++;
            if(state == TrackParseState.NEW_TRACK) {
                // Try to parse the cue number.
                if(lineEntry.isEmpty()) {
                    // empty string, move along.
                    continue;
                } else if(isInteger(lineEntry)) {
                    // We've reach a new cue.
                    state = TrackParseState.PARSED_CUE;
                    if(line != null && textStringBuilder.length() > 0) {
                        // Add the previous track.
                        String lineText = textStringBuilder.toString();
                        line.setText(lineText.substring(0, lineText.length() - LINE_BREAK.length()));
                        addTrack(track, line);
                        line = null;
                        textStringBuilder.setLength(0); // reset the string builder
                    }
                    continue;
                } else {
                    if (textStringBuilder.length() > 0) {
                        // Support invalid formats which have line spaces between text.
                        textStringBuilder.append(lineEntry).append(LINE_BREAK);
                        continue;
                    }
                    // Be lenient, just log the error and move along.
                    Log.w(TAG, "No cue number found at line: " + lineNumber);
                }
            }

            if(state == TrackParseState.PARSED_CUE) {
                // Try to parse the time codes.
                String[] times = lineEntry.split("-->");
                if(times.length == 2) {
                    long startTime = parseSrt(times[0]);
                    long endTime = parseSrt(times[1]);
                    line = new Line(startTime, endTime);
                    state = TrackParseState.PARSED_TIME;
                    continue;
                }
                // Handle invalid formats gracefully. Better to have some subtitle than none.
                Log.w(TAG, "No time-code found at line: " + lineNumber);
            }

            if(state == TrackParseState.PARSED_TIME) {
                // Try to parse the text.
                if(!lineEntry.isEmpty()) {
                    textStringBuilder.append(lineEntry).append(LINE_BREAK);
                } else {
                    state = TrackParseState.NEW_TRACK;
                }
            }
        }
        if(line != null && textStringBuilder.length() > 0) {
            // Add the final track.
            String lineText = textStringBuilder.toString();
            line.setText(lineText.substring(0, lineText.length() - LINE_BREAK.length()));
            addTrack(track, line);
        }

        return track;
    }

    private static void addTrack( TreeMap<Long, Line> track, Line line) {
        track.put(line.from, line);
    }

    private static boolean isInteger(String s) {
        if(s.isEmpty()) return false;
        for(int i = 0; i < s.length(); i++) {
            if(i == 0 && s.charAt(i) == '-') {
                if(s.length() == 1) return false;
                else continue;
            }
            if(Character.digit(s.charAt(i), 10) < 0) return false;
        }
        return true;
    }

    private static long parseSrt(String in) {
        String[] timeSections = in.split(":");
        String[] secondAndMillisecond = timeSections[2].split(",");
        long hours = Long.parseLong(timeSections[0].trim());
        long minutes = Long.parseLong(timeSections[1].trim());
        long seconds = Long.parseLong(secondAndMillisecond[0].trim());
        long millies = Long.parseLong(secondAndMillisecond[1].trim());

        return hours * 60 * 60 * 1000 + minutes * 60 * 1000 + seconds * 1000 + millies;

    }

    public static TreeMap<Long, Line> parseVtt(InputStream is) throws IOException {
        LineNumberReader r = new LineNumberReader(new InputStreamReader(is, "UTF-8"));
        TreeMap<Long, Line> track = new TreeMap<>();
        r.readLine(); // Read first WEBVTT FILE cue
        r.readLine(); // Empty line after cue
        String timeString;
        while ((timeString = r.readLine()) != null) /*Read cue number*/{
            String lineString = "";
            String s;
            while (!((s = r.readLine()) == null || s.trim().equals(""))) {
                lineString += s + LINE_BREAK;
            }
            // Remove final line-break at the end of the string
            lineString = lineString.substring(0, lineString.length() - LINE_BREAK.length());

            String[] times = timeString.split(" --> ");
            if(times.length == 2) {
                long startTime = parseVtt(times[0]);
                long endTime = parseVtt(times[1]);
                track.put(startTime, new Line(startTime, endTime, lineString));
            }
        }
        return track;
    }

    private static long parseVtt(String in) {
        String[] timeUnits = in.split(":");
        boolean hoursAvailable = timeUnits.length == 3;
        if(hoursAvailable) {
            String[] secondAndMillisecond = timeUnits[2].split("\\.");
            long hours = Long.parseLong(timeUnits[0].trim());
            long minutes = Long.parseLong(timeUnits[1].trim());
            long seconds = Long.parseLong(secondAndMillisecond[0].trim());
            long millies = Long.parseLong(secondAndMillisecond[1].trim());
            return hours * 60 * 60 * 1000 + minutes * 60 * 1000 + seconds * 1000 + millies;
        }
        else{
            String[] secondAndMillisecond = timeUnits[1].split("\\.");
            long minutes = Long.parseLong(timeUnits[0].trim());
            long seconds = Long.parseLong(secondAndMillisecond[0].trim());
            long millies = Long.parseLong(secondAndMillisecond[1].trim());
            return minutes * 60 * 1000 + seconds * 1000 + millies;
        }

    }

    private TreeMap<Long, Line> getSubtitleFile(String path) {
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(new File(path));
            TreeMap<Long, Line> tracks = parse(inputStream, mimeType);
            if(captionsViewLoadListener != null) {
                captionsViewLoadListener.onCaptionLoadSuccess(path, 0);
            }
            return tracks;
        } catch (Exception e) {
            if(captionsViewLoadListener != null) {
                captionsViewLoadListener.onCaptionLoadFailed(e, path, 0);
            }
            e.printStackTrace();
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    private TreeMap<Long, Line> getSubtitleFile(int resId) {
        InputStream inputStream = null;
        try {
            inputStream = getResources().openRawResource(resId);
            TreeMap<Long, Line> result = parse(inputStream, mimeType);
            if(captionsViewLoadListener != null) {
                captionsViewLoadListener.onCaptionLoadSuccess(null, resId);
            }
            return result;
        } catch (Exception e) {
            if(captionsViewLoadListener != null) {
                captionsViewLoadListener.onCaptionLoadFailed(e, null, resId);
            }
            e.printStackTrace();
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    private void getSubtitleFile(final URL url) {
        DownloadFile downloader = new DownloadFile(getContext(), new DownloadCallback() {
            @Override
            public void onDownload(File file) {
                try {
                    track = getSubtitleFile(file.getPath());
                } catch (Exception e){
                    if(captionsViewLoadListener != null) {
                        captionsViewLoadListener.onCaptionLoadFailed(e, url.toString(), 0);
                    }
                    // Possibility of download returning 500
                }
            }

            @Override
            public void onFail(Exception e) {
                Log.d(TAG, e.getMessage());
                if(captionsViewLoadListener != null) {
                    captionsViewLoadListener.onCaptionLoadFailed(e, url.toString(), 0);
                }
            }
        });
        Log.d(TAG, "url: " + url.toString());
        downloader.execute(url.toString(), "subtitle.srt");
    }

    public static class Line {
        long from;
        long to;
        String text;

        public Line(long from, long to, String text) {
            this.from = from;
            this.to = to;
            this.text = text;
        }

        public Line(long from, long to) {
            this.from = from;
            this.to = to;
        }

        public void setText(String text) {
            this.text = text;
        }
    }

    public enum CMime {
        SUBRIP, WEBVTT
    }
}