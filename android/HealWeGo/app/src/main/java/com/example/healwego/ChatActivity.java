package com.example.healwego;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.navigation.NavigationView;

import com.amazonaws.mobile.client.AWSMobileClient;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import java.lang.ref.WeakReference;
import java.util.Iterator;

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

    // 방장 여부 확인 (API 요청으로 바뀌어야 함)
    private boolean isHost = false;  // 방장이면 true로 설정
    private boolean isReady = false; // READY 상태 관리
    private String hostId;  // 방장 ID
    private String userId;  // 현재 사용자 ID

    // API 요청을 위한 URL
    private String mURL = "https://e2fqrjfyj9.execute-api.ap-northeast-2.amazonaws.com/healwego-stage/";

    // MyHandler를 static으로 선언하여 메모리 누수를 방지하고, WeakReference로 액티비티 참조
    private static class MyHandler extends Handler {
        private final WeakReference<ChatActivity> weakReference;

        // Deprecated 경고를 해결하기 위해 Looper를 명시적으로 받도록 수정
        public MyHandler(Looper looper, ChatActivity chatActivity) {
            super(looper);  // 명시적으로 Looper를 전달
            weakReference = new WeakReference<>(chatActivity);
        }

        @Override
        public void handleMessage(Message msg) {
            ChatActivity chatActivity = weakReference.get();
            if (chatActivity != null && !chatActivity.isFinishing()) {
                // 액티비티가 여전히 존재하는 경우에만 작업 수행
                String jsonString = (String) msg.obj;
                Toast.makeText(chatActivity.getApplicationContext(), jsonString, Toast.LENGTH_LONG).show();
                Log.i("ChatActivity", "응답: " + jsonString);
            }
        }
    }

    // WeakReference로 Activity에 대한 참조를 가진 MyHandler 객체
    private final ChatActivity.MyHandler mHandler = new ChatActivity.MyHandler(Looper.getMainLooper(), this); // MainLooper 전달


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chatting_page);  // 채팅 레이아웃 사용

        // 현재 사용자 ID 가져오기
        userId = AWSMobileClient.getInstance().getUsername();

        // 뒤로가기 버튼을 처리하는 부분
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // 뒤로가기 버튼을 눌렀을 때 실행할 코드
                Intent intent = new Intent(ChatActivity.this, ChatListActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            }
        });

        // Intent에서 전달된 방 생성 여부 (isHost) 값을 가져옴
        Intent intent = getIntent();
        String roomTitle = intent.getStringExtra("roomTitle");
        String usersInfo = intent.getStringExtra("usersInfo");
        hostId = intent.getStringExtra("hostId");
        Log.i("ChatActiviy: ", "사용자 정보: " + usersInfo);

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

        // 기본 메시지 추가 (아마 처음 입장일 때로 변경해야 할 듯)
        chatMessages.add("채팅에 오신 것을 환영합니다.");
        chatAdapter.notifyDataSetChanged();

        // DrawerLayout 및 NavigationView 설정
        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.nav_view);
        View headerView = navigationView.getHeaderView(0);

        // 참여자 리스트 설정
        participantRecyclerView = headerView.findViewById(R.id.participantRecyclerView);
        participants = new ArrayList<>();
        participantAdapter = new ParticipantAdapter(this, participants);
        participantRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        participantRecyclerView.setAdapter(participantAdapter);

        // usersInfo 데이터를 처리하여 participants 리스트에 추가
        if (usersInfo != null){
            try{
                JSONObject usersJson = new JSONObject(usersInfo);
                addParticipants(usersJson);
            } catch(JSONException e){
                Log.e("ChatActivity", "참여자 정보 처리 오류", e);
            }
        }

        // 정산 금액 설정
        totalAmountTextView = headerView.findViewById(R.id.totalAmountTextView);
        totalAmountTextView.setText("예상 결제 요금: 16,000원");

        // 메뉴 버튼 클릭 시 Drawer 열기
        findViewById(R.id.menuButton).setOnClickListener(v -> {
            // 내비게이션 열기
            drawerLayout.openDrawer(navigationView);
        });

        // READY 또는 GO 버튼 설정
        readyButton = headerView.findViewById(R.id.readyButton);
        if (readyButton != null) {
            updateReadyButton();
        } else{
            Log.e("ChatActivity", "readyButton is null");
        }

        // 화살표 버튼 설정 (Drawer 닫기)
        backToChatButton = headerView.findViewById(R.id.backToChatButton);
        backToChatButton.setOnClickListener(v -> drawerLayout.closeDrawer(navigationView));

        // 나가기 버튼 설정
        ImageButton exitButton = headerView.findViewById(R.id.exitButton);
        exitButton.setOnClickListener(v -> {
            sendDeleteRequest();
        });

    }

    // addParticipants 메서드
    private void addParticipants(JSONObject usersJson) throws JSONException {
        Iterator<String> keys = usersJson.keys();
        while (keys.hasNext()) {
            String key = keys.next();  // "userId"를 key로 사용
            JSONObject userObject = usersJson.getJSONObject(key);
            Log.i("participant", "참여자 정보: " + userObject);
            String userName = userObject.optString("username", "Unknown");
            boolean isReady = userObject.getInt("is_ready") == 1;  // 1이면 READY
            boolean isCurrentUser = key.equals(this.userId);  // key를 사용하여 현재 사용자인지 확인
            boolean isHost = key.equals(hostId);  // 방장 확인

            String role = isHost ? "(방장)" : "";  // 방장일 경우 "방장" 역할 추가
            Log.i("ChatActivity", "Adding participant: " + key + ", Role: " + role + ", IsReady: " + isReady + ", IsCurrentUser: " + isCurrentUser);

            participants.add(new Participant(userName, role, isCurrentUser, isReady));  // key가 userId
        }
        Log.i("ChatActivity", "참여 인원: " + participants.size());

        participantAdapter.notifyDataSetChanged();
        checkAllReady();
    }


    // READY 상태 토글 및 서버에 상태 업데이트
    private void toggleReadyState() {
        isReady = !isReady;

        // 서버에 READY 상태 업데이트
        updateReadyStatusInDB(isReady ? 1 : 0);

        for (Participant participant : participants) {
            if (participant.isCurrentUser()) {
                participant.setReady(isReady);
            }
        }
        participantAdapter.notifyDataSetChanged();
        checkAllReady();  // 모든 참가자가 READY 상태인지 확인하여 GO 버튼 상태 갱신
    }

    // 서버에 READY 상태 업데이트
    private void updateReadyStatusInDB(int readyStatus) {
        String connMethod = "PATCH";
        String apiURL = mURL + "user/go";

        JSONObject body = new JSONObject();
        try {
            body.put("Method", connMethod);
            body.put("User_ID", this.userId);  // userId 대신 this.userId 사용
        } catch (JSONException e) {
            Log.e("ChatActivity", "PATCH JSON 생성 오류", e);
            return;
        }

        String bodyJson = body.toString();
        ApiRequestHandler.getJSON(apiURL, connMethod, mHandler, bodyJson);
        Log.i("ChatActivity", "PATCH 요청: " + bodyJson);
    }



    // 방 나가기 DELETE API 요청 함수
    private void sendDeleteRequest(){
        // API 유형
        String connMethod = "DELETE";
        String apiURL = mURL + "room/list";

        JSONObject body = new JSONObject();

        try{
            body.put("Method", connMethod);
            body.put("User_ID", this.userId);
        } catch(JSONException e){
            Log.e("ChatActivity", "DELETE JSON 생성 오류");
            return;
        }

        String bodyJson = body.toString();

        // API 요청 함수
        ApiRequestHandler.getJSON(apiURL, connMethod, mHandler, bodyJson);
        Log.i("ChatActivity", "DELETE 요청: " + bodyJson);

        Intent chatListIntent = new Intent(ChatActivity.this, ChatListActivity.class);
        startActivity(chatListIntent);  // 방 리스트로 화면 전환
    }

    // 모든 참가자가 READY 상태인지 확인
    private void checkAllReady(){
        if (readyButton != null) {
            boolean allReady = true;
            for (Participant participant : participants) {
                if (!participant.isReady()) {
                    allReady = false;
                    break;
                }
            }
            if (this.userId.equals(hostId)) {  // this.userId로 참조
                readyButton.setEnabled(allReady);
                if (allReady) {
                    readyButton.setText("GO");
                    readyButton.setOnClickListener(v -> {
                        // 방장이 GO 버튼을 누르면 PaymentCompleteActivity로 이동
                        Intent payIntent = new Intent(ChatActivity.this, PaymentCompleteActivity.class);
                        startActivity(payIntent);  // PaymentCompleteActivity로 화면 전환
                    });
                } else {
                    readyButton.setText("WAITING");
                }
            }
        } else{
            Log.e("ChatActivity", "readyButton is null in checkAllReady");
        }
    }

    // READY 버튼 및 상태 갱신
    private void updateReadyButton(){
        if (this.userId.equals(hostId)) {  // 방장이면 GO 버튼
            readyButton.setText("GO");
            readyButton.setEnabled(false); // 기본적으로 비활성화, 모두 READY 상태가 되면 활성화
            checkAllReady();  // 방장이면 모든 참가자가 READY 상태인지 체크
        } else {
            // 참여자면 READY -> CANCEL 상태로 토글
            readyButton.setText(isReady ? "CANCEL" : "READY");  // 초기 상태에 따라 버튼 설정
            readyButton.setOnClickListener(v -> toggleReadyState());  // 클릭 시 READY 상태 토글
        }
    }

}
