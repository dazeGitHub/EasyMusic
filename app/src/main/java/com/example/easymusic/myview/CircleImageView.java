package com.example.easymusic.myview;

import com.example.easymusic.util.BitmapUtil;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.ImageView;

public class CircleImageView extends ImageView {
	private int width, height;

	public CircleImageView(Context context) {
		super(context);
	}

	public CircleImageView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		Drawable drawable = getDrawable();
		Bitmap bitmap = null;
		if (drawable != null) {
			bitmap = ((BitmapDrawable) drawable).getBitmap();
			Bitmap b = BitmapUtil.createCircleImage(bitmap, height);
			/*Paint paint = new Paint();
			paint.setAntiAlias(true);
			paint.setColor(Color.MAGENTA);
			canvas.drawCircle(width / 2, height / 2, height / 2 + 3, paint);*/
			canvas.drawBitmap(b, width / 2 - height / 2, 0, null);
			
		} else {
			super.onDraw(canvas);
		}
		

	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		width = w;
		height = h;
	}

}
