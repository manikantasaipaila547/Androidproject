package com.example.recipefinder.ui.favourites

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
import com.example.recipefinder.data.model.FavoriteRecipe
import com.example.recipefinder.data.remote.RetrofitClient
import com.example.recipefinder.data.repository.RecipeRepository
import com.example.recipefinder.databinding.ActivityFavouritesBinding
import com.example.recipefinder.databinding.ItemRecipeResultBinding
import com.example.recipefinder.ui.details.RecipeDetailsActivity
import kotlinx.coroutines.launch

class FavouritesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFavouritesBinding
    private lateinit var viewModel: FavouritesViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFavouritesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener {
            finish()
        }

        val db = RecipeDatabase.getInstance(this)
        val repository = RecipeRepository(
            api = RetrofitClient.api,
            favoriteDao = db.favoriteDao(),
            searchHistoryDao = db.searchHistoryDao()
        )
        viewModel = FavouritesViewModel(repository)

        setupStateObservers()
    }

    private fun setupStateObservers() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.favoritesFlow.collect { favorites ->
                    if (favorites.isEmpty()) {
                        binding.layoutEmptyState.visibility = View.VISIBLE
                        binding.rvFavorites.visibility = View.GONE
                    } else {
                        binding.layoutEmptyState.visibility = View.GONE
                        binding.rvFavorites.visibility = View.VISIBLE
                        setupRecyclerView(favorites)
                    }
                }
            }
        }
    }

    private fun setupRecyclerView(favorites: List<FavoriteRecipe>) {
        binding.rvFavorites.adapter = FavoritesAdapter(
            favorites = favorites,
            onFavoriteClick = { recipe ->
                viewModel.removeFavorite(recipe)
                Toast.makeText(this, "Removed from Favourites", Toast.LENGTH_SHORT).show()
            },
            onRecipeClick = { recipe ->
                val intent = Intent(this, RecipeDetailsActivity::class.java).apply {
                    putExtra("MEAL_ID", recipe.idMeal)
                }
                startActivity(intent)
            }
        )
    }

    private class FavoritesAdapter(
        private val favorites: List<FavoriteRecipe>,
        private val onFavoriteClick: (FavoriteRecipe) -> Unit,
        private val onRecipeClick: (FavoriteRecipe) -> Unit
    ) : RecyclerView.Adapter<FavoritesAdapter.ViewHolder>() {

        class ViewHolder(val binding: ItemRecipeResultBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemRecipeResultBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = favorites[position]
            with(holder.binding) {
                tvMealName.text = item.strMeal
                tvMealCategory.text = "${item.strCategory} • ${item.strArea}"
                btnFavorite.setImageResource(R.drawable.ic_heart_filled)

                Glide.with(ivMealThumb.context)
                    .load(item.strMealThumb)
                    .placeholder(R.color.divider)
                    .centerCrop()
                    .into(ivMealThumb)

                btnFavorite.setOnClickListener {
                    onFavoriteClick(item)
                }

                root.setOnClickListener {
                    onRecipeClick(item)
                }
            }
        }

        override fun getItemCount(): Int = favorites.size
    }
}
