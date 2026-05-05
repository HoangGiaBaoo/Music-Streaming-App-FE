package com.example.musicstreamingapp.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.musicstreamingapp.MainActivity;
import com.example.musicstreamingapp.R;
import com.example.musicstreamingapp.util.AccountStore;

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class AccountAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int TYPE_ACCOUNT = 0;
    private static final int TYPE_ADD = 1;

    public interface Listener {
        void onSwitchAccount(AccountStore.Account a);
        void onAddAccount();
    }

    private final List<AccountStore.Account> accounts = new ArrayList<>();
    private String currentUsername;
    private final Listener listener;

    public AccountAdapter(List<AccountStore.Account> initial, Listener listener) {
        this.accounts.addAll(initial);
        this.listener = listener;
    }

    public void setData(List<AccountStore.Account> data, String currentUsername) {
        accounts.clear();
        accounts.addAll(data);
        this.currentUsername = currentUsername;
        notifyDataSetChanged();
    }

    @Override public int getItemViewType(int position) {
        return position == accounts.size() ? TYPE_ADD : TYPE_ACCOUNT;
    }

    @Override public int getItemCount() {
        return accounts.size() + 1;
    }

    @NonNull @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layout = (viewType == TYPE_ADD)
            ? R.layout.item_drawer_add_account
            : R.layout.item_drawer_account;
        View v = LayoutInflater.from(parent.getContext()).inflate(layout, parent, false);
        return new RecyclerView.ViewHolder(v) {};
    }

    @Override public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (getItemViewType(position) == TYPE_ADD) {
            holder.itemView.setOnClickListener(v -> listener.onAddAccount());
            return;
        }
        AccountStore.Account a = accounts.get(position);
        CircleImageView avatar = holder.itemView.findViewById(R.id.iv_account_avatar);
        TextView name = holder.itemView.findViewById(R.id.tv_account_name);
        name.setText(a.shortName());
        if (a.avatarUrl != null && !a.avatarUrl.isEmpty()) {
            Glide.with(holder.itemView)
                .load(MainActivity.buildMediaUrl(a.avatarUrl))
                .placeholder(R.drawable.ic_avatar_placeholder)
                .into(avatar);
        } else {
            avatar.setImageResource(R.drawable.ic_avatar_placeholder);
        }
        holder.itemView.setAlpha(a.username.equals(currentUsername) ? 1f : 0.6f);
        holder.itemView.setOnClickListener(v -> listener.onSwitchAccount(a));
    }
}
