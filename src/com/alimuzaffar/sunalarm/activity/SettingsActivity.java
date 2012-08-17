package com.alimuzaffar.sunalarm.activity;

import android.os.Bundle;
import android.preference.PreferenceActivity;

import com.alimuzaffar.sunalarm.R;

public class SettingsActivity extends PreferenceActivity {
    @SuppressWarnings("deprecation")
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
    }
}