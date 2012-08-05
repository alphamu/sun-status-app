package com.alimuzaffar.sunalarm.receiver;

import com.alimuzaffar.sunalarm.util.AppSettings;
import com.alimuzaffar.sunalarm.util.AppSettings.Key;
import com.alimuzaffar.sunalarm.util.Utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class ResetAlarmOnBootReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		try {
			if ("android.intent.action.BOOT_COMPLETED".equals(intent.getAction())) {
	            AppSettings settings = AppSettings.getInstance(context);
	            if(settings.getBoolean(Key.DAWN_ALARM)) {
	            	Utils.setAlarm(context, Key.DAWN_ALARM.toString());
	            } else if (settings.getBoolean(Key.DUSK_ALARM)) {
	            	Utils.setAlarm(context, Key.DUSK_ALARM.toString());
	            }
	        }	
			
		} catch (Exception e) {
			Log.e(this.getClass().getName(), e.getMessage(), e);

		}

	}
	

}
