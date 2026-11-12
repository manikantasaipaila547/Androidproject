package com.example.recipefinder.data.repository

import com.example.recipefinder.data.local.FavoriteDao
import com.example.recipefinder.data.local.SearchHistoryDao
import com.example.recipefinder.data.model.FavoriteRecipe
import com.example.recipefinder.data.model.MealDetail
import com.example.recipefinder.data.model.MealSummary
import com.example.recipefinder.data.model.SearchHistory
import com.example.recipefinder.data.remote.TheMealDbApi
import kotlinx.coroutines.flow.Flow

class RecipeRepository(
    private val api: TheMealDbApi,
    private val favoriteDao: FavoriteDao,
    private val searchHistoryDao: SearchHistoryDao
) {

    suspend fun filterRecipesByIngredient(ingredient: String): Result<List<MealSummary>> {
        return try {
            val response = api.filterByIngredient(ingredient)
            if (response.isSuccessful) {
                val meals = response.body()?.meals ?: emptyList()
                Result.success(meals)
            } else {
                Result.failure(Exception("Failed to fetch recipes for $ingredient"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun searchRecipesByName(name: String): Result<List<MealDetail>> {
        return try {
            val response = api.searchByName(name)
            if (response.isSuccessful) {
                val meals = response.body()?.meals ?: emptyList()
                Result.success(meals)
            } else {
                Result.failure(Exception("Failed to search recipes for $name"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getRecipeDetails(idMeal: String): Result<MealDetail?> {
        return try {
            val response = api.getRecipeDetails(idMeal)
            if (response.isSuccessful) {
                val meal = response.body()?.meals?.firstOrNull()
                Result.success(meal)
            } else {
                Result.failure(Exception("Failed to fetch details for recipe $idMeal"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getAllFavorites(): Flow<List<FavoriteRecipe>> = favoriteDao.getAllFavorites()

    suspend fun addFavorite(recipe: FavoriteRecipe) = favoriteDao.insertFavorite(recipe)

    suspend fun removeFavorite(recipe: FavoriteRecipe) = favoriteDao.deleteFavorite(recipe)

    suspend fun removeFavoriteById(idMeal: String) = favoriteDao.deleteFavoriteById(idMeal)

    suspend fun isFavorite(idMeal: String): Boolean = favoriteDao.isFavorite(idMeal)

    fun getRecentSearches(): Flow<List<SearchHistory>> = searchHistoryDao.getRecentSearches()

    suspend fun saveSearch(query: String) {
        if (query.isNotBlank()) {
            searchHistoryDao.insertSearch(SearchHistory(query = query.trim()))
        }
    }

    suspend fun clearSearchHistory() = searchHistoryDao.clearHistory()
}
