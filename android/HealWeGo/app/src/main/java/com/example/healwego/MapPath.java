package com.example.healwego;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;

import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobile.client.UserState;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.snackbar.Snackbar;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.io.IOException;
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
import java.util.Locale;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;
import org.json.JSONObject;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import com.google.android.gms.maps.model.BitmapDescriptor;

public class MapPath extends AppCompatActivity
        implements OnMapReadyCallback,
        ActivityCompat.OnRequestPermissionsResultCallback{

    private GoogleMap mMap;
    private Marker currentMarker = null;
    private Marker currentMarkerWithImage;
    private static final String TAG = "googlemap_example";
    private static final int GPS_ENABLE_REQUEST_CODE = 2001;
    private static final int UPDATE_INTERVAL_MS = 1000;  // 1초
    private static final int FASTEST_UPDATE_INTERVAL_MS = 500; // 0.5초
    private Toast currentToast;
    private List<Polyline> polylines = new ArrayList<>();  // 그려진 선들을 저장하는 리스트

    // onRequestPermissionsResult에서 수신된 결과에서 ActivityCompat.requestPermissions를 사용한 퍼미션 요청을 구별하기 위해 사용됩니다.
    private static final int PERMISSIONS_REQUEST_CODE = 100;
    boolean needRequest = false;

    // 앱을 실행하기 위해 필요한 퍼미션을 정의합니다.
    String[] REQUIRED_PERMISSIONS  = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};  // 외부 저장소

    Location mCurrentLocatiion;
    LatLng currentPosition;

    /*
    0 : 탑승 안한 상태
    1 : 탑승 중
    2 : 비상 정지 상태
     */
    int state = 0;
    int isFinished = 0;
    boolean board_avail = false;

    private FusedLocationProviderClient mFusedLocationClient;
    private LocationRequest locationRequest;
    private Location location;
    private View mLayout;  // Snackbar 사용하기 위해서는 View가 필요합니다.
    // (참고로 Toast에서는 Context가 필요했습니다.)

    private static final String BROKER_URL = "ssl://a3boaptn83mu7y-ats.iot.ap-northeast-2.amazonaws.com:8883";
    private static final String CLIENT_ID = "AndroidClient";
    private static final String GPS_TOPIC = "gps/001";
    private static final String PATH_TOPIC = "path/001";
    private static final String POINT_TOPIC = "path/points/ros/001";

    private static final String SIGNAL_ROS_TOPIC = "signal/ros/001";
    private static final String SIGNAL_APP_TOPIC = "signal/app/001";



    private MqttAsyncClient mqttClient;

    private TextView textView;  // TextView 선언
    private String init_path;
    private String init_order;
    boolean firstCameraUpdate = false;
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // SharedPreferences에서 데이터를 복원
        SharedPreferences preferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        init_path = preferences.getString("init_path", null);  // init_path 복원
        init_order = preferences.getString("init_order", null);  // init_order 복원




    }

    @Override
    protected void onPause() {
        super.onPause();
        // 데이터를 SharedPreferences에 저장
        SharedPreferences preferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("init_path", init_path);  // init_path 저장
        editor.putString("init_order", init_order);  // init_order 저장
        editor.apply();  // 변경 사항을 저장
        System.out.println(init_path);
        System.out.println(init_path);
        System.out.println(init_path);
        System.out.println("onPause");
    }

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.path_map);

        // 뒤로가기 버튼을 처리하는 부분
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {

                // 예약 후 홈 페이지로 돌아갑니다.
                Intent intent = new Intent(MapPath.this, MainActivity.class);
                startActivity(intent);
            }
        });

        mLayout = findViewById(R.id.map_path);

        locationRequest = new LocationRequest()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(UPDATE_INTERVAL_MS)
                .setFastestInterval(FASTEST_UPDATE_INTERVAL_MS);

        LocationSettingsRequest.Builder builder =
                new LocationSettingsRequest.Builder();

        builder.addLocationRequest(locationRequest);

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        Button leftBtn = findViewById(R.id.leftButton);
        Button rightBtn = findViewById(R.id.rightButton);

        // leftButton 클릭 이벤트 처리
        leftBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 여기에 leftButton을 눌렀을 때 실행할 코드를 작성합니다.
                // 필요에 따라 지도의 카메라 이동, 마커 추가, 상태 변경 등의 작업을 여기에 추가
                handleLeftButtonClick();
            }
        });

        // rightButton 클릭 이벤트 처리
        rightBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 여기에 rightButton을 눌렀을 때 실행할 코드를 작성합니다.
                // 필요에 따라 지도의 카메라 이동, 마커 추가, 상태 변경 등의 작업을 여기에 추가
                handleRightButtonClick();
            }
        });

        // 홈버튼클릭 이벤트처리
        ImageButton homeBtn = findViewById(R.id.backToHomeButton);
        homeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 예약 후 홈 페이지로 돌아갑니다.
                Intent intent = new Intent(MapPath.this, MainActivity.class);
                startActivity(intent);
            }
        });
        textView = findViewById(R.id.testtext);
        connectToMqtt();

        Intent getIntent = getIntent();
        if(init_path!=null) {
            init_path = getIntent.getStringExtra("global_Path");
        }
        if(init_order!=null) {
            init_order = getIntent.getStringExtra("order");
        }

    }
    // Left Button 클릭 시 수행할 작업을 위한 함수
    private void handleLeftButtonClick() {
        Button btn = findViewById(R.id.leftButton);
        if (state == 0 && board_avail){
            sendMessage(SIGNAL_APP_TOPIC,"{" +
                    "\"command\" : \"boarding\"" +
                    "}");
            state = 1;
            btn.setText("하차");
        }
        else if (state == 0){
            showToastMessage("아직 차가 도착하지 않았습니다");
        }
        else if(state == 1){
            if(isFinished==1) {
                new AlertDialog.Builder(this)
                        .setMessage("이용해 주셔서 감사합니다")  // 메시지 설정
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // Yes를 눌렀을 때 동작
                                board_avail = false;
                                Intent intent = new Intent(MapPath.this, MainActivity.class);
                                startActivity(intent);
                            }
                        })
                        .show();  // 다이얼로그 표시
            }else{
                showToastMessage("아직 도착 안했습니다");
            }
        }else if(state == 2){
           showToastMessage("이동부터 하세요");
        }
    }

    // Right Button 클릭 시 수행할 작업을 위한 함수
    private void handleRightButtonClick() {
        Button btn = findViewById(R.id.rightButton);
        if (state == 0){
            showToastMessage("탑승부터 하세요");
        }
        else if(state == 1){
            new AlertDialog.Builder(this)
                    .setTitle("주의")  // 타이틀 설정
                    .setMessage("차량을 정지하시겠습니까?")  // 메시지 설정
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // Yes를 눌렀을 때 동작
                            state = 2;
                            btn.setText("이동 재개");
                            sendMessage(SIGNAL_APP_TOPIC,"{" +
                                    "\"command\" : \"stop\"" +
                                    "}");
                        }
                    })
                    .setNegativeButton("No", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss(); // 다이얼로그 닫기
                        }
                    })
                    .show();  // 다이얼로그 표시
        }else if(state == 2){
            state = 1;
            btn.setText("비상 정지");
            sendMessage(SIGNAL_APP_TOPIC,"{" +
                    "\"command\" : \"resume\"" +
                    "}");
        }
    }

    @Override
    public void onMapReady(final GoogleMap googleMap) {
        Log.d(TAG, "onMapReady :");

        mMap = googleMap;

        //런타임 퍼미션 요청 대화상자나 GPS 활성 요청 대화상자 보이기전에
        //지도의 초기위치를 서울로 이동
        setDefaultLocation();

        //런타임 퍼미션 처리
        // 1. 위치 퍼미션을 가지고 있는지 체크합니다.
        int hasFineLocationPermission = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        int hasCoarseLocationPermission = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION);

        if (hasFineLocationPermission == PackageManager.PERMISSION_GRANTED &&
                hasCoarseLocationPermission == PackageManager.PERMISSION_GRANTED   ) {

            // 2. 이미 퍼미션을 가지고 있다면
            // ( 안드로이드 6.0 이하 버전은 런타임 퍼미션이 필요없기 때문에 이미 허용된 걸로 인식합니다.)

            // destLocationName과 startLocationName 처리
            Intent getIntent = getIntent();
            String destLocationName = getIntent.getStringExtra("dest_locationName");
            String startLocationName = getIntent.getStringExtra("start_locationName");

            // 두 위치가 있는 경우
            if (destLocationName != null && startLocationName != null) {
                LatLng destLatLng = getLatLngFromLocationName(destLocationName);
                LatLng startLatLng = getLatLngFromLocationName(startLocationName);

                if (destLatLng != null && startLatLng != null) {
                    // 두 위치 모두 표시할 수 있도록 카메라 위치를 조정
                    LatLngBounds.Builder builder = new LatLngBounds.Builder();
                    builder.include(destLatLng);
                    builder.include(startLatLng);
                    LatLngBounds bounds = builder.build();

                    // 마커 추가
                    addMarker(destLatLng, destLocationName);
                    addMarker(startLatLng, startLocationName);

                    // 카메라 이동 및 줌 조정
                    int padding = 100; // 경계에 패딩 추가
                    CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, padding);
                    mMap.moveCamera(cu);
                }
            }
            // destLocationName만 있는 경우
            else if (destLocationName != null) {
                LatLng destLatLng = getLatLngFromLocationName(destLocationName);
                if (destLatLng != null) {
                    addMarker(destLatLng, destLocationName);
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(destLatLng, 15));
                }
            }
            // startLocationName만 있는 경우
            else if (startLocationName != null) {
                LatLng startLatLng = getLatLngFromLocationName(startLocationName);
                if (startLatLng != null) {
                    addMarker(startLatLng, startLocationName);
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(startLatLng, 15));
                }
            } else {
                // 내 위치로 이동
                setDefaultLocation();
            }
            startLocationUpdates(); // 3. 위치 업데이트 시작

            if (init_path != null && init_order != null) {
                handleFirstPathMessage(init_path);
                handleFirstPointMessage(init_order);
            }

            // 데이터를 사용하여 필요한 작업 수행
            if (init_path != null) {
                handleFirstPathMessage(init_path);
            }

            if (init_order != null) {
                handleFirstPointMessage(init_order);
            }
        }else {  //2. 퍼미션 요청을 허용한 적이 없다면 퍼미션 요청이 필요합니다. 2가지 경우(3-1, 4-1)가 있습니다.

            // 3-1. 사용자가 퍼미션 거부를 한 적이 있는 경우에는
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, REQUIRED_PERMISSIONS[0])) {

                // 3-2. 요청을 진행하기 전에 사용자가에게 퍼미션이 필요한 이유를 설명해줄 필요가 있습니다.
                Snackbar.make(mLayout, "이 앱을 실행하려면 위치 접근 권한이 필요합니다.",
                        Snackbar.LENGTH_INDEFINITE).setAction("확인", new View.OnClickListener() {

                    @Override
                    public void onClick(View view) {
                        // 3-3. 사용자게에 퍼미션 요청을 합니다. 요청 결과는 onRequestPermissionResult에서 수신됩니다.
                        ActivityCompat.requestPermissions( MapPath.this, REQUIRED_PERMISSIONS,
                                PERMISSIONS_REQUEST_CODE);
                    }
                }).show();

            } else {
                // 4-1. 사용자가 퍼미션 거부를 한 적이 없는 경우에는 퍼미션 요청을 바로 합니다.
                // 요청 결과는 onRequestPermissionResult에서 수신됩니다.
                ActivityCompat.requestPermissions( this, REQUIRED_PERMISSIONS,
                        PERMISSIONS_REQUEST_CODE);
            }
        }

        mMap.getUiSettings().setMyLocationButtonEnabled(true);
        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                Log.d( TAG, "onMapClick :");
            }
        });
    }

    LocationCallback locationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            super.onLocationResult(locationResult);
            List<Location> locationList = locationResult.getLocations();

            if (!locationList.isEmpty()) {
                location = locationList.get(locationList.size() - 1);
                //location = locationList.get(0);
                currentPosition
                        = new LatLng(location.getLatitude(), location.getLongitude());
                String markerTitle = getCurrentAddress(currentPosition);
                String markerSnippet = "위도:" + String.valueOf(location.getLatitude())
                        + " 경도:" + String.valueOf(location.getLongitude());
                Log.d(TAG, "onLocationResult : " + markerSnippet);
                //현재 위치에 마커 생성하고 이동
                setCurrentLocation(location, markerTitle, markerSnippet);
                mCurrentLocatiion = location;
            }
        }
    };

    private List<LatLng> markerPositions = new ArrayList<>();

    private void adjustCameraToMarkers(List<LatLng> markerPositions) {
        if (markerPositions == null || markerPositions.isEmpty()) {
            return;
        }

        // LatLngBounds를 생성하여 모든 마커를 포함
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (LatLng position : markerPositions) {
            builder.include(position);
        }
        LatLngBounds bounds = builder.build();

        // 화면 크기에 맞게 카메라 업데이트 (패딩을 추가하여 경계를 벗어나지 않게 설정)
        int padding = 400; // 패딩은 원하는 대로 설정 (px 단위)
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, padding);
        mMap.moveCamera(cameraUpdate);
    }


    private void startLocationUpdates() {

        if (!checkLocationServicesStatus()) {
            Log.d(TAG, "startLocationUpdates : call showDialogForLocationServiceSetting");
            showDialogForLocationServiceSetting();
        }else {
            int hasFineLocationPermission = ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION);
            int hasCoarseLocationPermission = ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION);
            if (hasFineLocationPermission != PackageManager.PERMISSION_GRANTED ||
                    hasCoarseLocationPermission != PackageManager.PERMISSION_GRANTED   ) {
                Log.d(TAG, "startLocationUpdates : 퍼미션 안가지고 있음");
                return;
            }
            Log.d(TAG, "startLocationUpdates : call mFusedLocationClient.requestLocationUpdates");
            mFusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
            if (checkPermission())
                mMap.setMyLocationEnabled(true);
        }
    }

    // 위치명으로 LatLng 가져오기
    private LatLng getLatLngFromLocationName(String locationName) {
        Geocoder geocoder = new Geocoder(this);
        List<Address> addressList;
        try {
            addressList = geocoder.getFromLocationName(locationName, 1);
            if (addressList != null && !addressList.isEmpty()) {
                Address address = addressList.get(0);
                return new LatLng(address.getLatitude(), address.getLongitude());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    // 마커 추가하는 함수
    private void addMarker(LatLng latLng, String title) {
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(latLng);
        markerOptions.title(title);
        mMap.addMarker(markerOptions);
        // 마커 위치를 리스트에 추가
        markerPositions.add(latLng);
        // 모든 마커가 보이도록 카메라 조정
        adjustCameraToMarkers(markerPositions);
    }

    private void addMarkerWithImage(LatLng latLng) {
        // 만약 기존 마커가 있으면 제거
        if (currentMarkerWithImage != null) {
            currentMarkerWithImage.remove();
        }

        // 리소스에서 비트맵 이미지를 불러오고 크기를 조정
        Bitmap originalBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.logo);
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, 200, 200, false);  // 원하는 크기로 조절 (100x100 예시)

        // 리사이즈한 비트맵을 마커에 적용
        BitmapDescriptor customMarker = BitmapDescriptorFactory.fromBitmap(resizedBitmap);

        MarkerOptions markerOptions = new MarkerOptions()
                .position(latLng)  // 마커 위치 설정
                .icon(customMarker)  // 커스텀 이미지 설정
                .anchor(0.5f, 0.5f);  // 이미지의 중심이 좌표에 맞도록 설정

        // 마커를 지도에 추가하고 해당 마커를 저장
        currentMarkerWithImage = mMap.addMarker(markerOptions);
    }

    private void addMarkerWithNumber(LatLng latLng, int order) {
        // 순서(order)에 따라 이미지를 선택
        int drawableId;
        switch (order) {
            case 1:
                drawableId = R.drawable.one; // 1번 마커에 사용할 이미지
                break;
            case 2:
                drawableId = R.drawable.two; // 2번 마커에 사용할 이미지
                break;
            case 3:
                drawableId = R.drawable.three; // 3번 마커에 사용할 이미지
                break;
            case 4:
                drawableId = R.drawable.four; // 기본 마커 이미지
                break;
            case 5:
                drawableId = R.drawable.five; // 기본 마커 이미지
                break;
            default:
                drawableId = 0;
        }

        if(drawableId!=0){
            // 리소스에서 비트맵 이미지를 불러오고 크기를 조정
            Bitmap originalBitmap = BitmapFactory.decodeResource(getResources(), drawableId);
            Bitmap resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, 150, 150, false);  // 원하는 크기로 조절 (200x200 예시)

            // 리사이즈한 비트맵을 마커에 적용
            BitmapDescriptor customMarker = BitmapDescriptorFactory.fromBitmap(resizedBitmap);

            // 마커 옵션 설정
            MarkerOptions markerOptions = new MarkerOptions()
                    .position(latLng)  // 마커 위치 설정
                    .icon(customMarker)  // 커스텀 이미지 설정
                    .anchor(0.5f, 0.5f);  // 이미지의 중심이 좌표에 맞도록 설정

            // 마커를 지도에 추가하고 해당 마커를 저장
            mMap.addMarker(markerOptions);
        }

    }

    private void showToastMessage(String message) {
        // 현재 표시 중인 Toast가 있다면 취소
        if (currentToast != null) {
            currentToast.cancel();
        }

        // 새로운 Toast 생성 및 표시
        currentToast = Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT);
        currentToast.show();
    }


    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart");
        if (checkPermission()) {
            Log.d(TAG, "onStart : call mFusedLocationClient.requestLocationUpdates");
            mFusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
            if (mMap!=null)
                mMap.setMyLocationEnabled(true);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mFusedLocationClient != null) {
            Log.d(TAG, "onStop : call stopLocationUpdates");
            mFusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }

    public String getCurrentAddress(LatLng latlng) {
        //지오코더... GPS를 주소로 변환
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());

        List<Address> addresses;

        try {
            addresses = geocoder.getFromLocation(
                    latlng.latitude,
                    latlng.longitude,
                    1);
        } catch (IOException ioException) {
            //네트워크 문제
            Toast.makeText(this, "지오코더 서비스 사용불가", Toast.LENGTH_LONG).show();
            return "지오코더 서비스 사용불가";
        } catch (IllegalArgumentException illegalArgumentException) {
            Toast.makeText(this, "잘못된 GPS 좌표", Toast.LENGTH_LONG).show();
            return "잘못된 GPS 좌표";
        }

        if (addresses == null || addresses.isEmpty()) {
            Toast.makeText(this, "주소 미발견", Toast.LENGTH_LONG).show();
            return "주소 미발견";
        } else {
            Address address = addresses.get(0);
            return address.getAddressLine(0);
        }
    }

    public boolean checkLocationServicesStatus() {
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }


    public void setCurrentLocation(Location location, String markerTitle, String markerSnippet) {
// 따라다니는 마커를 제거 (기존 마커를 추가하지 않음)
        if (currentMarker != null) {
            currentMarker.remove();
            currentMarker = null;  // 마커를 완전히 제거하고 null로 초기화
        }
        LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());


        if (firstCameraUpdate) {
            // 카메라를 사용자의 현재 위치로 이동
            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLng(currentLatLng);
            mMap.moveCamera(cameraUpdate);
            firstCameraUpdate=true;
        }
    }


    public void setDefaultLocation() {
        //디폴트 위치, Seoul
        LatLng DEFAULT_LOCATION = new LatLng(37.56, 126.97);
        String markerTitle = "위치정보 가져올 수 없음";
        String markerSnippet = "위치 퍼미션과 GPS 활성 요부 확인하세요";

        if (currentMarker != null) currentMarker.remove();

        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(DEFAULT_LOCATION);
        markerOptions.title(markerTitle);
        markerOptions.snippet(markerSnippet);
        markerOptions.draggable(true);
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));

        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(DEFAULT_LOCATION, 15);
        mMap.moveCamera(cameraUpdate);
    }


    //ㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡ
    //여기부터는 런타임 퍼미션 처리을 위한 메소드들
    private boolean checkPermission() {
        int hasFineLocationPermission = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        int hasCoarseLocationPermission = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION);
        if (hasFineLocationPermission == PackageManager.PERMISSION_GRANTED &&
                hasCoarseLocationPermission == PackageManager.PERMISSION_GRANTED   ) {
            return true;
        }
        return false;
    }
    /*
     * ActivityCompat.requestPermissions를 사용한 퍼미션 요청의 결과를 리턴받는 메소드입니다.
     */
    @Override
    public void onRequestPermissionsResult(int permsRequestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grandResults) {
        super.onRequestPermissionsResult(permsRequestCode, permissions, grandResults);
        if (permsRequestCode == PERMISSIONS_REQUEST_CODE && grandResults.length == REQUIRED_PERMISSIONS.length) {
            // 요청 코드가 PERMISSIONS_REQUEST_CODE 이고, 요청한 퍼미션 개수만큼 수신되었다면
            boolean check_result = true;
            // 모든 퍼미션을 허용했는지 체크합니다.
            for (int result : grandResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    check_result = false;
                    break;
                }
            }

            if (check_result) {

                // 퍼미션을 허용했다면 위치 업데이트를 시작합니다.
                startLocationUpdates();
            } else {
                // 거부한 퍼미션이 있다면 앱을 사용할 수 없는 이유를 설명해주고 앱을 종료합니다.2 가지 경우가 있습니다.
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, REQUIRED_PERMISSIONS[0])
                        || ActivityCompat.shouldShowRequestPermissionRationale(this, REQUIRED_PERMISSIONS[1])) {
                    // 사용자가 거부만 선택한 경우에는 앱을 다시 실행하여 허용을 선택하면 앱을 사용할 수 있습니다.
                    Snackbar.make(mLayout, "퍼미션이 거부되었습니다. 앱을 다시 실행하여 퍼미션을 허용해주세요. ",
                            Snackbar.LENGTH_INDEFINITE).setAction("확인", new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            finish();
                        }
                    }).show();
                } else {
                    // "다시 묻지 않음"을 사용자가 체크하고 거부를 선택한 경우에는 설정(앱 정보)에서 퍼미션을 허용해야 앱을 사용할 수 있습니다.
                    Snackbar.make(mLayout, "퍼미션이 거부되었습니다. 설정(앱 정보)에서 퍼미션을 허용해야 합니다. ",
                            Snackbar.LENGTH_INDEFINITE).setAction("확인", new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            finish();
                        }
                    }).show();
                }
            }
        }
    }

    //여기부터는 GPS 활성화를 위한 메소드들
    private void showDialogForLocationServiceSetting() {

        AlertDialog.Builder builder = new AlertDialog.Builder(MapPath.this);
        builder.setTitle("위치 서비스 비활성화");
        builder.setMessage("앱을 사용하기 위해서는 위치 서비스가 필요합니다.\n"
                + "위치 설정을 수정하실래요?");
        builder.setCancelable(true);
        builder.setPositiveButton("설정", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                Intent callGPSSettingIntent
                        = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivityForResult(callGPSSettingIntent, GPS_ENABLE_REQUEST_CODE);
            }
        });
        builder.setNegativeButton("취소", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        builder.create().show();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case GPS_ENABLE_REQUEST_CODE:
                //사용자가 GPS 활성 시켰는지 검사
                if (checkLocationServicesStatus()) {
                    if (checkLocationServicesStatus()) {

                        Log.d(TAG, "onActivityResult : GPS 활성화 되있음");
                        needRequest = true;
                        return;
                    }
                }
                break;
        }
    }

    //ㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡㅡ
    //MQTT
    private void connectToMqtt() {
        try {
            mqttClient = new MqttAsyncClient(BROKER_URL, CLIENT_ID, new MemoryPersistence());
            MqttConnectOptions options = new MqttConnectOptions();
            options.setSocketFactory(getSocketFactory());
            mqttClient.connect(options, null, new org.eclipse.paho.client.mqttv3.IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    System.out.println("Connected to AWS IoT Core");
                    subscribeToTopic(GPS_TOPIC);
                    subscribeToTopic(PATH_TOPIC);
                    subscribeToTopic(POINT_TOPIC);
                    subscribeToTopic(SIGNAL_ROS_TOPIC);
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
                    // GPS 토픽 처리
                    if (topic.equals(GPS_TOPIC)) {
                        new Thread(() -> handleGpsMessage(message)).start(); // GPS 메시지는 별도의 쓰레드에서 처리
                    }
                    // Path 토픽 처리
                    else if (topic.equals(PATH_TOPIC)) {
                        new Thread(() -> handlePathMessage(message)).start(); // Path 메시지도 별도의 쓰레드에서 처리
                    }
                    else if (topic.equals(POINT_TOPIC)){
                        new Thread(() -> handlePointMessage(message)).start();
                    }
                    else if (topic.equals(SIGNAL_ROS_TOPIC)){
                        String jsonString = message.toString();
                        JSONObject jsonObject = new JSONObject(jsonString);

                        // finish와 user_id 값 추출
                        int finish = jsonObject.getInt("finish");
                        String userId = jsonObject.getString("user_id");

                        // 각 key-value 쌍을 처리


                        // 값 출력 (저장하는 대신 예시로 출력)
                        System.out.println("Finish: " + finish);
                        System.out.println("User ID: " + userId);
                        String Name = AWSMobileClient.getInstance().getUsername();

                        if(finish==1){
                            isFinished = 1;
                        }
                        else if(userId.equals(Name)) {
                            board_avail = true;
                        }
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

    // MQTT 토픽 구독
    private void subscribeToTopic(String topic) {
        try {
            mqttClient.subscribe(topic, 1);  // QoS 1로 토픽 구독
            Log.d(TAG, "Subscribed to topic: " + topic);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    // GPS 메시지 처리
    private void handleGpsMessage(MqttMessage message) {

        // MQTT 메시지 수신 (String 형식)
        String receivedMessage = message.toString();
        Log.d(TAG, "Received GPS MQTT message: " + receivedMessage);

        // 메시지가 {"latitude":34.412,"longitude":123.21312} 형식의 String이므로 수동으로 파싱
        try {
            // 문자열에서 불필요한 문자 제거 ({} 및 "latitude", "longitude" 제거)
            receivedMessage = receivedMessage.replace("{", "").replace("}", "");
            receivedMessage = receivedMessage.replace("\"latitude\":", "").replace("\"longitude\":", "");

            // 쉼표를 기준으로 나누어 위도와 경도 추출
            String[] parts = receivedMessage.split(",");
            if (parts.length == 2) {
                double latitude = Double.parseDouble(parts[0].trim());
                double longitude = Double.parseDouble(parts[1].trim());

                // 지도에 점을 추가
                LatLng latLng = new LatLng(latitude, longitude);
                runOnUiThread(() -> addMarkerWithImage(latLng));

                // UI 업데이트: 수신한 메시지를 TextView에 표시
                runOnUiThread(() -> textView.setText("Latitude: " + latitude + ", Longitude: " + longitude));
            } else {
                Log.e(TAG, "Invalid GPS message format: " + receivedMessage);
            }
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
    }

    // Path 메시지 처리 (여기서 path 메시지를 처리하고 로그 출력)
    // Path 메시지 처리 (여기서 path 메시지를 처리하고 로그 출력)
    private void handlePathMessage(MqttMessage message) {
        String pathMessage = message.toString();
        Log.d(TAG, "Received Path MQTT message: " + pathMessage);
        init_path=pathMessage;
        List<Double[]> latLongList = new ArrayList<>();
        if(pathMessage.equals("new")){
            removeAllPolylines();
        }else {
            try {
                pathMessage = pathMessage.replace("{", "").replace("}", ""); // {} 제거
                pathMessage = pathMessage.replace("\"", ""); // "" 제거
                pathMessage = pathMessage.replace("]", ""); // "]" 제거 (마지막 값에서 문제 발생 방지)
                String[] parts = pathMessage.split(","); // 쉼표로 나눈다


                double latitude = 0.0;
                double longitude = 0.0;
                for (String part : parts) {
                    if (part.contains("latitude")) {
                        latitude = Double.parseDouble(part.split(":")[1].trim());
                    }
                    if (part.contains("longitude")) {
                        longitude = Double.parseDouble(part.split(":")[1].trim());
                        latLongList.add(new Double[]{latitude, longitude});
                    }


                }
                runOnUiThread(() -> {
                    boolean isFirstPoint = true;
                    double ui_latitude = 0.0;
                    double ui_longitude = 0.0;
                    double prev_ui_latitude = 0.0;
                    double prev_ui_longitude = 0.0;
                    for (Double[] latLong : latLongList) {

                        ui_latitude = latLong[0];
                        ui_longitude = latLong[1];

                        if (!isFirstPoint) {
                            LatLng startLatLng = new LatLng(ui_latitude, ui_longitude);
                            LatLng endLatLng = new LatLng(prev_ui_latitude, prev_ui_longitude);

                            drawLineBetweenPoints(mMap, startLatLng, endLatLng, Color.RED, 20);
                        } else {
                            isFirstPoint = false; // 첫 번째 지점 처리 완료
                        }

                        prev_ui_latitude = ui_latitude;
                        prev_ui_longitude = ui_longitude;
                    }
                });


            } catch (NumberFormatException e) {
                Log.e(TAG, "Error parsing path message: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    public void drawLineBetweenPoints(GoogleMap map, LatLng startLatLng, LatLng endLatLng, int color, float width) {
        // PolylineOptions 생성하고 선의 시작점과 끝점 추가
        PolylineOptions polylineOptions = new PolylineOptions()
                .add(startLatLng)  // 시작 좌표
                .add(endLatLng)    // 끝 좌표
                .width(width)      // 선의 두께
                .color(color);     // 선의 색깔

        Polyline polyline = map.addPolyline(polylineOptions);
        polylines.add(polyline);  // 선을 리스트에 추가
    }

    public void removeAllPolylines() {
        runOnUiThread(() -> {
            // 저장된 모든 Polyline 객체 삭제
            for (Polyline polyline : polylines) {
                polyline.remove();  // 지도에서 Polyline 제거
            }
            polylines.clear();  // 리스트 비우기
        });
    }

    private void handlePointMessage(MqttMessage pmessage) {
        String message = pmessage.toString();
        Log.d("MQTT", "Received Point message: " + message);
        init_order=message;
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

    private void handleFirstPathMessage(String message) {
        String pathMessage = message;

        List<Double[]> latLongList = new ArrayList<>();

        try {
            pathMessage = pathMessage.replace("new","");
            pathMessage = pathMessage.replace("[","");
            pathMessage = pathMessage.replace("{", "").replace("}", ""); // {} 제거
            pathMessage = pathMessage.replace("\"", ""); // "" 제거
            pathMessage = pathMessage.replace("]", ""); // "]" 제거 (마지막 값에서 문제 발생 방지)

            System.out.println(pathMessage);
            String[] parts = pathMessage.split(","); // 쉼표로 나눈다

            // 이전 위도와 경도
            double prev_latitude = 0.0;
            double prev_longitude = 0.0;
            boolean isFirstPoint = true;
            double latitude = 0.0;
            double longitude = 0.0;

            for (String part : parts) {
                if (part.contains("latitude")) {
                    latitude = Double.parseDouble(part.split(":")[1].trim());
                }

                if (part.contains("longitude")) {
                    longitude = Double.parseDouble(part.split(":")[1].trim());
                    latLongList.add(new Double[]{latitude, longitude});
                }
            }

            for (Double[] latLong : latLongList) {
                latitude = latLong[0];
                longitude = latLong[1];

                if (!isFirstPoint) {
                    LatLng startLatLng = new LatLng(latitude, longitude);  // 서울 좌표 예시
                    LatLng endLatLng = new LatLng(prev_latitude, prev_longitude);    // 다른 좌표 예시
                    drawLineBetweenPoints(mMap, startLatLng, endLatLng, Color.RED, 5);
                } else {
                    isFirstPoint = false; // 첫 번째 지점 처리 완료
                }

                // 현재 지점을 다음 계산을 위한 이전 지점으로 저장
                prev_latitude = latitude;
                prev_longitude = longitude;
            }
        } catch (NumberFormatException e) {
            Log.e(TAG, "Error parsing path message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleFirstPointMessage(String pmessage) {
        String message = pmessage;

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
                int finalOrder = order;
                runOnUiThread(() -> {
                    addMarkerWithNumber(latLng,finalOrder);// 마커 추가
                    markerPositions.add(latLng);  // 마커 위치 저장
                    adjustCameraToMarkers(markerPositions);  // 카메라 조정
                });
            }
        } catch (Exception e) {
            Log.e("MQTT", "Error parsing point message: " + e.getMessage());
        }
    }

    // 마커를 추가하는 함수, 방문 순서에 따른 색상을 지정
    private void addMarkerWithColor(LatLng latLng, String title, int order) {

        // 순서에 따라 마커 색상 설정
        float markerColor;
        switch (order) {
            case 1:
                markerColor = BitmapDescriptorFactory.HUE_RED;
                break;
            case 2:
                markerColor = BitmapDescriptorFactory.HUE_GREEN;
                break;
            case 3:
                markerColor = BitmapDescriptorFactory.HUE_BLUE;
                break;
            default:
                markerColor = BitmapDescriptorFactory.HUE_ORANGE; // 기본값
                break;
        }

        // 마커 옵션 설정
        MarkerOptions markerOptions = new MarkerOptions()
                .position(latLng)
                .title(title)
                .icon(BitmapDescriptorFactory.defaultMarker(markerColor));
        mMap.addMarker(markerOptions);
    }

    private void sendMessage(String topic, String message) {
        try {
            MqttMessage mqttMessage = new MqttMessage();
            mqttMessage.setPayload(message.getBytes());
            mqttClient.publish(topic, mqttMessage);
            System.out.println("Message sent to topic '" + topic + "': " + message);
        } catch (MqttException e) {
            e.printStackTrace();
        }
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

