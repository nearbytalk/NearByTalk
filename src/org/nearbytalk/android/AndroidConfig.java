package org.nearbytalk.android;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.Context;
import android.content.SharedPreferences;

public class AndroidConfig {
	private boolean firstRun = true;

	private static Logger log = LoggerFactory.getLogger(AndroidConfig.class);

	/**
	 * default save to external storage, call set to other later by user
	 * 
	 */
	private String appRootDir;

	public static final String WEB_ROOT = "WebRoot";

	public static final String FIRST_RUN_KEY = "firstRun";

	public static final String APP_ROOT_DIR_KEY = "appRootDir";

	public static final String USE_DEFAULT_PASSWORK_KEY = "useDefaultKeyword";

	private static final String NET_INTERFACE_NAME_SEPERATOR = ";";

	public boolean isFirstRun() {
		return firstRun;
	}

	public String getAppRootDir() {
		return this.appRootDir;
	}

	private final SharedPreferences preference;

	public AndroidConfig(Context context) throws NoExternalStorageException {
		this.preference = context.getSharedPreferences(
				PreferenceKey.ACTIVE_PREFENCE_KEY, Context.MODE_PRIVATE);

		firstRun = preference.getBoolean(FIRST_RUN_KEY, true);

		log.info("is first run : {}", firstRun);

		File externalFilesDir = context.getExternalFilesDir(null);

		if (externalFilesDir == null) {

			log.error("phone has no external storage");

			throw new NoExternalStorageException();

		}

		String defaultAppRootPath = context.getExternalFilesDir(null)
				.getAbsolutePath() + File.separatorChar;
		appRootDir = context.getSharedPreferences("PREFERENCE",
				Context.MODE_PRIVATE).getString(APP_ROOT_DIR_KEY,
				defaultAppRootPath);

		if (firstRun) {
			preference.edit().putString(APP_ROOT_DIR_KEY, appRootDir).commit();
		}

		preference.edit().putBoolean(FIRST_RUN_KEY, false).commit();

		log.info("firstRun:{},appRootDir : {}", firstRun, appRootDir);
	}

	

	/**
	 * true means resouce copy finished ,set
	 * RESOURCE_COPY_FINISHED = true,and set USE_DEFAULT_PASSWORD = NULL
	 * 
	 * false means didn't do resource copy ,set
	 * RESOURCE_COPY_FINISHED = NULL, and set USE_DEFAULT_PASSWORD = NULL
	 * @param value
	 */
	public void setResourceCopyFinished(boolean value) {

		if (value) {
			preference.edit()
					.putBoolean(PreferenceKey.RESOURCE_COPY_FINISHED_KEY, true)
					.commit();

		}else {
			preference.edit()
			.putBoolean(PreferenceKey.RESOURCE_COPY_FINISHED_KEY, false).commit();
		}
	}
	

	
	public boolean isResourceCopyFinished(){
		return preference.getBoolean(PreferenceKey.RESOURCE_COPY_FINISHED_KEY, false);
	}

	public void setDNSForwardPort(int port) {
		preference.edit().putInt(PreferenceKey.DNS_FORWARD_PORT_KEY, port)
				.commit();
	}

	public static int INVALID_PORT = -1;

	public int getDNSForwardPort() {
		return preference.getInt(PreferenceKey.DNS_FORWARD_PORT_KEY,
				INVALID_PORT);
	}

	public void setHTTPForwardPort(int port) {
		preference.edit().putInt(PreferenceKey.HTTP_FORWARD_PORT_KEY, port)
				.commit();
	}

	public int getHTTPForwardPort() {
		return preference.getInt(PreferenceKey.HTTP_FORWARD_PORT_KEY,
				INVALID_PORT);
	}
	
	public boolean isDestroyWhenPasswordWrong(){
		return preference.getBoolean(PreferenceKey.DESTROY_WHEN_PASSWORD_WRONG_KEY, false);
	}
	
