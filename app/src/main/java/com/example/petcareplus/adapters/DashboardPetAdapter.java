package com.example.petcareplus.adapters;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.petcareplus.R;
import com.example.petcareplus.models.Pet;

import java.util.List;

public class DashboardPetAdapter extends RecyclerView.Adapter<DashboardPetAdapter.ViewHolder> {

    private List<Pet> pets;
    private OnPetClickListener listener;

    public interface OnPetClickListener {
        void onPetClick(Pet pet);
    }

    public DashboardPetAdapter(List<Pet> pets, OnPetClickListener listener) {
        this.pets = pets;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_dashboard_pet, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Pet pet = pets.get(position);
        holder.txtPetName.setText(pet.getName());

        String photoUri = pet.getPhotoUri();
        if (photoUri != null && !photoUri.isEmpty()) {
            try {
                holder.imgPet.setImageURI(Uri.parse(photoUri));
                holder.imgPet.setBackground(null);
                holder.imgPet.setPadding(0, 0, 0, 0);
                holder.imgPet.setScaleType(ImageView.ScaleType.CENTER_CROP);
            } catch (Exception e) {
                setPlaceholder(holder.imgPet, pet);
            }
        } else {
            setPlaceholder(holder.imgPet, pet);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onPetClick(pet);
        });
    }

    private void setPlaceholder(ImageView img, Pet pet) {
        String type = pet.getType();
        int icon = (type != null && type.equalsIgnoreCase("Dog")) ? R.drawable.ic_species_dog : R.drawable.ic_species_cat;
        img.setImageResource(icon);
        img.setBackgroundResource(R.drawable.bg_avatar_placeholder);
        img.setPadding(16, 16, 16, 16);
        img.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
    }

    @Override
    public int getItemCount() {
        return pets.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgPet;
        TextView txtPetName;

        ViewHolder(View itemView) {
            super(itemView);
            imgPet = itemView.findViewById(R.id.imgPet);
            txtPetName = itemView.findViewById(R.id.txtPetName);
        }
    }
}