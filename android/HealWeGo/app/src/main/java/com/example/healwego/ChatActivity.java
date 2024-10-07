package com.example.healwego;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.navigation.NavigationView;

import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;

import java.lang.ref.WeakReference;
import java.util.Iterator;

import android.util.Pair;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

public class ChatActivity extends AppCompatActivity {

    // MQTT
    MqttAsyncClient mqttClient;
    private static final String BROKER_URL = "ssl://a3boaptn83mu7y-ats.iot.ap-northeast-2.amazonaws.com:8883";
    private String CLIENT_ID = "";

    private static final String CHAT_TOPIC = "chatting/";

    private String roomId;
    private String chatsString;

    private RecyclerView chatRecyclerView;
    private ChatAdapter chatAdapter;
    private ArrayList<Pair<String, Pair<String, String>>> chatMessages;  // 채팅 메시지 리스트
    private TextView chatTitle;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private RecyclerView participantRecyclerView;
    private ParticipantAdapter participantAdapter;
    private ArrayList<Participant> participants;  // 참여자 리스트
    private TextView totalAmountTextView;
    private Button readyButton;
    private ImageButton backToChatButton;

    private JSONObject participantsInfos;

    // 방장 여부 확인 (API 요청으로 바뀌어야 함)
    private boolean afterCreate = false;  // 방장이면 true로 설정
    private boolean isReady = false; // READY 상태 관리
    private String hostId;  // 방장 ID
    private String userId;  // 현재 사용자 ID
    private String myUserName; // 현재 사용자 userName

    private static final int MAX_RETRY_COUNT = 10; // 최대 재시도 횟수
    private int retryCount = 0; // 현재 재시도 횟수

