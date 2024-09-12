package com.example.healwego;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.navigation.NavigationView;

import java.util.ArrayList;

public class ChatActivity extends AppCompatActivity {

    private RecyclerView chatRecyclerView;
    private ChatAdapter chatAdapter;
    private ArrayList<String> chatMessages;  // 채팅 메시지 리스트
    private TextView chatTitle;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private RecyclerView participantRecyclerView;
    private ParticipantAdapter participantAdapter;
    private ArrayList<Participant> participants;  // 참여자 리스트
    private TextView totalAmountTextView;
    private Button readyButton;
    private ImageButton backToChatButton;

    // 방장 여부 확인
    private boolean isHost = false;  // 방장이면 true로 설정
    private boolean isReady = false; // READY 상태 관리

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chatting_page);  // 채팅 레이아웃 사용

        // Intent에서 전달된 방 생성 여부 (isHost) 값을 가져옴
        Intent intent = getIntent();
        String roomTitle = intent.getStringExtra("roomTitle");
        isHost = intent.getBooleanExtra("isHost", false);  // 방 생성자 여부 확인

        // 방 제목 설정
        chatTitle = findViewById(R.id.chatTitle);
        if (roomTitle != null) {
            chatTitle.setText(roomTitle);
        }

        // RecyclerView 설정
        chatRecyclerView = findViewById(R.id.chatRecyclerView);
        chatMessages = new ArrayList<>();
        chatAdapter = new ChatAdapter(chatMessages);
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        chatRecyclerView.setAdapter(chatAdapter);

        // 예시로 기본 메시지 추가
        chatMessages.add("채팅에 오신 것을 환영합니다.");
        chatAdapter.notifyDataSetChanged();

        // DrawerLayout 및 NavigationView 설정
        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.nav_view);
        View headerView = navigationView.getHeaderView(0);

        // 참여자 리스트 설정
        participantRecyclerView = headerView.findViewById(R.id.participantRecyclerView);
        participants = new ArrayList<>();
        participantAdapter = new ParticipantAdapter(participants);
        participantRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        participantRecyclerView.setAdapter(participantAdapter);

        // 정산 금액 설정
        totalAmountTextView = headerView.findViewById(R.id.totalAmountTextView);
        totalAmountTextView.setText("예상 결제 요금: 16,000원");

        // 예시 참여자 추가
        participants.add(new Participant("방구대장", "(방장)", false));
        participants.add(new Participant("자드가자", "(나)", true));  // 자기 자신 강조
        participants.add(new Participant("예스맨", "", false));
        participants.add(new Participant("가면감", "", false));
        participantAdapter.notifyDataSetChanged();

        // 메뉴 버튼 클릭 시 Drawer 열기
        findViewById(R.id.menuButton).setOnClickListener(v -> drawerLayout.openDrawer(navigationView));

        // READY 또는 GO 버튼 설정
        readyButton = headerView.findViewById(R.id.readyButton);

        if (isHost) {
            // 방장이면 GO 버튼
            readyButton.setText("GO");
            readyButton.setOnClickListener(v -> {
                // 방장이 GO 버튼을 누르면 PaymentCompleteActivity(자동결제 완료 페이지)로 이동
                Intent payIntent = new Intent(ChatActivity.this, PaymentCompleteActivity.class);
                startActivity(payIntent);  // PaymentCompleteActivity로 화면 전환
            });
        } else {
            // 참여자면 READY -> CANCEL 상태로 토글
            readyButton.setText("READY");
            readyButton.setOnClickListener(v -> {
                // READY 상태 토글
                isReady = !isReady;

                if (isReady) {
                    readyButton.setText("CANCEL");
                    readyButton.setBackgroundColor(ContextCompat.getColor(this, R.color.darkcyan));  // READY 상태 색상
                    readyButton.setTextColor(ContextCompat.getColor(this, R.color.lightteal));  // READY 상태 글씨 색상
                } else {
                    readyButton.setText("READY");
                    readyButton.setBackgroundColor(ContextCompat.getColor(this, R.color.lightteal));  // READY 전 상태 색상
                    readyButton.setTextColor(ContextCompat.getColor(this, R.color.darkcyan));  // READY 전 상태 글씨 색상
                }
            });
        }

        // 화살표 버튼 설정 (Drawer 닫기)
        backToChatButton = headerView.findViewById(R.id.backToChatButton);
        backToChatButton.setOnClickListener(v -> drawerLayout.closeDrawer(navigationView));
    }

    // 참여자 데이터 클래스
    class Participant {
        String name;
        String role;
        boolean isCurrentUser;

        Participant(String name, String role, boolean isCurrentUser) {
            this.name = name;
            this.role = role;
            this.isCurrentUser = isCurrentUser;
        }
    }

    // 참여자 리스트 어댑터
    class ParticipantAdapter extends RecyclerView.Adapter<ParticipantAdapter.ParticipantViewHolder> {

        private ArrayList<Participant> participants;

        ParticipantAdapter(ArrayList<Participant> participants) {
            this.participants = participants;
        }

        @Override
        public ParticipantViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.participant_item, parent, false);
            return new ParticipantViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ParticipantViewHolder holder, int position) {
            Participant participant = participants.get(position);
            holder.nameTextView.setText(participant.name + " " + participant.role);

            // 사용자가 자신일 경우 이름을 노란색으로 강조
            if (participant.isCurrentUser) {
                holder.nameTextView.setTextColor(Color.YELLOW);
            } else {
                holder.nameTextView.setTextColor(Color.WHITE);
            }
        }

        @Override
        public int getItemCount() {
            return participants.size();
        }

        class ParticipantViewHolder extends RecyclerView.ViewHolder {

            TextView nameTextView;

            ParticipantViewHolder(View itemView) {
                super(itemView);
                nameTextView = itemView.findViewById(R.id.participantName);
            }
        }
    }
}
