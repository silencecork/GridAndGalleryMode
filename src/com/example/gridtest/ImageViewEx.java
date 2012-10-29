package com.example.gridtest;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.widget.ImageView;

class ImageViewEx extends ImageView {
	private static final Paint BMP_PAINT = new Paint();
    	private Bitmap mBmp;
    	
		public ImageViewEx(Context context) {
			super(context);
			init();
		}
    	
		public ImageViewEx(Context context, AttributeSet attrs) {
			super(context, attrs);
			init();
		}
    	
		public ImageViewEx(Context context, AttributeSet attrs, int defStyle) {
			super(context, attrs, defStyle);
			init();
		}
		
		private void init() {
			BMP_PAINT.setAntiAlias(true);
			BMP_PAINT.setFilterBitmap(true);
			BMP_PAINT.setDither(true);
		}

		@Override
		protected void onDraw(Canvas canvas) {
			if (mBmp == null || mBmp.isRecycled()) return;
			//super.onDraw(canvas);
			Bitmap bitmap = mBmp;
			Rect src = mSrcRect;
			Rect dst = mDestRect;
			canvas.drawBitmap(bitmap, src, dst, BMP_PAINT);
		}

		private Rect mSrcRect = new Rect();
		private Rect mDestRect = new Rect();
		
		@Override
		public void setImageBitmap(Bitmap bm) {
			//super.setImageBitmap(bm);
			if (bm == mBmp) return; 
			mBmp = bm;
			
			int width = this.getLayoutParams().width;
			int height = this.getLayoutParams().height;
			
			mSrcRect.left = 0;
			mSrcRect.top = 0;
			mSrcRect.right = bm.getWidth();
			mSrcRect.bottom = bm.getHeight();
			
			mDestRect.left = 0;
			mDestRect.top = 0;
			mDestRect.right = width;
			mDestRect.bottom = height;
		}
		
    }