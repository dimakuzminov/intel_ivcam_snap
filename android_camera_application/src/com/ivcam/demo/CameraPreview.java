package com.ivcam.demo;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.view.SurfaceHolder;

public class CameraPreview implements SurfaceHolder.Callback,
		Camera.PreviewCallback {
	public static final String TAG = "ivcam.demo.preview";
    SurfaceHolder mSurfHolder;
    Camera mCamera;
    int mCamId;
    
    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        Parameters p = camera.getParameters();  
        int width = p.getPreviewSize().width;
        int height = p.getPreviewSize().height;

        ByteArrayOutputStream outstr = new ByteArrayOutputStream();
        Rect rect = new Rect(0, 0, width, height);
        YuvImage yuvimage = new YuvImage(data, ImageFormat.NV21, width,
                height, null);
        yuvimage.compressToJpeg(rect, 80, outstr); // outstr contains image in jpeg  
        //String encodedImage = Base64.encodeToString(
        //        outstr.toByteArray(), Base64.DEFAULT); // this is base64 encoding of image


    }

	public Camera.Size getBestPreviewSize(Camera.Parameters parameters) {
		Camera.Size result = null;
		int width = 0;
		int height = 0;
		for (Camera.Size size : parameters.getSupportedPreviewSizes()) {
			if (size.width > width && size.height > height) {
				width = size.width;
				height = size.height;
				result = size;
			}
		}
		return (result);
	}
    
    public CameraPreview(int camId) {
        mCamId = camId;
    }

	
    @Override
    public void surfaceChanged(SurfaceHolder arg0, int arg1, int arg2, int arg3) {
        Parameters parameters;
        mSurfHolder = arg0;

        parameters = mCamera.getParameters();
        Camera.Size size = getBestPreviewSize(parameters);
        parameters.setPreviewSize(size.width, size.height);

        mCamera.setParameters(parameters);
        mCamera.startPreview();
    }

    public void surfaceCreated(SurfaceHolder arg0) {
        mCamera = Camera.open(mCamId);
        try {
            // If did not set the SurfaceHolder, the preview area will be black.
            mCamera.setPreviewDisplay(arg0);
            mCamera.setPreviewCallback(this);
            Parameters parameters = mCamera.getParameters();
            Camera.Size size = getBestPreviewSize(parameters);
            parameters.setPreviewSize(size.width, size.height);
            mCamera.setParameters(parameters);
        } catch (IOException e) {
            mCamera.release();
            mCamera = null;
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder arg0) {
        mCamera.setPreviewCallback(null);
        mCamera.stopPreview();
        mCamera.release();
        mCamera = null;
    }
    

}
