package com.halilibo.bettervideoplayer;

import android.net.Uri;

import java.util.Locale;

/**
 * @author Halil Ozercan
 */

public class HelperMethods {

    public static boolean isRemotePath(Uri path){
        return ("http".equals(path.getScheme()) || "https".equals(path.getScheme()));
    }

    public static String secondsToDuration(int seconds){
        return String.format(Locale.getDefault(), "%02d:%02d:%02d",
            seconds / 3600,
            (seconds % 3600) / 60,
            (seconds % 60)
        );
    }
}
