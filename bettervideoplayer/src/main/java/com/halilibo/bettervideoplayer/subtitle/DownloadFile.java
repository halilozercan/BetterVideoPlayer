package com.halilibo.bettervideoplayer.subtitle;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

/**
 * Background Async Task to download file
 * */
class DownloadFile extends AsyncTask<String, String, String> {

    private final DownloadCallback listener;
    private final Context context;
    private File outputFile;

    public DownloadFile(Context context, DownloadCallback cb){
        this.context = context;
        this.listener = cb;
    }
    /**
     * Before starting background thread
     * Show Progress Bar Dialog
     * */
    @Override
    protected void onPreExecute() {
        super.onPreExecute();

    }
 
    /**
     * Downloading file in background thread
     * */
    @Override
    protected String doInBackground(String... f_url) {
        int count;
        try {
            URL url = new URL(f_url[0]);
            URLConnection conection = url.openConnection();
            conection.connect();
            // getting file length
            int lenghtOfFile = conection.getContentLength();
 
            // input stream to read file - with 8k buffer
            InputStream input = new BufferedInputStream(url.openStream(), 8192);
 
            // Output stream to write file
            outputFile = new File(context.getCacheDir(), f_url[1]);
            OutputStream output = new FileOutputStream(outputFile);
 
            byte data[] = new byte[1024];
 
            long total = 0;
 
            while ((count = input.read(data)) != -1) {
                total += count;
 
                // writing data to file
                output.write(data, 0, count);
            }
 
            // flushing output
            output.flush();
 
            // closing streams
            output.close();
            input.close();
 
        } catch (Exception e) {
            Log.e("Error: ", e.getMessage());
            listener.onFail(e);
        }
 
        return null;
    }
 
    /**
     * Updating progress bar
     * */
    protected void onProgressUpdate(String... progress) {
        // setting progress percentage
   }
 
    /**
     * After completing background task
     * Dismiss the progress dialog
     * **/
    @Override
    protected void onPostExecute(String file_url) {

        if(listener!=null){
            listener.onDownload(outputFile);
        }
    }
 
}