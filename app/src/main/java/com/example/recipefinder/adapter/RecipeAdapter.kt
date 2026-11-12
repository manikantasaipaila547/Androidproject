package com.example.recipefinder.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.recipefinder.R
import com.example.recipefinder.databinding.ItemRecipeCardBinding
import com.example.recipefinder.model.Recipe

class RecipeAdapter(
    private var recipes: List<Recipe>,
    private val onFavoriteClick: (Recipe) -> Unit,
    private val onRecipeClick: (Recipe) -> Unit
) : RecyclerView.Adapter<RecipeAdapter.RecipeViewHolder>() {

    inner class RecipeViewHolder(val binding: ItemRecipeCardBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecipeViewHolder {
        val binding = ItemRecipeCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return RecipeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RecipeViewHolder, position: Int) {
        val recipe = recipes[position]
        with(holder.binding) {
            tvRecipeTitle.text = recipe.title
            tvCategory.text = recipe.category.uppercase()
            tvTime.text = "⏱ ${recipe.cookingTime}"
            tvCalories.text = "🔥 ${recipe.calories}"
            tvRating.text = "★ ${recipe.rating}"

            if (recipe.isFavorite) {
                btnFavorite.setImageResource(R.drawable.ic_heart_filled)
            } else {
                btnFavorite.setImageResource(R.drawable.ic_heart_outline)
            }

            Glide.with(ivRecipeImage.context)
                .load(recipe.imageUrl)
                .placeholder(R.color.divider)
                .error(R.color.primary_light)
                .centerCrop()
                .into(ivRecipeImage)

            btnFavorite.setOnClickListener {
                recipe.isFavorite = !recipe.isFavorite
                notifyItemChanged(position)
                onFavoriteClick(recipe)
            }

            root.setOnClickListener {
                onRecipeClick(recipe)
            }
        }
    }

    override fun getItemCount(): Int = recipes.size

    fun updateData(newRecipes: List<Recipe>) {
        recipes = newRecipes
        notifyDataSetChanged()
    }
}
