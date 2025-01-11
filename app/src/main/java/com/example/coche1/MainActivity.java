package com.example.coche1;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnControls = findViewById(R.id.btnControls);
        Button btnTakePhoto = findViewById(R.id.btnTakePhoto);

        // Abrir la actividad de controles
        btnControls.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ControlActivity.class);
            startActivity(intent);
        });

        // Abrir la actividad para tomar fotos
        btnTakePhoto.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, PhotoActivity.class);
            startActivity(intent);
        });
    }
}
