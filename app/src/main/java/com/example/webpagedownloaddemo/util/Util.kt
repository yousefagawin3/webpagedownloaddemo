package com.example.webpagedownloaddemo.util

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

fun Activity.isWriteFilePermissionGranted(): Boolean {
    return ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    ) == PackageManager.PERMISSION_GRANTED
}


fun String.addHttps() : String {

    return if(this.contains("https://")) {
        this
    }
    else {
        "https://$this"
    }
}


