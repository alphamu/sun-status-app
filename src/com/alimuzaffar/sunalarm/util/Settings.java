package com.alimuzaffar.sunalarm.util;

import android.content.Context;
import android.content.SharedPreferences;

public class Settings {
	private static final String	SETTINGS_NAME	= "SUNALARM_SETTINGS";
	private static Settings		settings;
	private SharedPreferences	pref;

	public enum Key {
			DAWN_ALARM,
			DUSK_ALARM,
			DAWN_DELAY,
			DUSK_DELAY;
	}

	public Settings(Context context) {
		pref = context.getSharedPreferences(SETTINGS_NAME, 0);
	}

	public static Settings getInstance(Context context) {
		if (settings == null) {
			settings = new Settings(context);
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

	public void set(Key key, long val) {
		SharedPreferences.Editor editor = pref.edit();
		editor.putLong(key.toString(), val);
		editor.commit();
	}
	
	public String getString(Key key) {
		return pref.getString(key.toString(), null);
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
	
	public boolean getBoolean(Key key) {
		return pref.getBoolean(key.toString(), false);
	}
}
