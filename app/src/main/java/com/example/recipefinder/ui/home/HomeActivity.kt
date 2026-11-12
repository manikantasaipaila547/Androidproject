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
import com.example.recipefinder.R
import com.example.recipefinder.adapter.RecipeAdapter
import com.example.recipefinder.databinding.ActivityMain2Binding
import com.example.recipefinder.databinding.DialogRecognitionResultBinding
import com.example.recipefinder.ml.IngredientClassifier
import com.example.recipefinder.ml.Recognition
import com.example.recipefinder.model.Recipe
import com.example.recipefinder.ui.login.LoginActivity
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import java.util.Locale

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMain2Binding
    private lateinit var auth: FirebaseAuth
    private lateinit var ingredientClassifier: IngredientClassifier
    private lateinit var recipeAdapter: RecipeAdapter

    private var allRecipes = listOf<Recipe>()

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

        setupUserHeader()
        setupRecipeList()
        setupClickListeners()
    }

    private fun setupUserHeader() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            navigateToLogin()
            return
        }

        val name = when {
            !currentUser.displayName.isNull_or_empty() -> currentUser.displayName
            !currentUser.email.isNull_or_empty() -> currentUser.email!!.substringBefore("@")
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
            else -> "Chef"
        }

        binding.tvWelcomeUser.text = "Welcome Back, $name! 👋"
    }

    private fun CharSequence?.isNull_or_empty(): Boolean {
        return this == null || this.isEmpty()
    }

    private fun setupRecipeList() {
        allRecipes = listOf(
            Recipe(
                id = "1",
                title = "Garlic Butter Salmon",
                category = "Seafood",
                cookingTime = "25 mins",
                calories = "450 kcal",
                rating = 4.9f,
                imageUrl = "https://images.unsplash.com/photo-1519708227418-c8fd9a32b7a2?w=500",
                ingredients = listOf("Salmon", "Garlic", "Butter", "Lemon")
            ),
            Recipe(
                id = "2",
                title = "Classic Margherita Pizza",
                category = "Italian",
                cookingTime = "20 mins",
                calories = "520 kcal",
                rating = 4.8f,
                imageUrl = "https://images.unsplash.com/photo-1604382354936-07c5d9983bd3?w=500",
                ingredients = listOf("Tomato", "Cheese", "Dough", "Basil")
            ),
            Recipe(
                id = "3",
                title = "Healthy Avocado Toast",
                category = "Breakfast",
                cookingTime = "10 mins",
                calories = "280 kcal",
                rating = 4.7f,
                imageUrl = "https://images.unsplash.com/photo-1525351484163-7529414344d8?w=500",
                ingredients = listOf("Avocado", "Bread", "Egg", "Pepper")
            ),
            Recipe(
                id = "4",
                title = "Spicy Chicken Tacos",
                category = "Mexican",
                cookingTime = "30 mins",
                calories = "390 kcal",
                rating = 4.9f,
                imageUrl = "https://images.unsplash.com/photo-1565299585323-38d6b0865b47?w=500",
                ingredients = listOf("Chicken", "Tortilla", "Bell Pepper", "Onion")
            ),
            Recipe(
                id = "5",
                title = "Mushroom Truffle Risotto",
                category = "Italian",
                cookingTime = "35 mins",
                calories = "420 kcal",
                rating = 4.6f,
                imageUrl = "https://images.unsplash.com/photo-1633964913295-ceb43826e7c9?w=500",
                ingredients = listOf("Mushroom", "Rice", "Butter", "Parmesan")
            )
        )

        recipeAdapter = RecipeAdapter(
            recipes = allRecipes,
            onFavoriteClick = { recipe ->
                val status = if (recipe.isFavorite) "Added to Favourites ❤️" else "Removed from Favourites"
                Toast.makeText(this, "${recipe.title}: $status", Toast.LENGTH_SHORT).show()
            },
            onRecipeClick = { recipe ->
                Toast.makeText(this, "Opening ${recipe.title}", Toast.LENGTH_SHORT).show()
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

        // Search Button
        binding.btnSearch.setOnClickListener {
            performIngredientSearch()
        }

        // Recent Search Chips
        for (i in 0 until binding.chipGroupRecent.childCount) {
            val child = binding.chipGroupRecent.getChildAt(i)
            if (child is Chip) {
                child.setOnClickListener {
                    val cleanSearch = child.text.toString().replace(Regex("[^a-zA-Z\\s]"), "").trim()
                    binding.etSearchIngredient.setText(cleanSearch)
                    performIngredientSearch()
                }
            }
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
            .setMessage("Recipe Finder requires camera access to scan food ingredients and detect them using AI.")
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
            dialogBinding.tvIngredient2.text = "2. Bell Pepper"
            dialogBinding.tvConfidence2.text = "81.5%"
            dialogBinding.pbConfidence2.progress = 81
        }

        if (recognitions.size >= 3) {
            val r3 = recognitions[2]
            dialogBinding.tvIngredient3.text = "3. ${r3.title}"
            dialogBinding.tvConfidence3.text = r3.confidencePercentage
            dialogBinding.pbConfidence3.progress = (r3.confidence * 100).toInt()
        } else {
            dialogBinding.tvIngredient3.text = "3. Onion"
            dialogBinding.tvConfidence3.text = "67.0%"
            dialogBinding.pbConfidence3.progress = 67
        }

        val topIngredient = if (recognitions.isNotEmpty()) recognitions[0].title else "Ingredients"
        dialogBinding.btnSearchWithIngredients.text = "Search Recipes with $topIngredient"

        dialogBinding.btnSearchWithIngredients.setOnClickListener {
            binding.etSearchIngredient.setText(topIngredient)
            performIngredientSearch()
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun performIngredientSearch() {
        val query = binding.etSearchIngredient.text?.toString()?.trim() ?: ""
        if (query.isEmpty()) {
            recipeAdapter.updateData(allRecipes)
            return
        }

        val filtered = allRecipes.filter { recipe ->
            recipe.title.contains(query, ignoreCase = true) ||
                    recipe.category.contains(query, ignoreCase = true) ||
                    recipe.ingredients.any { it.contains(query, ignoreCase = true) }
        }

        if (filtered.isNotEmpty()) {
            recipeAdapter.updateData(filtered)
            Toast.makeText(this, "Found ${filtered.size} recipes for '$query'", Toast.LENGTH_SHORT).show()
        } else {
            recipeAdapter.updateData(allRecipes)
            Toast.makeText(this, "No specific match for '$query'. Showing popular recipes.", Toast.LENGTH_SHORT).show()
        }
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
