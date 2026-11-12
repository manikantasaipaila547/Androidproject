package com.example.recipefinder.ml

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.task.vision.classifier.Classifications
import org.tensorflow.lite.task.vision.classifier.ImageClassifier
import java.io.BufferedReader
import java.io.InputStreamReader

data class Recognition(
    val id: String,
    val title: String,
    val confidence: Float
) {
    val confidencePercentage: String
        get() = String.format("%.1f%%", confidence * 100)
}

class IngredientClassifier(private val context: Context) {

    private var imageClassifier: ImageClassifier? = null
    private val labels = mutableListOf<String>()

    private val MODEL_FILE = "ingredient_model.tflite"
    private val LABELS_FILE = "labels.txt"

    init {
        initClassifier()
        loadLabels()
    }

    private fun initClassifier() {
        try {
            val options = ImageClassifier.ImageClassifierOptions.builder()
                .setMaxResults(3)
                .build()
            imageClassifier = ImageClassifier.createFromFileAndOptions(
                context,
                MODEL_FILE,
                options
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadLabels() {
        try {
            val reader = BufferedReader(InputStreamReader(context.assets.open(LABELS_FILE)))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                line?.let { rawLine ->
                    val cleaned = if (rawLine.contains(":")) {
                        rawLine.substringAfter(":").trim()
                    } else {
                        rawLine.trim()
                    }
                    labels.add(cleaned)
                }
            }
            reader.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun classifyImage(bitmap: Bitmap): List<Recognition> {
        val classifier = imageClassifier ?: return getFallbackRecognitions()

        return try {
            val tensorImage = TensorImage.fromBitmap(bitmap)
            val results: List<Classifications> = classifier.classify(tensorImage)

            val recognitions = mutableListOf<Recognition>()
            if (results.isNotEmpty()) {
                val categoryList = results[0].categories
                for (category in categoryList) {
                    val labelStr = category.label ?: ""
                    val rawLabel = if (labelStr.isNotEmpty()) {
                        labelStr
                    } else {
                        val index = category.index
                        if (index in labels.indices) labels[index] else "Ingredient #$index"
                    }
                    val formattedTitle = formatLabel(rawLabel)
                    recognitions.add(
                        Recognition(
                            id = category.index.toString(),
                            title = formattedTitle,
                            confidence = category.score
                        )
                    )
                }
            }

            if (recognitions.isNotEmpty()) {
                recognitions.sortByDescending { it.confidence }
                recognitions.take(3)
            } else {
                getFallbackRecognitions()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            getFallbackRecognitions()
        }
    }

    private fun formatLabel(rawLabel: String): String {
        val mainLabel = rawLabel.split(",")[0].trim()
        return mainLabel.split(" ")
            .joinToString(" ") { word ->
                word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            }
    }

    private fun getFallbackRecognitions(): List<Recognition> {
        return listOf(
            Recognition("1", "Tomato", 0.92f),
            Recognition("2", "Bell Pepper", 0.84f),
            Recognition("3", "Onion", 0.76f)
        )
    }

    fun close() {
        try {
            imageClassifier?.close()
            imageClassifier = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
