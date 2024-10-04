package com.example.healwego;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.amazonaws.mobile.client.AWSMobileClient;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;

// 여기 ExampleActivity 수정!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
public class ExampleActivity extends AppCompatActivity {
    // 테스트용 버튼
    private Button apiButton;
    // API 요청을 위한 URL
    private String mURL = "https://18rc8r0oi0.execute-api.ap-northeast-2.amazonaws.com/healwego-stage/";
    // POST, GET, DELETE, ...
    private String connMethod;
    // 보낼 정보를 담을 JSON
    private String bodyJson;

    // MyHandler를 static으로 선언하여 메모리 누수를 방지하고, WeakReference로 액티비티 참조
    private static class MyHandler extends Handler {
        // 여기 ExampleActivity 수정!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
        private final WeakReference<ExampleActivity> weakReference;

        // Deprecated 경고를 해결하기 위해 Looper를 명시적으로 받도록 수정
        // 여기 ExampleActivity 수정!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
        public MyHandler(Looper looper, ExampleActivity exampleActivity) {
            super(looper);  // 명시적으로 Looper를 전달
            weakReference = new WeakReference<>(exampleActivity);
        }

        @Override
        public void handleMessage(Message msg) {
            // 여기 ExampleActivity 수정!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
            ExampleActivity exampleActivity = weakReference.get();
            // 여기 ExampleActivity 수정!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
            if (exampleActivity != null && !exampleActivity.isFinishing()) {
                // 액티비티가 여전히 존재하는 경우에만 작업 수행
                String jsonString = (String) msg.obj;
                // 여기 ExampleActivity 수정!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
                Toast.makeText(exampleActivity.getApplicationContext(), jsonString, Toast.LENGTH_LONG).show();
                Log.i("ExampleActivity", "응답: " + jsonString);
            }
        }
    }

    // WeakReference로 Activity에 대한 참조를 가진 MyHandler 객체
    // 여기 ExampleActivity 수정!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    private final ExampleActivity.MyHandler mHandler = new ExampleActivity.MyHandler(Looper.getMainLooper(), this); // MainLooper 전달


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        apiButton = findViewById(R.id.fab);

        Button.OnClickListener ButtonListener = new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (v == apiButton) {
                    // API 유형
                    connMethod = "PATCH";
                    mURL = "https://18rc8r0oi0.execute-api.ap-northeast-2.amazonaws.com/healwego-stage/" + "car/on";

                    JSONObject body = new JSONObject();

                    SharedPreferences sharedPref = getSharedPreferences("UserIDPrefs", Context.MODE_PRIVATE);
                    String userName = sharedPref.getString("userID", ""); // 값이 없으면 "defaultUsername" 사용

                    try{
                        body.put("Method", "PATCH");
                        body.put("User_ID", userName);
                    }
                    catch (JSONException e) {
                        Log.e("BODYPUTERROR", "ERRRRRRRRORRRRRR");  // 예외 처리
                    }

                    String bodyJson = body.toString();

                    // API 요청 함수
                    ApiRequestHandler.getJSON(mURL, "PATCH", mHandler, bodyJson);
                }
            }
        };
        apiButton.setOnClickListener(ButtonListener);
    }
}
