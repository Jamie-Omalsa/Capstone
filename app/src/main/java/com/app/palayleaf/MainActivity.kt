package com.app.palayleaf

import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.app.palayleaf.diseases.*
import java.io.ByteArrayOutputStream
import java.io.IOException
import kotlin.system.exitProcess


class MainActivity : AppCompatActivity() {

    private lateinit var mClassifier: Classifier
    private lateinit var secondClassifier: SecondClassifier
    private lateinit var mBitmap: Bitmap

    private val mCameraRequestCode = 0
    private val mGalleryRequestCode = 2

    private val mInputSize = 224
    private val mModelPath = "palaymodel1.tflite"
    private val mLabelPath = "labels.txt"
    private val mSamplePath = "logo.png"

    lateinit var builder: AlertDialog.Builder
    lateinit var mPhoto: ImageView
    lateinit var infoBtn: ImageView
    lateinit var mCameraButton: ImageView
    lateinit var mGalleryButton: ImageView
    lateinit var mDetectButton: Button
    lateinit var mResult: TextView
    lateinit var mResultTextView: TextView
    lateinit var logOut: ImageView
    private lateinit var progressDialog1: ProgressDialog

    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        this.enableEdgeToEdge()
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY

        setContentView(R.layout.activity_main)
        // Initialize OpenCV


        mClassifier = Classifier(assets, mModelPath, mLabelPath, mInputSize)
        secondClassifier = SecondClassifier(
            assets,
            "palaymodel2.tflite",
            "labels2.txt",
            mInputSize
        )

        mPhoto = findViewById(R.id.mPhoto)
        mCameraButton = findViewById(R.id.mCameraButton)
        mGalleryButton = findViewById(R.id.mGalleryButton)
        mDetectButton = findViewById(R.id.mDetectButton)
        mResult = findViewById(R.id.mResult)
        mResultTextView = findViewById(R.id.mResultTextView)
        logOut = findViewById(R.id.logOut)
        infoBtn = findViewById(R.id.infoBtn)

        resources.assets.open(mSamplePath).use {
            mBitmap = BitmapFactory.decodeStream(it)
            mBitmap = Bitmap.createScaledBitmap(mBitmap, mInputSize, mInputSize, true)
            mPhoto.setImageBitmap(mBitmap)
        }



        infoBtn.setOnClickListener {
            val builder = AlertDialog.Builder(this@MainActivity)
            builder.setTitle("Impormasyon")
                .setMessage("Ang layunin ng aplikasyon na ito ay makatulong sa pag tuklas ng iba't ibang sakit na mayroon sa dahon ng palay. Ito ay upang matulungan ang ating mga magsasakang Pilipino na magkaroon ng mas kalidad na ani.")
                .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            val dialog = builder.create()
            dialog.show()
        }

        mCameraButton.setOnClickListener {
            val callCameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            startActivityForResult(callCameraIntent, mCameraRequestCode)

            mResult.text=""
            mResultTextView.text=""
        }

        mGalleryButton.setOnClickListener {
            val callGalleryIntent = Intent(Intent.ACTION_PICK)
            callGalleryIntent.type = "image/*"
            startActivityForResult(callGalleryIntent, mGalleryRequestCode)
        }

        mDetectButton.setOnClickListener {
            val progressDialog = ProgressDialog(this)
            progressDialog.setMessage("Processing Image...")
            progressDialog.setCancelable(false)
            progressDialog.show()

            Thread {
                val results = mClassifier.recognizeImage(mBitmap).firstOrNull()

                runOnUiThread {
                    if (results == null) {
                        mResult.text = "Unrecognized Image"
                        mResultTextView.text = "Confidence: N/A"
                        Toast.makeText(this, "Image Undetected...Try Again.", Toast.LENGTH_SHORT).show()
                    } else {
                        mResult.text = when (results.title) {
                            "nonpalay" -> "Non-palay Leaf"
                            "palay" -> "Palay Leaf"
                            else -> "Unknown Leaf"
                        }
                        mResultTextView.text = "Confidence: ${results.confidence}"

                        if (results.title == "palay") {
                            val progressDialog1 = ProgressDialog(this)
                            progressDialog1.setMessage("Applying CLAHE... Please wait.")
                            progressDialog1.setCancelable(false)
                            progressDialog1.show()

                            // Apply CLAHE to the image
                            val claheBitmap = applyCLAHE(mBitmap)

                            // Show the CLAHE image in an AlertDialog with another button
                            if (claheBitmap != null) {
                                showCLAHEImageDialog(claheBitmap)
                            }

                            progressDialog1.dismiss()
                        }
                    }

                    progressDialog.dismiss()
                }
            }.start()
        }


