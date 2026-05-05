package com.example.musicstreamingapp;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class LyricsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lyrics);

        String title = getIntent().getStringExtra("title");
        String artist = getIntent().getStringExtra("artist");
        String lyrics = getIntent().getStringExtra("lyrics");

        ((TextView) findViewById(R.id.tv_lyrics_title)).setText(title != null ? title : "");
        ((TextView) findViewById(R.id.tv_lyrics_artist)).setText(artist != null ? artist : "");
        ((TextView) findViewById(R.id.tv_lyrics_full)).setText(lyrics != null ? lyrics : "");

        findViewById(R.id.btn_lyrics_back).setOnClickListener(v -> finish());
    }
}
