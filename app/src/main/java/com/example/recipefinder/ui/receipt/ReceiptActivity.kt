package com.example.recipefinder.ui.receipt

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.recipefinder.R
import com.example.recipefinder.databinding.ActivityReceiptBinding
import com.example.recipefinder.ui.recipes.RecipeListActivity
import com.google.android.material.chip.Chip
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

class ReceiptActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReceiptBinding

    // Camera Result Launcher
    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            processReceiptImage(bitmap)
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
                processReceiptImage(bitmap)
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
            Toast.makeText(this, "Camera permission required for receipts", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReceiptBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnSnapReceipt.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                cameraLauncher.launch(null)
            } else {
                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }

        binding.btnUploadReceipt.setOnClickListener {
            galleryLauncher.launch("image/*")
        }
    }

    private fun processReceiptImage(bitmap: Bitmap) {
        binding.cardPreview.visibility = View.VISIBLE
        binding.ivReceiptPreview.setImageBitmap(bitmap)
        binding.pbLoading.visibility = View.VISIBLE

        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val image = InputImage.fromBitmap(bitmap, 0)

        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                binding.pbLoading.visibility = View.GONE
                val rawText = visionText.text

                if (rawText.isBlank()) {
                    binding.tvRawText.text = "No readable text detected in receipt."
                    Toast.makeText(this, "No text detected in receipt", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                binding.tvRawText.text = rawText

                // Extract lines as items
                val lines = visionText.textBlocks.flatMap { block ->
                    block.lines.map { it.text.trim() }
                }.filter { line ->
                    line.isNotBlank() && line.length > 2 && !line.matches(Regex("^[0-9.\\s$€£]+$"))
                }.distinct()

                populateExtractedChips(lines)
            }
            .addOnFailureListener { e ->
                binding.pbLoading.visibility = View.GONE
                e.printStackTrace()
                Toast.makeText(this, "Failed to extract text: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun populateExtractedChips(items: List<String>) {
        binding.chipGroupExtractedItems.removeAllViews()

        if (items.isEmpty()) {
            val chip = Chip(this).apply {
                text = "No specific food items detected"
                isClickable = false
            }
            binding.chipGroupExtractedItems.addView(chip)
            return
        }

        for (item in items.take(15)) {
            // Clean quantity/price numbers from item name if present
            val cleanItem = item.replace(Regex("[$€£0-9.,xX]"), "").trim()
            if (cleanItem.length < 2) continue

            val chip = Chip(this).apply {
                text = cleanItem
                isCheckable = false
                isClickable = true
                setChipBackgroundColorResource(R.color.background_light)
                setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                setOnClickListener {
                    val intent = Intent(this@ReceiptActivity, RecipeListActivity::class.java).apply {
                        putExtra("SEARCH_INGREDIENT", cleanItem)
                    }
                    startActivity(intent)
                }
            }
            binding.chipGroupExtractedItems.addView(chip)
        }
    }
}
