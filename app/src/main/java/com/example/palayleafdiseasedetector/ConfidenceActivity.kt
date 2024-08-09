package com.example.palayleafdiseasedetector

import ImageClassifier
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class ConfidenceActivity : AppCompatActivity() {

    private lateinit var imageClassifier: ImageClassifier

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.confidence)

        // Initialize the ImageClassifier
        imageClassifier = ImageClassifier(this)

        // Retrieve the image URI and classification result
        val imageUriString = intent.getStringExtra("imageUri")
        val label = intent.getStringExtra("label")
        val confidence = intent.getFloatExtra("confidence", 0f)

        if (imageUriString != null) {
            val imageUri = Uri.parse(imageUriString)
            val inputStream = contentResolver.openInputStream(imageUri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (label == "PALAY_LEAF") {
                // Apply CLAHE to the palay leaf image
                val claheBitmap = applyCLAHE(bitmap)

                // Display or save the processed image as needed
                // Example: save the image or display it in an ImageView (if you want to see the result)
                // saveBitmap(claheBitmap)
            } else {
                // The image is not a palay leaf, reopen the camera
                openCamera()
            }
        } else {
            // Handle case where imageUri is null (optional error handling)
        }

        // Set up button to retake the photo
        val retakeButton: Button = findViewById(R.id.retakebutton)
        retakeButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish() // Optionally finish ConfidenceActivity if you don't want it in the back stack
        }
    }

    // Function to apply CLAHE to the bitmap (implement CLAHE algorithm here)
    private fun applyCLAHE(bitmap: Bitmap): Bitmap {
        // Implement your CLAHE image processing here
        // Return the processed bitmap
        return bitmap // Placeholder: replace with actual CLAHE implementation
    }

    private fun openCamera() {
        // Implement the logic to reopen the camera
        // This could be similar to the camera opening logic in your MainActivity
    }
}
