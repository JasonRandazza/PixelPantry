package com.jasonrandazza.pixelpantry

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import ai.liquid.leap.Conversation
import ai.liquid.leap.ModelRunner
import ai.liquid.leap.MessageResponse
import ai.liquid.leap.downloader.LeapModelDownloader
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val modelDownloader = LeapModelDownloader(application)
    private var modelRunner: ModelRunner? = null
    private var conversation: Conversation? = null

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    fun loadModel() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                modelRunner = modelDownloader.loadModel(
                    modelName = "LFM2-1.2B",
                    quantizationType = "Q5_K_M"
                )
                conversation = modelRunner?.createConversation()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun generateResponse(userMessage: String) {
        viewModelScope.launch {
            conversation?.generateResponse(userMessage)
                ?.collect { response ->
                    when (response) {
                        is MessageResponse.Chunk -> println(response.text)
                        is MessageResponse.Complete -> println("Complete")
                    }
                }
        }
    }
}
