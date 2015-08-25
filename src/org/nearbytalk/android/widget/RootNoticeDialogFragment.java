package org.nearbytalk.android.widget;

import org.nearbytalk.R;
import org.nearbytalk.NearByTalkApplication;
import org.nearbytalk.android.AndroidConfig;
import org.nearbytalk.android.PreferenceKey;
import org.nearbytalk.android.RootUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
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
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;

public class RootNoticeDialogFragment extends DialogFragment implements
		OnClickListener, OnCheckedChangeListener {

	public static final String FRAGMENT_TAG = RootNoticeDialogFragment.class
			.getName();

	public RootNoticeDialogFragment() {
		interactiveState = RootInteractiveState.FIRST;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		View v = inflater
				.inflate(R.layout.dialog_root_notice, container, false);

		okOrRetryButton = (Button) v.findViewById(R.id.buttonOk);
		cancelButton = (Button) v.findViewById(R.id.buttonCancel);

		cancelButton.setVisibility(View.GONE);

		informationView = (TextView) v.findViewById(R.id.textRootNotice);
		notShowAgainBox = (CheckBox) v.findViewById(R.id.checkBoxNotShowAgain);

		okOrRetryButton.setOnClickListener(this);
		cancelButton.setOnClickListener(this);
		notShowAgainBox.setOnCheckedChangeListener(this);

		preferences = getActivity().getSharedPreferences(
				PreferenceKey.ACTIVE_PREFENCE_KEY, Context.MODE_PRIVATE);

		// first time ,only OK button is shown ,force user to acquire root
		// permission

		uiMessageHandler = new Handler();

		setCancelable(false);
		return v;

	}

	@Override
	public void onDismiss(DialogInterface dialog) {
		super.onDismiss(dialog);
		
		
		NearByTalkApplication.getInstance().getConfig().
		setShowNoRootNoticeAgain(!notShowAgainBox.isChecked());

		rootCheckCallback.onRootCheckFinish(NearByTalkApplication
				.getInstance().getConfig().isRootAcquired());

	}

	public static interface RootCheckCallback {
		/**
		 * run on ui thread
		 */
		void onRootCheckFinish(boolean hasRoot);
	}

	private SharedPreferences preferences;

	private Button okOrRetryButton;
	private Button cancelButton;
	private TextView informationView;

	private CheckBox notShowAgainBox;

	private int retryCounter = 0;

	private static final int RETRY_MAX = 5;

	private static final Logger log = LoggerFactory
			.getLogger(RootNoticeDialogFragment.class);

	private static enum RootInteractiveState {
		FIRST(0), WAIT(1), RETRY(2), SUCCESS(3), FAILED(4);
		private RootInteractiveState(int index) {
			this.index = index;
		}

		int index;

	}

	RootInteractiveState interactiveState;

	RootCheckCallback rootCheckCallback;

	public RootInteractiveState getInteractiveState() {
		return interactiveState;
	}

	/**
	 * only run on ui thread
	 * 
	 * @param interactiveState
	 */
	private void setInteractiveState(final RootInteractiveState set) {

		final String[] notices = getResources().getStringArray(
				R.array.array_root_notice);

		interactiveState = set;
		informationView.setText(notices[interactiveState.index]);
		
		cancelButton.setVisibility(View.GONE);

		switch (interactiveState) {
		case FIRST: {
			acquireRootPermission();
			return;
		}
		case SUCCESS: {
			okOrRetryButton.setText(android.R.string.ok);
			delayHide();
			return;
		}

		case RETRY: {
			retry();
			return;

		}
		case FAILED: {

			okOrRetryButton.setText(android.R.string.ok);
			delayHide();
			return;
		}
		}
	}

	protected void retry() {
		log.debug("retry");

		okOrRetryButton.setText(R.string.button_retry);
		cancelButton.setVisibility(View.VISIBLE);

	}

	Handler uiMessageHandler;

	protected void delayHide() {
		uiMessageHandler.postDelayed(new Runnable() {
			@Override
			public void run() {
				//http://stackoverflow.com/questions/9325238/proper-way-of-dismissing-dialogfragment-while-application-is-in-background
				
				if (isResumed()) {
					dismiss();
				}
			}
		}, DELAY_HIDE_MS);
	}
	
	

	private static void popDialog(final Fragment prevFragment,
			final RootCheckCallback rootCheckCallback, RootInteractiveState state) {

		FragmentTransaction ft = prevFragment.getFragmentManager()
				.beginTransaction();
		Fragment prev = prevFragment.getFragmentManager().findFragmentByTag(
				FRAGMENT_TAG);
		if (prev != null) {
			ft.remove(prev);
		}
		ft.addToBackStack(null);

		// Create and show the dialog.

		RootNoticeDialogFragment frag = new RootNoticeDialogFragment();
		frag.rootCheckCallback = rootCheckCallback;

		frag.interactiveState = state;
		frag.show(ft, FRAGMENT_TAG);
	}

	@Override
	public void onStart() {
		super.onStart();

		setInteractiveState(interactiveState);
	}

	/**
	 * @param sCallback
	 *            callback run on ui thread
	 */
	private static void acquireRootPermissionInternal(final Activity activity,
			final RootCheckCallback sCallback) {

		new Thread() {
			@Override
			public void run() {
				final boolean rootPermission = RootUtil.getInstance()
						.acquirePermission();

				activity.runOnUiThread(new Runnable() {

					@Override
					public void run() {
						sCallback.onRootCheckFinish(rootPermission);
					}
				});
			}

		}.start();
	}

	/**
	 * acuqire root permission quiet, if failed ,translate to
	 * RootNoticeDialogFragment
	 * 
	 * @param fm
	 */
	public static void popIfNecessary(final Fragment prevFragment,
			final RootCheckCallback rootCheckCallback, boolean forceShow) {

		boolean firstAcquire = NearByTalkApplication.getInstance().getConfig().isFirstRootAcquire();
		

		boolean notShowRootNoticeAgain = !NearByTalkApplication.getInstance().getConfig().isShowNoRootNoticeAgain();

		// no quiet ,just popup Fragment
		if (firstAcquire || !notShowRootNoticeAgain || forceShow) {

			prevFragment.getActivity().runOnUiThread(new Runnable() {
				@Override
				public void run() {
					popDialog(prevFragment, rootCheckCallback,
							RootInteractiveState.FIRST);
				}
			});

			return;
		}

		// quiet

		final boolean previousAcquired=NearByTalkApplication.getInstance().getConfig().isRootAcquired();

		acquireRootPermissionInternal(prevFragment.getActivity(),
				new RootCheckCallback() {

					@Override
					public void onRootCheckFinish(final boolean hasRoot) {
						if (hasRoot) {
							// quiet
							rootCheckCallback.onRootCheckFinish(true);
							return;
						}
						if (previousAcquired) {
						// acquired before ,but failed this time
							popDialog(prevFragment, rootCheckCallback,
									RootInteractiveState.RETRY);
							return;
						}
						rootCheckCallback.onRootCheckFinish(false);
						//hasn't acquired, also failed this time
					}
				});
	}

	private static int DELAY_HIDE_MS = 5000;

	/**
	 * acquire root permission in background, if success ,workflow translate to
	 * setInteractiveState
	 * 
	 * SUCCESS
	 */
	private void acquireRootPermission() {

		// do not allow to press OK button
		okOrRetryButton.setEnabled(false);

		setInteractiveState(RootInteractiveState.WAIT);

		acquireRootPermissionInternal(getActivity(), new RootCheckCallback() {

			@Override
			public void onRootCheckFinish(boolean hasRoot) {
				
				AndroidConfig config=NearByTalkApplication.getInstance().getConfig();


				if (config.isFirstRootAcquire()) {
					config.setFirstRootAcquire(false);
				}			

				okOrRetryButton.setEnabled(true);

				if (hasRoot) {
					setInteractiveState(RootInteractiveState.SUCCESS);
					return;
				}

				log.info("retry {}", retryCounter);

				if (retryCounter < RETRY_MAX) {
					setInteractiveState(RootInteractiveState.RETRY);
				} else {
					okOrRetryButton.setText(android.R.string.ok);
					cancelButton.setVisibility(View.GONE);
					setInteractiveState(RootInteractiveState.FAILED);
				}
			}
		});
	}

	@Override
	public void onClick(View v) {

		switch (interactiveState) {
		case FIRST:
			acquireRootPermission();
			break;
		case SUCCESS: {
			dismiss();
			break;
		}
		case RETRY: {
			if (v == okOrRetryButton) {
				++retryCounter;
				acquireRootPermission();
				break;
			} else if (v == cancelButton) {
				setInteractiveState(RootInteractiveState.FAILED);
				break;
			}
		}
		case FAILED: {
			dismiss();
			break;
		}
		}

	}

	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

		if (buttonView == notShowAgainBox) {
			NearByTalkApplication.getInstance().getConfig().setShowNoRootNoticeAgain(!isChecked);
		}

	}

}
