package com.example.dormchef.activities;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import android.widget.Button;
import android.content.Intent;


import com.example.dormchef.R;

public class LandingActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_landing);

        Button startBtn = findViewById(R.id.startBtn);

        startBtn.setOnClickListener(v -> {
            Intent intent = new Intent(LandingActivity.this, HomeActivity.class);
            startActivity(intent);
        });
    }
}