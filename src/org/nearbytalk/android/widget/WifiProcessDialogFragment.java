package org.nearbytalk.android.widget;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.nearbytalk.R;
import org.nearbytalk.NearByTalkApplication;
import org.nearbytalk.android.AndroidConfig;
import org.nearbytalk.android.widget.NameAndIpAdapter.SelectionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.app.Activity;
import android.content.Intent;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;

import com.whitebyte.wifihotspotutils.WIFI_AP_STATE;
import com.whitebyte.wifihotspotutils.WifiApManager;

public class WifiProcessDialogFragment extends DialogFragment implements
		OnClickListener, SelectionHandler {

	public WifiProcessDialogFragment() {
		super();
		setCancelable(false);
	}

	private static Logger log = LoggerFactory
			.getLogger(WifiProcessDialogFragment.class);

	public static final String FRAGMENT_TAG = WifiProcessDialogFragment.class
			.getName();

	public static interface InterfaceChoosedHandler {
		/**
		 * @param nameAndIp
		 *            null means user didn't want to continue, empty means no
		 *            avaible
		 */
		public void onInterfaceChoosed(List<NameAndIp> nameAndIp);
	}

	private static enum WifiInteractiveState {
		NOTICE_WIFI_DISCONNECT, CREATE_WIFI_HOTSPOT, SELECT_NET_INTERFACE, WIFI_HOTSPOT_NOT_STARTED
	}

	private WifiInteractiveState interactiveState = WifiInteractiveState.NOTICE_WIFI_DISCONNECT;

	Button okButton;
	Button cancelButton;

	private View okCancelButtonLayout;

	private InterfaceChoosedHandler interfaceChoosedHandler;

	public static void popIfNecesseryForServer(Fragment prevFragment,
			Activity activity, InterfaceChoosedHandler handler) {

		if (getWifiApManager(activity).isWifiApEnabled()) {
			// already has wifi ap, skip create ap step

			popIfNecersseryForInterface(prevFragment.getFragmentManager(),
					null, handler);
			return;
		}

		WifiManager wifiManager = NearByTalkApplication.getInstance().getWifiManager();

		AndroidConfig config = NearByTalkApplication.getInstance().getConfig();

		if (!wifiManager.isWifiEnabled()
				|| !config.isShowWifiDisconnectNoticeAgain()) {

			popupAs(prevFragment.getFragmentManager(),
					WifiInteractiveState.CREATE_WIFI_HOTSPOT, null, handler);
			return;
		}

		popupAs(prevFragment.getFragmentManager(),
				WifiInteractiveState.NOTICE_WIFI_DISCONNECT, null, handler);

	}

	public static class NameAndIp {
		public NameAndIp(String name, String ipAddress) {
			this.name = name;
			this.ipAddress = ipAddress;

			if (name.contains("cmwap") || name.contains("cnet")
					|| name.contains("cmnet")) {
				name = name
						+ NearByTalkApplication.getInstance().getString(
								R.string.text_may_not_wifi_ap);

			}
		}

		public String name;
		public String ipAddress;

		@Override
		public String toString() {
			return name + ":" + ipAddress;
		}

	}

	private List<NameAndIp> nameAndIps;
	private NameAndIpAdapter arrayAdapter;

	private Map<WifiInteractiveState, View> stateMapView = new HashMap<WifiProcessDialogFragment.WifiInteractiveState, View>();

	private Button continueWithoutHotspotButton;
	private Button gotoWifiSettingsButton;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.dialog_wifi_process, container);

		okButton = (Button) view.findViewById(R.id.buttonOk);
		cancelButton = (Button) view.findViewById(R.id.buttonCancel);

		okButton.setOnClickListener(this);
		cancelButton.setOnClickListener(this);

		okCancelButtonLayout = view.findViewById(R.id.layoutOkCancelButtons);

		continueWithoutHotspotButton = (Button) view
				.findViewById(R.id.buttonContinueWithoutWifiHotspot);

		continueWithoutHotspotButton.setOnClickListener(this);

		gotoWifiSettingsButton = (Button) view
				.findViewById(R.id.buttonGotoWifiSettings);

		gotoWifiSettingsButton.setOnClickListener(this);

		nameAndIpListView = (ListView) view
				.findViewById(R.id.listViewNameAndIp);

		stateMapView.put(WifiInteractiveState.NOTICE_WIFI_DISCONNECT,
				view.findViewById(R.id.layoutWifiDisconnectNotice));

		stateMapView.put(WifiInteractiveState.SELECT_NET_INTERFACE,
				view.findViewById(R.id.layoutSelectInterface));

		stateMapView.put(WifiInteractiveState.CREATE_WIFI_HOTSPOT,
				view.findViewById(R.id.layoutCreateWifiAp));

		stateMapView.put(WifiInteractiveState.WIFI_HOTSPOT_NOT_STARTED,
				view.findViewById(R.id.layoutWifiHotspotNotStarted));

		arrayAdapter = new NameAndIpAdapter(getActivity(),
				R.id.listViewNameAndIp, this);

		return view;
	}

	private void setInteractiveState(final WifiInteractiveState nextState) {

	
		getView().post(new Runnable() {

			@Override
			public void run() {

				interactiveState = nextState;

				if(nextState!=WifiInteractiveState.SELECT_NET_INTERFACE){
					//when select net interface , UI may just dismiss
					//if only one or none is avaiable, so update UI after decide that
					updateUiState();
				}

				switch (nextState) {
				case SELECT_NET_INTERFACE: {

					selectNetInterface();
					return;
				}
				case CREATE_WIFI_HOTSPOT: {
					createWifiAp();
					break;
				}
				case WIFI_HOTSPOT_NOT_STARTED:{
					enableWifi();
					break;
				}

				default:
					break;
				}

			}
		});

	}

	protected void enableWifi() {
		
		WifiManager wifiManager=NearByTalkApplication.getInstance().getWifiManager();
		
		//normally ,this will connect to avaiable wifi 
		wifiManager.setWifiEnabled(true);
		
		
	}

	protected void selectNetInterface() {

		if (nameAndIps == null) {
			// we may resume from paused
			nameAndIps = getNameAndIps();
		}

		//no need to popup list
		if (nameAndIps.size() <= 1) {
			interfaceChoosedHandler.onInterfaceChoosed(nameAndIps);
			dismiss();
			return;
		}

		//we delay select interface UI update after we actually want to display list view
		//so must update UI here
		updateUiState();		
		
		//need user interactive
		
		arrayAdapter.clear();

		for (NameAndIp nameAndIp : nameAndIps) {
			arrayAdapter.add(nameAndIp);
		}

		arrayAdapter.notifyDataSetChanged();
	}

	private static final int CALL_WIFI_SETTINGS_REQUEST_CODE = 112;

	@Override
	public void onClick(View v) {

		switch (interactiveState) {
		case NOTICE_WIFI_DISCONNECT: {
			if (v == okButton) {
				// only possible with for server

				WifiManager wifiManager=NearByTalkApplication.getInstance().getWifiManager();
				
				if (wifiManager.isWifiEnabled()) {
					//recheck
					wifiManager.disconnect();
				}

				setInteractiveState(WifiInteractiveState.CREATE_WIFI_HOTSPOT);
				return;
			} else if (v == cancelButton) {

				interfaceChoosedHandler.onInterfaceChoosed(null);
				dismiss();
				return;
			}

		}
		case SELECT_NET_INTERFACE: {
			if (v == okButton) {
				interfaceChoosedHandler.onInterfaceChoosed(arrayAdapter
						.getSelected());
				dismiss();
				return;
			} else if (v == cancelButton) {
				interfaceChoosedHandler.onInterfaceChoosed(null);
				dismiss();
				return;
			}
			break;
		}
		case WIFI_HOTSPOT_NOT_STARTED: {

			if (v == continueWithoutHotspotButton) {

				setInteractiveState(WifiInteractiveState.SELECT_NET_INTERFACE);
				return;

			} else if (v == gotoWifiSettingsButton) {
				// http://stackoverflow.com/questions/2318310/how-can-i-call-wi-fi-settings-screen-from-my-application-using-android
				startActivityForResult(new Intent(Settings.ACTION_SETTINGS),
						CALL_WIFI_SETTINGS_REQUEST_CODE);
				return;
			}
		}

		default:
			// should never happen here
			throw new IllegalAccessError();
		}

	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {

		super.onActivityResult(requestCode, resultCode, data);

		if (requestCode == CALL_WIFI_SETTINGS_REQUEST_CODE) {
			// calling from wifi settings
			if (getWifiApManager(getActivity()).isWifiApEnabled()) {
				// successful
				setInteractiveState(WifiInteractiveState.SELECT_NET_INTERFACE);
				return;
			} else {
				setInteractiveState(WifiInteractiveState.WIFI_HOTSPOT_NOT_STARTED);
				return;
			}
		}

	}

	/**
	 * 
	 * open wifi ap ,blocking
	 * 
	 */

	private ListView nameAndIpListView;

	public static List<NameAndIp> getNameAndIps() {

		List<NameAndIp> ret = new ArrayList<NameAndIp>();
		try {

			for (Enumeration<NetworkInterface> en = NetworkInterface
					.getNetworkInterfaces(); en.hasMoreElements();) {
				NetworkInterface intf = en.nextElement();

				NameAndIp information = convertToInformation(intf);
				if (information != null) {
					ret.add(information);
				}
			}

		} catch (SocketException ex) {

			log.error("can not get ip address : {}", ex);

		}
		return ret;
	}

	private static NameAndIp convertToInformation(
			NetworkInterface networkInterface) {

		for (Enumeration<InetAddress> enumIpAddr = networkInterface
				.getInetAddresses(); enumIpAddr.hasMoreElements();) {
			InetAddress inetAddress = enumIpAddr.nextElement();
			if (!inetAddress.isLoopbackAddress()
					&& (inetAddress.getAddress().length == 4)) {
				log.debug("found {}", inetAddress.getHostAddress());
				return new NameAndIp(networkInterface.getName(),
						inetAddress.getHostAddress());
			}
		}

		return null;
	}

	@Override
	public void onPause() {
		super.onPause();
		nameAndIps = null;
	}

	private void updateUiState() {

		for (WifiInteractiveState state : stateMapView.keySet()) {
			stateMapView.get(state).setVisibility(
					state == interactiveState ? View.VISIBLE : View.GONE);
		}

		if (interactiveState == WifiInteractiveState.NOTICE_WIFI_DISCONNECT
				|| interactiveState == WifiInteractiveState.SELECT_NET_INTERFACE) {
			okCancelButtonLayout.setVisibility(View.VISIBLE);
		} else {
			okCancelButtonLayout.setVisibility(View.GONE);
		}
	}

	@Override
	public void onStart() {
		super.onStart();

		updateUiState();

		setInteractiveState(interactiveState);

	}

	public static void popIfNecersseryForInterface(FragmentManager fm,
			WifiProcessDialogFragment fakeThis,
			InterfaceChoosedHandler interfaceChoosedHandler) {

		List<NameAndIp> allNameAndIps = getNameAndIps();

		Map<String, NameAndIp> nameAndIpMap = new HashMap<String, NameAndIp>();

		for (NameAndIp nameAndIp : allNameAndIps) {
			nameAndIpMap.put(nameAndIp.name, nameAndIp);
		}

		AndroidConfig config = NearByTalkApplication.getInstance().getConfig();

		Set<String> rememberSelectInterface = config
				.getDefaultSelectInterfaces();

		// check if we remember last select interface

		if (config.isRememberDefaultSelectInterface()) {

			if (nameAndIpMap.keySet().containsAll(rememberSelectInterface)) {
				List<NameAndIp> toUse = new ArrayList<NameAndIp>();
				for (String string : rememberSelectInterface) {
					toUse.add(nameAndIpMap.get(string));
				}
				interfaceChoosedHandler.onInterfaceChoosed(toUse);

				if (fakeThis != null) {
					fakeThis.dismiss();
				}
				return;

			} else {
				// runtime interfaces didn't match remembered
				config.clearRememberDefaultSelectInterfaceFlag();
			}
		}

		if ((allNameAndIps.size() == 1)||allNameAndIps.isEmpty()) {
			//only one avaible ,or none avaible

			// no need to popup
			interfaceChoosedHandler.onInterfaceChoosed(allNameAndIps);

			if (fakeThis != null) {
				fakeThis.dismiss();
			}

			return;
		}

		// has multi selection ,need user interactive
		if (fakeThis == null) {
			popupAs(fm, WifiInteractiveState.SELECT_NET_INTERFACE,
					allNameAndIps, interfaceChoosedHandler);

		} else {
			fakeThis.nameAndIps = allNameAndIps;
			fakeThis.setInteractiveState(WifiInteractiveState.SELECT_NET_INTERFACE);
		}
	}

	static private void popupAs(FragmentManager fm,
			WifiInteractiveState wifiInteractiveState,
			List<NameAndIp> nameAndIps, InterfaceChoosedHandler handler) {
		// wifi connected or user havn't see wifi disconnect notice

		WifiProcessDialogFragment frag = new WifiProcessDialogFragment();

		FragmentTransaction ft = fm.beginTransaction();

		Fragment prevWifiDisconnectNoticeFrag = fm
				.findFragmentByTag(FRAGMENT_TAG);

		if (prevWifiDisconnectNoticeFrag != null) {
			ft.remove(prevWifiDisconnectNoticeFrag);
		}
		ft.addToBackStack(null);

		WifiProcessDialogFragment thisFragment = new WifiProcessDialogFragment();

		thisFragment.interfaceChoosedHandler = handler;

		thisFragment.nameAndIps = nameAndIps;

		thisFragment.interactiveState = wifiInteractiveState;

		thisFragment.show(ft, FRAGMENT_TAG);
	}

	static public void blockingWaitWifiAp(final FragmentManager fm,
			final WifiProcessDialogFragment fakeThis, final Activity activiy,
			final InterfaceChoosedHandler handler) {
		for (int i = 0; i < 5; ++i) {

			if (getWifiApManager(activiy).getWifiApState() == WIFI_AP_STATE.WIFI_AP_STATE_ENABLED) {
				break;
			}

			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				log.error("thread interrupted");
			}
		}

		activiy.runOnUiThread(new Runnable() {
			@Override
			public void run() {

				if (getWifiApManager(activiy).getWifiApState() != WIFI_AP_STATE.WIFI_AP_STATE_ENABLED) {
					if (fakeThis != null) {
						fakeThis.setInteractiveState(WifiInteractiveState.WIFI_HOTSPOT_NOT_STARTED);
					} else {
						popupAs(fm,
								WifiInteractiveState.WIFI_HOTSPOT_NOT_STARTED,
								null, handler);
					}
				} else {
					popIfNecersseryForInterface(fm, fakeThis, handler);
				}
			}
		});
	}

	private static WifiApManager getWifiApManager(Activity activity) {
		synchronized (wifiApManagerLock) {
			if (swifiApManager == null) {
				swifiApManager = new WifiApManager();
			}

			return swifiApManager;
		}
	}

	private static String SSID_FORMAT_STRING = "freewifi_%s";

	private static Object wifiApManagerLock = new Object();

	private static WifiApManager swifiApManager;

	private static Random random = new Random(Calendar.getInstance()
			.getTimeInMillis());

	private static int VALUE_A = Character.valueOf('a').charValue();

	private static String randomString() {

		char[] ret = new char[3];

		for (int i = 0; i < ret.length; ++i) {
			ret[i] = Character.valueOf((char) (VALUE_A + random.nextInt(26)));
		}

		return new String(ret);
	}

	public void createWifiAp() {

		WifiApManager wifiApManager = getWifiApManager(getActivity());

		// if wifi ap is opened, not change anything.
		// TODO maybe ask user to determine ?
		if (!wifiApManager.isWifiApEnabled()) {
			// TODO if not connect ,create AP

			WifiConfiguration configuration = new WifiConfiguration();

			String randomSSID = String.format(SSID_FORMAT_STRING,
					randomString());
			String customSSID = NearByTalkApplication.getInstance().getConfig().getCustomSsid();
			configuration.SSID = (customSSID==null?randomSSID:customSSID);

			// always create open network
			configuration.allowedAuthAlgorithms
					.set(WifiConfiguration.AuthAlgorithm.OPEN);

			log.info("ap not enabled, try to start ");

			wifiApManager.setWifiApEnabled(configuration, true);

		} else {
			log.info("wifi AP already started");
		}

		new Thread() {
			@Override
			public void run() {
				blockingWaitWifiAp(getFragmentManager(),
						WifiProcessDialogFragment.this, getActivity(), interfaceChoosedHandler);
			}
		}.start();
	}

	@Override
	public void onHasSelections() {

		if (interactiveState != WifiInteractiveState.SELECT_NET_INTERFACE) {
			throw new IllegalAccessError();
		}
		okButton.setEnabled(true);
	}

	@Override
	public void onNoneSelection() {

		if (interactiveState!=WifiInteractiveState.SELECT_NET_INTERFACE) {
			throw new IllegalAccessError();
		}
		okButton.setEnabled(false);
	}
}
