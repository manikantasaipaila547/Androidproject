package com.example.recipefinder.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.recipefinder.data.model.FavoriteRecipe
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {

    @Query("SELECT * FROM favorite_recipes ORDER BY addedTimestamp DESC")
    fun getAllFavorites(): Flow<List<FavoriteRecipe>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(recipe: FavoriteRecipe)

    @Delete
    suspend fun deleteFavorite(recipe: FavoriteRecipe)

    @Query("DELETE FROM favorite_recipes WHERE idMeal = :idMeal")
    suspend fun deleteFavoriteById(idMeal: String)

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_recipes WHERE idMeal = :idMeal)")
    suspend fun isFavorite(idMeal: String): Boolean
}
