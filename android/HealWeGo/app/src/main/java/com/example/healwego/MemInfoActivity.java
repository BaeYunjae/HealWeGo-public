package com.example.healwego;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class MemInfoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.info_register); // info_register.xml 레이아웃을 설정

        // 다음 버튼 클릭 시 card_register 액티비티로 이동
        Button nextButton = findViewById(R.id.btn_next); // info_register.xml에 정의된 버튼 ID 사용
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MemInfoActivity.this, CardInfoActivity.class);
                startActivity(intent); // CardRegisterActivity로 전환
            }
        });
    }
}