        logOut.setOnClickListener {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Confirm Exit").setMessage("Do you really want to Exit the App?(Y/N)")
                .setPositiveButton("Yes") { _, _ ->
                    finishAffinity()
                    exitProcess(0)
                }
                .setNegativeButton("No") { dialogInterface, _ -> dialogInterface.dismiss() }
                .setCancelable(false)
                .show()
        }
    }
    private fun applyCLAHE(originalBitmap: Bitmap): Bitmap {
        val width = originalBitmap.width
        val height = originalBitmap.height
        val pixels = IntArray(width * height)
        originalBitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        // Parameters for CLAHE
        val tileSize = 8 // Tile size for CLAHE
        val clipLimit = 4.0f // Larger clip limit to avoid clipping too aggressively

        // Create an output bitmap
        val outputBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val outputPixels = IntArray(width * height)

        // Step 1: Convert image to grayscale
        for (i in pixels.indices) {
            val pixel = pixels[i]
            val red = (pixel shr 16 and 0xFF)
            val green = (pixel shr 8 and 0xFF)
            val blue = (pixel and 0xFF)
            val grayValue = (0.299 * red + 0.587 * green + 0.114 * blue).toInt()
            val alpha = (pixel shr 24 and 0xFF)

            // Debug: Log grayscale value
            if (i % 1000 == 0) {
                Log.d("CLAHE", "Grayscale value at pixel $i: $grayValue")
            }

            pixels[i] = (alpha shl 24) or (grayValue shl 16) or (grayValue shl 8) or grayValue
        }

        // Step 2: Iterate through the image in tiles
        for (y in 0 until height step tileSize) {
            for (x in 0 until width step tileSize) {
                val xEnd = minOf(x + tileSize, width)
                val yEnd = minOf(y + tileSize, height)

                // Histogram and pixel counting for each tile
                val histogram = IntArray(256) { 0 }
                var totalPixels = 0

                // Step 3: Calculate histogram for the tile
                for (tileY in y until yEnd) {
                    for (tileX in x until xEnd) {
                        val pixel = pixels[tileY * width + tileX]
                        val grayValue = pixel and 0xFF // Grayscale value
                        histogram[grayValue]++
                        totalPixels++
                    }
                }

                // Debug: Log histogram for the tile
                Log.d("CLAHE", "Histogram for tile ($x, $y): ${histogram.joinToString()}")

                // Step 4: Clip the histogram
                val clipLimitValue = (totalPixels * clipLimit / 256).toInt()
                var excessPixels = 0
                for (i in histogram.indices) {
                    if (histogram[i] > clipLimitValue) {
                        excessPixels += histogram[i] - clipLimitValue
                        histogram[i] = clipLimitValue
                    }
                }

                // Step 5: Redistribute excess pixels
                val excessPerBucket = excessPixels / 256
                for (i in histogram.indices) {
                    histogram[i] += excessPerBucket
                }

                // Step 6: Compute the cumulative distribution function (CDF)
                val cdf = IntArray(256)
                cdf[0] = histogram[0]
                for (i in 1 until 256) {
                    cdf[i] = cdf[i - 1] + histogram[i]
                }

                // Normalize CDF
                for (i in cdf.indices) {
                    cdf[i] = ((cdf[i] - cdf[0]) * 255 / (totalPixels - cdf[0])).coerceIn(0, 255)
                }

                // Debug: Log the CDF values
                Log.d("CLAHE", "CDF for tile ($x, $y): ${cdf.joinToString()}")

                // Step 7: Apply the new pixel values based on the CDF
                for (tileY in y until yEnd) {
                    for (tileX in x until xEnd) {
                        val pixel = pixels[tileY * width + tileX]
                        val grayValue = pixel and 0xFF
                        val newGrayValue = cdf[grayValue]
                        val alpha = (pixel shr 24 and 0xFF)

                        // Debug: Log the old and new grayscale values
                        if (tileY % 50 == 0 && tileX % 50 == 0) {
                            Log.d("CLAHE", "Old gray value: $grayValue -> New gray value: $newGrayValue")
                        }

                        outputPixels[tileY * width + tileX] = (alpha shl 24) or (newGrayValue shl 16) or (newGrayValue shl 8) or newGrayValue
                    }
                }
            }
        }

        // Step 8: Set the processed pixel data back to the output Bitmap
        outputBitmap.setPixels(outputPixels, 0, width, 0, 0, width, height)

        return outputBitmap
    }



    private fun showCLAHEImageDialog(claheImage: Bitmap) {
        val builder = AlertDialog.Builder(this)
        val dialogView = layoutInflater.inflate(R.layout.dialog_clahe_image, null)
        builder.setView(dialogView)

        val imageView = dialogView.findViewById<ImageView>(R.id.claheImageView)
        val detectButton = dialogView.findViewById<Button>(R.id.detectButton)
        val resultTextView = dialogView.findViewById<TextView>(R.id.resultTextView) // New TextView for results
        val confidenceTextView = dialogView.findViewById<TextView>(R.id.confidenceTextView) // New TextView for confidence

        // Set the CLAHE processed image to the ImageView
        imageView.setImageBitmap(claheImage)

        val alertDialog = builder.create()

        detectButton.setOnClickListener {
            // Start a new thread to run the second classifier
            Thread {
                // Call the second classifier function with the CLAHE image
                val secondResults = secondClassifier.recognizeImage(mBitmap).firstOrNull()

                runOnUiThread {
                    if (secondResults == null) {
                        // Display results in the dialog
                        resultTextView.text = "Unrecognized Image"
                        confidenceTextView.text = "Confidence: N/A"
                        Toast.makeText(this, "Image Undetected...Try Again.", Toast.LENGTH_SHORT).show()
                    } else {
                        // Update the result views based on the classifier results
                        resultTextView.text = when (secondResults.title) {
                            "BACTERIAL_BLIGHT" -> "Bacterial Blight"
                            "BROWN_SPOT" -> "Brown Spot"
                            "NARROW" -> "Narrow Brown Spot"
                            "LEAF SMUT" -> "Leaf Smut"
                            "LEAF STREAK" -> "Leaf Streak"
                            "LEAF_SCALD" -> "Leaf Scald"
                            "LEAF_BLAST" -> "Leaf Blast"
                            "SHEATH_BLIGHT" -> "Sheath Blight"
                            "TUNGRO" -> "Tungro"
                            "HISPA" -> "Hispa"
                            "HEALTHY" -> "Healthy Palay Leaf"
                            else -> "Unknown Disease"
                        }
                        confidenceTextView.text = "Confidence: ${secondResults.confidence}"

                        // Check if the disease detected is BACTERIAL_BLIGHT
                        if (secondResults.title == "BACTERIAL_BLIGHT") {
                            // Start BacterialBlight activity
                            val intent = Intent(this, BacterialBlight::class.java)

                            // Optional: You can pass the image byte array if needed
                            val stream = ByteArrayOutputStream()
                            claheImage.compress(Bitmap.CompressFormat.PNG, 100, stream)
                            val byteArray = stream.toByteArray()
                            intent.putExtra("image", byteArray) // Send the CLAHE image as byte array

                            startActivity(intent) // Start the new activity
                        }else if (secondResults.title == "BROWN_SPOT") {
                            // Start BacterialBlight activity
                            val intent = Intent(this, BrownSpot::class.java)

                            // Optional: You can pass the image byte array if needed
                            val stream = ByteArrayOutputStream()
                            claheImage.compress(Bitmap.CompressFormat.PNG, 100, stream)
                            val byteArray = stream.toByteArray()
                            intent.putExtra("image", byteArray) // Send the CLAHE image as byte array

                            startActivity(intent) // Start the new activity
                        }else if (secondResults.title == "NARROW") {
                            // Start BacterialBlight activity
                            val intent = Intent(this, NarrowBrownSpot::class.java)

                            // Optional: You can pass the image byte array if needed
                            val stream = ByteArrayOutputStream()
                            claheImage.compress(Bitmap.CompressFormat.PNG, 100, stream)
                            val byteArray = stream.toByteArray()
                            intent.putExtra("image", byteArray) // Send the CLAHE image as byte array

                            startActivity(intent) // Start the new activity
                        }else if (secondResults.title == "LEAF SMUT") {
                            // Start BacterialBlight activity
                            val intent = Intent(this,LeafSmut::class.java)

                            // Optional: You can pass the image byte array if needed
                            val stream = ByteArrayOutputStream()
                            claheImage.compress(Bitmap.CompressFormat.PNG, 100, stream)
                            val byteArray = stream.toByteArray()
                            intent.putExtra("image", byteArray) // Send the CLAHE image as byte array

                            startActivity(intent) // Start the new activity
                        }else if (secondResults.title == "LEAF STREAK") {
                            // Start BacterialBlight activity
                            val intent = Intent(this,LeafStreak::class.java)

                            // Optional: You can pass the image byte array if needed
                            val stream = ByteArrayOutputStream()
                            claheImage.compress(Bitmap.CompressFormat.PNG, 100, stream)
                            val byteArray = stream.toByteArray()
                            intent.putExtra("image", byteArray) // Send the CLAHE image as byte array

                            startActivity(intent) // Start the new activity
                        }else if (secondResults.title == "LEAF_SCALD") {
                            // Start BacterialBlight activity
                            val intent = Intent(this,LeafScald::class.java)

                            // Optional: You can pass the image byte array if needed
                            val stream = ByteArrayOutputStream()
                            claheImage.compress(Bitmap.CompressFormat.PNG, 100, stream)
                            val byteArray = stream.toByteArray()
                            intent.putExtra("image", byteArray) // Send the CLAHE image as byte array

                            startActivity(intent) // Start the new activity
                        }else if (secondResults.title == "LEAF_BLAST") {
                            // Start BacterialBlight activity
                            val intent = Intent(this,LeafBlast::class.java)

                            // Optional: You can pass the image byte array if needed
                            val stream = ByteArrayOutputStream()
                            claheImage.compress(Bitmap.CompressFormat.PNG, 100, stream)
                            val byteArray = stream.toByteArray()
                            intent.putExtra("image", byteArray) // Send the CLAHE image as byte array

                            startActivity(intent) // Start the new activity
                        }else if (secondResults.title == "SHEATH_BLIGHT") {
                            // Start BacterialBlight activity
                            val intent = Intent(this,SheathBlight::class.java)

                            // Optional: You can pass the image byte array if needed
                            val stream = ByteArrayOutputStream()
                            claheImage.compress(Bitmap.CompressFormat.PNG, 100, stream)
                            val byteArray = stream.toByteArray()
                            intent.putExtra("image", byteArray) // Send the CLAHE image as byte array

                            startActivity(intent) // Start the new activity
                        }else if (secondResults.title == "TUNGRO") {
                            // Start BacterialBlight activity
                            val intent = Intent(this,Tungro::class.java)

                            // Optional: You can pass the image byte array if needed
                            val stream = ByteArrayOutputStream()
                            claheImage.compress(Bitmap.CompressFormat.PNG, 100, stream)
                            val byteArray = stream.toByteArray()
                            intent.putExtra("image", byteArray) // Send the CLAHE image as byte array

                            startActivity(intent) // Start the new activity
                        }else if (secondResults.title == "HISPA") {
                            // Start BacterialBlight activity
                            val intent = Intent(this,Hispa::class.java)

                            // Optional: You can pass the image byte array if needed
                            val stream = ByteArrayOutputStream()
                            claheImage.compress(Bitmap.CompressFormat.PNG, 100, stream)
                            val byteArray = stream.toByteArray()
                            intent.putExtra("image", byteArray) // Send the CLAHE image as byte array

                            startActivity(intent) // Start the new activity
                        }else if (secondResults.title == "HEALTHY") {
                            Toast.makeText(this, "This is a Healthy Palay Leaf.", Toast.LENGTH_SHORT).show()
                        }

                        // No need to dismiss dialog here if you want to keep it open for review
                        // alertDialog.dismiss()
                    }
                }
            }.start()
        }

        alertDialog.setCancelable(true)
        alertDialog.show()
    }




    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            mCameraRequestCode -> {
                if (resultCode == Activity.RESULT_OK && data != null) {
                    val extras = data.extras
                    if (extras != null) {
                        mBitmap = extras.get("data") as Bitmap
                        mBitmap = scaleImage(mBitmap)
                        Toast.makeText(this, "Image crop to: w= ${mBitmap.width} h= ${mBitmap.height}", Toast.LENGTH_LONG).apply {
                            setGravity(Gravity.BOTTOM, 0, 20)
                        }.show()
                        mPhoto.setImageBitmap(mBitmap)
                        mResultTextView.text = "Your photo image set now."
                    } else {
                        Toast.makeText(this, "No image data found", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(this, "Camera cancelled..", Toast.LENGTH_LONG).show()
                }
            }
            mGalleryRequestCode -> {
                if (data != null && data.data != null) {
                    val uri = data.data
                    try {
                        mBitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, uri)
                        mBitmap = scaleImage(mBitmap)
                        mPhoto.setImageBitmap(mBitmap)
                    } catch (e: IOException) {
                        e.printStackTrace()
                        Toast.makeText(this, "Failed to load image", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(this, "No image selected", Toast.LENGTH_LONG).show()
                }
            }
            else -> {
                Toast.makeText(this, "Unrecognized request code", Toast.LENGTH_LONG).show()
            }
        }
    }


    fun scaleImage(image: Bitmap): Bitmap {
        // Create a matrix for the manipulation
        val matrix = Matrix()

        // Rotate the image by 90 degrees
//        matrix.postRotate(90F)

        // Scale the image to 224 x 224
        val scaledImage = Bitmap.createScaledBitmap(image, 224, 224, true)

        // Apply the matrix for rotation
        return Bitmap.createBitmap(scaledImage, 0, 0, scaledImage.width, scaledImage.height, matrix, true)
    }
}
