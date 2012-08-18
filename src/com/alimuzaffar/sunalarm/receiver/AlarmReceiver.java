package com.alimuzaffar.sunalarm.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.alimuzaffar.sunalarm.activity.AlarmActivity;

public class AlarmReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		try {
			Bundle bundle = intent.getExtras();
			String message = bundle.getString("alarm_type");

			Intent newIntent = new Intent(context, AlarmActivity.class);
			Log.d("AlarmReceiver", "Alarm Receiver Got "+message);
			newIntent.putExtra("alarm_type", message);
			newIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			context.startActivity(newIntent);		
			
		} catch (Exception e) {
			Log.e(this.getClass().getName(), e.getMessage(), e);

		}

	}
	

}
