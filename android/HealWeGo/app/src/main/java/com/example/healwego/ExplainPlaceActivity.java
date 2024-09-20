package com.example.healwego;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class ExplainPlaceActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // explain_place.xml 레이아웃을 설정
        setContentView(R.layout.explain_place);

        // 필요한 경우, UI 요소를 찾아서 데이터를 세팅할 수 있습니다.
        // 예를 들어 장소 이름이나 설명을 설정하는 경우:
        // TextView placeTextView = findViewById(R.id.placeText);
        // TextView explainTextView = findViewById(R.id.explainText);
        // placeTextView.setText("을왕리 해수욕장");
        // explainTextView.setText("Lorem ipsum 설명입니다.");
    }
}
