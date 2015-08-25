package org.nearbytalk.android;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.io.FileUtils;
import org.nearbytalk.NearByTalkApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.res.AssetManager;

public class InitResourceCopy {

	private static Logger log = LoggerFactory.getLogger(InitResourceCopy.class);

	private final String destRootPath;

	private final AssetManager assetManager;
	
	/**
	 * 
	 */
	private static final int TOTAL_FILE_TO_COPY = 60;

	private double initCopyPercent = 0;

	private InitState initState = InitState.STARTING;

	public InitState getInitState() {
		return initState;
	}

	// http://www.twodee.org/blog/?p=4518
	private void copyAsset(String path) throws IOException {

		// If we have a directory, we make it and recurse. If a file, we
		// copy its
		// contents.
		try {
			String[] contents = assetManager.list(path);

			// The documentation suggests that list throws an IOException,
			// but doesn't
			// say under what conditions. It'd be nice if it did so when the
			// path was
			// to a file. That doesn't appear to be the case. If the
			// returned array is
			// null or has 0 length, we assume the path is to a file. This
			// means empty
			// directories will get turned into files.
			if (contents == null || contents.length == 0)
				throw new IOException();

			// Make the directory.
			File dir = new File(destRootPath, path);
			dir.mkdirs();

			// Recurse on the contents.
			for (String entry : contents) {
				copyAsset(path + "/" + entry);
			}
		} catch (IOException e) {
			copyFileAsset(path);
		}
	}

	/**
	 * Copy the asset file specified by path to app's data directory. Assumes
	 * parent directories have already been created.
	 * 
	 * @param path
	 *            Path to asset, relative to app's assets directory.
	 * @throws IOException
	 */
	private void copyFileAsset(String path) throws IOException {
		File file = new File(destRootPath, path);

		log.debug("copy file {} to {}", path,file);

		initCopyPercent = Math.min(1.0, initCopyPercent + 1.0
				/ TOTAL_FILE_TO_COPY);

		InputStream in = assetManager.open(path);
		OutputStream out = new FileOutputStream(file);
		byte[] buffer = new byte[1024];
		int read = in.read(buffer);
		while (read != -1) {
			out.write(buffer, 0, read);
			read = in.read(buffer);
		}
		out.close();
		in.close();

	}

	public InitResourceCopy(String destRootPath, final AssetManager assetManager) {
		this.destRootPath = destRootPath;
		this.assetManager = assetManager;
	}

	/**
	 * copy all files from asset .
	 * config.passwordSHA1 will be reset to default password's
	 * 
	 */
	public void doBackgroundThreadCopy() {
		
		
		new Thread() {
			@Override
			public void run() {
				
				NearByTalkApplication.getInstance().getConfig().setResourceCopyFinished(false);

				try {
					
					//must delete previous resources ...
					FileUtils.deleteDirectory(new File(destRootPath));
					
					// http://stackoverflow.com/questions/3631370/list-assets-in-a-subdirectory-using-assetmanager-list
					
					//anrdoird reference is confuse. pass '.' as current Asset path not work
					//pass "/" as Asset path gives all file struct in apk root. so use a subfolder
					
					copyAsset(AndroidConfig.WEB_ROOT);
					
					NearByTalkApplication
							.getInstance()
							.getConfig()
							.setResourceCopyFinished(true);

					
					initState = InitState.COMPLETE;
				} catch (IOException e) {
					log.error("init resource copy error {}", e);
					initState = InitState.ERROR;
				}
			}
		}.start();
	}

	public double getInitCopyPercent() {
		return initCopyPercent;
	}

	
}
