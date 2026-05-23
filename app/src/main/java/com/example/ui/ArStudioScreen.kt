package com.example.ui

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Dish
import com.example.ui.theme.*
import com.example.viewmodel.MenuViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalAnimationApi::class, ExperimentalPermissionsApi::class)
@Composable
fun ArStudioScreen(
    viewModel: MenuViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val cameraPermissionState = rememberPermissionState(android.Manifest.permission.CAMERA)

    val activeArDish by viewModel.activeArDish.collectAsState()
    val allDishes by viewModel.allDishes.collectAsState()

    val isScanningPlanes by viewModel.isScanningPlanes.collectAsState()
    val isDishPlaced by viewModel.isDishPlaced.collectAsState()

    // Gestures states
    val scale by viewModel.modelScale.collectAsState()
    val rotationY by viewModel.modelRotationY.collectAsState()
    val rotationX by viewModel.modelRotationX.collectAsState()
    val offsetX by viewModel.modelOffsetX.collectAsState()
    val offsetY by viewModel.modelOffsetY.collectAsState()
    val shutterFlashActive by viewModel.shutterFlashActive.collectAsState()

    var showLongPressSpecs by remember { mutableStateOf(false) }
    var isCameraRequested by remember { mutableStateOf(false) }

    // Keep physical camera off by default to prevent unexpected surface crashes on virtual platforms lacking CameraX.
    // Users can manually choose to toggle the live camera feed with the dedicated visual button.

    // Multi-staged Scanning timer simulation (simulates ARCore scanning a table floor)
    LaunchedEffect(activeArDish) {
        if (activeArDish != null) {
            viewModel.placeDish() // Preset a placement position to make it extremely responsive
            delay(1800)
            viewModel.finishPlanesScanning()
        }
    }

    if (activeArDish == null) {
        // Safe Empty state if launched without active selection
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(SlateGreyDark),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.QrCodeScanner,
                    contentDescription = "Scan",
                    tint = ClayPrimary,
                    modifier = Modifier.size(80.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Select a Dish to Visualize in AR",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = BoneWhite,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Browse our culinary catalog, then tap 'See in AR' to place a real-size high-fidelity 3D plate directly on your dining table.",
                    fontSize = 13.sp,
                    color = TextMutedSlate,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
                )
            }
        }
    } else {
        val currentDish = activeArDish!!

        Box(
            modifier = modifier
                .fillMaxSize()
                .testTag("ar_studio_view")
        ) {
            // 1. Live Camera / Oak Table stream background
            CameraXOrSimulatedTableContainer(
                isArActive = true,
                isCameraRequested = isCameraRequested,
                onCameraFallback = {
                    isCameraRequested = false
                },
                modifier = Modifier.fillMaxSize()
            ) {

                // 2. Play Scan Line Overlay representing Active Horizontal Mesh Scanning
                if (isScanningPlanes) {
                    ArPlaneScanningOverlay(modifier = Modifier.fillMaxSize())
                } else {
                    // Tap to place grid mesh on the simulated table
                    ArPlanesGridMesh(
                        isDishPlaced = isDishPlaced,
                        onGridTap = {
                            viewModel.placeDish()
                            Toast.makeText(context, "Dish placed on table surface!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // 3. Interactive Placed Dish Model Frame
                if (isDishPlaced) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(currentDish.id) {
                                // Decouple movement and rotation so they do not fight each other.
                                // Zoom + Spin twist (multi-finger) modifies scale/rotation in place.
                                // Drag (single-finger) moves the plate tabletop location cleanly.
                                detectTransformGestures { _, pan, zoom, rotation ->
                                    val isMultiFinger = zoom != 1.0f || rotation != 0.0f
                                    if (isMultiFinger) {
                                        viewModel.scaleModel(zoom)
                                        if (rotation != 0.0f) {
                                            viewModel.rotateModel(rotation, 0f)
                                        } else {
                                            viewModel.rotateModel(pan.x / 2.0f, 0f)
                                        }
                                    } else {
                                        viewModel.translateModel(pan.x, pan.y)
                                    }
                                }
                            }
                            .pointerInput(currentDish.id) {
                                detectTapGestures(
                                    onLongPress = {
                                        showLongPressSpecs = true
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Food3DModelVisualizer(
                            dish = currentDish,
                            scale = scale,
                            rotationX = rotationX,
                            rotationY = rotationY,
                            modifier = Modifier
                                .offset(x = offsetX.dp, y = offsetY.dp)
                                .testTag("placed_3d_dish")
                        )

                        // Float label instruction on placed dish
                        Text(
                            text = "Pinch to scale • Drag to rotate/position • Hold for info",
                            color = BoneWhite.copy(alpha = 0.70f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .offset(y = (-230).dp)
                                .background(GlassyOverDark, RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }

                // 4. White-out Flash Screen shutter animation when customer captures screenshot
                AnimatedVisibility(
                    visible = shutterFlashActive,
                    enter = fadeIn(animationSpec = tween(50)),
                    exit = fadeOut(animationSpec = tween(400))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White)
                    )
                }

                // 5. HUD Upper Panel overlays: Dish active banner
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Elevated pill showing title and description
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = GlassyOverDark),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, SlateBorderLight)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Camera,
                                contentDescription = "Active Lens",
                                tint = ClayPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = currentDish.name,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BoneWhite,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "$${String.format("%.2f", currentDish.price)} • ${currentDish.calories} kcal",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = WarmGoldAccent
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // Camera live stream toggle button
                    IconButton(
                        onClick = {
                            if (isEmulator(context)) {
                                Toast.makeText(context, "Cloud Preview Mode: Stunning 3D Simulated Tabletop active!", Toast.LENGTH_LONG).show()
                            } else if (!cameraPermissionState.status.isGranted) {
                                try {
                                    cameraPermissionState.launchPermissionRequest()
                                } catch (e: Throwable) {
                                    Toast.makeText(context, "Unable to request camera permission", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                isCameraRequested = !isCameraRequested
                                if (isCameraRequested) {
                                    Toast.makeText(context, "Activating camera stream...", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Active: Premium tabletop simulation", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .shadow(4.dp, CircleShape)
                            .background(if (isCameraRequested) ClayPrimary else GlassyOverDark, CircleShape)
                            .border(1.dp, SlateBorderLight, CircleShape)
                            .testTag("camera_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (isCameraRequested) Icons.Default.Camera else Icons.Default.Layers,
                            contentDescription = "Toggle physical camera stream",
                            tint = BoneWhite,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // Close Button to safely exit AR lens
                    IconButton(
                        onClick = { viewModel.stopArSession() },
                        modifier = Modifier
                            .size(44.dp)
                            .shadow(4.dp, CircleShape)
                            .background(GlassyOverDark, CircleShape)
                            .border(1.dp, SlateBorderLight, CircleShape)
                            .testTag("exit_ar_button")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Exit AR Lens", tint = BoneWhite, modifier = Modifier.size(20.dp))
                    }
                }

                // 6. Floating Control buttons panel (For mouse cursor and accessibilities)
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Zoom In Button
                    ArQuickControlIcon(
                        icon = Icons.Default.ZoomIn,
                        desc = "Scale up",
                        onClick = { viewModel.scaleModel(1.10f) }
                    )

                    // Zoom Out Button
                    ArQuickControlIcon(
                        icon = Icons.Default.ZoomOut,
                        desc = "Scale down",
                        onClick = { viewModel.scaleModel(0.90f) }
                    )

                    // Rotate Left Button
                    ArQuickControlIcon(
                        icon = Icons.Default.RotateLeft,
                        desc = "Rotate Left",
                        onClick = { viewModel.rotateModel(-25f) }
                    )

                    // Rotate Right Button
                    ArQuickControlIcon(
                        icon = Icons.Default.RotateRight,
                        desc = "Rotate Right",
                        onClick = { viewModel.rotateModel(25f) }
                    )

                    // Reset Placement Button
                    ArQuickControlIcon(
                        icon = Icons.Default.SettingsBackupRestore,
                        desc = "Reset",
                        onClick = {
                            viewModel.startArSession(currentDish)
                            Toast.makeText(context, "Placement reset!", Toast.LENGTH_SHORT).show()
                        }
                    )
                }

                // 7. AR Camera View Bottom Controls (Carousel selector + Take Photo & Quick Add to order)
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(bottom = 8.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Multi dish toggle swipeable selection carousel
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(allDishes) { dish ->
                            val isSelected = dish.id == currentDish.id
                            val borderMod = if (isSelected) Modifier.border(2.dp, ClayPrimary, RoundedCornerShape(12.dp)) else Modifier

                            Card(
                                modifier = Modifier
                                    .width(135.dp)
                                    .height(55.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { viewModel.startArSession(dish) }
                                    .then(borderMod),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) SlateMediumSurface else GlassyOverDark
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Bullet representation drawing miniature icon
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(SlateGreyDark, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Food3DModelVisualizer(
                                            dish = dish,
                                            scale = 0.28f,
                                            rotationX = 15f,
                                            rotationY = 0f,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text(
                                            dish.name,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = BoneWhite,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            "$${String.format("%.2f", dish.price)}",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = WarmGoldAccent
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Viewfinder Action Bar: Take Photo + Quick Add order tray
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Quick order from AR view button
                        Button(
                            onClick = {
                                viewModel.addToCart(currentDish)
                                Toast.makeText(context, "${currentDish.name} added to cart!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .height(46.dp)
                                .weight(1f)
                                .testTag("ar_quick_order_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = ClayPrimary),
                            shape = RoundedCornerShape(23.dp)
                        ) {
                            Icon(Icons.Default.ShoppingCart, contentDescription = "Order", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Quick Order", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        // Camera Photo Capture Shutter Circular Button
                        IconButton(
                            onClick = {
                                viewModel.triggerShutterAnimate()
                                viewModel.saveArCapture(currentDish.name)
                                Toast.makeText(context, "Captured! Photo saved in gallery hub.", Toast.LENGTH_LONG).show()
                            },
                            modifier = Modifier
                                .size(64.dp)
                                .shadow(8.dp, CircleShape)
                                .background(Color.White, CircleShape)
                                .border(5.dp, GlassyOverDark, CircleShape)
                                .testTag("ar_camera_shutter_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Capture Moment",
                                tint = SlateGreyDark,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        // Remove active item button helper
                        IconButton(
                            onClick = {
                                if (isDishPlaced) {
                                    viewModel.removePlacedDish()
                                    Toast.makeText(context, "Dish removed from surface", Toast.LENGTH_SHORT).show()
                                } else {
                                    viewModel.placeDish()
                                    Toast.makeText(context, "Dish placed!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier
                                .size(46.dp)
                                .shadow(4.dp, CircleShape)
                                .background(GlassyOverDark, CircleShape)
                        ) {
                            Icon(
                                imageVector = if (isDishPlaced) Icons.Default.LayersClear else Icons.Default.LibraryAdd,
                                contentDescription = "Toggle Place",
                                tint = BoneWhite
                            )
                        }
                    }
                }
            }
        }
    }

    // Interactive details sheet display during Long Press Gestures
    if (showLongPressSpecs && activeArDish != null) {
        val currentDish = activeArDish!!
        AlertDialog(
            onDismissRequest = { showLongPressSpecs = false },
            confirmButton = {
                TextButton(onClick = { showLongPressSpecs = false }) {
                    Text("Close", color = ClayPrimary, fontWeight = FontWeight.Bold)
                }
            },
            title = {
                Text(
                    text = currentDish.name,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = BoneWhite
                )
            },
            text = {
                Column {
                    Text(
                        text = "Recipe Composition Insights",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = ClayPrimary,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Text(
                        text = "• Primary ingredients: ${currentDish.ingredients}",
                        fontSize = 12.sp,
                        color = BoneWhite,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                    Text(
                        text = "• Nutrition target: ${currentDish.calories} calories per serving",
                        fontSize = 12.sp,
                        color = BoneWhite,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                    Text(
                        text = "• Active allergen trace: ${if (currentDish.allergens.isEmpty()) "None" else currentDish.allergens}",
                        fontSize = 12.sp,
                        color = if (currentDish.allergens.isEmpty()) SageGreen else WarmRedMuted,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                    Text(
                        text = "• Ideal scale: Real-world 1.0x desktop ratio scale",
                        fontSize = 12.sp,
                        color = TextMutedSlate,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            },
            containerColor = SlateMediumSurface,
            shape = RoundedCornerShape(20.dp)
        )
    }
}

/**
 * Animated camera scanner indicator lines. Shows spinning scanning circles
 * and depth lines over viewfinder environment.
 */
@Composable
fun ArPlaneScanningOverlay(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "scanner_rotation")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Box(
        modifier = modifier.background(Color.Black.copy(alpha = 0.40f)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(contentAlignment = Alignment.Center) {
                // Spinning scanner sweep lines
                Canvas(modifier = Modifier.size(100.dp)) {
                    drawCircle(
                        color = Color(0x3310B981),
                        radius = size.width * 0.48f
                    )
                    drawArc(
                        color = Color(0xFF10B981),
                        startAngle = angle,
                        sweepAngle = 90f,
                        useCenter = false,
                        style = Stroke(width = 6f)
                    )
                }
                Icon(
                    imageVector = Icons.Default.QrCodeScanner,
                    contentDescription = "Locking",
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(36.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Detecting Table Surfaces...",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "Slowing panning environment layout with camera depth",
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.70f),
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

/**
 * Perspective-transformed horizontal plane alignment grid yellow dots.
 * This looks extremely similar to ARCore session planes in physical space.
 */
@Composable
fun ArPlanesGridMesh(
    isDishPlaced: Boolean,
    onGridTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Elegant pulsing animation on dots grid representing placement readiness
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alphaAnim by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable(
                enabled = !isDishPlaced,
                onClick = onGridTap
            )
    ) {
        // Perspective custom plane mesh grid
        Canvas(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.35f)
                .offset(y = 80.dp)
                .graphicsLayer {
                    rotationX = 75f // Tilt back to create flat tabletop perspective!
                    scaleY = 0.6f
                }
        ) {
            val cellCountX = 10
            val cellCountY = 8
            val width = size.width
            val height = size.height

            // Render grid vertices as glowing points
            for (i in 0..cellCountX) {
                val x = (width / cellCountX) * i
                for (j in 0..cellCountY) {
                    val y = (height / cellCountY) * j
                    // Highlight gold grid if dish is not yet placed, showing anchoring vertices
                    val gridColor = if (isDishPlaced) Color.White.copy(alpha = 0.08f) else Color(0xFFF59E0B).copy(alpha = alphaAnim)
                    drawCircle(
                        color = gridColor,
                        radius = 4f,
                        center = Offset(x, y)
                    )
                }
            }

            // Draw outer wireframe rectangle
            if (!isDishPlaced) {
                drawRoundRect(
                    color = Color(0xFFF59E0B).copy(alpha = 0.25f),
                    size = size,
                    style = Stroke(width = 2f, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f))
                )
            }
        }
    }
}

@Composable
fun ArQuickControlIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    desc: String,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(40.dp)
            .shadow(4.dp, CircleShape)
            .background(GlassyOverDark, CircleShape)
            .border(1.dp, SlateBorderLight, CircleShape)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = desc,
            tint = BoneWhite,
            modifier = Modifier.size(18.dp)
        )
    }
}
