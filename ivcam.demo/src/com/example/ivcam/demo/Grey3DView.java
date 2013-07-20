package com.example.ivcam.demo;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

import android.app.Activity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class Grey3DView extends Activity implements OnClickListener {
	private Grey3DOpenGLView mGLSurfaceView;
	private Grey3DOpenGlRender mRenderer;
	private Button mPreviewButton;
	private final static int mImageWdith=640;
	private final static int mImageHeight=480;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_grey3d_view);
		mGLSurfaceView = (Grey3DOpenGLView) findViewById(R.id.gl_surface_view);
		mGLSurfaceView.setEGLContextClientVersion(2);
		final DisplayMetrics displayMetrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
		float[] calibData = new float[Calibration.nParamters()];
		readCalibration(calibData);
		int[] depth = new int[mImageWdith*mImageHeight];
		readDepthData(depth);
		mRenderer = new Grey3DOpenGlRender(calibData, depth, mImageWdith, mImageHeight);
		mGLSurfaceView.setRenderer(mRenderer, displayMetrics.density);
		mPreviewButton = (Button) findViewById(R.id.preview_button);
		mPreviewButton.setOnClickListener(this);
		}

	private void readCalibration(float[] calibData) {
		InputStream stream = getResources().openRawResource(R.raw.calibration_params);
		DataInputStream dataStream = new DataInputStream(stream);
		for (int i = 0; i<Calibration.nParamters();i++) {
			try {
				calibData[i] = (float)dataStream.readFloat();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private void readDepthData(int[] depth) {
		InputStream stream = getResources().openRawResource(R.raw.hand0_0000);
		int dataSize = mImageWdith*mImageHeight*2;
		byte[] rawData = new byte[dataSize];
		try {
			stream.read(rawData, 0, dataSize);
		} catch (IOException e) {
			e.printStackTrace();
		}
		int j = 0;
		int i = 0;
		for (; i < dataSize;) {
			depth[j++] = rawData[i] + (rawData[i + 1] << 8);
			i += 2;
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.grey3_dview, menu);

		return true;
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.preview_button:
			finish();
			break;
		}		
	}

}
