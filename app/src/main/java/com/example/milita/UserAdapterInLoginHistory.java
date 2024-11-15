package com.example.milita;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class UserAdapterInLoginHistory extends RecyclerView.Adapter<UserAdapterInLoginHistory.UserViewHolder> {
    private Context context;
    private List<User> userList;

    public UserAdapterInLoginHistory(Context context, List<User> userList) {
        this.context = context;
        this.userList = userList != null ? userList : new ArrayList<>();

    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.user_item_in_login_history, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = userList.get(position);
        holder.tvUserName.setText(user.getName());
        holder.tvRole.setText(user.getRole());
        holder.tvStatus.setText(user.getStatus());

        if (user.getProfileImage() != null) {
            holder.imgAvt.setImageBitmap(user.getProfileImage());
        } else {
            holder.imgAvt.setImageResource(R.drawable.avatar_error);
        }

    }

    @Override
    public int getItemCount() {
        return userList != null ? userList.size() : 0;
    }


    public static class UserViewHolder extends RecyclerView.ViewHolder {
        ImageView imgAvt;
        TextView tvUserName, tvRole, tvStatus, tvSeeMore;
        CheckBox checkBox;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            imgAvt = itemView.findViewById(R.id.imgAvt);
            tvUserName = itemView.findViewById(R.id.tvUserName);
            tvRole = itemView.findViewById(R.id.tvRole);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvSeeMore = itemView.findViewById(R.id.tvSeeMore);
        }
    }
}
