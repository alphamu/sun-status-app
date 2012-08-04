package com.alimuzaffar.sunalarm.activity;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.alimuzaffar.sunalarm.R;
import com.alimuzaffar.sunalarm.receiver.AlarmReceiver;
import com.alimuzaffar.sunalarm.util.Settings;
import com.alimuzaffar.sunalarm.util.Settings.Key;
import com.luckycatlabs.sunrisesunset.SunriseSunsetCalculator;

public class ShowStatusActivity extends Activity implements OnCheckedChangeListener {
	private static final String TAG = "ShowStatusActivity";
	
	LocationManager locationManager;
	
	@SuppressWarnings("unused")
	private static SimpleDateFormat TIME_24HRS = new SimpleDateFormat("HH:mm");
	private static SimpleDateFormat TIME_12HRS = new SimpleDateFormat("hh:mm a");
	
	private TextView duskTime, dawnTime;
	
	private ToggleButton duskAlarmSet, dawnAlarmSet;
	
	private EditText delayDawnAlarm, delayDuskAlarm;
	
	private Calendar sunriseCal;
	private Calendar sunsetCal;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_status);
        
        dawnTime = (TextView)findViewById(R.id.dawnTime);
        duskTime = (TextView)findViewById(R.id.duskTime);
        
        duskAlarmSet = (ToggleButton) findViewById(R.id.duskAlarmSet);
        dawnAlarmSet = (ToggleButton) findViewById(R.id.dawnAlarmSet);
        
        delayDawnAlarm = (EditText) findViewById(R.id.delayDawnAlarm);
        delayDuskAlarm = (EditText) findViewById(R.id.delayDuskAlarm);
        
        bindToggleButtons();
    }
    
    

    @Override
	protected void onResume() {
		super.onResume();

        
        locationManager = (LocationManager)this.getSystemService(LOCATION_SERVICE); //<2>
        
        Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER); //<5>
        if (location != null) {
        	Log.d(TAG, location.toString());
        	Log.d(TAG, "Time Zone Id: "+TimeZone.getDefault().getID());
        	SunriseSunsetCalculator calculator = new SunriseSunsetCalculator(new com.luckycatlabs.sunrisesunset.dto.Location(location.getLatitude(), location.getLongitude()), TimeZone.getDefault().getID());
        	sunriseCal = calculator.getAstronomicalSunriseCalendarForDate(Calendar.getInstance());
        	sunsetCal = calculator.getOfficialSunsetCalendarForDate(Calendar.getInstance());
          
        	dawnTime.setText(TIME_12HRS.format(sunriseCal.getTime()));
        	duskTime.setText(TIME_12HRS.format(sunsetCal.getTime()));
        }
        

	}
    
    private void bindToggleButtons() {
    	duskAlarmSet.setOnCheckedChangeListener(this);
    	dawnAlarmSet.setOnCheckedChangeListener(this);
    	
    	Settings settings = Settings.getInstance(ShowStatusActivity.this);
    	
    	dawnAlarmSet.setChecked(settings.getBoolean(Key.DAWN_ALARM));
    	duskAlarmSet.setChecked(settings.getBoolean(Key.DUSK_ALARM));
    	
    	delayDawnAlarm.setText(String.valueOf(settings.getInt(Key.DAWN_DELAY)));
    	delayDuskAlarm.setText(String.valueOf(settings.getInt(Key.DUSK_DELAY)));
    }


	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.show_status, menu);
        return true;
    }



	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		Settings settings = Settings.getInstance(ShowStatusActivity.this);
		if(buttonView.getId() == R.id.dawnAlarmSet) {
			settings.set(Key.DAWN_ALARM, isChecked);
			if(isChecked)
				setAlarm(sunriseCal, Key.DAWN_ALARM.toString());
			else
				stopAlarm(Key.DAWN_ALARM.toString());
				
		} else if (buttonView.getId() ==  R.id.duskAlarmSet) {
			settings.set(Key.DUSK_ALARM, isChecked);
			if(isChecked)
				setAlarm(sunsetCal, Key.DUSK_ALARM.toString());
			else
				stopAlarm(Key.DAWN_ALARM.toString());
		}
	}
	
	private void setAlarm(Calendar calendar, String type) {
		Intent intent = new Intent(this, AlarmReceiver.class);
		intent.putExtra("alarm_type", type);
		PendingIntent sender = PendingIntent.getBroadcast(this, 0, intent, 0);
		
		AlarmManager alarmManager = (AlarmManager) ShowStatusActivity.this.getSystemService(Context.ALARM_SERVICE);
		alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), sender);
	}
	
	private void stopAlarm(String type) {
		Intent intent = new Intent(this, AlarmReceiver.class);
		intent.putExtra("alarm_type", type);
		PendingIntent sender = PendingIntent.getBroadcast(this, 0, intent, 0);
		
		AlarmManager alarmManager = (AlarmManager) ShowStatusActivity.this.getSystemService(Context.ALARM_SERVICE);
		alarmManager.cancel(sender);
	}

    
}
