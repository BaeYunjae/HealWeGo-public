package com.example.healwego;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
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
import android.widget.TextView;
import android.widget.Toast;

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
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.snackbar.Snackbar;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class PathSelect extends AppCompatActivity
        implements OnMapReadyCallback,
        ActivityCompat.OnRequestPermissionsResultCallback{

    private GoogleMap mMap;
    private Marker currentMarker = null;
    private List<LatLng> markerPositions = new ArrayList<>();

    private static final String TAG = "googlemap_example";
    private static final int GPS_ENABLE_REQUEST_CODE = 2001;
    private static final int UPDATE_INTERVAL_MS = 1000;  // 1초
    private static final int FASTEST_UPDATE_INTERVAL_MS = 500; // 0.5초

    // onRequestPermissionsResult에서 수신된 결과에서 ActivityCompat.requestPermissions를 사용한 퍼미션 요청을 구별하기 위해 사용됩니다.
    private static final int PERMISSIONS_REQUEST_CODE = 100;
    boolean needRequest = false;

    // 앱을 실행하기 위해 필요한 퍼미션을 정의합니다.
    String[] REQUIRED_PERMISSIONS  = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};  // 외부 저장소

    Location mCurrentLocatiion;
    LatLng currentPosition;

    private FusedLocationProviderClient mFusedLocationClient;
    private LocationRequest locationRequest;
    private Location location;

    private View mLayout;  // Snackbar 사용하기 위해서는 View가 필요합니다.
    // (참고로 Toast에서는 Context가 필요했습니다.)

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // 현재 상태를 저장 (dest와 location 데이터를 저장)
        TextView dest_textview = findViewById(R.id.dest_textview);
        TextView start_textview = findViewById(R.id.start_textview);

        // TextView에 있는 값을 저장
        outState.putString("dest_locationName", dest_textview.getText().toString());
        outState.putString("start_locationName", start_textview.getText().toString());
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.select_path);

        mLayout = findViewById(R.id.layout_select_path);

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

        // SearchView와 Google Map 연결
        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // UI 요소 초기화
        TextView dest_text = findViewById(R.id.dest_textview);
        TextView start_text = findViewById(R.id.start_textview);
        String destLocationName = "";
        String startLocationName ="";
        String startLongitude = "";
        String startLatitude = "";
        String destLatitude = "";
        String destLongitude ="";

        Button startButton = findViewById(R.id.start_button);
        Button destButton = findViewById(R.id.dest_button);


        // 저장된 상태가 있으면 복구
        if (savedInstanceState != null) {
            dest_text.setText(savedInstanceState.getString("dest_locationName", "목적지를 설정하세요"));
            start_text.setText(savedInstanceState.getString("start_locationName", "출발지를 설정하세요"));
        } else {
            // Intent로부터 데이터 받기
            Intent getintent = getIntent();
            destLocationName = getintent.getStringExtra("dest_locationName");
            startLocationName = getintent.getStringExtra("start_locationName");
            startLongitude = getintent.getStringExtra("start_longitude");
            startLatitude = getintent.getStringExtra("start_latitude");
            destLatitude = getintent.getStringExtra("dest_latitude");
            destLongitude = getintent.getStringExtra("dest_longitude");
            // 기존 출발지와 목적지를 그대로 유지
            if (destLocationName != null) {
                dest_text.setText(destLocationName);
            }
            if (startLocationName != null) {
                start_text.setText(startLocationName);
            }
        }

        String finalDestLatitude = destLatitude;
        String finalDestLongitude = destLongitude;
        String finalStartLatitude = startLatitude;
        String finalStartLongitude = startLongitude;

        startButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                Intent intent = new Intent(PathSelect.this, StartSelect.class);
                intent.putExtra("dest_locationName", dest_text.getText().toString());
                intent.putExtra("dest_latitude", finalDestLatitude);
                intent.putExtra("dest_longitude", finalDestLongitude);
                startActivity(intent);
            }
        });

        destButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){

                Intent intent2 = new Intent(PathSelect.this, DestSelect.class);

                intent2.putExtra("start_locationName", start_text.getText().toString());
                intent2.putExtra("start_latitude", finalStartLatitude);
                intent2.putExtra("start_longitude", finalStartLongitude);
                startActivity(intent2);
            }
        });

        Button complete_btn = findViewById(R.id.complete_button);
        complete_btn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                String dest = (String) dest_text.getText();
                String start = (String) start_text.getText();

                if(!Objects.equals(dest, "Please Select dest") && !Objects.equals(start, "Please Select start")){
                    Intent intent = new Intent(PathSelect.this, AloneSetActivity.class);
                    intent.putExtra("dest_latitude", finalDestLatitude);
                    intent.putExtra("dest_longitude", finalDestLongitude);
                    intent.putExtra("start_latitude", finalStartLatitude);
                    intent.putExtra("start_longitude", finalStartLongitude);
                    intent.putExtra("dest_locationName", dest_text.getText().toString());
                    startActivity(intent);
                }else{
                    Toast.makeText(PathSelect.this, "모두 설정하세요", Toast.LENGTH_SHORT).show();
                }
            }
        });

        Button myLocationButton = findViewById(R.id.my_location_button);
        myLocationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mCurrentLocatiion != null) {
                    LatLng currentLatLng = new LatLng(mCurrentLocatiion.getLatitude(), mCurrentLocatiion.getLongitude());
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15));
                } else {
                    Toast.makeText(PathSelect.this, "현재 위치를 사용할 수 없습니다.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // 뒤로가기 버튼을 처리하는 부분
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // 뒤로가기 버튼을 눌렀을 때 실행할 코드
                Intent intent = new Intent(PathSelect.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            }
        });
    }

    // 마커가 하나라도 있으면 스크롤을 비활성화하는 함수
    private void disableScrollIfMarkersExist() {
        if (!markerPositions.isEmpty()) {
            // 마커가 하나라도 있으면 스크롤 비활성화
            mMap.getUiSettings().setScrollGesturesEnabled(false);
        } else {
            // 마커가 없으면 스크롤 활성화
            mMap.getUiSettings().setScrollGesturesEnabled(true);
        }
    }

    @Override
    public void onMapReady(final GoogleMap googleMap) {
        Log.d(TAG, "onMapReady :");

        mMap = googleMap;
        //런타임 퍼미션 요청 대화상자나 GPS 활성 요청 대화상자 보이기전에
        //지도의 초기위치를 서울로 이동
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

            Log.i("20241007test","dest"+destLocationName);
            // 두 위치가 있는 경

            if (destLocationName != null && startLocationName != null) {
                // destLocationName만 있는 경우/.
                if( (!destLocationName.equals("출발지를 설정하세요") && !startLocationName.equals("목적지를 설정하세요"))){
                    LatLng destLatLng = getLatLngFromLocationName(destLocationName);
                    LatLng startLatLng = getLatLngFromLocationName(startLocationName);

                    Log.w("20241007test","이거맞음?" );
                    if (destLatLng != null && startLatLng   != null) {
                        // 두 위치 모두 표시할 수 있도록 카메라 위치를 조정
                        LatLngBounds.Builder builder = new LatLngBounds.Builder();
                        builder.include(destLatLng);
                        builder.include(startLatLng);
                        LatLngBounds bounds = builder.build();
                        // 마커 추가
                        addMarker(destLatLng, destLocationName,false);
                        addMarker(startLatLng, startLocationName,true);
                        // 카메라 이동 및 줌 조정
                        int padding = 150; // 경계에 패딩 추가
                        CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, padding);
                        mMap.moveCamera(cu);
                    }else if (destLatLng!=null){
                        Log.i("20241007test","목적지설정");
                        // 두 위치 모두 표시할 수 있도록 카메라 위치를 조정
                        LatLngBounds.Builder builder = new LatLngBounds.Builder();
                        builder.include(destLatLng);
                        LatLngBounds bounds = builder.build();
                        // 마커 추가
                        addMarker(destLatLng, destLocationName,false);
                        // 카메라 이동 및 줌 조정
                        //int padding = 150; // 경계에 패딩 추가
                        //CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, padding);
                        //mMap.moveCamera(cu);
                    }else if(startLatLng!=null){
                        Log.i("20241007test","출발지설정");
                        // 두 위치 모두 표시할 수 있도록 카메라 위치를 조정
                        LatLngBounds.Builder builder = new LatLngBounds.Builder();
                        builder.include(startLatLng);
                        LatLngBounds bounds = builder.build();
                        // 마커 추가
                        addMarker(startLatLng, startLocationName,true);
                        // 카메라 이동 및 줌 조정
                        //int padding = 200; // 경계에 패딩 추가
                        //CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, padding);
                        //mMap.moveCamera(cu);
                    }
                }
            }
            else {
                // 내 위치로 이동
                setDefaultLocation();
            }

            startLocationUpdates(); // 3. 위치 업데이트 시작
            disableScrollIfMarkersExist();
        }else {  //2. 퍼미션 요청을 허용한 적이 없다면 퍼미션 요청이 필요합니다. 2가지 경우(3-1, 4-1)가 있습니다.
            // 3-1. 사용자가 퍼미션 거부를 한 적이 있는 경우에는
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, REQUIRED_PERMISSIONS[0])) {
                // 3-2. 요청을 진행하기 전에 사용자가에게 퍼미션이 필요한 이유를 설명해줄 필요가 있습니다.
                Snackbar.make(mLayout, "이 앱을 실행하려면 위치 접근 권한이 필요합니다.",
                        Snackbar.LENGTH_INDEFINITE).setAction("확인", new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        // 3-3. 사용자게에 퍼미션 요청을 합니다. 요청 결과는 onRequestPermissionResult에서 수신됩니다.
                        ActivityCompat.requestPermissions( PathSelect.this, REQUIRED_PERMISSIONS, PERMISSIONS_REQUEST_CODE);
                    }
                }).show();
            } else {
                // 4-1. 사용자가 퍼미션 거부를 한 적이 없는 경우에는 퍼미션 요청을 바로 합니다.
                // 요청 결과는 onRequestPermissionResult에서 수신됩니다.
                ActivityCompat.requestPermissions( this, REQUIRED_PERMISSIONS,
                        PERMISSIONS_REQUEST_CODE);
            }
        }
        // 카메라가 이동을 멈출 때 마커를 중앙으로 이동시키는 리스너 추가
        mMap.setOnCameraIdleListener(new GoogleMap.OnCameraIdleListener() {
            @Override
            public void onCameraIdle() {
                // 카메라의 중앙 위치를 가져옴
                LatLng centerPosition = mMap.getCameraPosition().target;

            }
        });

        mMap.getUiSettings().setMyLocationButtonEnabled(false);

        mMap.setOnCameraMoveStartedListener(new GoogleMap.OnCameraMoveStartedListener() {
            @Override
            public void onCameraMoveStarted(int reason) {
                if (reason == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE) {
                    // 사용자가 지도를 움직이기 시작하면 위치 업데이트를 일시 중지
                    mFusedLocationClient.removeLocationUpdates(locationCallback);
                }
            }
        });


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

            if (locationList.size() > 0) {
                location = locationList.get(locationList.size() - 1);
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
        int padding = 200; // 패딩은 원하는 대로 설정 (px 단위)
        Log.i("20241007test","여기오는거지? "+markerPositions.size());

        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, padding);
        mMap.moveCamera(cameraUpdate);


        mMap.animateCamera(CameraUpdateFactory.zoomTo(15)); // 줌 레벨을 10으로 설정
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
    private void addMarker(LatLng latLng, String title, boolean isStartLocation) {
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(latLng);

        if (isStartLocation) {
            // 출발지 마커는 빨간색
            markerOptions.title("출발지");
            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
        } else {
            // 도착지 마커는 파란색
            markerOptions.title("도착지");
            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
        }
        Marker marker  = mMap.addMarker(markerOptions);
        // 마커 위치를 리스트에 추가
        assert marker != null;
        markerPositions.add(latLng);
        // 모든 마커가 보이도록 카메라 조정
        adjustCameraToMarkers(markerPositions);
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

        if (addresses == null || addresses.size() == 0) {
            Toast.makeText(this, "주소 미발견", Toast.LENGTH_LONG).show();
            return "주소 미발견";
        } else {
            Address address = addresses.get(0);
            return address.getAddressLine(0).toString();
        }
    }

    public boolean checkLocationServicesStatus() {
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    public void setCurrentLocation(Location location, String markerTitle, String markerSnippet) {
        LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
    }

    public void setDefaultLocation() {
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

    // ActivityCompat.requestPermissions를 사용한 퍼미션 요청의 결과를 리턴받는 메소드입니다.
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
        AlertDialog.Builder builder = new AlertDialog.Builder(PathSelect.this);
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
}

