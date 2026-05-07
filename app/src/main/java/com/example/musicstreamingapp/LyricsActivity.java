package com.example.musicstreamingapp;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.musicstreamingapp.databinding.ActivityLyricsBinding;

public class LyricsActivity extends AppCompatActivity {

    private ActivityLyricsBinding b;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityLyricsBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        String title = getIntent().getStringExtra("title");
        String artist = getIntent().getStringExtra("artist");
        String lyrics = getIntent().getStringExtra("lyrics");

        b.tvLyricsTitle.setText(title != null ? title : "");
        b.tvLyricsArtist.setText(artist != null ? artist : "");
        b.tvLyricsFull.setText(lyrics != null ? lyrics : "");

        b.btnLyricsBack.setOnClickListener(v -> finish());
    }
}
