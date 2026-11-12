package com.example.recipefinder.ui.favourites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.recipefinder.data.model.FavoriteRecipe
import com.example.recipefinder.data.repository.RecipeRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FavouritesViewModel(private val repository: RecipeRepository) : ViewModel() {

    val favoritesFlow: StateFlow<List<FavoriteRecipe>> = repository.getAllFavorites()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun removeFavorite(recipe: FavoriteRecipe) {
        viewModelScope.launch {
            repository.removeFavorite(recipe)
        }
    }
}
