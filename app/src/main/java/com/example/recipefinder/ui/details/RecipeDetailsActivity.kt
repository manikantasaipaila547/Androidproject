package com.example.recipefinder.ui.details

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bumptech.glide.Glide
import com.example.recipefinder.R
import com.example.recipefinder.data.local.RecipeDatabase
import com.example.recipefinder.data.model.MealDetail
import com.example.recipefinder.data.remote.RetrofitClient
import com.example.recipefinder.data.repository.RecipeRepository
import com.example.recipefinder.databinding.ActivityRecipeDetailsBinding
import kotlinx.coroutines.launch

class RecipeDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRecipeDetailsBinding
    private lateinit var viewModel: RecipeDetailsViewModel
    private var mealId: String = ""
    private var currentMeal: MealDetail? = null
    private var isFavoriteState: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRecipeDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mealId = intent.getStringExtra("MEAL_ID") ?: "52772"

        binding.btnBack.setOnClickListener {
            finish()
        }

        val db = RecipeDatabase.getInstance(this)
        val repository = RecipeRepository(
            api = RetrofitClient.api,
            favoriteDao = db.favoriteDao(),
            searchHistoryDao = db.searchHistoryDao()
        )
        viewModel = RecipeDetailsViewModel(repository)

        setupStateObservers()
        viewModel.loadRecipeDetails(mealId)
    }

    private fun setupStateObservers() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is RecipeDetailsUiState.Loading -> {
                            binding.pbLoading.visibility = View.VISIBLE
                            binding.layoutDetailsContent.visibility = View.GONE
                        }
                        is RecipeDetailsUiState.Success -> {
                            binding.pbLoading.visibility = View.GONE
                            binding.layoutDetailsContent.visibility = View.VISIBLE
                            currentMeal = state.mealDetail
                            isFavoriteState = state.isFavorite
                            bindMealData(state.mealDetail, state.isFavorite)
                        }
                        is RecipeDetailsUiState.Error -> {
                            binding.pbLoading.visibility = View.GONE
                            binding.layoutDetailsContent.visibility = View.GONE
                            Toast.makeText(this@RecipeDetailsActivity, state.message, Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    }

    private fun bindMealData(meal: MealDetail, isFav: Boolean) {
        binding.tvDetailTitle.text = meal.strMeal
        binding.tvCategoryBadge.text = meal.strCategory ?: "Recipe"
        binding.tvAreaBadge.text = meal.strArea ?: "International"

        Glide.with(this)
            .load(meal.strMealThumb)
            .placeholder(R.color.divider)
            .into(binding.ivDetailImage)

        // Favorite icon state
        binding.btnDetailFavorite.setImageResource(
            if (isFav) R.drawable.ic_heart_filled else R.drawable.ic_heart_outline
        )

        binding.btnDetailFavorite.setOnClickListener {
            viewModel.toggleFavorite(meal, isFavoriteState)
            val toastMsg = if (isFavoriteState) "Removed from Favourites" else "Saved to Favourites ❤️"
            Toast.makeText(this, toastMsg, Toast.LENGTH_SHORT).show()
        }

        // Formatted Ingredients List
        val ingredientsList = meal.getFormattedIngredientsList()
        val ingredientsText = ingredientsList.joinToString("\n") { (ing, measure) ->
            "• $ing — $measure"
        }
        binding.tvIngredientsList.text = if (ingredientsText.isNotBlank()) ingredientsText else "Ingredients not listed."

        // Instructions
        binding.tvInstructions.text = meal.strInstructions ?: "Instructions not available."

        // YouTube Tutorial Button
        if (!meal.strYoutube.isNull_or_empty()) {
            binding.btnYoutube.visibility = View.VISIBLE
            binding.btnYoutube.setOnClickListener {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(meal.strYoutube))
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(this, "Unable to open YouTube link", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            binding.btnYoutube.visibility = View.GONE
        }
    }

    private fun CharSequence?.isNull_or_empty(): Boolean {
        return this == null || this.isEmpty()
    }
}
