package com.transee.viditcam.app.comp;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;

import com.amap.api.maps2d.AMap;
import com.amap.api.maps2d.AMapOptions;
import com.amap.api.maps2d.CameraUpdate;
import com.amap.api.maps2d.CameraUpdateFactory;
import com.amap.api.maps2d.MapView;
import com.amap.api.maps2d.model.CameraPosition;
import com.amap.api.maps2d.model.LatLng;
import com.amap.api.maps2d.model.Marker;
import com.amap.api.maps2d.model.MarkerOptions;
import com.amap.api.maps2d.model.Polyline;
import com.amap.api.maps2d.model.PolylineOptions;
import com.transee.common.GPSPath;
import com.transee.common.GPSRawData;
import com.waylens.hachi.R;
import com.transee.viditcam.app.BaseActivity;
import com.transee.viditcam.app.VdbEditor;

import java.util.ArrayList;

public class VdbMapAmap extends VdbMap {

	static final String DEF_LAT = "39.9388838";
	static final String DEF_LNG = "116.3974589";
	static final float DEF_ZOOM = 15.0f;
	static final String PREF_NAME = "amap";

	private MapView mMapView;
	private AMap mMap;
	private Button mButton;
	private Marker mMarker;
	private final ArrayList<Polyline> mTrackList = new ArrayList<Polyline>();

