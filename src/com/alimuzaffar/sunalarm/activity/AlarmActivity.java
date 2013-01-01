package com.alimuzaffar.sunalarm.activity;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

import com.alimuzaffar.sunalarm.R;
import com.alimuzaffar.sunalarm.util.AppSettings;
import com.alimuzaffar.sunalarm.util.AppSettings.Key;
import com.alimuzaffar.sunalarm.util.Utils;

public class AlarmActivity extends Activity {
	private static String TAG = "AlarmActivity";
	private static final int _ID = 20120804;
	MediaPlayer mMediaPlayer = null;

	private String alarmType = null;
	private boolean fromAlert = false;

	Handler alarmAutoStop = new Handler();
	Runnable stopAlarmTask;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// new way of wake of doing things but only works on fullscreen
		// activities.
			getWindow().addFlags(
					WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
							| WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
							| WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
							| WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
							| WindowManager.LayoutParams.FLAG_FULLSCREEN);

		setContentView(R.layout.activity_alarm);
		setVolumeControlStream(AudioManager.STREAM_ALARM);

		Bundle bundle = getIntent().getExtras();

		if (bundle == null && savedInstanceState != null) {
			bundle = savedInstanceState;
		} else if (bundle == null) {
			finish();
			return;
		} else {
			fromAlert = bundle.getBoolean("from_alert");
			alarmType = bundle.getString("alarm_type");
		}

		if (bundle.getBoolean("alarm_auto_off")) {
			NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
			mNotificationManager.cancel(_ID);
			finish();
			return;
		}

		if (fromAlert) {
			stopRingtone();
		} else {
			setNotification();

			if (mMediaPlayer == null) {
				playRingtone();
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

	@Override
	protected void onResume() {
		super.onResume();
		if (alarmType.equals(Key.DAWN_ALARM.toString())) {
			setTitle(getString(R.string.ring_alarm, getString(R.string.dawn)));

		} else if (alarmType.equals(Key.DUSK_ALARM.toString())) {
			setTitle(getString(R.string.ring_alarm, getString(R.string.dusk)));
		}
	}

	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
		super.onSaveInstanceState(savedInstanceState);

		Bundle bundle = getIntent().getExtras();
		if (bundle != null) {
			savedInstanceState.putBoolean("from_alert",
					bundle.getBoolean("from_alert"));
			savedInstanceState.putString("alarm_type",
					bundle.getString("alarm_type"));
		}

	}

	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);

		alarmType = savedInstanceState.getString("alarm_type");
		fromAlert = savedInstanceState.getBoolean("from_alert");
	}

	private void playRingtone() {
		try {
			Uri alert = Utils.getRingtone(this);
			mMediaPlayer = new MediaPlayer();
			mMediaPlayer.setDataSource(this, alert);
			final AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
			audioManager.setStreamMute(AudioManager.STREAM_ALARM, false);
			
			if (audioManager.getStreamVolume(AudioManager.STREAM_ALARM) == 0) {
				audioManager.setStreamVolume(AudioManager.STREAM_ALARM, audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM), 0);
			}
			if (audioManager.getStreamVolume(AudioManager.STREAM_ALARM) != 0) {
				mMediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
				mMediaPlayer.setLooping(true);
				mMediaPlayer.prepare();
				mMediaPlayer.start();
			}
		} catch (Exception e) {
			Log.e(TAG, e.getMessage(), e);
		}

		alarmAutoStop.postDelayed(stopAlarmTask = new Runnable() {

			@Override
			public void run() {
				stopRingtone();
				setAlarmTimeoutNotification();
			}
		}, (1000 * 60) * 5); // stop alarm after 5 minutes
	}

	private void stopRingtone() {
		if (alarmAutoStop != null) {
			alarmAutoStop.removeCallbacks(stopAlarmTask);
		}
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		mNotificationManager.cancel(_ID);
		if (mMediaPlayer != null) {
			mMediaPlayer.stop();
			mMediaPlayer = null;
		}

		// if alarm is on set next////
		AppSettings appSettings = AppSettings
				.getInstance(getApplicationContext());
		if (appSettings.getBoolean(alarmType)) {
			Utils.setAlarm(getApplicationContext(), alarmType);
		}
		finish();
	}

	@SuppressWarnings("deprecation")
	private void setNotification() {
		String ns = Context.NOTIFICATION_SERVICE;
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(ns);

		int icon = android.R.drawable.stat_notify_more;
		CharSequence tickerText = getString(R.string.ring_alarm, (alarmType.equals(Key.DAWN_ALARM.toString())) ? getString(R.string.dawn) : getString(R.string.dusk));
		long when = System.currentTimeMillis();

		Notification notification = new Notification(icon, tickerText, when);

		Context context = getApplicationContext();
		CharSequence contentTitle = tickerText; // "My notification";
		CharSequence contentText = getString(R.string.alert_description);
		Intent notificationIntent = new Intent(this, AlarmActivity.class);
		notificationIntent.putExtra("alarm_type", alarmType);
		notificationIntent.putExtra("from_alert", true);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
				notificationIntent, 0);

		notification.setLatestEventInfo(context, contentTitle, contentText,
				contentIntent);
		notification.flags = Notification.FLAG_AUTO_CANCEL;

		mNotificationManager.notify(_ID, notification);

	}

	@SuppressWarnings("deprecation")
	private void setAlarmTimeoutNotification() {
		String ns = Context.NOTIFICATION_SERVICE;
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(ns);

		int icon = android.R.drawable.stat_notify_more;
		CharSequence tickerText = getString(
				R.string.alarm_auto_off,
				(alarmType == Key.DAWN_ALARM.toString()) ? getString(R.string.dawn)
						: getString(R.string.dusk));
		long when = System.currentTimeMillis();

		Notification notification = new Notification(icon, tickerText, when);

		Context context = getApplicationContext();
		CharSequence contentTitle = tickerText; // "My notification";
		CharSequence contentText = getString(R.string.alarm_auto_off_description);
		Intent notificationIntent = new Intent(this, AlarmActivity.class);
		notificationIntent.putExtra("alarm_auto_off", true);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
				notificationIntent, 0);

		notification.setLatestEventInfo(context, contentTitle, contentText,
				contentIntent);
		notification.flags = Notification.FLAG_AUTO_CANCEL;

		mNotificationManager.notify(_ID, notification);

	}
	
	@Override
	public void onBackPressed() {
	}

}
