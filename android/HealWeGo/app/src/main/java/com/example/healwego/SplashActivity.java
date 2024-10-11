package com.example.healwego;

import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.content.Intent;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        View content = findViewById(android.R.id.content);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            content.getViewTreeObserver().addOnDrawListener(() -> {
                // Do nothing
            });
        }
        callNextScreen();
    }

    private void callNextScreen() {
        new Handler().postDelayed(() -> {
            Intent intent = new Intent(SplashActivity.this, AuthActivity.class);
            startActivity(intent);
            finish();
        }, 1000);
    }
}
