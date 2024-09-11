package com.example.healwego;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Calendar;

public class CreateRoomActivity extends AppCompatActivity {

    private EditText editRoomTitle, editMinAge, editMaxAge;
    private Spinner timeSpinner;
    private RadioGroup radioGroupGender;
    private Button buttonCreateRoom, buttonCancel, buttonFindDestination;
    private String selectedTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.create_room);

        // 레이아웃 요소 초기화
        editRoomTitle = findViewById(R.id.editRoomTitle);
        timeSpinner = findViewById(R.id.timeSpinner);
        radioGroupGender = findViewById(R.id.radioGroupGender);
        editMinAge = findViewById(R.id.editMinAge);
        editMaxAge = findViewById(R.id.editMaxAge);
        buttonCreateRoom = findViewById(R.id.buttonCreateRoom);
        buttonCancel = findViewById(R.id.buttonCancel);
        buttonFindDestination = findViewById(R.id.buttonFindDestination);

        // Spinner에 시간 목록 설정
        ArrayList<String> availableTimes = getAvailableTimes();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.spinner_item, availableTimes);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        timeSpinner.setAdapter(adapter);

        // 기본 선택된 시간으로 첫 번째 항목 설정
        selectedTime = availableTimes.get(0);

        // 방 제목 입력 후 엔터를 누르면 키보드를 숨김
        editRoomTitle.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                    hideKeyboard();
                    return true;
                }
                return false;
            }
        });

        // 목적지 찾기 버튼 클릭 처리 (DestSelect 액티비티로 이동)
        buttonFindDestination.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(CreateRoomActivity.this, DestSelect.class);
                startActivity(intent);
            }
        });

        // 방 생성 버튼 클릭 처리
        buttonCreateRoom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (validateInput()) {
                    showConfirmationDialog();  // 설정된 정보를 보여주는 다이얼로그 호출
                }
            }
        });

        // 취소 버튼 클릭 처리
        buttonCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); // 액티비티 종료
            }
        });
    }

    // 방 생성 정보 확인 다이얼로그
    private void showConfirmationDialog() {
        // 방 제목 가져오기
        String roomTitle = editRoomTitle.getText().toString().trim();

        // 성별 필터 가져오기
        int selectedGenderId = radioGroupGender.getCheckedRadioButtonId();
        String genderFilter = "";
        if (selectedGenderId != -1) {
            RadioButton selectedRadioButton = findViewById(selectedGenderId);
            genderFilter = selectedRadioButton.getText().toString();
        }

        // 나이 필터 가져오기
        String minAgeStr = editMinAge.getText().toString().trim();
        String maxAgeStr = editMaxAge.getText().toString().trim();
        int minAge = Integer.parseInt(minAgeStr); // 최소 나이
        int maxAge = Integer.parseInt(maxAgeStr); // 최대 나이

        // 방 정보 구성
        String roomInfo = "방 제목: " + roomTitle + "\n출발 시간: " + selectedTime +
                "\n성별 필터: " + genderFilter + "\n나이 필터: " + minAge + " ~ " + maxAge;

        // 다이얼로그 생성
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("방 생성 정보 확인")
                .setMessage(roomInfo)
                .setPositiveButton("확인", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // ChatActivity로 이동하며 방 제목 전달
                        Intent intent = new Intent(CreateRoomActivity.this, ChatActivity.class);
                        intent.putExtra("roomTitle", roomTitle);  // 방 제목 전달
                        startActivity(intent);
                    }
                })
                .setNegativeButton("취소", null)  // 취소 버튼 클릭 시 아무 동작도 하지 않음
                .show();
    }

    // 입력값 검증 메소드
    private boolean validateInput() {
        String roomTitle = editRoomTitle.getText().toString().trim();
        if (roomTitle.isEmpty()) {
            Toast.makeText(this, "방 제목을 입력하세요.", Toast.LENGTH_SHORT).show();
            return false;
        }

        // 최소 나이 검증
        if (!validateMinAge()) {
            return false;
        }

        // 최대 나이 검증
        if (!validateMaxAge()) {
            return false;
        }

        // 최소 나이와 최대 나이 비교
        int minAge = Integer.parseInt(editMinAge.getText().toString().trim());
        int maxAge = Integer.parseInt(editMaxAge.getText().toString().trim());

        if (minAge > maxAge) {
            Toast.makeText(this, "최소 나이는 최대 나이보다 작아야 합니다.", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    // 최소 나이 검증 메소드
    private boolean validateMinAge() {
        String minAgeStr = editMinAge.getText().toString().trim();
        if (minAgeStr.isEmpty()) {
            Toast.makeText(this, "최소 나이를 입력하세요.", Toast.LENGTH_SHORT).show();
            return false;
        }

        int minAge = Integer.parseInt(minAgeStr);
        if (minAge < 20) {
            Toast.makeText(this, "최소 나이는 20세 이상이어야 합니다.", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    // 최대 나이 검증 메소드
    private boolean validateMaxAge() {
        String maxAgeStr = editMaxAge.getText().toString().trim();
        if (maxAgeStr.isEmpty()) {
            Toast.makeText(this, "최대 나이를 입력하세요.", Toast.LENGTH_SHORT).show();
            return false;
        }

        int maxAge = Integer.parseInt(maxAgeStr);
        if (maxAge > 100) {
            Toast.makeText(this, "최대 나이는 100세 이하여야 합니다.", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    // 현재 시간 이후의 정각 시간 목록을 반환하는 메소드
    private ArrayList<String> getAvailableTimes() {
        ArrayList<String> times = new ArrayList<>();

        // 현재 시간 가져오기
        Calendar calendar = Calendar.getInstance();
        int currentHour = calendar.get(Calendar.HOUR_OF_DAY);

        // 현재 시간 이후의 정각 시간을 리스트에 추가 (예: 17:00 ~ 23:00)
        for (int hour = currentHour + 1; hour <= 23; hour++) {
            String time = String.format("%02d:00", hour);
            times.add(time);
        }

        return times;
    }

    // 키보드를 숨기는 메소드
    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
}
