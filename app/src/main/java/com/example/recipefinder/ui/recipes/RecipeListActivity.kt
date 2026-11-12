package com.example.recipefinder.ui.recipes

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.recipefinder.R
import com.example.recipefinder.data.local.RecipeDatabase
import com.example.recipefinder.data.model.MealSummary
import com.example.recipefinder.data.remote.RetrofitClient
import com.example.recipefinder.data.repository.RecipeRepository
import com.example.recipefinder.databinding.ActivityRecipeListBinding
import com.example.recipefinder.databinding.ItemRecipeResultBinding
import com.example.recipefinder.ui.details.RecipeDetailsActivity
import kotlinx.coroutines.launch

class RecipeListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRecipeListBinding
    private lateinit var viewModel: RecipeListViewModel
    private var searchQuery: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRecipeListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        searchQuery = intent.getStringExtra("SEARCH_INGREDIENT")
            ?: intent.getStringExtra("SEARCH_NAME")
            ?: "Tomato"

        binding.tvHeaderTitle.text = "Recipes with '$searchQuery'"

        binding.btnBack.setOnClickListener {
            finish()
        }

        val db = RecipeDatabase.getInstance(this)
        val repository = RecipeRepository(
            api = RetrofitClient.api,
            favoriteDao = db.favoriteDao(),
            searchHistoryDao = db.searchHistoryDao()
        )
        viewModel = RecipeListViewModel(repository)

        setupStateObservers()
        viewModel.searchByIngredient(searchQuery)
    }

    private fun setupStateObservers() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is RecipeListUiState.Loading -> {
                            binding.pbLoading.visibility = View.VISIBLE
                            binding.rvRecipes.visibility = View.GONE
                            binding.layoutEmptyState.visibility = View.GONE
                        }
                        is RecipeListUiState.Success -> {
                            binding.pbLoading.visibility = View.GONE
                            binding.layoutEmptyState.visibility = View.GONE
                            binding.rvRecipes.visibility = View.VISIBLE
                            setupRecyclerView(state.meals)
                        }
                        is RecipeListUiState.Empty -> {
                            binding.pbLoading.visibility = View.GONE
                            binding.rvRecipes.visibility = View.GONE
                            binding.layoutEmptyState.visibility = View.VISIBLE
                        }
                        is RecipeListUiState.Error -> {
                            binding.pbLoading.visibility = View.GONE
                            binding.rvRecipes.visibility = View.GONE
                            binding.layoutEmptyState.visibility = View.VISIBLE
                            binding.tvEmptyTitle.text = "Error Loading Recipes"
                            binding.tvEmptyMessage.text = state.message
                        }
                    }
                }
            }
        }
    }

    private fun setupRecyclerView(meals: List<MealSummary>) {
        binding.rvRecipes.adapter = ResultAdapter(
            meals = meals,
            onFavoriteClick = { meal, isFav ->
                viewModel.toggleFavorite(meal, isFav)
                val status = if (isFav) "Removed from Favourites" else "Saved to Favourites ❤️"
                Toast.makeText(this, status, Toast.LENGTH_SHORT).show()
            },
            onMealClick = { meal ->
                val intent = Intent(this, RecipeDetailsActivity::class.java).apply {
                    putExtra("MEAL_ID", meal.idMeal)
                }
                startActivity(intent)
            }
        )
    }

    private class ResultAdapter(
        private val meals: List<MealSummary>,
        private val onFavoriteClick: (MealSummary, Boolean) -> Unit,
        private val onMealClick: (MealSummary) -> Unit
    ) : RecyclerView.Adapter<ResultAdapter.ViewHolder>() {

        class ViewHolder(val binding: ItemRecipeResultBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemRecipeResultBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val meal = meals[position]
            var isFav = false
            with(holder.binding) {
                tvMealName.text = meal.strMeal
                tvMealCategory.text = "THEMEALDB RECIPE"

                Glide.with(ivMealThumb.context)
                    .load(meal.strMealThumb)
                    .placeholder(R.color.divider)
                    .centerCrop()
                    .into(ivMealThumb)

                btnFavorite.setOnClickListener {
                    isFav = !isFav
                    btnFavorite.setImageResource(
                        if (isFav) R.drawable.ic_heart_filled else R.drawable.ic_heart_outline
                    )
                    onFavoriteClick(meal, !isFav)
                }

                root.setOnClickListener {
                    onMealClick(meal)
                }
            }
        }

        override fun getItemCount(): Int = meals.size
    }
}
