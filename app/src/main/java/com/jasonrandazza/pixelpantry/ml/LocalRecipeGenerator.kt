package com.jasonrandazza.pixelpantry.ml

import ai.liquid.leap.ModelRunner
import ai.liquid.leap.downloader.LeapModelDownloader
import ai.liquid.leap.message.MessageResponse
import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * On-device text recipe generation via Leap LFM2-1.2B. Loads once, generates recipes
 * from a flat ingredient list, no inventory/network round-trip.
 */
class LocalRecipeGenerator(context: Context) {
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

    /** Emits the running text snapshot as chunks arrive; last emission is the full recipe. */
    fun generateRecipe(ingredientNames: List<String>): Flow<String> = flow {
        val model = runner ?: error("Model not loaded — call loadModel() first.")
        val conversation = model.createConversation()
        val builder = StringBuilder()
        conversation.generateResponse(buildPrompt(ingredientNames)).collect { response ->
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
        private const val TAG = "LocalRecipeGenerator"
        const val MODEL_NAME = "LFM2-1.2B"
        const val QUANTIZATION = "Q5_K_M"

        fun buildPrompt(ingredientNames: List<String>): String {
            val list = ingredientNames.joinToString(", ")
            return """
                Cook a practical recipe using ONLY these ingredients: $list.
                You may also assume basic pantry staples (salt, pepper, oil, water) if needed.
                Do not require any ingredient that is not listed above or a basic staple.
                Respond in this exact format:
                Title: <recipe name>
                Ingredients:
                - <ingredient with rough quantity>
                Steps:
                1. <step>
            """.trimIndent()
        }
    }
}
