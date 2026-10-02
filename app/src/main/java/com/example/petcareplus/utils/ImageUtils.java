package com.example.petcareplus.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Base64;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class ImageUtils {

    // Compress + encode: turns a picked photo Uri into a small Base64 string
    // small enough to store directly inside a Firestore document field.
    public static String uriToCompressedBase64(Context context, Uri uri) {

        try {
            InputStream input = context.getContentResolver().openInputStream(uri);
            Bitmap original = BitmapFactory.decodeStream(input);
            if (input != null) input.close();

            if (original == null) return "";

            // Resize down to max 300x300 — plenty for a circular profile photo
            Bitmap resized = resizeBitmap(original, 300);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            resized.compress(Bitmap.CompressFormat.JPEG, 55, outputStream);
            byte[] bytes = outputStream.toByteArray();

            return "data:image/jpeg;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP);

        } catch (Exception e) {
            return "";
        }
    }

    private static Bitmap resizeBitmap(Bitmap source, int maxSize) {

        int width = source.getWidth();
        int height = source.getHeight();

        float ratio = Math.min((float) maxSize / width, (float) maxSize / height);

        if (ratio >= 1) return source;

        int newWidth = Math.round(width * ratio);
        int newHeight = Math.round(height * ratio);

        return Bitmap.createScaledBitmap(source, newWidth, newHeight, true);
    }

    // Decode: turns a stored Base64 string back into a displayable Bitmap
    public static Bitmap base64ToBitmap(String base64String) {

        try {
            String pureBase64 = base64String;

            if (base64String.contains(",")) {
                pureBase64 = base64String.substring(base64String.indexOf(",") + 1);
            }

            byte[] bytes = Base64.decode(pureBase64, Base64.NO_WRAP);
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

        } catch (Exception e) {
            return null;
        }
    }

    // Quick check to tell a Base64 photo string apart from an old local file path
    public static boolean isBase64Image(String photoUri) {
        return photoUri != null && photoUri.startsWith("data:image");
    }
}