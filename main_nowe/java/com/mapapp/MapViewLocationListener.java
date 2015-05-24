/*
 * MapApp : Simple offline map application, made by Hisham Ghosheh for tutorial purposes only
 * Tutorial on my blog
 * http://ghoshehsoft.wordpress.com/2012/03/09/building-a-map-app-for-android/
 * 
 * Class tutorial:
 * http://ghoshehsoft.wordpress.com/2012/04/06/mapapp5-mapview-and-activity/
 */

package com.mapapp;

import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;

public class MapViewLocationListener implements LocationListener
{
	MapView mapView;
	boolean stopped = false;

	public MapViewLocationListener(MapView mapView)
	{
		this.mapView = mapView;
	}

	@Override
	public void onLocationChanged(Location location)
	{
		if (!stopped && location != null)
		{
			// Set location and update the mapView
			mapView.setGpsLocation(location.getLongitude(), location.getLatitude(), location.getAltitude(), location.getAccuracy());
			mapView.postInvalidate();
		}
	}

	public void stop()
	{
		stopped = true;
		mapView = null;
	}

	@Override
	public void onProviderDisabled(String provider)
	{
	}

	@Override
	public void onProviderEnabled(String provider)
	{
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras)
	{
	}

}