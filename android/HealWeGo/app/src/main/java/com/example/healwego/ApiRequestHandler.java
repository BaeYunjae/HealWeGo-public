package com.example.healwego;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.amazonaws.mobile.client.AWSMobileClient;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class ApiRequestHandler {

    public static void getJSON(final String mUrl, final String connMethod, final Handler responseHandler, String bodyJson) {
        Thread thread = new Thread(() -> {
            String result;
            try {
                URL url = new URL(mUrl);
                Log.i("URL: ", mUrl);
                HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
                httpURLConnection.setReadTimeout(3000);
                httpURLConnection.setConnectTimeout(3000);
                httpURLConnection.setDoInput(true);
                httpURLConnection.setRequestMethod(connMethod);
                httpURLConnection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");  // UTF-8 설정
                httpURLConnection.setUseCaches(false);

                httpURLConnection.setDoOutput(true);

                // 데이터 전송 시 UTF-8로 인코딩
                DataOutputStream wr = new DataOutputStream(httpURLConnection.getOutputStream());
                wr.write(bodyJson.getBytes("UTF-8"));  // UTF-8로 변경
                wr.flush();
                wr.close();

                httpURLConnection.connect();
                int responseStatusCode = httpURLConnection.getResponseCode();
                InputStream inputStream = responseStatusCode == HttpURLConnection.HTTP_OK ? httpURLConnection.getInputStream() : httpURLConnection.getErrorStream();

                // 데이터 수신 시에도 UTF-8로 디코딩
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream, "UTF-8");  // UTF-8로 변경
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    sb.append(line);
                }

                bufferedReader.close();
                httpURLConnection.disconnect();
                result = sb.toString().trim();
            } catch (Exception e) {
                result = e.toString();
            }

            // 외부에서 전달받은 핸들러로 메시지를 전달
            Message message = responseHandler.obtainMessage(101, result);
            responseHandler.sendMessage(message);
        });
        thread.start();
    }
}