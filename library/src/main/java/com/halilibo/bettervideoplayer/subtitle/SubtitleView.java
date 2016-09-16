package com.halilibo.bettervideoplayer.subtitle;

import android.content.Context;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.Uri;
import android.support.annotation.DimenRes;
import android.support.annotation.Dimension;
import android.support.annotation.Nullable;
import android.support.annotation.RawRes;
import android.text.Html;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public class SubtitleView extends TextView implements Runnable{
    private static final String TAG = "SubtitleView";
    private static final boolean DEBUG = false;
    private static final int UPDATE_INTERVAL = 50;
    private MediaPlayer player;
    private TreeMap<Long, Line> track;
    private SubtitleMime mimeType;

    public SubtitleView(Context context) {
        super(context);
    }

    public SubtitleView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SubtitleView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void run() {
        if (player !=null && track!= null){
            int seconds = player.getCurrentPosition() / 1000;
            setText(Html.fromHtml(
                    // If debug mode is on, subtitle is shown with timing
                    (DEBUG?"[" + secondsToDuration(seconds) + "] ":"")
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

    // To display the seconds in the duration format 00:00:00
    public String secondsToDuration(int seconds) {
        return String.format("%02d:%02d:%02d", seconds / 3600,
                (seconds % 3600) / 60, (seconds % 60), Locale.US);
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

    public void setSubSource(@RawRes int ResID, SubtitleMime mime){
        this.mimeType = mime;
        track = getSubtitleFile(ResID);

    }

    public void setSubSource(@Nullable Uri path, SubtitleMime mime){
        this.mimeType = mime;
        if(path == null){
            track = new TreeMap<>();
        }
        try {
            URL url = new URL(path.toString());
            getSubtitleFile(url);
        } catch (MalformedURLException e) {
            track = getSubtitleFile(path.toString());
        }

    }

    /////////////Utility Methods:
    //Based on https://github.com/sannies/mp4parser/
    //Apache 2.0 Licence at: https://github.com/sannies/mp4parser/blob/master/LICENSE

    public static TreeMap<Long, Line> parse(InputStream in, SubtitleMime mime) throws IOException {
        if(mime == SubtitleMime.SUBRIP){
            return parseSrt(in);
        }
        else if(mime == SubtitleMime.WEBVTT){
            return parseVtt(in);
        }

        return parseSrt(in);
    }

    public static TreeMap<Long, Line> parseSrt(InputStream is) throws IOException {
        LineNumberReader r = new LineNumberReader(new InputStreamReader(is, "UTF-8"));
        TreeMap<Long, Line> track = new TreeMap<>();
        while ((r.readLine()) != null) /*Read cue number*/{
            String timeString = r.readLine();
            String lineString = "";
            String s;
            while (!((s = r.readLine()) == null || s.trim().equals(""))) {
                lineString += s + "\n";
            }
            // Remove unnecessary \n at the end of the string
            lineString = lineString.substring(0, lineString.length()-1);

            long startTime = parseSrt(timeString.split("-->")[0]);
            long endTime = parseSrt(timeString.split("-->")[1]);
            track.put(startTime, new Line(startTime, endTime, lineString));
        }
        return track;
    }

    private static long parseSrt(String in) {
        long hours = Long.parseLong(in.split(":")[0].trim());
        long minutes = Long.parseLong(in.split(":")[1].trim());
        long seconds = Long.parseLong(in.split(":")[2].split(",")[0].trim());
        long millies = Long.parseLong(in.split(":")[2].split(",")[1].trim());

        return hours * 60 * 60 * 1000 + minutes * 60 * 1000 + seconds * 1000 + millies;

    }

    public static TreeMap<Long, Line> parseVtt(InputStream is) throws IOException {
        LineNumberReader r = new LineNumberReader(new InputStreamReader(is, "UTF-8"));
        TreeMap<Long, Line> track = new TreeMap<>();
        r.readLine(); // Read first WEBVTT FILE cue
        r.readLine(); // Empty line after cue
        while ((r.readLine()) != null) /*Read cue number*/{
            String timeString = r.readLine();
            String lineString = "";
            String s;
            while (!((s = r.readLine()) == null || s.trim().equals(""))) {
                lineString += s + "\n";
            }
            // Remove unnecessary \n at the end of the string
            lineString = lineString.substring(0, lineString.length()-1);

            long startTime = parseVtt(timeString.split("-->")[0]);
            long endTime = parseVtt(timeString.split("-->")[1]);
            track.put(startTime, new Line(startTime, endTime, lineString));
        }
        return track;
    }

    private static long parseVtt(String in) {
        long hours = Long.parseLong(in.split(":")[0].trim());
        long minutes = Long.parseLong(in.split(":")[1].trim());
        long seconds = Long.parseLong(in.split(":")[2].split(".")[0].trim());
        long millies = Long.parseLong(in.split(":")[2].split(".")[1].trim());

        return hours * 60 * 60 * 1000 + minutes * 60 * 1000 + seconds * 1000 + millies;

    }

    private TreeMap<Long, Line> getSubtitleFile(String path) {
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(new File(path));
            return parse(inputStream, mimeType);
        } catch (Exception e) {
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
            return parse(inputStream, mimeType);
        } catch (Exception e) {
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

    private void getSubtitleFile(URL url) {
        DownloadFile downloader = new DownloadFile(getContext(), new DownloadCallback() {
            @Override
            public void onDownload(File file) {
                try {
                    track = getSubtitleFile(file.getPath());
                }catch (Exception ignored){} // Possibility of download returning 500
            }

            @Override
            public void onFail(Exception e) {
                Log.d(TAG, e.getMessage());
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
    }

    public enum SubtitleMime {
        SUBRIP, WEBVTT
    }
}