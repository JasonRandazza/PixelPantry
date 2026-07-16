package com.jasonrandazza.pixelpantry.scan

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.util.concurrent.Executors

@Composable
fun ScanScreen(
    viewModel: ScanViewModel,
    onUseText: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.ui.collectAsState()
    val context = LocalContext.current
    var cameraGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted -> cameraGranted = granted }
    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri: Uri? -> uri?.let { viewModel.analyzeUri(it) } }
    val imageCapture = remember { ImageCapture.Builder().build() }
    val captureExecutor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) { onDispose { captureExecutor.shutdown() } }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Scan a photo", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Fill the frame with as many food items as you can, then Capture. Model download needs " +
                "network the first time; inference runs on-device.",
            style = MaterialTheme.typography.bodyMedium,
        )

        if (cameraGranted) {
            CameraPreview(
                imageCapture = imageCapture,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(3f / 4f)
                    .heightIn(max = 360.dp),
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(3f / 4f)
                    .heightIn(max = 360.dp),
                contentAlignment = Alignment.Center,
            ) {
                OutlinedButton(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                    Text("Allow camera")
                }
            }
        }

        if (state.modelStatus == VlModelStatus.Loading || state.isAnalyzing) {
            CircularProgressIndicator()
        }
        state.downloadProgress?.let { p ->
            if (state.modelStatus == VlModelStatus.Loading) {
                Text("Download progress: ${(p * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = viewModel::loadModel,
                enabled = state.modelStatus != VlModelStatus.Loading && !state.isAnalyzing,
            ) {
                Text("Load VL model")
            }
            Button(
                onClick = {
                    if (!cameraGranted) {
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                        return@Button
                    }
                    imageCapture.takePicture(
                        captureExecutor,
                        object : ImageCapture.OnImageCapturedCallback() {
                            override fun onCaptureSuccess(image: ImageProxy) {
                                val bitmap = image.toBitmap()
                                image.close()
                                viewModel.analyzeBitmap(bitmap)
                            }

                            override fun onError(exception: ImageCaptureException) {
                                Log.e("ScanScreen", "capture failed", exception)
                                viewModel.reportCaptureError(exception.message ?: exception.toString())
                            }
                        },
                    )
                },
                enabled = state.modelStatus == VlModelStatus.Ready && !state.isAnalyzing && cameraGranted,
            ) {
                Text("Capture")
            }
            OutlinedButton(
                onClick = { picker.launch("image/*") },
                enabled = state.modelStatus == VlModelStatus.Ready && !state.isAnalyzing,
            ) {
                Text("Gallery")
            }
        }

        TextButton(onClick = viewModel::useFixtureSample, modifier = Modifier.fillMaxWidth()) {
            Text("Use sample data (no model needed)")
        }

        state.error?.let { Text("Error: $it", color = MaterialTheme.colorScheme.error) }

        if (state.responseText.isNotBlank()) {
            Text("Detected items", style = MaterialTheme.typography.titleMedium)
            Text(state.responseText, style = MaterialTheme.typography.bodyMedium)
            Button(
                onClick = { onUseText(state.responseText) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Continue to confirm")
            }
        }
    }
}

@Composable
private fun CameraPreview(
    imageCapture: ImageCapture,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember {
        PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER }
    }

    DisposableEffect(lifecycleOwner, imageCapture) {
        val providerFuture = ProcessCameraProvider.getInstance(context)
        val mainExecutor = ContextCompat.getMainExecutor(context)
        val listener = Runnable {
            val provider = providerFuture.get()
            val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
            try {
                provider.unbindAll()
                provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture)
            } catch (e: Exception) {
                Log.e("ScanScreen", "camera bind failed", e)
            }
        }
        providerFuture.addListener(listener, mainExecutor)
        onDispose {
            providerFuture.addListener(
                {
                    try {
                        providerFuture.get().unbindAll()
                    } catch (_: Exception) {
                    }
                },
                mainExecutor,
            )
        }
    }

    AndroidView(factory = { previewView }, modifier = modifier)
}
