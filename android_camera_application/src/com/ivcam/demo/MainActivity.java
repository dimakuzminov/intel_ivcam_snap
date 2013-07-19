package com.ivcam.demo;

import android.app.Activity;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

public class MainActivity extends Activity implements OnClickListener {
	public static final String TAG = "ivcam.demo.main";
	private Camera mCamera = null;
	private Button mCam0;
	private Button mCam1;
	private Button mCam2;
	private Button mCam3;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		mCam0 = (Button) findViewById(R.id.cam0_button);
		mCam1 = (Button) findViewById(R.id.cam1_button);
		mCam2 = (Button) findViewById(R.id.cam2_button);
		mCam3 = (Button) findViewById(R.id.cam3_button);
		mCam0.setOnClickListener(this);
		mCam1.setOnClickListener(this);
		mCam2.setOnClickListener(this);
		mCam3.setOnClickListener(this);
		getCamerasInfo();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public void onClick(View arg0) {
		switch (arg0.getId()) {
		case R.id.cam0_button:
			getCamerasPreview(0);
			break;
		case R.id.cam1_button:
			getCamerasPreview(1);
			break;
		case R.id.cam2_button:
			getCamerasPreview(2);
			break;
		case R.id.cam3_button:
			getCamerasPreview(3);
			break;
		}
	}

	private void getCamerasInfo() {
		int cameraCount = 0;
		Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
		TextView number = (TextView) this.findViewById(R.id.number);
		TextView info = (TextView) this.findViewById(R.id.info);
		cameraCount = Camera.getNumberOfCameras();
		number.setText("Number of cameras:" + cameraCount);
		Log.i(TAG, "Number of cameras:" + cameraCount);
		StringBuffer buffer = new StringBuffer();
		for (int camIdx = 0; camIdx < cameraCount; camIdx++) {
//			if(camIdx != 2){
				Camera mcam = Camera.open(camIdx);
				Camera.Parameters cameraParams = mcam.getParameters();
				String str = cameraParams.flatten();
				Log.i(TAG, "info " + str + "\n");
				mcam.release();
//			}
			Camera.getCameraInfo(camIdx, cameraInfo);
			String facing = cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT ? "front"
					: cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK ? "back"
							: "no facing";
			buffer.append("Camera " + camIdx + " facing " + facing
					+ ", orientation " + cameraInfo.orientation + "\n");
			Log.i(TAG, "Camera " + camIdx + " facing " + facing
					+ ", orientation " + cameraInfo.orientation);
		}
		info.setText(buffer.toString());
	}

	@SuppressWarnings("deprecation")
	private void getCamerasPreview(int id) {
	    SurfaceView camView = new SurfaceView(this);
	    SurfaceHolder camHolder = camView.getHolder();
	    CameraPreview preview = new CameraPreview(id);
	    camHolder.addCallback(preview);
		FrameLayout layout = (FrameLayout) findViewById(R.id.camera_preview);
		layout.addView(camView);
	}


}
