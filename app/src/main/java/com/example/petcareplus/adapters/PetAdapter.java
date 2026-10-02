package com.example.petcareplus.adapters;

import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.petcareplus.R;
import com.example.petcareplus.activities.PetMenuActivity;
import com.example.petcareplus.models.Pet;

import java.util.ArrayList;

public class PetAdapter extends RecyclerView.Adapter<PetAdapter.PetViewHolder> {

    private final ArrayList<Pet> petList;

    public PetAdapter(ArrayList<Pet> petList) {
        this.petList = petList;
    }

    @NonNull
    @Override
    public PetViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_pet, parent, false);

        return new PetViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PetViewHolder holder, int position) {

        Pet pet = petList.get(position);

        holder.txtPetName.setText(pet.getName());
        holder.txtPetDetails.setText(pet.getType() + " • " + pet.getBreed());
        holder.txtPetGender.setText(pet.getGender());
        holder.txtPetAge.setText("Age: " + pet.getAge());

        String photoUri = pet.getPhotoUri();

        if (photoUri != null && !photoUri.trim().isEmpty()) {
            try {
                holder.imgPet.setBackground(null);
                holder.imgPet.setPadding(0, 0, 0, 0);
                holder.imgPet.setScaleType(ImageView.ScaleType.CENTER_CROP);
                holder.imgPet.setImageURI(Uri.parse(photoUri));
            } catch (Exception e) {
                setPlaceholderPhoto(holder.imgPet, pet);
            }
        } else {
            setPlaceholderPhoto(holder.imgPet, pet);
        }

        holder.itemView.setOnClickListener(v -> {

            Intent intent = new Intent(v.getContext(), PetMenuActivity.class);
            intent.putExtra("pet", pet);

            v.getContext().startActivity(intent);
        });
    }

    // =====================================================
    // UPDATED METHOD WITH REPLACEMENT LOGIC
    // =====================================================
    private void setPlaceholderPhoto(ImageView imgPet, Pet pet) {
        int iconRes = (pet.getType() != null && pet.getType().equalsIgnoreCase("Dog")) ? R.drawable.ic_species_dog : R.drawable.ic_species_cat;
        imgPet.setImageResource(iconRes);
        imgPet.setBackgroundResource(R.drawable.bg_avatar_placeholder);
        imgPet.setPadding(16, 16, 16, 16);
        imgPet.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
    }

    @Override
    public int getItemCount() {
        return petList.size();
    }

    public static class PetViewHolder extends RecyclerView.ViewHolder {

        ImageView imgPet;
        TextView txtPetName;
        TextView txtPetDetails;
        TextView txtPetGender;
        TextView txtPetAge;

        public PetViewHolder(@NonNull View itemView) {
            super(itemView);

            imgPet = itemView.findViewById(R.id.imgPet);
            txtPetName = itemView.findViewById(R.id.txtPetName);
            txtPetDetails = itemView.findViewById(R.id.txtPetDetails);
            txtPetGender = itemView.findViewById(R.id.txtPetGender);
            txtPetAge = itemView.findViewById(R.id.txtPetAge);
        }
    }
}
