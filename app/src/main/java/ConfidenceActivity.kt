package com.example.palayleafdiseasedetector

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import java.io.IOException

class ConfidenceActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.confidence)

        val imageView: ImageView = findViewById(R.id.imageView)

        val imagePath = intent.getStringExtra("imagePath")
        val bitmap = BitmapFactory.decodeFile(imagePath)
        val rotatedBitmap = imagePath?.let { rotateImageIfRequired(bitmap, it) }
        imageView.setImageBitmap(rotatedBitmap)
    }

    private fun rotateImageIfRequired(img: Bitmap, selectedImage: String): Bitmap? {
        val ei = try {
            ExifInterface(selectedImage)
        } catch (e: IOException) {
            e.printStackTrace()
            return img
        }

        val orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED)

        return when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> rotateImage(img, 90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> rotateImage(img, 180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> rotateImage(img, 270f)
            ExifInterface.ORIENTATION_NORMAL -> img
            else -> img
        }
    }

    private fun rotateImage(img: Bitmap, degree: Float): Bitmap? {
        val matrix = Matrix()
        matrix.postRotate(degree)
        return Bitmap.createBitmap(img, 0, 0, img.width, img.height, matrix, true)
    }
}
