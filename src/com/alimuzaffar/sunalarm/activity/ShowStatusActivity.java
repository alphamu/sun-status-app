package com.alimuzaffar.sunalarm.activity;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.media.AudioManager;
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
import com.alimuzaffar.sunalarm.util.AppRater;
import com.alimuzaffar.sunalarm.util.AppSettings;
import com.alimuzaffar.sunalarm.util.AppSettings.Key;
import com.alimuzaffar.sunalarm.util.ChangeLog;
import com.alimuzaffar.sunalarm.util.LocationUtils;
import com.alimuzaffar.sunalarm.util.OnNoProviderEnabledListener;
import com.alimuzaffar.sunalarm.util.UserLocation;
import com.alimuzaffar.sunalarm.util.UserLocation.OnLocationChangedListener;
import com.alimuzaffar.sunalarm.util.Utils;
import com.luckycatlabs.sunrisesunset.SunriseSunsetCalculator;

public class ShowStatusActivity extends Activity implements OnCheckedChangeListener {
	private static final String		TAG			= "ShowStatusActivity";
	private static int SETTINGS = 20120808;

	@SuppressWarnings("unused")
	public static SimpleDateFormat	TIME_24HRS	= new SimpleDateFormat("HH:mm", Locale.getDefault());
	public static SimpleDateFormat	TIME_12HRS	= new SimpleDateFormat("hh:mm a", Locale.getDefault());

	private TextView				duskTime, dawnTime, duskTitle, dawnTitle;
	private View					duskTimeProgress, dawnTimeProgress;

	private CompoundButton			duskAlarmSet, dawnAlarmSet;

	private EditText				delayDawnAlarm, delayDuskAlarm;

	private Calendar				todaySunriseCal;
	private Calendar				todaySunsetCal;
	private Calendar				tomorrowSunriseCal;
	private Calendar				tomorrowSunsetCal;
	private Calendar				nextSunriseCal;
	private Calendar				nextSunsetCal;
	
	SunriseSunsetCalculator calculator = null;
	
	LocationListener coarseListener;
	
