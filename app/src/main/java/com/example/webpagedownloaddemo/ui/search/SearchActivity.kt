package com.example.webpagedownloaddemo.ui.search

import android.os.Bundle
import com.example.webpagedownloaddemo.R
import com.example.webpagedownloaddemo.ui.BaseActivity
import com.example.webpagedownloaddemo.databinding.ActivitySearchBinding
import com.example.webpagedownloaddemo.ui.webview.WebViewActivity
import com.example.webpagedownloaddemo.util.addHttps
import com.example.webpagedownloaddemo.util.showSnackbar
import com.google.android.material.snackbar.Snackbar

class SearchActivity : BaseActivity() {

    private lateinit var binding : ActivitySearchBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.searchBtn.setOnClickListener {
            if(binding.searchEt.text.toString().isBlank()) {
                binding.root.showSnackbar(getString(R.string.no_url_found), Snackbar.LENGTH_LONG)
                return@setOnClickListener
            }
            startActivity(WebViewActivity.getIntent(this, binding.searchEt.text.toString().addHttps()))
        }
    }
}