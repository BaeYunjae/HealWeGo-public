package com.example.healwego;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import org.json.JSONException;
import org.json.JSONObject;

public class ChatExitDialogFragment extends DialogFragment {

    private String userId;

    @Nullable
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // SharedPreferences에서 userId 가져오기
        SharedPreferences sharedPref = requireActivity().getSharedPreferences("UserIDPrefs", Context.MODE_PRIVATE);
        userId = sharedPref.getString("userID", ""); // 기본값 설정
    }

    @NonNull
    @Override
    public android.app.Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        // 다이얼로그 빌더 생성
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(requireContext(), R.style.RoundedCornerDialog);

        // 레이아웃 인플레이터 가져오기
        LayoutInflater inflater = requireActivity().getLayoutInflater();

        // 레이아웃을 인플레이트하여 View로 변환
        View dialogView = inflater.inflate(R.layout.activity_chat_popup, null);

        // "예" 버튼과 "아니오" 버튼 처리
        dialogView.findViewById(R.id.yesBtn).setOnClickListener(view -> {
            sendDeleteRequest();  // "예" 버튼 클릭 시 삭제 요청
        });

        dialogView.findViewById(R.id.noBtn).setOnClickListener(view -> {
            dismiss();  // "아니오" 버튼 클릭 시 다이얼로그 닫기
        });

        // 다이얼로그에 커스텀 레이아웃 설정
        builder.setView(dialogView);

        return builder.create();
    }

    private void sendDeleteRequest() {
        String apiURL = "https://18rc8r0oi0.execute-api.ap-northeast-2.amazonaws.com/healwego-stage/room/list";

        JSONObject body = new JSONObject();
        try {
            body.put("Method", "DELETE");
            body.put("User_ID", userId);  // userId 전달
        } catch (JSONException e) {
            Log.e("ChatExitDialog", "DELETE JSON 생성 오류", e);
            return;
        }

        String bodyJson = body.toString();

        // API 요청 함수
        ApiRequestHandler.getJSON(apiURL, "DELETE", new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                String response = (String) msg.obj;

                // 응답 전체 로그 출력
                Log.d("ChatExitDialog", "API 응답: " + response);

                try {
                    JSONObject responseObject = new JSONObject(response);

                    // 여기서 먼저 응답이 어떻게 오는지 확인 필요
                    if (responseObject.has("statusCode")) {
                        int statusCode = responseObject.getInt("statusCode");

                        // DELETE 요청 성공 시 실행할 작업
                        if (statusCode == 200) {
                            Log.i("ChatExitDialog", "방 나가기 성공");

                            if (isAdded() && getActivity() != null) {
                                Toast.makeText(requireContext(), "방에서 나왔습니다", Toast.LENGTH_SHORT).show();
                                requireActivity().finish();  // 현재 액티비티 종료
                            }
                        } else {
                            Log.e("ChatExitDialog", "방 나가기 실패: " + statusCode);
                            if (isAdded() && getActivity() != null) {
                                Toast.makeText(requireContext(), "방 나가기에 실패했습니다.", Toast.LENGTH_SHORT).show();
                            }
                        }
                    } else {
                        // statusCode가 없을 경우 다른 처리 (혹은 추가적인 확인)
                        Log.e("ChatExitDialog", "statusCode 없음, 응답 처리 오류");
                    }
                } catch (JSONException e) {
                    Log.e("ChatExitDialog", "DELETE 응답 처리 오류", e);
                }
            }
        }, bodyJson);
    }

}

