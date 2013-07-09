package com.ivcam.demo;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import android.content.Context;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.os.Environment;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class CameraPreview extends SurfaceView implements
		SurfaceHolder.Callback {
	private SurfaceHolder mHolder;
	private Camera mCamera;
	private int mId;
	private PreviewCallback mPreviewCallback = new PreviewCallback() {
		public void onPreviewFrame(byte[] data, Camera arg1) {
			Log.d(MainActivity.TAG, "onPreviewFrame");
			try {
				Camera.Parameters parameters = mCamera.getParameters();
				Size size = parameters.getPreviewSize();
				YuvImage image = new YuvImage(data,
						parameters.getPreviewFormat(), size.width, size.height,
						null);
				File file = new File(Environment.getExternalStorageDirectory()
						.getPath() + "/out.jpg");
				FileOutputStream filecon = new FileOutputStream(file);
				image.compressToJpeg(
						new Rect(0, 0, image.getWidth(), image.getHeight()),
						90, filecon);
			} catch (FileNotFoundException e) {
				Log.d(MainActivity.TAG, "Error in onPreviewFrame id " + mId
						+ ", " + e.getMessage());
			}
			CameraPreview.this.invalidate();

		}
	};

	public CameraPreview(Context context, Camera camera, int id) {
		super(context);
		mCamera = camera;
		mId = id;

		// Install a SurfaceHolder.Callback so we get notified when the
		// underlying surface is created and destroyed.
		mHolder = getHolder();
		mHolder.addCallback(this);
		// deprecated setting, but required on Android versions prior to 3.0
		mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
	}

	public void surfaceCreated(SurfaceHolder holder) {
		Log.i(MainActivity.TAG, "surfaceCreated id " + mId);
		// The Surface has been created, now tell the camera where to draw the
		// preview.
		try {
			mCamera.setPreviewDisplay(holder);
			mCamera.setPreviewCallback(mPreviewCallback);
			mCamera.startPreview();
		} catch (IOException e) {
			Log.d(MainActivity.TAG, "Error setting camera preview id " + mId
					+ ", " + e.getMessage());
		}
	}

	public void surfaceDestroyed(SurfaceHolder holder) {
		Log.i(MainActivity.TAG, "surfaceDestroyed id " + mId);
		// empty. Take care of releasing the Camera preview in your activity.
		if (mCamera != null) {
			mCamera.stopPreview();
			mCamera.release();
			mCamera = null;
		}
	}

	public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
		Log.i(MainActivity.TAG, "surfaceChanged id " + mId);
		// If your preview can change or rotate, take care of those events here.
		// Make sure to stop the preview before resizing or reformatting it.

		if (mHolder.getSurface() == null) {
			// preview surface does not exist
			return;
		}

		// stop preview before making changes
		try {
			mCamera.stopPreview();
		} catch (Exception e) {
			// ignore: tried to stop a non-existent preview
		}

		// set preview size and make any resize, rotate or
		// reformatting changes here

		// start preview with new settings
		try {
			mCamera.setPreviewDisplay(mHolder);
			mCamera.setPreviewCallback(mPreviewCallback);
			mCamera.startPreview();
		} catch (Exception e) {
			Log.d(MainActivity.TAG, "Error starting camera preview id " + mId
					+ ", " + e.getMessage());
		}
	}
}
