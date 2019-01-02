package com.halilibo.bvpkotlin.captions

import java.io.File

interface DownloadCallback {
    fun onDownload(file: File)
    fun onFail(e: Exception)
}