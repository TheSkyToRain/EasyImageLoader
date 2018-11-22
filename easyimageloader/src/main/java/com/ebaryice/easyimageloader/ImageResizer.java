package com.ebaryice.easyimageloader;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.FileDescriptor;

public class ImageResizer {
    private static final String TAG = "ImageResizer";

    public ImageResizer(){

    }

    public static Bitmap decodeSampledBitmapFromResource(Resources res, int resId,
                                                  int reqWidth, int reqHeight){
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(res,resId,options);

        // 计算采样率
        options.inSampleSize = calculateInSampleSize(options,reqWidth,reqHeight);
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeResource(res,resId,options);
    }

    public static Bitmap decodeSampledBitmapFromFileDescriptor(FileDescriptor fd,
                                                        int reqWidth, int reqHeight){
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFileDescriptor(fd, null, options);

        // 计算采样率
        options.inSampleSize = calculateInSampleSize(options,reqWidth,reqHeight);
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFileDescriptor(fd, null, options);
    }

    /**
     * 计算采样率
     * @param options BitmapFactory.Options
     * @param reqWidth 要求宽度
     * @param reqHeight 要求高度
     * @return 采样率
     */
    private static int calculateInSampleSize(BitmapFactory.Options options,
                                             int reqWidth, int reqHeight) {
        if (reqHeight == 0 || reqWidth == 0){
            return 1;
        }

        final int width = options.outWidth;
        final int height = options.outHeight;

        int inSampleSize = 1;

        if (width > reqWidth || height > reqHeight){
            final int halfWidth = width / 2;
            final int halfHeight = height / 2;
            while ((halfWidth / inSampleSize) >= reqWidth && (halfHeight / inSampleSize) >= reqHeight){
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }
}
