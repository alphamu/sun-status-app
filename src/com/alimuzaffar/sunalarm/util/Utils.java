package com.alimuzaffar.sunalarm.util;

import java.util.Calendar;
import java.util.TimeZone;

import com.alimuzaffar.sunalarm.receiver.AlarmReceiver;
import com.alimuzaffar.sunalarm.util.AppSettings.Key;
import com.luckycatlabs.sunrisesunset.SunriseSunsetCalculator;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.provider.Settings;
import android.widget.Toast;

public class Utils {
	public static void buildAlertMessageNoGps(final Activity context) {
		final AlertDialog.Builder builder = new AlertDialog.Builder(context);
		String message = "Yout GPS seems to be disabled, do you want to enable it?";
		AppSettings settings = AppSettings.getInstance(context.getApplicationContext());
		
		if(settings.getDouble(Key.LAST_LATITUDE) != 0 && settings.getDouble(Key.LAST_LONGITUDE) != 0) {
			message += "\nYou appear to have a saved location.\nSelecting 'No' will use the saved location.";
		} else {
			message += "\nYou do not appear to have a saved location.\nIf you select 'No' the application will not work.";
		}
		
		builder.setMessage(message).setCancelable(false).setPositiveButton("Yes", new DialogInterface.OnClickListener() {
			public void onClick(final DialogInterface dialog, final int id) {
				context.startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
			}
		}).setNegativeButton("No", new DialogInterface.OnClickListener() {
			public void onClick(final DialogInterface dialog, final int id) {
				dialog.cancel();
			}
		});
		final AlertDialog alert = builder.create();
		alert.show();
	}
	
	public static void setAlarm(Context context, Calendar calendar, String type) {
		Intent intent = new Intent(context, AlarmReceiver.class);
		intent.putExtra("alarm_type", type);
		PendingIntent sender = PendingIntent.getBroadcast(context, 0, intent, 0);
		
		Calendar alarmCal = Calendar.getInstance();
		alarmCal.setTimeInMillis(calendar.getTimeInMillis());
		
		AppSettings appSettings = AppSettings.getInstance(context);
		
		if(type.equals(Key.DAWN_ALARM.toString())) {
			alarmCal.add(Calendar.MINUTE, appSettings.getInt(Key.DAWN_DELAY));
		} else {
			alarmCal.add(Calendar.MINUTE, appSettings.getInt(Key.DUSK_DELAY));
		}
		
		AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		alarmManager.set(AlarmManager.RTC_WAKEUP, alarmCal.getTimeInMillis(), sender);
		
		Toast.makeText(context, type+" set for "+alarmCal.getTime(), Toast.LENGTH_LONG).show();
	}

	public static void stopAlarm(Context context, String type) {
		Intent intent = new Intent(context, AlarmReceiver.class);
		intent.putExtra("alarm_type", type);
		PendingIntent sender = PendingIntent.getBroadcast(context, 0, intent, 0);

		AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		alarmManager.cancel(sender);
	}
	
	public static void setAlarm(Context context, String alarmType) {
		LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE); // <2>

		Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER); // <5>
		SunriseSunsetCalculator calculator = null;
		
		AppSettings appSettings = AppSettings.getInstance(context);
		
		if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
			calculator = new SunriseSunsetCalculator(new com.luckycatlabs.sunrisesunset.dto.Location(appSettings.getDouble(Key.LAST_LATITUDE), appSettings.getDouble(Key.LAST_LONGITUDE)), TimeZone.getDefault().getID());
		} else if (location != null) {
			calculator = new SunriseSunsetCalculator(new com.luckycatlabs.sunrisesunset.dto.Location(location.getLatitude(), location.getLongitude()), TimeZone.getDefault().getID());
		}
		
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DATE, 1);
		Calendar nextAlarmCal;
		if(alarmType.equals(Key.DAWN_ALARM)) {
			nextAlarmCal = calculator.getAstronomicalSunriseCalendarForDate(cal);
		} else {
			nextAlarmCal = calculator.getOfficialSunsetCalendarForDate(cal);
		}
		
		Utils.setAlarm(context, nextAlarmCal, alarmType);
	}

}
