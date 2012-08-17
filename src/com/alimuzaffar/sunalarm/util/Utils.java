package com.alimuzaffar.sunalarm.util;

import java.util.Calendar;
import java.util.TimeZone;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import com.alimuzaffar.sunalarm.receiver.AlarmReceiver;
import com.alimuzaffar.sunalarm.util.AppSettings.Key;
import com.luckycatlabs.sunrisesunset.SunriseSunsetCalculator;

public class Utils {
	public static void buildAlertMessageNoGps(final Activity context) {
		final AlertDialog.Builder builder = new AlertDialog.Builder(context);
		String message = "Your GPS seems to be disabled, do you want to enable it?";
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
		if(AppSettings.DEBUG) {
			alarmCal.setTimeInMillis(System.currentTimeMillis());
			alarmCal.add(Calendar.SECOND, 10);
		}
		alarmManager.set(AlarmManager.RTC_WAKEUP, alarmCal.getTimeInMillis(), sender);
		
		Toast.makeText(context, type+" set for "+alarmCal.getTime(), Toast.LENGTH_SHORT).show();
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

		Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER); // <5>
		SunriseSunsetCalculator calculator = null;
		
		AppSettings appSettings = AppSettings.getInstance(context);
		
		if (!locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
			calculator = new SunriseSunsetCalculator(new com.luckycatlabs.sunrisesunset.dto.Location(appSettings.getDouble(Key.LAST_LATITUDE), appSettings.getDouble(Key.LAST_LONGITUDE)), TimeZone.getDefault().getID());
		} else if (location != null) {
			calculator = new SunriseSunsetCalculator(new com.luckycatlabs.sunrisesunset.dto.Location(location.getLatitude(), location.getLongitude()), TimeZone.getDefault().getID());
		}
		
		if(calculator == null) {
			calculator = new SunriseSunsetCalculator(new com.luckycatlabs.sunrisesunset.dto.Location(appSettings.getDouble(Key.LAST_LATITUDE), appSettings.getDouble(Key.LAST_LONGITUDE)), TimeZone.getDefault().getID());
		}
		
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DATE, 1);
		Calendar nextAlarmCal;
		if(alarmType.equals(Key.DAWN_ALARM.toString())) {
			nextAlarmCal = Utils.getSunrise(context, calculator, cal);
		} else {
			nextAlarmCal = Utils.getSunset(context, calculator, cal);
		}
		
		Log.d("Utils", "Next Alarm "+nextAlarmCal.getTime().toString());
		
		Utils.setAlarm(context, nextAlarmCal, alarmType);
	}

	public static boolean checkInternet(Context context) {

	    ConnectivityManager connec = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
	    android.net.NetworkInfo wifi = connec.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
	    android.net.NetworkInfo mobile = connec.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);

	    // Here if condition check for wifi and mobile network is available or not.
	    // If anyone of them is available or connected then it will return true, otherwise false;

	    if (wifi.isConnected()) {
	        return true;
	    } else if (mobile.isConnected()) {
	        return true;
	    }
	    return false;
	}
	
	public static Calendar getSunrise(Context context, SunriseSunsetCalculator calculator, Calendar cal) {
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
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
	
	public static Calendar getSunset(Context context, SunriseSunsetCalculator calculator, Calendar cal) {
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
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

}
