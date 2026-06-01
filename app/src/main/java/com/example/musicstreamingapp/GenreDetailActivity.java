package com.example.musicstreamingapp;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.palette.graphics.Palette;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.example.musicstreamingapp.adapter.GenreFeedAdapter;
import com.example.musicstreamingapp.data.Resource;
import com.example.musicstreamingapp.databinding.ActivityGenreDetailBinding;
import com.example.musicstreamingapp.model.Genre;
import com.example.musicstreamingapp.model.GenreFeedDto;
import com.example.musicstreamingapp.model.GenreSectionDto;
import com.example.musicstreamingapp.network.RetrofitClient;
import com.example.musicstreamingapp.viewmodel.GenreDetailViewModel;
import com.example.musicstreamingapp.viewmodel.VmFactory;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

public class GenreDetailActivity extends AppCompatActivity {

    public static final String EXTRA_GENRE_ID = "EXTRA_GENRE_ID";
    public static final String EXTRA_GENRE_NAME = "EXTRA_GENRE_NAME";

    private ActivityGenreDetailBinding b;
    private GenreDetailViewModel vm;
    private final List<GenreSectionDto> sections = new ArrayList<>();
    private GenreFeedAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityGenreDetailBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        long genreId = getIntent().getLongExtra(EXTRA_GENRE_ID, -1);
        String genreName = getIntent().getStringExtra(EXTRA_GENRE_NAME);

        setSupportActionBar(b.toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Tiêu đề tạm từ Intent, sẽ ghi đè khi feed về (có tên chuẩn từ backend).
        b.collapsingToolbar.setTitle(genreName != null ? genreName : "");

        adapter = new GenreFeedAdapter(sections);
        b.rvSections.setLayoutManager(new LinearLayoutManager(this));
        b.rvSections.setAdapter(adapter);

        b.shimmerGenre.getRoot().startShimmer();

        vm = new ViewModelProvider(this, new VmFactory(this)).get(GenreDetailViewModel.class);
        vm.feed().observe(this, this::renderFeed);
        vm.load(genreId);
    }

    private void renderFeed(Resource<GenreFeedDto> res) {
        if (res == null) return;
        switch (res.status) {
            case LOADING:
                showShimmer(true);
                break;
            case SUCCESS:
                showShimmer(false);
                bindFeed(res.data);
                break;
            case ERROR:
                showShimmer(false);
                Snackbar.make(b.getRoot(), R.string.error_network, Snackbar.LENGTH_SHORT).show();
                break;
        }
    }

    private void showShimmer(boolean show) {
        if (show) {
            b.shimmerGenre.getRoot().setVisibility(View.VISIBLE);
            b.shimmerGenre.getRoot().startShimmer();
            b.rvSections.setVisibility(View.GONE);
        } else {
            b.shimmerGenre.getRoot().stopShimmer();
            b.shimmerGenre.getRoot().setVisibility(View.GONE);
            b.rvSections.setVisibility(View.VISIBLE);
        }
    }

    private void bindFeed(GenreFeedDto feed) {
        if (feed == null) return;
        Genre g = feed.getGenre();
        if (g != null) {
            if (g.getName() != null && !g.getName().isEmpty()) {
                b.collapsingToolbar.setTitle(g.getName());
            }
            applyHeader(g);
        }
        sections.clear();
        if (feed.getSections() != null) sections.addAll(feed.getSections());
        adapter.notifyDataSetChanged();
    }

    /**
     * Header lấy màu nền từ {@code coverColor}; nếu có ảnh bìa thì load ảnh và
     * dùng Palette tinh chỉnh lại màu scrim. CollapsingToolbar tự đổi màu status
     * bar khi collapse qua {@code statusBarScrimColor}.
     */
    private void applyHeader(Genre g) {
        int color = resolveColor(g.getCoverColor());
        b.ivCover.setBackgroundColor(color);
        b.collapsingToolbar.setContentScrimColor(color);
        b.collapsingToolbar.setStatusBarScrimColor(color);

        String coverUrl = g.getCoverUrl();
        if (coverUrl == null || coverUrl.isEmpty()) return;

        String url = RetrofitClient.BASE_MEDIA_URL + coverUrl;
        Glide.with(this)
            .load(url)
            .placeholder(R.drawable.placeholder_gradient)
            .centerCrop()
            .into(b.ivCover);

        Glide.with(this).asBitmap().load(url).into(new CustomTarget<Bitmap>() {
            @Override public void onResourceReady(@NonNull Bitmap resource,
                                                  @Nullable Transition<? super Bitmap> t) {
                applyPalette(resource);
            }
            @Override public void onLoadCleared(@Nullable Drawable placeholder) { }
        });
    }

    private void applyPalette(Bitmap bitmap) {
        Palette.from(bitmap).generate(palette -> {
            if (palette == null) return;
            int c = palette.getDarkVibrantColor(palette.getDarkMutedColor(0));
            if (c == 0) return;
            b.collapsingToolbar.setContentScrimColor(c);
            b.collapsingToolbar.setStatusBarScrimColor(c);
        });
    }

    private int resolveColor(String hex) {
        if (hex != null) {
            try { return Color.parseColor(hex); } catch (Exception ignore) {}
        }
        return getColor(R.color.bg_secondary);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) { finish(); return true; }
        return super.onOptionsItemSelected(item);
    }
}
