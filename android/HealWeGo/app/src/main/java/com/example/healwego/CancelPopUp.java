package com.example.healwego;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.amazonaws.mobile.client.AWSMobileClient;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;

public class CancelPopUp extends AppCompatActivity {

    // API 요청을 위한 URL
    private String mURL = "https://18rc8r0oi0.execute-api.ap-northeast-2.amazonaws.com/healwego-stage/";
    private String connMethod;
    // 보낼 정보를 담을 JSON
    private String bodyJson;

    private TextView reserveText;
    private TextView yesBtn;
    private TextView noBtn;

    private class CancelHandler extends Handler {
        private final WeakReference<CancelPopUp> weakReference;

        // Deprecated 경고를 해결하기 위해 Looper를 명시적으로 받도록 수정
        public CancelHandler(Looper looper, CancelPopUp cancelpopup) {
            super(looper);  // 명시적으로 Looper를 전달
            weakReference = new WeakReference<>(cancelpopup);
        }


        @Override
        public void handleMessage(Message msg) {
            CancelPopUp cancelpopup = weakReference.get();
            if (cancelpopup != null && !cancelpopup.isFinishing()) {
                // 액티비티가 여전히 존재하는 경우에만 작업 수행
                String jsonString = (String) msg.obj;

                JSONObject responseObject = null;
                try {
                    // 응답을 JSONObject로 변환
                    responseObject = new JSONObject(jsonString);
                    // "option" 필드를 문자열로 가져옴
                    int option = responseObject.getInt("option");

                    if(option == 0){
                        Toast.makeText(cancelpopup.getApplicationContext(), "현재는 예약을 취소하실 수 없습니다.", Toast.LENGTH_LONG).show();
                    }
                    else{
                        Toast.makeText(cancelpopup.getApplicationContext(), "예약이 정상적으로 취소되었습니다.", Toast.LENGTH_LONG).show();
                    }

                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }

                // 메인 화면으로 복귀
                Intent intent = new Intent(CancelPopUp.this,MainActivity.class);
                startActivity(intent);
                finish();
            }
        }
    }

    // WeakReference로 Activity에 대한 참조를 가진 MyHandler 객체
    private final CancelPopUp.CancelHandler cHandler = new CancelPopUp.CancelHandler(Looper.getMainLooper(), this); // MainLooper 전달

    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE); // 타이틀 바 제거
        setContentView(R.layout.activity_cancel_popup);

        reserveText = (TextView)findViewById(R.id.reserveText);
        yesBtn = (TextView)findViewById(R.id.yesBtn);
        noBtn = (TextView)findViewById(R.id.noBtn);

        Intent intent = getIntent();
        String data = intent.getStringExtra("data");
        reserveText.setText(data);

        yesBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                requestCancelReserve();
            }
        });

        noBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(CancelPopUp.this,MainActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }

    private void requestCancelReserve(){
        // JSON 요청 생성
        JSONObject body = new JSONObject();
        String userId = AWSMobileClient.getInstance().getUsername();

        // API 호출에 필요한 정보
        // API 유형
        connMethod = "DELETE";
        mURL = "https://18rc8r0oi0.execute-api.ap-northeast-2.amazonaws.com/healwego-stage/" + "room/list";

        try{
            body.put("Method", connMethod);
            body.put("User_ID", userId);
        }catch(JSONException e){
            Log.e("ChatListActivity", "JSON 생성 오류", e);
            return;
        }

        String bodyJson = body.toString();
        Log.i("MainActivity", "요청 바디: " + bodyJson);

        // API 요청 함수
        ApiRequestHandler.getJSON(mURL, connMethod, cHandler, bodyJson);
    }
}
