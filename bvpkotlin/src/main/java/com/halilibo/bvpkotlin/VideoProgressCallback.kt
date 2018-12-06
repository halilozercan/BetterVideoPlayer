package com.halilibo.bvpkotlin

interface VideoProgressCallback {

    fun onProgressUpdate(position: Int, duration: Int)
}