package com.example.musicstreamingapp.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.musicstreamingapp.PremiumPlansActivity;
import com.example.musicstreamingapp.R;

public class PremiumFragment extends Fragment {

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_premium, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        view.findViewById(R.id.btn_premium_start).setOnClickListener(v ->
            startActivity(new Intent(requireContext(), PremiumPlansActivity.class)));

        bindReason(view, R.id.reason_no_ads,   R.drawable.ic_no_ads,        R.string.premium_no_ads);
        bindReason(view, R.id.reason_download, R.drawable.ic_download,      R.string.premium_download);
        bindReason(view, R.id.reason_shuffle,  R.drawable.ic_shuffle,       R.string.premium_shuffle);
        bindReason(view, R.id.reason_quality,  R.drawable.ic_audio_quality, R.string.premium_quality);
        bindReason(view, R.id.reason_friends,  R.drawable.ic_friends,       R.string.premium_friends);
        bindReason(view, R.id.reason_queue,    R.drawable.ic_queue_add,     R.string.premium_queue);
    }

    private void bindReason(View root, int rootId, int iconRes, int textRes) {
        View row = root.findViewById(rootId);
        ((ImageView) row.findViewById(R.id.iv_reason_icon)).setImageResource(iconRes);
        ((TextView) row.findViewById(R.id.tv_reason_text)).setText(textRes);
    }
}
