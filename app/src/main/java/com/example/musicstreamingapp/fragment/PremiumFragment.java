package com.example.musicstreamingapp.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.musicstreamingapp.PremiumPlansActivity;
import com.example.musicstreamingapp.R;
import com.example.musicstreamingapp.databinding.FragmentPremiumBinding;
import com.example.musicstreamingapp.databinding.ItemPremiumReasonBinding;
import com.example.musicstreamingapp.model.Subscription;
import com.example.musicstreamingapp.viewmodel.SubscriptionViewModel;
import com.example.musicstreamingapp.viewmodel.VmFactory;

public class PremiumFragment extends Fragment {

    private FragmentPremiumBinding b;
    private SubscriptionViewModel vm;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        b = FragmentPremiumBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        vm = new ViewModelProvider(this, new VmFactory(requireContext()))
            .get(SubscriptionViewModel.class);

        b.btnPremiumStart.setOnClickListener(v ->
            startActivity(new Intent(requireContext(), PremiumPlansActivity.class)));

        bindReason(b.reasonNoAds,    R.drawable.ic_no_ads,         R.string.premium_no_ads);
        bindReason(b.reasonDownload, R.drawable.ic_download,       R.string.premium_download);
        bindReason(b.reasonShuffle,  R.drawable.ic_shuffle,        R.string.premium_shuffle);
        bindReason(b.reasonQuality,  R.drawable.ic_audio_quality,  R.string.premium_quality);
        bindReason(b.reasonFriends,  R.drawable.ic_friends,        R.string.premium_friends);
        bindReason(b.reasonQueue,    R.drawable.ic_queue_add,      R.string.premium_queue);

        vm.subscription().observe(getViewLifecycleOwner(), this::renderSubscription);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Tải lại mỗi lần quay về tab để phản ánh ngay sau khi nâng cấp.
        if (vm != null) vm.loadCurrentSubscription();
    }

    private void renderSubscription(@Nullable Subscription sub) {
        if (b == null) return;
        boolean premium = sub != null && sub.isPremium();

        if (premium) {
            b.layoutCurrentPlan.setVisibility(View.VISIBLE);
            b.tvCurrentPlan.setText(getString(R.string.premium_current_plan, planName(sub.getPlan())));

            String end = formatDate(sub.getEndDate());
            if (end != null) {
                b.tvCurrentExpiry.setVisibility(View.VISIBLE);
                b.tvCurrentExpiry.setText(getString(R.string.premium_current_expiry, end));
            } else {
                b.tvCurrentExpiry.setVisibility(View.GONE);
            }

            // FE chặn tạo trùng: đã Premium thì không cho subscribe nữa.
            b.btnPremiumStart.setText(R.string.premium_already_active);
            b.btnPremiumStart.setEnabled(false);
            b.btnPremiumStart.setAlpha(0.6f);
        } else {
            b.layoutCurrentPlan.setVisibility(View.GONE);
            b.btnPremiumStart.setText(R.string.premium_start);
            b.btnPremiumStart.setEnabled(true);
            b.btnPremiumStart.setAlpha(1f);
        }
    }

    private String planName(String plan) {
        if (plan == null) return "";
        switch (plan) {
            case "INDIVIDUAL": return getString(R.string.plan_name_individual);
            case "STUDENT":    return getString(R.string.plan_name_student);
            case "FAMILY":     return getString(R.string.plan_name_family);
            default:           return plan;
        }
    }

    /** "yyyy-MM-dd" → "dd/MM/yyyy"; trả null nếu không parse được. */
    @Nullable
    private String formatDate(@Nullable String iso) {
        if (iso == null) return null;
        String datePart = iso.length() >= 10 ? iso.substring(0, 10) : iso;
        String[] p = datePart.split("-");
        if (p.length != 3) return iso;
        return p[2] + "/" + p[1] + "/" + p[0];
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
