package com.example.healwego;


import static android.content.ContentValues.TAG;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import android.content.Intent;  // Intent를 사용하기 위해 추가
import android.os.Bundle;
import android.util.Log;
import android.view.View;  // View를 사용하기 위해 추가
import android.widget.Button;  // Button 사용을 위해 추가
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.model.LatLng;


public class PaymentCompleteActivity extends AppCompatActivity {

    private Button confirmButton;  // 버튼 선언
    private static final String BROKER_URL = "ssl://a3boaptn83mu7y-ats.iot.ap-northeast-2.amazonaws.com:8883";
    private static final String CLIENT_ID = "AndroidClient";

    private static final String PATH_TOPIC = "path";
    private static final String POINT_TOPIC = "path/points/ros";


    MqttAsyncClient mqttClient;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.finish_pay);  // finish_pay.xml 파일 연결

        // confirmButton과 연결
        confirmButton = findViewById(R.id.confirmButton);  // XML에서 버튼의 ID로 찾음

        // confirmButton 클릭 시 MapPath로 이동
        confirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // MapPath로 이동하는 Intent 생성
                Intent intent = new Intent(PaymentCompleteActivity.this, MapPath.class);
                startActivity(intent);  // MapPath Activity 시작
            }
        });

        connectToMqtt();
    }

    //ㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡ
    private void connectToMqtt() {
        try {
            mqttClient = new MqttAsyncClient(BROKER_URL, CLIENT_ID, new MemoryPersistence());
            MqttConnectOptions options = new MqttConnectOptions();
            options.setSocketFactory(getSocketFactory());
            mqttClient.connect(options, null, new org.eclipse.paho.client.mqttv3.IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    System.out.println("Connected to AWS IoT Core");
                    subscribeToTopic(PATH_TOPIC);
                    subscribeToTopic(POINT_TOPIC);
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    exception.printStackTrace();
                }
            });
            // 메시지 수신 콜백 추가
            mqttClient.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    // 연결이 끊겼을 때 처리할 내용
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    // Path 토픽 처리
                    else if (topic.equals(PATH_TOPIC)) {
                        new Thread(() -> handlePathMessage(message)).start(); // Path 메시지도 별도의 쓰레드에서 처리
                    }
                    else if (topic.equals(POINT_TOPIC)){
                        new Thread(() -> handlePointMessage(message)).start();
                    }
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {

                }


            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void subscribeToTopic(String topic) {
        try {
            mqttClient.subscribe(topic, 1);  // QoS 1로 토픽 구독
            Log.d(TAG, "Subscribed to topic: " + topic);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    // Path 메시지 처리 (여기서 path 메시지를 처리하고 로그 출력)
    // Path 메시지 처리 (여기서 path 메시지를 처리하고 로그 출력)
    private void handlePathMessage(MqttMessage message) {
        String pathMessage = message.toString();
        Log.d(TAG, "Received Path MQTT message: " + pathMessage);

        try {
            // 메시지를 JSON 형식으로 파싱
            pathMessage = pathMessage.replace("{", "").replace("}", ""); // {} 제거
            pathMessage = pathMessage.replace("\"", ""); // "" 제거
            pathMessage = pathMessage.replace("]", ""); // "]" 제거 (마지막 값에서 문제 발생 방지)
            String[] parts = pathMessage.split(","); // 쉼표로 나눈다

            // 'latitude'와 'longitude' 값 추출
            double latitude = 0.0;
            double longitude = 0.0;

            for (String part : parts) {
                if (part.contains("latitude")) {
                    latitude = Double.parseDouble(part.split(":")[1].trim());
                } else if (part.contains("longitude")) {
                    longitude = Double.parseDouble(part.split(":")[1].trim());
                }

                LatLng latLng = new LatLng(latitude, longitude);

                // 지도에 원(Circle)을 그리기 위해 UI 쓰레드에서 실행
                runOnUiThread(() -> {
                    addCircle(latLng, 10);
                    textView2.setText("Received Path message");
                });
            }

        } catch (NumberFormatException e) {
            Log.e(TAG, "Error parsing path message: " + e.getMessage());
            e.printStackTrace();
        }
    }
    private void handlePointMessage(MqttMessage pmessage) {
        String message = pmessage.toString();
        Log.d("MQTT", "Received Point message: " + message);

        try {
            // 1. 메시지에서 불필요한 대괄호 및 따옴표 제거
            message = message.replace("[", "")  // 시작 대괄호 제거
                    .replace("]", "")  // 끝 대괄호 제거
                    .replace("\"", ""); // 따옴표 제거

            // 2. 각 항목을 }, {로 구분하여 분리
            String[] pointEntries = message.split("\\},\\s*\\{");

            // pointEntries 배열의 길이 확인
            int numberOfPoints = pointEntries.length;
            Log.d("MQTT", "Number of points: " + numberOfPoints);

            // 3. 각 항목에서 데이터를 추출하고 처리
            for (String entry : pointEntries) {
                // 각 엔트리의 시작과 끝에 있는 중괄호 제거
                entry = entry.replace("{", "").replace("}", "").trim();

                // 쉼표로 구분하여 key-value 쌍을 처리
                String[] elements = entry.split(",");

                double latitude = 0.0;
                double longitude = 0.0;
                int order = 0;
                String name = "";

                // 각 요소를 처리
                for (String element : elements) {
                    element = element.trim(); // 앞뒤 공백 제거

                    if (element.startsWith("latitude")) {
                        latitude = Double.parseDouble(element.split(":")[1].trim());
                    } else if (element.startsWith("longitude")) {
                        longitude = Double.parseDouble(element.split(":")[1].trim());
                    } else if (element.startsWith("order")) {
                        order = Integer.parseInt(element.split(":")[1].trim());
                    } else if (element.startsWith("name")) {
                        name = element.split(":")[1].trim();
                    }
                }

                // LatLng 객체 생성
                LatLng latLng = new LatLng(latitude, longitude);

                // 마커 추가 및 마커 위치 리스트에 저장
                String finalName = name;
                int finalOrder = order;
                runOnUiThread(() -> {
                    addMarkerWithColor(latLng, finalName, finalOrder);  // 마커 추가
                    markerPositions.add(latLng);  // 마커 위치 저장
                    adjustCameraToMarkers(markerPositions);  // 카메라 조정
                });
            }

        } catch (Exception e) {
            Log.e("MQTT", "Error parsing point message: " + e.getMessage());
        }
    }
}


