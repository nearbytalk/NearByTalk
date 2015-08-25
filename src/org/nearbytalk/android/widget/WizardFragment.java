package org.nearbytalk.android.widget;

import org.nearbytalk.ClientActivity;
import org.nearbytalk.R;
import org.nearbytalk.ServerActivity;
import org.nearbytalk.NearByTalkApplication;
import org.nearbytalk.android.NoExternalStorageException;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;

public class WizardFragment extends Fragment implements OnClickListener {

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		View view = inflater
				.inflate(R.layout.fragment_wizard, container, false);

		int buttonIds[] = { R.id.buttonAutomaticMode, R.id.buttonClientMode,
				R.id.buttonMaunal, R.id.buttonServerMode };

		for (int id : buttonIds) {

			view.findViewById(id).setOnClickListener(this);

		}

		return view;
	}

	@Override
	public void onStart() {

		super.onStart();

		try {
			NearByTalkApplication.getInstance().initCheck();
		} catch (NoExternalStorageException e) {

			AlertDialog alertDialog = new AlertDialog.Builder(getActivity())
					.setTitle(R.string.error_no_external_storage_title)
					.setPositiveButton(android.R.string.ok,
							new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									getActivity().finish();
								}
							}).create();

			// Showing Alert Message
			alertDialog.show();
		}
	}

	@Override
	public void onClick(View v) {

		switch (v.getId()) {
		case R.id.buttonServerMode: {
			Intent intent = new Intent(getActivity(), ServerActivity.class);
			startActivity(intent);
			break;
		}
		case R.id.buttonClientMode: {
			Intent intent = new Intent(getActivity(), ClientActivity.class);
			startActivity(intent);
			break;
		}

		default:
			break;
		}

	}

}
