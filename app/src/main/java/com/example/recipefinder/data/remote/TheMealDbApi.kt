package com.example.recipefinder.data.remote

import com.example.recipefinder.data.model.MealDetailResponse
import com.example.recipefinder.data.model.MealFilterResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface TheMealDbApi {

    @GET("filter.php")
    suspend fun filterByIngredient(
        @Query("i") ingredient: String
    ): Response<MealFilterResponse>

    @GET("search.php")
    suspend fun searchByName(
        @Query("s") name: String
    ): Response<MealDetailResponse>

    @GET("lookup.php")
    suspend fun getRecipeDetails(
        @Query("i") id: String
    ): Response<MealDetailResponse>
}
