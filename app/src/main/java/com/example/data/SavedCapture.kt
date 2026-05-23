package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_captures")
data class SavedCapture(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val dishName: String,
    val timestamp: Long = System.currentTimeMillis(),
    val imageFilePath: String // Path to cached screenshot, or simulated captured asset
)
