package com.halilibo.bettervideoplayer;

import android.net.Uri;

import java.util.Locale;

/**
 * Created by halo on 05.02.2017.
 */

public class HelperMethods {

    public static boolean isRemotePath(Uri path){
        return (path.getScheme().equals("http") || path.getScheme().equals("https"));
    }

    public static String secondsToDuration(int seconds){
        return String.format("%02d:%02d:%02d", seconds / 3600,
                (seconds % 3600) / 60, (seconds % 60));
    }
}
