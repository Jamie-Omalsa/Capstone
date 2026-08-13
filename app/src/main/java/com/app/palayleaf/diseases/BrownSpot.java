package com.app.palayleaf.diseases;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.app.palayleaf.R;

import java.io.ByteArrayInputStream;

public class BrownSpot extends AppCompatActivity {

    private ImageView bckBtn;
    private Button saveBtn;
    private ImageView imageV;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_brown_spot);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        bckBtn = findViewById(R.id.bckBtn);
        saveBtn = findViewById(R.id.saveBtn); // Initialize save button
        imageV = findViewById(R.id.imageV);

        // Retrieve the byte array from the Intent
        byte[] byteArray = getIntent().getByteArrayExtra("image");

        // Convert byte array back to Bitmap
        if (byteArray != null) {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(byteArray);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

            // Display the Bitmap in the ImageView
            imageV.setImageBitmap(bitmap);

            // Set OnClickListener for the Save button to save the image
            saveBtn.setOnClickListener(v -> saveImageToGallery(bitmap));
        } else {
            Toast.makeText(this, "No image data received.", Toast.LENGTH_SHORT).show();
            finish(); // Close the activity if no image is passed
        }


        // Back button to go to the previous activity
        bckBtn.setOnClickListener(view -> onBackPressed());
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    // Function to save the Bitmap to the gallery
    private void saveImageToGallery(Bitmap bitmap) {
        ContentResolver contentResolver = getContentResolver();
        String savedImageURL = MediaStore.Images.Media.insertImage(
                contentResolver,
                bitmap,
                "Brown Spot Disease Image",
                "Image of a leaf detected with Brown Spot disease"
        );

        if (savedImageURL != null) {
            Toast.makeText(this, "Image saved to gallery.", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Failed to save image.", Toast.LENGTH_SHORT).show();
        }
    }
}
