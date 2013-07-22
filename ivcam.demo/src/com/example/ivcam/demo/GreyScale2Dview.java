package com.example.ivcam.demo;

import java.io.IOException;
import java.io.InputStream;



import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.hardware.SensorManager;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.hardware.usb.UsbRequest;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.app.PendingIntent;
import android.os.Handler;

public class GreyScale2Dview extends Activity implements OnClickListener {
	private Button mCaptureButton;
    private UsbManager mManager;
    private UsbDevice mDevice = null;
    private UsbDeviceConnection mDeviceConnection = null;
    private UsbInterface mInterface = null;
    private UsbEndpoint mEndpointIn = null;
    private static final String TAG = "ivcam.demo";
    private final WaiterThread mWaiterThread = new WaiterThread();
    private PendingIntent mPermissionIntent;
    private Handler mHandler = new Handler();
    public byte[] frame = new byte[4*640*480];
    private final int TIMEOUT = 500;
    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        mManager = (UsbManager)getSystemService(Context.USB_SERVICE);
        // check for existing devices
        mPermissionIntent = PendingIntent.getBroadcast((Context)this, 0, new Intent(ACTION_USB_PERMISSION), 0);
        // listen for new devices
        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        filter.addAction(ACTION_USB_PERMISSION);
        registerReceiver(mUsbReceiver, filter);
       for (UsbDevice device :  mManager.getDeviceList().values()) {
        	if(device.getVendorId() != 0x04b4 || device.getProductId() != 0x00f1){
        		continue;
        	}
            mManager.requestPermission(device, mPermissionIntent);
        	setupDevice(device);
        	break;
        }
		setContentView(R.layout.activity_grey_scale2_dview);
		mCaptureButton = (Button) findViewById(R.id.capture_button);
		mCaptureButton.setOnClickListener(this);
    	mWaiterThread.run();
		//set2DImageView();
	}

    @Override
    public void onDestroy() {
        unregisterReceiver(mUsbReceiver);
        setupDevice(null);
        super.onDestroy();
    }

    // Sets the current USB device and interface
    private boolean setupDevice(UsbDevice device) {
    	UsbInterface intf = null;
        if (mDeviceConnection != null) {
            // stop the thread that receives images here (if exists)

            if (mInterface != null) {
                mDeviceConnection.releaseInterface(mInterface);
                mInterface = null;
            }
            mDeviceConnection.close();
            mDevice = null;
            mDeviceConnection = null;
            mEndpointIn = null;
        }

        if (device != null) {
        	intf = device.getInterface(0);
        	if(intf != null){
	            UsbDeviceConnection connection = mManager.openDevice(device);
	            if (connection != null) {
	                if (connection.claimInterface(intf, false)) {
	                    mDevice = device;
	                    mDeviceConnection = connection;
	                    mInterface = intf;
	                    
	                    
	                    for(int i = 0;i<4;i++){
	                    	mEndpointIn = intf.getEndpoint(i);
	                    	if (mEndpointIn.getType() == UsbConstants.USB_ENDPOINT_XFER_BULK) {
	                    		if (mEndpointIn.getDirection() == UsbConstants.USB_DIR_IN) {
	                    			return true;
	                    		}
	                    	}
	                    }
	                } else {
	                    connection.close();
	                }
	            }
        	}
        }
        return false;
    }
    
    BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice device = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if(device != null){
                          setupDevice(device);//call method to set up device communication
                       }
                    }
                }
            	
            } else if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                UsbDevice device = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
            	if(device.getVendorId() == 0x04b4 && device.getProductId() == 0x00f1){
                	setupDevice(device);
            	}
            } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                String deviceName = device.getDeviceName();
                if (mDevice != null && mDevice.equals(deviceName)) {
                    setupDevice(null);
                }
            }
        }
    };

	private void set2DImageView() {
		InputStream stream = getResources().openRawResource(R.raw.hand0_0000);
		byte[] rawData = new byte[614400];
		try {
			stream.read(rawData, 0, 614400);
		} catch (IOException e) {
			e.printStackTrace();
		}
		int[] colors = new int[640 * 480];
		int j = 0;
		int i = 0;
		for (; i < rawData.length;) {
			int alpha = 255;//rawData[i+1];
			int red = rawData[i+1];
			int green = rawData[i+1];
			int blue = rawData[i+1];
			colors[j++] =  (alpha<<24)+ (red)+(green << 8)+(blue << 16);
			i += 2;
		}
		Bitmap bmpGrayscale = Bitmap.createBitmap(colors, 640, 480,
				Bitmap.Config.ARGB_8888);
		ImageView image = (ImageView) findViewById(R.id.gray_scale_2d_image);
		image.setImageBitmap(bmpGrayscale);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.capture_button:
			mWaiterThread.mStop = true;
			Intent grey3DView = new Intent(this, Grey3DView.class);
			startActivity(grey3DView);
			break;
		}
	}

	// This gets executed in a non-UI thread:
    public void getDrawFrame() {
        mDeviceConnection.bulkTransfer(mEndpointIn, frame, 2*640*480, TIMEOUT);
 //       Log.e(TAG, "got a packet\n");
        mHandler.post(new Runnable() {
            @Override
            public void run() {
            	set2DImageView();
/*
                // This gets executed on the UI thread so it can safely modify Views
        		int[] colors = new int[640 * 480];
        		int j = 0;
        		int i = 0;
        		for (; i < 2*640*480;) {
        			short greycolor = (short)((short)frame[i] | (short)frame[i+1]<<8);
        			int alpha = 255;
        			int red = (greycolor >> 8);
        			int green = greycolor >> 8;
        			int blue = 255 - (greycolor >> 8);
        			colors[j++] =  (alpha<<24)+ (red)+(green << 8)+(blue << 16);
        			i += 2;
        		}
        		Bitmap bmpGrayscale = Bitmap.createBitmap(colors, 640, 480,
        				Bitmap.Config.ARGB_8888);
        		ImageView image = (ImageView) findViewById(R.id.gray_scale_2d_image);
        		image.setImageBitmap(bmpGrayscale);
*/
            }
        });
    }	

    private class WaiterThread extends Thread {
        public boolean mStop;
        public void run() {
        	mStop = false;
            while (true) {
                synchronized (this) {
                    if (mStop) {
                        return;
                    }
                }
                getDrawFrame();
            }
        }
    }
}
