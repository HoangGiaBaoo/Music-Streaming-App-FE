package com.example.musicstreamingapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.example.musicstreamingapp.databinding.ActivityListeningStatsBinding;
import com.example.musicstreamingapp.model.ListeningStats;
import com.example.musicstreamingapp.viewmodel.ListeningStatsViewModel;
import com.example.musicstreamingapp.viewmodel.VmFactory;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class ListeningStatsActivity extends AppCompatActivity {

    private ActivityListeningStatsBinding b;
    private ListeningStatsViewModel vm;
    private int renderedCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityListeningStatsBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        setSupportActionBar(b.toolbar);
        b.toolbar.setNavigationOnClickListener(v -> finish());

        vm = new ViewModelProvider(this, new VmFactory(this)).get(ListeningStatsViewModel.class);
        vm.sections().observe(this, this::renderSections);
        vm.loadIfNeeded();
    }

    private void renderSections(List<ListeningStatsViewModel.Section> sections) {
        if (sections == null) return;
        // Append only new sections (avoid full re-render after config change since
        // ViewModel survives — re-add all when restoring inflated state was lost).
        if (sections.size() < renderedCount) {
            b.containerSections.removeAllViews();
            renderedCount = 0;
        }
        for (int i = renderedCount; i < sections.size(); i++) {
            renderSection(sections.get(i));
        }
        renderedCount = sections.size();
    }

    private void renderSection(ListeningStatsViewModel.Section section) {
        ListeningStats s = section.stats;
        int offset = section.offset;

        View card = LayoutInflater.from(this)
            .inflate(R.layout.item_stats_section, b.containerSections, false);

        TextView label = card.findViewById(R.id.tv_label);
        TextView range = card.findViewById(R.id.tv_range);
        View cards = card.findViewById(R.id.layout_cards);
        View empty = card.findViewById(R.id.empty_card);
        TextView emptyTitle = card.findViewById(R.id.tv_empty_title);
        TextView emptyBody = card.findViewById(R.id.tv_empty_body);

        label.setText(s.periodLabel != null ? s.periodLabel
            : (offset == 0 ? getString(R.string.stats_period_this_week)
                            : getString(R.string.stats_period_last_week)));
        range.setText(formatRange(s.periodStart, s.periodEnd));

        if (s.isEmpty()) {
            cards.setVisibility(View.GONE);
            empty.setVisibility(View.VISIBLE);
            if (offset == 0) {
                emptyTitle.setText(R.string.stats_empty_title);
                emptyBody.setText(R.string.stats_empty_body);
            } else {
                emptyTitle.setText(R.string.stats_week_empty);
                emptyBody.setText(R.string.stats_week_empty_body);
            }
        } else {
            cards.setVisibility(View.VISIBLE);
            empty.setVisibility(View.GONE);

            View artistCard = card.findViewById(R.id.card_artist);
            View trackCard = card.findViewById(R.id.card_track);

            if (s.topArtist != null) {
                ((TextView) card.findViewById(R.id.tv_artist_name)).setText(s.topArtist.name);
                if (s.topArtist.avatarUrl != null) {
                    Glide.with(this)
                        .load(MainActivity.buildMediaUrl(s.topArtist.avatarUrl))
                        .placeholder(R.drawable.ic_avatar_placeholder)
                        .into((CircleImageView) card.findViewById(R.id.iv_artist));
                }
                artistCard.setVisibility(View.VISIBLE);
            } else {
                artistCard.setVisibility(View.INVISIBLE);
            }

            if (s.topTrack != null) {
                ((TextView) card.findViewById(R.id.tv_track_title)).setText(s.topTrack.title);
                if (s.topTrack.coverUrl != null) {
                    Glide.with(this)
                        .load(MainActivity.buildMediaUrl(s.topTrack.coverUrl))
                        .placeholder(R.drawable.placeholder_gradient)
                        .into((ImageView) card.findViewById(R.id.iv_track));
                }
                trackCard.setVisibility(View.VISIBLE);
            } else {
                trackCard.setVisibility(View.INVISIBLE);
            }
        }

        b.containerSections.addView(card);
    }

    private String formatRange(String start, String end) {
        if (start == null || end == null) return "";
        return formatDate(start) + " – " + formatDate(end);
    }

    private String formatDate(String iso) {
        if (iso == null || iso.length() < 10) return "";
        String[] parts = iso.substring(0, 10).split("-");
        if (parts.length != 3) return iso;
        return Integer.parseInt(parts[2]) + " thg " + Integer.parseInt(parts[1]);
    }
}
