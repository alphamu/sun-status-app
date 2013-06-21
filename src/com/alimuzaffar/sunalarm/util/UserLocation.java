package com.alimuzaffar.sunalarm.util;

import com.alimuzaffar.sunalarm.R;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

public class UserLocation {
	
	private static UserLocation userLocation;
	
    private static final int TEN_SECONDS = 10000;
    private static final int TEN_METERS = 10;
    private static final int TWO_MINUTES = 1000 * 60 * 2;
    // UI handler codes.
    public static final int UPDATE_LATLNG = 2;

	private LocationManager mLocationManager;
	private Context mContext;
	private Handler mHandler;
	private OnNoProviderEnabledListener onNoProviderEnabledListener;
	private OnLocationChangedListener onLocationChangedListener;
	
    private final LocationListener listener = new LocationListener() {

        @Override
        public void onLocationChanged(Location location) {
            // A new location update is received.  Do something useful with it.  Update the UI with
            // the location update.
        	if(mHandler != null)
        		updateUILocation(location);
        	
            if(onLocationChangedListener != null)
            	onLocationChangedListener.onLocationChanged(location);
        }

        @Override
        public void onProviderDisabled(String provider) {
        }

        @Override
        public void onProviderEnabled(String provider) {
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }
    };

    public interface OnLocationChangedListener {
    	public void onLocationChanged(Location location);
    }

	
	public UserLocation(Context context, Handler handler) {
		this.mContext = context;
		this.mHandler = handler;
		// Get a reference to the LocationManager object.
        mLocationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
	}
	
	public static UserLocation getInstance(Context context, Handler handler) {
		if(userLocation == null) {
			userLocation = new UserLocation(context, handler);
		}
		return userLocation;
	}
	
	public void registerLocationListener() {
        Location gpsLocation = null;
        Location networkLocation = null;
        mLocationManager.removeUpdates(listener);
        // Get fine location updates only.
        
            // Request updates from both fine (gps) and coarse (network) providers.
            gpsLocation = requestUpdatesFromProvider(
                    LocationManager.GPS_PROVIDER, R.string.not_support_gps);
            networkLocation = requestUpdatesFromProvider(
                    LocationManager.NETWORK_PROVIDER, R.string.not_support_network);

            // If both providers return last known locations, compare the two and use the better
            // one to update the UI.  If only one provider returns a location, use it.
            if (gpsLocation != null && networkLocation != null) {
                updateUILocation(getBetterLocation(gpsLocation, networkLocation));
            } else if (gpsLocation != null) {
            	if(listener != null)
            		listener.onLocationChanged(gpsLocation);
            	else
            		updateUILocation(gpsLocation);
            } else if (networkLocation != null) {
            	if(listener != null)
            		listener.onLocationChanged(networkLocation);
            	else
            		updateUILocation(networkLocation);
            } else {
            	if(onNoProviderEnabledListener != null)
            		onNoProviderEnabledListener.onNoProviderEnabled();
            }
		
	}
	
    private Location requestUpdatesFromProvider(final String provider, final int errorResId) {
        Location location = null;
        if (mLocationManager.isProviderEnabled(provider)) {
            mLocationManager.requestLocationUpdates(provider, TEN_SECONDS, TEN_METERS, listener);
            location = mLocationManager.getLastKnownLocation(provider);
            if(location != null)
            	updateUILocation(location);
        } else {
            //Toast.makeText(mContext, errorResId, Toast.LENGTH_LONG).show();
        }
        return location;
    }
    
    /** Determines whether one Location reading is better than the current Location fix.
     * Code taken from
     * http://developer.android.com/guide/topics/location/obtaining-user-location.html
     *
     * @param newLocation  The new Location that you want to evaluate
     * @param currentBestLocation  The current Location fix, to which you want to compare the new
     *        one
     * @return The better Location object based on recency and accuracy.
     */
   protected Location getBetterLocation(Location newLocation, Location currentBestLocation) {
       if (currentBestLocation == null) {
           // A new location is always better than no location
           return newLocation;
       }

       // Check whether the new location fix is newer or older
       long timeDelta = newLocation.getTime() - currentBestLocation.getTime();
       boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
       boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
       boolean isNewer = timeDelta > 0;

       // If it's been more than two minutes since the current location, use the new location
       // because the user has likely moved.
       if (isSignificantlyNewer) {
           return newLocation;
       // If the new location is more than two minutes older, it must be worse
       } else if (isSignificantlyOlder) {
           return currentBestLocation;
       }

       // Check whether the new location fix is more or less accurate
       int accuracyDelta = (int) (newLocation.getAccuracy() - currentBestLocation.getAccuracy());
       boolean isLessAccurate = accuracyDelta > 0;
       boolean isMoreAccurate = accuracyDelta < 0;
       boolean isSignificantlyLessAccurate = accuracyDelta > 200;

       // Check if the old and new location are from the same provider
       boolean isFromSameProvider = isSameProvider(newLocation.getProvider(),
               currentBestLocation.getProvider());

       // Determine location quality using a combination of timeliness and accuracy
       if (isMoreAccurate) {
           return newLocation;
       } else if (isNewer && !isLessAccurate) {
           return newLocation;
       } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
           return newLocation;
       }
       return currentBestLocation;
   }
   
   
   /** Checks whether two providers are the same */
   private boolean isSameProvider(String provider1, String provider2) {
       if (provider1 == null) {
         return provider2 == null;
       }
       return provider1.equals(provider2);
   }
   
   
   private void updateUILocation(Location location) {
       // We're sending the update to a handler which then updates the UI with the new
       // location.
	   if(mHandler != null)
		   Message.obtain(mHandler, UPDATE_LATLNG, location.getLatitude() + "," + location.getLongitude()).sendToTarget();
   }


	
	public void unRegisterLocationListener() {
		mLocationManager.removeUpdates(listener);
	}

	
	public void setOnNoProviderEnabledListener(OnNoProviderEnabledListener listener) {
		this.onNoProviderEnabledListener = listener;
	}
	
	public void setOnLocationChangedListener(OnLocationChangedListener listener) {
		this.onLocationChangedListener = listener;
	}
}