	public VdbMapAmap(BaseActivity activity, RelativeLayout mapControl, VdbEditor editor) {
		super(activity, mapControl, editor);
		mButton = (Button)mapControl.findViewById(R.id.button1);
		mButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				toggleMapType();
			}
		});
		createMap();
		setButtonText();
		mButton.bringToFront();
	}

	private void onCreateMap(Bundle bundle) {
		if (mMapView != null) {
			mMapView.onCreate(bundle);
			mMap = mMapView.getMap();
			mMap.setOnMapClickListener(new AMap.OnMapClickListener() {
				@Override
				public void onMapClick(LatLng arg0) {
					Button button = mButton;
					if (button.getVisibility() == View.VISIBLE) {
						button.setVisibility(View.INVISIBLE);
					} else {
						button.setVisibility(View.VISIBLE);
					}
				}
			});
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		// call onCreateMap in createMap()
	}

	@Override
	public void onDestroy() {
		if (mMap != null) {
			writeSettings();
			onPauseMap();
			mMapControl.removeView(mMapView);
			mMapView.onDestroy();
		}
	}

	private void onPauseMap() {
		if (mMapView != null) {
			if (mMap != null) {
				mMapConfig.zoom = mMap.getCameraPosition().zoom;
				mMapConfig.mapType = mMap.getMapType();
			}
			mMapView.onPause();
		}
	}

	@Override
	public void onPause() {
		// call onPauseMap in onDestroy()
	}

	private void onResumeMap() {
		if (mMapView != null) {
			mMapView.onResume();
		}
	}

	@Override
	public void onResume() {
		// call onResumeMap in createMap()
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		/*
		if (mMapView != null) {
			mMapView.onSaveInstanceState(outState);
		}
		*/
	}

	@Override
	public void onLowMemory() {
	}

	private LatLng newLatLng(GPSRawData.Coord coord) {
		switch (mMapConfig.mapType) {
		case AMap.MAP_TYPE_NORMAL:
			break;
		case AMap.MAP_TYPE_SATELLITE:
			// return new LatLng(coord.lat_orig, coord.lng_orig);
			break;
		default:
			break;
		}
		return new LatLng(coord.lat, coord.lng);
	}

	@Override
	public void setGPSRawData(GPSRawData rawData) {
		if (mMap != null && rawData.hasLatLng()) {

			mMapConfig.coord.set(rawData.coord);

			LatLng latlng = newLatLng(rawData.coord);

			createMarker();
			mMarker.setPosition(latlng);

			CameraUpdate update = CameraUpdateFactory.newLatLngZoom(latlng, getZoom());
			mMap.moveCamera(update);

			mButton.setVisibility(View.INVISIBLE);
		}
	}

	private void createMarker() {
		if (mMarker == null) {
			MarkerOptions opt = new MarkerOptions();
			mMarker = mMap.addMarker(opt);
		}
	}

	private void clearMarker() {
		if (mMarker != null) {
			mMarker.remove();
			mMarker = null;
		}
	}

	private void clearLines() {
		for (int i = 0; i < mTrackList.size(); i++) {
			Polyline line = mTrackList.get(i);
			line.remove();
		}
		mTrackList.clear();
	}

	@Override
	public void showMap() {
		if (mMap != null) {
			if (mMapView != null) {
				mMapView.setVisibility(View.VISIBLE);
			}
			clearMarker();
			clearLines();
			mMap.invalidate();
		}
	}

	@Override
	public void hideMap() {
		if (mMap != null) {
			clearMarker();
			clearLines();
		}
	}

	private final float getZoom() {
		return mMap.getCameraPosition().zoom;
	}

	private void createMap() {
		readSettings();

		AMapOptions options = new AMapOptions();

		options.mapType(mMapConfig.mapType);
		options.zoomControlsEnabled(false);
		options.scaleControlsEnabled(true);

		CameraPosition.Builder builder = CameraPosition.builder();
		builder.target(newLatLng(mMapConfig.coord));
		builder.zoom(mMapConfig.zoom);
		options.camera(builder.build());

		mMapView = new MapView(mActivity, options);
		RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT,
				LayoutParams.MATCH_PARENT);
		mMapView.setLayoutParams(lp);
		mMapView.setVisibility(View.GONE); // workaround: on CoolPad & MIUI, it will hang otherwise
		mMapControl.addView(mMapView);

		onCreateMap(new Bundle());
		onResumeMap();
	}

	private void toggleMapType() {
		int type = mMap.getMapType();
		if (type == AMap.MAP_TYPE_NORMAL) {
			type = AMap.MAP_TYPE_SATELLITE;
		} else {
			type = AMap.MAP_TYPE_NORMAL;
		}
		mMapConfig.mapType = type;
		mMap.setMapType(type);
		setButtonText();
	}

	private void setButtonText() {
		int type = mMap.getMapType();
		int resId;
		if (type == AMap.MAP_TYPE_NORMAL) {
			resId = R.string.btn_map_normal;
		} else {
			resId = R.string.btn_map_satellite;
		}
		mButton.setText(resId);
	}

	private void readSettings() {
		SharedPreferences pref = mActivity.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
		String lat = pref.getString(PREF_LAT, DEF_LAT);
		mMapConfig.coord.lat = Double.parseDouble(lat);
		String lng = pref.getString(PREF_LNG, DEF_LNG);
		mMapConfig.coord.lng = Double.parseDouble(lng);
		String latOrig = pref.getString(PREF_LAT_ORIG, DEF_LAT);
		mMapConfig.coord.lat_orig = Double.parseDouble(latOrig);
		String lngOrig = pref.getString(PREF_LNG_ORIG, DEF_LNG);
		mMapConfig.coord.lng_orig = Double.parseDouble(lngOrig);
		mMapConfig.zoom = pref.getFloat(PREF_ZOOM, DEF_ZOOM);
		mMapConfig.mapType = pref.getInt(PREF_TYPE, AMap.MAP_TYPE_NORMAL);
	}

	private void writeSettings() {
		SharedPreferences pref = mActivity.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
		Editor editor = pref.edit();
		editor.putString(PREF_LAT, Double.toString(mMapConfig.coord.lat));
		editor.putString(PREF_LNG, Double.toString(mMapConfig.coord.lng));
		editor.putString(PREF_LAT_ORIG, Double.toString(mMapConfig.coord.lat_orig));
		editor.putString(PREF_LNG_ORIG, Double.toString(mMapConfig.coord.lng_orig));
		editor.putFloat(PREF_ZOOM, mMapConfig.zoom);
		editor.putInt(PREF_TYPE, mMapConfig.mapType);
		editor.commit();
	}

	private PolylineOptions newOptions() {
		PolylineOptions options = new PolylineOptions();
		options.color(TRACK_COLOR);
		return options;
	}

	private void addOptions(PolylineOptions options) {
		Polyline line = mMap.addPolyline(options);
		mTrackList.add(line);
	}

	@Override
	public void addSegment(GPSPath.Segment segment) {
		if (mMap == null) {
			return;
		}

		PolylineOptions options = newOptions();
		int count = 0;

		for (int i = 0; i < segment.mNumPoints; i++) {
			if (segment.mSepArray[i] == 1) {
				if (count > 0) {
					addOptions(options);
					options = newOptions();
					count = 0;
				}
			} else {
				LatLng latlng = new LatLng(segment.mLatArray[i], segment.mLngArray[i]);
				options.add(latlng);
				count++;
			}
		}

		if (count > 0) {
			addOptions(options);
		}
	}
}
