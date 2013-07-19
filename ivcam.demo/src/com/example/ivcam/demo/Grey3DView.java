package com.example.ivcam.demo;

import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;

public class Grey3DView extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_grey3d_view);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.grey3_dview, menu);
		return true;
	}

}
