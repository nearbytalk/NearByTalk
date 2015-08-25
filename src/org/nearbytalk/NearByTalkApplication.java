package org.nearbytalk;

import org.nearbytalk.android.AndroidConfig;
import org.nearbytalk.android.NoExternalStorageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.app.Application;
import android.content.Context;
import android.net.wifi.WifiManager;


public class NearByTalkApplication extends Application {

	
	private Object wifiLock=new Object();
	
	private WifiManager wifiManager;

	private static NearByTalkApplication instance;

	private static Logger log = LoggerFactory
			.getLogger(NearByTalkApplication.class);

	private AndroidConfig config;
	
	public WifiManager getWifiManager(){
		synchronized (wifiLock) {
			if (wifiManager==null) {
				wifiManager=(WifiManager) getSystemService(Context.WIFI_SERVICE);
			}
		}
		
		return wifiManager;
	}


	/**
	 * use a background thread to do init copy,not block UI
	 * @throws NoExternalStorageException 
	 * 
	 */

	
	public void initCheck() throws NoExternalStorageException{

		if (!inited) {
				config=new AndroidConfig(this);
				inited=true;
				
		}

	}
	
	private boolean inited=false;
	 
	public void onCreate() {
		super.onCreate();
		instance = this;

		
		
	}

	public static NearByTalkApplication getInstance() {
		return instance;
	}

	public AndroidConfig getConfig() {
		return config;
	}

	

}
