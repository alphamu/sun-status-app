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
	private static final int	_ID				= 20120804;
	private static Ringtone		ringtone;

	private String				alarmType;

	Handler						alarmAutoStop	= new Handler();
	Runnable					stopAlarmTask;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_alarm);
		Bundle bundle = getIntent().getExtras();

		if (bundle == null) {
			finish();
			return;
		}

		if (bundle.getBoolean("from_alert")) {
			stopRingtone();
		} else {
			setNotification();

			if (ringtone == null) {
				playRingtone();

				String type = bundle.getString("alarm_type");
				if (type.equals(Key.DAWN_ALARM.toString())) {
					setTitle(getString(R.string.ring_alarm, getString(R.string.dawn)));
				} else if (type.equals(Key.DUSK_ALARM.toString())) {
					setTitle(getString(R.string.ring_alarm, getString(R.string.dusk)));

				}

				alarmType = type;
			}

			Button turnOff = (Button) findViewById(R.id.turnAOffAlarm);
			turnOff.setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View v) {
					stopRingtone();
				}
			});
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

		alarmAutoStop.postDelayed(stopAlarmTask = new Runnable() {

			@Override
			public void run() {
				stopRingtone();

			}
		}, (1000 * 60) * 5); // stop alarm after 5 minutes
	}

	private void stopRingtone() {
		if (alarmAutoStop != null) {
			alarmAutoStop.removeCallbacks(stopAlarmTask);
		}
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		mNotificationManager.cancel(_ID);
		if(ringtone != null) {
			ringtone.stop();
			ringtone = null;
		}
		finish();
	}

	private void setNotification() {
		String ns = Context.NOTIFICATION_SERVICE;
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(ns);

		int icon = android.R.drawable.stat_notify_more;
		CharSequence tickerText = getString(R.string.ring_alarm, (alarmType == Key.DAWN_ALARM.toString())? getString(R.string.dawn) : getString(R.string.dusk));
		long when = System.currentTimeMillis();

		Notification notification = new Notification(icon, tickerText, when);

		Context context = getApplicationContext();
		CharSequence contentTitle = tickerText; //"My notification";
		CharSequence contentText = getString(R.string.alert_description);
		Intent notificationIntent = new Intent(this, AlarmActivity.class);
		notificationIntent.putExtra("alarm_type", alarmType);
		notificationIntent.putExtra("from_alert", true);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

		notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);
		notification.flags = Notification.FLAG_AUTO_CANCEL;

		mNotificationManager.notify(_ID, notification);

	}

}