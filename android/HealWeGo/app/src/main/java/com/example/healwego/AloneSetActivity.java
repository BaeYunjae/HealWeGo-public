package com.example.healwego;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.Calendar;

public class AloneSetActivity extends AppCompatActivity {  // 이름 변경

    private String selectedTime;

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
