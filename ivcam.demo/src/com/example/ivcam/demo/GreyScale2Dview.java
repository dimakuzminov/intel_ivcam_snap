package com.example.ivcam.demo;

import java.io.IOException;
import java.io.InputStream;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.ImageView;

public class GreyScale2Dview extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_grey_scale2_dview);
		InputStream stream = getResources().openRawResource(R.raw.hand0_0000);
		byte[] rawData = new byte[614400];
		try {
			stream.read(rawData, 0, 614400);
		} catch (IOException e) {
			e.printStackTrace();
		}
		int[] colors = new int[640*480];
		int j = 0;
		int i = 0;
		for ( ; i < rawData.length; ) {
			colors[j++] = rawData[i] + (rawData[i+1] << 8 );
			i += 2;
		}
		Bitmap bmpGrayscale = Bitmap.createBitmap(colors, 640, 480, Bitmap.Config.RGB_565);
		ImageView image = (ImageView) findViewById(R.id.gray_scale_2d_image);
		image.setImageBitmap(bmpGrayscale);
	}

}
