package com.example.petcareplus.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.util.Base64;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Turns picked / captured photos into compact base64 JPEG strings (max ~1280px, ~250 KB)
 * so they can be stored in Firestore, and decodes them back for display.
 */
public final class RescueImageUtils {

    private static final int MAX_DIM = 1280;
    private static final int MAX_BYTES = 250_000;

    private RescueImageUtils() {
    }

    /** Reads an image Uri, fixes its rotation, shrinks it and returns a base64 JPEG. */
    public static String uriToBase64(Context ctx, Uri uri) throws IOException {
        ContentResolver cr = ctx.getContentResolver();

        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        try (InputStream in = cr.openInputStream(uri)) {
            if (in == null) throw new IOException("Cannot open image");
            BitmapFactory.decodeStream(in, null, bounds);
        }

        int sample = 1;
        while (Math.max(bounds.outWidth, bounds.outHeight) / sample > MAX_DIM * 2) {
            sample *= 2;
        }

        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inSampleSize = sample;

        Bitmap bmp;
        try (InputStream in = cr.openInputStream(uri)) {
            if (in == null) throw new IOException("Cannot open image");
            bmp = BitmapFactory.decodeStream(in, null, opts);
        }
        if (bmp == null) throw new IOException("Cannot decode image");

        int rotation = readRotation(cr, uri);
        float scale = Math.min(1f, (float) MAX_DIM / Math.max(bmp.getWidth(), bmp.getHeight()));

        if (scale < 1f || rotation != 0) {
            Matrix m = new Matrix();
            m.postScale(scale, scale);
            if (rotation != 0) m.postRotate(rotation);
            Bitmap out = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), m, true);
            if (out != bmp) bmp.recycle();
            bmp = out;
        }

        return bitmapToBase64(bmp);
    }

    private static int readRotation(ContentResolver cr, Uri uri) {
        try (InputStream in = cr.openInputStream(uri)) {
            if (in == null) return 0;
            ExifInterface exif = new ExifInterface(in);
            int o = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL);
            switch (o) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    return 90;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    return 180;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    return 270;
                default:
                    return 0;
            }
        } catch (Exception e) {
            return 0;
        }
    }

    /** Compresses until the JPEG is under MAX_BYTES (lowering quality, then size). */
    public static String bitmapToBase64(Bitmap bmp) {
        int quality = 80;
        byte[] data;

        while (true) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            bmp.compress(Bitmap.CompressFormat.JPEG, quality, out);
            data = out.toByteArray();

            if (data.length <= MAX_BYTES) break;

            if (quality > 45) {
                quality -= 10;
            } else if (bmp.getWidth() > 400) {
                bmp = Bitmap.createScaledBitmap(bmp,
                        (int) (bmp.getWidth() * 0.8f), (int) (bmp.getHeight() * 0.8f), true);
            } else {
                break;
            }
        }
        return Base64.encodeToString(data, Base64.NO_WRAP);
    }

    /** Tiny JPEG (used for the list-screen thumbnail stored on the main document). */
    public static String bitmapToSmallBase64(Bitmap bmp) {
        if (bmp == null) return "";
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.JPEG, 60, out);
        return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP);
    }

    public static Bitmap base64ToBitmap(String s) {
        byte[] bytes = decode(s);
        if (bytes == null) return null;
        try {
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        } catch (Exception e) {
            return null;
        }
    }

    /** Decodes a downsampled copy (at most about maxPx on the long side). */
    public static Bitmap base64ToThumb(String s, int maxPx) {
        byte[] bytes = decode(s);
        if (bytes == null) return null;
        try {
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(bytes, 0, bytes.length, o);

            int sample = 1;
            while (Math.max(o.outWidth, o.outHeight) / (sample * 2) >= maxPx) {
                sample *= 2;
            }

            BitmapFactory.Options o2 = new BitmapFactory.Options();
            o2.inSampleSize = sample;
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length, o2);
        } catch (Exception e) {
            return null;
        }
    }

    private static byte[] decode(String s) {
        if (s == null || s.isEmpty()) return null;
        try {
            if (s.startsWith("data:")) {
                int comma = s.indexOf(',');
                if (comma >= 0) s = s.substring(comma + 1);
            }
            return Base64.decode(s, Base64.DEFAULT);
        } catch (Exception e) {
            return null;
        }
    }
}