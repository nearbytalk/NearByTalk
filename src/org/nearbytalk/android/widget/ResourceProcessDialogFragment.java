package org.nearbytalk.android.widget;

import org.nearbytalk.R;
import org.nearbytalk.NearByTalkApplication;
import org.nearbytalk.android.AndroidConfig;
import org.nearbytalk.android.InitResourceCopy;
import org.nearbytalk.android.InitState;
import org.nearbytalk.android.widget.PasswordDialogFragment.PasswordInteractiveState;
import org.nearbytalk.android.widget.PasswordDialogFragment.PasswordProcessHandler;
import org.nearbytalk.runtime.Global;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

public class ResourceProcessDialogFragment extends DialogFragment implements
		OnClickListener, PasswordProcessHandler {

	public static interface ResourceProcessedCallback {

		void onResourceProcessFinish(boolean successful);

	}

	private ResourceProcessedCallback resourceProcessedCallback;

	static int SHOW_RESOURCE_COPY_COMPLETE_DELAY = 1000;

	public ResourceProcessDialogFragment() {
		interactiveState = ResourceInteractiveState.COPY_RESOURCE;
		setCancelable(false);
	}

	public static enum ResourceInteractiveState {
		COPY_RESOURCE, RECOVERY_PROMPT, COMPLETE, FAILED;
	}

	private int failedInformationStringId;

	private static final Logger log = LoggerFactory
			.getLogger(ResourceProcessDialogFragment.class);

	private static final String FRAGMENT_TAG = ResourceProcessDialogFragment.class
			.getName();

	private static final long DELAY_HIDE_MS = 3000;

	private ResourceInteractiveState interactiveState = ResourceInteractiveState.COPY_RESOURCE;

	private Button okButton;
	private Button cancelButton;

	private ProgressBar copyProgressBar;
	private TextView genericInfoTextView;

	private View buttonsView;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		View view = inflater.inflate(R.layout.dialog_resource_process,
				container);
		okButton = (Button) view.findViewById(R.id.buttonOk);
		cancelButton = (Button) view.findViewById(R.id.buttonRetry);

		copyProgressBar = (ProgressBar) view
				.findViewById(R.id.progressBar_copy_resource);

		genericInfoTextView = (TextView) view
				.findViewById(R.id.textView_generic_info);

		buttonsView = view.findViewById(R.id.layoutButtons);

		okButton.setOnClickListener(this);
		cancelButton.setOnClickListener(this);

		return view;
	}

	@Override
	public void onStart() {
		super.onStart();
		setInteractiveState(interactiveState);
	}

	/**
	 * set process type, can run on thread
	 * 
	 * @param interactiveState
	 */
	protected void setInteractiveState(
			final ResourceInteractiveState interactiveState) {

		getActivity().runOnUiThread(new Runnable() {

			@Override
			public void run() {
				// hide previous

				ResourceProcessDialogFragment.this.interactiveState = interactiveState;

				// default set to GONE, SET_PASSWORD/UNLOCK_PASSWORD set to
				// VISIBLE

				copyProgressBar.setVisibility(View.GONE);

				okButton.setEnabled(true);

				// TODO giving better tile
				getDialog().setTitle(interactiveState.toString());

				switch (interactiveState) {
				case COPY_RESOURCE:
					copyResource();
					break;
				case RECOVERY_PROMPT:
					// show message only ,dont copy until user click ok
					genericInfoTextView.setText(R.string.text_recover_prompt);
					break;
				case COMPLETE:
					complete();
					break;
				case FAILED:
					failed();
					break;
				}
			}
		});

	}

	protected void complete() {

		genericInfoTextView.setText(R.string.text_resource_copy_complete);

		new Handler().postDelayed(new Runnable() {

			public void run() {
				// http://stackoverflow.com/questions/9325238/proper-way-of-dismissing-dialogfragment-while-application-is-in-background

				if (isResumed()) {
					dismiss();
				}
			}
		}, DELAY_HIDE_MS);
	}

	private void failed() {

		genericInfoTextView.setText(failedInformationStringId);

		buttonsView.setVisibility(View.VISIBLE);

		cancelButton.setVisibility(View.GONE);
	}

	@Override
	public void onClick(View v) {
		switch (interactiveState) {
		case RECOVERY_PROMPT:
			if (v == okButton) {
				copyResource();
			} else {

				// user do not want to recovery ,go back to previous fragment
				failedInformationStringId = R.string.error_resource_corrupted;
				setInteractiveState(ResourceInteractiveState.FAILED);
			}
			return;

		case FAILED:
			if (v == okButton) {
				dismiss();
			}
			break;
		}

	}

	public static void popIfNecessary(Fragment previousFragment,
			final ResourceProcessedCallback resourceProcessedCallback,
			boolean forceReinit) {

		AndroidConfig config = NearByTalkApplication.getInstance().getConfig();

		boolean alreadyCopied = config.isResourceCopyFinished();

		if (alreadyCopied &&  !forceReinit) {
			// no need to copy resouce ,just continue with password stage

			log.info("resource already copied");
			PasswordDialogFragment.unlockPassword(
					previousFragment.getFragmentManager(),
					new PasswordProcessHandler() {
						@Override
						public void onPasswordProcessed(
								PasswordInteractiveState state, boolean success) {

							resourceProcessedCallback
							.onResourceProcessFinish(success);
						}
					});
			return;
		}

		// not copied, or user force reinit
		// Create and show the dialog.

		ResourceProcessDialogFragment frag = new ResourceProcessDialogFragment();

		frag.resourceProcessedCallback = resourceProcessedCallback;

		if (forceReinit) {
			frag.interactiveState = ResourceInteractiveState.RECOVERY_PROMPT;
		} else if (!alreadyCopied) {
			frag.interactiveState = ResourceInteractiveState.COPY_RESOURCE;
		} else {
			throw new IllegalAccessError("should not reach here");
		}

		FragmentTransaction ft = previousFragment.getFragmentManager()
				.beginTransaction();

		Fragment prev = previousFragment.getFragmentManager()
				.findFragmentByTag(FRAGMENT_TAG);
		if (prev != null) {
			ft.remove(prev);
		}

		ft.addToBackStack(null);

		frag.show(ft, FRAGMENT_TAG);

	}

	/**
	 * copy resource and translate control flow to set_password
	 * 
	 */
	private void copyResource() {
		
		//when recopy resource ,reset global password
		
		Global.getInstance().getPlatformAbstract().commitPasswordChangeNew(Global.DEFAULT_RAW_DATASTORE_PASSWORD);

		// don't allow user trigger button action, or interrupt

		copyProgressBar.setVisibility(View.VISIBLE);

		buttonsView.setVisibility(View.GONE);
		NearByTalkApplication application = (NearByTalkApplication) getActivity()
				.getApplication();

		final InitResourceCopy backgroundThreadCopy = new InitResourceCopy(
				application.getConfig().getAppRootDir(), getActivity()
						.getAssets());

		new AsyncTask<Void, Double, InitState>() {

			@Override
			protected void onProgressUpdate(Double... values) {

				int progress = (int) (values[0] * 100);

				genericInfoTextView.setText(String
						.format(getString(R.string.diag_init_loading_message),
								progress));
			}

			@Override
			protected void onPostExecute(InitState result) {

				if (result != InitState.COMPLETE) {
					failedInformationStringId = R.string.error_copy_resource_failed;
					setInteractiveState(ResourceInteractiveState.FAILED);
				}

				genericInfoTextView.setText(R.string.diag_init_complete);

				copyProgressBar.setProgress(copyProgressBar.getMax());

				PasswordDialogFragment.setPassword(getFragmentManager(),
						ResourceProcessDialogFragment.this);

				return;
			}

			@Override
			protected void onPreExecute() {
				// has created
				onProgressUpdate(0.0);
			}

			@Override
			protected InitState doInBackground(Void... params) {
				try {
					publishProgress(0.0);
					for (int i = 0; i < 100; ++i) {

						switch (backgroundThreadCopy.getInitState()) {
						case COMPLETE: {
							return InitState.COMPLETE;
						}
						case ERROR: {
							return InitState.ERROR;
						}
						case CLEANING: {
							return InitState.CLEANING;
						}
						case STARTING: {

							publishProgress(backgroundThreadCopy

							.getInitCopyPercent());

							Thread.sleep(2000);
							continue;
						}
						}
					}

				} catch (InterruptedException e) {
				}
				log.error("init prepare timeout");
				return InitState.STARTING;

			}
		}.execute();

		backgroundThreadCopy.doBackgroundThreadCopy();
	}

	@Override
	public void onDismiss(DialogInterface dialog) {
		super.onDismiss(dialog);

		resourceProcessedCallback
				.onResourceProcessFinish(interactiveState != ResourceInteractiveState.FAILED);
	}

	@Override
	public void onPasswordProcessed(PasswordInteractiveState state,
			boolean success) {
		
		
		if (state!=PasswordInteractiveState.SET_PASSWORD) {
			throw new IllegalAccessError("bad use of password dialog fragment");
		}
		
		//no password or set password successfully
		failedInformationStringId=R.string.error_set_password_failed;
		setInteractiveState(success?ResourceInteractiveState.COMPLETE:ResourceInteractiveState.FAILED);

	}

}
