package com.transee.viditcam.actions;

import android.app.Activity;
import android.content.res.Resources;

import com.waylens.hachi.R;
import com.transee.viditcam.app.comp.MapProvider;

abstract public class SelectMapProvider extends SingleSelect {

	abstract public void onMapProviderSelected(int mapProvider);

	private int mMapProvider = MapProvider.MAP_GOOGLE;

	public SelectMapProvider(Activity activity) {
		super(activity);
	}

	@Override
	protected void onSelectItem(int id) {
		mMapProvider = id;
	}

	@Override
	protected void onClickPositiveButton() {
		onMapProviderSelected(mMapProvider);
	}

	@Override
	public void show() {
		Resources res = mContext.getResources();
		String text = res.getString(R.string.sel_google_map);
		addItem(text, MapProvider.MAP_GOOGLE);

		text = res.getString(R.string.sel_amap);
		addItem(text, MapProvider.MAP_AMAP);

		setSelId(mMapProvider);

		setTitle(R.string.title_select_map_provider);
		setButtons(DialogBuilder.DLG_OK_CANCEL);
		mbNoAutoDismiss = true;

		super.show();
	}

}
