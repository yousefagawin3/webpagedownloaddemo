package com.example.webpagedownloaddemo.ui.webview


import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import javax.inject.Inject

@HiltViewModel
class WebViewViewModel @Inject constructor(

): ViewModel () {

    var webUrl : String = ""

    private val _toDownload = MutableSharedFlow <ToDownload>()
    val toDownload = _toDownload.asSharedFlow()

    //used to show the loading progress bar in the ui
    private val _downloadState = MutableStateFlow(DownloadState.NOT_DOWNLOADING)
    val downloadState = _downloadState.asStateFlow()

    var imagesForDownload = emptyList<String>()

    private val _imageDownloadProgress = MutableStateFlow (emptyList<String>())
    val imageDownloadProgress = _imageDownloadProgress.asStateFlow()

//    private val _imagesForDownloadFlow = MutableSharedFlow<List<String>>()
//    val imagesForDownloadFlow = _imagesForDownloadFlow.asSharedFlow()

    //can be used by UI Layer to display errors from ViewModel
    private val _errorFlow = MutableSharedFlow<String>()
    val errorFlow = _errorFlow.asSharedFlow()


    //this func can be used by the UI layer to update the _downloadState
    fun updateDownloadState (downloadState: DownloadState) {
        _downloadState.value = downloadState
    }

    //this function is used to update the download progress
    fun updateDownloadProgressState (url: String) {
        val nonDownloaded = _imageDownloadProgress.value.toMutableList()
        nonDownloaded.remove(url)
        _imageDownloadProgress.value = nonDownloaded.toList()
    }

    fun resetDownloadProgressState() {
        _imageDownloadProgress.value = emptyList()
    }

    fun saveUrl (url : String) {
        webUrl = url
        extractAllImages(url)
    }

    private fun extractAllImages (url : String) {

        if (_downloadState.value == DownloadState.NOT_DOWNLOADING) {

            _downloadState.value = DownloadState.DOWNLOADING

            viewModelScope.launch {
                withContext(Dispatchers.IO) {
                    try{
                        val doc : Document = Jsoup.connect(url).userAgent("Mozilla/5.0").get()
                        val images : Elements = doc.select("img")

                        val imageList = mutableListOf<String>()
                        for (i in images) {
                            imageList.add(i.attr("abs:src"))
                            Log.e(TAG, "imageFound -- $i")
                        }
                        imagesForDownload = imageList

                        //PUT IMAGES IN THE LIST
                    } catch (e: Exception) {
                    Log.e(TAG, "caughtException -- $e")
                        viewModelScope.launch {
                            _errorFlow.emit(e.toString())
                        }
                    }
                    _downloadState.value = DownloadState.NOT_DOWNLOADING
                }
            }
        }
    }

    fun saveFileLocation (fileLoc : String) {
        if(imagesForDownload.isEmpty()){
            viewModelScope.launch {
                _errorFlow.emit("No images found to download.")
            }
            return
        }
        viewModelScope.launch {
            _toDownload.emit(ToDownload(
                fileLoc,
                imagesForDownload
            ))

        }

        _imageDownloadProgress.value = imagesForDownload
    }

    companion object {
        private const val TAG = "WebViewViewModel"
    }

    enum class DownloadState {
        DOWNLOADING, NOT_DOWNLOADING
    }

    data class ToDownload(
        val fileLoc: String,
        val imageList: List<String>
    )
}