package com.example.healwego;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.List;

public class RoomListAdapter extends RecyclerView.Adapter<RoomListAdapter.RoomViewHolder> {
    private List<Room> roomList;  // Room 객체 리스트
    private Context context;

    // API 요청을 위한 URL
    private String mURL = "https://18rc8r0oi0.execute-api.ap-northeast-2.amazonaws.com/healwego-stage/";

    public RoomListAdapter(Context context, List<Room> roomList) {
        this.roomList = roomList;
        this.context = context;
    }

    // MyHandler를 static으로 선언하여 메모리 누수를 방지하고, WeakReference로 액티비티 참조
    private static class MyHandler extends Handler {
        private final WeakReference<RoomListAdapter> weakReference;

        // Deprecated 경고를 해결하기 위해 Looper를 명시적으로 받도록 수정
        public MyHandler(Looper looper, RoomListAdapter roomListAdapter) {
            super(looper);  // 명시적으로 Looper를 전달
            weakReference = new WeakReference<>(roomListAdapter);
        }

        @Override
        public void handleMessage(Message msg) {
            RoomListAdapter roomListAdapter = weakReference.get();
            if (roomListAdapter != null) {
                // API 응답을 JSON으로 변환하고 처리
                String jsonString = (String) msg.obj;
                Log.i("RoomListAdapter", "응답: " + jsonString);

                // 응답을 처리하고 UI 업데이트
                roomListAdapter.handleEnterResponse(jsonString);
            }
        }
    }

    // WeakReference로 Activity에 대한 참조를 가진 MyHandler 객체
    private final RoomListAdapter.MyHandler mHandler = new RoomListAdapter.MyHandler(Looper.getMainLooper(), this);

    @Override
    public RoomViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // room_list_item.xml 레이아웃을 가져와 ViewHolder에 설정
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.room_list_item, parent, false);
        return new RoomViewHolder(view);
    }

    @Override
    public void onBindViewHolder(RoomViewHolder holder, int position) {
        Room room = roomList.get(position);
        String userId = room.getUserId();

        holder.textRoomInfo.setText(
                room.getRoomName()
        );
        holder.locationInfo.setText(
                room.getLocName()
        );
        holder.themeTag.setText(
                room.getTheme()
        );
        holder.genderTag.setText(
                room.getGender()
        );

        String peopleFill = room.getNumUsers() + "/4";
        holder.peopleInfo.setText(
                peopleFill
        );

        String timeTemp = room.getTime();
        String timeInfo = timeTemp.substring(0, 3) + ":" + timeTemp.substring(3);
        holder.timeTag.setText(
                timeInfo
        );

        // 첫 번째 방이고 option 값이 특정 조건을 만족할 때 배경을 변경
        if (position == 0 && room.getOption() == 1) {  // option 값이 1인 경우 예시
            holder.itemView.setBackgroundResource(R.drawable.first_room_background);

            // TextView 글자 색상 변경
            holder.textRoomInfo.setTextColor(context.getResources().getColor(R.color.white));
            holder.peopleInfo.setTextColor(context.getResources().getColor(R.color.white));
        }

        // 클릭 이벤트 처리
        holder.itemView.setOnClickListener(v -> {
            // API 요청 준비
            JSONObject body = new JSONObject();
            String connMethod = "PATCH";

            try {
                body.put("Method", connMethod);
                body.put("User_ID", userId);
                body.put("Rooms_ID", room.getRoomId());
            } catch (JSONException e) {
                Log.e("RoomListAdapter", "JSON 생성 오류", e);
                return;
            }

            String bodyJson = body.toString();
            String apiUrl = mURL + "room/enter";

            // API 요청 시작 (비동기 처리)
            ApiRequestHandler.getJSON(apiUrl, connMethod, mHandler, bodyJson);
            Log.i("RoomListAdapter", "요청: " + bodyJson);
        });
    }

    @Override
    public int getItemCount() {
        return roomList.size();
    }

    public static class RoomViewHolder extends RecyclerView.ViewHolder {
        TextView textRoomInfo;
        TextView locationInfo;
        TextView themeTag;
        TextView genderTag;
        TextView timeTag;
        TextView peopleInfo;

        public RoomViewHolder(View itemView) {
            super(itemView);
            textRoomInfo = itemView.findViewById(R.id.textRoomInfo);
            locationInfo = itemView.findViewById(R.id.locationInfo);
            themeTag = itemView.findViewById(R.id.themeTag);
            genderTag = itemView.findViewById(R.id.genderTag);
            timeTag = itemView.findViewById(R.id.timeTag);
            peopleInfo = itemView.findViewById(R.id.peopleInfo);

        }
    }

    // 방 입장 가능 여부 처리
    private void handleEnterResponse(String response) {
        try {
            // 1. 응답을 JSONObject로 변환
            JSONObject responseObject = new JSONObject(response);

            // 2. "body" 필드를 문자열로 가져오고 JSONObject로 변환
            String body = responseObject.optString("body", null);
            if (body == null) {
                return;
            }
            JSONObject bodyJson = new JSONObject(body);

            // 3. "option" 값 처리
            int option = bodyJson.optInt("option", -1);  // 기본값으로 -1 반환
            String msg = bodyJson.optString("msg", "");
            JSONObject users = bodyJson.optJSONObject("users");
            String masterId = bodyJson.optString("master_ID");
            String roomId = bodyJson.optString("Rooms_ID");
            JSONArray chats = bodyJson.optJSONArray("Chats");

            Log.i("RoomListAdapter", "참여자: " + users);
            Log.i("RoomListAdapter", "옵션: " + option + "메시지: " + msg);

            if (option != 0) {
                // 방 입장 가능 -> ChatActivity로 이동
                String roomName = bodyJson.optString("roomname", "");  // 방 이름

                Intent intent = new Intent(context, ChatActivity.class);
                intent.putExtra("roomTitle", roomName);
                intent.putExtra("usersInfo", users.toString());
                intent.putExtra("hostId", masterId);
                intent.putExtra("roomId", roomId);
                intent.putExtra("isNewEnter", option);
                if (option == 2){
                    intent.putExtra("chats", chats.toString());
                }
                context.startActivity(intent);
            } else {
                // 방 입장 불가
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
            }

        } catch (JSONException e) {
            // JSON 파싱 오류 처리
            Log.i("RoomList", "JSON 파싱 오류 : " + e);
        }
    }
}

