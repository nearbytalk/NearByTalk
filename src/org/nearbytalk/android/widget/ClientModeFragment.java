package org.nearbytalk.android.widget;

import java.math.BigInteger;
import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.util.ArrayList;

import org.nearbytalk.R;
import org.nearbytalk.NearByTalkApplication;
import org.nearbytalk.android.ServerState;
import org.nearbytalk.android.ServerState.ScanState;
import org.nearbytalk.runtime.Global;
import org.nearbytalk.util.ServerDiscover;
import org.nearbytalk.util.ServerDiscover.ScanCallback;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.wvr.widget.TextProgressBar;

public class ClientModeFragment extends Fragment implements ScanCallback,
		OnItemClickListener {

	private ListView listViewServer;

	private TextProgressBar progressBarScanState;
	
	private boolean hasResult=false;
	
	private void createDialog(){
		noServerFoundDialog = new AlertDialog.Builder(getActivity())
				.setMessage(R.string.text_no_server_avaiable)
				.setPositiveButton("change wifi",
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								startActivity(new Intent(
										Settings.ACTION_WIFI_SETTINGS));
								dialog.dismiss();
							}
						})
				.setNegativeButton("rescan",
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int which) {

								scanServer();

								dialog.dismiss();
							}
						}).create();
		
		wifiDisabledDialog = new AlertDialog.Builder(getActivity())
				.setTitle("Alert Dialog")
				.setMessage("Wifi disabled, connected?")
				.setNegativeButton(android.R.string.cancel,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								getActivity().finish();
								dialog.dismiss();
							}
						})
				.setPositiveButton(android.R.string.ok,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int which) {
								startActivity(new Intent(
										Settings.ACTION_WIFI_SETTINGS));
								dialog.dismiss();
							}
						}).create();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		// TODO Auto-generated method stub

		View ret = inflater.inflate(R.layout.fragment_client, container, false);

		listViewServer = (ListView) ret.findViewById(R.id.list_view_server);

		progressBarScanState = (TextProgressBar) ret
				.findViewById(R.id.progress_bar_scan_state);

		createDialog();

		progressBarScanState.setProgress(0);
		
		IntentFilter filter=new IntentFilter();
		
		filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
		filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
		
		//TODO getActivity().registerReceiver(wifiStateChangeReceiver, );

		return ret;
	}

	ServerDiscover serverDiscover;

	final int SCAN_ORDER[] = { ConnectivityManager.TYPE_WIFI,
			ConnectivityManager.TYPE_ETHERNET };

	private ArrayAdapter<ServerState> adapter;

	private AlertDialog noServerFoundDialog;

	private AlertDialog wifiDisabledDialog;

	private void scanServer() {
		WifiManager wifiManager = NearByTalkApplication.getInstance().getWifiManager();

		if (!wifiManager.isWifiEnabled()) {
			wifiDisabledDialog.show();
			return;
		}

		WifiInfo wifiInfo = wifiManager.getConnectionInfo();

		int ip = wifiInfo.getIpAddress();

		byte[] ipBytes = BigInteger.valueOf(ip).toByteArray();

		try {
			serverDiscover = new ServerDiscover(
					(Inet4Address) Inet4Address.getByAddress(new byte[] {
							ipBytes[3], ipBytes[2], ipBytes[1], ipBytes[0] }),
					this);

			progressBarScanState.setText(R.string.text_scanning);

			serverDiscover.start();

		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	

	@Override
	public void onPause() {
		super.onPause();
		
		serverDiscover.stop();
	}

	@Override
	public void onStart() {

		super.onStart();

		// init setup
		if (serverDiscover == null) {

			// discover wifi first
			// TODO only support wifi currently

			if (adapter == null) {
				adapter = new ArrayAdapter<ServerState>(getActivity(),
						android.R.layout.simple_list_item_1, availableServers);
			}

			listViewServer.setAdapter(adapter);
			
			if (hasResult) {
				adapter.notifyDataSetChanged();
			}

			listViewServer.setOnItemClickListener(this);
		}

		//resume ?
		scanServer();
	}

	private ArrayList<ServerState> availableServers = new ArrayList<ServerState>();

	@Override
	public void onFound(final Inet4Address serverAddress) {
		getActivity().runOnUiThread(new Runnable() {
			@Override
			public void run() {

				availableServers.add(new ServerState(ScanState.FOUND,
						serverAddress));
				adapter.notifyDataSetChanged();
			}
		});
	}

	@Override
	public void onNotFound(Inet4Address arg0) {
		// TODO debug ui
	}

	@Override
	public void onScan(Inet4Address arg0, float percent) {
		String displayString = getString(R.string.text_scanning)
				+ arg0.toString();

		progressBarScanState.setText(displayString);
		progressBarScanState.setProgress((int) (percent * 100));
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {

		ServerState serverState = availableServers.get(arg2);

		String url = "http://" + serverState.address.getHostAddress() + ":"
				+ Global.HttpServerInfo.listenPort;
		Intent i = new Intent(Intent.ACTION_VIEW);
		i.setData(Uri.parse(url));
		startActivity(i);

	}

	@Override
	public void onFinished() {
		getActivity().runOnUiThread(new Runnable() {
			@Override
			public void run() {

				if (availableServers.isEmpty()) {
					noServerFoundDialog.show();
					return;
				}
				progressBarScanState.setText(R.string.text_scan_complete);
			}
		});

	}
}
