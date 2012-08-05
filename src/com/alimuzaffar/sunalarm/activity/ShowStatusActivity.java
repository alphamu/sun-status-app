package com.alimuzaffar.sunalarm.activity;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;

import android.app.Activity;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.alimuzaffar.sunalarm.R;
import com.alimuzaffar.sunalarm.receiver.AlarmReceiver;
import com.alimuzaffar.sunalarm.util.AppSettings;
import com.alimuzaffar.sunalarm.util.AppSettings.Key;
import com.alimuzaffar.sunalarm.util.Utils;
import com.luckycatlabs.sunrisesunset.SunriseSunsetCalculator;

public class ShowStatusActivity extends Activity implements OnCheckedChangeListener {
	private static final String		TAG			= "ShowStatusActivity";

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
	}

	@Override
	protected void onResume() {
		super.onResume();
		
		LinearLayout myLayout = (LinearLayout) findViewById(R.id.focussucker);
		myLayout.requestFocus();

		locationManager = (LocationManager) this.getSystemService(LOCATION_SERVICE); // <2>

		Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER); // <5>
		if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
			Utils.buildAlertMessageNoGps(this);
			
		} else if (location != null) {
			Log.d(TAG, location.toString());
			Log.d(TAG, "Time Zone Id: " + TimeZone.getDefault().getID());
			SunriseSunsetCalculator calculator = new SunriseSunsetCalculator(new com.luckycatlabs.sunrisesunset.dto.Location(location.getLatitude(), location.getLongitude()), TimeZone.getDefault().getID());
			
			AppSettings settings = AppSettings.getInstance(getApplicationContext());
			settings.set(Key.LAST_LATITUDE, location.getLatitude());
			settings.set(Key.LAST_LONGITUDE, location.getLongitude());
			
			Calendar cal = Calendar.getInstance();
			todaySunriseCal = calculator.getAstronomicalSunriseCalendarForDate(cal);
			todaySunsetCal = calculator.getOfficialSunsetCalendarForDate(cal);
			
			cal.add(Calendar.DATE, 1);
			tomorrowSunriseCal = calculator.getAstronomicalSunriseCalendarForDate(cal);
			tomorrowSunsetCal = calculator.getOfficialSunsetCalendarForDate(cal);
			
			String dawnText = null;
			boolean dawnToday, duskToday = false;
			if(todaySunriseCal.before(Calendar.getInstance())) {
				nextSunriseCal = tomorrowSunriseCal;
				dawnText = TIME_12HRS.format(nextSunriseCal.getTime());
				dawnToday = false;
			} else {
				nextSunsetCal = todaySunsetCal;
				dawnText = TIME_12HRS.format(nextSunriseCal.getTime());
				dawnToday = true;
			}
			
			String duskText = null;
			if(todaySunsetCal.before(Calendar.getInstance())) {
				nextSunsetCal = tomorrowSunsetCal;
				duskText = TIME_12HRS.format(nextSunsetCal.getTime());
				duskToday = false;
			} else {
				nextSunsetCal = todaySunsetCal;
				duskText = TIME_12HRS.format(nextSunsetCal.getTime());
				duskToday = true;
			}
			
			if(dawnToday) {
				dawnTitle.setText(getString(R.string.s_dawn,getString(R.string.today)));
			} else {
				dawnTitle.setText(getString(R.string.s_dawn,getString(R.string.tomorrow)));
			}
			
			if(duskToday) {
				duskTitle.setText(getString(R.string.s_dusk,getString(R.string.today)));
			} else {
				duskTitle.setText(getString(R.string.s_dusk,getString(R.string.tomorrow)));
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
			public void onTextChanged(CharSequence s, int start, int before, int count) { }
			
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
			
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
			}
		});
		
		delayDuskAlarm.addTextChangedListener(new TextWatcher() {
			
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) { }
			
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
			
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
		//getMenuInflater().inflate(R.menu.show_status, menu);
		//return true;
		return false;
	}

	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		AppSettings settings = AppSettings.getInstance(ShowStatusActivity.this.getApplicationContext());
		if (buttonView.getId() == R.id.dawnAlarmSet) {
			settings.set(Key.DAWN_ALARM, isChecked);
			if (isChecked)
				Utils.setAlarm(getApplicationContext(), nextSunriseCal, Key.DAWN_ALARM.toString());
			else
				Utils.stopAlarm(getApplicationContext(), Key.DAWN_ALARM.toString());

		} else if (buttonView.getId() == R.id.duskAlarmSet) {
			settings.set(Key.DUSK_ALARM, isChecked);
			if (isChecked)
				Utils.setAlarm(getApplicationContext(), nextSunsetCal, Key.DUSK_ALARM.toString());
			else
				Utils.stopAlarm(getApplicationContext(), Key.DUSK_ALARM.toString());
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
