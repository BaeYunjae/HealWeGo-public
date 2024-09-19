package com.example.healwego;

import android.content.DialogInterface;
import android.content.Intent; // Intent 사용
import android.content.SharedPreferences; // SharedPreferences 사용
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class ChatListActivity extends AppCompatActivity {

    // 필터 상태 저장을 위한 SharedPreferences
    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "FilterPreferences";
    private static final String SELECTED_FILTER_KEY = "selected_filter";

    // 테마 버튼
    private Button buttonThemeHealing, buttonThemeExtreme, buttonThemeMeeting, buttonThemeEating;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // chat_list.xml 레이아웃 설정
        setContentView(R.layout.chat_list);

        // SharedPreferences 초기화
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // 테마 버튼 초기화
        buttonThemeHealing = findViewById(R.id.healing);
        buttonThemeExtreme = findViewById(R.id.extreme);
        buttonThemeMeeting = findViewById(R.id.meeting);
        buttonThemeEating = findViewById(R.id.eating);

        // 테마 버튼 클릭 리스너 설정
        buttonThemeHealing.setOnClickListener(v -> onThemeButtonClicked(buttonThemeHealing));
        buttonThemeExtreme.setOnClickListener(v -> onThemeButtonClicked(buttonThemeExtreme));
        buttonThemeMeeting.setOnClickListener(v -> onThemeButtonClicked(buttonThemeMeeting));
        buttonThemeEating.setOnClickListener(v -> onThemeButtonClicked(buttonThemeEating));

        // ImageButton 클릭 시 필터 팝업창 띄우기
        ImageButton filterButton = findViewById(R.id.filterButton);
        filterButton.setOnClickListener(v -> showFilterDialog());

        // 방 선택 시 정보로 ChatActivity를 그린다
        // 첫 번째 방 선택 시 ChatActivity로 이동
        Button chatroom1 = findViewById(R.id.chatroom1);
        chatroom1.setOnClickListener(v -> {
            Intent intent = new Intent(ChatListActivity.this, ChatActivity.class);
            intent.putExtra("roomTitle", "을왕리 해수욕장");
            intent.putExtra("departure", "서울역");
            intent.putExtra("time", "오후 3시");
            intent.putExtra("participants", "2/4");
            intent.putExtra("tags", "#힐링 #여성만");
            startActivity(intent);  // ChatActivity로 이동
        });

        // 두 번째 방 선택 시 ChatActivity로 이동
        Button chatroom2 = findViewById(R.id.chatroom2);
        chatroom2.setOnClickListener(v -> {
            Intent intent = new Intent(ChatListActivity.this, ChatActivity.class);
            intent.putExtra("roomTitle", "익스트림 방");
            intent.putExtra("departure", "서울역");
            intent.putExtra("time", "오후 3시");
            intent.putExtra("participants", "3/4");
            intent.putExtra("tags", "#익스트림 #20대만");
            startActivity(intent);  // ChatActivity로 이동
        });

        // 세 번째 방 선택 시 ChatActivity로 이동
        Button chatroom3 = findViewById(R.id.chatroom3);
        chatroom3.setOnClickListener(v -> {
            Intent intent = new Intent(ChatListActivity.this, ChatActivity.class);
            intent.putExtra("roomTitle", "혼자 가기");
            intent.putExtra("departure", "서울역");
            intent.putExtra("time", "오후 3시");
            intent.putExtra("participants", "혼자 가기");
            intent.putExtra("tags", "#먹부림");
            startActivity(intent);  // ChatActivity로 이동
        });

        // 방 만들기 버튼 클릭 시 CreateRoomActivity로 이동
        Button createRoomButton = findViewById(R.id.wantCreateButton);
        createRoomButton.setOnClickListener(v -> {
            Intent intent = new Intent(ChatListActivity.this, CreateRoomActivity.class);
            startActivity(intent);  // CreateRoomActivity로 이동
        });
    }

    // 테마 버튼이 클릭될 때 호출되는 메소드
    private void onThemeButtonClicked(Button clickedButton) {
        // 모든 테마 버튼 선택 해제
        buttonThemeHealing.setSelected(false);
        buttonThemeExtreme.setSelected(false);
        buttonThemeMeeting.setSelected(false);
        buttonThemeEating.setSelected(false);

        // 클릭된 테마 버튼을 선택 상태로 설정
        clickedButton.setSelected(true);
    }


    private void showFilterDialog() {
        // 필터 옵션 배열
        final String[] filters = {"성별무관", "남성만", "여성만"};

        // 이전에 저장된 필터를 불러오기
        String savedFilter = sharedPreferences.getString(SELECTED_FILTER_KEY, "성별무관");
        int checkedItem = getCheckedItem(savedFilter);

        // 다이얼로그 생성
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("필터 선택")
                .setSingleChoiceItems(filters, checkedItem, (dialog, which) -> {
                    // 선택한 필터에 따라 동작 처리
                    String selectedFilter = filters[which];
                    Toast.makeText(ChatListActivity.this, selectedFilter + " 필터가 선택되었습니다.", Toast.LENGTH_SHORT).show();

                    // 선택한 필터 저장
                    saveSelectedFilter(selectedFilter);
                })
                .setPositiveButton("확인", (dialog, which) -> {
                    // 확인 버튼 클릭 시 추가 동작 없음 (필터 이미 적용됨)
                })
                .setNegativeButton("취소", null);

        // 다이얼로그 보여주기
        builder.show();
    }

    // 선택된 필터 저장 메소드
    private void saveSelectedFilter(String filter) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(SELECTED_FILTER_KEY, filter);
        editor.apply(); // 저장
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
}
