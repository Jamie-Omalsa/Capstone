package com.example.palayleafdiseasedetector

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class ConfidenceActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.confidence)

        val imageView: ImageView = findViewById(R.id.imageView)

        val byteArray = intent.getByteArrayExtra("imageBitmap")
        if (byteArray != null) {
            val bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
            imageView.setImageBitmap(bitmap)
            Log.d("ConfidenceActivity", "Image displayed successfully")
        } else {
            Log.e("ConfidenceActivity", "Failed to receive image data")
            Toast.makeText(this, "Failed to receive image data", Toast.LENGTH_SHORT).show()
        }
    }
}
