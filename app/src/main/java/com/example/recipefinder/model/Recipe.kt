package com.example.recipefinder.model

data class Recipe(
    val id: String,
    val title: String,
    val category: String,
    val cookingTime: String,
    val calories: String,
    val rating: Float,
    val imageUrl: String,
    val ingredients: List<String>,
    var isFavorite: Boolean = false
)
