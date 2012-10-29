package com.example.gridtest;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore.Images;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;

import com.aphidmobile.flip.FlipViewController;

public class FlipActivity extends Activity {
	private static final int MSG_INIT = 0;
	private static final int MSG_UPDATE = 1;
	private PhotoAdapter mAdapter;
	private FlipViewController mFlipper;
	private Thread mLoadPhotoThread;
	
	private Cursor mCursor;
	private Handler mHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			if (msg.what == MSG_INIT) {
				mAdapter = new PhotoAdapter();
				int pos = getIntent().getIntExtra("pos", 0);
				mAdapter.updateCursor(mCursor);
				mFlipper.setAdapter(mAdapter);
				if (pos >= 0 && mFlipper != null) {
					mFlipper.setSelection(pos);
				}
			} else if (msg.what == MSG_UPDATE) {
				int pos = msg.arg1;
				Bitmap b = (Bitmap) msg.obj;
				if (mAdapter != null) {
					mAdapter.update(pos, b);
				}
			} 
		}
		
	};
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mFlipper = new FlipViewController(this);
        setContentView(mFlipper);
        
        mLoadPhotoThread = new Thread(new Runnable() {

			@Override
			public void run() {
				mCursor = LoadPhotoExecutor.getPhotoCursor(getContentResolver());
				mHandler.sendEmptyMessage(MSG_INIT);
			}
        	
        }); 
        mLoadPhotoThread.start();
    }
    
    
    @Override
	protected void onDestroy() {
		super.onDestroy();
		if (mAdapter != null) {
			mAdapter.clearAndStopAll();
		}
		if (mLoadPhotoThread != null) {
			mLoadPhotoThread.interrupt();
		}
		if (mCursor != null) {
			mCursor.close();
		}
	}

	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }
	
    @Override
	public boolean onOptionsItemSelected(MenuItem item) {
		return super.onOptionsItemSelected(item);
	}
    
    

    // Lazy way... Use Cache in MainActivity...
//	private LruCache<Integer, Bitmap> mCache = new LruCache<Integer, Bitmap>(1024 * 1024 * 8) {
//
//		@Override
//		protected int sizeOf(Integer key, Bitmap value) {
//			return (value != null) ? (value.getWidth() * value.getHeight()) : 0;
//		}
//
//		@Override
//		protected void entryRemoved(boolean evicted, Integer key,
//				Bitmap oldValue, Bitmap newValue) {
//			synchronized(oldValue) {
//				if (oldValue != null) {
//					Log.i("LruCache", "recycle " + key);
//					oldValue.recycle();
//				}
//			}
//		}
//		
//		
//	};
    
	@Override
	public void onBackPressed() {
		Intent intent = new Intent();
		intent.putExtra("pos", mFlipper.getSelectedItemPosition());
		setResult(RESULT_OK, intent);
		super.onBackPressed();
	}



	class PhotoAdapter extends BaseAdapter {
    	
    	private static final int THREAD_COUNT = 2;
    	
    	ViewGroup.LayoutParams mGeneralParams;
    	
    	private SparseArray<ImageView> mViewCache;
    	private Bitmap mDefaultBitmap;
    	private DecodeThread[] mDecodeThreads;
    	private SaveThread mSaveThread;
    	private DecodeThread.OnBitmapCallback mListener = new DecodeThread.OnBitmapCallback() {
			
			@Override
			public void onBitampBack(int pos, Bitmap b) {
				if (b != null) {
            		synchronized (MainActivity.sBitmapCache) {
            			MainActivity.sBitmapCache.put(pos, b);
            		}
            		if (mSaveThread != null && !mCursor.isClosed()) {
            			mSaveThread.addTask(mCursor.getString(mCursor.getColumnIndexOrThrow(Images.ImageColumns.DATA)), 
            					mCursor.getLong(mCursor.getColumnIndexOrThrow(Images.ImageColumns.DATE_TAKEN)), b);
            		}
            		Message.obtain(mHandler, MSG_UPDATE, pos, 0, b).sendToTarget();
            	}
			}
		};
    	
    	public PhotoAdapter() {
    		mGeneralParams = new AbsListView.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT);
    		
    		mViewCache = new SparseArray<ImageView>();
    		mDefaultBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.photo_load);
    		initDecodeThread();
    		
    		mSaveThread = new SaveThread();
    		mSaveThread.start();
    	}
    	
    	private void initDecodeThread() {
    		mDecodeThreads = new DecodeThread[THREAD_COUNT];
    		for (int i = 0; i < THREAD_COUNT; i++) {
    			mDecodeThreads[i] = new DecodeThread(FlipActivity.this, mListener);
        		mDecodeThreads[i].start();
    		}
    	}
    	
    	public void updateCursor(Cursor c) {
    		for (int i = 0; i < THREAD_COUNT; i++) {
        		mDecodeThreads[i].setCursor(c);
    		}
    	}
    	
    	public void update(int pos, Bitmap b) {
    		ImageView imageView;
    		synchronized (mViewCache) {
				imageView = mViewCache.get(pos, null);
			}
    		if (imageView != null) {
    			imageView.setImageBitmap(b);
    			imageView.invalidate();
    		}
    	}

		@Override
		public int getCount() {
			return (mCursor != null) ? mCursor.getCount() : 0;
		}

		@Override
		public Object getItem(int arg0) {
			return (mCursor != null) ? mCursor.moveToPosition(arg0) : null;
		}

		@Override
		public long getItemId(int arg0) {
			return arg0;
		}

		@Override
		public View getView(int pos, View convertView, ViewGroup parent) {
			ImageView imageView;
			synchronized (mViewCache) {
				imageView = mViewCache.get(pos, null);
			}
			
			if (imageView == null) {
				imageView = new ImageView(FlipActivity.this);
				synchronized (mViewCache) {
					mViewCache.put(pos, imageView);
				}
			}
			if (mGeneralParams != null) {
				imageView.setLayoutParams(mGeneralParams);
			}

			imageView.setScaleType(ScaleType.FIT_CENTER);
			
			if (mCursor == null) {
				return imageView;
			}
			
			Bitmap b = null;
			if (MainActivity.sBitmapCache != null) {
				synchronized (MainActivity.sBitmapCache) {
					b = MainActivity.sBitmapCache.get(pos);
				}
			}
			
			if (b == null || b.isRecycled()) {
				mDecodeThreads[pos % THREAD_COUNT].addTask(pos);
	    		b =  mDefaultBitmap;
			}
			
			imageView.setImageBitmap(b);
			
			return imageView;
		}
		
		public void stop() {
			if (mDecodeThreads != null) {
				for (int i = 0; i < THREAD_COUNT; i++) {
					mDecodeThreads[i].stopThread();
				}
			}
			if (mSaveThread != null) {
				mSaveThread.stopThread();
			}
			
			if (mViewCache != null) {
				mViewCache.clear();
			}
		}
		
		
		public void clearAndStopAll() {
			stop();
//			if (MainActivity.sBitmapCache != null) {
//				for (int i = 0; i < MainActivity.sBitmapCache.size(); i++) {
//					MainActivity.sBitmapCache.remove(i);
//				}
//			}
		}
    	
    }
}
