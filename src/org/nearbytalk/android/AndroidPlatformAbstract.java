package org.nearbytalk.android;

import org.nearbytalk.NearByTalkApplication;
import org.nearbytalk.runtime.Global;
import org.nearbytalk.runtime.IPlatformAbstract;

public class AndroidPlatformAbstract implements IPlatformAbstract {

	@Override
	public String getAppRootDirectory() {
		return NearByTalkApplication.getInstance().getConfig().getAppRootDir()
				+ AndroidConfig.WEB_ROOT;
	}

	@Override
	public void commitPasswordChangeNew(String newRawPassword) {

		//this should be run only one thread access datastore, no need to protect
		runtimeRawDataStorePassword=newRawPassword;
	}

	/**
	 * password using at runntime ,never stored in anywhere.
	 * 
	 */
	private String runtimeRawDataStorePassword;

	@Override
	public String getRawDataStorePassword() {

		if (runtimeRawDataStorePassword == null) {

			runtimeRawDataStorePassword = Global.DEFAULT_RAW_DATASTORE_PASSWORD;
		}

		return runtimeRawDataStorePassword;

	}

}
