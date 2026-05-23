package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "dishes")
data class Dish(
    @PrimaryKey val id: Int,
    val name: String,
    val category: String,
    val price: Double,
    val description: String,
    val calories: Int,
    val ingredients: String, // Comma separated
    val allergens: String,   // Comma separated
    val prepTime: String,
    val isFavorite: Boolean = false,
    val modelColor: Long = 0xFFFFA726, // Hex color representing the principal color of the 3D model
    val modelPath: String? = null,
    val modelStylePreset: String? = null
)
