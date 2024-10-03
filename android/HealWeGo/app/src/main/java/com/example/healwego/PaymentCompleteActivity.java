package com.example.healwego;


import static android.content.ContentValues.TAG;

import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.json.JSONArray;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.content.Intent;  // Intent를 사용하기 위해 추가
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;  // View를 사용하기 위해 추가
import android.widget.Button;  // Button 사용을 위해 추가
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;


import com.amazonaws.mobile.client.AWSMobileClient;
import com.google.android.gms.maps.model.LatLng;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;


public class PaymentCompleteActivity extends AppCompatActivity {

    private Button confirmButton;  // 버튼 선언
    private static final String BROKER_URL = "ssl://a3boaptn83mu7y-ats.iot.ap-northeast-2.amazonaws.com:8883";
    private static final String CLIENT_ID = "AndroidClient";

    private static final String PATH_TOPIC = "path/001";
    private static final String POINT_TOPIC = "path/points/ros/001";
    private static int price = 0;
    private TextView paymentAmout;
    private TextView completeTextView;

    private String pathMessage="init";
    private String pointMessage;

    private ProgressBar progressBar;

    MqttAsyncClient mqttClient;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.finish_pay);  // finish_pay.xml 파일 연결

        // confirmButton과 연결
        confirmButton = findViewById(R.id.confirmButton);  // XML에서 버튼의 ID로 찾음
        paymentAmout = findViewById(R.id.paymentAmount);
        completeTextView = findViewById(R.id.paymentCompleteMessage);
        // confirmButton 클릭 시 MapPath로 이동
        confirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // MapPath로 이동하는 Intent 생성
                try {
                    mqttClient.disconnect();
                } catch (MqttException e) {
                    throw new RuntimeException(e);
                }
                Intent intent = new Intent(PaymentCompleteActivity.this, MapPath.class);
                SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("first_global_path", pathMessage);
                editor.apply();

               //intent.putExtra("global_Path", pathMessage);
                intent.putExtra("order",pointMessage);
                startActivity(intent);  // MapPath Activity 시작
            }
        });

        progressBar = findViewById(R.id.progressBar);
        progressBar.setVisibility(View.VISIBLE);
        confirmButton.setVisibility(View.GONE);
        paymentAmout.setVisibility(View.GONE);
        completeTextView.setVisibility(View.GONE);
        connectToMqtt();
    }

    //ㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡ
    private void connectToMqtt() {
        try {
            mqttClient = new MqttAsyncClient(BROKER_URL, AWSMobileClient.getInstance().getUsername(), new MemoryPersistence());
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
                    if (topic.equals(PATH_TOPIC)) {
                        handlePathMessage(message);
                    }
                    else if (topic.equals(POINT_TOPIC)){
                        handlePointMessage(message);
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
    @SuppressLint("SetTextI18n")
    private void handlePathMessage(MqttMessage message) {

        Log.w("20241002test", "paymentComplete path"+message.toString() );
        if (!message.toString().equals("null") && !message.toString().equals("\"new\"")) {
            List<Double[]> latLongList = new ArrayList<>();
            if (Objects.equals(pathMessage, "init")){
                pathMessage=message.toString();
            }else {
                pathMessage = pathMessage +"|"+ message;
            }
            System.out.println(message);
            Log.d(TAG, "Received Path MQTT message: " + pathMessage);
            String tempMessage = message.toString();
            try {
                // JSON 배열로 변환하여 처리
                JSONArray jsonArray = new JSONArray(tempMessage); // JSON 배열로 변환

                // 이전 위도와 경도
                double prev_latitude = 0.0;
                double prev_longitude = 0.0;
                boolean isFirstPoint = true;
                double totalDistance = 0.0;

                // JSON 배열에서 각 객체를 순회하며 데이터 처리
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                    double latitude = jsonObject.getDouble("latitude");
                    double longitude = jsonObject.getDouble("longitude");

                    latLongList.add(new Double[]{latitude, longitude});

                    // 거리 계산 로직
                    if (!isFirstPoint) {
                        double distance = haversine(prev_latitude, prev_longitude, latitude, longitude);
                        totalDistance += distance; // 거리 합산
                        System.out.println("Distance between points: " + distance + " km");
                    } else {
                        isFirstPoint = false; // 첫 번째 지점 처리 완료
                    }

                    // 현재 지점을 이전 지점으로 저장
                    prev_latitude = latitude;
                    prev_longitude = longitude;
                }

                // 요소 개수 세기 및 가격 계산
                int elementCount = jsonArray.length();
                price = price + (elementCount * 5);

                Log.d(TAG, "Number of elements in path message: " + elementCount);
                Log.d(TAG, "Total distance: " + totalDistance + " km");

                // UI 업데이트는 메인 스레드에서 실행
                double finalTotalDistance = totalDistance;
                int finalPrice = price;
                runOnUiThread(() -> paymentAmout.setText(finalPrice + "원"));
                runOnUiThread(()->progressBar.setVisibility(View.GONE));  // 데이터 처리 완료 후 로딩 화면 숨김
                runOnUiThread(()->confirmButton.setVisibility(View.VISIBLE));
                runOnUiThread(()->paymentAmout.setVisibility(View.VISIBLE));
                runOnUiThread(()->completeTextView.setVisibility(View.VISIBLE));

            } catch (Exception e) {
                Log.e(TAG, "Error handling path message: " + e.getMessage());
                e.printStackTrace();

            }
        }
    }

    // 두 위도와 경도 사이의 거리를 계산하는 하버사인 공식
    private double haversine(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371.0; // 지구의 반지름 (단위: km)
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c; // 두 지점 간의 거리 반환 (단위: km)
    }

    private void handlePointMessage(MqttMessage pmessage) {
        pointMessage = pmessage.toString();

        Log.w("20241002test", "paymentComplete point"+pointMessage );
        Log.d("MQTT", "Received Point message: " + pointMessage);

    }

    private SSLSocketFactory getSocketFactory() {
        try {
            // CA 인증서 로드
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            InputStream caInput = getAssets().open("AmazonRootCA1.pem");
            X509Certificate caCert = (X509Certificate) cf.generateCertificate(caInput);

            // 클라이언트 인증서 로드
            InputStream crtInput = getAssets().open("certificate.pem.crt");
            X509Certificate clientCert = (X509Certificate) cf.generateCertificate(crtInput);

            // Private Key 로드
            PrivateKey privateKey = loadPrivateKey();

            // KeyStore 생성
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(null, null);
            keyStore.setCertificateEntry("caCert", caCert);
            keyStore.setCertificateEntry("clientCert", clientCert);
            keyStore.setKeyEntry("privateKey", privateKey, "password".toCharArray(), new java.security.cert.Certificate[]{clientCert});

            // TrustManagerFactory 및 KeyManagerFactory 초기화
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(keyStore);

            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(keyStore, "password".toCharArray());

            // SSLContext 초기화
            SSLContext sslContext = SSLContext.getInstance("TLSv1.3");
            sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

            return sslContext.getSocketFactory();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private PrivateKey loadPrivateKey() throws Exception {
        PemReader pemReader = new PemReader(new InputStreamReader(getAssets().open("private.pem.key")));
        PemObject pemObject = pemReader.readPemObject();
        byte[] keyBytes = pemObject.getContent();
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA"); // 또는 "EC" (키 타입에 따라 다름)
        return keyFactory.generatePrivate(keySpec);
    }
}
