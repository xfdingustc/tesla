package com.waylens.hachi.comp;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.transee.common.GPSPath;
import com.transee.common.GPSRawData;
import com.waylens.hachi.R;
import com.transee.viditcam.app.VdbEditor;
import com.waylens.hachi.ui.activities.BaseActivity;

import java.util.ArrayList;
import java.util.Locale;

abstract public class VdbMapGoogle extends VdbMap {

    abstract public void onRequestChangeMap(VdbMapGoogle vdbMap);

    static final String DEF_LAT = "39.9388838";
    static final String DEF_LNG = "116.3974589";
    static final float DEF_ZOOM = 15.0f;
    static final String PREF_NAME = "googlemap";

    private View mLayout;

    private MapView mMapView;
    private GoogleMap mMap;
    private Button mButton;
    private Marker mMarker;
    private final ArrayList<Polyline> mTrackList = new ArrayList<Polyline>();

    public static void initialize(Context context) {
        MapsInitializer.initialize(context);
    }

    protected VdbMapGoogle(BaseActivity activity, RelativeLayout mapControl, VdbEditor editor) {
        super(activity, mapControl, editor);
        mButton = (Button) mapControl.findViewById(R.id.button1);
        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleMapType();
            }
        });

        int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(mActivity);
        if (status != ConnectionResult.SUCCESS) {
            createInfoView(status);
        } else {
            createMap();
            setButtonText();
            mButton.bringToFront();
        }
    }

    // API
    public void removeEmptyMap() {
        if (mLayout != null) {
            mMapControl.removeView(mLayout);
            mLayout = null;
        }
    }

    private void onCreateMap(Bundle bundle) {
        if (mMapView != null) {
            mMapView.onCreate(bundle);
            //
            mMap = mMapView.getMap();
            mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
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
        if (mMapView != null) {
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
        if (mMapView != null) {
            mMapView.onLowMemory();
        }
    }

    private LatLng newLatLng(GPSRawData.Coord coord) {
        int type = mMapConfig.mapType;
        switch (type) {
            case GoogleMap.MAP_TYPE_NORMAL:
                break;
            case GoogleMap.MAP_TYPE_SATELLITE:
                return new LatLng(coord.lat_orig, coord.lng_orig);
            case GoogleMap.MAP_TYPE_TERRAIN:
                break;
            default:
                break;
        }
        return new LatLng(coord.lat, coord.lng);
    }

    private void moveMarkerAndCamera(GPSRawData.Coord coord) {
        LatLng latlng = newLatLng(coord);

        CameraUpdate update = CameraUpdateFactory.newLatLngZoom(latlng, getZoom());
        mMap.animateCamera(update, 100, null);
        // mMap.animateCamera(update);

        if (mMarker == null) {
            MarkerOptions opt = new MarkerOptions();
            opt.position(latlng);
            mMarker = mMap.addMarker(opt);
        } else {
            mMarker.setPosition(latlng);
        }
    }

    @Override
    public void setGPSRawData(GPSRawData rawData) {
        if (mMap != null && rawData.hasLatLng()) {
            mMapConfig.coord.set(rawData.coord);
            moveMarkerAndCamera(rawData.coord);
            mButton.setVisibility(View.INVISIBLE);
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
            clearMarker();
            clearLines();
            // mMap.invalidate();
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

    // SUCCESS, SERVICE_MISSING, SERVICE_VERSION_UPDATE_REQUIRED,
    // SERVICE_DISABLED, SERVICE_INVALID
    private void createInfoView(int status) {
        View layout = LayoutInflater.from(mActivity).inflate(R.layout.group_google_map_error, mMapControl, false);
        TextView error = (TextView) layout.findViewById(R.id.textView2);
        Resources res = mActivity.getResources();
        String fmt = res.getString(R.string.msg_googlemap_error_1);
        String text;
        switch (status) {
            case ConnectionResult.SERVICE_MISSING:
                text = String.format(Locale.US, fmt, "SERVICE_MISSING");
                break;
            case ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED:
                text = String.format(Locale.US, fmt, "SERVICE_VERSION_UPDATE_REQUIRED");
                break;
            case ConnectionResult.SERVICE_DISABLED:
                text = String.format(Locale.US, fmt, "SERVICE_DISABLED");
                break;
            case ConnectionResult.SERVICE_INVALID:
                text = String.format(Locale.US, fmt, "SERVICE_INVALID");
                break;
            default:
                text = res.getString(R.string.msg_googlemap_error_2);
                break;
        }
        error.setText(text);

        Button button = (Button) layout.findViewById(R.id.button1);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onRequestChangeMap(VdbMapGoogle.this);
            }
        });

        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT);
        layout.setLayoutParams(lp);

        mLayout = layout;
        mMapControl.addView(layout);
    }

    private void createMap() {
        readSettings();

        GoogleMapOptions options = new GoogleMapOptions();

        options.mapType(mMapConfig.mapType);
        options.zoomControlsEnabled(false);
        options.tiltGesturesEnabled(false);

        CameraPosition.Builder builder = CameraPosition.builder();
        builder.target(newLatLng(mMapConfig.coord));
        builder.zoom(mMapConfig.zoom);
        options.camera(builder.build());

        mMapView = new MapView(mActivity, options);
        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT);
        mMapView.setLayoutParams(lp);
        mLayout = mMapView;
        mMapControl.addView(mMapView);

        onCreateMap(new Bundle());
        onResumeMap();
    }

    private void toggleMapType() {
        int type = mMap.getMapType();
        switch (type) {
            case GoogleMap.MAP_TYPE_NORMAL:
                type = GoogleMap.MAP_TYPE_SATELLITE;
                break;
            case GoogleMap.MAP_TYPE_SATELLITE:
                type = GoogleMap.MAP_TYPE_TERRAIN;
                break;
            case GoogleMap.MAP_TYPE_TERRAIN:
                type = GoogleMap.MAP_TYPE_HYBRID;
                break;
            default:
                type = GoogleMap.MAP_TYPE_NORMAL;
                break;
        }
        mMapConfig.mapType = type;
        mMap.setMapType(type);
        setButtonText();
        if (mMarker != null) {
            moveMarkerAndCamera(mMapConfig.coord);
        }
    }

    private void setButtonText() {
        int type = mMapConfig.mapType;
        int resId;
        switch (type) {
            default:
            case GoogleMap.MAP_TYPE_NORMAL:
                resId = R.string.btn_map_normal;
                break;
            case GoogleMap.MAP_TYPE_SATELLITE:
                resId = R.string.btn_map_satellite;
                break;
            case GoogleMap.MAP_TYPE_TERRAIN:
                resId = R.string.btn_map_terrain;
                break;
            case GoogleMap.MAP_TYPE_HYBRID:
                resId = R.string.btn_map_hybrid;
                break;
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
        mMapConfig.mapType = pref.getInt(PREF_TYPE, GoogleMap.MAP_TYPE_NORMAL);
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
