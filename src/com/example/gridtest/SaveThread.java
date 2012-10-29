package com.example.gridtest;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedList;

import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

class SaveThread extends Thread {

	private LinkedList<SaveTask> mTaskList;
	private boolean mDone;
	private boolean mPause;
	public static final String CACHE_DIR = Environment.getExternalStorageDirectory()
			.getPath() + File.separatorChar + "my_cache" + File.separatorChar;

	class SaveTask {
		String path;
		long date;
		Bitmap bitmap;

		SaveTask(String p, long d, Bitmap b) {
			path = p;
			date = d;
			bitmap = b;
		}
	}

	public SaveThread() {
		super("SaveThread");
		mTaskList = new LinkedList<SaveTask>();
		File f = new File(CACHE_DIR);
		if (!f.exists()) {
			f.mkdirs();
		}
	}

	public void addTask(String path, long dateTaken, Bitmap b) {
		synchronized (mTaskList) {
			mTaskList.addFirst(new SaveTask(path, dateTaken, b));
		}
		synchronized (SaveThread.this) {
			SaveThread.this.notify();
		}
	}

	public void stopThread() {
		mDone = true;
		mTaskList.clear();
		synchronized (SaveThread.this) {
			SaveThread.this.notify();
		}
	}
	
	public void pauseThread() {
		mPause = true;
		synchronized (SaveThread.this) {
			SaveThread.this.notify();
		}
	}
	
	public void resumeThread() {
		mPause = false;
		synchronized (SaveThread.this) {
			SaveThread.this.notify();
		}
	}

	public void run() {
		while (!mDone) {
			synchronized (SaveThread.this) {
				SaveThread.this.notify();
				if (mTaskList == null || mTaskList.size() == 0 || mPause) {
					try {
						SaveThread.this.wait();
					} catch (InterruptedException ex) {
						continue;
					}
					continue;
				}
			}
			SaveTask k;
			synchronized (mTaskList) {
				k = mTaskList.remove(0);
			}
			if (k != null && k.bitmap != null && !k.bitmap.isRecycled()) {
				int hashName = (k.path + k.date).hashCode();
				StringBuilder str = new StringBuilder(CACHE_DIR);
				storeBitmapToFile(str.append(hashName).toString(), k.bitmap);
			}
		}
	}

	private boolean storeBitmapToFile(String filePath, Bitmap b) {
		if (b == null || TextUtils.isEmpty(filePath)) {
			return false;
		}
		File file = new File(filePath);
		if (file.exists())
			return false;
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(file);
			synchronized (b) {
				return (b != null && !b.isRecycled()) ? b.compress(
						CompressFormat.JPEG, 85, fos) : false;
			}
		} catch (Exception ex) {
			Log.e("SaveThread", "compress error", ex);
			return false;
		} finally {
			if (fos != null) {
				try {
					fos.close();
				} catch (IOException e) {

				}
				fos = null;
			}
		}
	}

}