package com.jasonrandazza.pixelpantry.scan

enum class VlModelStatus { Idle, Loading, Ready, Error }

data class ScanUiState(
    val modelStatus: VlModelStatus = VlModelStatus.Idle,
    val downloadProgress: Float? = null,
    val isAnalyzing: Boolean = false,
    val responseText: String = "",
    val error: String? = null,
)
