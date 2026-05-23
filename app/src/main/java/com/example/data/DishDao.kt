package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DishDao {
    @Query("SELECT * FROM dishes ORDER BY name ASC")
    fun getAllDishes(): Flow<List<Dish>>

    @Query("SELECT * FROM dishes WHERE id = :id")
    fun getDishById(id: Int): Flow<Dish?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDishes(dishes: List<Dish>)

    @Query("UPDATE dishes SET isFavorite = :isFav WHERE id = :dishId")
    suspend fun updateFavorite(dishId: Int, isFav: Boolean)

    @Query("SELECT * FROM saved_captures ORDER BY timestamp DESC")
    fun getAllCaptures(): Flow<List<SavedCapture>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCapture(capture: SavedCapture)

    @Query("DELETE FROM saved_captures WHERE id = :id")
    suspend fun deleteCapture(id: Int)
}
