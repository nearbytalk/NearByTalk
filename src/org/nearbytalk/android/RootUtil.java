package org.nearbytalk.android;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import org.nearbytalk.NearByTalkApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sufficientlysecure.rootcommands.Shell;
import org.sufficientlysecure.rootcommands.command.SimpleCommand;
import org.sufficientlysecure.rootcommands.util.RootAccessDeniedException;

import android.content.Context;
import android.content.SharedPreferences;


/**
 * assume root permission is acquired already
 * 
 */
public class RootUtil {

	private static RootUtil instance;

	private Shell rootShell;

	private SharedPreferences preferences;

	public RootUtil() {

		preferences = NearByTalkApplication.getInstance()
				.getSharedPreferences(PreferenceKey.ACTIVE_PREFENCE_KEY,
						Context.MODE_PRIVATE);

	}

	public synchronized static RootUtil getInstance() {

		if (instance == null) {
			instance = new RootUtil();
		}
		return instance;
	}

	

	/**
	 * blocking call ,try to acquire root permission
	 * 
	 * @return true if success ,false if failed
	 */
	public boolean acquirePermission() {
		try {
			rootShell = Shell.startRootShell();

			log.info("root permission acquired");

			preferences
					.edit()
					.putBoolean(PreferenceKey.ROOT_PERMISSION_ACQUIRED_KEY,
							true).commit();

			return true;
		} catch (RootAccessDeniedException e) {
			log.error("root access denied");
		} catch (IOException e) {
			log.error("io exception");
		}

		preferences.edit()
				.putBoolean(PreferenceKey.ROOT_PERMISSION_ACQUIRED_KEY, false)
				.commit();

		return false;

	}

	private static Logger log = LoggerFactory.getLogger(RootUtil.class);

	public static void executeCommand(String commandString)
			throws RootFailedException {

		SimpleCommand command = new SimpleCommand(commandString);
		Shell rootShell = null;
		try {
			rootShell = Shell.startRootShell();

			rootShell.add(command).waitForFinish();
		} catch (RootAccessDeniedException e) {
			log.error("error:", e);
			throw new RootFailedException();
		} catch (IOException e) {
			log.error("error", e);
			throw new RootFailedException();
		} catch (TimeoutException e) {
			log.error("timeout:", e);
			throw new RootFailedException();
		}

		if (rootShell != null) {
			try {
				rootShell.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				log.error("close root shell failed:", e);
			}
		}
	}
}
