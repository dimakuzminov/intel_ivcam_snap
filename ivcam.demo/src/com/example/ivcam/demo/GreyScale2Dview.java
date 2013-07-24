package com.example.ivcam.demo;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentLinkedQueue;

import android.hardware.usb.UsbRequest;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;


import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.hardware.usb.UsbRequest;
import android.os.Bundle;
import android.os.Message;
import android.text.Layout;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.app.PendingIntent;
import android.os.Handler;

public class GreyScale2Dview extends Activity implements OnClickListener {
	private Button mCaptureButton;
	//usb related 
    private UsbManager mManager;
    private UsbDevice mDevice = null;
    private UsbDeviceConnection mDeviceConnection = null;
    private UsbInterface mInterface = null;
    private UsbEndpoint mEndpointIn = null;
    private PendingIntent mPermissionIntent;
    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
    private final int TIMEOUT = 500;//timeout in miliseconds - in case bulk data is not there...
    //the USB receiver thread
    private final WaiterThread mWaiterThread = new WaiterThread(this);
    //pool to hold frames
    private final ConcurrentLinkedQueue<ByteBuffer> mFramePool = new ConcurrentLinkedQueue<ByteBuffer>();
    private final ConcurrentLinkedQueue<ByteBuffer> mFrameQueue = new ConcurrentLinkedQueue<ByteBuffer>();
    //NOTE: todo: lock the frame between the intent and thread or use dynamic memory.
    //for logging
    private static final String TAG = "ivcam.demo";
    private Handler mHandler;
	private int[] colors;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		boolean bDeviceExists = false;
		super.onCreate(savedInstanceState);
		colors = new int[640 * 480];
	    for(int i = 0;i<2;i++){
	        ByteBuffer frame = ByteBuffer.allocate(614416);
	        mFramePool.add(frame);
	    }
		//setup the USB
		mManager = (UsbManager)getSystemService(Context.USB_SERVICE);
		// check for existing devices
		mPermissionIntent = PendingIntent.getBroadcast((Context)this, 0, new Intent(ACTION_USB_PERMISSION), 0);
		// listener for new devices
		IntentFilter filter = new IntentFilter();
		filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
		filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
		filter.addAction(ACTION_USB_PERMISSION);
		registerReceiver(mUsbReceiver, filter);
	    mHandler = new Handler() {
	        @Override
	        public void handleMessage(Message msg) {
	            switch (msg.what) {
	                case 0:
	                case 10:
	               	default:
	                    redrawView();
	                    break;
	            }
	        }
	    };
		for (UsbDevice device :  mManager.getDeviceList().values()) {
			if(device.getVendorId() != 0x04b4 || device.getProductId() != 0x00f1){
				continue;
			}
			mManager.requestPermission(device, mPermissionIntent);
//			bDeviceExists = setupDevice(device);
			break;
		}
//		if(bDeviceExists == false){
//			throw new RuntimeException("usb not initialized\n");
//		}
		setContentView(R.layout.activity_grey_scale2_dview);
		mCaptureButton = (Button) findViewById(R.id.capture_button);
		mCaptureButton.setOnClickListener(this);
		//TODO: add an exception if there is no device? or wait for one?
//	    set2DImageView(); //fixed resource demo only
	}

    @Override
    public void onDestroy() {
    	mWaiterThread.mStop = true;
        unregisterReceiver(mUsbReceiver);
        setupDevice(null);
        super.onDestroy();
    }

    // Sets the current USB device and interface
    private boolean setupDevice(UsbDevice device) {
    	boolean retval = false;
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
	                    			if(mWaiterThread.isAlive() == false)
	                    				mWaiterThread.start();
	                    			retval = true;
	                    			break;
	                    		}
	                    	}
	                    }
	                } else {
	                    connection.close();
	                }
	            }
        	}
        }
        return retval;
    }

    //hanldes the USB related intents
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

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.capture_button:
			Intent grey3DView = new Intent(this, Grey3DView.class);
			startActivity(grey3DView);
			break;
		}
	}
	public void refreshView(){
		runOnUiThread(new Runnable(){			
		@Override
            public void run() { redrawView(); }});
	}
	
	public UsbRequest AllocRequest(){
        UsbRequest request = new UsbRequest();
        request.initialize(mDeviceConnection, mEndpointIn);
        return request;
	}

    private class WaiterThread extends Thread {
    	int i;
        // pool of requests for the IN endpoint
        private final LinkedList<UsbRequest> mReqPool = new LinkedList<UsbRequest>();
        private final LinkedList<ByteBuffer> mBufPool = new LinkedList<ByteBuffer>();
        public boolean mStop;
        public GreyScale2Dview mActivity;
        public WaiterThread(GreyScale2Dview activity){
        	mActivity = activity;
        }
        
        public void run() {
        	ByteBuffer frame;
        	mStop = false;
        	int j;
        	UsbRequest req;
        	ByteBuffer buf;
        	for(int i=0;i<38;i++){
                req = AllocRequest();
                mReqPool.addFirst(req);
            	buf = ByteBuffer.allocate(16384);
        		mBufPool.addFirst(buf);
        	}
            for(j = 0; j<10;j++){
                req = mReqPool.getFirst();
                buf = mBufPool.getFirst();
                buf.rewind();
                req.setClientData(buf);
            	req.queue(buf, 16384);
            }
            while (true) {
                synchronized (this) {
                    if (mStop) {
                        return;
                    }
                }
                
                try{
                	frame = mFramePool.remove();
                }catch(NoSuchElementException e){
                	Log.e(TAG, "no frame to fill\n");
                	Thread.yield();
                	continue;
                }
            	frame.rewind();
                for(j = 0; j<38;j++){
                    req = mDeviceConnection.requestWait();
                    buf = (ByteBuffer)req.getClientData();
                    req.setClientData(null);
                	buf.rewind();
                	frame.rewind();
                	frame.put(buf);
                	mBufPool.addLast(buf);
                	mReqPool.addLast(req);
                    req = mReqPool.getFirst();
                    buf = mBufPool.getFirst();
                    buf.rewind();
                    req.setClientData(buf);
                	req.queue(buf, 16384);
                }
            	Log.d(TAG,"frame length = "+frame.capacity()+" \n");
            	frame.rewind();
               	mFrameQueue.add(frame);
            	mActivity.refreshView();
            	Thread.yield();
            }
        }
    }


    private void redrawView() {
    	Log.d(TAG,"entering redraw\n");
 		ByteBuffer frame;
        try{
           	frame = mFrameQueue.remove();
        }catch(NoSuchElementException e){
        	Log.e(TAG, "no frame to render\n");
        	return;
        }
 		frame.rewind();
 		frame.position(16);
 		short val;
/*
		for (int i = 0; i < 640*480;i++) {
			
//			short val = (short)(frame.array()[2*i]);
//			val += 256 * (short)(frame.array()[1+2*i]);
//			short val = frame.getShort(i);
//			int alpha = 255;
//			int red = 70;//val/256;
//			int green = val/256;
//			int blue = val/256;
			val = frame.getShort();
			colors[i] =  (int) val;
		}
*/
		frame.rewind();
		mFramePool.add(frame);
		Bitmap bmpGrayscale = Bitmap.createBitmap(colors, 640, 480,	Bitmap.Config.ARGB_8888);
		ImageView image = (ImageView) findViewById(R.id.gray_scale_2d_image);
		image.setImageBitmap(bmpGrayscale);

    	//set2DImageView();
    }
/*    
    //used for a static resource demo only
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
		image.invalidate();
	}
*/
}
