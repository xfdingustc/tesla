package com.transee.viditcam.actions;

import android.app.Activity;
import android.net.wifi.ScanResult;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.transee.common.Utils;
import com.waylens.hachi.hardware.WifiAdmin;
import com.waylens.hachi.R;
import com.waylens.hachi.app.Hachi;


import java.util.ArrayList;
import java.util.List;

abstract public class SelectWifiAp extends DialogBuilder {

	// TODO - limit the dialog size
	// TODO - need to known if password needed
	abstract protected void onSelectWifi(String ssid);

	private final Hachi mThisApp;
	private final LayoutInflater mLayoutInflater;
	private final WifiListAdapter mWifiListAdapter;
	private ListView mListView;

	public SelectWifiAp(Activity activity, Hachi thisApp) {
		super(activity);
		mThisApp = thisApp;
		mLayoutInflater = activity.getLayoutInflater();
		mWifiListAdapter = new WifiListAdapter();
	}

	@Override
	public void show() {
		setTitle(R.string.title_select_wifi);
		setContent(R.layout.dialog_wifi_list);
		setButtons(DialogBuilder.DLG_CANCEL);
		super.show();
	}

	@Override
	protected void onDialogCreated(BaseDialog dialog, View layout) {
		dialog.requestNoPadding();
		mListView = (ListView)layout.findViewById(R.id.listView1);
		WifiAdmin wifiAdmin = mThisApp.attachWifiAdmin(mWifiCallback);
		mWifiListAdapter.filterWifiList(wifiAdmin.getScanResult());
		mListView.setAdapter(mWifiListAdapter);
		mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				String ssid = mWifiListAdapter.getSSID(position);
				dismiss();
				onSelectWifi(ssid);
			}
		});
	}

	@Override
	protected void onDismiss() {
		mThisApp.detachWifiAdmin(mWifiCallback, false);
	}

	final Hachi.WifiCallback mWifiCallback = new Hachi.WifiCallback() {
		@Override
		public void wifiScanResult(WifiAdmin wifiAdmin) {
		}

		@Override
		public void networkStateChanged(WifiAdmin wifiAdmin) {
		}

		@Override
		public void onConnectError(WifiAdmin wifiAdmin) {
		}

		@Override
		public void onConnectDone(WifiAdmin wifiAdmin) {
		}
	};

	class WifiListAdapter extends BaseAdapter {

		ArrayList<String> mWifiList = new ArrayList<String>();

		public void filterWifiList(List<ScanResult> list) {
			mWifiList.clear();
			for (ScanResult r : list) {
				String ssid = Utils.normalizeNetworkName(r.SSID);
				if (ssid != null) {
					mWifiList.add(ssid);
				}
			}
			notifyDataSetChanged();
		}

		public String getSSID(int position) {
			return mWifiList.get(position);
		}

		@Override
		public int getCount() {
			return mWifiList.size();
		}

		@Override
		public Object getItem(int position) {
			return mWifiList.get(position);
		}

		@Override
		public long getItemId(int position) {
			return 0;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView == null) {
				convertView = mLayoutInflater.inflate(R.layout.item_wifi_list_2, parent, false);
			}
			TextView tv = (TextView)convertView;
			tv.setText(mWifiList.get(position));
			return convertView;
		}

	};
}
