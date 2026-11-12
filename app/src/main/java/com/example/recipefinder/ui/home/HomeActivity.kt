package com.example.recipefinder.ui.home

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.recipefinder.R
import com.example.recipefinder.adapter.RecipeAdapter
import com.example.recipefinder.data.local.RecipeDatabase
import com.example.recipefinder.data.model.FavoriteRecipe
import com.example.recipefinder.data.remote.RetrofitClient
import com.example.recipefinder.data.repository.RecipeRepository
import com.example.recipefinder.databinding.ActivityMain2Binding
import com.example.recipefinder.databinding.DialogRecognitionResultBinding
import com.example.recipefinder.ml.IngredientClassifier
import com.example.recipefinder.ml.Recognition
import com.example.recipefinder.model.Recipe
import com.example.recipefinder.ui.favourites.FavouritesActivity
import com.example.recipefinder.ui.login.LoginActivity
import com.example.recipefinder.ui.receipt.ReceiptActivity
import com.example.recipefinder.ui.recipes.RecipeListActivity
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.util.Locale

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMain2Binding
    private lateinit var auth: FirebaseAuth
    private lateinit var ingredientClassifier: IngredientClassifier
    private lateinit var recipeAdapter: RecipeAdapter
    private lateinit var viewModel: HomeViewModel

    private var defaultRecipes = listOf<Recipe>()

    // Camera Result Launcher
    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            processRecognizedImage(bitmap)
        } else {
            Toast.makeText(this, "No photo captured", Toast.LENGTH_SHORT).show()
        }
    }

    // Gallery Result Launcher
    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    ImageDecoder.decodeBitmap(ImageDecoder.createSource(contentResolver, uri))
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(contentResolver, uri)
                }
                processRecognizedImage(bitmap)
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Failed to load image from gallery", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Camera Permission Launcher
    private val requestCameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            cameraLauncher.launch(null)
        } else {
            showSettingsPermissionDialog()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMain2Binding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        ingredientClassifier = IngredientClassifier(this)

        val db = RecipeDatabase.getInstance(this)
        val repository = RecipeRepository(
            api = RetrofitClient.api,
            favoriteDao = db.favoriteDao(),
            searchHistoryDao = db.searchHistoryDao()
        )
        viewModel = HomeViewModel(repository)

        setupUserHeader()
        setupRecipeList()
        setupClickListeners()
        setupRecentSearchObservers()
    }

    private fun setupUserHeader() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            navigateToLogin()
            return
        }

        val name = when {
            !currentUser.displayName.isNullOrEmpty() -> currentUser.displayName
            !currentUser.email.isNullOrEmpty() -> currentUser.email!!.substringBefore("@")
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
            else -> "Chef"
        }

        binding.tvWelcomeUser.text = "Welcome Back, $name! 👋"
    }

    private fun setupRecentSearchObservers() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.recentSearches.collect { searches ->
                    if (searches.isNotEmpty()) {
                        binding.chipGroupRecent.removeAllViews()
                        for (search in searches) {
                            val chip = Chip(this@HomeActivity).apply {
                                text = "🔍 ${search.query}"
                                isClickable = true
                                setChipBackgroundColorResource(R.color.background_light)
                                setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                                setOnClickListener {
                                    binding.etSearchIngredient.setText(search.query)
                                    performIngredientSearch()
                                }
                            }
                            binding.chipGroupRecent.addView(chip)
                        }
                    }
                }
            }
        }
    }

    private fun setupRecipeList() {
        defaultRecipes = listOf(
            Recipe(
                id = "52772",
                title = "Teriyaki Chicken Casserole",
                category = "Chicken",
                cookingTime = "25 mins",
                calories = "450 kcal",
                rating = 4.9f,
                imageUrl = "https://www.themealdb.com/images/media/meals/wvpsxx1468256321.jpg",
                ingredients = listOf("Chicken", "Soy Sauce", "Rice")
            ),
            Recipe(
                id = "52882",
                title = "Three Fish Pie",
                category = "Seafood",
                cookingTime = "35 mins",
                calories = "520 kcal",
                rating = 4.8f,
                imageUrl = "https://www.themealdb.com/images/media/meals/spswwy1511723449.jpg",
                ingredients = listOf("Fish", "Potatoes", "Butter")
            ),
            Recipe(
                id = "52977",
                title = "Corba Lentil Soup",
                category = "Soup",
                cookingTime = "20 mins",
                calories = "280 kcal",
                rating = 4.7f,
                imageUrl = "https://www.themealdb.com/images/media/meals/58o10a1562512614.jpg",
                ingredients = listOf("Lentils", "Onion", "Carrot")
            )
        )

        recipeAdapter = RecipeAdapter(
            recipes = defaultRecipes,
            onFavoriteClick = { recipe ->
                viewModel.removeFavorite(
                    FavoriteRecipe(
                        idMeal = recipe.id,
                        strMeal = recipe.title,
                        strMealThumb = recipe.imageUrl,
                        strCategory = recipe.category,
                        strArea = "International"
                    )
                )
            },
            onRecipeClick = { recipe ->
                val intent = Intent(this, com.example.recipefinder.ui.details.RecipeDetailsActivity::class.java).apply {
                    putExtra("MEAL_ID", recipe.id)
                }
                startActivity(intent)
            }
        )

        binding.rvFavouriteRecipes.adapter = recipeAdapter
    }

    private fun setupClickListeners() {
        // Logout Button
        binding.btnLogout.setOnClickListener {
            auth.signOut()
            Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()
            navigateToLogin()
        }

        // Camera Button
        binding.btnCamera.setOnClickListener {
            checkCameraPermissionAndLaunch()
        }

        // Gallery Button
        binding.btnGallery.setOnClickListener {
            galleryLauncher.launch("image/*")
        }

        // Receipt Scanner Card
        binding.cardReceiptScanner.setOnClickListener {
            startActivity(Intent(this, ReceiptActivity::class.java))
        }

        // Favourites See All Button
        binding.tvSeeAll.setOnClickListener {
            startActivity(Intent(this, FavouritesActivity::class.java))
        }

        // Search Button
        binding.btnSearch.setOnClickListener {
            performIngredientSearch()
        }
    }

    private fun checkCameraPermissionAndLaunch() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                cameraLauncher.launch(null)
            }
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                showCameraPermissionRationaleDialog()
            }
            else -> {
                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun showCameraPermissionRationaleDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Camera Permission Needed")
            .setMessage("Recipe Finder uses your camera to take photos of food ingredients and detect them using AI.")
            .setPositiveButton("Grant Permission") { _, _ ->
                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showSettingsPermissionDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Camera Permission Required")
            .setMessage("Camera permission is required to capture ingredient photos. Please enable Camera permission in application settings.")
            .setPositiveButton("Open Settings") { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", packageName, null)
                }
                startActivity(intent)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun processRecognizedImage(bitmap: Bitmap) {
        val recognitions = ingredientClassifier.classifyImage(bitmap)
        showRecognitionResultsDialog(bitmap, recognitions)
    }

    private fun showRecognitionResultsDialog(bitmap: Bitmap, recognitions: List<Recognition>) {
        val dialog = BottomSheetDialog(this, R.style.Theme_RecipeFinder)
        val dialogBinding = DialogRecognitionResultBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)

        dialogBinding.ivCapturedPreview.setImageBitmap(bitmap)

        if (recognitions.isEmpty()) {
            dialogBinding.tvIngredient1.text = "Ingredient Not Recognized"
            dialogBinding.tvConfidence1.text = "Low Confidence"
            dialogBinding.pbConfidence1.progress = 0

            dialogBinding.tvIngredient2.text = "Please ensure food is clearly visible"
            dialogBinding.tvConfidence2.text = ""
            dialogBinding.pbConfidence2.progress = 0

            dialogBinding.tvIngredient3.text = "Or search manually using the search bar"
            dialogBinding.tvConfidence3.text = ""
            dialogBinding.pbConfidence3.progress = 0

            dialogBinding.btnSearchWithIngredients.text = "Try Another Photo"
            dialogBinding.btnSearchWithIngredients.setOnClickListener {
                dialog.dismiss()
                checkCameraPermissionAndLaunch()
            }
            dialog.show()
            return
        }

        if (recognitions.size >= 1) {
            val r1 = recognitions[0]
            dialogBinding.tvIngredient1.text = "1. ${r1.title}"
            dialogBinding.tvConfidence1.text = r1.confidencePercentage
            dialogBinding.pbConfidence1.progress = (r1.confidence * 100).toInt()
        }

        if (recognitions.size >= 2) {
            val r2 = recognitions[1]
            dialogBinding.tvIngredient2.text = "2. ${r2.title}"
            dialogBinding.tvConfidence2.text = r2.confidencePercentage
            dialogBinding.pbConfidence2.progress = (r2.confidence * 100).toInt()
        } else {
            dialogBinding.tvIngredient2.visibility = View.GONE
            dialogBinding.tvConfidence2.visibility = View.GONE
            dialogBinding.pbConfidence2.visibility = View.GONE
        }

        if (recognitions.size >= 3) {
            val r3 = recognitions[2]
            dialogBinding.tvIngredient3.text = "3. ${r3.title}"
            dialogBinding.tvConfidence3.text = r3.confidencePercentage
            dialogBinding.pbConfidence3.progress = (r3.confidence * 100).toInt()
        } else {
            dialogBinding.tvIngredient3.visibility = View.GONE
            dialogBinding.tvConfidence3.visibility = View.GONE
            dialogBinding.pbConfidence3.visibility = View.GONE
        }

        val topIngredient = recognitions[0].title
        dialogBinding.btnSearchWithIngredients.text = "Search Recipes for '$topIngredient'"

        dialogBinding.btnSearchWithIngredients.setOnClickListener {
            viewModel.saveSearch(topIngredient)
            val intent = Intent(this, RecipeListActivity::class.java).apply {
                putExtra("SEARCH_INGREDIENT", topIngredient)
            }
            startActivity(intent)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun performIngredientSearch() {
        val query = binding.etSearchIngredient.text?.toString()?.trim() ?: ""
        if (query.isEmpty()) {
            Toast.makeText(this, "Please enter an ingredient to search", Toast.LENGTH_SHORT).show()
            return
        }

        viewModel.saveSearch(query)
        val intent = Intent(this, RecipeListActivity::class.java).apply {
            putExtra("SEARCH_INGREDIENT", query)
        }
        startActivity(intent)
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        ingredientClassifier.close()
    }
}
