package com.example.healwego;

import static android.content.ContentValues.TAG;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.Calendar;

public class AloneSetActivity extends AppCompatActivity {  // 이름 변경

    private String selectedTime;


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
                // PaymentCompleteActivity로 이동
                Intent intent = new Intent(AloneSetActivity.this, PaymentCompleteActivity.class);
                intent.putExtra("selectedTime", selectedTime);  // 선택한 시간 전달 (옵션)
                startActivity(intent);
            }
        });

        Intent getintent = getIntent();
        String destLatitude = getintent.getStringExtra("dest_latitude");
        String destLongitude = getintent.getStringExtra("dest_longitude");
        String startLongitude = getintent.getStringExtra("start_longitude");
        String startLatitude = getintent.getStringExtra("start_latitude");

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

        double dist = haversine(startLat,startLong,destLat,destLong);

        Log.d(TAG, ""+dist);
        Log.d(TAG, ""+dist);
        Log.d(TAG, ""+dist);
        Log.d(TAG, ""+dist);
        Log.d(TAG, ""+dist);        Log.d(TAG, ""+dist);

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
