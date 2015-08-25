package org.nearbytalk.android.widget;

import org.nearbytalk.R;
import org.nearbytalk.NearByTalkApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

public class CustomSsidDialogFragment extends DialogFragment implements OnClickListener {
	private static final Logger log = LoggerFactory
			.getLogger(PasswordDialogFragment.class);

	Button okButton;
	Button cancelButton;

	TextView customSsidInput;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.dialog_custom_ssid,
				container);
		okButton = (Button) view.findViewById(R.id.buttonOk);
		cancelButton = (Button) view.findViewById(R.id.buttonCancel);

		customSsidInput= (TextView) view.findViewById(R.id.inputSsid);

		okButton.setOnClickListener(this);
		cancelButton.setOnClickListener(this);
		
		String customSsid=NearByTalkApplication.getInstance().getConfig().getCustomSsid();
		
		if (customSsid!=null) {
			customSsidInput.setText(customSsid);
		}


		return view;
	}

	@Override
	public void onClick(View v) {
		
		if(v==okButton){
			
			String customSsid=this.customSsidInput.getText().toString();
			
			NearByTalkApplication.getInstance().getConfig().setCustomSsid(customSsid.length()==0?null:customSsid);
			
			dismiss();
			return;
			
		}else if(v==cancelButton){
			
			dismiss();
			return;
		}
		
	}
	
	
	private final static String FRAGMENT_TAG=CustomSsidDialogFragment.class.getSimpleName();

	public static void pop(FragmentManager fragmentManager,
			ServerModeFragment serverModeFragment) {
		FragmentTransaction ft = fragmentManager.beginTransaction();

		Fragment prev = fragmentManager.findFragmentByTag(FRAGMENT_TAG);

		if (prev != null) {
			ft.remove(prev);
		}

		ft.addToBackStack(null);

		CustomSsidDialogFragment fragment = new CustomSsidDialogFragment();

		fragment.show(ft, FRAGMENT_TAG);

	}

}
