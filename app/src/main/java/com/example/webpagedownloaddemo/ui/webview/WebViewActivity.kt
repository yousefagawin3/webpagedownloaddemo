package com.example.webpagedownloaddemo.ui.webview

import android.Manifest
import android.app.Activity
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.util.Log
import android.view.View
import android.webkit.WebSettings
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.webpagedownloaddemo.R
import com.example.webpagedownloaddemo.databinding.ActivityWebViewBinding
import com.example.webpagedownloaddemo.ui.BaseActivity
import com.example.webpagedownloaddemo.util.FilePath
import com.example.webpagedownloaddemo.util.showSnackbar
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File


class WebViewActivity : BaseActivity() {

    private lateinit var binding: ActivityWebViewBinding
    private val viewModel: WebViewViewModel by viewModels()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
//        Log.e(TAG, "requestPermissionLauncher -- isGranted -- $isGranted")
        if (isGranted) {
            //permission granted
            showDirectoryChooser()
        } else {
            //permission denied
            binding.root.showSnackbar(
                R.string.write_permission_denied,
                Snackbar.LENGTH_INDEFINITE,
                R.string.ok
            )
        }


    }

    private val openDocumentTree = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (it.resultCode == Activity.RESULT_OK) {

            val uri  = it.data?.data
            val docUri = DocumentsContract.buildDocumentUriUsingTree(uri,
                DocumentsContract.getTreeDocumentId(uri))
            val path: String = FilePath.getPath(this, docUri).toString()

            viewModel.saveFileLocation(path)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWebViewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val webUrl = intent.getStringExtra(webUrlKey).toString()

        viewModel.saveUrl(webUrl)
        initUI()
        initWebView(webUrl)

        lifecycleScope.launchWhenStarted {
            viewModel.downloadState.collectLatest {
                when(it) {
                    WebViewViewModel.DownloadState.DOWNLOADING -> {
                        binding.awvDownloadBtn.visibility = View.GONE
                        binding.awvDownloadPb.visibility = View.VISIBLE
                    }

                    WebViewViewModel.DownloadState.NOT_DOWNLOADING -> {
                        binding.awvDownloadBtn.visibility = View.VISIBLE
                        binding.awvDownloadPb.visibility = View.GONE
                    }
                }
            }
        }

        lifecycleScope.launchWhenStarted {
            viewModel.errorFlow.collectLatest {
                binding.root.showSnackbar(
                    "Error: $it",
                    Snackbar.LENGTH_LONG
                )
            }
        }

        lifecycleScope.launchWhenStarted {
            viewModel.toDownload.collectLatest {
                Log.e(TAG, "toDownload -- $it")

                //download stuff here
                for(i in it.imageList) {
                    startDownloading(it.fileLoc, i)

                }
            }
        }

        lifecycleScope.launch {
            viewModel.imageDownloadProgress.collect {
//                Log.e(TAG, "imageDownloadProgress -- ${it.size}")
                if(it.isNotEmpty()) {
                    "Downloading ${viewModel.imagesForDownload.size - it.size} / ${viewModel.imagesForDownload.size} ..".also { binding.awvDownloadProgressTv.text = it }
                } else {
                    //this extra if-statement will prevent the startup snackbar popup
                    if(!viewModel.imagesForDownload.isEmpty()){
                        binding.awvDownloadProgressTv.text = ""
                        binding.root.showSnackbar("Downloads completed!", Snackbar.LENGTH_SHORT)
                    }
                }
            }
        }
    }

    private fun checkForWritePermission() {

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            == PackageManager.PERMISSION_GRANTED
        ) {
            showDirectoryChooser()
        } else {
            //permission has not been asked yet
            requestWritePermission()
        }
    }

    private fun requestWritePermission() {
        if (shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            // Provide an additional rationale to the user if the permission was not granted
            // and the user would benefit from additional context for the use of the permission.
            // Display a SnackBar with a button to request the missing permission.
            binding.root.showSnackbar(
                R.string.write_access_required,
                Snackbar.LENGTH_INDEFINITE,
                R.string.ok
            ) {
                requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        } else {
            // You can directly ask for the permission.
            binding.root.showSnackbar(
                R.string.write_permission_not_available,
                Snackbar.LENGTH_LONG,
                R.string.ok
            ) {
                requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
    }

    private fun showDirectoryChooser() {

        val i = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        i.addCategory(Intent.CATEGORY_DEFAULT)
        openDocumentTree.launch(Intent.createChooser(i, "Choose directory"))
    }

    private fun initUI() {
        binding.awvToolbar.setNavigationIcon(R.drawable.ic_back)
        binding.awvToolbar.setNavigationOnClickListener {
            finish()
        }

        binding.awvDownloadBtn.setOnClickListener {
            checkForWritePermission()
        }
    }

    private fun initWebView(url: String) {

        binding.awvWebview.apply {
            settings.domStorageEnabled = true
            settings.loadWithOverviewMode = true
            settings.useWideViewPort = true
            settings.javaScriptEnabled = true
            settings.builtInZoomControls = true
            settings.displayZoomControls = false
            settings.setSupportZoom(true)
            settings.defaultTextEncodingName = "utf-8"
            settings.cacheMode = WebSettings.LOAD_DEFAULT

            setLayerType(View.LAYER_TYPE_HARDWARE, null)
            webViewClient = MyBrowser()
            loadUrl(url)
        }
    }



    private fun startDownloading(fileLoc: String, url: String) {

        //START OF DOWNLOAD
        val downloadManager: DownloadManager =
            getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

        registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(ctxt: Context, intent: Intent) {
//                binding.root.showSnackbar("$url has been downloaded", Snackbar.LENGTH_SHORT)
                viewModel.updateDownloadProgressState(url)
            }
        }, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))

        val uri: Uri = Uri.parse(url)

        try {
            val request: DownloadManager.Request = DownloadManager.Request(uri)
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)

            val file = File(
                fileLoc,
                System.currentTimeMillis().toString() + ".jpeg"
            )
            request.setDestinationUri(Uri.fromFile(file))
            request.setMimeType("image/jpeg")
            val reference: Long = downloadManager.enqueue(request)
        } catch (e: Exception) {
            Log.e(TAG, "startDownloading -- Exception -- $e")
            Toast.makeText(this, "CaughtException: $e", Toast.LENGTH_LONG).show()
            viewModel.resetDownloadProgressState()
        }
    }

    private class MyBrowser : WebViewClient() {

    }

    companion object {
        private const val TAG = "WebViewActivity"
        private const val webUrlKey = "webUrlKey"


        fun getIntent(
            context: Context,
            webUrl: String,
            shareUrl: String? = null,
        ) = Intent(context, WebViewActivity::class.java).apply {
            putExtra(webUrlKey, webUrl)
        }
    }
}