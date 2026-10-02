package com.example.petcareplus.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.petcareplus.R;
import com.example.petcareplus.models.Pet;
import com.example.petcareplus.utils.TodayScoreHelper;
import com.example.petcareplus.utils.WellnessScoreCalculator;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class HealthScorePagerAdapter extends RecyclerView.Adapter<HealthScorePagerAdapter.ViewHolder> {

    public interface OnCardClickListener {
        void onCardClick(Pet pet);
    }

    private final List<Pet> petList;
    private final FirebaseFirestore db;
    private final OnCardClickListener listener;

    public HealthScorePagerAdapter(List<Pet> petList, FirebaseFirestore db, OnCardClickListener listener) {
        this.petList = petList;
        this.db = db;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_health_score_card, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        Pet pet = petList.get(position);

        holder.txtHealthTitle.setText(pet.getName() + "'s Health Score");
        holder.txtHealthSubtitle.setText("Loading...");
        holder.txtHealthScoreNumber.setText("--");
        holder.healthProgressRing.setProgress(0);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onCardClick(pet);
        });

        TodayScoreHelper.computeTodayScore(db, pet, result -> {

            // Guard: this ViewHolder may have been recycled for a
            // different page by the time this async call returns.
            if (holder.getBindingAdapterPosition() != position) return;

            if (!result.hasAnyData) {
                holder.txtHealthTitle.setText(pet.getName() + "'s Health Score");
                holder.txtHealthSubtitle.setText("No data logged yet");
                holder.txtHealthScoreNumber.setText("--");
                holder.healthProgressRing.setProgress(0);
            } else {
                holder.txtHealthTitle.setText(pet.getName() + "'s Health Score — " + result.score);
                holder.txtHealthSubtitle.setText(pet.getAge() + " old");
                holder.txtHealthScoreNumber.setText(String.valueOf(result.score));
                holder.healthProgressRing.setProgress(result.score);
            }
        });
    }

    @Override
    public int getItemCount() {
        return petList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtHealthTitle, txtHealthSubtitle, txtHealthScoreNumber;
        CircularProgressIndicator healthProgressRing;

        ViewHolder(View itemView) {
            super(itemView);
            txtHealthTitle = itemView.findViewById(R.id.txtHealthTitle);
            txtHealthSubtitle = itemView.findViewById(R.id.txtHealthSubtitle);
            txtHealthScoreNumber = itemView.findViewById(R.id.txtHealthScoreNumber);
            healthProgressRing = itemView.findViewById(R.id.healthProgressRing);
        }
    }
}