package com.example.webpagedownloaddemo.ui

import android.R
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.webpagedownloaddemo.util.ConnectionLiveData
import com.example.webpagedownloaddemo.util.showSnackbar
import com.google.android.material.snackbar.Snackbar


open class BaseActivity : AppCompatActivity() {
    lateinit var connectionLiveData: ConnectionLiveData

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        connectionLiveData = ConnectionLiveData(applicationContext)

        val rootView = findViewById<View>(R.id.content)
        connectionLiveData.observe(this) {
            Log.e("BaseActivity", "connectionLiveData -- $it")

            if(it == false) {
                rootView.showSnackbar(
                    getString(com.example.webpagedownloaddemo.R.string.no_connection),
                    Snackbar.LENGTH_INDEFINITE,
                    "OK"
                ) {

                }
            }
        }
    }


}