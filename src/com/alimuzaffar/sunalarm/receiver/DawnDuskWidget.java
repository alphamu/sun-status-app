package com.alimuzaffar.sunalarm.receiver;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.widget.RemoteViews;

import com.alimuzaffar.sunalarm.R;
import com.alimuzaffar.sunalarm.activity.AlarmActivity;
import com.alimuzaffar.sunalarm.activity.ShowStatusActivity;
import com.alimuzaffar.sunalarm.util.AppSettings;
import com.alimuzaffar.sunalarm.util.Utils;
import com.alimuzaffar.sunalarm.util.AppSettings.Key;
import com.luckycatlabs.sunrisesunset.SunriseSunsetCalculator;

import static com.alimuzaffar.sunalarm.activity.ShowStatusActivity.TIME_12HRS;
import static com.alimuzaffar.sunalarm.activity.ShowStatusActivity.TIME_24HRS;


public class DawnDuskWidget extends AppWidgetProvider {
	public static final String	TAG					= "DawnDuskWidget";
	
	public static final String	ACTION_1			= "com.alimuzaffar.sunalarm.WIDGET_CLICK";

	public static final int		REQUEST_1			= 2012081001;

	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		Log.d(TAG, "onUpdate()");
		context.startService(new Intent(context, UpdateService.class));
	}

	public static class UpdateService extends Service {
		SunriseSunsetCalculator calculator = null;
		private Calendar				todaySunriseCal;
		private Calendar				todaySunsetCal;
		private Calendar				tomorrowSunriseCal;
		private Calendar				tomorrowSunsetCal;
		private Calendar				nextSunriseCal;
		private Calendar				nextSunsetCal;
//		BroadcastReceiver	configReceiver;
		@Override
		public void onCreate() {
			Log.d(TAG+".UpdateService","onCreate()");
			super.onCreate();
		}

		@Override
		public void onStart(Intent intent, int startId) {
			Log.d(TAG + ".UpdateService", "onStart()");
			
//			if(configReceiver == null) {
//				final IntentFilter theFilter = new IntentFilter(Intent.ACTION_CONFIGURATION_CHANGED);
//				this.configReceiver = new BroadcastReceiver() {
//
//					@Override
//					public void onReceive(Context context, Intent intent) {
//						// Do whatever you need it to do when it receives the broadcast
//						// Example show a Toast message...
//						context.startService(new Intent(context, UpdateService.class));
//					}
//				};
//				registerReceiver(this.configReceiver, theFilter);
//			}
			
			// Build the widget update for today
			RemoteViews updateViews = buildUpdate(this);
			Log.d("DawnDuskWidget.UpdateService", "update built");

			// Push update for this widget to the home screen
			ComponentName thisWidget = new ComponentName(this, DawnDuskWidget.class);
			AppWidgetManager manager = AppWidgetManager.getInstance(this);
			manager.updateAppWidget(thisWidget, updateViews);
			Log.d("DawnDuskWidget.UpdateService", "widget updated");
		}

		@Override
		public IBinder onBind(Intent intent) {
			return null;
		}

		/**
		 * Build a widget update to show the current Wiktionary "Word of the day." Will block until the online API
		 * returns.
		 */
		public RemoteViews buildUpdate(Context context) {
			Log.d(TAG + ".UpdateService", "buildUpdate()");
			// Pick out month names from resources
			RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_layout);

			// Create an Intent to launch ExampleActivity
			Intent intent = new Intent(context, ShowStatusActivity.class);

			intent.setAction(ACTION_1);
//			PendingIntent pendingIntent = PendingIntent.getBroadcast(context, REQUEST_1, intent, PendingIntent.FLAG_UPDATE_CURRENT);
			PendingIntent pendingIntent = PendingIntent.getActivity(context, REQUEST_1, intent, PendingIntent.FLAG_UPDATE_CURRENT);
//			views.setTextViewText(R.id.dawn, "Sehri "+System.currentTimeMillis());
//			views.setTextViewText(R.id.dusk, "Iftar "+System.currentTimeMillis());
			calculate(views);
			views.setOnClickPendingIntent(R.id.widget_parent, pendingIntent);
			// attach click listeners.
			// check if a button is selected and highlight it's text.
			return views;
		}
		
		private void calculate(RemoteViews views) {
			final AppSettings settings = AppSettings.getInstance(getApplicationContext());
			calculator = new SunriseSunsetCalculator(new com.luckycatlabs.sunrisesunset.dto.Location(settings.getDouble(Key.LAST_LATITUDE), settings.getDouble(Key.LAST_LONGITUDE)), TimeZone.getDefault().getID());
			
			if (calculator != null) {
				Calendar cal = Calendar.getInstance();

				todaySunriseCal = Utils.getSunrise(this, calculator, cal);
				todaySunsetCal = Utils.getSunset(this, calculator, cal);

				cal.add(Calendar.DATE, 1);
				tomorrowSunriseCal = Utils.getSunrise(this, calculator, cal);
				tomorrowSunsetCal = Utils.getSunset(this, calculator, cal);

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

				String dawnTitle = null;
				String duskTitle = null;
				if (dawnToday) {
					dawnTitle = getString(R.string.s_dawn, getString(R.string.today));
				} else {
					dawnTitle = getString(R.string.s_dawn, getString(R.string.tomorrow));
				}

				if (duskToday) {
					duskTitle = getString(R.string.s_dusk, getString(R.string.today));
				} else {
					duskTitle = getString(R.string.s_dusk, getString(R.string.tomorrow));
				}

				views.setTextViewText(R.id.dawn, dawnTitle + "\n\t\t\t" + dawnText);
				views.setTextViewText(R.id.dusk, duskTitle + "\n\t\t\t" + duskText);
			}
		}

		
	}

}
