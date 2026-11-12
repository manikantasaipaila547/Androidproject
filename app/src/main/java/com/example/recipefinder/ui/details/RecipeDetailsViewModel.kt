package com.example.recipefinder.ui.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.recipefinder.data.model.FavoriteRecipe
import com.example.recipefinder.data.model.MealDetail
import com.example.recipefinder.data.repository.RecipeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class RecipeDetailsUiState {
    object Loading : RecipeDetailsUiState()
    data class Success(val mealDetail: MealDetail, val isFavorite: Boolean) : RecipeDetailsUiState()
    data class Error(val message: String) : RecipeDetailsUiState()
}

class RecipeDetailsViewModel(private val repository: RecipeRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<RecipeDetailsUiState>(RecipeDetailsUiState.Loading)
    val uiState: StateFlow<RecipeDetailsUiState> = _uiState

    fun loadRecipeDetails(idMeal: String) {
        viewModelScope.launch {
            _uiState.value = RecipeDetailsUiState.Loading
            val result = repository.getRecipeDetails(idMeal)
            val isFav = repository.isFavorite(idMeal)

            result.onSuccess { meal ->
                if (meal != null) {
                    _uiState.value = RecipeDetailsUiState.Success(meal, isFav)
                } else {
                    _uiState.value = RecipeDetailsUiState.Error("Recipe details not found.")
                }
            }.onFailure { error ->
                _uiState.value = RecipeDetailsUiState.Error(error.localizedMessage ?: "Failed to load recipe details.")
            }
        }
    }

    fun toggleFavorite(meal: MealDetail, currentIsFav: Boolean) {
        viewModelScope.launch {
            if (currentIsFav) {
                repository.removeFavoriteById(meal.idMeal)
                _uiState.value = RecipeDetailsUiState.Success(meal, false)
            } else {
                repository.addFavorite(
                    FavoriteRecipe(
                        idMeal = meal.idMeal,
                        strMeal = meal.strMeal,
                        strMealThumb = meal.strMealThumb ?: "",
                        strCategory = meal.strCategory ?: "Recipe",
                        strArea = meal.strArea ?: "International"
                    )
                )
                _uiState.value = RecipeDetailsUiState.Success(meal, true)
            }
        }
    }
}
