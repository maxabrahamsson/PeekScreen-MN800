/*
 * Copyright (c) 2010 Sony Ericsson
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * 
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.sonyericsson.extras.liveview.plugins.peekscreen;

import com.sonyericsson.extras.liveview.plugins.AbstractPluginService;
import com.sonyericsson.extras.liveview.plugins.PluginConstants;
import com.sonyericsson.extras.liveview.plugins.PluginUtils;

import android.app.KeyguardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.os.RemoteException;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class PeekScreenService extends AbstractPluginService {

    private Handler mHandler = null;
    
    private Bitmap mRotateBitmap = null;

	public PowerManager pm;
	public PowerManager.WakeLock wl ; 
	public KeyguardManager km;
	public Process sh;
	public OutputStream os;
	@Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);

		// Create handler.
		if(mHandler == null) {
		    mHandler = new Handler();
		}
		
		
		pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		wl = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "tag");
		km = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
		sendImage();
	}
	@Override
	public void onCreate() {
		super.onCreate();
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
	}
	
    /**
     * Plugin is sandbox.
     */
    protected boolean isSandboxPlugin() {
        return true;
    }
	
	/**
	 * Must be implemented. Starts plugin work, if any.
	 */
	protected void startWork() {

	}
	
	/**
	 * Must be implemented. Stops plugin work, if any.
	 */
	protected void stopWork() {
		
	}
	
	/**
	 * Must be implemented.
	 * 
	 * PluginService has done connection and registering to the LiveView Service. 
	 * 
	 * If needed, do additional actions here, e.g. 
	 * starting any worker that is needed.
	 */
	protected void onServiceConnectedExtended(ComponentName className, IBinder service) {
		
	}
	
	/**
	 * Must be implemented.
	 * 
	 * PluginService has done disconnection from LiveView and service has been stopped. 
	 * 
	 * Do any additional actions here.
	 */
	protected void onServiceDisconnectedExtended(ComponentName className) {
		
	}

	/**
	 * Must be implemented.
	 * 
	 * PluginService has checked if plugin has been enabled/disabled.
	 * 
	 * The shared preferences has been changed. Take actions needed. 
	 */	
	protected void onSharedPreferenceChangedExtended(SharedPreferences prefs, String key) {
		
	}

	protected void startPlugin() {
		Log.d(PluginConstants.LOG_TAG, "startPlugin");
		startWork();
	}
			
	protected void stopPlugin() {
		km.newKeyguardLock("unlock1").reenableKeyguard();
		if (wl != null && wl.isHeld()) {
			  wl.release();
			  wl = null;
			  
			}
		Log.d(PluginConstants.LOG_TAG, "stopPlugin");
		

	}
	/* 
	 * isReady bool value is checked prior to starting a process of taking screenshot
	 * If bool is false, another request of screenshot is currently being processed.
	 * If bool is true, it is ready to take another screenshot
	 */ 
	public boolean isReady=true;
	public void sendImage()
	{
		 /*
		In case we want to take a screenshot from a specific activity.
			
		 if(mSharedPreferences.getBoolean(PluginConstants.PREFERENCES_PLUGIN_ENABLED, false)) {
			 	final Uri uri = Uri.parse("");  
		        final Intent browserIntent = new Intent();  
		        browserIntent.setData(uri);  
		        String pname=mSharedPreferences.getString("AppPackageName", "");
		        String cname=mSharedPreferences.getString("AppClassName", "");
		        browserIntent.setClassName(pname, pname+"."+cname);  
		        browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);  
		        startActivity(browserIntent); 
		}*/

		// Stops & Exits the method incase it is not ready(isReady==false)
		if(!isReady) {return;}

		// Locks screenshot process until finish
		isReady=false;

		// Unlocks keylock & turn on backlight to take screenshot of current app.
		if ((wl != null) && (wl.isHeld() == false)) { 
			 wl.acquire();
			 km.newKeyguardLock("unlock1").disableKeyguard();
		}
		
		// Takes screenshot and saves it
		 try {
			 sh = Runtime.getRuntime().exec("su", null,null);
			 os = sh.getOutputStream();
	        os.write(("/system/bin/screencap -p " + Environment.getExternalStorageDirectory()+ File.separator +"img.png").getBytes("ASCII"));
	        os.flush();
	        
	        os.close();
	        sh.waitFor();
		} catch (IOException e) {
			System.out.println("didnt do it");
			e.printStackTrace();
		} catch (InterruptedException e) {
			System.out.println("didnt do it");
			e.printStackTrace();
		}	
			
		 BitmapFactory.Options bfOptions=new BitmapFactory.Options();
		 bfOptions.inDither=false;                     //Disable Dithering mode
		 bfOptions.inPurgeable=true;                   //Tell to gc that whether it needs free memory, the Bitmap can be cleared
		 bfOptions.inInputShareable=true;              //Which kind of reference will be used to recover the Bitmap data after being clear, when it will be used in the future
		 bfOptions.inTempStorage=new byte[32 * 1024]; 
		 // Loads screenshot as bitmap
		mRotateBitmap=BitmapFactory.decodeFile(Environment.getExternalStorageDirectory()+ File.separator +"img.png",bfOptions);
		if(mRotateBitmap==null) {return;}
		
		// Makes sure offset is not smaller than the lowest pixel position possible.
		offsetY=Math.max(0, offsetY);
		offsetX=Math.max(0, offsetX);
		
		// Gets FrameSize value from settings, If it is not set set 128
		int size=Integer.parseInt(mSharedPreferences.getString("FrameSize", "128"));
		
		// If cropped screenshot hits edges of screen, stop it moving further
		if(offsetX+size > mRotateBitmap.getWidth())
		{
			offsetX=mRotateBitmap.getWidth()-size;
		}
		if(offsetY+size > mRotateBitmap.getHeight())
		{
			offsetY=mRotateBitmap.getHeight()-size;
		}		
		// Crop image with final values
		mRotateBitmap=Bitmap.createBitmap(mRotateBitmap, offsetX,offsetY,size, size);
		// Scale image by framesize
		mRotateBitmap=Bitmap.createScaledBitmap(mRotateBitmap, 128, 128, false);
		// Send Image
		PluginUtils.rotateAndSend(mLiveViewAdapter, mPluginId, mRotateBitmap, 0);
		// Unlocks screenshot process since it is finished
		isReady=true;
	}
	// Offset values for screenshot positioning
	public int offsetX,offsetY;
	// Button event handler
	// Moves offset values to position cropping area of screenshot
	protected void button(String buttonType, boolean doublepress, boolean longpress) {
	    Log.d(PluginConstants.LOG_TAG, "button - type " + buttonType + ", doublepress " + doublepress + ", longpress " + longpress);
	    int hareketIvme=(Integer.parseInt(mSharedPreferences.getString("MovementAcc", "32")))*(longpress?2:1);
		if(buttonType.equalsIgnoreCase(PluginConstants.BUTTON_UP)) {
		    offsetY-=hareketIvme;
		    sendImage();
		} else if(buttonType.equalsIgnoreCase(PluginConstants.BUTTON_DOWN)) {
            offsetY+=hareketIvme;
            sendImage();
		} else if(buttonType.equalsIgnoreCase(PluginConstants.BUTTON_RIGHT)) {
			offsetX+=hareketIvme;
			sendImage();
		} else if(buttonType.equalsIgnoreCase(PluginConstants.BUTTON_LEFT)) {
		    offsetX-=hareketIvme;
		    sendImage();
		} else if(buttonType.equalsIgnoreCase(PluginConstants.BUTTON_SELECT)) {
			sendImage();
		}
	}

	protected void displayCaps(int displayWidthPx, int displayHeigthPx) {
        Log.d(PluginConstants.LOG_TAG, "displayCaps - width " + displayWidthPx + ", height " + displayHeigthPx);
    }

	protected void onUnregistered() throws RemoteException {
		Log.d(PluginConstants.LOG_TAG, "onUnregistered");
		
	}

	
    protected void screenMode(int mode) {
        Log.d(PluginConstants.LOG_TAG, "screenMode: screen is now " + ((mode == 0) ? "OFF" : "ON"));
    }

	@Override
	protected void openInPhone(String openInPhoneAction) {
		// TODO Auto-generated method stub
		
	}
    
}