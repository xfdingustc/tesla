package com.waylens.hachi.hardware.vdtcamera;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Map;

// maintains WIFI SSID-password table
public class PasswordList {

	public static final int MAX_ITEMS = 10;

	// ssid/password pair
	public class Item {
		String mSSID;
		String mPassword;

		Item(String ssid, String password) {
			mSSID = ssid;
			mPassword = password;
		}
	}

	SharedPreferences mPreferences;
	Item[] mList = new Item[MAX_ITEMS];
	int mNumItems;

	// API
	public void load(Context context, String name) {
		// reset
		for (int index = 0; index < mList.length; index++) {
			mList[index] = null;
		}
		mNumItems = 0;

		// load
		mPreferences = context.getSharedPreferences(name, 0);
		Map<String, ?> all = mPreferences.getAll();
		for (Map.Entry<String, ?> entry : all.entrySet()) {
			String sIndex = entry.getKey();
			int index = Integer.parseInt(sIndex);
			if (index >= 0 && index < mList.length && mList[index] == null) {
				String info = entry.getValue().toString();
				String[] pair = info.split(":");
				if (pair.length == 2) {
					Item item = new Item(pair[0], pair[1]);
					mList[index] = item;
					mNumItems++;
				}
			}
		}

		//
		for (int index = 0; index < mNumItems; index++) {
			if (mList[index] == null) {
				mList[index] = getNextValid(index);
			}
		}
	}

	private Item getNextValid(int start) {
		for (int i = start + 1; i < mList.length; i++) {
			Item result = mList[i];
			if (result != null) {
				mList[i] = null;
				return result;
			}
		}
		return null;
	}

	// API
	public String getPassword(String ssid) {
		for (int index = 0; index < mNumItems; index++) {
			Item item = mList[index];
			if (item.mSSID.equals(ssid)) {
				return item.mPassword;
			}
		}
		return null;
	}

	// API
	public void setPassword(String ssid, String password) {
		addItem(ssid, password);
		saveList();
	}

	private void addItem(String ssid, String password) {
		// modify existed one
		for (int index = 0; index < mNumItems; index++) {
			Item item = mList[index];
			if (item.mSSID.endsWith(ssid)) {
				item.mPassword = password;
				return;
			}
		}
		// if full, remove the first one
		if (mNumItems >= mList.length) {
			for (int index = 0; index < mList.length - 1; index++) {
				mList[index] = mList[index + 1];
			}
			mNumItems--;
		}
		// append
		Item item = new Item(ssid, password);
		mList[mNumItems] = item;
		mNumItems++;
	}

	private void saveList() {
		SharedPreferences.Editor editor = mPreferences.edit();
		editor.clear();
		for (int index = 0; index < mNumItems; index++) {
			Item item = mList[index];
			editor.putString(String.valueOf(index), item.mSSID + ":" + item.mPassword);
		}
		editor.commit();
	}
}
