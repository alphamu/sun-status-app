package com.alimuzaffar.sunalarm.activity;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;

import com.alimuzaffar.sunalarm.R;
import com.alimuzaffar.sunalarm.util.Settings.Key;

public class AlarmActivity extends Activity {
	private static final int _ID = 20120804;
	Ringtone ringtone;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setNotification();
		setContentView(R.layout.activity_alarm);
		
		Button turnOff = (Button) findViewById(R.id.turnAOffAlarm);
		turnOff.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				stopRingtone();
			}
		});
	}
	
	
	
	@Override
	protected void onResume() {
		super.onResume();
		Bundle bundle = getIntent().getExtras();
		String type = bundle.getString("alarm_type");
		if (type.equals(Key.DAWN_ALARM.toString())) {

		} else if (type.equals(Key.DAWN_ALARM.toString())) {

		}
	}



	private void playRingtone() {
		Uri alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
		if (alert == null) {
			// alert is null, using backup
			alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
			if (alert == null) { // I can't see this ever being null (as always have a default notification) but just
									// incase
				// alert backup is null, using 2nd backup
				alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
			}
		}
		ringtone = RingtoneManager.getRingtone(getApplicationContext(), alert);
		ringtone.play();
		
		Handler handler = new Handler();
		handler.postDelayed(new Runnable() {
			
			@Override
			public void run() {
				stopRingtone();
				
			}
		}, (1000 * 60) * 5); //stop alarm after 5 minutes
	}
	
	private void stopRingtone() {
		ringtone.stop();
	}
	
	private void setNotification() {
		String ns = Context.NOTIFICATION_SERVICE;
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(ns);
		
		int icon = android.R.drawable.stat_notify_more;
		CharSequence tickerText = "Hello";
		long when = System.currentTimeMillis();

		Notification notification = new Notification(icon, tickerText, when);
		
		Context context = getApplicationContext();
		CharSequence contentTitle = "My notification";
		CharSequence contentText = "Hello World!";
		Intent notificationIntent = new Intent(this, AlarmActivity.class);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

		notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);
		
		mNotificationManager.notify(_ID, notification);
		
		playRingtone();
	}

}
