package com.example.ui

import android.Manifest
import android.content.Context
import android.util.Log
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.accompanist.permissions.*
import kotlinx.coroutines.launch

fun isEmulator(context: Context? = null): Boolean {
    val fingerprint = (android.os.Build.FINGERPRINT ?: "").lowercase()
    val model = (android.os.Build.MODEL ?: "").lowercase()
    val manufacturer = (android.os.Build.MANUFACTURER ?: "").lowercase()
    val hardware = (android.os.Build.HARDWARE ?: "").lowercase()
    val product = (android.os.Build.PRODUCT ?: "").lowercase()
    val brand = (android.os.Build.BRAND ?: "").lowercase()
    val device = (android.os.Build.DEVICE ?: "").lowercase()
    val board = (android.os.Build.BOARD ?: "").lowercase()

    var isVirtual = fingerprint.contains("generic") ||
            fingerprint.contains("unknown") ||
            fingerprint.contains("cf_") ||
            fingerprint.contains("cuttlefish") ||
            fingerprint.contains("vsoc") ||
            fingerprint.contains("emulator") ||
            fingerprint.contains("simulator") ||
            fingerprint.contains("sdk") ||
            model.contains("google_sdk") ||
            model.contains("emulator") ||
            model.contains("simulator") ||
            model.contains("sdk") ||
            model.contains("cuttlefish") ||
            model.contains("vsoc") ||
            model.contains("gphone") ||
            model.contains("gce") ||
            manufacturer.contains("genymotion") ||
            (manufacturer.contains("google") && !model.contains("pixel") && !model.contains("nexus")) ||
            (brand.contains("generic") && device.contains("generic")) ||
            product.contains("google_sdk") ||
            product.contains("sdk_gphone") ||
            product.contains("cuttlefish") ||
            product.contains("cf_") ||
            product.contains("vsoc") ||
            product.contains("emulator") ||
            product.contains("simulator") ||
            hardware.contains("goldfish") ||
            hardware.contains("ranchu") ||
            hardware.contains("nox") ||
            hardware.contains("cuttlefish") ||
            hardware.contains("vsoc") ||
            hardware.contains("qemu") ||
            hardware.contains("gce") ||
            board.contains("cuttlefish") ||
            board.contains("vsoc") ||
            board.contains("emulator") ||
            board.contains("gce") ||
            device.contains("cuttlefish") ||
            device.contains("cf_") ||
            device.contains("vsoc") ||
            device.contains("emulator") ||
            device.contains("gce") ||
            brand.contains("generic") ||
            (brand.contains("google") && !model.contains("pixel") && !model.contains("nexus"))

    if (!isVirtual) {
        try {
            val qemuFiles = listOf(
                "/dev/socket/qemud",
                "/dev/qemu_pipe",
                "/system/lib/libc_malloc_debug_qemu.so",
                "/sys/qemu_trace",
                "/system/bin/qemu-props"
            )
            for (file in qemuFiles) {
                if (java.io.File(file).exists()) {
                    isVirtual = true
                    break
                }
            }
        } catch (e: Throwable) {
            // Safe fallback
        }
    }

    if (!isVirtual && context != null) {
        try {
            val hasAnyCamera = context.packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_CAMERA_ANY)
            if (!hasAnyCamera) {
                isVirtual = true
            } else {
                val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? android.hardware.camera2.CameraManager
                val idList = cameraManager?.cameraIdList
                if (idList == null || idList.isEmpty()) {
                    isVirtual = true
                }
            }
        } catch (e: Throwable) {
            isVirtual = true
        }
    }

    Log.d("EmulatorDetection", "fingerprint=$fingerprint, model=$model, hardware=$hardware, board=$board, isVirtual=$isVirtual")

    return isVirtual
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraXOrSimulatedTableContainer(
    isArActive: Boolean,
    isCameraRequested: Boolean,
    onCameraFallback: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    val hasCameraFeature = remember {
        context.packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_CAMERA_ANY)
    }
    val isRunningOnEmulator = remember { isEmulator(context) }
    
    var isCameraFailed by remember { mutableStateOf(false) }
    
    // We only try to activate camera on a non-emulator hardware that has camera and hasn't failed, and when requested
    val shouldAttemptCamera = isCameraRequested && !isCameraFailed && hasCameraFeature && !isRunningOnEmulator

    Box(modifier = modifier) {
        if (shouldAttemptCamera && cameraPermissionState.status.isGranted) {
            CameraPreviewLayout(
                onError = {
                    coroutineScope.launch {
                        Log.e("CameraX", "Binding failed, falling back to simulated tabletop", it)
                        isCameraFailed = true
                        onCameraFallback()
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            SimulatedDinnerTabletop(modifier = Modifier.fillMaxSize())
        }

        // Draw general AR scanner hud overlays (depth indicators and lens framing)
        ArLensHudFraming(modifier = Modifier.fillMaxSize())

        // Nested elements (like 3D placed food items, gesture overlays, control pills)
        content()
    }
}

@Composable
fun CameraPreviewLayout(
    onError: (Throwable) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Detect if device has any system camera feature before calling CameraX
    val hasCameraFeature = remember {
        context.packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_CAMERA_ANY)
    }

    if (!hasCameraFeature) {
        LaunchedEffect(Unit) {
            onError(Exception("This device or emulator does not support hardware camera features."))
        }
        Box(modifier = modifier)
        return
    }

    val cameraProviderFuture = remember {
        try {
            ProcessCameraProvider.getInstance(context)
        } catch (exc: Throwable) {
            null
        }
    }

    if (cameraProviderFuture == null) {
        LaunchedEffect(Unit) {
            onError(Exception("Failed to retrieve ProcessCameraProvider instance (unsupported platform)."))
        }
        Box(modifier = modifier)
        return
    }

    var isDisposed by remember { mutableStateOf(false) }
    var activeCameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }

    DisposableEffect(lifecycleOwner) {
        onDispose {
            isDisposed = true
            try {
                activeCameraProvider?.unbindAll()
            } catch (e: Throwable) {
                Log.e("CameraX", "Failed to unbind camera provider on disposal", e)
            }
        }
    }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }

            try {
                cameraProviderFuture.addListener({
                    try {
                        if (isDisposed) return@addListener
                        val cameraProvider = cameraProviderFuture.get()
                        activeCameraProvider = cameraProvider
                        val preview = Preview.Builder().build().also {
                            it.surfaceProvider = previewView.surfaceProvider
                        }
                        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview
                        )
                    } catch (exc: Throwable) {
                        onError(exc)
                    }
                }, ContextCompat.getMainExecutor(ctx))
            } catch (err: Throwable) {
                onError(err)
            }

            previewView
        },
        modifier = modifier
    )
}

