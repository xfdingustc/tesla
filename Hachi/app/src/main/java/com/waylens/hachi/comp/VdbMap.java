package com.waylens.hachi.comp;

import android.os.Bundle;
import android.widget.RelativeLayout;

import com.transee.common.GPSPath;
import com.transee.common.GPSRawData;
import com.transee.viditcam.app.VdbEditor;
import com.waylens.hachi.ui.activities.BaseActivity;

abstract public class VdbMap {

    static final String PREF_LAT = "lat";
    static final String PREF_LNG = "lng";
    static final String PREF_LAT_ORIG = "latOrig";
    static final String PREF_LNG_ORIG = "lngOrig";
    static final String PREF_ZOOM = "zoom";
    static final String PREF_TYPE = "type";

    static final int TRACK_COLOR = 0x800000ff;

    //0x803f48cc;

    static class MapConfig {
        public final GPSRawData.Coord coord = new GPSRawData.Coord();
        public float zoom;
        public int mapType;
    }

    protected MapConfig mMapConfig = new MapConfig();

    protected static final boolean DEBUG = false;
    protected static final String TAG = "VdbMap";

    protected final BaseActivity mActivity;
    protected final RelativeLayout mMapControl;
    protected final VdbEditor mEditor;

    protected VdbMap(BaseActivity activity, RelativeLayout mapControl, VdbEditor editor) {
        mActivity = activity;
        mMapControl = mapControl;
        mEditor = editor;
    }

    abstract public void onCreate(Bundle savedInstanceState);

    abstract public void onDestroy();

    abstract public void onPause();

    abstract public void onResume();

    abstract public void onSaveInstanceState(Bundle outState);

    abstract public void onLowMemory();

    abstract public void setGPSRawData(GPSRawData rawData);

    abstract public void showMap();

    abstract public void hideMap();

    abstract public void addSegment(GPSPath.Segment segment);
}
