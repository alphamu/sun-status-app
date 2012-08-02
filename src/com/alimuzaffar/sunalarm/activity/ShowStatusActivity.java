package com.alimuzaffar.sunalarm.activity;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;

import android.app.Activity;
import android.content.ComponentCallbacks;
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
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.show_status);
        
        dawnTime = (TextView)findViewById(R.id.dawnTime);
        duskTime = (TextView)findViewById(R.id.duskTime);
        
        duskAlarmSet = (ToggleButton) findViewById(R.id.duskAlarmSet);
        dawnAlarmSet = (ToggleButton) findViewById(R.id.dawnAlarmSet);
        
        delayDawnAlarm = (EditText) findViewById(R.id.delayDawnAlarm);
        delayDuskAlarm = (EditText) findViewById(R.id.delayDuskAlarm);
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
        	Calendar sunriseCal = calculator.getAstronomicalSunriseCalendarForDate(Calendar.getInstance());
        	Calendar sunsetCal = calculator.getOfficialSunsetCalendarForDate(Calendar.getInstance());
          
        	dawnTime.setText(TIME_12HRS.format(sunriseCal.getTime()));
        	duskTime.setText(TIME_12HRS.format(sunsetCal.getTime()));
        }
        
        bindToggleButtons();
	}
    
    private void bindToggleButtons() {
    	duskAlarmSet.setOnCheckedChangeListener(this);
    	dawnAlarmSet.setOnCheckedChangeListener(this);
    }


	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.show_status, menu);
        return true;
    }



	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		
		
	}

    
}
