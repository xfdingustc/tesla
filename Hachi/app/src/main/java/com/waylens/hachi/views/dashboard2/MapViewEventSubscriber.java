package com.waylens.hachi.views.dashboard2;

import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.views.MapView;
import com.orhanobut.logger.Logger;
import com.transee.common.GPSRawData;
import com.waylens.hachi.views.dashboard2.eventbus.EventBus;
import com.waylens.hachi.views.dashboard2.eventbus.EventConstants;



/**
 * Created by Xiaofei on 2015/11/25.
 */
public class MapViewEventSubscriber implements EventBus.EventSubscriber {
    private static final String TAG = MapViewEventSubscriber.class.getSimpleName();
    private final MapView mMapView;

    public MapViewEventSubscriber(MapView mapView) {
        this.mMapView = mapView;
    }


    @Override
    public String getSubscribe() {
        return EventConstants.EVENT_GPS;
    }

    @Override
    public void onEvent(Object value) {
        GPSRawData gpsRawData = (GPSRawData) value;
        Logger.t(TAG).d("Lat: " + gpsRawData.coord.lat_orig + " Lng: " + gpsRawData.coord.lng_orig);

    }
}
