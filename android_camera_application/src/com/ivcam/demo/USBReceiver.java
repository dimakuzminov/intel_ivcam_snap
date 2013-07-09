package com.ivcam.demo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.widget.Toast;

public class USBReceiver extends BroadcastReceiver {
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();
		Toast.makeText(context, "USBReceiver: " + action, Toast.LENGTH_SHORT).show();
		UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
		Toast.makeText(context, "Device: " + device.getDeviceName(), Toast.LENGTH_SHORT).show();
	}
}
