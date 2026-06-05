package com.example.musicstreamingapp.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.musicstreamingapp.PremiumPlansActivity;
import com.example.musicstreamingapp.databinding.BottomSheetShuffleGateBinding;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

/**
 * Popup hiện khi user Free chạm nút trộn bài để cố tắt trộn.
 * Free bị ép luôn bật trộn bài — chỉ Premium mới tắt được.
 * Nút "Khám phá Premium" mở {@link PremiumPlansActivity}.
 */
public class ShuffleGateBottomSheet extends BottomSheetDialogFragment {

    private BottomSheetShuffleGateBinding b;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        b = BottomSheetShuffleGateBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        b.btnShuffleGateCta.setOnClickListener(v -> {
            startActivity(new Intent(requireContext(), PremiumPlansActivity.class));
            dismiss();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        b = null;
    }
}