	UserLocation mUserLocation;
	private static boolean FOUND_LOCATION = false;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_show_status);
		setVolumeControlStream(AudioManager.STREAM_ALARM);
		
		//initialize preferences
		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

		dawnTime = (TextView) findViewById(R.id.dawnTime);
		duskTime = (TextView) findViewById(R.id.duskTime);
		
		dawnTimeProgress = findViewById(R.id.dawnTimeProgress);
		duskTimeProgress = findViewById(R.id.duskTimeProgress);

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
	    
	    mUserLocation = new UserLocation(this, null);

	    AppRater.app_launched(this);
	}

	@Override
	protected void onResume() {
		super.onResume();
		
		final AppSettings settings = AppSettings.getInstance(getApplicationContext());
		
	    mUserLocation.setOnLocationChangedListener(new OnLocationChangedListener() {
			
			@Override
			public void onLocationChanged(Location location) {
		        // do something here to save this new location
				FOUND_LOCATION = true;
		      	calculator = new SunriseSunsetCalculator(new com.luckycatlabs.sunrisesunset.dto.Location(location.getLatitude(), location.getLongitude()), TimeZone.getDefault().getID());
				calculate();
				settings.set(Key.LAST_LATITUDE, location.getLatitude());
				settings.set(Key.LAST_LONGITUDE, location.getLongitude());
				
				dawnTimeProgress.setVisibility(View.INVISIBLE);
				duskTimeProgress.setVisibility(View.INVISIBLE);
				dawnTime.setVisibility(View.VISIBLE);
				duskTime.setVisibility(View.VISIBLE);				
			}
		});
	    
	    mUserLocation.setOnNoProviderEnabledListener(new OnNoProviderEnabledListener() {
			
			@Override
			public void onNoProviderEnabled() {
				Utils.buildAlertMessageNoGps(ShowStatusActivity.this, new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						
						if(settings.getDouble(Key.LAST_LATITUDE) != 0 && settings.getDouble(Key.LAST_LONGITUDE) != 0) {
							//use saved location.
							calculator = new SunriseSunsetCalculator(new com.luckycatlabs.sunrisesunset.dto.Location(settings.getDouble(Key.LAST_LATITUDE), settings.getDouble(Key.LAST_LONGITUDE)), TimeZone.getDefault().getID());
							calculate();
							dawnTimeProgress.setVisibility(View.INVISIBLE);
							duskTimeProgress.setVisibility(View.INVISIBLE);
							dawnTime.setVisibility(View.VISIBLE);
							duskTime.setVisibility(View.VISIBLE);	
						} else {
							// disable everything, no saved location, no way to get it.
							dawnTime.setVisibility(View.INVISIBLE);
							duskTime.setVisibility(View.INVISIBLE);
							dawnTimeProgress.setVisibility(View.INVISIBLE);
							duskTimeProgress.setVisibility(View.INVISIBLE);
							duskAlarmSet.setEnabled(false);
							dawnAlarmSet.setEnabled(false);
							delayDawnAlarm.setEnabled(false);
							delayDuskAlarm.setEnabled(false);
						}
					}
					
				});

			}
		});
		
		mUserLocation.registerLocationListener();

		// enable everything
		duskAlarmSet.setEnabled(true);
		dawnAlarmSet.setEnabled(true);
		delayDawnAlarm.setEnabled(true);
		delayDuskAlarm.setEnabled(true);

		LinearLayout myLayout = (LinearLayout) findViewById(R.id.focussucker);
		myLayout.requestFocus();

		//if no loaction is available, initialize with saved settings.
		if(!FOUND_LOCATION && settings.getDouble(Key.LAST_LATITUDE) != 0 && settings.getDouble(Key.LAST_LATITUDE) != 0) {
			calculator = new SunriseSunsetCalculator(new com.luckycatlabs.sunrisesunset.dto.Location(settings.getDouble(Key.LAST_LATITUDE), settings.getDouble(Key.LAST_LONGITUDE)), TimeZone.getDefault().getID());
			calculate();
			dawnTimeProgress.setVisibility(View.INVISIBLE);
			duskTimeProgress.setVisibility(View.INVISIBLE);
			dawnTime.setVisibility(View.VISIBLE);
			duskTime.setVisibility(View.VISIBLE);	
		}
		
		Log.d(TAG, "Time Zone Id: " + TimeZone.getDefault().getID());
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		
		mUserLocation.unRegisterLocationListener();
		mUserLocation.setOnLocationChangedListener(null);
		mUserLocation.setOnNoProviderEnabledListener(null);
	}
	
	private void calculate() {
		if (calculator != null) {
			Calendar cal = Calendar.getInstance();

			todaySunriseCal = Utils.getSunrise(this, calculator, cal);
			todaySunsetCal = Utils.getSunset(this, calculator, cal);

			cal.add(Calendar.DATE, 1);
			tomorrowSunriseCal = Utils.getSunrise(this, calculator, cal);
			tomorrowSunsetCal = Utils.getSunset(this, calculator, cal);
			
			if (todaySunriseCal == null || todaySunsetCal == null || tomorrowSunriseCal == null || tomorrowSunsetCal == null) {
				Toast.makeText(this, "Something went wrong. Cannot calculate timing.", Toast.LENGTH_LONG).show();
				return;
			}

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
		} else if(item.getItemId() == R.id.menu_feedback) {
			final Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);

			/* Fill it with Data */
			emailIntent.setType("plain/text");
			emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{"ali@muzaffar.me"});
			emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Sehri and Iftar Alarm - Feedback");
			emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, "I'd like to report a bug or request a feature.");

			/* Send it off to the Activity-Chooser */
			startActivity(Intent.createChooser(emailIntent, "Send mail..."));
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
		if (dawn && settings.getBoolean(Key.DAWN_ALARM) && nextSunriseCal != null){
			Utils.stopAlarm(getApplicationContext(), Key.DAWN_ALARM.toString());
			Utils.setAlarm(getApplicationContext(), nextSunriseCal, Key.DAWN_ALARM.toString());
		}
		
		if (dusk && settings.getBoolean(Key.DUSK_ALARM) && nextSunsetCal != null) {
			Utils.stopAlarm(getApplicationContext(), Key.DUSK_ALARM.toString());
			Utils.setAlarm(getApplicationContext(), nextSunsetCal, Key.DUSK_ALARM.toString());
		}
		
		if(nextSunriseCal == null || nextSunsetCal == null) {
			Toast.makeText(this, "ERROR: Alarm has not have been set.\nUnable to determine alarm times.\nEnable GPS and try again.", Toast.LENGTH_LONG).show();
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
		
		if(nextSunriseCal == null || nextSunsetCal == null) {
			Toast.makeText(this, "ERROR: Alarm has not have been set.\nUnable to determine alarm times.\nEnable GPS and try again.", Toast.LENGTH_LONG).show();
		}
	}

	private void bindTestButtons() {
		Button testDawn = ((Button) findViewById(R.id.testDawn));
		testDawn.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent intent = new Intent(ShowStatusActivity.this, AlarmReceiver.class);
				intent.putExtra("alarm_type", Key.DAWN_ALARM.toString());//
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
