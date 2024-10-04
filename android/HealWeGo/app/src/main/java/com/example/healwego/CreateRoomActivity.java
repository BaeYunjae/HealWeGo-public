package com.example.healwego;

import static android.content.ContentValues.TAG;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.amazonaws.mobile.client.AWSMobileClient;

import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.lang.ref.WeakReference;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Objects;
import java.util.UUID;

public class CreateRoomActivity extends AppCompatActivity {

    private EditText editRoomTitle, editMinAge, editMaxAge;
    private Spinner timeSpinner;
    private RadioGroup radioGroupGender;
    private Button buttonCreateRoom, buttonCancel, buttonFindDestination;
    private String selectedTime;
    private RadioButton radioAll, radioMale, radioFemale;
    private Button buttonThemeHealing, buttonThemeExtreme, buttonThemeFood, buttonThemeMeeting;
    private String selectedTheme = "힐링"; // 선택된 테마 정보를 저장하는 변수

    // 목척지 찾기 버튼에서 얻어야 하는 값, 기본값은 일단 "서울"
    private String latitude = "37.5665";
    private String longitude = "126.9780";
    private String locationName = "서울";

    // MyHandler를 static으로 선언하여 메모리 누수를 방지하고, WeakReference로 액티비티 참조
    private static class MyHandler extends Handler {
        private final WeakReference<CreateRoomActivity> weakReference;

        // Deprecated 경고를 해결하기 위해 Looper를 명시적으로 받도록 수정
        public MyHandler(Looper looper, CreateRoomActivity createRoomActivity) {
            super(looper);  // 명시적으로 Looper를 전달
            weakReference = new WeakReference<>(createRoomActivity);
        }

        @Override
        public void handleMessage(Message msg) {
            CreateRoomActivity createRoomActivity = weakReference.get();
            if (createRoomActivity != null && !createRoomActivity.isFinishing()) {
                // 액티비티가 여전히 존재하는 경우에만 작업 수행
                String jsonString = (String) msg.obj;
                Toast.makeText(createRoomActivity.getApplicationContext(), jsonString, Toast.LENGTH_LONG).show();
                Log.i("CreateRoomActivity", "응답: " + jsonString);
            }
        }
    }

    // WeakReference로 Activity에 대한 참조를 가진 MyHandler 객체
    private final CreateRoomActivity.MyHandler mHandler = new CreateRoomActivity.MyHandler(Looper.getMainLooper(), this); // MainLooper 전달


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
        radioAll = findViewById(R.id.radioAll);
        radioMale = findViewById(R.id.radioMale);
        radioFemale = findViewById(R.id.radioFemale);

        // 테마 버튼 초기화
        buttonThemeHealing = findViewById(R.id.buttonThemeHealing);
        buttonThemeExtreme = findViewById(R.id.buttonThemeExtreme);
        buttonThemeFood = findViewById(R.id.buttonThemeFood);
        buttonThemeMeeting = findViewById(R.id.buttonThemeMeeting);

