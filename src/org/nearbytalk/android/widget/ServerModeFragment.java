package org.nearbytalk.android.widget;

import java.net.UnknownHostException;
import java.util.List;

import org.nearbytalk.HttpService;
import org.nearbytalk.R;
import org.nearbytalk.NearByTalkApplication;
import org.nearbytalk.android.IpTablesUtil;
import org.nearbytalk.android.MessageConstant;
import org.nearbytalk.android.RootFailedException;
import org.nearbytalk.android.widget.PasswordDialogFragment.PasswordInteractiveState;
import org.nearbytalk.android.widget.PasswordDialogFragment.PasswordProcessHandler;
import org.nearbytalk.android.widget.ResourceProcessDialogFragment.ResourceProcessedCallback;
import org.nearbytalk.android.widget.WifiProcessDialogFragment.NameAndIp;
import org.nearbytalk.dummydns.DummyDNS;
import org.nearbytalk.runtime.Global;
import org.nearbytalk.runtime.Global.HttpServerInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class ServerModeFragment extends Fragment implements OnClickListener,
		RootNoticeDialogFragment.RootCheckCallback, ResourceProcessedCallback,
		WifiProcessDialogFragment.InterfaceChoosedHandler,
		PasswordProcessHandler {

	private Button buttonToggleService;

	private Button buttonChangePassword;

	private Button buttonOpen;

	private Button buttonReinit;
	
	private Button buttonCustomSSID;

	private Button buttonAcquireRootPermission;

	private Button changePasswordButton;

	private TextView textViewServerState;

	private TextView textViewIptablesState;

	private Logger log = LoggerFactory.getLogger(ServerModeFragment.class);

	private DummyDNS dnsServer;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		View view = inflater
				.inflate(R.layout.fragment_server, container, false);

		buttonToggleService = (Button) view
				.findViewById(R.id.buttonToggleService);

		buttonOpen = (Button) view.findViewById(R.id.buttonOpen);

		buttonOpen.setEnabled(false);
		
		buttonCustomSSID = (Button) view.findViewById(R.id.buttonCustomSSID);
		
		buttonCustomSSID.setOnClickListener(this);

		buttonChangePassword = (Button) view
				.findViewById(R.id.buttonChangePassword);

		buttonChangePassword.setEnabled(true);

		buttonChangePassword.setOnClickListener(this);

		textViewServerState = (TextView) view
				.findViewById(R.id.textServerState);

		textViewIptablesState = (TextView) view
				.findViewById(R.id.textIptablesState);

		buttonToggleService.setOnClickListener(this);
		buttonOpen.setOnClickListener(this);
		// only enable after start
		buttonToggleService.setEnabled(false);

		buttonReinit = (Button) view.findViewById(R.id.buttonReinit);

		buttonReinit.setOnClickListener(this);

		buttonAcquireRootPermission = (Button) view
				.findViewById(R.id.buttonAcquireRoot);

		changePasswordButton = (Button) view
				.findViewById(R.id.buttonChangePassword);

		buttonAcquireRootPermission.setOnClickListener(this);

		changePasswordButton.setOnClickListener(this);

		return view;
	}

	IpTablesUtil ipTablesUtil = new IpTablesUtil();

	private enum IptablesFailReason {
		NO_ROOT, NO_IPTABLES, OTHER
	};

	private void updateIpTablesState(final int what) {
		updateIpTablesState(what, null);
	}

	private void updateIpTablesState(final int what,
			final IptablesFailReason reason) {

		textViewIptablesState.post(new Runnable() {

			@Override
			public void run() {

				switch (what) {
				case MessageConstant.ServiceState.STOPPED:
					textViewIptablesState.setText(String.format(
							getString(R.string.text_iptables_state),
							getString(R.string.text_stopped)));
					break;
				case MessageConstant.ServiceState.STOPPING:
					textViewIptablesState.setText(String.format(
							getString(R.string.text_iptables_state),
							getString(R.string.text_stopping)));
					break;
				case MessageConstant.ServiceState.STARTING:
					textViewIptablesState.setText(String.format(
							getString(R.string.text_iptables_state),
							getString(R.string.text_starting)));
					break;
				case MessageConstant.ServiceState.STARTED:
					textViewIptablesState.setText(String.format(
							getString(R.string.text_iptables_state),
							getString(R.string.text_started)));
					break;
				case MessageConstant.ServiceState.FAILURE: {
					switch (reason) {
					case NO_ROOT:
						textViewIptablesState.setText(String.format(
								getString(R.string.text_iptables_state),
								getString(R.string.text_no_root)));
						break;
					case NO_IPTABLES:
						textViewIptablesState.setText(String.format(
								getString(R.string.text_iptables_state),
								getString(R.string.error_no_iptables)));
						break;
					case OTHER:
						textViewIptablesState.setText(String.format(
								getString(R.string.text_iptables_state),
								getString(R.string.error_start_failure)));
						break;
					default: {
						// not possible
						throw new IllegalAccessError();
					}
					}
					break;
				}

				default: {
					throw new IllegalAccessError();
				}
				}
			}
		});

	}

	/**
	 * async call
	 * 
	 * @param start
	 */
	private void iptablePortfoward(final boolean start) {

		new Thread() {

			@Override
			public void run() {
				if (!start) {
					try {
						updateIpTablesState(MessageConstant.ServiceState.STOPPING);
						ipTablesUtil.stopDNSAndHttpForward();
						updateIpTablesState(MessageConstant.ServiceState.STOPPED);
					} catch (RootFailedException e) {
						updateIpTablesState(
								MessageConstant.ServiceState.FAILURE,
								IptablesFailReason.NO_ROOT);
					}
					return;
				}

				Global global = Global.getInstance();
				try {
					updateIpTablesState(MessageConstant.ServiceState.STARTING);
					ipTablesUtil.startDNSAndHttpForward(global.hostIp,
							global.dnsInfo.listenPort,
							HttpServerInfo.listenPort);
					updateIpTablesState(MessageConstant.ServiceState.STARTED);
				} catch (RootFailedException e) {
					updateIpTablesState(MessageConstant.ServiceState.FAILURE,
							IptablesFailReason.NO_ROOT);
				}
			}

		}.start();

	}

	public static String intToIp(int i) {
		return ((i >> 24) & 0xFF) + "." + ((i >> 16) & 0xFF) + "."
				+ ((i >> 8) & 0xFF) + "." + (i & 0xFF);
	}

	private synchronized void recreateDNS() {
		dnsServer = new DummyDNS();
		try {
			dnsServer.setHostIp(Global.getInstance().hostIp);
		} catch (UnknownHostException e) {
			// impossible
		}
		dnsServer.setListenPort(Global.DNSInfo.DEFAULT_LISTEN_PORT);
	}

	/**
	 * start wifi ap first,then HTTP service execute in UI thread.
	 * 
	 */

	@Override
	public void onClick(View view) {

		if (view == buttonToggleService) {

			if (HttpService.getState() == MessageConstant.ServiceState.STOPPED
					|| HttpService.getState() == MessageConstant.ServiceState.FAILURE) {

				// if start pushed ,disable toggle button
				setButtonToggleService(R.string.text_starting, false);

				// update ui earlier than Messenger callback
				updateJavaServerStateTextView(MessageConstant.ServiceState.STARTING);

				WifiProcessDialogFragment.popIfNecesseryForServer(this,
						getActivity(), this);

				return;
			} else if (HttpService.getState() == MessageConstant.ServiceState.STARTED) {

				// disable push
				setButtonToggleService(R.string.text_stopping, false);

				// update ui early
				updateJavaServerStateTextView(MessageConstant.ServiceState.STOPPING);

				stopService();
				return;
			}
		} else if (view == buttonOpen) {
			openServerUrl();
			return;
		} else if (view == buttonAcquireRootPermission) {
			RootNoticeDialogFragment.popIfNecessary(this, this, true);
			return;
		} else if (view == buttonReinit) {

			ResourceProcessDialogFragment.popIfNecessary(this, this, true);
		} else if (view == changePasswordButton) {

			PasswordDialogFragment.setPassword(getFragmentManager(), this);
		} else if (view == buttonCustomSSID){
			CustomSsidDialogFragment.pop(getFragmentManager(),this);
		}
	}

	private void stopService() {
		Intent intent = new Intent(getActivity(), HttpService.class);

		intent.putExtra(MessageConstant.COMMAND_KEY,
				MessageConstant.Command.STOP_SERVER);
		intent.putExtra(MessageConstant.MESSENGER_KEY, messenger);

		if (dnsServer != null && dnsServer.isAlive()) {

			dnsServer.setStopping(true);
		}

		// send stop command to service
		getActivity().startService(intent);

	}

	private void openServerUrl() {
		String url = "http://" + Global.getInstance().hostIp + ":"
				+ Global.HttpServerInfo.listenPort;
		Intent i = new Intent(Intent.ACTION_VIEW);
		i.setData(Uri.parse(url));
		startActivity(i);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		serverStateHandler = new ServerStateHandler(getFragmentManager());
		// TODO move some running state to savedInstanceState

		messenger = new Messenger(serverStateHandler);
	}

	@Override
	public void onStart() {
		super.onStart();

		if (inBackgroundToast != null) {
			// close toast if any
			inBackgroundToast.cancel();
		}

		// for security, delay service information update until
		// password unlocked

		String hiddenString = "---";

		textViewServerState.setText(getString(R.string.text_server_state,
				hiddenString, hiddenString, hiddenString));
		textViewIptablesState.setText(getString(R.string.text_iptables_state,
				hiddenString));

		if (HttpService.getState() == MessageConstant.ServiceState.STARTED) {

			// ui maybe killed with background running
			// do not check root
			// but should check password
			PasswordDialogFragment.unlockPassword(getFragmentManager(), this);
		} else {
			// server not run, may be first time enter, or resumed with server
			// not running
			// must ask permission again , to assume everytime root command OK
			// workflow translate to onRootCheckFinish (will through
			// unlock_password)
			RootNoticeDialogFragment.popIfNecessary(this, this, false);
		}


	}

	/**
	 * none-block
	 * 
	 */

	/**
	 * handle messages from service ,callback UI
	 * 
	 */
	private Handler serverStateHandler;

	/**
	 * http://stackoverflow.com/questions/11407943/this-handler-class-should-be-
	 * static-or-leaks-might-occur-incominghandler
	 */
	static class ServerStateHandler extends Handler {

		private FragmentManager fm;

		public ServerStateHandler(FragmentManager fm) {
			this.fm = fm;
		}

		@Override
		public void handleMessage(Message message) {

			ServerModeFragment fakeThis = (ServerModeFragment) fm
					.findFragmentById(R.id.fragment_server_mode);

			if (fakeThis == null) {
				return;
			}

			fakeThis.onServiceStateChange(message.what);
		}
	}

	/**
	 * set ToggleService button text to resId,enabled as enabled
	 * 
	 * @param resId
	 * @param enable
	 */
	private void setButtonToggleService(int resId, boolean enable) {
		buttonToggleService.setText(resId);
		buttonToggleService.setEnabled(enable);
	}

	private void updateJavaServerStateTextView(final int javaServerWhat) {

		// TODO need check if button state should update , since Fragment start
		// we may disable any button until all dialog poped

		switch (javaServerWhat) {
		case MessageConstant.ServiceState.STARTED: {
			textViewServerState.setText(String.format(
					getString(R.string.text_server_state), String.format(
							getString(R.string.text_running_on),
							String.valueOf(HttpServerInfo.listenPort)),
					String.format(getString(R.string.text_running_on), String
							.valueOf(Global.getInstance().dnsInfo.listenPort)),
					String.valueOf(Global.getInstance().hostIp)));

			setButtonToggleService(R.string.button_stop_text, true);

			textViewServerState.setTextColor(getResources().getColor(
					R.color.color_server_started));
			break;
		}
		case MessageConstant.ServiceState.STARTING: {
			textViewServerState.setText(String.format(
					getString(R.string.text_server_state),
					getString(R.string.text_starting),
					getString(R.string.text_starting),
					getString(R.string.text_starting)));

			setButtonToggleService(R.string.text_starting, false);

			textViewServerState.setTextColor(getResources().getColor(
					R.color.color_server_starting));
			break;
		}

		case MessageConstant.ServiceState.FAILURE: {
			textViewServerState.setText(String.format(
					getString(R.string.text_server_state),
					getString(R.string.error_start_failure),
					getString(R.string.error_start_failure),
					getString(R.string.error_start_failure)));

			setButtonToggleService(R.string.button_start_text, true);

			textViewServerState.setTextColor(getResources().getColor(
					R.color.color_server_failure));

			break;
		}
		case MessageConstant.ServiceState.STOPPED: {
			textViewServerState.setText(String.format(
					getString(R.string.text_server_state),
					getString(R.string.text_stopped),
					getString(R.string.text_stopped),
					getString(R.string.text_stopped)));

			setButtonToggleService(R.string.button_start_text, true);

			textViewServerState.setTextColor(getResources().getColor(
					R.color.color_server_stopped));

			break;
		}
		case MessageConstant.ServiceState.STOPPING: {
			textViewServerState.setText(String.format(
					getString(R.string.text_server_state),
					getString(R.string.text_stopping),
					getString(R.string.text_stopping),
					getString(R.string.text_stopping)));

			setButtonToggleService(R.string.text_stopping, false);

			textViewServerState.setTextColor(getResources().getColor(
					R.color.color_server_stopping));

			break;
		}
		default: {
			throw new IllegalAccessError();
			// bad logic
		}
		}

	}

	/**
	 * switch ToggleServerButton by state, which is defined as value passed
	 * between service and activity
	 * 
	 * @param what
	 */
	private void onServiceStateChange(int what) {

		updateJavaServerStateTextView(what);

		switch (what) {
		case MessageConstant.ServiceState.STARTED: {

			setButtonToggleService(R.string.button_stop_text, true);
			buttonOpen.setEnabled(true);

			log.info("http server start ok");

			if (Global.getInstance().dnsInfo.isRunning) {
				if (NearByTalkApplication.getInstance().getConfig()
						.isRootAcquired()) {
					iptablePortfoward(true);
				} else {
					updateIpTablesState(MessageConstant.ServiceState.FAILURE,
							IptablesFailReason.NO_ROOT);
				}
			} else {
				// TODO this check does not consider thread ,DNS server may be
				// ready after a while
				log.error("DNS not running");
				// dns server start failed,
				// 53 port in use,but no root permission,can not do port forward
			}

			break;
		}
		case MessageConstant.ServiceState.STARTING: {

			log.info("server is starting...");
			// TODO if in starting state,will button stop enabled?
			setButtonToggleService(R.string.text_starting, false);
			buttonReinit.setEnabled(false);
			buttonAcquireRootPermission.setEnabled(false);
			buttonChangePassword.setEnabled(false);
			break;
		}
		case MessageConstant.ServiceState.STOPPED: {
			setButtonToggleService(R.string.button_start_text, true);
			buttonOpen.setEnabled(false);
			buttonReinit.setEnabled(true);
			buttonAcquireRootPermission.setEnabled(true);
			buttonChangePassword.setEnabled(true);
			break;
		}
		case MessageConstant.ServiceState.STOPPING: {

			// didn't check root permission first, maybe its changed before try
			iptablePortfoward(false);

			setButtonToggleService(R.string.text_stopping, false);
			buttonOpen.setEnabled(false);
			break;
		}
		case MessageConstant.ServiceState.FAILURE: {

			Toast.makeText(getActivity(), R.string.error_start_failure,
					Toast.LENGTH_LONG).show();
			setButtonToggleService(R.string.button_start_text, true);
			buttonOpen.setEnabled(false);
			buttonChangePassword.setEnabled(true);
			break;
		}

		}
	}

	private Messenger messenger;

	@Override
	public void onInterfaceChoosed(List<NameAndIp> selected) {

		if (selected == null) {
			// user didn't want to start server
			// but ui state already updated by click
			// must restore stopped state
			updateJavaServerStateTextView(MessageConstant.ServiceState.STOPPED);
			return;
		}

		if (selected.isEmpty()) {

			// no avaible interface found
			textViewServerState.setText(String.format(
					getString(R.string.text_server_state),
					getString(R.string.error_interface_not_found),
					getString(R.string.error_interface_not_found),
					getString(R.string.error_interface_not_found)));

			textViewIptablesState.setText(String.format(
					getString(R.string.text_iptables_state),
					getString(R.string.error_interface_not_found)));

			setButtonToggleService(R.string.button_start_text, true);

			return;
		}

		onServiceStateChange(MessageConstant.ServiceState.STARTING);

		// TODO backend currently do not support multi interface

		Global.getInstance().hostIp = selected.get(0).ipAddress;

		final Intent intent = new Intent(getActivity(), HttpService.class);

		intent.putExtra(MessageConstant.COMMAND_KEY,
				MessageConstant.Command.START_SERVER);
		intent.putExtra(MessageConstant.MESSENGER_KEY, messenger);

		recreateDNS();

		dnsServer.start();

		getActivity().startService(intent);

	}

	// this code always runs on UI thread, so no need to protect
	private static Toast inBackgroundToast;

	@Override
	public void onPause() {

		if (Global.getInstance().httpServerInfo.isRunning) {

			if (inBackgroundToast == null) {
				inBackgroundToast = Toast.makeText(getActivity(),
						R.string.text_server_running_in_background,
						Toast.LENGTH_LONG);
			}

			inBackgroundToast.show();
		}

		super.onPause();

	}

	@Override
	public void onRootCheckFinish(boolean hasRoot) {

		log.info("root check finished: {}", hasRoot);
		// TODO
		// actually ,these check only valid in first time in
		// we need to check database connection works between session
		NearByTalkApplication application = (NearByTalkApplication) getActivity()
				.getApplication();

		buttonAcquireRootPermission.setVisibility(hasRoot ? View.GONE
				: View.VISIBLE);

		// root check ok

		ResourceProcessDialogFragment.popIfNecessary(this, this, false);

	}

	@Override
	public void onResourceProcessFinish(boolean successful) {

		if (successful) {
			int serverState = HttpService.getState();
			onServiceStateChange(serverState);
			updateIpTablesState(ipTablesUtil.getState());

		} else {
			// back to main
			getFragmentManager().popBackStackImmediate();
		}
	}

	@Override
	public void onPasswordProcessed(PasswordInteractiveState state,
			boolean success) {

		if (state == PasswordInteractiveState.UNLOCK_PASSWORD) {
			
			if (!success) {
				//unlock password failed (only destroyWhenWrongPassword can trigger this)
				
				//force re-init all resources
				ResourceProcessDialogFragment.popIfNecessary(this, this, true);
				return;
			}

			// password unlocked, ok
			//update ui state
			onServiceStateChange(HttpService.getState());
			updateIpTablesState(ipTablesUtil.getState());
			
		} else {
			if (state != PasswordInteractiveState.SET_PASSWORD) {
				throw new IllegalAccessError();
			}
			
			//set password failed, do nothing				
			//set password success, do nothing
			
		}

	}

}
