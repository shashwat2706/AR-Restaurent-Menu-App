package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Dish
import com.example.ui.theme.*
import com.example.viewmodel.MenuViewModel
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.provider.OpenableColumns
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.FileOpen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuExplorerScreen(
    viewModel: MenuViewModel,
    onViewInAr: (Dish) -> Unit,
    modifier: Modifier = Modifier
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val filteredDishes by viewModel.filteredDishes.collectAsState()
    val keyboardController = LocalSoftwareKeyboardController.current

    var activeDetailDish by remember { mutableStateOf<Dish?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current
    var showAddCustomDialog by remember { mutableStateOf(false) }
    var pickedFileUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var pickedFileName by remember { mutableStateOf<String>("") }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            pickedFileUri = uri
            var name = ""
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1 && cursor.moveToFirst()) {
                        name = cursor.getString(nameIndex)
                    }
                }
            } catch (e: Exception) {
                // Ignore query error fallback
            }
            if (name.isEmpty()) {
                name = uri.lastPathSegment ?: "3DModel.glb"
            }
            pickedFileName = name
        }
    }

    val categories = listOf("All", "Mains", "Appetizers", "Desserts", "Drinks")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // High-end elegant header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(SlateGreyDark, SlateMediumSurface.copy(alpha = 0.8f))
                    )
                )
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Gourmet Menu",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = BoneWhite,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "Tap on any dish to view recipe. Launch AR to visualize table placement.",
                        fontSize = 12.sp,
                        color = TextMutedSlate,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                FilledTonalButton(
                    onClick = { showAddCustomDialog = true },
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = ClayPrimary,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("add_custom_dish_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Custom Model", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Import glTF", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Search Bar with testTag
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.updateSearchQuery(it) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .testTag("menu_search_input"),
            placeholder = { Text("Search dishes, ingredients...", color = TextMutedSlate) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = TextMutedSlate) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear search", tint = TextMutedSlate)
                    }
                }
            },
            singleLine = true,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = SlateMediumSurface,
                unfocusedContainerColor = SlateGreyDark,
                focusedTextColor = BoneWhite,
                unfocusedTextColor = BoneWhite,
                focusedIndicatorColor = ClayPrimary,
                unfocusedIndicatorColor = SlateBorderLight
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { keyboardController?.hide() }),
            shape = RoundedCornerShape(24.dp)
        )

        // Custom Category Scroller
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
                .testTag("categories_row"),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(categories) { category ->
                val isSelected = selectedCategory == category
                val backgroundBrush = if (isSelected) {
                    Brush.horizontalGradient(listOf(ClayPrimary, ClayPrimaryDim))
                } else {
                    Brush.horizontalGradient(listOf(SlateMediumSurface, SlateMediumSurface))
                }
                val borderModifier = if (isSelected) Modifier else Modifier.border(1.dp, SlateBorderLight, RoundedCornerShape(20.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(backgroundBrush)
                        .then(borderModifier)
                        .clickable { viewModel.selectCategory(category) }
                        .padding(horizontal = 18.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = category,
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.White else TextMutedSlate
                    )
                }
            }
        }

        // Menu Items List
        if (filteredDishes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.RestaurantMenu,
                        contentDescription = "Empty list",
                        tint = TextMutedSlate,
                        modifier = Modifier.size(72.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No culinary creations found",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = BoneWhite
                    )
                    Text(
                        text = "Try adjusting your search query or categories.",
                        fontSize = 12.sp,
                        color = TextMutedSlate,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("dishes_list"),
                contentPadding = PaddingValues(16.dp, bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(filteredDishes, key = { it.id }) { dish ->
                    DishCardItem(
                        dish = dish,
                        onCardClick = { activeDetailDish = dish },
                        onFavoriteToggle = { viewModel.toggleFavorite(dish) },
                        onArClick = { onViewInAr(dish) },
                        onAddToCart = { viewModel.addToCart(dish) }
                    )
                }
            }
        }
    }

    // Recipe & Details Bottom Sheet Display
    activeDetailDish?.let { dish ->
        ModalBottomSheet(
            onDismissRequest = { activeDetailDish = null },
            containerColor = SlateMediumSurface,
            contentColor = BoneWhite,
            dragHandle = { BottomSheetDefaults.DragHandle(color = TextMutedSlate) },
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            DishDetailSheetContent(
                dish = dish,
                onDismiss = { activeDetailDish = null },
                onAddToCart = {
                    viewModel.addToCart(dish)
                    activeDetailDish = null
                },
                onViewInAr = {
                    onViewInAr(dish)
                    activeDetailDish = null
                }
            )
        }
    }

    if (showAddCustomDialog) {
        var dishName by remember { mutableStateOf("") }
        var category by remember { mutableStateOf("Mains") }
        var priceStr by remember { mutableStateOf("") }
        var description by remember { mutableStateOf("") }
        var caloriesStr by remember { mutableStateOf("") }
        var ingredients by remember { mutableStateOf("") }
        var allergens by remember { mutableStateOf("") }
        var prepTime by remember { mutableStateOf("5 min") }
        
        var selectedColorIndex by remember { mutableStateOf(0) }
        val availableColors = listOf(
            0xFFFFB300 to "Golden Yellow",
            0xFFE57373 to "Ruby Red",
            0xFFF57C00 to "Sunset Orange",
            0xFF81C784 to "Pistachio Green",
            0xFF8D6E63 to "Chocolate Velvet",
            0xFF4DB6AC to "Mint Teal",
            0xFF64B5F6 to "Sea Blue",
            0xFFBA68C8 to "Lavender Delight"
        )

        AlertDialog(
            onDismissRequest = { showAddCustomDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CloudUpload,
                        contentDescription = null,
                        tint = ClayPrimary,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text("Import 3D Model", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = BoneWhite)
                }
            },
            containerColor = SlateGreyDark,
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Configure and mount your custom glTF, glb, or obj model as a digital dish with custom pricing and visual parameters.",
                        fontSize = 12.sp,
                        color = TextMutedSlate
                    )

                    // Pick File Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SlateMediumSurface),
                        border = BorderStroke(1.dp, if (pickedFileUri != null) ClayPrimary else SlateBorderLight),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .clickable { filePickerLauncher.launch("*/*") }
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = if (pickedFileUri != null) Icons.Default.CheckCircle else Icons.Default.FileOpen,
                                contentDescription = null,
                                tint = if (pickedFileUri != null) Color(0xFF81C784) else ClayPrimary,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (pickedFileUri != null) "Successfully Linked File" else "Select 3D Model File",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = BoneWhite
                            )
                            Text(
                                text = if (pickedFileUri != null) pickedFileName else "Tap to choose (.gltf, .glb, .obj)",
                                fontSize = 11.sp,
                                color = if (pickedFileUri != null) Color(0xFFC8E6C9) else TextMutedSlate,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }

                    // Form Fields
                    OutlinedTextField(
                        value = dishName,
                        onValueChange = { dishName = it },
                        label = { Text("Dish Name") },
                        singleLine = true,
                        placeholder = { Text("e.g. Alphonso Mango Mousse") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = BoneWhite,
                            unfocusedTextColor = BoneWhite,
                            focusedContainerColor = SlateMediumSurface,
                            unfocusedContainerColor = SlateMediumSurface,
                            focusedBorderColor = ClayPrimary,
                            unfocusedBorderColor = SlateBorderLight
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("custom_dish_name_input")
                    )

                    // Category Selector
                    Text("Category", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMutedSlate)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val availableCats = listOf("Mains", "Appetizers", "Desserts", "Drinks")
                        availableCats.forEach { cat ->
                            val isSelected = category == cat
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) ClayPrimary else SlateMediumSurface)
                                    .clickable { category = cat }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(cat, fontSize = 11.sp, color = if (isSelected) Color.White else TextMutedSlate)
                            }
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = priceStr,
                            onValueChange = { priceStr = it },
                            label = { Text("Price ($)") },
                            singleLine = true,
                            placeholder = { Text("12.50") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = BoneWhite,
                                unfocusedTextColor = BoneWhite,
                                focusedContainerColor = SlateMediumSurface,
                                unfocusedContainerColor = SlateMediumSurface,
                                focusedBorderColor = ClayPrimary,
                                unfocusedBorderColor = SlateBorderLight
                            ),
                            modifier = Modifier.weight(1f).testTag("custom_dish_price_input")
                        )

                        OutlinedTextField(
                            value = caloriesStr,
                            onValueChange = { caloriesStr = it },
                            label = { Text("Calories") },
                            singleLine = true,
                            placeholder = { Text("350") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = BoneWhite,
                                unfocusedTextColor = BoneWhite,
                                focusedContainerColor = SlateMediumSurface,
                                unfocusedContainerColor = SlateMediumSurface,
                                focusedBorderColor = ClayPrimary,
                                unfocusedBorderColor = SlateBorderLight
                            ),
                            modifier = Modifier.weight(1f).testTag("custom_dish_calories_input")
                        )
                    }

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description") },
                        maxLines = 3,
                        placeholder = { Text("Describe the exquisite flavor...") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = BoneWhite,
                            unfocusedTextColor = BoneWhite,
                            focusedContainerColor = SlateMediumSurface,
                            unfocusedContainerColor = SlateMediumSurface,
                            focusedBorderColor = ClayPrimary,
                            unfocusedBorderColor = SlateBorderLight
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = ingredients,
                        onValueChange = { ingredients = it },
                        label = { Text("Ingredients") },
                        placeholder = { Text("e.g. Alphonso Mango, Royal Almonds") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = BoneWhite,
                            unfocusedTextColor = BoneWhite,
                            focusedContainerColor = SlateMediumSurface,
                            unfocusedContainerColor = SlateMediumSurface,
                            focusedBorderColor = ClayPrimary,
                            unfocusedBorderColor = SlateBorderLight
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = allergens,
                            onValueChange = { allergens = it },
                            label = { Text("Allergens") },
                            placeholder = { Text("e.g. Nuts") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = BoneWhite,
                                unfocusedTextColor = BoneWhite,
                                focusedContainerColor = SlateMediumSurface,
                                unfocusedContainerColor = SlateMediumSurface,
                                focusedBorderColor = ClayPrimary,
                                unfocusedBorderColor = SlateBorderLight
                            ),
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = prepTime,
                            onValueChange = { prepTime = it },
                            label = { Text("Prep Time") },
                            placeholder = { Text("e.g. 5 min") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = BoneWhite,
                                unfocusedTextColor = BoneWhite,
                                focusedContainerColor = SlateMediumSurface,
                                unfocusedContainerColor = SlateMediumSurface,
                                focusedBorderColor = ClayPrimary,
                                unfocusedBorderColor = SlateBorderLight
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Text("Preset culinary theme color", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMutedSlate)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        availableColors.forEachIndexed { i, (colorValue, name) ->
                            val isSelected = selectedColorIndex == i
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(Color(colorValue))
                                    .border(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color = if (isSelected) Color.White else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedColorIndex = i }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (dishName.isNotBlank()) {
                            val price = priceStr.toDoubleOrNull() ?: 10.0
                            val calories = caloriesStr.toIntOrNull() ?: 200
                            val selectedColor = availableColors[selectedColorIndex].first

                            viewModel.addNewCustomDish(
                                name = dishName,
                                category = category,
                                price = price,
                                description = description.ifBlank { "Unbelievable dynamic flavor composition with premium fresh ingredients." },
                                calories = calories,
                                ingredients = ingredients.ifBlank { "Unreported" },
                                allergens = allergens.ifBlank { "None" },
                                prepTime = prepTime.ifBlank { "8 min" },
                                modelColor = selectedColor,
                                modelPath = pickedFileUri?.toString(),
                                modelStylePreset = "custom"
                            )
                            showAddCustomDialog = false
                            pickedFileUri = null
                            pickedFileName = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ClayPrimary),
                    enabled = dishName.isNotBlank()
                ) {
                    Text("Decompile & Mount", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCustomDialog = false }) {
                    Text("Cancel", color = TextMutedSlate)
                }
            }
        )
    }
}

