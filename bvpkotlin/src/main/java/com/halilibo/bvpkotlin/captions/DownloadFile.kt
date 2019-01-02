package com.halilibo.bvpkotlin.captions

import android.os.AsyncTask
import android.util.Log
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.URL

internal class DownloadFile(
        private val cacheDir: File,
        private val listener: DownloadCallback?
) : AsyncTask<String, String, String>() {

    private lateinit var outputFile: File

    /**
     * Downloading file in background thread
     */
    override fun doInBackground(vararg f_url: String): String? {
        var count: Int
        try {
            val url = URL(f_url[0])
            val conection = url.openConnection()
            conection.connect()
            // input stream to read file - with 8k buffer
            val input = BufferedInputStream(url.openStream(), 8192)

            // Output stream to write file
            outputFile = File(cacheDir, f_url[1])
            val output = FileOutputStream(outputFile)

            val data = ByteArray(1024)

            var total: Long = 0

            count = input.read(data)
            while (count != -1) {
                total += count.toLong()

                // writing data to file
                output.write(data, 0, count)
                count = input.read(data)
            }

            // flushing output
            output.flush()

            // closing streams
            output.close()
            input.close()

        } catch (e: Exception) {
            Log.e("Error: ", e.message)
            listener!!.onFail(e)
        }

        return null
    }

    /**
     * Updating progress bar
     */
    override fun onProgressUpdate(vararg progress: String) {
        // setting progress percentage
    }

    /**
     * After completing background task
     * Dismiss the progress dialog
     */
    override fun onPostExecute(file_url: String) {

        listener?.onDownload(outputFile)
    }

}