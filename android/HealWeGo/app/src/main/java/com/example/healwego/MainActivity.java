package com.example.healwego;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Base64;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.Insets;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.graphics.drawable.GradientDrawable;

import com.amazonaws.mobile.client.AWSMobileClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.lang.ref.WeakReference;

public class MainActivity extends AppCompatActivity {

    // API 요청을 위한 URL
    private String mURL = "https://18rc8r0oi0.execute-api.ap-northeast-2.amazonaws.com/healwego-stage/";
    private String connMethod;
    // 보낼 정보를 담을 JSON
    private String bodyJson;

    private Button buttonAlone;
    private Button buttonTogether;
    private View blankView;

    private Button buttonReserve;
    private Button buttonCancel;

    private TextView nameText;
    private TextView messageText;

    private RelativeLayout loadView;
    private int isLoad = 0;

    // MyHandler를 static으로 선언하여 메모리 누수를 방지하고, WeakReference로 액티비티 참조
    private static class ReserveHandler extends Handler {
        private final WeakReference<MainActivity> weakReference;

        // Deprecated 경고를 해결하기 위해 Looper를 명시적으로 받도록 수정
        public ReserveHandler(Looper looper, MainActivity mainActivity) {
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

    private static class RecommendHandler extends Handler {
        private final WeakReference<MainActivity> weakReference;

        // Deprecated 경고를 해결하기 위해 Looper를 명시적으로 받도록 수정
        public RecommendHandler(Looper looper, MainActivity mainActivity) {
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
                mainActivity.handleRecommendResponse(jsonString);
            }
        }
    }

    // WeakReference로 Activity에 대한 참조를 가진 MyHandler 객체
    private final MainActivity.ReserveHandler rsHandler = new MainActivity.ReserveHandler(Looper.getMainLooper(), this); // MainLooper 전달
    private final MainActivity.RecommendHandler rcHandler = new MainActivity.RecommendHandler(Looper.getMainLooper(), this); // MainLooper 전달

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Edge-to-Edge 기능 활성화
        EdgeToEdge.enable(this);
        // 레이아웃 파일을 화면에 보여줌
        setContentView(R.layout.basic_main);

        // AWSMobileClient로부터 사용자 이름을 가져와 SharedPreferences에 저장
        String userID = AWSMobileClient.getInstance().getUsername();
        if(userID != null){
            SharedPreferences sharedPref = getSharedPreferences("UserIDPrefs", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString("userID", userID);
            editor.apply(); // 값을 비동기적으로 저장
        }

//        String userId = AWSMobileClient.getInstance().getUsername();
//        Constant.setUserNameKey(userId);

        Animation loadAnimation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.progress_image);
        loadView = (RelativeLayout)findViewById(R.id.loadView);
        ImageView loadImage = (ImageView) findViewById(R.id.loadImage);
        loadImage.startAnimation(loadAnimation);

        nameText = (TextView)findViewById(R.id.userName);
        messageText = (TextView)findViewById(R.id.textView1);

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
        requestRecommendation();
        requestReservation();
    }

    // 방 목록 요청 메소드
    private void requestReservation(){

        // JSON 요청 생성
        JSONObject body = new JSONObject();
        SharedPreferences sharedPref = getSharedPreferences("UserIDPrefs", Context.MODE_PRIVATE);
        String userId = sharedPref.getString("userID", ""); // 값이 없으면 "defaultUsername" 사용

        // API 호출에 필요한 정보
        // API 유형
        connMethod = "GET";
        mURL = "https://18rc8r0oi0.execute-api.ap-northeast-2.amazonaws.com/healwego-stage/" + "user/info";

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
        ApiRequestHandler.getJSON(mURL, connMethod, rsHandler, bodyJson);
    }

    // 방 목록 응답 처리 메소드
    private void handleReserveResponse(String response){

        Context context = this;
        LinearLayout parentLayout = (LinearLayout)findViewById(R.id.linearLayout);
        parentLayout.removeAllViews();

        try {
            // 1. 응답을 JSONObject로 변환
            JSONObject responseObject = new JSONObject(response);

            // 2. "body" 필드를 문자열로 가져옴
            String body = responseObject.getString("body");

            // 3. "body"를 JSONObject로 변환
            JSONObject bodyJson = new JSONObject(body);

            // 4. 파싱한 결과를 바탕으로 레이아웃 그림
            int option = bodyJson.getInt("option");

            String userName = bodyJson.getString("username") + "님, ";
            nameText.setText(userName);

            if(option == 0){ // 참여 중인 방이 없는 상태

                messageText.setText("오늘은 어떻게 갈까요?");

                View aloneTogetherView = getLayoutInflater().inflate(R.layout.button_layout_alone_together, parentLayout, false);
                LinearLayout buttonAlone = aloneTogetherView.findViewById(R.id.buttonAlone);
                LinearLayout buttonTogether = aloneTogetherView.findViewById(R.id.buttonTogether);

                // Add the inflated layout to the parentLayout
                parentLayout.addView(aloneTogetherView);

                // onClickListener 추가
                buttonAlone.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Toast.makeText(context, "혼자가기 버튼이 눌렸어요.", Toast.LENGTH_SHORT).show();
                        if(isLoad == 0) return;

                        // PathSelect로 이동
                        Intent intent = new Intent(MainActivity.this, PathSelect.class);
                        startActivity(intent);
                    }
                });

                buttonTogether.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Toast.makeText(context, "함께가기 버튼이 눌렸어요.", Toast.LENGTH_SHORT).show();
                        if(isLoad == 0) return;

