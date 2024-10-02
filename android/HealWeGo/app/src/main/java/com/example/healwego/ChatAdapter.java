package com.example.healwego;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import android.util.Pair;
import androidx.constraintlayout.widget.ConstraintLayout;

import org.w3c.dom.Text;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_WELCOME = 0; // 환영 메시지
    private static final int VIEW_TYPE_MESSAGE = 1; // 채팅 메시지

    private ArrayList<Pair<String, Pair<String, String>>> chatMessages;
    private String currentUserId;

    // 생성자: 채팅 메시지 리스트를 받음
    public ChatAdapter(ArrayList<Pair<String, Pair<String, String>>> chatMessages, String currentUserId) {
        this.chatMessages = chatMessages;
        this.currentUserId = currentUserId;
    }

    @Override
    public int getItemViewType(int position) {
        // 첫 번째 항목은 환영 메시지, 나머지는 채팅 메시지로 처리
        if (position == 0) {
            return VIEW_TYPE_WELCOME;
        } else {
            return VIEW_TYPE_MESSAGE;
        }
    }

    // ViewHolder 클래스
    public static class ChatViewHolder extends RecyclerView.ViewHolder {
        public TextView chatMessageTextView;
        public TextView chatUsernameTextView;
        public TextView chatTimeTextView;
        public ConstraintLayout messageLayout;
        public LinearLayout messageTimeContainer;

        public ChatViewHolder(View itemView) {
            super(itemView);
            chatMessageTextView = itemView.findViewById(R.id.chatMessageTextView);
            chatUsernameTextView = itemView.findViewById(R.id.chatUsernameTextView);
            chatTimeTextView = itemView.findViewById(R.id.chatTimeTextView);
            messageLayout = itemView.findViewById(R.id.messageLayout);
            messageTimeContainer = itemView.findViewById(R.id.messageTimeContainer);
        }
    }

    // ViewHolder 클래스 (환영 메시지용)
    public static class WelcomeViewHolder extends RecyclerView.ViewHolder {
        public TextView welcomeTextView;

        public WelcomeViewHolder(View itemView) {
            super(itemView);
            welcomeTextView = itemView.findViewById(R.id.welcomeTextView);  // XML에서 정의한 ID로 가져옴
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_WELCOME) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.welcome_message_item, parent, false);
            return new WelcomeViewHolder(view);  // XML 레이아웃을 inflate해서 반환
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.chat_item, parent, false);
            return new ChatViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder.getItemViewType() == VIEW_TYPE_WELCOME) {
            WelcomeViewHolder welcomeViewHolder = (WelcomeViewHolder) holder;
            welcomeViewHolder.welcomeTextView.setGravity(Gravity.CENTER);
        } else {
            ChatViewHolder chatViewHolder = (ChatViewHolder) holder;
            // 채팅 메시지 처리
            // 현재 위치의 채팅 메시지를 가져와서 TextView에 설정
            Pair<String, Pair<String, String>> currentMessage = chatMessages.get(position - 1);
            String senderId = currentMessage.first;  // 보낸 사람의 ID (chatid을 포함해야 함)
            String userName = currentMessage.second.first;
            String messageWithTime = currentMessage.second.second;  // 메시지 내용

            // 메시지와 시간 분리
            String[] splitMessage = messageWithTime.split(" ", 2);  // 시간과 메시지를 분리
            String time = splitMessage[0];
            String message = splitMessage[1];

            // 메시지 설정
            chatViewHolder.chatMessageTextView.setText(message);
            chatViewHolder.chatTimeTextView.setText(time);

            ConstraintSet constraintSet = new ConstraintSet();
            constraintSet.clone(chatViewHolder.messageLayout);

            // 현재 사용자가 보낸 메시지일 경우
            if (senderId.equals(currentUserId)) {
                //message container의 gravity를 end로 설정 해서 오른쪽에 배치
                chatViewHolder.messageTimeContainer.setGravity(Gravity.END);

                //time view를 지웠다가 다시 추가해 채팅의 왼쪽에 오도록
                chatViewHolder.messageTimeContainer.removeView(chatViewHolder.chatTimeTextView);  // 시간 텍스트 뷰 제거
                chatViewHolder.messageTimeContainer.addView(chatViewHolder.chatTimeTextView, 0);

                // 사용자가 보낸 메시지: 오른쪽 정렬, 이름 숨김, 시간은 왼쪽에 표시
                chatViewHolder.chatUsernameTextView.setVisibility(View.GONE);  // 이름 숨기기
                chatViewHolder.chatTimeTextView.setVisibility(View.VISIBLE);

                // 메시지를 오른쪽으로 정렬
                constraintSet.clear(R.id.chatMessageTextView, ConstraintSet.START);
                constraintSet.connect(R.id.chatMessageTextView, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, 0);
                chatViewHolder.chatMessageTextView.setBackgroundResource(R.drawable.bubble_background_right);  // 오른쪽 정렬용 배경
                chatViewHolder.chatMessageTextView.setGravity(Gravity.END);

            } else {
                // 상대방이 보낸 메시지: 왼쪽 정렬, 이름 표시, 시간은 오른쪽에 표시
                chatViewHolder.chatUsernameTextView.setText(userName);  // 상대방 이름 표시
                chatViewHolder.chatUsernameTextView.setVisibility(View.VISIBLE);
                chatViewHolder.chatTimeTextView.setVisibility(View.VISIBLE);

                // 메시지를 왼쪽으로 정렬
                constraintSet.clear(R.id.chatMessageTextView, ConstraintSet.END);
                constraintSet.connect(R.id.chatMessageTextView, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, 0);
                chatViewHolder.chatMessageTextView.setBackgroundResource(R.drawable.bubble_background_left);  // 왼쪽 정렬용 배경
                chatViewHolder.chatMessageTextView.setGravity(Gravity.START);

                // 시간은 메시지 오른쪽에 표시
                constraintSet.clear(R.id.chatTimeTextView, ConstraintSet.START);
                constraintSet.connect(R.id.chatTimeTextView, ConstraintSet.START, R.id.chatMessageTextView, ConstraintSet.END, 0);  // 메시지와 시간 사이의 간격 8dp 설정
                constraintSet.connect(R.id.chatTimeTextView, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, 0);  // 부모 뷰의 끝에 배치
                chatViewHolder.chatTimeTextView.setGravity(Gravity.END);
            }

            constraintSet.applyTo(chatViewHolder.messageLayout);  // 변경사항 적용
        }
    }

    @Override
    public int getItemCount() {
        return chatMessages.size() + 1; // 환영 메시지를 위한 하나의 추가 항목
    }
}



