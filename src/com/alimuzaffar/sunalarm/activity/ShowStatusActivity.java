package com.alimuzaffar.sunalarm.activity;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.alimuzaffar.sunalarm.R;
import com.alimuzaffar.sunalarm.receiver.AlarmReceiver;
import com.alimuzaffar.sunalarm.util.AppSettings;
import com.alimuzaffar.sunalarm.util.AppSettings.Key;
import com.alimuzaffar.sunalarm.util.AppRater;
import com.alimuzaffar.sunalarm.util.ChangeLog;
import com.alimuzaffar.sunalarm.util.LocationUtils;
import com.alimuzaffar.sunalarm.util.Utils;
import com.luckycatlabs.sunrisesunset.SunriseSunsetCalculator;

public class ShowStatusActivity extends Activity implements OnCheckedChangeListener {
	private static final String		TAG			= "ShowStatusActivity";
	private static int SETTINGS = 20120808;

	LocationManager					locationManager;

	@SuppressWarnings("unused")
	private static SimpleDateFormat	TIME_24HRS	= new SimpleDateFormat("HH:mm");
	private static SimpleDateFormat	TIME_12HRS	= new SimpleDateFormat("hh:mm a");

	private TextView				duskTime, dawnTime, duskTitle, dawnTitle;

	private CompoundButton			duskAlarmSet, dawnAlarmSet;

	private EditText				delayDawnAlarm, delayDuskAlarm;

	private Calendar				todaySunriseCal;
	private Calendar				todaySunsetCal;
	private Calendar				tomorrowSunriseCal;
	private Calendar				tomorrowSunsetCal;
	private Calendar				nextSunriseCal;
	private Calendar				nextSunsetCal;
	
	private static boolean initialGPSCheck = false;
	
	SunriseSunsetCalculator calculator = null;
	
