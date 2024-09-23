package com.example.healwego;

import android.content.Intent; // Intent 사용
import android.content.SharedPreferences; // SharedPreferences 사용
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.amazonaws.mobile.client.AWSMobileClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class ChatListActivity extends AppCompatActivity {

    private RecyclerView recyclerViewRooms;
    private RoomListAdapter roomListAdapter;

    // 필터 상태 저장을 위한 SharedPreferences
    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "FilterPreferences";
    private static final String SELECTED_FILTER_KEY = "selected_filter";
    private static final String SELECTED_THEME_KEY = "selected_theme";

    // UI 요소 - 테마 버튼, 방 없음
    private Button buttonThemeHealing, buttonThemeExtreme, buttonThemeMeeting, buttonThemeEating;
    private TextView noRoomTextView;

    // 선택된 필터 값 - 기본값은 성별무관, 모든 테마
    private String selectedGender = "성별무관"; // 남성만(0), 여성만(1), 성별 무관(2)
    private String selectedTheme = "";

    // API 요청을 위한 URL
    private String mURL = "https://e2fqrjfyj9.execute-api.ap-northeast-2.amazonaws.com/healwego-stage/";
    private String connMethod;

    private final ChatListActivity.MyHandler mHandler = new ChatListActivity.MyHandler(Looper.getMainLooper(), this);

    // MyHandler를 static으로 선언하여 메모리 누수를 방지하고, WeakReference로 액티비티 참조
    private static class MyHandler extends Handler {
        private final WeakReference<ChatListActivity> weakReference;

        public MyHandler(Looper looper, ChatListActivity chatListActivity) {
            super(looper);
            weakReference = new WeakReference<>(chatListActivity);
        }

        @Override
        public void handleMessage(Message msg) {
            ChatListActivity chatListActivity = weakReference.get();
            if (chatListActivity != null && !chatListActivity.isFinishing()) {
                String jsonString = (String) msg.obj;
                Toast.makeText(chatListActivity.getApplicationContext(), jsonString, Toast.LENGTH_LONG).show();
                Log.i("ChatListActivity", "응답: " + jsonString);
                chatListActivity.handleRoomResponse(jsonString);
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chat_list);

        recyclerViewRooms = findViewById(R.id.recyclerViewRooms);
        recyclerViewRooms.setLayoutManager(new LinearLayoutManager(this));

        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        selectedGender = sharedPreferences.getString(SELECTED_FILTER_KEY, "성별무관"); // 기본값 성별무관

        buttonThemeHealing = findViewById(R.id.healing);
        buttonThemeExtreme = findViewById(R.id.extreme);
        buttonThemeMeeting = findViewById(R.id.meeting);
        buttonThemeEating = findViewById(R.id.eating);
        noRoomTextView = findViewById(R.id.noRoomTextView);
        noRoomTextView.setVisibility(View.GONE);

        buttonThemeHealing.setOnClickListener(v -> onThemeButtonClicked(buttonThemeHealing, "힐링"));
        buttonThemeExtreme.setOnClickListener(v -> onThemeButtonClicked(buttonThemeExtreme, "익스트림"));
        buttonThemeMeeting.setOnClickListener(v -> onThemeButtonClicked(buttonThemeMeeting, "만남"));
        buttonThemeEating.setOnClickListener(v -> onThemeButtonClicked(buttonThemeEating, "먹부림"));

        // 필터 버튼 설정
        ImageButton filterButton = findViewById(R.id.filterButton);
        filterButton.setOnClickListener(v -> showFilterDialog());

        // 방 목록 요청 (기본 필터 설정 후 호출)
        requestRoomList();

        // 방 만들기 버튼 설정
        Button createRoomButton = findViewById(R.id.wantCreateButton);
        createRoomButton.setOnClickListener(v -> {
            Intent intent = new Intent(ChatListActivity.this, CreateRoomActivity.class);
            startActivity(intent);
        });
    }

    // 방 목록 요청 메소드
    private void requestRoomList(){
        String themeFilter = selectedTheme.isEmpty() ? "" : selectedTheme;

        JSONObject body = new JSONObject();
        String userId = AWSMobileClient.getInstance().getUsername();

        connMethod = "PATCH";
        mURL = "https://e2fqrjfyj9.execute-api.ap-northeast-2.amazonaws.com/healwego-stage/" + "room/list";

        try{
            body.put("Method", connMethod);
            body.put("User_ID", userId);
            body.put("gender", getGenderFilterValue());
            body.put("theme", themeFilter);
        }catch(JSONException e){
            Log.e("ChatListActivity", "JSON 생성 오류", e);
            return;
        }

        String bodyJson = body.toString();
        Log.i("ChatListActivity", "요청 바디: " + bodyJson);

        ApiRequestHandler.getJSON(mURL, connMethod, mHandler, bodyJson);
    }

    // gender 값을 반환하는 메서드
    private int getGenderFilterValue() {
        switch (selectedGender) {
            case "남성만":
                return 0;
            case "여성만":
                return 1;
            case "성별무관":
            default:
                return 2;
        }
    }

    // 테마 버튼이 클릭될 때 호출되는 메소드
    private void onThemeButtonClicked(Button clickedButton, String theme) {
        if (clickedButton.isSelected()) {
            clickedButton.setSelected(false);
            selectedTheme = "";
        } else {
            buttonThemeHealing.setSelected(false);
            buttonThemeExtreme.setSelected(false);
            buttonThemeMeeting.setSelected(false);
            buttonThemeEating.setSelected(false);

            clickedButton.setSelected(true);
            selectedTheme = theme;
        }

        requestRoomList();
    }

    // 필터 다이얼로그
    private void showFilterDialog() {
        final String[] filters = {"성별무관", "남성만", "여성만"};

        // SharedPreferences에서 저장된 필터를 불러옵니다.
        String savedFilter = sharedPreferences.getString(SELECTED_FILTER_KEY, "성별무관");
        int checkedItem = getCheckedItem(savedFilter);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("필터 선택")
                .setSingleChoiceItems(filters, checkedItem, (dialog, which) -> {
                    String selectedFilter = filters[which];
                    Toast.makeText(ChatListActivity.this, selectedFilter + " 필터가 선택되었습니다.", Toast.LENGTH_SHORT).show();

                    // 선택한 필터를 저장하고 업데이트
                    selectedGender = selectedFilter; // selectedGender 업데이트
                    saveSelectedFilter(selectedFilter);

                    requestRoomList();  // 필터 적용 후 목록 갱신
                })
                .setPositiveButton("확인", (dialog, which) -> {})
                .setNegativeButton("취소", null);

        builder.show();
    }


    // 선택된 필터 저장 메소드
    private void saveSelectedFilter(String filter) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(SELECTED_FILTER_KEY, filter);
        editor.apply();
    }

    // 저장된 필터에 맞는 체크 항목 반환 메소드
    private int getCheckedItem(String filter) {
        switch (filter) {
            case "남성만":
                return 1;
            case "여성만":
                return 2;
            case "성별무관":
            default:
                return 0;
        }
    }

    // 방 목록 응답 처리 메소드
    private void handleRoomResponse(String response) {
        List<Room> rooms = parseRoomsFromResponse(response);

        if (rooms.isEmpty()) {
            noRoomTextView.setVisibility(View.VISIBLE);
            recyclerViewRooms.setVisibility(View.GONE);
        } else {
            noRoomTextView.setVisibility(View.GONE);
            recyclerViewRooms.setVisibility(View.VISIBLE);

            roomListAdapter = new RoomListAdapter(this, rooms);
            recyclerViewRooms.setAdapter(roomListAdapter);
        }
    }

    // 응답에서 방 목록 파싱
    private List<Room> parseRoomsFromResponse(String response) {
        List<Room> rooms = new ArrayList<>();
        try {
            JSONObject jsonResponse = new JSONObject(response);
            JSONArray roomArray = new JSONArray(jsonResponse.getString("body"));

            if (jsonResponse.has("errorMessage")) {
                String errorMessage = jsonResponse.getString("errorMessage");
                Log.e("ChatListActivity", "서버 오류: " + errorMessage);
                return rooms;
            }

            for (int i = 0; i < roomArray.length(); i++) {
                JSONObject roomObject = roomArray.getJSONObject(i);

                String roomId = roomObject.getString("Rooms_ID");
                String roomName = roomObject.getString("roomname");
                String theme = roomObject.getString("theme");
                String locName = roomObject.getString("Loc_name");
                int numUsers = roomObject.getInt("num");

                int genderValue = roomObject.getInt("gender");
                String gender;
                if (genderValue == 0) {
                    gender = "남성만";
                } else if (genderValue == 1) {
                    gender = "여성만";
                } else {
                    gender = "성별무관";
                }

                if (filterRoomByGender(genderValue)) {
                    Room room = new Room(roomId, roomName, theme, locName, numUsers, gender);
                    rooms.add(room);
                }
            }
        } catch (JSONException e) {
            Log.e("ChatListActivity", "방 목록 파싱 오류", e);
        }
        return rooms;
    }

    // gender 필터링 로직
    private boolean filterRoomByGender(int genderValue) {
        int genderFilterValue = getGenderFilterValue();
        if (genderFilterValue == 2) {
            // 성별무관일 경우 모든 gender(0, 1, 2) 포함
            return true;
        }
        // 남성만(0) 또는 여성만(1)일 경우 gender가 일치하는 방만 포함
        return genderFilterValue == genderValue;
    }
}