@Composable
fun DishCardItem(
    dish: Dish,
    onCardClick: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onArClick: () -> Unit,
    onAddToCart: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (dish.isFavorite) 1.2f else 1.0f,
        animationSpec = spring(dampingRatio = 0.4f),
        label = "fav_scale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(18.dp))
            .clickable { onCardClick() }
            .testTag("dish_card_${dish.id}"),
        colors = CardDefaults.cardColors(containerColor = SlateMediumSurface),
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: High Fidelity Miniature interactive 3D model container as preview
            Box(
                modifier = Modifier
                    .size(95.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(SlateGreyDark)
                    .border(1.dp, SlateBorderLight, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                // Renders miniature visual canvas of model inside preview card! Extremely sleek.
                Food3DModelVisualizer(
                    dish = dish,
                    scale = 0.45f,
                    rotationX = 22f,
                    rotationY = 0f,
                    modifier = Modifier.fillMaxSize()
                )

                // Category banner tag
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .background(
                            Brush.horizontalGradient(listOf(ClayPrimary, ClayPrimaryDim)),
                            RoundedCornerShape(bottomEnd = 8.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = dish.category,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Right: Text details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = dish.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = BoneWhite,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    // Favorite Button
                    IconButton(
                        onClick = onFavoriteToggle,
                        modifier = Modifier
                            .size(28.dp)
                            .scale(scale)
                            .testTag("favorite_button_${dish.id}")
                    ) {
                        Icon(
                            imageVector = if (dish.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (dish.isFavorite) WarmRedMuted else TextMutedSlate,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Text(
                    text = dish.description,
                    fontSize = 11.sp,
                    color = TextMutedSlate,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp, bottom = 6.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Price tag
                    Text(
                        text = "$${String.format("%.2f", dish.price)}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        color = WarmGoldAccent
                    )

                    // Calorie and preparation tag
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocalFireDepartment, contentDescription = "Calories", tint = ClayPrimary, modifier = Modifier.size(14.dp))
                            Text("${dish.calories} kcal", fontSize = 10.sp, color = TextMutedSlate)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.AccessTime, contentDescription = "Time", tint = TextMutedSlate, modifier = Modifier.size(14.dp))
                            Text(dish.prepTime, fontSize = 10.sp, color = TextMutedSlate)
                        }
                    }
                }

                // Action Tray
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // AR View Button
                    Button(
                        onClick = onArClick,
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp)
                            .testTag("view_ar_button_${dish.id}"),
                        colors = ButtonDefaults.buttonColors(containerColor = SlateGreyDark),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = "AR", tint = ClayPrimary, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("See in AR", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BoneWhite)
                        }
                    }

                    // Add to Cart Button
                    Button(
                        onClick = onAddToCart,
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp)
                            .testTag("add_cart_button_${dish.id}"),
                        colors = ButtonDefaults.buttonColors(containerColor = ClayPrimary),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AddShoppingCart, contentDescription = "Cart", tint = Color.White, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Order", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DishDetailSheetContent(
    dish: Dish,
    onDismiss: () -> Unit,
    onAddToCart: () -> Unit,
    onViewInAr: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .testTag("dish_detail_sheet")
    ) {
        // Detailed Top Title
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = dish.name,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = BoneWhite
                )
                Text(
                    text = "${dish.category} • ${dish.prepTime} Preparation",
                    fontSize = 12.sp,
                    color = TextMutedSlate
                )
            }
            Text(
                text = "$${String.format("%.2f", dish.price)}",
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                color = WarmGoldAccent
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Center visual food model close up preview (beautifully rotating and large)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(SlateGreyDark)
                .border(1.dp, SlateBorderLight, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Food3DModelVisualizer(
                dish = dish,
                scale = 0.85f,
                rotationX = 24f,
                rotationY = 45f,
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Description
        Text(
            text = "Description",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = ClayPrimary
        )
        Text(
            text = dish.description,
            fontSize = 13.sp,
            color = BoneWhite,
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
        )

        // Nutrition summary indicators
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Calories", fontSize = 11.sp, color = TextMutedSlate)
                Text("${dish.calories} kcal", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = BoneWhite)
            }
            Divider(modifier = Modifier.width(1.dp).height(30.dp), color = SlateBorderLight)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Allergens", fontSize = 11.sp, color = TextMutedSlate)
                Text(
                    text = if (dish.allergens.isEmpty()) "None" else dish.allergens,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (dish.allergens.isEmpty()) SageGreen else WarmRedMuted
                )
            }
            Divider(modifier = Modifier.width(1.dp).height(30.dp), color = SlateBorderLight)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Cook Space", fontSize = 11.sp, color = TextMutedSlate)
                Text("AR Core", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SageGreen)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Ingredients list
        Text(
            text = "Ingredients Used",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = ClayPrimary
        )
        Text(
            text = dish.ingredients,
            fontSize = 12.sp,
            color = TextMutedSlate,
            modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
        )

        // Large high fidelity Actions in bottom drawer
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // View in AR Button
            OutlinedButton(
                onClick = onViewInAr,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                border = BorderStroke(1.5.dp, ClayPrimary),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = ClayPrimary)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = "Launch AR", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Launch AR", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Quick add to cart
            Button(
                onClick = onAddToCart,
                modifier = Modifier
                    .weight(1.2f)
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ClayPrimary),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ShoppingCart, contentDescription = "Add order", tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add Chef Order", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}
