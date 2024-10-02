package com.example.healwego;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.amazonaws.mobile.client.AWSMobileClient;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;

public class ChatPopUpActivity extends AppCompatActivity {

    // API 요청을 위한 URL
    private String mURL = "https://18rc8r0oi0.execute-api.ap-northeast-2.amazonaws.com/healwego-stage/";
    private String connMethod;
    // 보낼 정보를 담을 JSON
    private String bodyJson;

    private String userId;

    private TextView yesBtn;
    private TextView noBtn;

    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE); // 타이틀 바 제거
        setContentView(R.layout.activity_chat_popup);

        userId = AWSMobileClient.getInstance().getUsername();

        yesBtn = (TextView)findViewById(R.id.yesBtn);
        noBtn = (TextView)findViewById(R.id.noBtn);

        yesBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v){
                sendDeleteRequest();
            }
        });

        noBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v){
                // cancelPopUp으로 이동
                Intent intent = new Intent(ChatPopUpActivity.this, ChatActivity.class);
                intent.putExtra("result", 0);
                startActivity(intent);
                finish();
            }
        });
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
        // DELETE처리를 위한 핸들러 생성
        ApiRequestHandler.getJSON(apiURL, connMethod, new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                String response = (String) msg.obj;

                try {
                    JSONObject responseObject = new JSONObject(response);
                    int statusCode = responseObject.getInt("statusCode");

                    // DELETE 요청 성공 시 실행할 작업
                    if (statusCode == 200) {
                        Log.i("ChatPopUpActivity", "DELETE 요청 성공");

                        // DELETE 성공 후 MQTT 메시지 전송
                        JSONObject jsonObject = new JSONObject();
                        try {
                            jsonObject.put("User_ID", userId);
                            jsonObject.put("option", "enter");
                            jsonObject.put("message", userId);
                        } catch (JSONException e) {
                            throw new RuntimeException(e);
                        }

                        // 방 리스트로 화면 전환
                        Intent chatIntent = new Intent(ChatPopUpActivity.this, ChatActivity.class);
                        chatIntent.putExtra("result", 1);
                        startActivity(chatIntent);
                        finish();
                    } else {
                        // DELETE 요청 실패 시 처리
                        Log.e("ChatActivity", "DELETE 요청 실패: " + statusCode);
                        Toast.makeText(ChatPopUpActivity.this, "방 나가기에 실패했습니다.", Toast.LENGTH_SHORT).show();
                    }
                } catch (JSONException e) {
                    Log.e("ChatActivity", "DELETE 응답 처리 오류", e);
                    Intent chatIntent = new Intent(ChatPopUpActivity.this, ChatActivity.class);
                    chatIntent.putExtra("result", 0);
                    startActivity(chatIntent);
                    finish();
                }
            }
        }, bodyJson);

        Log.i("ChatPopUpActivity", "DELETE 요청: " + bodyJson);
    }
}
