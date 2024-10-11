package com.example.healwego;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.appcompat.app.AlertDialog;

import com.amazonaws.mobile.client.AWSMobileClient;

import org.json.JSONException;
import org.json.JSONObject;

public class CancelDialogFragment extends DialogFragment {

    private String reservationDetails;  // 전달받은 예약 정보를 저장

    // 예약 정보를 다이얼로그에 전달하는 방법
    public static CancelDialogFragment newInstance(String reservationDetails) {
        CancelDialogFragment fragment = new CancelDialogFragment();
        Bundle args = new Bundle();
        args.putString("reservationDetails", reservationDetails);
        fragment.setArguments(args);
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        reservationDetails = getArguments().getString("reservationDetails");

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext(), R.style.RoundedCornerDialog);
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.activity_cancel_popup, null); // 기존 팝업 레이아웃 사용

        TextView reserveText = dialogView.findViewById(R.id.reserveText);
        reserveText.setText(reservationDetails);  // 예약 정보 설정

        // "예" 버튼
        TextView yesBtn = dialogView.findViewById(R.id.yesBtn);
        yesBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                requestCancelReserve();  // 예약 취소 요청
            }
        });

        // "아니오" 버튼
        TextView noBtn = dialogView.findViewById(R.id.noBtn);
        noBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();  // 다이얼로그 닫기
            }
        });

        // 다이얼로그에 레이아웃을 설정
        builder.setView(dialogView);

        return builder.create();
    }

    private void requestCancelReserve() {
        // JSON 요청 생성
        JSONObject body = new JSONObject();
        String userId = AWSMobileClient.getInstance().getUsername();

        try {
            body.put("Method", "DELETE");  // API 메서드
            body.put("User_ID", userId);   // 사용자 ID
        } catch (JSONException e) {
            e.printStackTrace();
        }

        // API 요청 (예약 취소)
        ApiRequestHandler.getJSON("https://18rc8r0oi0.execute-api.ap-northeast-2.amazonaws.com/healwego-stage/room/list", "DELETE", new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                String response = (String) msg.obj;

                // 응답 처리
                handleCancelResponse(response);
            }
        }, body.toString());
    }

    private void handleCancelResponse(String response) {
        if (getActivity() == null || !isAdded()) {
            return;  // 다이얼로그가 이미 닫힌 경우
        }

        try {
            // 추가 디버깅 로그
            Log.d("CancelDialogFragment", "handleCancelResponse response: " + response);

            JSONObject responseObject = new JSONObject(response);
            int statusCode = responseObject.getInt("statusCode");  // statusCode 가져오기
            String body = responseObject.getString("body");        // body 가져오기

            Log.d("CancelDialogFragment", "Parsed statusCode value: " + statusCode);
            Log.d("CancelDialogFragment", "Parsed body value: " + body);

            if (statusCode != 200) {
                Toast.makeText(getContext(), "예약 취소에 실패했습니다.", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(getContext(), "예약이 정상적으로 취소되었습니다.", Toast.LENGTH_LONG).show();

                // 예약 취소 후 MainActivity 다시 시작
                if (getActivity() != null) {
                    Intent intent = new Intent(getActivity(), MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK); // MainActivity 새로 시작
                    startActivity(intent);
                }

                dismiss(); // 예약 취소 후 다이얼로그 닫기
            }
        } catch (JSONException e) {
            // JSON 파싱에 실패한 경우, 에러 로그 출력
            Log.e("CancelDialogFragment", "JSON 파싱 중 오류 발생: ", e);
        } catch (Exception e) {
            // 그 외 예상치 못한 오류 처리
            Log.e("CancelDialogFragment", "handleCancelResponse 중 오류 발생: ", e);
        }
    }

}
