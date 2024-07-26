package com.example.palayleafdiseasedetector

import ImageClassifier
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
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

        // Retrieve the image path and process it
        val imagePath = intent.getStringExtra("imagePath")
        if (imagePath != null) {
            val bitmap = decodeSampledBitmapFromFile(imagePath, 244, 244)
            if (bitmap != null) {
                try {
                    val result = imageClassifier.classify(bitmap)

                    // Display the image and the classification result
                    val imageView: ImageView = findViewById(R.id.imageView)
                    val classifiedTextView: TextView = findViewById(R.id.classified)
                    val resultTextView: TextView = findViewById(R.id.result)
                    val confidencesTextView: TextView = findViewById(R.id.confidencesText)
                    val confidenceTextView: TextView = findViewById(R.id.confidence)

                    imageView.setImageBitmap(bitmap)
                    classifiedTextView.text = "Classified as:"
                    resultTextView.text = "Label: ${result.first}"
                    confidenceTextView.text = "Confidence:"
                    confidencesTextView.text = "${result.second}"
                } finally {
                    // Recycle the bitmap to free up memory
                    bitmap.recycle()
                }
            } else {
                // Handle case where bitmap is null after decoding
                val classifiedTextView: TextView = findViewById(R.id.classified)
                val resultTextView: TextView = findViewById(R.id.result)

                classifiedTextView.text = "Failed to decode image."
                resultTextView.text = ""
            }
        } else {
            // Handle case where imagePath is null
            val classifiedTextView: TextView = findViewById(R.id.classified)
            val resultTextView: TextView = findViewById(R.id.result)

            classifiedTextView.text = "No image available for classification."
            resultTextView.text = ""
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
}
