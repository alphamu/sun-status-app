package com.alimuzaffar.sunalarm.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.alimuzaffar.sunalarm.util.AppSettings;
import com.alimuzaffar.sunalarm.util.AppSettings.Key;
import com.alimuzaffar.sunalarm.util.Utils;

public class ResetAlarmOnPackageReplaced extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		try {
			if (intent.getDataString().contains(context.getPackageName())) {
	            AppSettings settings = AppSettings.getInstance(context);
	            if(settings.getBoolean(Key.DAWN_ALARM)) {
	            	Utils.stopAlarm(context, Key.DAWN_ALARM.toString());
	            	Utils.setAlarm(context, Key.DAWN_ALARM.toString());
	            } 
	            if (settings.getBoolean(Key.DUSK_ALARM)) {
	            	Utils.stopAlarm(context, Key.DUSK_ALARM.toString());
	            	Utils.setAlarm(context, Key.DUSK_ALARM.toString());
	            }
	        }	
			
		} catch (Exception e) {
			Log.e(this.getClass().getName(), e.getMessage(), e);

		}

	}
	

}
