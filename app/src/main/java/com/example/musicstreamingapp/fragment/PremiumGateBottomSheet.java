package com.example.musicstreamingapp.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.musicstreamingapp.PremiumPlansActivity;
import com.example.musicstreamingapp.R;
import com.example.musicstreamingapp.databinding.BottomSheetPremiumGateBinding;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

/**
 * Popup yêu cầu nâng cấp khi user Free chạm vào tính năng Premium.
 * Dùng: PremiumGateBottomSheet.newInstance("Chất lượng âm thanh cao").show(fm, tag);
 */
public class PremiumGateBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_FEATURE = "arg_feature";

    private BottomSheetPremiumGateBinding b;

    public static PremiumGateBottomSheet newInstance(String featureName) {
        PremiumGateBottomSheet sheet = new PremiumGateBottomSheet();
        Bundle args = new Bundle();
        args.putString(ARG_FEATURE, featureName);
        sheet.setArguments(args);
        return sheet;
    }

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        b = BottomSheetPremiumGateBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        String feature = getArguments() != null ? getArguments().getString(ARG_FEATURE, "") : "";
        b.tvGateDesc.setText(getString(R.string.premium_gate_desc, feature));

        b.btnGateUpgrade.setOnClickListener(v -> {
            startActivity(new Intent(requireContext(), PremiumPlansActivity.class));
            dismiss();
        });
        b.btnGateLater.setOnClickListener(v -> dismiss());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        b = null;
    }
}
