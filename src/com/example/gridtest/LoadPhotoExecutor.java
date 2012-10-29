package com.example.gridtest;

import java.io.File;
import java.io.IOException;

import android.content.ContentResolver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ExifInterface;
import android.os.Environment;
import android.provider.MediaStore.Images;
import android.text.TextUtils;

public class LoadPhotoExecutor {

	private ContentResolver mResolver;
	private BitmapFactory.Options mOptions;

	public LoadPhotoExecutor(ContentResolver resolver) {
		mResolver = resolver;
		mOptions = new BitmapFactory.Options();
	}

	public static Cursor getPhotoCursor(ContentResolver resolver) {
		if (resolver == null) {
			return null;
		}

		Cursor c = resolver.query(Images.Media.EXTERNAL_CONTENT_URI, null,
				null, null, Images.ImageColumns.DATE_TAKEN + " ASC");
		if (c != null) {
			c.moveToFirst();
		}
		return c;
	}

	private Bitmap getBitmapById(long id) {
		if (mResolver == null || id < 0) {
			return null;
		}
		mOptions.inJustDecodeBounds = true;
		Images.Thumbnails.getThumbnail(mResolver, id,
				Images.Thumbnails.MICRO_KIND, mOptions);
		mOptions.inJustDecodeBounds = false;
		if (mOptions.outWidth <= 0 || mOptions.outHeight <= 0) {
			return null;
		}

		return Images.Thumbnails.getThumbnail(mResolver, id,
				Images.Thumbnails.MICRO_KIND, mOptions);
	}

	private Bitmap getBitmapFromCache(String path, long date) {
		int hashName = (path + date).hashCode();
		StringBuilder str = new StringBuilder(SaveThread.CACHE_DIR);
		String cachePath = str.append(hashName).toString();
		File f = new File(cachePath);
		if (!f.exists()) {
			return null;
		}
		return BitmapFactory.decodeFile(cachePath);
	}

	private Bitmap getExifBitmapByPath(String path) {
		if (TextUtils.isEmpty(path)) {
			return null;
		}
		try {
			ExifInterface exif = new ExifInterface(path);
			if (exif.hasThumbnail()) {
				byte[] exifThumbBytes = exif.getThumbnail();
				return BitmapFactory.decodeByteArray(exifThumbBytes, 0,
						exifThumbBytes.length);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		return null;
	}

	private Bitmap getBitmapByPath(String path) {
		if (TextUtils.isEmpty(path)) {
			return null;
		}
		mOptions.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(path, mOptions);
		mOptions.inJustDecodeBounds = false;

		if (mOptions.outWidth <= 0 || mOptions.outHeight <= 0) {
			return null;
		}
		mOptions.inSampleSize = 8;
		Bitmap b = BitmapFactory.decodeFile(path, mOptions);
		return b;
	}

	public synchronized Bitmap getBitmap(Cursor c) {
		if (c == null || c.isClosed() || c.getCount() <= 0) {
			return null;
		}
		long id = c.getLong(c.getColumnIndexOrThrow(Images.ImageColumns._ID));
		String path = c.getString(c
				.getColumnIndexOrThrow(Images.ImageColumns.DATA));
		long date = c.getLong(c
				.getColumnIndexOrThrow(Images.ImageColumns.DATE_TAKEN));
		Bitmap b = getBitmapFromCache(path, date);
		if (b == null) {
			b = getBitmapById(id);
		}
		if (b == null) {
			b = getExifBitmapByPath(path);
		}

		if (b == null && !TextUtils.isEmpty(path)) {
			b = getBitmapByPath(path);
		}

		return b;
	}

}