package com.example.musicstreamingapp.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.example.musicstreamingapp.PremiumPlansActivity;
import com.example.musicstreamingapp.R;
import com.example.musicstreamingapp.databinding.BottomSheetDownloadGateBinding;
import com.example.musicstreamingapp.network.RetrofitClient;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

/**
 * Gate hiện khi user Free chạm nút "Tải về" ở màn playlist/album (xem ảnh gate_tai_xuong).
 * "Khám phá Premium" mở {@link PremiumPlansActivity}; "Bỏ qua" chỉ đóng.
 */
public class DownloadGateBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_NAME = "arg_name";
    private static final String ARG_COVER = "arg_cover";

    private BottomSheetDownloadGateBinding b;

    /** @param name tên playlist/album hiển thị trong tiêu đề; coverUrl có thể null. */
    public static DownloadGateBottomSheet newInstance(String name, @Nullable String coverUrl) {
        DownloadGateBottomSheet sheet = new DownloadGateBottomSheet();
        Bundle args = new Bundle();
        args.putString(ARG_NAME, name);
        args.putString(ARG_COVER, coverUrl);
        sheet.setArguments(args);
        return sheet;
    }

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        b = BottomSheetDownloadGateBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        String name = getArguments() != null ? getArguments().getString(ARG_NAME, "") : "";
        String coverUrl = getArguments() != null ? getArguments().getString(ARG_COVER) : null;

        b.tvGateTitle.setText(getString(R.string.download_gate_title, name));

        if (coverUrl != null && !coverUrl.isEmpty()) {
            Glide.with(this)
                .load(RetrofitClient.BASE_MEDIA_URL + coverUrl)
                .placeholder(R.drawable.placeholder_gradient)
                .error(R.drawable.placeholder_gradient)
                .centerCrop()
                .into(b.ivGateCover);
        }

        b.btnGateCta.setOnClickListener(v -> {
            startActivity(new Intent(requireContext(), PremiumPlansActivity.class));
            dismiss();
        });
        b.btnGateSkip.setOnClickListener(v -> dismiss());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        b = null;
    }
}
