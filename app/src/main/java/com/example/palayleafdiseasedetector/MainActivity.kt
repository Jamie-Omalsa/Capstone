package com.example.palayleafdiseasedetector

import ImageClassifier
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val REQUEST_CAMERA_PERMISSION = 1
private const val REQUEST_IMAGE_CAPTURE = 2

class MainActivity : AppCompatActivity() {

    private lateinit var currentPhotoURI: Uri

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val subukanButton: Button = findViewById(R.id.subukan)

        subukanButton.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), REQUEST_CAMERA_PERMISSION)
            } else {
                openCamera()
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                openCamera()
            } else {
                // Permission denied
                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), REQUEST_CAMERA_PERMISSION)
        } else {
            dispatchTakePictureIntent()
        }
    }

    private fun dispatchTakePictureIntent() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (takePictureIntent.resolveActivity(packageManager) != null) {
            val photoFile: File? = try {
                createImageFile()
            } catch (ex: IOException) {
                Log.e("MainActivity", "Error creating image file", ex)
                null
            }
            photoFile?.also {
                currentPhotoURI = FileProvider.getUriForFile(
                    this,
                    "com.example.palayleafdiseasedetector.fileprovider",
                    it
                )
                Log.d("MainActivity", "Photo URI: $currentPhotoURI")
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, currentPhotoURI)
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
            }
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES) ?: throw IOException("Failed to get storage directory")
        if (storageDir != null) {
            if (!storageDir.exists() && !storageDir.mkdirs()){
                throw IOException("Failed to create directory")
            }
        }
        return File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        ).apply {
            Log.d("MainActivity", "Image file created: $absolutePath")
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Log.d("MainActivity", "Received image capture result")
            Log.d("MainActivity", "Photo URI on activity result: $currentPhotoURI")

            try {
                // Attempt to open and decode the image
                val inputStream = contentResolver.openInputStream(currentPhotoURI)
                    ?: throw IOException("Failed to open input stream")
                val imageBitmap = BitmapFactory.decodeStream(inputStream)
                    ?: throw IOException("Failed to decode image")

                // Initialize the classifier
                val classifier = ImageClassifier(this)
                val (label, confidence) = classifier.classify(imageBitmap)

                // Start ConfidenceActivity with the captured image and classification results
                val intent = Intent(this, ConfidenceActivity::class.java).apply {
                    putExtra("imagePath", currentPhotoURI.toString()) // Pass URI as string
                    putExtra("label", label)
                    putExtra("confidence", confidence)
                }
                startActivity(intent)
            } catch (e: IOException) {
                Log.e("MainActivity", "IOException while handling image", e)
                Toast.makeText(this, "Failed to load image.", Toast.LENGTH_SHORT).show()
            }
        }
    }

}
//failed to decode image