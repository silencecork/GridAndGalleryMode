package com.example.gridtest;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore.Images;
import android.support.v4.util.LruCache;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;

public class MainActivity extends Activity {

	private static final int MSG_INIT = 0;
	private static final int MSG_UPDATE = 1;
	private GridView mGridView;
	private Gallery mGallery;
	private PhotoAdapter mAdapter;
	private Thread mLoadPhotoThread;
	
	private static final int MODE_GRID = 0x00010000;
	private static final int MODE_GALLERY = 0x00020000;
	private int mMode;
	
	private Cursor mCursor;
	private Handler mHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			if (msg.what == MSG_INIT) {
				if (mAdapter != null) {
					mAdapter.updateCursor(mCursor);
					mAdapter.notifyDataSetChanged();
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
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);
        
        mGridView = (GridView) findViewById(R.id.gridView1);
        mGridView.setHorizontalSpacing(3);
	    mGridView.setVerticalSpacing(3);
        mGallery = (Gallery) findViewById(R.id.gallery);
        mGallery.setUnselectedAlpha(180.f);
        changeMode(MODE_GALLERY);
        
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
    	if (item.getItemId() == R.id.menu_changemode) {
    		changeMode((mMode == MODE_GRID) ? MODE_GALLERY : MODE_GRID);
    	}
		return super.onOptionsItemSelected(item);
	}

    private void changeMode(int mode) {
    	synchronized(MainActivity.this) {
    		mMode = mode;
    	}
		if (mAdapter != null) {
			mAdapter.stop();
		}
		mAdapter = new PhotoAdapter(mode);
		if (mCursor != null) {
	    	mAdapter.updateCursor(mCursor);
	    }
		if (mode == MODE_GRID) {
			mGallery.setVisibility(View.GONE);
			int pos = mGallery.getSelectedItemPosition();
			mGridView.setVisibility(View.VISIBLE);
		    mGridView.setAdapter(mAdapter);
		    mGridView.setSelection(pos);
		} else if (mode == MODE_GALLERY) {
			mGridView.setVisibility(View.GONE);
			int pos = mGridView.getFirstVisiblePosition();
			mGallery.setVisibility(View.VISIBLE);
			mGallery.setAdapter(mAdapter);
			mGallery.setSelection(pos);
		}
	}
    
    private synchronized int getMode() {
    	synchronized(MainActivity.this) {
    		return mMode;
    	}
    }

    private LruCache<Integer, Bitmap> mCache = new LruCache<Integer, Bitmap>(1024 * 1024 * 8) {

		@Override
		protected int sizeOf(Integer key, Bitmap value) {
			return (value != null) ? (value.getWidth() * value.getHeight()) : 0;
		}

		@Override
		protected void entryRemoved(boolean evicted, Integer key,
				Bitmap oldValue, Bitmap newValue) {
			synchronized(oldValue) {
				if (oldValue != null) {
					Log.i("LruCache", "recycle " + key);
					oldValue.recycle();
				}
			}
		}
		
		
	};
    
	class PhotoAdapter extends BaseAdapter {
    	
    	private static final int THREAD_COUNT = 2;
    	private static final int GRID_SIZE = 150;
    	private static final int GALLERY_ITEM_WIDTH = 250;
    	private static final int GALLERY_ITEM_HEIGHT = 250;
    	
    	ViewGroup.LayoutParams mGeneralParams;
    	
    	private SparseArray<ImageView> mViewCache;
    	private Bitmap mDefaultBitmap;
    	private DecodeThread[] mDecodeThreads;
    	private SaveThread mSaveThread;
    	private DecodeThread.OnBitmapCallback mListener = new DecodeThread.OnBitmapCallback() {
			
			@Override
			public void onBitampBack(int pos, Bitmap b) {
				if (b != null) {
            		synchronized (mCache) {
            			mCache.put(pos, b);
            		}
            		if (mSaveThread != null && !mCursor.isClosed()) {
            			mSaveThread.addTask(mCursor.getString(mCursor.getColumnIndexOrThrow(Images.ImageColumns.DATA)), 
            					mCursor.getLong(mCursor.getColumnIndexOrThrow(Images.ImageColumns.DATE_TAKEN)), b);
            		}
            		Message.obtain(mHandler, MSG_UPDATE, pos, 0, b).sendToTarget();
            	}
			}
		};
    	
    	public PhotoAdapter(int mode) {
    		mGeneralParams = (mode == MODE_GRID) ? new AbsListView.LayoutParams(GRID_SIZE, GRID_SIZE) : 
    			new android.widget.Gallery.LayoutParams(GALLERY_ITEM_WIDTH, GALLERY_ITEM_HEIGHT)  ;
    		
    		mViewCache = new SparseArray<ImageView>();
    		mDefaultBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.photo_load);
    		initDecodeThread();
    		
    		mSaveThread = new SaveThread();
    		mSaveThread.start();
    	}
    	
    	private void initDecodeThread() {
    		mDecodeThreads = new DecodeThread[THREAD_COUNT];
    		for (int i = 0; i < THREAD_COUNT; i++) {
    			mDecodeThreads[i] = new DecodeThread(MainActivity.this, mListener);
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
				imageView = (getMode() == MODE_GRID) ? new SecureImageView(MainActivity.this) : 
					new ImageViewEx(MainActivity.this);
				synchronized (mViewCache) {
					mViewCache.put(pos, imageView);
				}
			}
			
			imageView.setLayoutParams(mGeneralParams);
			imageView.setScaleType(ScaleType.CENTER_CROP);
			
			if (mCursor == null) {
				return imageView;
			}
			
			Bitmap b = null;
			if (mCache != null) {
				synchronized (mCache) {
					b = mCache.get(pos);
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
			if (mCache != null) {
				for (int i = 0; i < mCache.size(); i++) {
					mCache.remove(i);
				}
			}
		}
    	
    }
    
 }
