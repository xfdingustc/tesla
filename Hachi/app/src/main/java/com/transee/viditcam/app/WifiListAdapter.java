package com.transee.viditcam.app;

import android.content.Context;
import android.net.wifi.ScanResult;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.waylens.hachi.R;

import java.util.ArrayList;
import java.util.List;

abstract public class WifiListAdapter extends BaseAdapter {

	abstract public List<ScanResult> getScanResult();

	abstract public void onClickDelete(int position);

	static final boolean DEBUG = false;
	static final String TAG = "WifiListAdapter";

	static class WifiItem {
		String mSSID;
		boolean mbActive;
		boolean mbSelected;

		WifiItem(String ssid) {
			mSSID = ssid;
			mbActive = false;
		}
	}

	final Context mContext;
	final ListView mListView;
	private final LayoutInflater mLayoutInflater;
	private final ArrayList<WifiItem> mWifiList;

	public WifiListAdapter(Context context, ListView listView) {
		mContext = context;
		mListView = listView;
		mLayoutInflater = LayoutInflater.from(context);
		mWifiList = new ArrayList<WifiItem>();
	}

	private WifiItem getBySSID(String ssid) {
		for (int i = 0; i < mWifiList.size(); i++) {
			WifiItem item = mWifiList.get(i);
			if (item.mSSID.equals(ssid)) {
				return item;
			}
		}
		return null;
	}

	private boolean ssidInList(String ssid) {
		return getBySSID(ssid) != null;
	}

	// API
	public void addSSID(String ssid) {
		if (!ssidInList(ssid)) {
			WifiItem item = new WifiItem(ssid);
			List<ScanResult> list = getScanResult();
			if (list != null && itemInList(item, list)) {
				item.mbActive = true;
			}
			mWifiList.add(item);
			notifyDataSetChanged();
		}
	}

	// API
	public String getSSID(int position) {
		WifiItem item = (WifiItem)getItem(position);
		return item == null ? null : item.mSSID;
	}

	// API
	public boolean removeSSID(String ssid) {
		for (int i = 0; i < mWifiList.size(); i++) {
			WifiItem item = mWifiList.get(i);
			if (item.mSSID.equals(ssid)) {
				mWifiList.remove(i);
				notifyDataSetChanged();
				return true;
			}
		}
		return false;
	}

	// API
	public boolean exists(String ssid) {
		for (WifiItem item : mWifiList) {
			if (item.mSSID.equals(ssid))
				return true;
		}
		return false;
	}

	// API
	public void clear() {
		mWifiList.clear();
		notifyDataSetChanged();
	}

	// API
	public void filterScanList(List<ScanResult> list) {
		int nChanged = 0;
		for (WifiItem item : mWifiList) {
			boolean bActive = itemInList(item, list);
			if (bActive != item.mbActive) {
				item.mbActive = bActive;
				nChanged++;
			}
		}
		if (nChanged > 0) {
			// TODO
			notifyDataSetChanged();
		}
	}

	// API
	public void toggleItem(View view, int position) {
		if (position >= 0 && position < mWifiList.size()) {
			WifiItem item = mWifiList.get(position);
			item.mbSelected = !item.mbSelected;
			ViewHolder holder = (ViewHolder)view.getTag();
			holder.button1.setVisibility(item.mbSelected ? View.VISIBLE : View.INVISIBLE);
		}
	}

	private boolean itemInList(WifiItem item, List<ScanResult> list) {
		for (ScanResult result : list) {
			if (item.mSSID.equals(result.SSID)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public int getCount() {
		return mWifiList.size();
	}

	@Override
	public Object getItem(int position) {
		if (position < 0 || position >= mWifiList.size())
			return null;
		return mWifiList.get(position);
	}

	@Override
	public long getItemId(int position) {
		// TODO
		return 0;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder holder = null;
		if (convertView != null) {
			holder = (ViewHolder)convertView.getTag();
		} else {
			convertView = mLayoutInflater.inflate(R.layout.item_wifi_list, parent, false);
			holder = new ViewHolder();
			holder.textView1 = (TextView)convertView.findViewById(R.id.dialogTitle);
			holder.button1 = (Button)convertView.findViewById(R.id.dlgLeftButton);
			convertView.setTag(holder);
		}
		holder.position = position;
		initView(holder, position);
		return convertView;
	}

	private void initView(ViewHolder holder, int position) {
		WifiItem item = (WifiItem)getItem(position);
		if (item == null) {

		} else {
			holder.button1.setTag(holder);
			holder.button1.setOnClickListener(mOnClickDelete);
			if (position < 0 || position > mWifiList.size()) {
				holder.textView1.setText("");
			} else {
				holder.textView1.setText(item.mSSID);
				holder.button1.setVisibility(item.mbSelected ? View.VISIBLE : View.INVISIBLE);
				if (item.mbActive) {
					holder.textView1.setCompoundDrawablesWithIntrinsicBounds(R.drawable.btn_wifi_on, 0, 0, 0);
				} else {
					holder.textView1.setCompoundDrawablesWithIntrinsicBounds(R.drawable.btn_wifi_off, 0, 0, 0);
				}
			}
		}
	}

	private final View.OnClickListener mOnClickDelete = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			ViewHolder holder = (ViewHolder)v.getTag();
			if (holder != null) {
				onClickDelete(holder.position);
			}
		}
	};

	final static class ViewHolder {
		int position;
		TextView textView1;
		Button button1;
	}

}
