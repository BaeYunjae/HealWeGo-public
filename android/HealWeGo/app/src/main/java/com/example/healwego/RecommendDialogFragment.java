package com.example.healwego;

import android.app.Dialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

public class RecommendDialogFragment extends DialogFragment {

    TextView title, txtText;
    ImageView locImg;

    public static RecommendDialogFragment newInstance(byte[] locImg, String locName, String subtitle, String description) {
        RecommendDialogFragment fragment = new RecommendDialogFragment();
        Bundle args = new Bundle();
        args.putByteArray("locImg", locImg);
        args.putString("locName", locName);
        args.putString("subtitle", subtitle);
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

        locImg = dialogView.findViewById(R.id.locImage);
        title = dialogView.findViewById(R.id.titleText);
        txtText = dialogView.findViewById(R.id.txtText);

        if (getArguments() != null) {
            String subtitle = getArguments().getString("subtitle");
            String description = getArguments().getString("description");
            byte[] decodedString = getArguments().getByteArray("locImg");
            Bitmap decodedImage = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);

            locImg.setImageBitmap(decodedImage);
            title.setText(subtitle);
            txtText.setText(description);
        }

        dialogView.findViewById(R.id.btnClose).setOnClickListener(v -> dismiss());

        builder.setView(dialogView);
        return builder.create();
    }
}
