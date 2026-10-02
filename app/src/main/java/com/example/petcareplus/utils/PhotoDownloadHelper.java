package com.example.petcareplus.utils;

import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import java.io.OutputStream;

public class PhotoDownloadHelper {

    /**
     * Saves a bitmap into the device's public Pictures/PetCarePlus folder
     * using MediaStore, so it appears in the gallery. Uses scoped storage
     * on Android 10+, which needs no runtime permission.
     */
    public static boolean saveToGallery(Context context, Bitmap bitmap, String displayName) {

        if (bitmap == null) return false;

        try {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, displayName + ".jpg");
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.put(MediaStore.Images.Media.RELATIVE_PATH,
                        Environment.DIRECTORY_PICTURES + "/PetCarePlus");
                values.put(MediaStore.Images.Media.IS_PENDING, 1);
            }

            Uri uri = context.getContentResolver()
                    .insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

            if (uri == null) return false;

            OutputStream out = context.getContentResolver().openOutputStream(uri);
            if (out == null) return false;

            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            out.flush();
            out.close();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear();
                values.put(MediaStore.Images.Media.IS_PENDING, 0);
                context.getContentResolver().update(uri, values, null, null);
            }

            return true;

        } catch (Exception e) {
            return false;
        }
    }
}