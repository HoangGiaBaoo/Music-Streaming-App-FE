package com.example.musicstreamingapp;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;

import com.example.musicstreamingapp.adapter.SelectableArtistAdapter;
import com.example.musicstreamingapp.databinding.ActivityAddArtistBinding;
import com.example.musicstreamingapp.viewmodel.AddArtistViewModel;
import com.example.musicstreamingapp.viewmodel.VmFactory;

import java.util.ArrayList;
import java.util.HashSet;

public class AddArtistActivity extends AppCompatActivity {

    private ActivityAddArtistBinding b;
    private AddArtistViewModel vm;
    private SelectableArtistAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityAddArtistBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        adapter = new SelectableArtistAdapter(
            new ArrayList<>(), new HashSet<>(), artist -> vm.toggleArtist(artist), this);
        b.rvArtists.setLayoutManager(new GridLayoutManager(this, 3));
        b.rvArtists.setAdapter(adapter);

        vm = new ViewModelProvider(this, new VmFactory(this)).get(AddArtistViewModel.class);

        vm.loading().observe(this, loading ->
            b.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE));

        vm.filtered().observe(this, artists -> adapter.setItems(artists));

        vm.selectedIds().observe(this, ids -> adapter.setSelectedIds(ids));

        vm.doneEvent().observe(this, e -> e.consume(ok -> finish()));

        b.etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int bef, int c) {
                vm.filter(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        b.btnClose.setOnClickListener(v -> finish());
        b.btnDone.setOnClickListener(v -> vm.save());
    }
}