	public void setDestroyWhenPasswordWrong(boolean value){
		preference.edit().putBoolean(PreferenceKey.DESTROY_RETRY_MAX_COUNT_KEY, value).commit();
	}
	
	public void setDestroyRetryMaxCount(int value){
		preference.edit().putInt(PreferenceKey.DESTROY_RETRY_MAX_COUNT_KEY, value).commit();
	}
	
	public int getDestroyRetryMaxCount(){
		return preference.getInt(PreferenceKey.DESTROY_RETRY_MAX_COUNT_KEY, 3);
	}

	
	public boolean isShowWifiDisconnectNoticeAgain(){
		return preference.getBoolean(PreferenceKey.SHOW_WIFI_DISCONNECT_NOTICE_AGAIN_KEY, true);
	}
	
	public void setShowWifiDisconnectNoticeAgain(boolean value){
		preference.edit().putBoolean(PreferenceKey.SHOW_WIFI_DISCONNECT_NOTICE_AGAIN_KEY, value).commit();
	}
	
	public boolean isShowNoRootNoticeAgain(){
		return preference.getBoolean(PreferenceKey.SHOW_NO_ROOT_NOTICE_AGAIN_KEY, true);
	}
	
	public void setShowNoRootNoticeAgain(boolean value){
		preference.edit().putBoolean(PreferenceKey.SHOW_NO_ROOT_NOTICE_AGAIN_KEY, value).commit();
	}
	
	public boolean isFirstRootAcquire(){
		return preference.getBoolean(PreferenceKey.FIRST_ROOT_ACQUIRE, true);
	}

	public void setFirstRootAcquire(boolean value){
		preference.edit().putBoolean(PreferenceKey.FIRST_ROOT_ACQUIRE, value).commit();
	}
	
	public void setDefaultSelectInterfaces(Set<String> names){
		
		StringBuilder toSave=new StringBuilder();
		
		for (String string : names) {
			toSave.append(NET_INTERFACE_NAME_SEPERATOR+string);
		}
		
		preference.edit().putString(PreferenceKey.DEFAULT_SELECT_INTERFACE_NAME_KEY, toSave.toString()).commit();
	}
	
	
	public Set<String> getDefaultSelectInterfaces(){
		String defaultSelected=preference.getString(PreferenceKey.DEFAULT_SELECT_INTERFACE_NAME_KEY, null);
		
		Set<String> ret=new HashSet<String>();
		
		if (defaultSelected==null) {
			return ret;
		}
		
		for (String string : defaultSelected.split(NET_INTERFACE_NAME_SEPERATOR)) {
			if (!string.matches("^[\\s]*$")) {
				ret.add(string);
			}
		}
		
		return ret;
	}
	
	public void setRememberDefaultSelectInterface(){
		preference.edit().putBoolean(PreferenceKey.REMEMBER_DEFAULT_SELECT_INTERFACE_KEY, true).commit();
	}
	
	public boolean isRememberDefaultSelectInterface(){
		return preference.getBoolean(PreferenceKey.REMEMBER_DEFAULT_SELECT_INTERFACE_KEY, false);
	}
	
	public void clearRememberDefaultSelectInterfaceFlag() {
		preference
				.edit()
				.remove(PreferenceKey.DEFAULT_SELECT_INTERFACE_NAME_KEY)
				.putBoolean(
						PreferenceKey.REMEMBER_DEFAULT_SELECT_INTERFACE_KEY,
						false).commit();
	}

	public boolean isRootAcquired() {
		return preference.getBoolean(
				PreferenceKey.ROOT_PERMISSION_ACQUIRED_KEY, false);
	}
	
	public void setCustomSsid(String customSsid){
		preference.edit().putString(PreferenceKey.CUSTOM_SSID_KEY, customSsid).commit();
	}
	
	public String getCustomSsid(){
		return preference.getString(PreferenceKey.CUSTOM_SSID_KEY, null);
	}
}
