package com.waylens.hachi.views.dashboard.subscribers;

import android.graphics.Color;

import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.annotations.PolylineOptions;
import com.mapbox.mapboxsdk.annotations.SpriteFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.views.MapView;
import com.orhanobut.logger.Logger;
import com.waylens.hachi.vdb.GPSRawData;
import com.waylens.hachi.R;
import com.waylens.hachi.views.dashboard.eventbus.EventBus;
import com.waylens.hachi.views.dashboard.eventbus.EventConstants;



/**
 * Created by Xiaofei on 2015/11/25.
 */
public class MapViewEventSubscriber implements EventBus.EventSubscriber {
    private static final String TAG = MapViewEventSubscriber.class.getSimpleName();
    private final MapView mMapView;
    private MarkerOptions mMarkerOptions;

    private SpriteFactory spriteFactory;
    private PolylineOptions mPolylineOptions;


    public MapViewEventSubscriber(MapView mapView) {
        this.mMapView = mapView;
        initMapView();
    }

    private void initMapView() {
        spriteFactory = new SpriteFactory(mMapView);
        mPolylineOptions = new PolylineOptions().color(Color.rgb(252, 219, 12)).width(3);
        LatLng firstPoint = new LatLng(0, 0);
        mMarkerOptions = new MarkerOptions().position(firstPoint)
            .icon(spriteFactory.fromResource(R.drawable.map_car_inner_red_triangle));
    }


    @Override
    public String getSubscribe() {
        return EventConstants.EVENT_GPS;
    }

    @Override
    public void onEvent(Object value) {
        GPSRawData gpsRawData = (GPSRawData) value;
        //Logger.t(TAG).d("Lat: " + gpsRawData.coord.lat_orig + " Lng: " + gpsRawData.coord
         //   .lng_orig);
        mMapView.removeAllAnnotations();
        LatLng point = new LatLng(gpsRawData.coord.lat_orig, gpsRawData.coord.lng_orig);


        mMarkerOptions.position(point);
        mPolylineOptions.add(point);
        mMapView.addMarker(mMarkerOptions);
        mMapView.addPolyline(mPolylineOptions);
        mMapView.setCenterCoordinate(point);

    }
}
