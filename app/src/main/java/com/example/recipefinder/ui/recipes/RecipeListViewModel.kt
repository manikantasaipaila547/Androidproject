package com.example.recipefinder.ui.recipes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.recipefinder.data.model.FavoriteRecipe
import com.example.recipefinder.data.model.MealSummary
import com.example.recipefinder.data.repository.RecipeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class RecipeListUiState {
    object Loading : RecipeListUiState()
    data class Success(val meals: List<MealSummary>) : RecipeListUiState()
    data class Error(val message: String) : RecipeListUiState()
    object Empty : RecipeListUiState()
}

class RecipeListViewModel(private val repository: RecipeRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<RecipeListUiState>(RecipeListUiState.Loading)
    val uiState: StateFlow<RecipeListUiState> = _uiState

    fun searchByIngredient(ingredient: String) {
        viewModelScope.launch {
            _uiState.value = RecipeListUiState.Loading
            repository.saveSearch(ingredient)
            val result = repository.filterRecipesByIngredient(ingredient)
            result.onSuccess { meals ->
                if (meals.isEmpty()) {
                    _uiState.value = RecipeListUiState.Empty
                } else {
                    _uiState.value = RecipeListUiState.Success(meals)
                }
            }.onFailure { error ->
                _uiState.value = RecipeListUiState.Error(error.localizedMessage ?: "Network or API Error")
            }
        }
    }

    fun toggleFavorite(meal: MealSummary, isFavorite: Boolean) {
        viewModelScope.launch {
            if (isFavorite) {
                repository.removeFavoriteById(meal.idMeal)
            } else {
                repository.addFavorite(
                    FavoriteRecipe(
                        idMeal = meal.idMeal,
                        strMeal = meal.strMeal,
                        strMealThumb = meal.strMealThumb,
                        strCategory = "Recipe",
                        strArea = "International"
                    )
                )
            }
        }
    }
}
