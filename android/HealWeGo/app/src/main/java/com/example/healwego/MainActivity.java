package com.example.healwego;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Edge-to-Edge 기능 활성화
        EdgeToEdge.enable(this);
        // 레이아웃 파일을 화면에 보여줌
        setContentView(R.layout.basic_main);

        // 시스템 바 인셋을 적용하여 화면 패딩 설정
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    // 혼자가기 버튼 클릭 리스너
    public void onButton1Clicked(View v) {
        Toast.makeText(this, "혼자가기 버튼이 눌렸어요.", Toast.LENGTH_SHORT).show();

        // AloneSetActivity로 이동
        Intent intent = new Intent(MainActivity.this, AloneSetActivity.class);
        startActivity(intent);
    }

    // 함께가기 버튼 클릭 리스너
    public void onButton2Clicked(View v) {
        Toast.makeText(this, "함께가기 버튼이 눌렸어요.", Toast.LENGTH_SHORT).show();
    }

    // 이미지 클릭 리스너 (을왕리 해수욕장 클릭)
    public void onImageClicked(View v) {
        // Toast 메시지 표시
        Toast.makeText(this, "을왕리 해수욕장이 눌렸어요.", Toast.LENGTH_SHORT).show();

        // ReserveActivity로 이동
        Intent intent = new Intent(MainActivity.this, ReserveMainActivity.class);
        startActivity(intent);
    }
}
