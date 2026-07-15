package com.jasonrandazza.pixelpantry.spike

import ai.liquid.leap.GenerationOptions
import ai.liquid.leap.ModelRunner
import ai.liquid.leap.downloader.LeapModelDownloader
import ai.liquid.leap.message.ChatMessage
import ai.liquid.leap.message.ChatMessageContent
import ai.liquid.leap.message.ImageUtils
import ai.liquid.leap.message.MessageResponse
import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Research spike: load Leap LFM2.5-VL-1.6B and run fridge-photo prompts.
 * Not product architecture — see docs/research/android/leap-vl-device-spike-plan.md
 */
class VlmSpikeViewModel(application: Application) : AndroidViewModel(application) {

    private val downloader = LeapModelDownloader(application)

    private var runner: ModelRunner? = null
    private var inferJob: Job? = null

    private val _ui = MutableStateFlow(VlmSpikeUiState())
    val uiState: StateFlow<VlmSpikeUiState> = _ui.asStateFlow()

    fun loadModel() {
        if (_ui.value.modelStatus == ModelStatus.Loading) return
        viewModelScope.launch(Dispatchers.Default) {
            val start = System.currentTimeMillis()
            _ui.update {
                it.copy(
                    modelStatus = ModelStatus.Loading,
                    statusLine = "Downloading/loading LFM2.5-VL-1.6B (Q4_K_M)…",
                    error = null,
                    lastLoadMs = null,
                )
            }
            try {
                val model = downloader.loadModel(
                    modelName = MODEL_NAME,
                    quantizationType = QUANTIZATION,
                )
                runner = model
                val elapsed = System.currentTimeMillis() - start
                _ui.update {
                    it.copy(
                        modelStatus = ModelStatus.Ready,
                        statusLine = "Model ready (${elapsed} ms).",
                        lastLoadMs = elapsed,
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "loadModel failed", e)
                runner = null
                _ui.update {
                    it.copy(
                        modelStatus = ModelStatus.Error,
                        statusLine = "Load failed.",
                        error = e.message ?: e.toString(),
                    )
                }
            }
        }
    }

    fun analyzeImage(uri: Uri) {
        val bitmap = loadBitmap(uri)
        if (bitmap == null) {
            _ui.update { it.copy(error = "Unable to decode image from gallery.") }
            return
        }
        analyzeBitmap(bitmap, sourceLabel = uri.toString())
    }

    /** Owns [bitmap] and recycles it after encoding for Leap. */
    fun analyzeBitmap(bitmap: Bitmap, sourceLabel: String = "camera") {
        val model = runner
        if (model == null) {
            if (!bitmap.isRecycled) bitmap.recycle()
            _ui.update { it.copy(error = "Load the model before analyzing.") }
            return
        }
        inferJob?.cancel()
        inferJob = viewModelScope.launch(Dispatchers.Default) {
            val start = System.currentTimeMillis()
            _ui.update {
                it.copy(
                    isInferring = true,
                    selectedImageUri = sourceLabel,
                    responseText = "",
                    statusLine = "Running VL inference…",
                    error = null,
                    lastInferMs = null,
                )
            }
            try {
                val imageContent = ImageUtils.fromBitmap(bitmap, compressionQuality = 85)
                if (!bitmap.isRecycled) bitmap.recycle()

                val conversation = model.createConversation()
                val message = ChatMessage(
                    role = ChatMessage.Role.USER,
                    content = listOf(
                        imageContent,
                        ChatMessageContent.Text(INGREDIENT_PROMPT),
                    ),
                )
                val options = GenerationOptions.build {
                    temperature = 0.1f
                    minP = 0.15f
                    repetitionPenalty = 1.05f
                }

                val builder = StringBuilder()
                conversation.generateResponse(message, options).collect { response ->
                    when (response) {
                        is MessageResponse.Chunk -> {
                            builder.append(response.text)
                            val snapshot = builder.toString()
                            _ui.update { it.copy(responseText = snapshot) }
                        }
                        is MessageResponse.Complete -> Unit
                        else -> Unit
                    }
                }
                val elapsed = System.currentTimeMillis() - start
                _ui.update {
                    it.copy(
                        isInferring = false,
                        statusLine = "Inference complete (${elapsed} ms).",
                        lastInferMs = elapsed,
                        responseText = builder.toString(),
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "analyzeBitmap failed", e)
                if (!bitmap.isRecycled) bitmap.recycle()
                _ui.update {
                    it.copy(
                        isInferring = false,
                        statusLine = "Inference failed.",
                        error = e.message ?: e.toString(),
                    )
                }
            }
        }
    }

    fun reportCaptureError(message: String) {
        _ui.update {
            it.copy(
                error = message,
                statusLine = "Capture failed.",
            )
        }
    }

    fun unloadModel() {
        inferJob?.cancel()
        viewModelScope.launch(Dispatchers.IO) {
            try {
                runner?.unload()
            } catch (e: Exception) {
                Log.e(TAG, "unload failed", e)
            } finally {
                runner = null
                _ui.update {
                    it.copy(
                        modelStatus = ModelStatus.Idle,
                        statusLine = "Model unloaded.",
                        isInferring = false,
                    )
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        inferJob?.cancel()
        val model = runner
        runner = null
        if (model != null) {
            // Do not use viewModelScope / runBlocking here — avoid ANRs after clear.
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    model.unload()
                } catch (e: Exception) {
                    Log.e(TAG, "onCleared unload failed", e)
                }
            }
        }
    }

    private fun loadBitmap(uri: Uri): Bitmap? {
        val resolver = getApplication<Application>().contentResolver
        return resolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input)
        }
    }

    companion object {
        private const val TAG = "VlmSpike"
        const val MODEL_NAME = "LFM2.5-VL-1.6B"
        const val QUANTIZATION = "Q4_K_M"

        val INGREDIENT_PROMPT = """
            This is a fridge or pantry shelf photo that may contain many items at once.
            List every distinct food ingredient you can see (produce, packaged goods, leftovers, condiments).
            Return one item per line as: name | rough quantity or "unknown" | confidence high/medium/low.
            Do not invent items you cannot see. Skip non-food objects. Prefer more complete lists over guessing quantities.
        """.trimIndent()
    }
}

data class VlmSpikeUiState(
    val modelStatus: ModelStatus = ModelStatus.Idle,
    val isInferring: Boolean = false,
    val statusLine: String = "Idle — load the VL model to begin.",
    val responseText: String = "",
    val selectedImageUri: String? = null,
    val error: String? = null,
    val lastLoadMs: Long? = null,
    val lastInferMs: Long? = null,
)

enum class ModelStatus {
    Idle,
    Loading,
    Ready,
    Error,
}
