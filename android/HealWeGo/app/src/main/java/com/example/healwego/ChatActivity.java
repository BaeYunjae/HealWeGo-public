package com.example.healwego;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class ChatActivity extends AppCompatActivity {

    private RecyclerView chatRecyclerView;
    private ChatAdapter chatAdapter;
    private ArrayList<String> chatMessages;  // 채팅 메시지 리스트
    private TextView chatTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chatting_page);  // 채팅 레이아웃 사용

        // 채팅 제목을 표시하는 TextView
        chatTitle = findViewById(R.id.chatTitle);

        // 전달된 방 제목을 가져옴
        Intent intent = getIntent();
        String roomTitle = intent.getStringExtra("roomTitle");

        // 방 제목이 null이 아니면 화면에 표시
        if (roomTitle != null) {
            chatTitle.setText(roomTitle);
        }

        // RecyclerView 설정
        chatRecyclerView = findViewById(R.id.chatRecyclerView);
        chatMessages = new ArrayList<>();
        chatAdapter = new ChatAdapter(chatMessages);  // 어댑터 설정
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        chatRecyclerView.setAdapter(chatAdapter);

        // 예시로 기본 메시지 추가
        chatMessages.add("채팅에 오신 것을 환영합니다.");
        chatAdapter.notifyDataSetChanged();
    }
}
