package com.example.easymusic.util;

import java.lang.ref.SoftReference;
import java.util.Map;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ImageView;

public class LoadImageTask extends AsyncTask<String, Void, Bitmap> {
	private static final String TAG = "LoadImageTask";
	
	private int width;
	private int height;
	private ImageView view;
	private Map<String, SoftReference<Bitmap>> ImageCacheMap = null;
	private String key;
	
	public LoadImageTask(ImageView view, int width, int height) {
		this.view = view;
		this.width = width;
		this.height = height;
	}
	
	public LoadImageTask(ImageView view, int width, int height, 
			String key, Map<String, SoftReference<Bitmap>> ImageCacheMap) {
		this.view = view;
		this.width = width;
		this.height = height;
		this.key = key;
		this.ImageCacheMap = ImageCacheMap;
	}
	
	@Override
	protected void onPreExecute() {
		super.onPreExecute();
	}

	@Override
	protected Bitmap doInBackground(String... params) {
		String filePath = params[0];
		Bitmap bitmap = BitmapUtil.getScropBitmap(filePath, width, height);
		//Bitmap bitmap = BitmapFactory.decodeFile(filePath);
		Log.d(TAG, "bitmap.getByteCount() = " + bitmap.getByteCount());//10000bytes vs 1000000bytes
		return bitmap;
	}

	@Override
	protected void onPostExecute(Bitmap result) {
		super.onPostExecute(result);
		if (ImageCacheMap != null && key != null) {
			Log.d(TAG, "add bitmap to ImageCacheMap");
			ImageCacheMap.put(key, new SoftReference<Bitmap>(result));
		}
		view.setImageBitmap(result);
	}

	@Override
	protected void onProgressUpdate(Void... values) {
		// TODO Auto-generated method stub
		super.onProgressUpdate(values);
	}

}
