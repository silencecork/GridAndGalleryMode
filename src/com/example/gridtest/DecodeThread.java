package com.example.gridtest;

import java.util.LinkedList;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;

public class DecodeThread extends Thread {
	private static int COUNT = 0;
	public interface OnBitmapCallback {
		public void onBitampBack(int pos, Bitmap b);
	}

	private OnBitmapCallback mCallback;

	private LinkedList<Integer> mTaskList;
	private boolean mDone;
	private LoadPhotoExecutor mExecutor;
	private Context mContext;
	private Cursor mCursor;

	public DecodeThread(Context context, OnBitmapCallback callback) {
		super("DecodeThread " + COUNT++);
		mContext = context;
		mCallback = callback;
		mTaskList = new LinkedList<Integer>();
		mExecutor = new LoadPhotoExecutor(mContext.getContentResolver());
	}
	
	public void setCursor(Cursor c) {
		mCursor = c;
	}

	public void addTask(Integer k) {
		System.out.println("add task " + k);
		synchronized (mTaskList) {
			mTaskList.addFirst(k);
		}
		synchronized (DecodeThread.this) {
			DecodeThread.this.notify();
		}
	}

	public void stopThread() {
		mDone = true;
		mTaskList.clear();
		synchronized (DecodeThread.this) {
			DecodeThread.this.notify();
		}
	}

	public void run() {
		android.os.Process
				.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
		while (!mDone) {
			synchronized (DecodeThread.this) {
				DecodeThread.this.notify();
				if (mTaskList == null || mTaskList.size() == 0) {
					try {
						DecodeThread.this.wait();
					} catch (InterruptedException ex) {
						continue;
					}
					continue;
				}
			}
			Integer k;
			synchronized (mTaskList) {
				k = mTaskList.remove(0);
			}
			if (mCursor != null && !mCursor.isClosed()
					&& mCursor.moveToPosition(k) && mExecutor != null) {
				Bitmap b = mExecutor.getBitmap(mCursor);
				if (mCallback != null) {
					mCallback.onBitampBack(k, b);
				}

			}
		}
	}

}