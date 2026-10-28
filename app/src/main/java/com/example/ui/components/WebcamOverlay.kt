package com.example.ui.components

import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.service.WebcamShape
import com.example.service.WebcamSize
import kotlin.math.roundToInt

@Composable
fun DraggableWebcamOverlay(
    modifier: Modifier = Modifier,
    shape: WebcamShape = WebcamShape.CIRCLE,
    size: WebcamSize = WebcamSize.MEDIUM,
    onClose: () -> Unit,
    onShapeChange: (WebcamShape) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_FRONT) }
    var showControls by remember { mutableStateOf(false) }

    var offsetX by remember { mutableFloatStateOf(40f) }
    var offsetY by remember { mutableFloatStateOf(160f) }

    val density = LocalDensity.current
    val overlaySize = size.sizeDp.dp

    val overlayShape = when (shape) {
        WebcamShape.CIRCLE -> CircleShape
        WebcamShape.ROUNDED_RECT -> RoundedCornerShape(24.dp)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                // Background pass-through
            }
    ) {
        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                .size(overlaySize)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        offsetX += dragAmount.x
                        offsetY += dragAmount.y
                    }
                }
                .testTag("webcam_overlay_container")
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(overlayShape)
                    .border(3.dp, MaterialTheme.colorScheme.primary, overlayShape),
                shape = overlayShape,
                color = Color.Black,
                shadowElevation = 8.dp
            ) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        val previewView = PreviewView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            scaleType = PreviewView.ScaleType.FILL_CENTER
                        }

                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                        cameraProviderFuture.addListener({
                            try {
                                val cameraProvider = cameraProviderFuture.get()
                                val preview = Preview.Builder().build().also {
                                    it.surfaceProvider = previewView.surfaceProvider
                                }
                                val selector = CameraSelector.Builder()
                                    .requireLensFacing(lensFacing)
                                    .build()

                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(lifecycleOwner, selector, preview)
                            } catch (e: Exception) {
                                // Fallback if front camera not found
                                try {
                                    val cameraProvider = cameraProviderFuture.get()
                                    val preview = Preview.Builder().build().also {
                                        it.surfaceProvider = previewView.surfaceProvider
                                    }
                                    cameraProvider.unbindAll()
                                    cameraProvider.bindToLifecycle(
                                        lifecycleOwner,
                                        CameraSelector.DEFAULT_BACK_CAMERA,
                                        preview
                                    )
                                } catch (_: Exception) {}
                            }
                        }, ContextCompat.getMainExecutor(ctx))

                        previewView
                    },
                    update = { view ->
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                        cameraProviderFuture.addListener({
                            try {
                                val cameraProvider = cameraProviderFuture.get()
                                val preview = Preview.Builder().build().also {
                                    it.surfaceProvider = view.surfaceProvider
                                }
                                val selector = CameraSelector.Builder()
                                    .requireLensFacing(lensFacing)
                                    .build()

                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(lifecycleOwner, selector, preview)
                            } catch (_: Exception) {}
                        }, ContextCompat.getMainExecutor(context))
                    }
                )
            }

            // Overlay quick toolbar on tap/hover
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
            ) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(28.dp)
                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                        .testTag("close_webcam_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Camera Overlay",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 6.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    IconButton(
                        onClick = {
                            lensFacing = if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
                                CameraSelector.LENS_FACING_BACK
                            } else {
                                CameraSelector.LENS_FACING_FRONT
                            }
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cameraswitch,
                            contentDescription = "Switch Camera Lens",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            val nextShape = if (shape == WebcamShape.CIRCLE) {
                                WebcamShape.ROUNDED_RECT
                            } else {
                                WebcamShape.CIRCLE
                            }
                            onShapeChange(nextShape)
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = if (shape == WebcamShape.CIRCLE) Icons.Default.CropSquare else Icons.Default.RadioButtonUnchecked,
                            contentDescription = "Toggle Camera Shape",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}
