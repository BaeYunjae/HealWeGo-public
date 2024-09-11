package com.example.healwego;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {

    private ArrayList<String> chatMessages;

    // 생성자: 채팅 메시지 리스트를 받음
    public ChatAdapter(ArrayList<String> chatMessages) {
        this.chatMessages = chatMessages;
    }

    // ViewHolder 클래스
    public static class ChatViewHolder extends RecyclerView.ViewHolder {
        public TextView chatMessageTextView;

        public ChatViewHolder(View itemView) {
            super(itemView);
            chatMessageTextView = itemView.findViewById(R.id.chatMessageTextView);
        }
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // chat_item.xml 레이아웃을 인플레이트하여 ViewHolder 생성
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.chat_item, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        // 현재 위치의 채팅 메시지를 가져와서 TextView에 설정
        String message = chatMessages.get(position);
        holder.chatMessageTextView.setText(message);
    }

    @Override
    public int getItemCount() {
        // 채팅 메시지 리스트의 크기 반환
        return chatMessages.size();
    }
}
