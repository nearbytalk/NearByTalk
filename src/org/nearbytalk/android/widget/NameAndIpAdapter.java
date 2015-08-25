package org.nearbytalk.android.widget;

import java.util.ArrayList;
import java.util.List;

import org.nearbytalk.R;
import org.nearbytalk.android.widget.WifiProcessDialogFragment.NameAndIp;

import android.content.Context;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

public class NameAndIpAdapter extends
		ArrayAdapter<NameAndIpAdapter.UiNameAndIp> implements OnClickListener {
	
	public static interface SelectionHandler{
		public void onHasSelections();
		public void onNoneSelection();
	}

	private static List<UiNameAndIp> convert(List<NameAndIp> nameAndIps) {

		List<UiNameAndIp> ret = new ArrayList<NameAndIpAdapter.UiNameAndIp>();

		for (NameAndIp nameAndIp : nameAndIps) {

			UiNameAndIp one = new UiNameAndIp();
			one.selected = false;
			one.nameAndIp = nameAndIp;

			ret.add(one);

		}

		return ret;

	}
	
	public void add(NameAndIp nameAndIp){
		
		UiNameAndIp uiNameAndIp=new UiNameAndIp();
		
		uiNameAndIp.selected=false;
		uiNameAndIp.nameAndIp=nameAndIp;
		
		add(uiNameAndIp);
	}
	
	private SelectionHandler handler;

	public NameAndIpAdapter(Context context, int resource,
			SelectionHandler handler) {

		super(context, resource);
		
		this.handler=handler;
	}

	static class UiNameAndIp {

		NameAndIp nameAndIp;

		boolean selected = false;

	}

	private static class RowHolder {
		CheckBox checkBox;
		TextView textView;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {

		RowHolder holder = null;
		if (convertView == null) {
			convertView = parent.inflate(getContext(),
					R.layout.row_net_interface, null);
			holder = new RowHolder();
			holder.checkBox = (CheckBox) convertView
					.findViewById(R.id.checkBoxNetInterface);
			holder.textView = (TextView) convertView
					.findViewById(R.id.textNameAndIp);

			convertView.setTag(holder);

			holder.checkBox.setOnClickListener(this);
		} else {
			holder = (RowHolder) convertView.getTag();
		}

		UiNameAndIp nameAndIp = this.getItem(position);

		holder.checkBox.setText(nameAndIp.nameAndIp.name);

		holder.textView.setText(nameAndIp.nameAndIp.ipAddress);

		holder.checkBox.setTag(nameAndIp);

		return convertView;
	}

	@Override
	public void onClick(View v) {

		CheckBox checkBox = (CheckBox) v;

		UiNameAndIp nameAndIp = (UiNameAndIp) checkBox.getTag();

		nameAndIp.selected = checkBox.isSelected();
		
		if (getSelected().isEmpty()) {
			handler.onNoneSelection();
		}else{
			handler.onHasSelections();
		}
	}

	public List<NameAndIp> getSelected() {

		List<NameAndIp> ret = new ArrayList<WifiProcessDialogFragment.NameAndIp>();

		int count = getCount();

		for (int i = 0; i < count; i++) {

			UiNameAndIp thisOne = getItem(i);

			if (thisOne.selected) {
				ret.add(thisOne.nameAndIp);
			}

		}

		return ret;
	}

}
