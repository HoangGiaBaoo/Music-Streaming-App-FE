package com.example.musicstreamingapp.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.example.musicstreamingapp.R;
import com.example.musicstreamingapp.model.Artist;
import com.example.musicstreamingapp.network.RetrofitClient;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class ArtistCardAdapter extends RecyclerView.Adapter<ArtistCardAdapter.VH> {
    private final List<Artist> artists;
    private final OnArtistClickListener listener;

    public interface OnArtistClickListener {
        void onArtistClick(Artist artist);
    }

    public ArtistCardAdapter(List<Artist> artists, OnArtistClickListener listener) {
        this.artists = artists;
        this.listener = listener;
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_artist_card, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        Artist a = artists.get(pos);
        h.name.setText(a.getName());
        Context ctx = h.itemView.getContext();
        if (a.getAvatarUrl() != null) {
            Glide.with(ctx)
                .load(RetrofitClient.BASE_MEDIA_URL + a.getAvatarUrl())
                .placeholder(R.drawable.ic_person)
                .error(R.drawable.ic_person)
                .transform(new CircleCrop())
                .into(h.avatar);
        } else {
            h.avatar.setImageResource(R.drawable.ic_person);
        }
        h.itemView.setOnClickListener(v -> listener.onArtistClick(a));
    }

    @Override public int getItemCount() { return artists.size(); }

    static class VH extends RecyclerView.ViewHolder {
        CircleImageView avatar;
        TextView name;
        VH(View v) {
            super(v);
            avatar = v.findViewById(R.id.iv_avatar);
            name = v.findViewById(R.id.tv_name);
        }
    }
}