                        // TogetherSelectStart로 이동
                        Intent intent = new Intent(MainActivity.this, TogetherSelectStart.class);
                        startActivity(intent);
                    }
                });
            }
            else if(option == 1){ // 예약 완료 상태

                messageText.setText("예약된 일정이 있습니다.");

                // 버튼 레이아웃을 XML에서 인플레이트
                LayoutInflater inflater = LayoutInflater.from(this);
                View buttonLayout = inflater.inflate(R.layout.button_layout_reserve_cancel, parentLayout, false);

                // 인플레이트한 레이아웃에서 버튼 객체를 찾음
                Button buttonReserve = buttonLayout.findViewById(R.id.buttonReserve);
                Button buttonCancel = buttonLayout.findViewById(R.id.buttonCancel);

                // 예약된 일정 정보 세팅
                String roomName = bodyJson.getString("roomname");
                String locName = bodyJson.getString("Loc_name");
                String start = bodyJson.getString("start");
                start = start.substring(0, 2) + ":" + start.substring(2);
                String displayTxt = roomName + "\n목적지 : " + locName + "\n출발 시각 : " + start;
                buttonReserve.setText(displayTxt);

                // 버튼들을 parentLayout에 추가
                parentLayout.addView(buttonLayout);

                buttonReserve.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Toast.makeText(context, "예약현황 버튼이 눌렸어요.", Toast.LENGTH_SHORT).show();
                        if(isLoad == 0) return;

                        // TogetherSelectStart로 이동
                        Intent intent = new Intent(MainActivity.this, MapPath.class);
                        startActivity(intent);
                    }
                });

                buttonCancel.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v){
                        Toast.makeText(context, "예약취소 버튼이 눌렸어요.", Toast.LENGTH_SHORT).show();
                        if(isLoad == 0) return;

                        // cancelPopUp으로 이동
                        Intent intent = new Intent(MainActivity.this, CancelPopUp.class);
                        intent.putExtra("data", displayTxt);
                        startActivity(intent);
                    }
                });
            }

        } catch (JSONException e) {
            Log.e("MainActivity", "예약 정보 파싱 오류", e);
        }
    }

    // 추천 장소 목록 요청 메소드
    private void requestRecommendation(){

        // JSON 요청 생성
        JSONObject body = new JSONObject();
        SharedPreferences sharedPref = getSharedPreferences("UserIDPrefs", Context.MODE_PRIVATE);
        String userId = sharedPref.getString("userID", ""); // 값이 없으면 "defaultUsername" 사용

        // API 호출에 필요한 정보
        // API 유형
        connMethod = "PATCH";
        mURL = "https://18rc8r0oi0.execute-api.ap-northeast-2.amazonaws.com/healwego-stage/" + "recommend-destination";

        try{
            body.put("Method", connMethod);
        }catch(JSONException e){
            Log.e("ChatListActivity", "JSON 생성 오류", e);
            return;
        }

        String bodyJson = body.toString();
        Log.i("MainActivity", "요청 바디: " + bodyJson);

        // API 요청 함수
        ApiRequestHandler.getJSON(mURL, connMethod, rcHandler, bodyJson);
    }

    // 추천 장소 목록 응답 처리 메소드
    private void handleRecommendResponse(String response){

        Context context = this;

        int[] imageViews = {R.id.imageView1, R.id.imageView2, R.id.imageView3, R.id.imageView4, R.id.imageView5};
        int[] textViews = {R.id.placeText1, R.id.placeText2, R.id.placeText3, R.id.placeText4, R.id.placeText5};

        Log.i("RESPONSE", "RESPONSE" + response);
        try {
            // 1. 응답을 JSONObject로 변환
            JSONObject responseObject = new JSONObject(response);

            // 2. "body" 필드를 문자열로 가져옴
            String body = responseObject.getString("body");

            // JSON 문자열을 JSONArray로 변환
            JSONArray jsonArray = new JSONArray(body);
            Log.i("RESPONSE", "ARRAY length : " + jsonArray.length());

            // JSONArray를 순회하며 각 객체를 파싱
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);

                String locName = jsonObject.getString("Loc_name");
                String description = jsonObject.getString("description");
                String encodedImage = jsonObject.getString("encoded_image");

                // Base64로 인코딩된 이미지 디코딩
                byte[] decodedString = Base64.decode(encodedImage, Base64.DEFAULT);
                Bitmap decodedImage = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);

                Log.i("DECODED", "locName " + locName);
                Log.i("DECODED", "description : " + description);
                Log.i("DECODED", "decodedImage : " + decodedImage);

                ImageView imageView = (ImageView)findViewById(imageViews[i]);
                TextView textView = (TextView)findViewById(textViews[i]);

                imageView.setImageBitmap(decodedImage);
                textView.setText(locName);

                imageView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // Toast 메시지 표시
                        Toast.makeText(context, "이미지 클릭!!", Toast.LENGTH_SHORT).show();
                        if(isLoad == 0) return;

                        // RecommendPopUpActivity로 이동
                        Intent intent = new Intent(context, RecommendPopUpActivity.class);
                        intent.putExtra("locName", locName);
                        intent.putExtra("description", description);
                        startActivityForResult(intent, 1);
                    }
                });
            }

        } catch (JSONException e) {
            Log.e("IMAGEVIEW", "추천 정보 파싱 오류", e);
        }

        loadView.animate()
                .alpha(0.0f)
                .setDuration(600)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        loadView.setVisibility(View.GONE);
                    }
                });
        isLoad = 1;
    }

    public static int ConvertDPtoPX(Context context, int dp) {
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round((float) dp * density);
    }
}
