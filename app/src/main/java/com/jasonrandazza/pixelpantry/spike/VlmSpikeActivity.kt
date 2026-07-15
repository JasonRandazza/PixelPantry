package com.jasonrandazza.pixelpantry.spike

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import java.util.concurrent.Executors

/**
 * Throwaway Leap VL research UI. Not the product shell.
 * Point-and-shoot camera is the primary capture path; gallery is fallback.
 */
class VlmSpikeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(colorScheme = lightColorScheme()) {
                val vm: VlmSpikeViewModel = viewModel()
                VlmSpikeScreen(vm)
            }
        }
    }
}

@Composable
private fun VlmSpikeScreen(vm: VlmSpikeViewModel) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var cameraGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        cameraGranted = granted
    }
    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        uri?.let { vm.analyzeImage(it) }
    }
    val imageCapture = remember { ImageCapture.Builder().build() }
    val captureExecutor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose { captureExecutor.shutdown() }
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("PixelPantry — Leap VL spike", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Point-and-shoot: fill the frame with as many foods as you can, then Capture. " +
                    "Gallery is optional. Model download needs network; inference is on-device.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(state.statusLine, style = MaterialTheme.typography.bodyLarge)
            state.lastLoadMs?.let {
                Text("Last load: $it ms", style = MaterialTheme.typography.bodySmall)
            }
            state.lastInferMs?.let {
                Text("Last infer: $it ms", style = MaterialTheme.typography.bodySmall)
            }

            if (state.modelStatus == ModelStatus.Loading || state.isInferring) {
                CircularProgressIndicator()
            }

            if (cameraGranted) {
                CameraPreview(
                    imageCapture = imageCapture,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(3f / 4f)
                        .heightIn(max = 420.dp),
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(3f / 4f)
                        .heightIn(max = 420.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    OutlinedButton(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                        Text("Allow camera")
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Button(
                    onClick = { vm.loadModel() },
                    enabled = state.modelStatus != ModelStatus.Loading && !state.isInferring,
                ) {
                    Text("Load VL")
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
                                    vm.analyzeBitmap(bitmap, sourceLabel = "camera")
                                }

                                override fun onError(exception: ImageCaptureException) {
                                    Log.e("VlmSpike", "capture failed", exception)
                                    vm.reportCaptureError(exception.message ?: exception.toString())
                                }
                            },
                        )
                    },
                    enabled = state.modelStatus == ModelStatus.Ready && !state.isInferring && cameraGranted,
                ) {
                    Text("Capture")
                }
                OutlinedButton(
                    onClick = { picker.launch("image/*") },
                    enabled = state.modelStatus == ModelStatus.Ready && !state.isInferring,
                ) {
                    Text("Gallery")
                }
                OutlinedButton(
                    onClick = { vm.unloadModel() },
                    enabled = state.modelStatus == ModelStatus.Ready || state.modelStatus == ModelStatus.Error,
                ) {
                    Text("Unload")
                }
            }

            state.error?.let {
                Text("Error: $it", color = MaterialTheme.colorScheme.error)
            }

            state.selectedImageUri?.let {
                Text("Source: $it", style = MaterialTheme.typography.bodySmall)
            }

            if (state.responseText.isNotBlank()) {
                Text("Model output", style = MaterialTheme.typography.titleMedium)
                Text(state.responseText, style = MaterialTheme.typography.bodyMedium)
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
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    DisposableEffect(lifecycleOwner, imageCapture) {
        val providerFuture = ProcessCameraProvider.getInstance(context)
        val mainExecutor = ContextCompat.getMainExecutor(context)
        val listener = Runnable {
            val provider = providerFuture.get()
            val preview = Preview.Builder().build().also {
                it.surfaceProvider = previewView.surfaceProvider
            }
            try {
                provider.unbindAll()
                provider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageCapture,
                )
            } catch (e: Exception) {
                Log.e("VlmSpike", "camera bind failed", e)
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

    AndroidView(
        factory = { previewView },
        modifier = modifier,
    )
}