        // 처음 실행 시 "힐링" 테마를 기본 선택 상태로 설정
        buttonThemeHealing.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.darkteal));
        buttonThemeHealing.setTextColor(ContextCompat.getColor(CreateRoomActivity.this, android.R.color.white));

        // 클릭했을 때 선택된 버튼의 색깔을 바꾸고 나머지는 기본 색으로 설정
        View.OnClickListener themeButtonClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 모든 버튼의 색을 초기화 (기본 색상)
                buttonThemeHealing.setBackgroundTintList(ContextCompat.getColorStateList(CreateRoomActivity.this, R.color.lightteal));
                buttonThemeHealing.setTextColor(ContextCompat.getColor(CreateRoomActivity.this, android.R.color.black));
                buttonThemeExtreme.setBackgroundTintList(ContextCompat.getColorStateList(CreateRoomActivity.this, R.color.lightteal));;
                buttonThemeExtreme.setTextColor(ContextCompat.getColor(CreateRoomActivity.this, android.R.color.black));
                buttonThemeFood.setBackgroundTintList(ContextCompat.getColorStateList(CreateRoomActivity.this, R.color.lightteal));;
                buttonThemeFood.setTextColor(ContextCompat.getColor(CreateRoomActivity.this, android.R.color.black));
                buttonThemeMeeting.setBackgroundTintList(ContextCompat.getColorStateList(CreateRoomActivity.this, R.color.lightteal));;
                buttonThemeMeeting.setTextColor(ContextCompat.getColor(CreateRoomActivity.this, android.R.color.black));
                // 클릭된 버튼의 색깔을 darkteal로 변경
                v.setBackgroundTintList(ContextCompat.getColorStateList(CreateRoomActivity.this, R.color.darkteal));
                ((TextView) v).setTextColor(ContextCompat.getColor(CreateRoomActivity.this, android.R.color.white));

                // 선택된 테마에 따라 selectedTheme 값을 업데이트
                if (v.getId() == R.id.buttonThemeHealing) {
                    selectedTheme = "힐링";
                } else if (v.getId() == R.id.buttonThemeExtreme) {
                    selectedTheme = "익스트림";
                } else if (v.getId() == R.id.buttonThemeFood) {
                    selectedTheme = "먹부림";
                } else if (v.getId() == R.id.buttonThemeMeeting) {
                    selectedTheme = "만남";
                }
            }
        };

        // 테마 버튼들에 클릭 리스너 설정
        buttonThemeHealing.setOnClickListener(themeButtonClickListener);
        buttonThemeExtreme.setOnClickListener(themeButtonClickListener);
        buttonThemeFood.setOnClickListener(themeButtonClickListener);
        buttonThemeMeeting.setOnClickListener(themeButtonClickListener);

        // Spinner에 시간 목록 설정
        ArrayList<String> availableTimes = getAvailableTimes();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.spinner_item, availableTimes);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        timeSpinner.setAdapter(adapter);

        // 기본 선택된 시간으로 첫 번째 항목 설정
        selectedTime = availableTimes.get(0);

        // Spinner의 선택 변경 리스너 추가
        timeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                selectedTime = availableTimes.get(position);  // 선택된 값을 selectedTime에 저장
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // 아무것도 선택되지 않은 경우 기본값 유지 (이미 첫 번째 값이 저장됨)
            }
        });

        Intent getintent = getIntent();
        String information = getintent.getStringExtra("saveInformation");
        if (getintent != null) {
            locationName = getintent.getStringExtra("Loc_name");
            latitude = getintent.getStringExtra("latitude");
            longitude = getintent.getStringExtra("longitude");

            // 값이 잘 받아졌는지 로그로 확인
            Log.d(TAG, "받은 위치 정보: " + locationName + ", 위도: " + latitude + ", 경도: " + longitude);
        }
        if (information != null) {
            Log.w(TAG, information);
            String[] parts = information.split("\\|");

            // 각 요소에 접근하여 필요한 값들을 할당합니다.
            String title = parts[0];            // 제목
            String time = parts[1];             // 시간
            String radio1 = parts[2];           // 첫 번째 라디오 버튼 상태
            String radio2 = parts[3];           // 두 번째 라디오 버튼 상태
            String radio3 = parts[4];           // 세 번째 라디오 버튼 상태
            String minAge = parts[5];
            String maxAge = parts[6];           // 나이 (비어 있을 수 있음)
            String address = parts[7];          // 주소

            editRoomTitle.setText(title);
            int position = ((ArrayAdapter<String>) timeSpinner.getAdapter()).getPosition(time);

            // Set the Spinner to the selected time
            if (position != -1) {
                timeSpinner.setSelection(position);
            }
            buttonFindDestination.setText(address);
            radioAll.setChecked(Objects.equals(radio1, "true"));
            radioMale.setChecked(Objects.equals(radio2, "true"));
            radioFemale.setChecked(Objects.equals(radio3, "true"));
            editMinAge.setText(minAge);
            editMaxAge.setText(maxAge);
        }

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

        // 목적지 찾기 버튼 클릭 처리 (TogetherDestSelect 액티비티로 이동)
        buttonFindDestination.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(CreateRoomActivity.this, TogetherSelectDest.class);
                intent.putExtra("information", editRoomTitle.getText() + "|" +
                        timeSpinner.getSelectedItem().toString() + "|" + radioAll.isChecked() + "|" + radioMale.isChecked() + "|" + radioFemale.isChecked()
                        + "|" + editMinAge.getText() + "|" + editMaxAge.getText()
                );  // 마커 위치 주소를 전달, TogetherDest에 이전에 설정한 정보를 보내서 목적지를 받아서 옴
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
                Intent intent = new Intent(CreateRoomActivity.this, ChatListActivity.class);
                startActivity(intent);
            }
        });


        // 뒤로가기 버튼을 처리하는 부분
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // 뒤로가기 버튼을 눌렀을 때 실행할 코드
                Intent intent = new Intent(CreateRoomActivity.this, ChatListActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            }
        });
    }

    // 방 생성 요청을 보내는 메소드
    private void sendCreateRoomRequest(){
        String roomTitle = editRoomTitle.getText().toString().trim();
        int minAge = Integer.parseInt(editMinAge.getText().toString().trim());
        int maxAge = Integer.parseInt(editMaxAge.getText().toString().trim());
        int genderFilter = getGenderFilter();

        String userId = AWSMobileClient.getInstance().getUsername();

        // 현재 시간 기준으로 출발 시간을 선택
        String startTime = selectedTime.replace(":", "");  // 시분 형식으로 포맷팅
        JSONObject body = new JSONObject();

        String connMethod = "POST";

        try{
            body.put("Method", connMethod);
            body.put("Loc_name", locationName);
            body.put("roomname", roomTitle);
            body.put("start", startTime);  // 시작 시간 (timestamp 형태)
            body.put("latitude", latitude);
            body.put("longitude", longitude);
            body.put("gender", genderFilter);
            body.put("min_age", minAge);
            body.put("max_age", maxAge);
            body.put("User_ID", userId);
            body.put("theme", selectedTheme);
        } catch (JSONException e) {
            Log.e(TAG, "JSON 생성 오류", e);
            return;
        }

        String bodyJson = body.toString();
        String mURL = "https://18rc8r0oi0.execute-api.ap-northeast-2.amazonaws.com/healwego-stage/" + "room/init";

        ApiRequestHandler.getJSON(mURL, connMethod, mHandler, bodyJson);
        Log.i("CreateRoomActivity", "요청: " + bodyJson);

        Toast.makeText(this, "방이 생성되었습니다!", Toast.LENGTH_LONG).show();

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
        String roomInfo = "방 제목: " + roomTitle +
                "\n출발 시간: " + selectedTime +
                "\n성별 필터: " + genderFilter +
                "\n나이 필터: " + minAge + "세 ~ " + maxAge + "세" +
                "\n테마: " + selectedTheme;  // 선택된 테마 추가

        // 다이얼로그 생성
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("방 생성 정보 확인")
                .setMessage(roomInfo)
                .setPositiveButton("확인", (dialog, which) -> {
                    // 서버에 POST 요청 보냄 
                    sendCreateRoomRequest();
                    // 방 생성 후 ChatActivity로 이동하며 방장 여부(isHost)를 전달
                    Intent intent = new Intent(CreateRoomActivity.this, ChatActivity.class);
                    intent.putExtra("roomTitle", roomTitle);
                    intent.putExtra("isHost", true);  // 방장은 항상 true로 전달
                    startActivity(intent);
                })
                .setNegativeButton("취소", null)
                .show();

    }

    // 성별 필터 값을 반환하는 메소드
    private int getGenderFilter() {
        int selectedGenderId = radioGroupGender.getCheckedRadioButtonId();
        if (selectedGenderId == R.id.radioMale) {
            return 0;  // 남성만
        } else if (selectedGenderId == R.id.radioFemale) {
            return 1;  // 여성만
        } else {
            return 2;  // 성별 무관
        }
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
