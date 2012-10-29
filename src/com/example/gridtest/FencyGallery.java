package com.example.gridtest;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Camera;
import android.graphics.Matrix;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Transformation;
import android.widget.Gallery;
import android.widget.ImageView;

public class FencyGallery extends Gallery {

	private static final String TAG = "Gallery";
	public static final int CHOICE_MODE_NONE = 0;
    public static final int CHOICE_MODE_SINGLE = 1;
    public static final int CHOICE_MODE_MULTIPLE = 2;
    private int mMode = CHOICE_MODE_NONE;
    @SuppressWarnings("unused")
	private static final int VELOCITY_MAX = 1500;
	
    private Camera mCamera = new Camera();
    
    private static final int MAX_DEGREES = 60;
    
    private boolean mIsRotatable;
    private boolean mIsZoomable;
    private int mDegrees = MAX_DEGREES;
    private float mZoomRatio;
    
    private int mCenter;
    private boolean mEnableAlpha;
    
	public interface OnDispatchEventListener {
		public boolean onKeyEventDispatch(KeyEvent ev);
	}
	
	private static final boolean VERBOSE = false;
	private OnDispatchEventListener mListener;

	public FencyGallery(Context context) {
	    super(context);
	    init(context, null, 0);
	}

	public FencyGallery(Context context, AttributeSet attrs) {
	    super(context, attrs);
	    init(context, attrs, 0);
	}

	public FencyGallery(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(context, attrs, defStyle);
	}
	
	private void init(Context context, AttributeSet attrs, int defStyle) {
	    TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.Gallery, defStyle, 0);
        int N = a.getIndexCount();
        for (int i = 0; i < N; i++) {
            int attr = a.getIndex(i);
            switch (attr) {
                case R.styleable.Gallery_center_zoomable:
                    setZoomable(a.getBoolean(attr, false));
                    break;
                case R.styleable.Gallery_center_zoom_ratio:
                    setZoomRatio(a.getFloat(attr, 0));
                    break;
                case R.styleable.Gallery_rotatable:
                    setRotatable(a.getBoolean(attr, false));
                    break;
                case R.styleable.Gallery_rotate_degrees:
                    setRotateDegree(a.getInt(attr, 0));
                    break;
                    
                case R.styleable.Gallery_enable_alpha:
                	enableAlpha(a.getBoolean(attr, true));
                	break;
            }
        }
        a.recycle();
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
	
	public void enableAlpha(boolean alpha) {
		mEnableAlpha = alpha;
	}

	@Override
	public boolean dispatchKeyEvent(KeyEvent event) {
		if (VERBOSE) Log.v(TAG, "dispatchKeyEvent");
		StringBuilder b = new StringBuilder();
		b.append("downtime: ");
		b.append(event.getDownTime());
		b.append(" eventtime: ");
		b.append(event.getEventTime());
		b.append(" action: ");
		b.append(event.getAction());
		b.append(" code: ");
		b.append(event.getKeyCode());
		b.append(" repeat: ");
		b.append(event.getRepeatCount());
		b.append(" metastate: ");
		b.append(event.getMetaState());
		b.append(" deviceId: ");
		b.append(event.getDeviceId());
		b.append(" scancode: ");
		b.append(event.getScanCode());
		b.append(" flag: ");
		b.append(event.getFlags());
//		b.append(" source: ");
//		b.append(event.getSource());
		b.append(" character: ");
		b.append(event.getCharacters());
		android.util.Log.d(TAG, b.toString());
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
		int keyCode = (velocityX > 0) ? KeyEvent.KEYCODE_DPAD_LEFT : KeyEvent.KEYCODE_DPAD_RIGHT;
		super.onKeyDown(keyCode, null);
	    return true;
	}
	
	@SuppressWarnings("unused")
	private int distanceToPrevious() {
        if (getCount() > 0 && computeHorizontalScrollOffset() > 0) {
            return scrollToChild(computeHorizontalScrollOffset() - getFirstVisiblePosition() - 1) + 100;
        } else {
            return 0;
        }
    }

	@SuppressWarnings("unused")
	private int distanceToNext() {
        if (getCount() > 0 && computeHorizontalScrollOffset() < getCount() - 1) {
            return scrollToChild(computeHorizontalScrollOffset() - getFirstVisiblePosition() + 1) - 100;
        } else {
            return 0;
        }
    }
	
	private int scrollToChild(int childPosition) {
        View child = getChildAt(childPosition);
        View currentView = getChildAt(computeHorizontalScrollOffset() - getFirstVisiblePosition());
        
        if (child != null) {
        	int childW = currentView.getWidth();
        	int childH = currentView.getHeight();
        	int baseLength = getWidth() > getHeight() ? getWidth() : getHeight();
            int distance = getCenterOfGallery() - getCenterOfView(child);
			int gap = (childW > childH) ? baseLength : baseLength / 2;
			if (distance > 0) {
				distance = (distance < gap) ? gap : distance;
			} else if (distance < 0) {
				distance = (distance > -gap) ? -gap : distance;
			}
            return distance;
        }
        
        return 0;
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
    
    private void setTransformAlpha(Transformation t, float alpha) {
    	if (t != null && mEnableAlpha) {
    		t.setAlpha(alpha);
    	}
    }

    @Override
    protected boolean getChildStaticTransformation(View child, Transformation t) {
        final int childCenter = getCenterOfView(child);
        final int childWidth = child.getWidth();
        int rotationAngle = 0;
        t.clear();
        setTransformAlpha(t, 0.5f);
        t.setTransformationType(Transformation.TYPE_MATRIX);
        if (childCenter == mCenter) {
            transformImageBitmap((ImageView) child, t, 0, 0.f, 1.f);
        } else {
            rotationAngle = (int) (((float) (mCenter - childCenter) / childWidth) * mDegrees);
            float absRotate = Math.abs(rotationAngle);
            if (absRotate > mDegrees) {
                rotationAngle = (rotationAngle < 0) ? -mDegrees : mDegrees;
            }
            float percent = absRotate / (float)mDegrees;
            float zoomRatio = mZoomRatio * percent;
            float alpha = 1.0f - (1.0f * percent);
            alpha = (alpha < 0.5f) ? 0.5f : alpha;
            transformImageBitmap((ImageView) child, t, rotationAngle, zoomRatio, alpha);
        }
    	
        return true;
    }
	
    private int getCenterOfView(View view) {
        return (view != null) ? view.getLeft() + view.getWidth() / 2 : 0;
    } 
    
    private int getCenterOfGallery() {
        return (getWidth() - getPaddingLeft() - getPaddingRight()) / 2 + getPaddingLeft();
    }
    
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        mCenter = getCenterOfGallery();
        super.onSizeChanged(w, h, oldw, oldh);
    }
    
    private void transformImageBitmap(ImageView child, Transformation t, int rotationAngle, float zoomRatio, float alpha) {
    	setTransformAlpha(t, alpha);
    	
    	mCamera.save();
        final Matrix imageMatrix = t.getMatrix();
        final int imageHeight = child.getLayoutParams().height;
        final int imageWidth = child.getLayoutParams().width;
        
        // As the angle of the view gets less, zoom in
        if (mIsZoomable && zoomRatio > 0.f) {
            mCamera.translate(0.0f, 0.0f, zoomRatio);
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
