package com.jasonrandazza.pixelpantry.scan

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jasonrandazza.pixelpantry.confirm.FixtureDetections
import com.jasonrandazza.pixelpantry.ml.LeapVisionClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Product Scan flow: load Leap VL, capture/pick a photo, parse into Confirm-ready text. */
class ScanViewModel(application: Application) : AndroidViewModel(application) {
    private val client = LeapVisionClient(application)

    private val _ui = MutableStateFlow(ScanUiState())
    val ui: StateFlow<ScanUiState> = _ui.asStateFlow()

    fun loadModel() {
        if (_ui.value.modelStatus == VlModelStatus.Loading) return
        viewModelScope.launch(Dispatchers.Default) {
            _ui.update { it.copy(modelStatus = VlModelStatus.Loading, error = null) }
            try {
                client.loadModel(onProgress = { p -> _ui.update { it.copy(downloadProgress = p) } })
                _ui.update { it.copy(modelStatus = VlModelStatus.Ready, downloadProgress = 1f) }
            } catch (e: Exception) {
                Log.e(TAG, "loadModel failed", e)
                _ui.update { it.copy(modelStatus = VlModelStatus.Error, error = e.message ?: e.toString()) }
            }
        }
    }

    fun analyzeUri(uri: Uri) {
        val bitmap = loadBitmap(uri)
        if (bitmap == null) {
            _ui.update { it.copy(error = "Unable to decode image.") }
            return
        }
        analyzeBitmap(bitmap)
    }

    /** Owns [bitmap]; recycled by the underlying client once encoded. */
    fun analyzeBitmap(bitmap: Bitmap) {
        if (_ui.value.modelStatus != VlModelStatus.Ready) {
            if (!bitmap.isRecycled) bitmap.recycle()
            _ui.update { it.copy(error = "Load the model before analyzing.") }
            return
        }
        viewModelScope.launch(Dispatchers.Default) {
            _ui.update { it.copy(isAnalyzing = true, responseText = "", error = null) }
            try {
                client.analyze(bitmap).collect { snapshot -> _ui.update { it.copy(responseText = snapshot) } }
                _ui.update { it.copy(isAnalyzing = false) }
            } catch (e: Exception) {
                Log.e(TAG, "analyze failed", e)
                _ui.update { it.copy(isAnalyzing = false, error = e.message ?: e.toString()) }
            }
        }
    }

    fun useFixtureSample() {
        _ui.update { it.copy(responseText = FixtureDetections.SAMPLE, error = null) }
    }

    fun reportCaptureError(message: String) {
        _ui.update { it.copy(error = message) }
    }

    private fun loadBitmap(uri: Uri): Bitmap? {
        val resolver = getApplication<Application>().contentResolver
        return resolver.openInputStream(uri)?.use { input -> BitmapFactory.decodeStream(input) }
    }

    override fun onCleared() {
        super.onCleared()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                client.unload()
            } catch (e: Exception) {
                Log.e(TAG, "unload failed", e)
            }
        }
    }

    private companion object {
        const val TAG = "ScanViewModel"
    }
}