/**
 * Beautifully drawn simulated dining table using Canvas.
 * Draws rich warm oak wood grains, candle outlines, and dinnerware shadows.
 */
@Composable
fun SimulatedDinnerTabletop(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        // 1. Dark Oak horizontal wood gradient base
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF331E0F), // Dark wood shadow top
                    Color(0xFF42220F), // Rich warm amber core
                    Color(0xFF261205)  // Dark mahogany bottom
                )
            ),
            topLeft = Offset.Zero,
            size = size
        )

        // 2. Draw vertical or horizontal oak wooden grain lines
        val lineSpacing = 110f
        var currentY = 0f
        while (currentY < height) {
            drawLine(
                color = Color(0xFF1E0C02).copy(alpha = 0.55f),
                start = Offset(0f, currentY),
                end = Offset(width, currentY),
                strokeWidth = 4f
            )
            // Wood plank splits accents
            drawLine(
                color = Color(0xFF5D3A1A).copy(alpha = 0.35f),
                start = Offset(0f, currentY + 12f),
                end = Offset(width, currentY + 12f),
                strokeWidth = 2f
            )
            currentY += lineSpacing
        }

        // 3. Draw a decorative soft central linen fabric table runner
        val runnerWidth = width * 0.65f
        val runnerStart = (width - runnerWidth) / 2
        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFFE5DCD3), Color(0xFFC7B7A7), Color(0xFFB19D8E))
            ),
            topLeft = Offset(runnerStart, 0f),
            size = Size(runnerWidth, height),
            cornerRadius = CornerRadius(15f, 15f)
        )
        // Red linen side stitching lines on the runner
        drawLine(
            color = Color(0xFF9E2A2B).copy(alpha = 0.65f),
            start = Offset(runnerStart + 15f, 0f),
            end = Offset(runnerStart + 15f, height),
            strokeWidth = 3f
        )
        drawLine(
            color = Color(0xFF9E2A2B).copy(alpha = 0.65f),
            start = Offset(runnerStart + runnerWidth - 15f, 0f),
            end = Offset(runnerStart + runnerWidth - 15f, height),
            strokeWidth = 3f
        )

        // 4. Ambient shadows on table corners representing low ambient lighting
        val shadowRadius = width * 0.75f
        if (shadowRadius > 0f && width > 0f) {
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.70f)),
                    center = Offset(width / 2, height / 2),
                    radius = shadowRadius
                ),
                topLeft = Offset.Zero,
                size = size
            )
        }

        // 5. Ambient restaurant context icons sketched on wood sides
        // We will draw elegant plate place-card circles where user is sitting
        val ambientRadius = width * 0.35f
        if (ambientRadius > 0f) {
            drawCircle(
                color = Color.White.copy(alpha = 0.04f),
                radius = ambientRadius,
                center = Offset(width / 2, height + 100f)
            )
        }
    }
}

/**
 * Modern high-precision camera viewfinder framing and depth guidelines
 */
@Composable
fun ArLensHudFraming(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val margin = 50f
        val cornerLength = 40f

        // Center crosshair
        val strokeWidth = 3f
        drawLine(
            color = Color.White.copy(alpha = 0.25f),
            start = Offset(width / 2 - 15f, height / 2),
            end = Offset(width / 2 + 15f, height / 2),
            strokeWidth = strokeWidth
        )
        drawLine(
            color = Color.White.copy(alpha = 0.25f),
            start = Offset(width / 2, height / 2 - 15f),
            end = Offset(width / 2, height / 2 + 15f),
            strokeWidth = strokeWidth
        )

        // Camera viewfinder framing corner brackets
        val bracketColor = Color.White.copy(alpha = 0.35f)
        // Top Left
        drawLine(bracketColor, Offset(margin, margin), Offset(margin + cornerLength, margin), 5f)
        drawLine(bracketColor, Offset(margin, margin), Offset(margin, margin + cornerLength), 5f)
        // Top Right
        drawLine(bracketColor, Offset(width - margin, margin), Offset(width - margin - cornerLength, margin), 5f)
        drawLine(bracketColor, Offset(width - margin, margin), Offset(width - margin, margin + cornerLength), 5f)
        // Bottom Left
        drawLine(bracketColor, Offset(margin, height - margin), Offset(margin + cornerLength, height - margin), 5f)
        drawLine(bracketColor, Offset(margin, height - margin), Offset(margin, height - margin - cornerLength), 5f)
        // Bottom Right
        drawLine(bracketColor, Offset(width - margin, height - margin), Offset(width - margin - cornerLength, height - margin), 5f)
        drawLine(bracketColor, Offset(width - margin, height - margin), Offset(width - margin, height - margin - cornerLength), 5f)
    }
}
