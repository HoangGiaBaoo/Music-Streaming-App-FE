package com.example.musicstreamingapp.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.musicstreamingapp.PremiumPlansActivity;
import com.example.musicstreamingapp.R;
import com.example.musicstreamingapp.databinding.FragmentPremiumBinding;
import com.example.musicstreamingapp.databinding.ItemPremiumReasonBinding;

public class PremiumFragment extends Fragment {

    private FragmentPremiumBinding b;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        b = FragmentPremiumBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        b.btnPremiumStart.setOnClickListener(v ->
            startActivity(new Intent(requireContext(), PremiumPlansActivity.class)));

        bindReason(b.reasonNoAds,    R.drawable.ic_no_ads,         R.string.premium_no_ads);
        bindReason(b.reasonDownload, R.drawable.ic_download,       R.string.premium_download);
        bindReason(b.reasonShuffle,  R.drawable.ic_shuffle,        R.string.premium_shuffle);
        bindReason(b.reasonQuality,  R.drawable.ic_audio_quality,  R.string.premium_quality);
        bindReason(b.reasonFriends,  R.drawable.ic_friends,        R.string.premium_friends);
        bindReason(b.reasonQueue,    R.drawable.ic_queue_add,      R.string.premium_queue);
    }

    private void bindReason(ItemPremiumReasonBinding row, int iconRes, int textRes) {
        row.ivReasonIcon.setImageResource(iconRes);
        row.tvReasonText.setText(textRes);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        b = null;
    }
}