	LocationListener coarseListener;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_show_status);

		dawnTime = (TextView) findViewById(R.id.dawnTime);
		duskTime = (TextView) findViewById(R.id.duskTime);

		dawnTitle = (TextView) findViewById(R.id.dawn);
		duskTitle = (TextView) findViewById(R.id.dusk);

		duskAlarmSet = (CompoundButton) findViewById(R.id.duskAlarmSet);
		dawnAlarmSet = (CompoundButton) findViewById(R.id.dawnAlarmSet);

		delayDawnAlarm = (EditText) findViewById(R.id.delayDawnAlarm);
		delayDuskAlarm = (EditText) findViewById(R.id.delayDuskAlarm);

		bindToggleButtons();
		
		ChangeLog cl = new ChangeLog(this);
	    if (cl.firstRun())
	        cl.getLogDialog().show();
	    
	    PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
	    
	    AppRater.app_launched(this);
	}

	@Override
	protected void onResume() {
		super.onResume();

		// enable everything
		duskAlarmSet.setEnabled(true);
		dawnAlarmSet.setEnabled(true);
		delayDawnAlarm.setEnabled(true);
		delayDuskAlarm.setEnabled(true);

		LinearLayout myLayout = (LinearLayout) findViewById(R.id.focussucker);
		myLayout.requestFocus();

		final AppSettings settings = AppSettings.getInstance(getApplicationContext());		

		locationManager = (LocationManager) this.getSystemService(LOCATION_SERVICE); // <2>
		
		if(AppSettings.DEBUG) {
			Log.d(TAG, "Enabled Providers:");
			for(String p : locationManager.getProviders(true)) {
				Log.d(TAG, p);
			}
			
			Log.d(TAG, "All Providers");
			for(String p : locationManager.getProviders(false)) {
				Log.d(TAG, p);
			}
		}
		
		//low accuracy provided used only.
		LocationProvider low;
		try {
			low = locationManager.getProvider(locationManager.getBestProvider(LocationUtils.createCoarseCriteria(),true));
		} catch(IllegalArgumentException iae) {
			low = locationManager.getProvider(LocationManager.NETWORK_PROVIDER);
		}
		String providerName = LocationManager.NETWORK_PROVIDER;
		if(low != null && low.getName() != null) {
			if(AppSettings.DEBUG)
				Log.d(TAG, "Low was not null and low.getName()="+low.getName());
			providerName = low.getName();
		}
		if(providerName == null)
			providerName = LocationManager.GPS_PROVIDER;
		
		if(AppSettings.DEBUG)
			Log.d(TAG, "final provider name="+providerName);

		//Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER); // <5>
		Location location = locationManager.getLastKnownLocation(providerName);
		
		if(location == null && settings.getDouble(Key.LAST_LATITUDE) != 0 && settings.getDouble(Key.LAST_LATITUDE) != 0) {
			calculator = new SunriseSunsetCalculator(new com.luckycatlabs.sunrisesunset.dto.Location(settings.getDouble(Key.LAST_LATITUDE), settings.getDouble(Key.LAST_LONGITUDE)), TimeZone.getDefault().getID());
			calculate();
		} else if (location != null) {
			calculate();
		}
		
		if(AppSettings.DEBUG)
			Log.d(TAG, "locationManager.isProviderEnabled()="+locationManager.isProviderEnabled(providerName));
		
		if (!locationManager.isProviderEnabled(providerName) /* || !Utils.checkInternet(this)*/) {
			if(!initialGPSCheck)
				Utils.buildAlertMessageNoGps(this);
			if (settings.getDouble(Key.LAST_LATITUDE) != 0 && settings.getDouble(Key.LAST_LATITUDE) != 0) {
				calculator = new SunriseSunsetCalculator(new com.luckycatlabs.sunrisesunset.dto.Location(settings.getDouble(Key.LAST_LATITUDE), settings.getDouble(Key.LAST_LONGITUDE)), TimeZone.getDefault().getID());
				initialGPSCheck = true;
				calculate();
			} else {
				// disable everything
				duskAlarmSet.setEnabled(false);
				dawnAlarmSet.setEnabled(false);
				delayDawnAlarm.setEnabled(false);
				delayDuskAlarm.setEnabled(false);
			}

		}
		
		if(location == null) {
			Toast.makeText(this, "Fetching location to calculate times. This can take a while!", Toast.LENGTH_LONG).show();
		}
		
		// using low accuracy provider... to listen for updates
		locationManager.requestLocationUpdates(providerName, 0, 0f,
		      coarseListener = new LocationListener() {
		      public void onLocationChanged(Location location) {
		        // do something here to save this new location
		      	calculator = new SunriseSunsetCalculator(new com.luckycatlabs.sunrisesunset.dto.Location(location.getLatitude(), location.getLongitude()), TimeZone.getDefault().getID());
				settings.set(Key.LAST_LATITUDE, location.getLatitude());
				settings.set(Key.LAST_LONGITUDE, location.getLongitude());
				calculate();
				locationManager.removeUpdates(coarseListener);
		      }
		      public void onStatusChanged(String s, int i, Bundle bundle) {
		 
		      }
		      public void onProviderEnabled(String s) {
		    	  // try switching to a different provider
		      }
		      public void onProviderDisabled(String s) {
		    	  // try switching to a different provider
		      }
		  });

		Log.d(TAG, "Time Zone Id: " + TimeZone.getDefault().getID());
	}
	
	private void calculate() {
		if (calculator != null) {
			Calendar cal = Calendar.getInstance();

			todaySunriseCal = getSunrise(cal);
			todaySunsetCal = getSunset(cal);

			cal.add(Calendar.DATE, 1);
			tomorrowSunriseCal = getSunrise(cal);
			tomorrowSunsetCal = getSunset(cal);

			String dawnText = null;
			boolean dawnToday, duskToday = false;
			if (todaySunriseCal.before(Calendar.getInstance())) {
				nextSunriseCal = tomorrowSunriseCal;
				dawnText = TIME_12HRS.format(nextSunriseCal.getTime());
				dawnToday = false;
			} else {
				nextSunriseCal = todaySunriseCal;
				dawnText = TIME_12HRS.format(nextSunriseCal.getTime());
				dawnToday = true;
			}

			String duskText = null;
			if (todaySunsetCal.before(Calendar.getInstance())) {
				nextSunsetCal = tomorrowSunsetCal;
				duskText = TIME_12HRS.format(nextSunsetCal.getTime());
				duskToday = false;
			} else {
				nextSunsetCal = todaySunsetCal;
				duskText = TIME_12HRS.format(nextSunsetCal.getTime());
				duskToday = true;
			}

			if (dawnToday) {
				dawnTitle.setText(getString(R.string.s_dawn, getString(R.string.today)));
			} else {
				dawnTitle.setText(getString(R.string.s_dawn, getString(R.string.tomorrow)));
			}

			if (duskToday) {
				duskTitle.setText(getString(R.string.s_dusk, getString(R.string.today)));
			} else {
				duskTitle.setText(getString(R.string.s_dusk, getString(R.string.tomorrow)));
			}

			dawnTime.setText(dawnText);
			duskTime.setText(duskText);
		}
	}

	private void bindToggleButtons() {

		AppSettings settings = AppSettings.getInstance(ShowStatusActivity.this.getApplicationContext());

		dawnAlarmSet.setChecked(settings.getBoolean(Key.DAWN_ALARM));
		duskAlarmSet.setChecked(settings.getBoolean(Key.DUSK_ALARM));

		delayDawnAlarm.setText(String.valueOf(settings.getInt(Key.DAWN_DELAY)));
		delayDuskAlarm.setText(String.valueOf(settings.getInt(Key.DUSK_DELAY)));

		duskAlarmSet.setOnCheckedChangeListener(this);
		dawnAlarmSet.setOnCheckedChangeListener(this);

		delayDawnAlarm.addTextChangedListener(new TextWatcher() {

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

			@Override
			public void afterTextChanged(Editable s) {
				AppSettings settings = AppSettings.getInstance(getApplicationContext());
				int num = 0;
				try {
					num = Integer.parseInt(s.toString());
				} catch (NumberFormatException nfe) {
					num = 0;
				}
				settings.set(Key.DAWN_DELAY, num);
				if (settings.getBoolean(Key.DAWN_ALARM)) {
					updateAlarms(true, false);
				}
			}
		});

		delayDuskAlarm.addTextChangedListener(new TextWatcher() {

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

			@Override
			public void afterTextChanged(Editable s) {
				AppSettings settings = AppSettings.getInstance(getApplicationContext());
				int num = 0;
				try {
					num = Integer.parseInt(s.toString());
				} catch (NumberFormatException nfe) {
					num = 0;
				}
				settings.set(Key.DUSK_DELAY, num);
				if (settings.getBoolean(Key.DUSK_ALARM)) {
					updateAlarms(false, true);
				}
			}
		});

		if (AppSettings.DEBUG) {
			bindTestButtons();
		} else {
			removeTestButtons();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.show_status, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if(item.getItemId() == R.id.menu_settings) {
			Intent intent = new Intent(this, SettingsActivity.class);
			startActivityForResult(intent, SETTINGS);
		}
		return false;
	}
	
	

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if(requestCode == SETTINGS) {
			calculate();
			updateAlarms(true, true);
			
		}
	}
	
	private void updateAlarms(boolean dawn, boolean dusk) {
		AppSettings settings = AppSettings.getInstance(getApplicationContext());
		if (dawn && settings.getBoolean(Key.DAWN_ALARM)){
			Utils.stopAlarm(getApplicationContext(), Key.DAWN_ALARM.toString());
			Utils.setAlarm(getApplicationContext(), nextSunriseCal, Key.DAWN_ALARM.toString());
		}
		
		if (dusk && settings.getBoolean(Key.DAWN_ALARM)) {
			Utils.stopAlarm(getApplicationContext(), Key.DUSK_ALARM.toString());
			Utils.setAlarm(getApplicationContext(), nextSunsetCal, Key.DUSK_ALARM.toString());
		}
	}

	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		AppSettings settings = AppSettings.getInstance(ShowStatusActivity.this.getApplicationContext());
		if (buttonView.getId() == R.id.dawnAlarmSet && nextSunriseCal != null) {
			settings.set(Key.DAWN_ALARM, isChecked);
			if (isChecked)
				Utils.setAlarm(getApplicationContext(), nextSunriseCal, Key.DAWN_ALARM.toString());
			else
				Utils.stopAlarm(getApplicationContext(), Key.DAWN_ALARM.toString());

		} else if (buttonView.getId() == R.id.duskAlarmSet && nextSunsetCal != null) {
			settings.set(Key.DUSK_ALARM, isChecked);
			if (isChecked)
				Utils.setAlarm(getApplicationContext(), nextSunsetCal, Key.DUSK_ALARM.toString());
			else
				Utils.stopAlarm(getApplicationContext(), Key.DUSK_ALARM.toString());
		}
	}
	
	private Calendar getSunrise(Calendar cal) {
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
		switch(Integer.valueOf(settings.getString("pref_dawnZenith", "108"))) {
		case 108:
			return calculator.getAstronomicalSunriseCalendarForDate(cal);
		case 102:
			return calculator.getNauticalSunriseCalendarForDate(cal);
		case 96:
			return calculator.getCivilSunriseCalendarForDate(cal);
		default:
			return calculator.getOfficialSunriseCalendarForDate(cal);
		}
		
		
	}
	
	private Calendar getSunset(Calendar cal) {
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
		switch(Integer.valueOf(settings.getString("pref_duskZenith", "91"))) {
		case 108:
			return calculator.getAstronomicalSunsetCalendarForDate(cal);
		case 102:
			return calculator.getNauticalSunsetCalendarForDate(cal);
		case 96:
			return calculator.getCivilSunsetCalendarForDate(cal);
		default:
			return calculator.getOfficialSunsetCalendarForDate(cal);
		}	
	}

	private void bindTestButtons() {
		Button testDawn = ((Button) findViewById(R.id.testDawn));
		testDawn.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent intent = new Intent(ShowStatusActivity.this, AlarmReceiver.class);
				intent.putExtra("alarm_type", Key.DAWN_ALARM.toString());
				sendBroadcast(intent);
			}
		});

		Button testDusk = ((Button) findViewById(R.id.testDusk));
		testDusk.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent intent = new Intent(ShowStatusActivity.this, AlarmReceiver.class);
				intent.putExtra("alarm_type", Key.DUSK_ALARM.toString());
				sendBroadcast(intent);
			}
		});
	}

	private void removeTestButtons() {
		TableRow tableRow7 = ((TableRow) findViewById(R.id.tableRow7));
		tableRow7.setVisibility(View.GONE);
	}
}
