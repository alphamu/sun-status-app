package com.alimuzaffar.sunalarm.receiver;

import com.alimuzaffar.sunalarm.activity.AlarmActivity;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

public class AlarmReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		try {
			Bundle bundle = intent.getExtras();
			String message = bundle.getString("alarm_type");

			Intent newIntent = new Intent(context, AlarmActivity.class);
			newIntent.putExtra("alarm_type", message);
			newIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			context.startActivity(newIntent);		
			
		} catch (Exception e) {
			Log.e(this.getClass().getName(), e.getMessage(), e);

		}

	}
	

}
