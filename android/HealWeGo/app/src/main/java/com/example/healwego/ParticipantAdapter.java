package com.example.healwego;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.util.Log;

import android.content.Context;
import androidx.core.content.ContextCompat;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class ParticipantAdapter extends RecyclerView.Adapter<ParticipantAdapter.ParticipantViewHolder> {

    private ArrayList<Participant> participants;
    private OnReadyStatusChangedListener readyStatusChangedListener; // 상태 변경 리스너
    private Context context;

    public ParticipantAdapter(Context context, ArrayList<Participant> participants) {
        this.context = context;
        this.participants = participants;
    }

    @NonNull
    @Override
    public ParticipantViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.participant_item, parent, false);
        return new ParticipantViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ParticipantViewHolder holder, int position) {
        Participant participant = participants.get(position);


        // 참여자 이름과 역할 설정 (이름을 먼저 초기화)
        String displayName = participant.getName() + " " + participant.getRole();
        holder.nameTextView.setText(displayName);  // 이름과 역할을 명확히 초기화

        // isReady 상태에 따라 텍스트 및 버튼 설정
        if (participant.isReady()) {
            holder.readyText.setVisibility(View.VISIBLE);
        } else {
            holder.readyText.setVisibility(View.GONE);
        }

        // 사용자가 자신일 경우 이름을 노란색으로 강조
        if (participant.isCurrentUser()) {
            holder.nameTextView.setTextColor(ContextCompat.getColor(context, R.color.teal));
        } else {
            holder.nameTextView.setTextColor(ContextCompat.getColor(context, R.color.darkcyan));
        }

        // READY 버튼 클릭 시 상태 변경
        holder.readyText.setOnClickListener(v -> {
            boolean isReady = !participant.isReady();
            participant.setReady(isReady);

            // 리스너를 통해 ChatActivity에서 상태 변경 처리
            if (readyStatusChangedListener != null) {
                readyStatusChangedListener.onReadyStatusChanged(participant.getName(), isReady);
            }

            notifyDataSetChanged(); // UI 업데이트
        });
    }

    @Override
    public int getItemCount() {
        return participants.size();
    }


    public static class ParticipantViewHolder extends RecyclerView.ViewHolder {

        TextView nameTextView;
        TextView readyStatusTextView; // READY 상태 표시
        TextView readyText;  // READY / CANCEL 버튼

        public ParticipantViewHolder(@NonNull View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.participantName);
            readyText = itemView.findViewById(R.id.readyText);  // 추가된 READY 상태 버튼
        }
    }

    // READY 상태 변경 리스너 인터페이스
    public interface OnReadyStatusChangedListener {
        void onReadyStatusChanged(String userName, boolean isReady);
    }
}
