package com.example.healwego;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

public class CardInfoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.card_register); // card_register.xml 레이아웃을 설정

        // 카드 번호 입력란들
        EditText cardNumber1 = findViewById(R.id.et_card_number_1);
        EditText cardNumber2 = findViewById(R.id.et_card_number_2);
        EditText cardNumber3 = findViewById(R.id.et_card_number_3);
        EditText cardNumber4 = findViewById(R.id.et_card_number_4);
        EditText expirationDate = findViewById(R.id.et_expiration_date);
        EditText cvc = findViewById(R.id.et_cvc);

        // 각 EditText에 TextWatcher를 추가하여 4자리 입력 시 자동으로 다음 칸으로 포커스 이동
        addTextWatcher(cardNumber1, cardNumber2, 4);
        addTextWatcher(cardNumber2, cardNumber3, 4);
        addTextWatcher(cardNumber3, cardNumber4, 4);
        addTextWatcher(cardNumber4, expirationDate, 4);

        // 유효기간 입력란 (MM/YY 형식 자동 변환)
        expirationDate.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);

        // TextWatcher로 MM/YY 형식 변환 및 자동 포커스 이동
        expirationDate.addTextChangedListener(new TextWatcher() {
            private String current = "";
            private boolean isDeleting;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                isDeleting = count > 0;
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (!s.toString().equals(current)) {
                    String clean = s.toString().replaceAll("[^\\d]", ""); // 숫자 외 문자 제거
                    int length = clean.length();

                    if (isDeleting && current.length() > 0) {
                        current = current.substring(0, current.length() - 1);
                        expirationDate.removeTextChangedListener(this);
                        expirationDate.setText(current);
                        expirationDate.setSelection(current.length());
                        expirationDate.addTextChangedListener(this);
                        return;
                    }

                    if (length <= 2) {
                        current = clean;
                    } else if (length <= 4) {
                        current = clean.substring(0, 2) + "/" + clean.substring(2);
                    } else {
                        current = clean.substring(0, 2) + "/" + clean.substring(2, 4);
                    }

                    expirationDate.removeTextChangedListener(this); // 무한 루프 방지
                    expirationDate.setText(current);
                    expirationDate.setSelection(current.length()); // 커서 위치 조정
                    expirationDate.addTextChangedListener(this);

                    // 5자리(MM/YY) 입력되면 CVC 입력란으로 포커스 이동
                    if (current.length() == 5) {
                        cvc.requestFocus();
                    }
                }
            }
        });

        // 완료 버튼 클릭 시 MainActivity로 이동
        Button completeButton = findViewById(R.id.btn_complete); // card_register.xml에 정의된 버튼 ID 사용
        completeButton.setOnClickListener(v -> {
            Intent intent = new Intent(CardInfoActivity.this, MainActivity.class);
            startActivity(intent); // MainActivity로 전환
        });
    }

    // TextWatcher로 설정된 자리수 입력 후 자동으로 다음 EditText로 이동
    private void addTextWatcher(final EditText currentEditText, final EditText nextEditText, final int maxLength) {
        currentEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() == maxLength) {
                    nextEditText.requestFocus(); // 입력이 완료되면 다음 EditText로 포커스 이동
                }
            }
        });
    }
}
