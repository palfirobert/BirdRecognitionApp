package com.example.birdrecognitionapp.activities;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import com.example.birdrecognitionapp.services.SessionManagerService;
public class SplashScreenActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SessionManagerService sessionManager = new SessionManagerService(this);
        Intent intent;
        if (sessionManager.isLoggedIn()) {
            intent = new Intent(SplashScreenActivity.this, MainActivity.class);
        } else {
            intent = new Intent(SplashScreenActivity.this, LoginActivity.class);
        }
        startActivity(intent);
        finish();
    }
}
