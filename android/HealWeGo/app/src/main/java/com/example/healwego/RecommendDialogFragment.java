package com.example.healwego;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

public class RecommendDialogFragment extends DialogFragment {

    TextView title, txtText;

    public static RecommendDialogFragment newInstance(String locName, String description) {
        RecommendDialogFragment fragment = new RecommendDialogFragment();
        Bundle args = new Bundle();
        args.putString("locName", locName);
        args.putString("description", description);
        fragment.setArguments(args);
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        androidx.appcompat.app.AlertDialog.Builder builder =
                new androidx.appcompat.app.AlertDialog.Builder(requireContext(), R.style.RoundedCornerDialog);

        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_recommend, null);

        title = dialogView.findViewById(R.id.title);
        txtText = dialogView.findViewById(R.id.txtText);

        if (getArguments() != null) {
            String locName = getArguments().getString("locName");
            String description = getArguments().getString("description");

            title.setText(locName);
            txtText.setText(description);
        }

        dialogView.findViewById(R.id.btnClose).setOnClickListener(v -> dismiss());

        builder.setView(dialogView);
        return builder.create();
    }
}
