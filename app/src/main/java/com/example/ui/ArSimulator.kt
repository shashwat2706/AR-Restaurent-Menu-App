package com.example.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.example.data.Dish
import kotlin.math.cos
import kotlin.math.sin

/**
 * Highly detailed custom 3D model visualizer using Compose graphicsLayer projection.
 * Enables 3D pitch/yaw rotation, dynamic lighting/shadow simulation, and gestures.
 */
@Composable
fun Food3DModelVisualizer(
    dish: Dish,
    scale: Float,
    rotationX: Float,
    rotationY: Float,
    modifier: Modifier = Modifier
) {
    // Elegant ambient pulsing light shine
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shineOffset by infiniteTransition.animateFloat(
        initialValue = -100f,
        targetValue = 400f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shine"
    )

    Box(
        modifier = modifier
            .size(240.dp)
            .graphicsLayer {
                this.rotationX = rotationX
                this.rotationY = rotationY
                this.scaleX = scale
                this.scaleY = scale
                // Enhance 3D perspective depth camera distance using density to avoid singular projection rendering crash
                this.cameraDistance = 16f * density
            },
        contentAlignment = Alignment.Center
    ) {
        // 3D Shadow Plate on the tabletop surface (cancels yaw rotation to cast a stable horizontal shadow)
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    // Cancel parent's yaw rotation so the shadow ellipse remains perfectly aligned horizontally on the table
                    this.rotationY = -rotationY
                    // Slightly lower the drawing coordinate system to create a realistic tabletop contact level
                    this.translationY = 16f
                }
        ) {
            val plateColor = Color(dish.modelColor)
            val w = size.width

            if (w > 1f) {
                val r1 = (w * 0.58f).coerceAtLeast(0.1f)
                val r2 = (w * 0.48f).coerceAtLeast(0.1f)
                val r3 = (w * 0.35f).coerceAtLeast(0.1f)

                // 1. HIGH-FIDELITY AMBIENT COLOR BLEED (Global Illumination table bounce)
                // Simulates the primary food plate color reflecting softly onto the wooden tabletop
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            plateColor.copy(alpha = 0.14f),
                            plateColor.copy(alpha = 0.04f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = r1
                    ),
                    radius = r1,
                    center = center
                )

                // 2. SOFT FEATHERED AMBIENT DROP SHADOW
                // A wide, smooth radial gradient that fades out naturally to create a realistic indoor shadow penumbra
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.45f),
                            Color.Black.copy(alpha = 0.25f),
                            Color.Black.copy(alpha = 0.08f),
                            Color.Transparent
                        ),
                        center = Offset(center.x, center.y - 10f), // Shift slightly up (further back on the table depth plane)
                        radius = r2
                    ),
                    radius = r2,
                    center = Offset(center.x, center.y - 10f)
                )

                // 3. SHARP DARK CONTACT SHADOW (Ambient Occlusion)
                // A tight, deep matte shadow right at the plate's bottom rim where it makes contact with the tabletop
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.70f),
                            Color.Black.copy(alpha = 0.40f),
                            Color.Transparent
                        ),
                        center = Offset(center.x, center.y + 12f), // Shift toward the screen slightly for the near contact edge
                        radius = r3
                    ),
                    radius = r3,
                    center = Offset(center.x, center.y + 12f)
                )
            }
        }

        // The Ceramic Serving Plate and the custom dish drawing
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val centerX = canvasWidth / 2
            val centerY = canvasHeight / 2
            val sizeRadius = canvasWidth * 0.41f
            val isCustom = !dish.modelPath.isNullOrEmpty() || dish.modelStylePreset == "custom" || dish.id >= 10

            if (canvasWidth > 1f && canvasHeight > 1f) {
                if (!isCustom) {
                    val outerRadius = (sizeRadius + 8f).coerceAtLeast(0.1f)
                    val outerCircleRadius = (sizeRadius + 6f).coerceAtLeast(0.1f)
                    val goldenRadius = (sizeRadius - 2f).coerceAtLeast(0.1f)
                    val innerRadius = (sizeRadius - 8f).coerceAtLeast(0.1f)
                    val innerCircleRadius = (sizeRadius - 6f).coerceAtLeast(0.1f)

                    // Outer golden-rimmed fine ceramic plate
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFF2C3E50), Color(0xFF1A252F), Color(0xFF0F172A)),
                            center = Offset(centerX - 30f, centerY - 30f),
                            radius = outerRadius
                        ),
                        radius = outerCircleRadius,
                        center = Offset(centerX, centerY)
                    )

                    // Golden elegant circular trim line
                    drawCircle(
                        color = Color(0xFFF59E0B),
                        radius = goldenRadius,
                        center = Offset(centerX, centerY),
                        style = Stroke(width = 3f)
                    )

                    // Inner plate surface
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFFFCFDFE), Color(0xFFECEFF1), Color(0xFFCFD8DC)),
                            center = Offset(centerX - 15f, centerY - 15f),
                            radius = innerRadius
                        ),
                        radius = innerCircleRadius,
                        center = Offset(centerX, centerY)
                    )
                }

            // Sauté and food layout center
            val foodCenterX = centerX
            val foodCenterY = centerY
            val foodRadius = sizeRadius * 0.72f

            // Draw specific high-fidelity visual representations of each dish based on the list ID
            when (dish.id) {
                1 -> { // Margherita Wood-Fired Pizza
                    // Red Sauce Base
                    drawCircle(
                        color = Color(0xFFD32F2F),
                        radius = foodRadius,
                        center = Offset(foodCenterX, foodCenterY)
                    )
                    // Charred crust lines
                    for (i in 0 until 8) {
                        val angle = (i * 45) * (Math.PI / 180f)
                        val startX = foodCenterX + (foodRadius * 0.85f * cos(angle)).toFloat()
                        val startY = foodCenterY + (foodRadius * 0.85f * sin(angle)).toFloat()
                        val endX = foodCenterX + (foodRadius * 1.05f * cos(angle)).toFloat()
                        val endY = foodCenterY + (foodRadius * 1.05f * sin(angle)).toFloat()
                        drawLine(
                            color = Color(0xFF3E2723),
                            start = Offset(startX, startY),
                            end = Offset(endX, endY),
                            strokeWidth = 5f
                        )
                    }
                    // Mozzarella cheese puddles
                    val cheeseCoords = listOf(
                        Offset(-15f, -30f), Offset(35f, -20f), Offset(-25f, 25f),
                        Offset(20f, 30f), Offset(-5f, 5f)
                    )
                    cheeseCoords.forEach { offset ->
                        drawCircle(
                            color = Color(0xFFFFFDE7),
                            radius = foodRadius * 0.28f,
                            center = Offset(foodCenterX + offset.x, foodCenterY + offset.y)
                        )
                    }
                    // Green Basil leaves
                    val basilCoords = listOf(Offset(25f, -40f), Offset(-40f, -5f), Offset(5f, 35f))
                    basilCoords.forEach { offset ->
                        drawCircle(
                            color = Color(0xFF2E7D32),
                            radius = foodRadius * 0.12f,
                            center = Offset(foodCenterX + offset.x, foodCenterY + offset.y)
                        )
                        drawCircle(
                            color = Color(0xFF81C784),
                            radius = foodRadius * 0.05f,
                            center = Offset(foodCenterX + offset.x - 2f, foodCenterY + offset.y - 2f)
                        )
                    }
                }
                2 -> { // Charred Wagyu Ribeye
                    // Thick juicy grilled steak shape
                    val path = Path().apply {
                        moveTo(foodCenterX - foodRadius * 0.8f, foodCenterY - foodRadius * 0.2f)
                        quadraticTo(
                            foodCenterX - foodRadius * 0.4f, foodCenterY - foodRadius * 0.9f,
                            foodCenterX + foodRadius * 0.4f, foodCenterY - foodRadius * 0.7f
                        )
                        quadraticTo(
                            foodCenterX + foodRadius * 1.0f, foodCenterY - foodRadius * 0.4f,
                            foodCenterX + foodRadius * 0.8f, foodCenterY + foodRadius * 0.4f
                        )
                        quadraticTo(
                            foodCenterX + foodRadius * 0.2f, foodCenterY + foodRadius * 0.9f,
                            foodCenterX - foodRadius * 0.5f, foodCenterY + foodRadius * 0.7f
                        )
                        close()
                    }
                    drawPath(
                        path = path,
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0xFF3E2723), Color(0xFF212121), Color(0xFF4E342E)),
                            start = Offset(foodCenterX - 50f, foodCenterY - 50f),
                            end = Offset(foodCenterX + 50f, foodCenterY + 50f)
                        )
                    )
                    // Golden-brown rosemary potatoes
                    val potatoPositions = listOf(Offset(-65f, -65f), Offset(-75f, 15f), Offset(70f, 60f))
                    potatoPositions.forEach { offset ->
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0xFFFFB300), Color(0xFFE65100)),
                                center = Offset(foodCenterX + offset.x, foodCenterY + offset.y),
                                radius = 24f
                            ),
                            radius = 18f,
                            center = Offset(foodCenterX + offset.x, foodCenterY + offset.y)
                        )
                    }
                    // White melting truffle butter block in the center
                    drawRect(
                        color = Color(0xFFFFF9C4),
                        topLeft = Offset(foodCenterX - 15f, foodCenterY - 15f),
                        size = Size(30f, 30f)
                    )
                    drawRect(
                        color = Color(0xFFFFF176),
                        topLeft = Offset(foodCenterX - 12f, foodCenterY - 12f),
                        size = Size(20f, 20f)
                    )
                }
                3 -> { // Avocado Mango Tartare
                    // Concentric layers of green and gold yellow
                    // Bottom green avocado layer
                    drawCircle(
                        color = Color(0xFF1B5E20),
                        radius = foodRadius,
                        center = Offset(foodCenterX, foodCenterY)
                    )
                    drawCircle(
                        color = Color(0xFF4CAF50),
                        radius = foodRadius * 0.88f,
                        center = Offset(foodCenterX, foodCenterY)
                    )
                    // Golden mango center layer
                    drawCircle(
                        color = Color(0xFFFF8F00),
                        radius = foodRadius * 0.65f,
                        center = Offset(foodCenterX, foodCenterY)
                    )
                    drawCircle(
                        color = Color(0xFFFFD54F),
                        radius = foodRadius * 0.58f,
                        center = Offset(foodCenterX, foodCenterY)
                    )
                    // Sesame seeds garnish on top
                    for (i in 0 until 12) {
                        val seedAngle = (i * 30) * (Math.PI / 180f)
                        val offsetRadius = foodRadius * 0.35f
                        drawCircle(
                            color = Color(0xFF3E2723),
                            radius = 3f,
                            center = Offset(
                                foodCenterX + (offsetRadius * cos(seedAngle)).toFloat(),
                                foodCenterY + (offsetRadius * sin(seedAngle)).toFloat()
                            )
                        )
                    }
                }
                4 -> { // Truffle Mushroom Bruschetta
                    // Diagonal golden toast bread
                    val breadPath = Path().apply {
                        moveTo(foodCenterX - foodRadius * 0.9f, foodCenterY - foodRadius * 0.2f)
                        lineTo(foodCenterX + foodRadius * 0.6f, foodCenterY - foodRadius * 0.8f)
                        lineTo(foodCenterX + foodRadius * 0.9f, foodCenterY + foodRadius * 0.2f)
                        lineTo(foodCenterX - foodRadius * 0.6f, foodCenterY + foodRadius * 0.8f)
                        close()
                    }
                    drawPath(
                        path = breadPath,
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0xFFE65100), Color(0xFFFFB74D)),
                            start = Offset(foodCenterX - 50f, foodCenterY - 50f),
                            end = Offset(foodCenterX + 50f, foodCenterY + 50f)
                        )
                    )
                    // Creamy whipped ricotta
                    drawCircle(
                        color = Color(0xFFFFFDE7),
                        radius = foodRadius * 0.40f,
                        center = Offset(foodCenterX, foodCenterY)
                    )
                    // Sautéed dark wild mushrooms on toast
                    val mushroomPositions = listOf(Offset(-15f, -15f), Offset(20f, -25f), Offset(15f, 15f), Offset(-25f, 25f))
                    mushroomPositions.forEach { offset ->
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0xFF4E342E), Color(0xFF2D1500)),
                                center = Offset(foodCenterX + offset.x, foodCenterY + offset.y)
                            ),
                            radius = 16f,
                            center = Offset(foodCenterX + offset.x, foodCenterY + offset.y)
                        )
                    }
                }
                5 -> { // Decadent Lava Fondant
                    // High-rising dark chocolate cake mold
                    drawCircle(
                        color = Color(0xFF2D1500),
                        radius = foodRadius * 0.75f,
                        center = Offset(foodCenterX, foodCenterY)
                    )
                    drawCircle(
                        color = Color(0xFF4E342E),
                        radius = foodRadius * 0.65f,
                        center = Offset(foodCenterX, foodCenterY)
                    )
                    // Brilliant ruby molten lava flow out of side
                    val lavaPath = Path().apply {
                        moveTo(foodCenterX - 10f, foodCenterY)
                        quadraticTo(foodCenterX - 35f, foodCenterY + 55f, foodCenterX - 55f, foodCenterY + 68f)
                        lineTo(foodCenterX - 25f, foodCenterY + 74f)
                        quadraticTo(foodCenterX - 5f, foodCenterY + 55f, foodCenterX + 10f, foodCenterY)
                        close()
                    }
                    drawPath(
                        path = lavaPath,
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFFFF1744), Color(0xFFD50000)),
                            center = Offset(foodCenterX - 25f, foodCenterY + 25f)
                        )
                    )
                    // Gourmet Vanilla bean gelato scoop
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color.White, Color(0xFFFFFDE7)),
                            center = Offset(foodCenterX + 45f, foodCenterY - 45f)
                        ),
                        radius = foodRadius * 0.33f,
                        center = Offset(foodCenterX + 45f, foodCenterY - 45f)
                    )
                }
                6 -> { // Matcha Pistachio Tiramisu
                    // Structured square layers of cake
                    drawRect(
                        color = Color(0xFF558B2F),
                        topLeft = Offset(foodCenterX - foodRadius * 0.7f, foodCenterY - foodRadius * 0.7f),
                        size = Size(foodRadius * 1.4f, foodRadius * 1.4f)
                    )
                    // Soft mascarpone cream piping loops
                    val creamOffsets = listOf(
                        Offset(-35f, -35f), Offset(1f, -35f), Offset(35f, -35f),
                        Offset(-35f, 1f), Offset(1f, 1f), Offset(35f, 1f),
                        Offset(-35f, 35f), Offset(1f, 35f), Offset(35f, 35f)
                    )
                    creamOffsets.forEach { pos ->
                        drawCircle(
                            color = Color(0xFFFFFFEE),
                            radius = 20f,
                            center = Offset(foodCenterX + pos.x, foodCenterY + pos.y)
                        )
                    }
                    // Green tea powder sprinkles dusted
                    for (i in 0 until 18) {
                        drawCircle(
                            color = Color(0xFF33691E),
                            radius = 4f,
                            center = Offset(
                                foodCenterX - 40f + (i * 5) + (i % 3 * 6),
                                foodCenterY - 40f + (i * 3) + (i % 2 * 12)
                            )
                        )
                    }
                    // Pistachio chunks
                    val nuts = listOf(Offset(-15f, -10f), Offset(20f, 18f), Offset(-10f, 30f))
                    nuts.forEach { pos ->
                        drawCircle(
                            color = Color(0xFFDCEDC8),
                            radius = 8f,
                            center = Offset(foodCenterX + pos.x, foodCenterY + pos.y)
                        )
                    }
                }
                7 -> { // Smoked Rosemary Old Fashioned
                    // Sleek octagonal clear glass silhouette
                    val glassPath = Path().apply {
                        moveTo(foodCenterX - 35f, foodCenterY - 55f)
                        lineTo(foodCenterX + 35f, foodCenterY - 55f)
                        lineTo(foodCenterX + 28f, foodCenterY + 50f)
                        lineTo(foodCenterX - 28f, foodCenterY + 50f)
                        close()
                    }
                    // Liquid amber contents
                    val orangeLiquidPath = Path().apply {
                        moveTo(foodCenterX - 32f, foodCenterY - 20f)
                        lineTo(foodCenterX + 32f, foodCenterY - 20f)
                        lineTo(foodCenterX + 28f, foodCenterY + 45f)
                        lineTo(foodCenterX - 28f, foodCenterY + 45f)
                        close()
                    }
                    // Draw outer glass
                    drawPath(
                        path = glassPath,
                        color = Color.White.copy(alpha = 0.25f),
                        style = Stroke(width = 4f)
                    )
                    // Draw liquid with gradient
                    drawPath(
                        path = orangeLiquidPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0xFFFFB300), Color(0xFFE65100), Color(0xFFE65100))
                        )
                    )
                    // Large square ice-cube melting inside
                    drawRect(
                        color = Color.White.copy(alpha = 0.5f),
                        topLeft = Offset(foodCenterX - 18f, foodCenterY + 5f),
                        size = Size(35f, 35f)
                    )
                    // Rosemary stalk
                    drawLine(
                        color = Color(0xFF1B5E20),
                        start = Offset(foodCenterX - 25f, foodCenterY + 40f),
                        end = Offset(foodCenterX + 25f, foodCenterY - 45f),
                        strokeWidth = 6f
                    )
                }
                8 -> { // Hibiscus Ginger Elixir
                    // Sleek tall chalice shape
                    val glassPath = Path().apply {
                        moveTo(foodCenterX - 28f, foodCenterY - 65f)
                        lineTo(foodCenterX + 28f, foodCenterY - 65f)
                        lineTo(foodCenterX + 16f, foodCenterY + 50f)
                        lineTo(foodCenterX - 16f, foodCenterY + 50f)
                        close()
                    }
                    // Liquid magenta contents
                    val rubyLiquidPath = Path().apply {
                        moveTo(foodCenterX - 26f, foodCenterY - 30f)
                        lineTo(foodCenterX + 26f, foodCenterY - 30f)
                        lineTo(foodCenterX + 16f, foodCenterY + 45f)
                        lineTo(foodCenterX - 16f, foodCenterY + 45f)
                        close()
                    }
                    // Draw glass rim
                    drawPath(
                        path = glassPath,
                        color = Color.White.copy(alpha = 0.3f),
                        style = Stroke(width = 4f)
                    )
                    // Draw glowing ruby nectar
                    drawPath(
                        path = rubyLiquidPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0xFFE91E63), Color(0xFF880E4F), Color(0xFF880E4F))
                        )
                    )
                    // Lemon crescent slice wedged on rim
                    drawArc(
                        color = Color(0xFFFFEB3B),
                        startAngle = -45f,
                        sweepAngle = 180f,
                        useCenter = true,
                        topLeft = Offset(foodCenterX - 45f, foodCenterY - 74f),
                        size = Size(35f, 35f)
                    )
                }
                9 -> { // Badam Mango Delight
                    // Rich golden mango pulp base circle
                    drawCircle(
                        color = Color(0xFFFFB300), // Rich Mango Gold
                        radius = foodRadius,
                        center = Offset(foodCenterX, foodCenterY)
                    )
                    // Concentric cream/cardamom swirl
                    val cardamomPath = Path().apply {
                        moveTo(foodCenterX - foodRadius * 0.4f, foodCenterY - foodRadius * 0.4f)
                        quadraticTo(
                            foodCenterX + foodRadius * 0.5f, foodCenterY - foodRadius * 0.6f,
                            foodCenterX + foodRadius * 0.4f, foodCenterY + foodRadius * 0.4f
                        )
                        quadraticTo(
                            foodCenterX - foodRadius * 0.6f, foodCenterY + foodRadius * 0.5f,
                            foodCenterX - foodRadius * 0.2f, foodCenterY - foodRadius * 0.2f
                        )
                    }
                    drawPath(
                        path = cardamomPath,
                        color = Color(0xFFFFF9C4).copy(alpha = 0.7f),
                        style = Stroke(width = 8f)
                    )
                    // Slivered almonds (ellipses)
                    val almondPositions = listOf(
                        Offset(-30f, -25f), Offset(25f, -15f), Offset(-15f, 35f), Offset(35f, 25f), Offset(-5f, -5f)
                    )
                    almondPositions.forEach { pos ->
                        drawCircle(
                            color = Color(0xFFFFFDE7), // Creamy almond white
                            radius = 12f,
                            center = Offset(foodCenterX + pos.x, foodCenterY + pos.y)
                        )
                        drawCircle(
                            color = Color(0xFFD7CCC8),
                            radius = 12f,
                            center = Offset(foodCenterX + pos.x, foodCenterY + pos.y),
                            style = Stroke(width = 2f)
                        )
                    }
                    // Saffron threads represented by thin bright red curves
                    val saffronSlopes = listOf(
                        Offset(-15f, -35f) to Offset(-5f, -25f),
                        Offset(15f, 15f) to Offset(25f, 30f),
                        Offset(-25f, 10f) to Offset(-15f, 15f)
                    )
                    saffronSlopes.forEach { (start, end) ->
                        drawLine(
                            color = Color(0xFFFF3000), // Vivid reddish orange (saffron)
                            start = Offset(foodCenterX + start.x, foodCenterY + start.y),
                            end = Offset(foodCenterX + end.x, foodCenterY + end.y),
                            strokeWidth = 3f
                        )
                    }
                    // Pistachio green crushed bits on top
                    val pistachioPositions = listOf(
                        Offset(-40f, -10f), Offset(10f, -40f), Offset(-10f, 45f), Offset(45f, -5f), Offset(5f, 15f)
                    )
                    pistachioPositions.forEach { pos ->
                        drawCircle(
                            color = Color(0xFF81C784), // Pistachio green
                            radius = 6f,
                            center = Offset(foodCenterX + pos.x, foodCenterY + pos.y)
                        )
                    }
                }
                else -> {
                    // Custom imported/uploaded 3D model simulation
                    val primaryColor = Color(dish.modelColor)
                    
                    val parsedRadialRadius = (foodRadius * 0.85f).coerceAtLeast(0.1f)
                    val parsedCircleRadius = (foodRadius * 0.8f).coerceAtLeast(0.1f)

                    // Main delicious component (large 3D sphere/dome with light gradient for volume)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                primaryColor,
                                primaryColor.copy(alpha = 0.8f),
                                Color(0xFF1E293B) // Dark core shadow inside the dome
                            ),
                            center = Offset(foodCenterX - 10f, foodCenterY - 10f),
                            radius = parsedRadialRadius
                        ),
                        radius = parsedCircleRadius,
                        center = Offset(foodCenterX, foodCenterY)
                    )

                    // Delicious glaze swirl/compote on top in high contract secondary tone
                    val glazeColor = if (dish.modelColor != 0xFFFFFFFF) Color.White.copy(alpha = 0.5f) else Color(0xFFF59E0B)
                    val glazePath = Path().apply {
                        moveTo(foodCenterX - foodRadius * 0.5f, foodCenterY - foodRadius * 0.3f)
                        quadraticTo(
                            foodCenterX + foodRadius * 0.6f, foodCenterY - foodRadius * 0.5f,
                            foodCenterX + foodRadius * 0.3f, foodCenterY + foodRadius * 0.4f
                        )
                        quadraticTo(
                            foodCenterX - foodRadius * 0.4f, foodCenterY + foodRadius * 0.5f,
                            foodCenterX, foodCenterY
                        )
                    }
                    drawPath(
                        path = glazePath,
                        color = glazeColor,
                        style = Stroke(width = 6f)
                    )

                    // Seed customized decorative microgreen/garlic/crispy toppings based on Name hash code
                    val hash = dish.name.hashCode()
                    val toppingsCount = 4 + (hash % 5).coerceIn(1, 6)
                    for (i in 0 until toppingsCount) {
                        val angle = (i * (360f / toppingsCount)) * (Math.PI / 180f)
                        val dist = foodRadius * 0.4f + (i * 1234 % 30) - 15f
                        val topX = foodCenterX + (dist * cos(angle)).toFloat()
                        val topY = foodCenterY + (dist * sin(angle)).toFloat()

                        // Alternating garnish sprinkles
                        if (i % 2 == 0) {
                            // Microgreen garnish
                            drawCircle(
                                color = Color(0xFF4CAF50), // Fresh microgreen
                                radius = 8f,
                                center = Offset(topX, topY)
                            )
                        } else {
                            // Gold leaf sprinkle
                            drawRect(
                                color = Color(0xFFFFD700), // Sparkling gold flake
                                topLeft = Offset(topX - 4f, topY - 4f),
                                size = Size(8f, 8f)
                            )
                        }
                    }

                    // Floating 3D Model Label indicator or watermark
                    val isCustomFile = !dish.modelPath.isNullOrEmpty()
                    if (isCustomFile) {
                        // Small glowing orbit indicator indicating a custom loaded mesh file
                        drawCircle(
                            color = Color(0xFF00E676),
                            radius = 6f,
                            center = Offset(foodCenterX - foodRadius * 0.7f, foodCenterY - foodRadius * 0.7f)
                        )
                    }
                }
            }

            // Pulsing light glint running sweeps across the plate to look highly active and 3D glossy
            val shineBrush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.0f),
                    Color.White.copy(alpha = 0.35f),
                    Color.White.copy(alpha = 0.0f)
                ),
                start = Offset(shineOffset, -50f),
                end = Offset(shineOffset + 120f, 350f)
            )
            val cleanShineRadius = (sizeRadius - 4f).coerceAtLeast(0.1f)
            drawCircle(
                brush = shineBrush,
                radius = cleanShineRadius,
                center = Offset(centerX, centerY)
            )
            }
        }
    }
}
