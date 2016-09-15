package com.halilibo.bettervideoplayer.subtitle;

import java.io.File;

/**
 * Created by halo on 7/14/16.
 */
public interface DownloadCallback {
    public void onDownload(File file);
    public void onFail(Exception e);
}
