package com.example.palayleafdiseasedetector

import ImageClassifier
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ExifInterface
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class ConfidenceActivity : AppCompatActivity() {

    private lateinit var imageClassifier: ImageClassifier

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.confidence)

        // Initialize the ImageClassifier
        imageClassifier = ImageClassifier(this)

        // Retrieve the image URI
        val imageUriString = intent.getStringExtra("imageUri")
        val label = intent.getStringExtra("label")
        val confidence = intent.getFloatExtra("confidence", 0f)

        if (imageUriString != null) {
            val imageUri = Uri.parse(imageUriString)
            val inputStream = contentResolver.openInputStream(imageUri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            val exifInterface = ExifInterface(contentResolver.openInputStream(imageUri)!!)
            val orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)

            val rotatedBitmap = when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> rotateBitmap(bitmap, 90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> rotateBitmap(bitmap, 180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> rotateBitmap(bitmap, 270f)
                else -> bitmap
            }

            if (rotatedBitmap != null) {
                // Process the bitmap
                val result = imageClassifier.classify(rotatedBitmap)

                // Display the image and the classification result
                val imageView: ImageView = findViewById(R.id.imageView)
                val classifiedTextView: TextView = findViewById(R.id.classified)
                val resultTextView: TextView = findViewById(R.id.result)
                val confidencesTextView: TextView = findViewById(R.id.confidencesText)
                val confidenceTextView: TextView = findViewById(R.id.confidence)

                imageView.setImageBitmap(rotatedBitmap)
                classifiedTextView.text = "Classified as:"
                resultTextView.text = "Label: $label"
                confidenceTextView.text = "Confidence:"
                confidencesTextView.text = "$confidence"
            } else {
                // Handle case where bitmap is null
                val classifiedTextView: TextView = findViewById(R.id.classified)
                val resultTextView: TextView = findViewById(R.id.result)

                classifiedTextView.text = "No image available for classification."
                resultTextView.text = ""
            }
        } else {
            // Handle case where imageUri is null
            val classifiedTextView: TextView = findViewById(R.id.classified)
            val resultTextView: TextView = findViewById(R.id.result)

            classifiedTextView.text = "No image available for classification."
            resultTextView.text = ""
        }

        // Set up button to retake the photo
        val retakeButton: Button = findViewById(R.id.retakebutton)
        retakeButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish() // Optionally finish ConfidenceActivity if you don't want it in the back stack
        }
    }

    private fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = android.graphics.Matrix().apply {
            postRotate(degrees)
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}


    private fun decodeSampledBitmapFromFile(filePath: String, reqWidth: Int, reqHeight: Int): Bitmap? {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
            BitmapFactory.decodeFile(filePath, this)
            inSampleSize = calculateInSampleSize(this, reqWidth, reqHeight)
            inJustDecodeBounds = false
            inPreferredConfig = Bitmap.Config.RGB_565  // Reduce memory usage
        }
        return BitmapFactory.decodeFile(filePath, options)
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.run { outHeight to outWidth }
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

