package com.example.gridtest;

import android.content.Context;
import android.graphics.Camera;
import android.graphics.Matrix;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Transformation;
import android.widget.ImageView;

public class Gallery extends android.widget.Gallery {

	private static final String TAG = "Gallery";
	public static final int CHOICE_MODE_NONE = 0;
    public static final int CHOICE_MODE_SINGLE = 1;
    public static final int CHOICE_MODE_MULTIPLE = 2;
    private int mMode = CHOICE_MODE_NONE;
    private static final int VELOCITY_MAX = 2000;
	
    private Camera mCamera = new Camera();
    
    private static final int MAX_DEGREES = 60;
    @SuppressWarnings("unused")
    private static final int MAX_ZOOM_STEPS = -100;
    private static final float ZOOM_DECREASE_RATIO = 1.5f;
    
    private boolean mIsRotatable = true;
    private boolean mIsZoomable = false;
    private int mDegrees = MAX_DEGREES;
    private float mZoomRatio;
    
    private int mCenter;
    
	public interface OnDispatchEventListener {
		public boolean onKeyEventDispatch(KeyEvent ev);
	}
	
	private static final boolean VERBOSE = false;
	private OnDispatchEventListener mListener;
	

	public Gallery(Context context) {
	    super(context);
	    init(context, null, 0);
	}

	public Gallery(Context context, AttributeSet attrs) {
	    super(context, attrs);
	    init(context, attrs, 0);
	}

	public Gallery(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(context, attrs, defStyle);
	}
	
	private void init(Context context, AttributeSet attrs, int defStyle) {

	}
	
	public void setRotatable(boolean isRotate) {
	    mIsRotatable = isRotate;
	}
	
	public void setZoomable(boolean isZoom) {
	    mIsZoomable = isZoom;
	}
	
	public void setRotateDegree(int degrees) {
	    mDegrees = (degrees > MAX_DEGREES) ? MAX_DEGREES : degrees;
	}
	
	public void setZoomRatio(float zoom) {
	    mZoomRatio = zoom;
	}
	 
	public void setOnDispatchEventListener(OnDispatchEventListener l) {
		mListener = l;
	}

	@Override
	public boolean dispatchKeyEvent(KeyEvent event) {
		if (VERBOSE) Log.v(TAG, "dispatchKeyEvent");
		// Gallery will eat all key events, I need that events
		// So I took events back
		return (mListener != null) ? mListener.onKeyEventDispatch(event) : super.dispatchKeyEvent(event);
	}

	@Override
	protected void dispatchSetPressed(boolean pressed) {
		if (VERBOSE) Log.v(TAG, "dispatchSetPressed " + pressed);
		
//		setPressed(disabled);
//		super.dispatchSetPressed(pressed);
	}

	@Override
	public void dispatchSetSelected(boolean selected) {
		if (VERBOSE) Log.v(TAG, "dispatchSetSelected " + selected);
		View v = this.getSelectedView();
//		super.dispatchSetSelected(selected);
		if (v != null)
			v.setSelected(selected);
	}
	

	@Override
	public boolean onDown(MotionEvent e) {
		boolean result = super.onDown(e);
		boolean disabled = false;
		setFocusable(disabled);
		setFocusableInTouchMode(disabled);
		setSelected(disabled);
		return result;
	}
	
	// distanceX > 0 to right
	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
		return super.onScroll(e1, e2, distanceX, distanceY);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		return super.onTouchEvent(event);
	}
	
	// velocityX < 0 to right
	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
//		return super.onFling(e1, e2, (velocityX > 0) ? 800 : -800, 0);
	    Log.v(TAG, "velocityX " + velocityX);
	    if (velocityX > 0) {
	        velocityX = (velocityX > VELOCITY_MAX) ? VELOCITY_MAX : velocityX;
	    } else {
	        velocityX = (velocityX < -VELOCITY_MAX) ? -VELOCITY_MAX : velocityX;
	    }
	    return super.onFling(e1, e2, velocityX, velocityY);
	}

	@Override
	public void detachAllViewsFromParent() {
		super.detachAllViewsFromParent();
	}
	
	public int getChoiceMode() {
        return mMode;
    }
    
    public void setChoiceMode(int mode) {
        mMode = mode;
    }
    
    public void stopFling() {
        MotionEvent fake = MotionEvent.obtain(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        this.onScroll(fake, fake, -30, 0);
        
//        int pos = this.getSelectedItemPosition();
//        setSelection(pos);
    }

    @Override
    protected boolean getChildStaticTransformation(View child, Transformation t) {
        final int childCenter = getCenterOfView(child);
        final int childWidth = child.getWidth();
        int rotationAngle = 0;
        t.clear();
        t.setTransformationType(Transformation.TYPE_MATRIX);
        if (childCenter == mCenter) {
            transformImageBitmap((ImageView) child, t, 0);
        } else {
            rotationAngle = (int) (((float) (mCenter - childCenter) / childWidth) * mDegrees);
            if (Math.abs(rotationAngle) > mDegrees) {
                rotationAngle = (rotationAngle < 0) ? -mDegrees : mDegrees;
            }
            transformImageBitmap((ImageView) child, t, rotationAngle);
        }

        return true;
    }
	
    private int getCenterOfView(View view) {
        return view.getLeft() + view.getWidth() / 2;
    } 
    
    private int getCenterOfGallery() {
        return (getWidth() - getPaddingLeft() - getPaddingRight()) / 2 + getPaddingLeft();
    }
    
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        mCenter = getCenterOfGallery();
        super.onSizeChanged(w, h, oldw, oldh);
    }
    
    private void transformImageBitmap(ImageView child, Transformation t, int rotationAngle) {
        mCamera.save();
        final Matrix imageMatrix = t.getMatrix();
        final int imageHeight = child.getLayoutParams().height;
        final int imageWidth = child.getLayoutParams().width;
        final int rotation = Math.abs(rotationAngle);

//        mCamera.translate(0.0f, 0.0f, 100.0f);

        // As the angle of the view gets less, zoom in
        if (rotation < MAX_DEGREES && mIsZoomable) {
//            float zoomAmount = (float) (MAX_ZOOM_STEPS + (rotation * ZOOM_DECREASE_RATIO));
            float zoomAmount = (float) (mZoomRatio + (rotation * ZOOM_DECREASE_RATIO));
            mCamera.translate(0.0f, 0.0f, zoomAmount);
        }
        
        // TODO if want to make gallery like Cover flow
        // Open this. But the performance is not good, unless use hardware drawing
        if (mIsRotatable) {
            mCamera.rotateY(rotationAngle);
        }
        mCamera.getMatrix(imageMatrix);
        imageMatrix.preTranslate(-(imageWidth / 2), -(imageHeight / 2));
        imageMatrix.postTranslate((imageWidth / 2), (imageHeight / 2));
        mCamera.restore();
    }
}
