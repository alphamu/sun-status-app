package com.alimuzaffar.sunalarm.util;

import android.content.Context;
import android.content.SharedPreferences;

public class AppSettings {
	public static boolean DEBUG = false;
	
	private static final String	SETTINGS_NAME	= "SUNALARM_SETTINGS";
	private static AppSettings		settings;
	private SharedPreferences	pref;

	public enum Key {
			DAWN_ALARM,
			DUSK_ALARM,
			DAWN_DELAY,
			DUSK_DELAY,
			LAST_LONGITUDE,
			LAST_LATITUDE,
			RATER_DONTSHOWAGAIN,
			RATER_LAUNCHCOUNT,
			RATHER_DATEFIRSTLAUNCH,
			MANUAL_LOCATION,
			MANUAL_LOCATION_NAME,
			TIMEZONE_ID;
			
	}

	public AppSettings(Context context) {
		pref = context.getSharedPreferences(SETTINGS_NAME, 0);
	}

	public static AppSettings getInstance(Context context) {
		if (settings == null) {
			settings = new AppSettings(context);
		}
		return settings;
	}

	public void set(Key key, String val) {
		SharedPreferences.Editor editor = pref.edit();
		editor.putString(key.toString(), val);
		editor.commit();
	}

	public void set(Key key, int val) {
		SharedPreferences.Editor editor = pref.edit();
		editor.putInt(key.toString(), val);
		editor.commit();
	}

	public void set(Key key, boolean val) {
		SharedPreferences.Editor editor = pref.edit();
		editor.putBoolean(key.toString(), val);
		editor.commit();
	}

	public void set(Key key, float val) {
		SharedPreferences.Editor editor = pref.edit();
		editor.putFloat(key.toString(), val);
		editor.commit();
	}
	
	public void set(Key key, double val) {
		SharedPreferences.Editor editor = pref.edit();
		editor.putString(key.toString(), String.valueOf(val));
		editor.commit();
	}

	public void set(Key key, long val) {
		SharedPreferences.Editor editor = pref.edit();
		editor.putLong(key.toString(), val);
		editor.commit();
	}
	
	public String getString(Key key) {
		return pref.getString(key.toString(), null);
	}
	
	public String getString(Key key, String defaultValue) {
		return pref.getString(key.toString(), defaultValue);
	}
	
	public String getString(String key) {
		return pref.getString(key, null);
	}	

	public int getInt(Key key) {
		return pref.getInt(key.toString(), 0);
	}
	
	public long getLong(Key key) {
		return pref.getLong(key.toString(), 0);
	}
	
	public float getFloat(Key key) {
		return pref.getFloat(key.toString(), 0);
	}
	
	public double getDouble(Key key) {
		try {
		return Double.valueOf(pref.getString(key.toString(), "0"));
		} catch (NumberFormatException nfe) {
			return 0;
		}
	}
		
	public boolean getBoolean(Key key) {
		return pref.getBoolean(key.toString(), false);
	}
	
	public boolean getBoolean(Key key, boolean defaultValue) {
		return pref.getBoolean(key.toString(), defaultValue);
	}
	
	public boolean getBoolean(String key) {
		return pref.getBoolean(key, false);
	}
}