    // API 요청을 위한 URL
    private String mURL = "https://18rc8r0oi0.execute-api.ap-northeast-2.amazonaws.com/healwego-stage/";

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
                chatActivity.handleParticipantsResponse(jsonString);
            }
        }
    }

    // WeakReference로 Activity에 대한 참조를 가진 MyHandler 객체
    private final ChatActivity.MyHandler mHandler = new ChatActivity.MyHandler(Looper.getMainLooper(), this); // MainLooper 전달


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chatting_page);  // 채팅 레이아웃 사용

        // MQTT 구독
        connectToMqtt();

        // 현재 사용자 ID 가져오기
        SharedPreferences sharedPref = getSharedPreferences("UserIDPrefs", Context.MODE_PRIVATE);
        userId = sharedPref.getString("userID", ""); // 값이 없으면 "defaultUsername" 사용
        CLIENT_ID = userId;


        // 뒤로가기 버튼을 처리하는 부분
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // DrawerLayout이 열려 있는지 확인
                if (drawerLayout.isDrawerOpen(navigationView)) {
                    // 내비게이션이 열려 있으면 닫기
                    drawerLayout.closeDrawer(navigationView);
                } else {
                    // 내비게이션이 닫혀 있으면 ChatListActivity로 이동
                    Intent intent = new Intent(ChatActivity.this, ChatListActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    finish();
                }
            }
        });


        // Intent에서 전달된 방 생성 여부 (isHost) 값을 가져옴
        Intent intent = getIntent();
        int resultIntent = intent.getIntExtra("result", 0);
        if(resultIntent == 1){
            // DELETE 성공 후 MQTT 메시지 전송
            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("User_ID", userId);
                jsonObject.put("option", "enter");
                jsonObject.put("message", userId);
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }

            sendMqttMessage(CHAT_TOPIC + roomId, jsonObject.toString());
        }
        int isNewEnter = intent.getIntExtra("isNewEnter", 0);
        String roomTitle = intent.getStringExtra("roomTitle");
        String usersInfo = intent.getStringExtra("usersInfo");
        hostId = intent.getStringExtra("hostId");
        roomId = intent.getStringExtra("roomId");
        Log.i("USER_ID", "roomId : " + roomId);
        afterCreate = intent.getBooleanExtra("isHost", false);

        if (isNewEnter == 2){
            chatsString = intent.getStringExtra("chats");
        }

        Log.i("ChatActiviy: ", "사용자 정보: " + usersInfo);

        // 방 제목 설정
        chatTitle = findViewById(R.id.chatTitle);
        if (roomTitle != null) {
            chatTitle.setText(roomTitle);
        }

        // RecyclerView 설정
        chatRecyclerView = findViewById(R.id.chatRecyclerView);
        chatMessages = new ArrayList<>();
        chatAdapter = new ChatAdapter(chatMessages, userId);
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        chatRecyclerView.setAdapter(chatAdapter);


        if (isNewEnter == 2 && chatsString != null) {
            try {
                JSONArray chats = new JSONArray(chatsString);

                // 채팅 기록을 chatMessages에 추가하고 어댑터에 알림
                for (int i = 0; i < chats.length(); i++) {
                    JSONArray chatItem = chats.getJSONArray(i);
                    JSONObject chatData = chatItem.getJSONObject(1);
                    String message = chatData.getString("message");
                    String username = chatData.getString("username");
                    String chatId = chatData.getString("User_ID");

                    // 원하는 형식으로 메시지 생성 (예: 시간 + 메시지)
                    String formattedMessage = message;

                    chatMessages.add(new Pair<>(chatId, new Pair<>(username, formattedMessage)));
                }
                chatAdapter.notifyDataSetChanged();


                // 마지막 메시지로 스크롤
                chatRecyclerView.scrollToPosition(chatMessages.size());  // 마지막 메시지로 스크롤


            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

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
                isReady = addParticipants(usersJson);
            } catch(JSONException e){
                Log.e("ChatActivity", "참여자 정보 처리 오류", e);
            }
        }

        // 정산 금액 설정
        totalAmountTextView = headerView.findViewById(R.id.totalAmountTextView);
        totalAmountTextView.setText("예상 결제 요금 로딩중...");

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
        if(isReady){
            readyButton.setText("CANCEL");
        }
        else{
            readyButton.setText("READY");
        }

        // 화살표 버튼 설정 (Drawer 닫기)
        backToChatButton = headerView.findViewById(R.id.backToChatButton);
        backToChatButton.setOnClickListener(v -> {
            drawerLayout.closeDrawer(navigationView);
        });

        // 나가기 버튼 설정
        ImageButton exitButton = headerView.findViewById(R.id.exitButton);
        exitButton.setOnClickListener(v -> {
            // ChatExitDialogFragment를 호출
            ChatExitDialogFragment dialogFragment = new ChatExitDialogFragment();
            dialogFragment.show(getSupportFragmentManager(), "ChatExitDialog");
        });

        ImageButton sendbtn = findViewById(R.id.sendButton);
        EditText usermsg = findViewById(R.id.messageInput);


        sendbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String userMessage = usermsg.getText().toString().trim();

                // 메시지가 비어있으면 전송하지 않음
                if (userMessage.isEmpty()) {
                    Toast.makeText(ChatActivity.this, "메시지를 입력하세요", Toast.LENGTH_SHORT).show();
                    return; // 메시지가 비어 있으면 더 이상 진행하지 않음
                }

                // 메시지 보낼 때마다 새로 생성
                JSONObject jsonObject = new JSONObject();
                String currentTime = getCurrentKoreanTime(new Date());

                try{
                    jsonObject.put("User_ID", userId);
                    jsonObject.put("option", "msg");
                    jsonObject.put("message", currentTime + " " + usermsg.getText().toString());
                    jsonObject.put("userName",myUserName);

                    sendMqttMessage(CHAT_TOPIC + roomId, jsonObject.toString());
                    usermsg.setText("");  // 메시지 보낸 후 입력 필드를 비웁니다.

                } catch(JSONException e){
                    return;
                }

            }
        });

    }

    @Override
    protected void onResume() {
        // 현재 사용자 ID 가져오기
        super.onResume();
        SharedPreferences sharedPref = getSharedPreferences("UserIDPrefs", Context.MODE_PRIVATE);
        userId = sharedPref.getString("userID", ""); // 값이 없으면 "defaultUsername" 사용
        CLIENT_ID = userId;
    }

    // addParticipant 메서드
    private boolean addParticipants(JSONObject usersJson) throws JSONException {
        boolean isUserReady = false;
        Iterator<String> keys = usersJson.keys();
        participants.clear();
        while (keys.hasNext()) {
            String key = keys.next();  // "userId"를 key로 사용
            JSONObject userObject = usersJson.getJSONObject(key);
            Log.i("participant", "참여자 정보: " + userObject);
            String userName = userObject.optString("username", "Unknown");
            boolean isReady = userObject.getInt("is_ready") == 1;  // 1이면 READY
            boolean isCurrentUser = key.equals(this.userId);  // key를 사용하여 현재 사용자인지 확인
            if(isCurrentUser && isReady){
                isUserReady = true;
            }
            boolean isHost = key.equals(hostId);  // 방장 확인

            String role = isHost ? "(방장)" : "";  // 방장일 경우 "방장" 역할 추가

            // afterCreate가 true이면 현재 사용자가 방장임을 표시
            if (afterCreate && isCurrentUser) {
                role = "(방장)";
            }

            if (isCurrentUser){
                myUserName = userName;
            }

            Log.i("ChatActivity", "Adding participant: " + key + ", Role: " + role + ", IsReady: " + isReady + ", IsCurrentUser: " + isCurrentUser);

            participants.add(new Participant(userName, role, isCurrentUser, isReady, key));  // key가 userId
        }
        Log.i("ChatActivity", "참여 인원: " + participants.size());

        participantAdapter.notifyDataSetChanged();
        return isUserReady;
    }


    private String getCurrentKoreanTime(Date date){
        // 한국 시간으로 변환하여 표시
        SimpleDateFormat koreanFormat = new SimpleDateFormat("ahh:mm", Locale.KOREA); // "a"는 오전/오후, "hh"는 12시간제
        koreanFormat.setTimeZone(TimeZone.getTimeZone("Asia/Seoul"));  // 한국 타임존 설정

        return koreanFormat.format(date);  // 한국 시간으로 포맷팅된 시간 반환
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

    @SuppressLint("SetTextI18n")
    private void handleParticipantsResponse(String response){
        try{
            if (response == null || response.isEmpty()) {
                Log.e("Chat", "Received null or empty response");
                return;
            }

            JSONObject responseObject = new JSONObject(response);

            String body = responseObject.optString("body", null);

            JSONObject bodyJson = new JSONObject(body);
            Log.i("Chat", bodyJson.toString());


            // users 밖에 있는 longitude와 latitude 값을 destLat, destLon에 저장
            String destLat = bodyJson.getString("latitude");
            String destLon = bodyJson.getString("longitude");

            // bodyObject에서 "users" 객체 추출
            JSONObject users = bodyJson.getJSONObject("users");

            // users의 keySet을 이용해 모든 사용자 정보 순회
            Iterator<String> keys = users.keys();

            while (keys.hasNext()) {
                String userEmail = keys.next();  // 각 사용자의 이메일 (key)
                JSONObject userDetails = users.getJSONObject(userEmail);  // 사용자 세부 정보 (value)

                // 사용자 세부 정보 출력
                String isReady = userDetails.getString("is_ready");
                String username = userDetails.getString("username");
                String latitude = userDetails.getString("latitude");
                String longitude = userDetails.getString("longitude");
                String timestamp = userDetails.getString("timestamp");

                Log.i("UserDetails", "User: " + userEmail + ", username: " + username +
                        ", is_ready: " + isReady + ", latitude: " + latitude +
                        ", longitude: " + longitude + ", timestamp: " + timestamp);

                if(userEmail.equals(userId)){
                    double dist = haversine(Double.parseDouble(latitude),Double.parseDouble(longitude),Double.parseDouble(destLat),Double.parseDouble(destLon));
                    int value = 3000;
                    if(dist>1.0){
                        int offset = (int)(dist*10)/5;
                        value += offset*100;
                    }
                    NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.US);
                    String formattedPrice = numberFormat.format(value);

                    totalAmountTextView.setText("예상 결제 요금 : "+formattedPrice+"원");
                }
            }

            // hostId및 ready버튼 업데이트
            hostId = bodyJson.getString("master_ID");
            updateReadyButton();

            //여기 와서도 roomId가 null이면 다시 구독
            if(roomId==null){
                roomId = bodyJson.getString("Rooms_ID");
                subscribeToTopic(CHAT_TOPIC + roomId);
            }
            participantsInfos = bodyJson.optJSONObject("users");
            Log.i("Chat", participantsInfos.toString());

            //방 참여자 목록 갱신
            addParticipants(participantsInfos);
        } catch(JSONException e){
            Log.i("Parti Error", "Error");
            return;
        }
    }

    public static double haversine(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371; // 지구의 반지름 (단위: km)
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c; // 결과 거리 (단위: km)
    }

    // 처음 들어왔을 때 API 요청 함수
    private void sendPatchRequest(){
        // API 유형
        String connMethod = "PATCH";
        String apiURL = mURL + "room/in";

        JSONObject body = new JSONObject();

        try{
            body.put("Method", connMethod);
            body.put("User_ID", this.userId);
        } catch(JSONException e){
            Log.e("ChatActivity", "PATCH JSON 생성 오류");
            return;
        }

        String bodyJson = body.toString();

        // API 요청 함수
        // PATCH 위한 핸들러 생성
        ApiRequestHandler.getJSON(apiURL, connMethod, new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                String response = (String) msg.obj;

                try {
                    JSONObject responseObject = new JSONObject(response);
                    int statusCode = responseObject.getInt("statusCode");

                    // DELETE 요청 성공 시 실행할 작업
                    if (statusCode == 200) {
                        Log.i("ChatActivity", "PATCH 요청 성공");
                        handleParticipantsResponse(response);
                    } else {
                        Log.e("ChatActivity", "PATCH 요청 실패: " + statusCode);
                        retryRequest();
                    }
                } catch (JSONException e) {
                    Log.e("ChatActivity", "PATCH 응답 처리 오류", e);
                    retryRequest();
                }
            }

        }, bodyJson);

        Log.i("ChatActivity", "PATCH 요청: " + bodyJson);
    }

    // 재시도 로직
    private void retryRequest() {
        if (retryCount < MAX_RETRY_COUNT) {
            retryCount++;
            Log.i("ChatActivity", "PATCH 요청 재시도 중... " + retryCount + "/" + MAX_RETRY_COUNT);
            sendPatchRequest(); // 재시도
        } else {
            Log.e("ChatActivity", "PATCH 요청 재시도 한도 초과");
            // 재시도 실패 시 추가 처리 로직 (예: 사용자에게 알림, UI 업데이트 등)
        }
    }
    // READY 버튼 및 상태 갱신
    private void updateReadyButton(){
        if (afterCreate || this.userId.equals(hostId)) {  // 방장이면 GO 버튼
            readyButton.setText("GO");
            readyButton.setBackgroundColor(getResources().getColor(R.color.darkteal)); // lightteal 색상
            readyButton.setEnabled(true); // 기본적으로 활성화, DB에서 처리
            readyButton.setOnClickListener(v -> {
                // 방장이 GO 버튼을 누르면 PaymentCompleteActivity로 이동
                String connMethod = "PATCH";
                String apiURL = mURL + "user/go";

                JSONObject body = new JSONObject();

                try{
                    body.put("Method", connMethod);
                    body.put("User_ID", this.userId);
                } catch(JSONException e){
                    Log.e("ChatActivity", "JSON 생성 오류");
                    return;
                }

                String bodyJson = body.toString();

                // API 요청 함수
                // 처리를 위한 핸들러 생성
                ApiRequestHandler.getJSON(apiURL, connMethod, new Handler(Looper.getMainLooper()) {
                    @Override
                    public void handleMessage(Message msg) {
                        String response = (String) msg.obj;

                        try {
                            JSONObject responseObject = new JSONObject(response);
                            String body = responseObject.getString("body");
                            JSONObject optionObject = new JSONObject(body);
                            String option = optionObject.getString("option");

                            if (option.equals("go")){
                                // mqtt 쏴야하고
                                // 성공 후 MQTT 메시지 전송
                                JSONObject jsonObject = new JSONObject();
                                try {
                                    jsonObject.put("User_ID", userId);
                                    jsonObject.put("option", "go");
                                    jsonObject.put("message", "");
                                } catch (JSONException e) {
                                    throw new RuntimeException(e);
                                }
                                sendMqttMessage(CHAT_TOPIC + roomId, jsonObject.toString());
                                // intent 넘기고
                                Intent payIntent = new Intent(ChatActivity.this, PaymentCompleteActivity.class);
                                startActivity(payIntent);  // PaymentCompleteActivity로 화면 전환
                            }else{
                                Toast.makeText(ChatActivity.this, "참여자 전원이 READY 해야 합니다.", Toast.LENGTH_SHORT).show();
                            }
                        } catch (JSONException e) {
                            Log.e("ChatActivity", "응답 처리 오류", e);
                        }
                    }
                }, bodyJson);
            });
        } else {
            // 참여자면 READY -> CANCEL 상태로 토글 (Callback해야 함)
            readyButton.setOnClickListener(v -> {
                toggleReadyState();
                if (isReady) {
                    readyButton.setText("CANCEL");
                    readyButton.setBackgroundColor(getResources().getColor(R.color.lightteal)); // teal 색상
                } else {
                    readyButton.setText("READY");
                    readyButton.setBackgroundColor(getResources().getColor(R.color.teal)); // teal 색상
                }
                // 메시지 보낼 때마다 새로 생성
                JSONObject jsonObject = new JSONObject();

                try{
                    jsonObject.put("User_ID", userId);
                    jsonObject.put("option", "ready");
                    if (isReady){
                        jsonObject.put("message", "READY");
                    }
                    else{
                        jsonObject.put("message", "CANCEL");
                    }

                    sendMqttMessage(CHAT_TOPIC + roomId, jsonObject.toString());
                } catch(JSONException e){
                    Log.i("MQTT", "ready" + e);
                    return;
                }

            });  // 클릭 시 READY 상태 토글
        }
    }

    private void connectToMqtt() {
        try {
            mqttClient = new MqttAsyncClient(BROKER_URL, CLIENT_ID, new MemoryPersistence());
            MqttConnectOptions options = new MqttConnectOptions();
            options.setSocketFactory(getSocketFactory());

            options.setKeepAliveInterval(60); // 초 단위, 60초마다 핑 메시지 전송

            mqttClient.connect(options, null, new org.eclipse.paho.client.mqttv3.IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    System.out.println("Connected to AWS IoT Core");
                    subscribeToTopic(CHAT_TOPIC + roomId);

                    // 내비게이션 갱신 메시지 보냄
                    try{
                        JSONObject jsonObject = new JSONObject();

                        jsonObject.put("User_ID", userId);
                        jsonObject.put("option", "enter");
                        jsonObject.put("message", "");

                        sendMqttMessage(CHAT_TOPIC + roomId, jsonObject.toString());
                    } catch(JSONException e){
                        Log.i("MQTT 갱신", "실패");
                        return;
                    }
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.e("MQTT", "Failed to connect to AWS IoT Core", exception);
                    exception.printStackTrace();
                }
            });

            // 메시지 수신 콜백 추가
            mqttClient.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    // 연결이 끊겼을 때 처리할 내용
                    Log.e("MQTT", "MQTT Connection Lost", cause);
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    // Chat 토픽 처리

                    if (topic.equals(CHAT_TOPIC+ roomId)) {
                        // 콜백 함수 자리 또는 여기에 동작 구현
                        // MQTT 메시지 수신 시 처리
                        runOnUiThread(() -> {
                            try {
                                receiveMessageCallback(message);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        });
                    }
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    Log.d("MQTT", "Message delivery complete");
                }


            });
        } catch (MqttException e) {
            Log.e("MQTT", "Failed to connect to MQTT broker", e);
            e.printStackTrace();
        }
    }

    private void subscribeToTopic(String topic) {
        try {
            mqttClient.subscribe(topic, 1);  // QoS 1로 토픽 구독
            Log.d("MQTT", "Subscribed to topic: " + topic);
        } catch (MqttException e) {
            Log.e("MQTT", "Subscription error", e);
            e.printStackTrace();
        }
    }


    // 메시지 받을 떄 함수
    private void receiveMessageCallback(MqttMessage rmessage) throws JSONException {
        String msg = rmessage.toString();
        JSONObject jsonObject = new JSONObject(msg);

        Log.i("MQTT", "수신: " + msg);

        String senderId = jsonObject.getString("User_ID");
        String option = jsonObject.getString("option");
        String message = jsonObject.getString("message");

        if (option.equals("msg")) {
            String userName = jsonObject.getString("userName");
            // 상대방 메시지를 받았을 경우 닉네임도 추가
            if (!senderId.equals(userId)) {
                // 상대방 메시지일 경우 상대방 닉네임 추가
                chatMessages.add(new Pair<>(senderId, new Pair<>(userName, message)));
            } else {
                // 사용자 본인이 보낸 메시지일 경우, 오른쪽에 표시되도록 처리
                chatMessages.add(new Pair<>(senderId, new Pair<>("나", message)));
            }

            // 어댑터에 데이터가 변경되었음을 알림 (새로 추가된 메시지를 반영)
            chatAdapter.notifyItemInserted(chatMessages.size());

            // RecyclerView를 마지막 메시지로 스크롤
            chatRecyclerView.scrollToPosition(chatMessages.size());
        }
        else if (option.equals("ready")){ // ready
            // 참여자 READY 상태 업데이트
            if (message.equals("READY")){
                Log.i("MQTT", "READY");
                participantAdapter.updateParticipantReadyState(senderId, true);
            }
            else{
                Log.i("MQTT", "CANCEL");
                participantAdapter.updateParticipantReadyState(senderId, false);

            }
        }
        else if (option.equals("enter")){
            Log.i("MQTT 방장", "들어옴?");
            if(!message.equals(userId)) {
                Log.i("MQTT 방장", "ㅇㅇ들어옴");
                sendPatchRequest();  // API 요청
            }
        } else if (option.equals("go")) {
            Intent payIntent = new Intent(ChatActivity.this, PaymentCompleteActivity.class);
            startActivity(payIntent);  // PaymentCompleteActivity로 화면 전환
        }

    }

    // 메시지 보낼 떄 함수
    private void sendMqttMessage(String topic, String message) {
        try {
            MqttMessage mqttMessage = new MqttMessage();
            mqttMessage.setPayload(message.getBytes());
            mqttClient.publish(topic, mqttMessage);
            System.out.println("MQTT Message sent to topic '" + topic + "': " + message);
        } catch (MqttException e) {
            Log.e("MQTT", "Error sending");
            e.printStackTrace();
        }
    }


    private SSLSocketFactory getSocketFactory() {
        try {
            // CA 인증서 로드
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            InputStream caInput = getAssets().open("AmazonRootCA1.pem");
            X509Certificate caCert = (X509Certificate) cf.generateCertificate(caInput);

            // 클라이언트 인증서 로드
            InputStream crtInput = getAssets().open("certificate.pem.crt");
            X509Certificate clientCert = (X509Certificate) cf.generateCertificate(crtInput);

            // Private Key 로드
            PrivateKey privateKey = loadPrivateKey();

            // KeyStore 생성
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(null, null);
            keyStore.setCertificateEntry("caCert", caCert);
            keyStore.setCertificateEntry("clientCert", clientCert);
            keyStore.setKeyEntry("privateKey", privateKey, "password".toCharArray(), new java.security.cert.Certificate[]{clientCert});

            // TrustManagerFactory 및 KeyManagerFactory 초기화
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(keyStore);

            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(keyStore, "password".toCharArray());

            // SSLContext 초기화
            SSLContext sslContext = SSLContext.getInstance("TLSv1.3");
            sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

            return sslContext.getSocketFactory();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private PrivateKey loadPrivateKey() throws Exception {
        PemReader pemReader = new PemReader(new InputStreamReader(getAssets().open("private.pem.key")));
        PemObject pemObject = pemReader.readPemObject();
        byte[] keyBytes = pemObject.getContent();
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA"); // 또는 "EC" (키 타입에 따라 다름)
        return keyFactory.generatePrivate(keySpec);
    }
}
