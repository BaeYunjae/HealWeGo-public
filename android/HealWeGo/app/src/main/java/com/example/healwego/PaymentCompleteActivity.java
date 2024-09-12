package com.example.healwego;

import android.content.Intent;  // Intent를 사용하기 위해 추가
import android.os.Bundle;
import android.view.View;  // View를 사용하기 위해 추가
import android.widget.Button;  // Button 사용을 위해 추가
import androidx.appcompat.app.AppCompatActivity;

public class PaymentCompleteActivity extends AppCompatActivity {

    private Button confirmButton;  // 버튼 선언

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.finish_pay);  // finish_pay.xml 파일 연결

        // confirmButton과 연결
        confirmButton = findViewById(R.id.confirmButton);  // XML에서 버튼의 ID로 찾음

        // confirmButton 클릭 시 MapPath로 이동
        confirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // MapPath로 이동하는 Intent 생성
                Intent intent = new Intent(PaymentCompleteActivity.this, MapPath.class);
                startActivity(intent);  // MapPath Activity 시작
            }
        });
    }
}
