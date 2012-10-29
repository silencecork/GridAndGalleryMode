package com.example.gridtest;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.widget.ImageView;

public class SecureImageView extends ImageView {
	
	private Bitmap mBitmap;
	
	public SecureImageView(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
	}
	
	public SecureImageView(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
	}
	
	public SecureImageView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		// TODO Auto-generated constructor stub
	}
	
	@Override
	public void setImageBitmap(Bitmap bm) {
		super.setImageBitmap(bm);
		mBitmap = bm;
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		if (mBitmap == null || mBitmap.isRecycled()) return;
		super.onDraw(canvas);
	}

}
