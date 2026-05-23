package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MenuViewModel(application: Application) : AndroidViewModel(application) {

    private val db = DishDatabase.getDatabase(application)
    private val repository = DishRepository(db.dishDao())

    // Database states
    val allDishes: StateFlow<List<Dish>> = repository.allDishes.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allCaptures: StateFlow<List<SavedCapture>> = repository.allCaptures.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Filter states
    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Filtered menu
    val filteredDishes: StateFlow<List<Dish>> = combine(
        allDishes,
        _selectedCategory,
        _searchQuery
    ) { dishes, category, query ->
        dishes.filter { dish ->
            val matchCategory = category == "All" || dish.category.lowercase() == category.lowercase()
            val matchQuery = query.isEmpty() || dish.name.lowercase().contains(query.lowercase()) ||
                    dish.description.lowercase().contains(query.lowercase())
            matchCategory && matchQuery
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Shopping Cart state
    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    val cartItems: StateFlow<List<CartItem>> = _cartItems.asStateFlow()

    val totalCartPrice: StateFlow<Double> = _cartItems.map { items ->
        items.sumOf { it.dish.price * it.quantity }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0.0
    )

    // AR Simulation states
    private val _isArActive = MutableStateFlow(false)
    val isArActive: StateFlow<Boolean> = _isArActive.asStateFlow()

    private val _activeArDish = MutableStateFlow<Dish?>(null)
    val activeArDish: StateFlow<Dish?> = _activeArDish.asStateFlow()

    // AR Lifecycle states
    private val _isScanningPlanes = MutableStateFlow(true)
    val isScanningPlanes: StateFlow<Boolean> = _isScanningPlanes.asStateFlow()

    private val _isDishPlaced = MutableStateFlow(false)
    val isDishPlaced: StateFlow<Boolean> = _isDishPlaced.asStateFlow()

    // Gestures states
    private val _modelScale = MutableStateFlow(1.0f)
    val modelScale: StateFlow<Float> = _modelScale.asStateFlow()

    private val _modelRotationY = MutableStateFlow(0f)
    val modelRotationY: StateFlow<Float> = _modelRotationY.asStateFlow()

    private val _modelRotationX = MutableStateFlow(0f)
    val modelRotationX: StateFlow<Float> = _modelRotationX.asStateFlow()

    // Custom 2D placement offsets
    private val _modelOffsetX = MutableStateFlow(0f)
    val modelOffsetX: StateFlow<Float> = _modelOffsetX.asStateFlow()

    private val _modelOffsetY = MutableStateFlow(0f)
    val modelOffsetY: StateFlow<Float> = _modelOffsetY.asStateFlow()

    // Trigger capture shutter animation state
    private val _shutterFlashActive = MutableStateFlow(false)
    val shutterFlashActive: StateFlow<Boolean> = _shutterFlashActive.asStateFlow()

    init {
        viewModelScope.launch {
            repository.seedDatabaseIfEmpty()
        }
    }

    // Filter controls
    fun selectCategory(category: String) {
        _selectedCategory.value = category
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleFavorite(dish: Dish) {
        viewModelScope.launch {
            repository.toggleFavorite(dish.id, dish.isFavorite)
        }
    }

    // Cart actions
    fun addToCart(dish: Dish, quantity: Int = 1) {
        val currentList = _cartItems.value.toMutableList()
        val index = currentList.indexOfFirst { it.dish.id == dish.id }
        if (index >= 0) {
            val existing = currentList[index]
            currentList[index] = existing.copy(quantity = existing.quantity + quantity)
        } else {
            currentList.add(CartItem(dish, quantity))
        }
        _cartItems.value = currentList
    }

    fun removeFromCart(dish: Dish) {
        val currentList = _cartItems.value.filter { it.dish.id != dish.id }
        _cartItems.value = currentList
    }

    fun updateCartQuantity(dish: Dish, quantity: Int) {
        if (quantity <= 0) {
            removeFromCart(dish)
            return
        }
        val currentList = _cartItems.value.toMutableList()
        val index = currentList.indexOfFirst { it.dish.id == dish.id }
        if (index >= 0) {
            currentList[index] = currentList[index].copy(quantity = quantity)
            _cartItems.value = currentList
        }
    }

    fun clearCart() {
        _cartItems.value = emptyList()
    }

    // AR controls
    fun startArSession(dish: Dish) {
        _activeArDish.value = dish
        _isArActive.value = true
        _isScanningPlanes.value = true
        _isDishPlaced.value = false
        // Reset scale/rotation to normal defaults (65f tilt sits perfectly flat on tabletop perspective)
        _modelScale.value = 1.0f
        _modelRotationX.value = 65f
        _modelRotationY.value = 0f
        _modelOffsetX.value = 0f
        _modelOffsetY.value = 80f // Centers the initial placement directly on the tabletop grid mesh
    }

    fun stopArSession() {
        _isArActive.value = false
        _activeArDish.value = null
        _isDishPlaced.value = false
    }

    fun finishPlanesScanning() {
        _isScanningPlanes.value = false
    }

    fun placeDish() {
        _isDishPlaced.value = true
    }

    fun removePlacedDish() {
        _isDishPlaced.value = false
    }

    // Transformation guestures
    fun scaleModel(factor: Float) {
        if (factor.isNaN() || factor.isInfinite()) return
        // Constrain scaling between 0.40x and 2.5x
        _modelScale.value = (_modelScale.value * factor).coerceIn(0.40f, 2.50f)
    }

    fun rotateModel(deltaDegreesY: Float, deltaDegreesX: Float = 0f) {
        if (deltaDegreesY.isNaN() || deltaDegreesY.isInfinite()) return
        if (deltaDegreesX.isNaN() || deltaDegreesX.isInfinite()) return
        _modelRotationY.value = (_modelRotationY.value + deltaDegreesY) % 360f
        // Set limits between 40f and 80f to keep the dish lying flat on the tabletop surface perspective correctly
        _modelRotationX.value = (_modelRotationX.value + deltaDegreesX).coerceIn(40f, 80f)
    }

    fun translateModel(dx: Float, dy: Float) {
        if (dx.isNaN() || dx.isInfinite() || dy.isNaN() || dy.isInfinite()) return
        _modelOffsetX.value += dx
        _modelOffsetY.value += dy
    }

    fun triggerShutterAnimate() {
        viewModelScope.launch {
            _shutterFlashActive.value = true
            kotlinx.coroutines.delay(180)
            _shutterFlashActive.value = false
        }
    }

    fun saveArCapture(dishName: String) {
        viewModelScope.launch {
            // Seed capture with custom description, timestamped, stored inside SQLite log
            val capture = SavedCapture(
                dishName = dishName,
                imageFilePath = "simulated_capture_${System.currentTimeMillis()}"
            )
            repository.addCapture(capture)
        }
    }

    fun deleteCapture(captureId: Int) {
        viewModelScope.launch {
            repository.removeCapture(captureId)
        }
    }

    fun addNewCustomDish(
        name: String,
        category: String,
        price: Double,
        description: String,
        calories: Int,
        ingredients: String,
        allergens: String,
        prepTime: String,
        modelColor: Long,
        modelPath: String?,
        modelStylePreset: String?
    ) {
        viewModelScope.launch {
            val randomId = (1000..999999).random()
            val newDish = Dish(
                id = randomId,
                name = name,
                category = category,
                price = price,
                description = description,
                calories = calories,
                ingredients = ingredients,
                allergens = allergens,
                prepTime = prepTime,
                modelColor = modelColor,
                modelPath = modelPath,
                modelStylePreset = modelStylePreset
            )
            repository.insertDish(newDish)
        }
    }
}
