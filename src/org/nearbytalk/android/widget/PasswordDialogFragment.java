package org.nearbytalk.android.widget;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.nearbytalk.NearByTalkApplication;
import org.nearbytalk.R;
import org.nearbytalk.datastore.PasswordChanger;
import org.nearbytalk.datastore.PasswordChanger.PasswordChangeCallback;
import org.nearbytalk.datastore.SQLiteDataStore;
import org.nearbytalk.exception.IncorrectPasswordException;
import org.nearbytalk.exception.NearByTalkException;
import org.nearbytalk.runtime.Global;
import org.nearbytalk.runtime.UniqueObject;
import org.nearbytalk.util.DigestUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.TextView;

public class PasswordDialogFragment extends DialogFragment implements
		TextWatcher, OnClickListener, PasswordChangeCallback {

	private static final Logger log = LoggerFactory
			.getLogger(PasswordDialogFragment.class);

	Button okButton;
	Button cancelButton;

	TextView passwordInput;

	TextView passwordInfo;

	private PasswordInteractiveState interactiveState;

	private PasswordProcessHandler handler;

	public static interface PasswordProcessHandler {
		/**
		 * for SET_PASSWORD, true means password setted, null means no password
		 * false means set password failed
		 * 
		 * @param success
		 */
		void onPasswordProcessed(PasswordInteractiveState state,boolean success);
	}

	public static enum PasswordInteractiveState {
		UNLOCK_PASSWORD, SET_PASSWORD
	}

	/**
	 * 
	 * used when setting password
	 */
	private String firstTimePassword;

	@Override
	public void onStart() {
		super.onStart();

		setInteractiveState(interactiveState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		View view = inflater.inflate(R.layout.dialog_password_process,
				container);
		okButton = (Button) view.findViewById(R.id.buttonOk);
		cancelButton = (Button) view.findViewById(R.id.buttonCancel);

		passwordInput = (TextView) view.findViewById(R.id.inputSsid);

		passwordInput.addTextChangedListener(this);

		passwordInfo = (TextView) view.findViewById(R.id.ssidInfo);

		okButton.setOnClickListener(this);
		cancelButton.setOnClickListener(this);

		return view;
	}

	@Override
	public void afterTextChanged(Editable s) {
		// listen password

		// don't allow empty password
		okButton.setEnabled(s.toString().length() != 0);

	}

	@Override
	public void beforeTextChanged(CharSequence s, int start, int count,
			int after) {

		// nothing
	}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {
		// nothing

	}

	public static void setPassword(FragmentManager fm,
			PasswordProcessHandler handler) {
		popAs(fm, handler, PasswordInteractiveState.SET_PASSWORD);
	}

	public static void unlockPassword(FragmentManager fm,
			PasswordProcessHandler handler) {

		try {

			// try to open with default password
			UniqueObject.getInstance().getDataStore()
					.preCheck(Global.DEFAULT_RAW_DATASTORE_PASSWORD);

			// successful here, no need to popup

			handler.onPasswordProcessed(
					PasswordInteractiveState.UNLOCK_PASSWORD, true);
			return;

		} catch (IncorrectPasswordException e) {
			// not default password

			popAs(fm, handler, PasswordInteractiveState.UNLOCK_PASSWORD);
			return;

		} catch (NearByTalkException e) {

			// error happend, should not continue

			handler.onPasswordProcessed(
					PasswordInteractiveState.UNLOCK_PASSWORD, false);
			return;
		}

	}

	public static final String FRAGMENT_TAG = PasswordDialogFragment.class
			.getName();

	private static void popAs(FragmentManager fm,
			PasswordProcessHandler handler, PasswordInteractiveState state) {
		FragmentTransaction ft = fm.beginTransaction();

		Fragment prev = fm.findFragmentByTag(FRAGMENT_TAG);

		if (prev != null) {
			ft.remove(prev);
		}

		ft.addToBackStack(null);

		PasswordDialogFragment fragment = new PasswordDialogFragment();

		fragment.interactiveState = state;
		
		fragment.handler=handler;

		fragment.show(ft, FRAGMENT_TAG);

	}

	private void setInteractiveState(final PasswordInteractiveState nextState) {

		getActivity().runOnUiThread(new Runnable() {

			@Override
			public void run() {
				switch (nextState) {

				case SET_PASSWORD: {
					// allow cancel password
					cancelButton.setVisibility(View.VISIBLE);
					okButton.setEnabled(false);
					passwordInfo.setText(R.string.text_set_password_notice);
					focusPasswordField();
					break;
				}
				case UNLOCK_PASSWORD: {
					passwordInfo.setText(R.string.text_input_unlock_password);
					cancelButton.setVisibility(View.GONE);
					focusPasswordField();
					break;
				}
				default:
					throw new IllegalAccessError();

				}
			}
		});

	}

	@Override
	public void onClick(View v) {

		switch (interactiveState) {
		case SET_PASSWORD:
			onClickSetPasswordOk(v == okButton);
			break;
		case UNLOCK_PASSWORD:
			if (v == okButton) {
				// disable button to avoid re-click
				okButton.setEnabled(false);
				// control flow transformed to background thread
				checkPassword(passwordInput.getText().toString());
			}

			break;
		default:
			throw new IllegalAccessError("erro here");

		}
	}

	@Override
	public void onPasswordChangeFinished(boolean successful) {
		dismiss();
		handler.onPasswordProcessed(interactiveState, successful);
	}

	@Override
	public boolean checkFreeSpace(long arg0) {
		// TODO Auto-generated method stub
		return false;
	}

	private void focusPasswordField() {
		// http://stackoverflow.com/questions/8080579/android-textfield-set-focus-soft-input-programmatically

		passwordInput.setFocusableInTouchMode(true);
		passwordInput.requestFocus();

		final InputMethodManager inputMethodManager = (InputMethodManager) getActivity()
				.getSystemService(Context.INPUT_METHOD_SERVICE);
		inputMethodManager.showSoftInput(passwordInput,
				InputMethodManager.SHOW_IMPLICIT);
	}

	private void onClickSetPasswordOk(boolean clickOk) {
		if (!clickOk) {

			if (interactiveState != PasswordInteractiveState.SET_PASSWORD) {
				throw new IllegalAccessError("bad logic");
			}
			
			//user do not set a password
			
			setDataStorePassword(null);

			return;
		}

		String thisTimePassword = passwordInput.getText().toString();

		if (firstTimePassword != null) {

			// second time input password, don't allow cancel
			cancelButton.setVisibility(View.GONE);

			// disable pending click
			okButton.setEnabled(false);

			if (firstTimePassword.equals(thisTimePassword)) {
				// password match , do change password
				setDataStorePassword(passwordInput.getText().toString());
				return;
			} else {
				firstTimePassword = null;
				passwordInput.setText("");
				setInteractiveState(PasswordInteractiveState.SET_PASSWORD);
				// password mismatch, re-enter
				passwordInfo.setText(R.string.error_password_mismatch);
				return;
			}
		} else {

			// first time enter password

			firstTimePassword = passwordInput.getText().toString();

			passwordInfo.setText(R.string.text_confirm_password);
			passwordInput.setText("");
			cancelButton.setVisibility(View.GONE);
			focusPasswordField();
			return;
		}
	}

	private void setDataStorePassword(final String plainPassword) {
		// first ,we need to lock datastore and block any new connection

		getDialog().setTitle(R.string.text_setting_password);

		new Thread(new Runnable() {

			@Override
			public void run() {
				// changer
				PasswordChanger changer = new PasswordChanger(
						(SQLiteDataStore) UniqueObject.getInstance().getDataStore(),
						PasswordDialogFragment.this, plainPassword, 1000);
				changer.doChangePassword();

			}
		}).start();
	}

	private void wrongPassword() {

		// clear wrong password
		passwordInput.setText("");

		boolean destroyWhenPasswordWrong = NearByTalkApplication.getInstance()
				.getConfig().isDestroyWhenPasswordWrong();

		int maxTry = NearByTalkApplication.getInstance().getConfig()
				.getDestroyRetryMaxCount();

		++wrongPasswordRetryCounter;

		if (!destroyWhenPasswordWrong) {

			passwordInfo.setText(String.format(
					getString(R.string.error_wrong_password), ""));

			// TODO blink text
			return;
		}

		if (wrongPasswordRetryCounter >= maxTry) {
			// destroy whole datastore silence

			// workflow transfered to UI thread again
			new Thread(new Runnable() {
				@Override
				public void run() {
					destroyQuiet();
				}
			}).start();

			// reset counter
			wrongPasswordRetryCounter = 0;
			return;
		}

		// retry counter didin't overflow
		String retryNotice = String.format(
				getString(R.string.text_more_password_try),
				String.valueOf(maxTry - wrongPasswordRetryCounter));

		passwordInfo.setText(String.format(
				getString(R.string.error_wrong_password), retryNotice));

	}

	private int wrongPasswordRetryCounter = 0;

	private void checkPassword(final String plainPassword) {

		// open database may be slow, try to do it in background thread

		new Thread(new Runnable() {

			@Override
			public void run() {

				String rawPassword = DigestUtility
						.byteArrayToHexString(DigestUtility
								.oneTimeDigest(plainPassword));

				try {
					UniqueObject.getInstance().getDataStore().preCheck(rawPassword);
					// success

					// reset counter, this field only update by this background
					// thread,
					// read action only happen when this background thread
					// finished
					// so no need to protect by mutex
					wrongPasswordRetryCounter = 0;

					Global.getInstance().getPlatformAbstract()
							.commitPasswordChangeNew(rawPassword);

					getActivity().runOnUiThread(new Runnable() {
						@Override
						public void run() {
							// TODO Auto-generated method stub
							if (isResumed()) {

								// directly close dialog
								handler.onPasswordProcessed(PasswordInteractiveState.UNLOCK_PASSWORD, true);
								dismiss();
							} else {
								// password checking may finished when UI is
								// hidden.
								// didn't continue, force user to reenter
								// password
								// next-time
							}
						}
					});

				} catch (IncorrectPasswordException e) {
					// failed

					getActivity().runOnUiThread(new Runnable() {
						@Override
						public void run() {
							wrongPassword();
						}
					});

				} catch (NearByTalkException e) {
					log.error("try open datastore failed: ", e);

					handler.onPasswordProcessed(interactiveState, false);
					dismiss();
				}

			}
		}).start();
	}

	private void destroyQuiet() {

		try {
			FileUtils.deleteDirectory(new File(Global.getInstance()
					.getAppRootDirectory()));
		} catch (IOException e) {
			log.error("destroy failed :", e);
		}
		// quietly

		handler.onPasswordProcessed(interactiveState, false);

		dismiss();
	}
}
