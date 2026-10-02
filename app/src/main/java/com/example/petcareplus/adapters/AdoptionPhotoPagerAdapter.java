package com.example.petcareplus.adapters;

import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.petcareplus.R;
import com.example.petcareplus.utils.ImageUtils;

import java.util.List;

public class AdoptionPhotoPagerAdapter extends RecyclerView.Adapter<AdoptionPhotoPagerAdapter.PhotoViewHolder> {

    private final List<String> photoBase64List;

    public AdoptionPhotoPagerAdapter(List<String> photoBase64List) {
        this.photoBase64List = photoBase64List;
    }

    @NonNull
    @Override
    public PhotoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_adoption_photo_page, parent, false);
        return new PhotoViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull PhotoViewHolder holder, int position) {

        String base64 = photoBase64List.get(position);

        if (base64 != null && ImageUtils.isBase64Image(base64)) {
            Bitmap bitmap = ImageUtils.base64ToBitmap(base64);
            if (bitmap != null) {
                holder.imageView.setImageBitmap(bitmap);
                return;
            }
        }

        holder.imageView.setImageDrawable(null);
    }

    @Override
    public int getItemCount() {
        return photoBase64List.size();
    }

    static class PhotoViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        PhotoViewHolder(View itemView) {
            super(itemView);
            imageView = (ImageView) itemView;
        }
    }
}