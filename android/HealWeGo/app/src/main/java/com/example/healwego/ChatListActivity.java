package com.example.healwego;

import android.content.DialogInterface;
import android.content.Intent; // Intent를 사용하기 위해 추가
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class ChatListActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // chat_list.xml 레이아웃 설정
        setContentView(R.layout.chat_list);

        // ImageButton 클릭 시 필터 팝업창 띄우기
        ImageButton filterButton = findViewById(R.id.filterButton);
        filterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showFilterDialog();
            }
        });

        // 방 생성 버튼 클릭 시 CreateRoomActivity로 이동
        Button createRoomButton = findViewById(R.id.wantCreateButton);
        createRoomButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // CreateRoomActivity로 이동하는 Intent 생성
                Intent intent = new Intent(ChatListActivity.this, CreateRoomActivity.class);
                startActivity(intent); // CreateRoomActivity로 전환
            }
        });
    }

    private void showFilterDialog() {
        // 필터 옵션 배열
        final String[] filters = {"힐링", "익스트림", "먹부림"};

        // 다이얼로그 생성
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("필터 선택")
                .setItems(filters, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // 선택한 필터에 맞는 동작 처리
                        String selectedFilter = filters[which];
                        Toast.makeText(ChatListActivity.this, selectedFilter + " 필터가 선택되었습니다.", Toast.LENGTH_SHORT).show();

                        // 선택한 필터에 따른 추가 처리 로직을 여기에 작성할 수 있습니다.
                        applyFilter(selectedFilter);
                    }
                });

        // 다이얼로그 보여주기
        builder.show();
    }

    // 선택된 필터에 따라 필터 적용
    private void applyFilter(String filter) {
        // 필터 적용 로직 작성 (필요에 맞게 구현 가능)
        if (filter.equals("힐링")) {
            // "힐링" 필터 선택 시 처리
        } else if (filter.equals("익스트림")) {
            // "익스트림" 필터 선택 시 처리
        } else if (filter.equals("먹부림")) {
            // "먹부림" 필터 선택 시 처리
        }
    }
}
