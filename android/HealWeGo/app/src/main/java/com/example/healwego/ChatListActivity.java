package com.example.healwego;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class ChatListActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // chat_list.xml 레이아웃 설정
        setContentView(R.layout.chat_list);
    }
}
