package com.example.easymusic.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.Bitmap.Config;
import android.graphics.PorterDuff.Mode;
import android.util.Log;

public class BitmapUtil {
	private static final String TAG = "BitmapUtil";
	
	/** * 将图片剪裁为圆形 */
	public static Bitmap createCircleImage(Bitmap source, int diameter) {
		Bitmap scropBitmap = null;
		//int length = source.getWidth() < source.getHeight() ? source.getWidth() : source.getHeight();
		//int dia = length > diameter ? diameter : length;
		//Matrix matrix = new Matrix();  
		//matrix.postScale(((float)dia)/source.getWidth(), ((float)dia)/source.getHeight());  
		Paint paint = new Paint();
		paint.setAntiAlias(true);
		Bitmap target = Bitmap.createBitmap(diameter, diameter, Config.ARGB_8888);
		Canvas canvas = new Canvas(target);
		canvas.drawCircle(diameter / 2, diameter / 2, diameter / 2, paint);
		paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
		scropBitmap = Bitmap.createScaledBitmap(source, diameter, diameter, false);
		//scropBitmap = Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
		canvas.drawBitmap(scropBitmap, 0, 0, paint);
		return target;
	}
	
	public static Bitmap getScropBitmap(String pathName, int reqWidth, int reqHeight) {
		Bitmap src = null;
		Options options = new Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(pathName, options);
		options.inSampleSize = computeInSampleSize(options, reqWidth, reqHeight);
		options.inJustDecodeBounds = false;
		src = BitmapFactory.decodeFile(pathName, options);
		Bitmap outPut = Bitmap.createScaledBitmap(src, reqWidth, reqHeight, false);
		if (src != outPut) {
			src.recycle();
		}
		return outPut;
	}
	
	private static int computeInSampleSize(Options options, int reqWidth,int reqHeight) {
		int inSampleSize = 1;
		int optWidth = options.outWidth;
		int optHeight = options.outHeight;
		if (optWidth > reqWidth || optHeight > reqHeight) {
			while (optWidth/reqWidth >= 2 && optHeight/reqHeight >= 2) {
				inSampleSize *= 2;
				optWidth /= 2;
				optHeight /= 2;
			}
		}
		Log.d(TAG, "inSampleSize = " + inSampleSize);
		return inSampleSize;
	}

	public Bitmap getCroppedRoundBitmap(Bitmap bmp, int radius) {  
        Bitmap scaledSrcBmp;  
        int diameter = radius * 2;  
        // 为了防止宽高不相等，造成圆形图片变形，因此截取长方形中处于中间位置最大的正方形图片  
        int bmpWidth = bmp.getWidth();  
        int bmpHeight = bmp.getHeight();  
        int squareWidth = 0, squareHeight = 0;  
        int x = 0, y = 0;  
        Bitmap squareBitmap;  
        if (bmpHeight > bmpWidth) {// 高大于宽  
            squareWidth = squareHeight = bmpWidth;  
            x = 0;  
            y = (bmpHeight - bmpWidth) / 2;  
            // 截取正方形图片  
            squareBitmap = Bitmap.createBitmap(bmp, x, y, squareWidth, squareHeight);  
        } else if (bmpHeight < bmpWidth) {// 宽大于高  
            squareWidth = squareHeight = bmpHeight;  
            x = (bmpWidth - bmpHeight) / 2;  
            y = 0;  
            squareBitmap = Bitmap.createBitmap(bmp, x, y, squareWidth,squareHeight);  
        } else {  
            squareBitmap = bmp;  
        }  
        if (squareBitmap.getWidth() != diameter || squareBitmap.getHeight() != diameter) {  
            scaledSrcBmp = Bitmap.createScaledBitmap(squareBitmap, diameter,diameter, true);  
        } else {  
            scaledSrcBmp = squareBitmap;  
        }  
        Bitmap output = Bitmap.createBitmap(scaledSrcBmp.getWidth(),  
                scaledSrcBmp.getHeight(),   
                Config.ARGB_8888);  
        Canvas canvas = new Canvas(output);  
  
        Paint paint = new Paint();  
        Rect rect = new Rect(0, 0, scaledSrcBmp.getWidth(),scaledSrcBmp.getHeight());  
  
        paint.setAntiAlias(true);  
        paint.setFilterBitmap(true);  
        paint.setDither(true);  
        canvas.drawARGB(0, 0, 0, 0);  
        canvas.drawCircle(scaledSrcBmp.getWidth() / 2,  
                scaledSrcBmp.getHeight() / 2,   
                scaledSrcBmp.getWidth() / 2,  
                paint);  
        paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));  
        canvas.drawBitmap(scaledSrcBmp, rect, rect, paint);  
        bmp = null;  
        squareBitmap = null;  
        scaledSrcBmp = null;  
        return output;  
    }

}
