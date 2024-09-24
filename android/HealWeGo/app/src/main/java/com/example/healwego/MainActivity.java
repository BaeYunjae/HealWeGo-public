package com.example.healwego;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.amazonaws.mobile.client.AWSMobileClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    // API 요청을 위한 URL
    private String mURL = "https://e2fqrjfyj9.execute-api.ap-northeast-2.amazonaws.com/healwego-stage/";
    private String connMethod;
    // 보낼 정보를 담을 JSON
    private String bodyJson;

    private Button buttonAlone;
    private Button buttonTogether;
    private View blankView;

    private Button buttonReseve;

    // MyHandler를 static으로 선언하여 메모리 누수를 방지하고, WeakReference로 액티비티 참조
    private static class MyHandler extends Handler {
        private final WeakReference<MainActivity> weakReference;

        // Deprecated 경고를 해결하기 위해 Looper를 명시적으로 받도록 수정
        public MyHandler(Looper looper, MainActivity mainActivity) {
            super(looper);  // 명시적으로 Looper를 전달
            weakReference = new WeakReference<>(mainActivity);
        }


        @Override
        public void handleMessage(Message msg) {
            MainActivity mainActivity = weakReference.get();
            if (mainActivity != null && !mainActivity.isFinishing()) {
                // 액티비티가 여전히 존재하는 경우에만 작업 수행
                String jsonString = (String) msg.obj;
                Toast.makeText(mainActivity.getApplicationContext(), jsonString, Toast.LENGTH_LONG).show();
                Log.i("mainActivity", "응답: " + jsonString);

                // 응답을 처리하는 메서드 호출
                mainActivity.handleReserveResponse(jsonString);
            }
        }
    }

    // WeakReference로 Activity에 대한 참조를 가진 MyHandler 객체
    private final MainActivity.MyHandler mHandler = new MainActivity.MyHandler(Looper.getMainLooper(), this); // MainLooper 전달

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Edge-to-Edge 기능 활성화
        EdgeToEdge.enable(this);
        // 레이아웃 파일을 화면에 보여줌
        setContentView(R.layout.basic_main);

        // 시스템 바 인셋을 적용하여 화면 패딩 설정
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });


    }

    @Override
    protected void onResume() {
        super.onResume();

        // 페이지로 돌아올 때마다 호출하고 싶은 함수
        requestReservation();
    }

    // 혼자가기 버튼 클릭 리스너
    public void onButton1Clicked(View v) {
        Toast.makeText(this, "혼자가기 버튼이 눌렸어요.", Toast.LENGTH_SHORT).show();

        // PathSelect로 이동
        Intent intent = new Intent(MainActivity.this, PathSelect.class);
        startActivity(intent);
    }

    // 함께가기 버튼 클릭 리스너
    public void onButton2Clicked(View v) {
        Toast.makeText(this, "함께가기 버튼이 눌렸어요.", Toast.LENGTH_SHORT).show();

        // TogetherSelectStart로 이동
        Intent intent = new Intent(MainActivity.this, TogetherSelectStart.class);
        startActivity(intent);
    }

    // 이미지 클릭 리스너 (을왕리 해수욕장 클릭)
    public void onImageClicked(View v) {
        // Toast 메시지 표시
        Toast.makeText(this, "을왕리 해수욕장이 눌렸어요.", Toast.LENGTH_SHORT).show();

        // 추천 장소별 정보 페이지로 가야 함 (아직 구현 안 함) - 앱 자체 안에 넣어? 아니면 DB? 서버에 장소 이름을 API로 보내
        // ReserveActivity로 이동
        Intent intent = new Intent(MainActivity.this, ExplainPlaceActivity.class);
        startActivity(intent);
    }

    // 방 목록 요청 메소드
    private void requestReservation(){

        // JSON 요청 생성
        JSONObject body = new JSONObject();
        String userId = AWSMobileClient.getInstance().getUsername();

        // API 호출에 필요한 정보
        // API 유형
        connMethod = "GET";
        mURL = "https://e2fqrjfyj9.execute-api.ap-northeast-2.amazonaws.com/healwego-stage/" + "user/info";

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
        ApiRequestHandler.getJSON(mURL, connMethod, mHandler, bodyJson);
    }

    // 방 목록 응답 처리 메소드
    private void handleReserveResponse(String response){

        Context context = this;
        LinearLayout parentLayout = (LinearLayout)findViewById(R.id.linearLayout);
        parentLayout.removeAllViews();

        // 버튼 레이아웃 파라미터 설정
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, // width
                ViewGroup.LayoutParams.MATCH_PARENT, // height
                3f  // weight: 3
        );

        // 뷰 레이아웃 파라미터 설정
        LinearLayout.LayoutParams viewParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, // width
                ViewGroup.LayoutParams.MATCH_PARENT, // height
                4f  // weight: 3
        );

        try {
            // 1. 응답을 JSONObject로 변환
            JSONObject responseObject = new JSONObject(response);

            // 2. "body" 필드를 문자열로 가져옴
            String body = responseObject.getString("body");

            // 3. "body"를 JSONObject로 변환
            JSONObject bodyJson = new JSONObject(body);

            // 4. 파싱한 결과를 바탕으로 레이아웃 그림
            int option = bodyJson.getInt("option");

            if(option == 0){ // 참여 중인 방이 없는 상태
                // 첫 번째 버튼 생성 및 설정
                buttonAlone = new Button(this);
                buttonAlone.setText("혼자 가기");
                buttonAlone.setTextSize(18);
                buttonAlone.setBackgroundColor(getResources().getColor(R.color.teal));
                buttonAlone.setLayoutParams(buttonParams);
                buttonAlone.setId(ViewCompat.generateViewId());

                // 두 번째 버튼 생성 및 설정
                buttonTogether = new Button(this);
                buttonTogether.setText("함께 가기");
                buttonTogether.setTextSize(18);
                buttonTogether.setBackgroundColor(getResources().getColor(R.color.teal));
                buttonTogether.setLayoutParams(buttonParams);
                buttonTogether.setId(ViewCompat.generateViewId());

                blankView = new View(this);
                blankView.setLayoutParams(viewParams);
                blankView.setId(ViewCompat.generateViewId());

                parentLayout.addView(buttonAlone);
                parentLayout.addView(blankView);
                parentLayout.addView(buttonTogether);

                // onClickListener 추가
                buttonAlone.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Toast.makeText(context, "혼자가기 버튼이 눌렸어요.", Toast.LENGTH_SHORT).show();

                        // PathSelect로 이동
                        Intent intent = new Intent(MainActivity.this, PathSelect.class);
                        startActivity(intent);
                    }
                });

                buttonTogether.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Toast.makeText(context, "함께가기 버튼이 눌렸어요.", Toast.LENGTH_SHORT).show();

                        // TogetherSelectStart로 이동
                        Intent intent = new Intent(MainActivity.this, TogetherSelectStart.class);
                        startActivity(intent);
                    }
                });
            }
            else if(option == 1){ // 예약 완료 상태
                String roomName = bodyJson.getString("roomname");
                String locName = bodyJson.getString("Loc_name");
                String start = bodyJson.getString("start");
                String displayTxt = roomName + "\n목적지 : " + locName + "\n출발 시각 : " + start;

                buttonReseve = new Button(this);
                buttonReseve.setText(displayTxt);
                buttonReseve.setTextSize(18);
                buttonReseve.setBackgroundColor(getResources().getColor(R.color.teal));
                buttonReseve.setLayoutParams(buttonParams);
                buttonReseve.setId(ViewCompat.generateViewId());

                parentLayout.addView(buttonReseve);

                buttonReseve.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Toast.makeText(context, "예약현황 버튼이 눌렸어요.", Toast.LENGTH_SHORT).show();

                        // TogetherSelectStart로 이동
                        Intent intent = new Intent(MainActivity.this, MapPath.class);
                        startActivity(intent);
                    }
                });
            }

        } catch (JSONException e) {
            Log.e("MainActivity", "예약 정보 파싱 오류", e);
        }
    }

}
