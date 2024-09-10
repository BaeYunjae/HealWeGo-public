package com.example.healwego;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class ReserveMainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // reserve_main.xml 레이아웃 설정
        setContentView(R.layout.reserve_main);

        // "함께 가기" 버튼에 대한 클릭 리스너 설정
        Button togetherButton = findViewById(R.id.together); // reserve_main.xml에서 버튼의 ID 사용
        togetherButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // ChatListActivity로 이동
                Intent intent = new Intent(ReserveMainActivity.this, ChatListActivity.class);
                startActivity(intent);
            }
        });
    }
}
