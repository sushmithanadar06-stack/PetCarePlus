package com.example.petcareplus.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.petcareplus.R;

public class OnboardingAdapter extends RecyclerView.Adapter<OnboardingAdapter.VH> {

    private final int[] images = {
            R.drawable.onboarding_health,
            R.drawable.onboarding_grooming,
            R.drawable.onboarding_rescue
    };

    private final String[] titles = {
            "All your pet's health in one place",
            "Care routines made simple",
            "Rescue and adopt with community support"
    };

    private final String[] descriptions = {
            "Manage vaccinations, daily walks, and nutrition in a sanctuary designed for your furry best friend.",
            "Track grooming, feeding, and daily care so nothing ever slips through the cracks.",
            "Report strays, browse adoptable pets, and connect with a community that cares as much as you do."
    };

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_onboarding_page, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        holder.imgOnboarding.setImageResource(images[position]);
        holder.txtTitle.setText(titles[position]);
        holder.txtDescription.setText(descriptions[position]);
    }

    @Override
    public int getItemCount() {
        return titles.length;
    }

    public static class VH extends RecyclerView.ViewHolder {

        ImageView imgOnboarding;
        TextView txtTitle, txtDescription;

        public VH(@NonNull View itemView) {
            super(itemView);
            imgOnboarding = itemView.findViewById(R.id.imgOnboarding);
            txtTitle = itemView.findViewById(R.id.txtTitle);
            txtDescription = itemView.findViewById(R.id.txtDescription);
        }
    }
}