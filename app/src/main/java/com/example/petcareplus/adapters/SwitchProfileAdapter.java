package com.example.petcareplus.adapters;

import android.graphics.Bitmap;
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
import com.example.petcareplus.utils.ImageUtils;

import java.util.ArrayList;

public class SwitchProfileAdapter extends RecyclerView.Adapter<SwitchProfileAdapter.SwitchViewHolder> {

    public interface OnPetSelectedListener {
        void onPetSelected(Pet pet);
    }

    private final ArrayList<Pet> petList;
    private final OnPetSelectedListener listener;

    public SwitchProfileAdapter(ArrayList<Pet> petList, OnPetSelectedListener listener) {
        this.petList = petList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public SwitchViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_switch_pet, parent, false);
        return new SwitchViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SwitchViewHolder holder, int position) {
        Pet pet = petList.get(position);
        holder.txtSwitchPetName.setText(pet.getName());
        holder.txtSwitchPetDetails.setText(pet.getType() + " • " + pet.getBreed());

        String photoUri = pet.getPhotoUri();

        if (photoUri != null && !photoUri.trim().isEmpty()) {
            try {
                if (ImageUtils.isBase64Image(photoUri)) {

                    Bitmap bitmap = ImageUtils.base64ToBitmap(photoUri);

                    if (bitmap != null) {
                        holder.imgSwitchPetPhoto.setImageBitmap(bitmap);
                        holder.imgSwitchPetPhoto.setScaleType(ImageView.ScaleType.CENTER_CROP);
                        holder.imgSwitchPetPhoto.setBackground(null);
                        holder.imgSwitchPetPhoto.setPadding(0, 0, 0, 0);
                    } else {
                        setPlaceholder(holder.imgSwitchPetPhoto, pet);
                    }

                } else {
                    holder.imgSwitchPetPhoto.setImageURI(Uri.parse(photoUri));
                    holder.imgSwitchPetPhoto.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    holder.imgSwitchPetPhoto.setBackground(null);
                    holder.imgSwitchPetPhoto.setPadding(0, 0, 0, 0);
                }
            } catch (Exception e) {
                setPlaceholder(holder.imgSwitchPetPhoto, pet);
            }
        } else {
            setPlaceholder(holder.imgSwitchPetPhoto, pet);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onPetSelected(pet);
            }
        });
    }

    private void setPlaceholder(ImageView imgView, Pet pet) {
        int iconRes = pet.getType() != null && pet.getType().equalsIgnoreCase("Dog")
                ? R.drawable.ic_species_dog
                : R.drawable.ic_species_cat;
        imgView.setImageResource(iconRes);
        imgView.setBackgroundResource(R.drawable.bg_avatar_placeholder);
        imgView.setPadding(12, 12, 12, 12);
        imgView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
    }

    @Override
    public int getItemCount() {
        return petList.size();
    }

    public static class SwitchViewHolder extends RecyclerView.ViewHolder {
        ImageView imgSwitchPetPhoto;
        TextView txtSwitchPetName, txtSwitchPetDetails;

        public SwitchViewHolder(@NonNull View itemView) {
            super(itemView);
            imgSwitchPetPhoto = itemView.findViewById(R.id.imgSwitchPetPhoto);
            txtSwitchPetName = itemView.findViewById(R.id.txtSwitchPetName);
            txtSwitchPetDetails = itemView.findViewById(R.id.txtSwitchPetDetails);
        }
    }
}