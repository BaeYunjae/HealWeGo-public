package com.example.healwego;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.healwego.ChatActivity;
import com.example.healwego.Room;

import java.util.List;

public class RoomListAdapter extends RecyclerView.Adapter<RoomListAdapter.RoomViewHolder> {
    private List<Room> roomList;  // Room 객체 리스트
    private Context context;

    public RoomListAdapter(Context context, List<Room> roomList) {
        this.roomList = roomList;
        this.context = context;
    }

    @Override
    public RoomViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // room_list_item.xml 레이아웃을 가져와 ViewHolder에 설정
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.room_list_item, parent, false);
        return new RoomViewHolder(view);
    }

    @Override
    public void onBindViewHolder(RoomViewHolder holder, int position) {
        Room room = roomList.get(position);  // Room 객체 가져오기

        // Room 정보를 TextView에 설정
        holder.textRoomInfo.setText(
                "Room Name: " + room.getRoomName() +
                        "\nTheme: " + room.getTheme() +
                        "\nLocation: " + room.getLocName() +
                        "\nNumber of Users: " + room.getNumUsers() +
                        "\n" + room.getGender()
        );

        // 클릭 이벤트 처리
        holder.itemView.setOnClickListener(v -> {
            // ChatActivity로 이동
            Intent intent = new Intent(context, ChatActivity.class);
            // Room ID를 Intent로 전달
            intent.putExtra("roomId", room.getRoomId());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return roomList.size();
    }

    public static class RoomViewHolder extends RecyclerView.ViewHolder {
        TextView textRoomInfo;

        public RoomViewHolder(View itemView) {
            super(itemView);
            textRoomInfo = itemView.findViewById(R.id.textRoomInfo);
        }
    }
}
