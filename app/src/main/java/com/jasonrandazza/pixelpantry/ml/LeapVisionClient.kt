package com.jasonrandazza.pixelpantry.ml

import ai.liquid.leap.GenerationOptions
import ai.liquid.leap.ModelRunner
import ai.liquid.leap.downloader.LeapModelDownloader
import ai.liquid.leap.message.ChatMessage
import ai.liquid.leap.message.ChatMessageContent
import ai.liquid.leap.message.ImageUtils
import ai.liquid.leap.message.MessageResponse
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * On-device vision-language client for the product Scan flow. Wraps Leap LFM2.5-VL
 * fridge/pantry ingredient detection — the shared, non-spike counterpart to
 * [com.jasonrandazza.pixelpantry.spike.VlmSpikeViewModel].
 */
class LeapVisionClient(context: Context) {
    private val downloader = LeapModelDownloader(context.applicationContext)
    private var runner: ModelRunner? = null

    val isLoaded: Boolean get() = runner != null

    suspend fun loadModel(onProgress: (Float) -> Unit = {}) {
        if (runner != null) return
        runner = downloader.loadModel(
            modelName = MODEL_NAME,
            quantizationType = QUANTIZATION,
            progress = { pd ->
                val fraction = if (pd.total > 0) pd.bytes.toFloat() / pd.total.toFloat() else 0f
                onProgress(fraction.coerceIn(0f, 1f))
            },
        )
    }

    /** Owns [bitmap]; recycles it once encoded. Emits the running response text snapshot. */
    fun analyze(bitmap: Bitmap): Flow<String> = flow {
        val model = runner
        if (model == null) {
            if (!bitmap.isRecycled) bitmap.recycle()
            error("Model not loaded — call loadModel() first.")
        }
        val imageContent = ImageUtils.fromBitmap(bitmap, compressionQuality = 85)
        if (!bitmap.isRecycled) bitmap.recycle()

        val conversation = model.createConversation()
        val message = ChatMessage(
            role = ChatMessage.Role.USER,
            content = listOf(imageContent, ChatMessageContent.Text(INGREDIENT_PROMPT)),
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
                    emit(builder.toString())
                }
                is MessageResponse.Complete -> Unit
                else -> Unit
            }
        }
    }

    suspend fun unload() {
        try {
            runner?.unload()
        } catch (e: Exception) {
            Log.e(TAG, "unload failed", e)
        } finally {
            runner = null
        }
    }

    companion object {
        private const val TAG = "LeapVisionClient"
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
