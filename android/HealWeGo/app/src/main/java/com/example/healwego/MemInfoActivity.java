package com.example.healwego;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.amazonaws.mobile.client.AWSMobileClient;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;

public class MemInfoActivity extends AppCompatActivity {

    // UI 요소
    private EditText usernameEditText;
    private EditText ageEditText;
    private RadioGroup genderRadioGroup;
    private Button nextButton;

    // API 요청을 위한 URL
    private String mURL = "https://18rc8r0oi0.execute-api.ap-northeast-2.amazonaws.com/healwego-stage/";
    // POST, GET, DELETE, ...
    private String connMethod;
    // 보낼 정보를 담을 JSON
    private String bodyJson;

    // MyHandler를 static으로 선언하여 메모리 누수를 방지하고, WeakReference로 액티비티 참조
    private static class MyHandler extends Handler {
        private final WeakReference<MemInfoActivity> weakReference;

        // Deprecated 경고를 해결하기 위해 Looper를 명시적으로 받도록 수정
        public MyHandler(Looper looper, MemInfoActivity memInfoActivity) {
            super(looper);  // 명시적으로 Looper를 전달
            weakReference = new WeakReference<>(memInfoActivity);
        }


        @Override
        public void handleMessage(Message msg) {
            MemInfoActivity memInfoActivity = weakReference.get();
            if (memInfoActivity != null && !memInfoActivity.isFinishing()) {
                // 액티비티가 여전히 존재하는 경우에만 작업 수행
                String jsonString = (String) msg.obj;
                Toast.makeText(memInfoActivity.getApplicationContext(), jsonString, Toast.LENGTH_LONG).show();
                Log.i("MemInfoActivity", "응답: " + jsonString);
            }
        }
    }

    // WeakReference로 Activity에 대한 참조를 가진 MyHandler 객체
    private final MemInfoActivity.MyHandler mHandler = new MemInfoActivity.MyHandler(Looper.getMainLooper(), this); // MainLooper 전달


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.info_register); // info_register.xml 레이아웃을 설정

        // UI 요소 초기화
        usernameEditText = findViewById(R.id.et_username);
        ageEditText = findViewById(R.id.et_age);
        genderRadioGroup = findViewById(R.id.rg_gender);
        nextButton = findViewById(R.id.btn_next);


        Button.OnClickListener ButtonListener = new Button.OnClickListener() {
            // 다음 버튼 클릭 시 API 호출 및 card_register 액티비티로 이동
            @Override
            public void onClick (View v) {
                if (v == nextButton) {
                    sendUserInfoToServer();

                    Intent intent = new Intent(MemInfoActivity.this, CardInfoActivity.class);
                    startActivity(intent); // CardRegisterActivity로 전환
                    finish();
                }
            }
        };
        nextButton.setOnClickListener(ButtonListener);
    };

    private void sendUserInfoToServer(){
        String username = usernameEditText.getText().toString();
        String ageStr = ageEditText.getText().toString();
        int age = 0;

        // 나이 칸이 비어있지 않으면
        if (!ageStr.isEmpty()){
            age = Integer.parseInt(ageStr);
        }

        int selectedGenderId = genderRadioGroup.getCheckedRadioButtonId();
        int gender = (selectedGenderId == R.id.rb_male) ? 0 : 1;  // 남성이면 0, 여성이면 1

        // API 호출에 필요한 정보
        // API 유형
        connMethod = "POST";
        mURL = "https://18rc8r0oi0.execute-api.ap-northeast-2.amazonaws.com/healwego-stage/" + "user/info";

        JSONObject body = new JSONObject();
        SharedPreferences sharedPref = getSharedPreferences("UserIDPrefs", Context.MODE_PRIVATE);
        String userId = sharedPref.getString("userID", ""); // 값이 없으면 "defaultUsername" 사용

        try{
            body.put("Method", connMethod);
            body.put("User_ID", userId);
            body.put("age", age);
            body.put("username", username);
            body.put("gender", gender);
        } catch(JSONException e){
            Log.e("MemInfoActivity", "JSON 생성 오류", e);
            return;
        }

        String bodyJson = body.toString();
        Log.i("MemInfoActivity", "요청 바디: " + bodyJson);

        // API 요청 함수
        ApiRequestHandler.getJSON(mURL, connMethod, mHandler, bodyJson);
    }
}
