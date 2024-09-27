package com.example.healwego;

import static android.content.ContentValues.TAG;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.amazonaws.mobile.client.AWSMobileClient;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Calendar;

public class AloneSetActivity extends AppCompatActivity {  // 이름 변경

    private static class MyHandler extends Handler {
        private final WeakReference<AloneSetActivity> weakReference;

        // Deprecated 경고를 해결하기 위해 Looper를 명시적으로 받도록 수정
        public MyHandler(Looper looper, AloneSetActivity aloneSetActivity) {
            super(looper);  // 명시적으로 Looper를 전달
            weakReference = new WeakReference<>(aloneSetActivity);
        }

        @Override
        public void handleMessage(Message msg) {
            AloneSetActivity aloneSetActivity = weakReference.get();
            if (aloneSetActivity != null && !aloneSetActivity.isFinishing()) {
                // 액티비티가 여전히 존재하는 경우에만 작업 수행
                String jsonString = (String) msg.obj;
                Toast.makeText(aloneSetActivity.getApplicationContext(), jsonString, Toast.LENGTH_LONG).show();
                Log.i("ExampleActivity", "응답: " + jsonString);
            }
        }
    }

    private String selectedTime;
    private String mURL = "https://18rc8r0oi0.execute-api.ap-northeast-2.amazonaws.com/healwego-stage/";
    private String connMethod;
    private final AloneSetActivity.MyHandler mHandler = new AloneSetActivity.MyHandler(Looper.getMainLooper(), this); // MainLooper 전달

    String destLatitude;
    String startLatitude;
    String destLongitude;
    String startLongitude;
    String destLocationName;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.alone_set);  // 레이아웃 파일 이름은 그대로 두어도 무방

        Spinner timeSpinner = findViewById(R.id.timeSpinner);
        Button confirmButton = findViewById(R.id.confirmButton);

        // 현재 시간 이후의 정각 시간을 구함
        ArrayList<String> availableTimes = getAvailableTimes();

        // Spinner에 시간을 연결하기 위한 어댑터 (커스텀 레이아웃 사용)
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.spinner_item, availableTimes);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);  // 드롭다운 항목 스타일 설정
        timeSpinner.setAdapter(adapter);

        // 기본 선택된 시간으로 첫 번째 항목 설정
        selectedTime = availableTimes.get(0);


        // Spinner에서 선택한 시간을 저장
        timeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, android.view.View view, int position, long id) {
                selectedTime = availableTimes.get(position);
                Toast.makeText(AloneSetActivity.this, "선택된 시간: " + selectedTime, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // 아무 것도 선택되지 않았을 때 기본 값 유지
            }
        });

        // 확인 버튼 클릭 시 PaymentCompleteActivity로 이동
        confirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedTime = selectedTime.replace(":","");

                // API 유형
                connMethod = "POST";
                mURL = "https://18rc8r0oi0.execute-api.ap-northeast-2.amazonaws.com/healwego-stage/" + "user/path";

                JSONObject body = new JSONObject();

                String userName = AWSMobileClient.getInstance().getUsername();
                try{
                    body.put("Method", "POST");
                    body.put("User_ID", userName);
                    body.put("start",selectedTime);
                    body.put("latitude",destLatitude);
                    body.put("longitude",destLongitude);
                    body.put("User_lat",startLatitude);
                    body.put("User_lon",destLongitude);
                    body.put("Loc_name",destLocationName);
                }
                catch (JSONException e) {
                    Log.e("BODYPUTERROR", "ERRRRRRRRORRRRRR");  // 예외 처리
                }

                String bodyJson = body.toString();

                // API 요청 함수
                ApiRequestHandler.getJSON(mURL, "POST", mHandler, bodyJson);

                // PaymentCompleteActivity로 이동
                Intent intent = new Intent(AloneSetActivity.this, PaymentCompleteActivity.class);
                intent.putExtra("selectedTime", selectedTime);  // 선택한 시간 전달 (옵션)
                startActivity(intent);
            }
        });

        //경로 설정 페이지에서 넘어온 값들을 받기
        Intent getintent = getIntent();
        destLatitude = getintent.getStringExtra("dest_latitude");
        destLongitude = getintent.getStringExtra("dest_longitude");
        startLongitude = getintent.getStringExtra("start_longitude");
        startLatitude = getintent.getStringExtra("start_latitude");
        destLocationName = getintent.getStringExtra("dest_locationName");

        double destLat = 0.0;
        double destLong = 0.0;
        double startLat = 0.0;
        double startLong = 0.0;

        if (destLongitude!=null) {
            destLong = Double.parseDouble(destLongitude);
        }
        if(destLatitude!=null) {
            destLat = Double.parseDouble(destLatitude);
        }
        if(startLongitude!=null) {
            startLong = Double.parseDouble(startLongitude);

        }
        if (startLatitude!=null){
            startLat = Double.parseDouble(startLatitude);
        }

        // 하버사인을 통해 두 좌표간 거리 구하고 거리에 비례한 가격 계산
        double dist = haversine(startLat,startLong,destLat,destLong);
        TextView payText = findViewById(R.id.paymentAmount);
        int result = (int) (dist*10);
        int pay = result*150;
        payText.setText(""+pay+"원");
    }

    public static double haversine(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371; // 지구의 반지름 (단위: km)
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c; // 결과 거리 (단위: km)
    }

    // 현재 시간 이후의 정각 시간 목록을 반환하는 메소드
    private ArrayList<String> getAvailableTimes() {
        ArrayList<String> times = new ArrayList<>();

        // 현재 시간 가져오기
        Calendar calendar = Calendar.getInstance();
        int currentHour = calendar.get(Calendar.HOUR_OF_DAY);

        // 현재 시간 이후의 정각 시간을 리스트에 추가 (예: 17:00 ~ 23:00)
        for (int hour = currentHour + 1; hour <= 23; hour++) {
            String time = String.format("%02d:00", hour); // 정각 형식
            times.add(time);
        }

        return times;
    }

}
