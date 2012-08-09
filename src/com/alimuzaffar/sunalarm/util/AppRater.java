package com.alimuzaffar.sunalarm.util;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;

import com.alimuzaffar.sunalarm.R;
import com.alimuzaffar.sunalarm.util.AppSettings.Key;

public class AppRater {
    private static String APP_TITLE = "";
    private static String APP_PNAME = "";
    
    private final static int DAYS_UNTIL_PROMPT = 0;
    private final static int LAUNCHES_UNTIL_PROMPT = 2;
    
    public static void app_launched(Context mContext) {
        AppSettings settings = AppSettings.getInstance(mContext);
        if (settings.getBoolean(Key.RATER_DONTSHOWAGAIN)) { return ; }
        
        Resources res = mContext.getResources();
        APP_TITLE = ((CharSequence)res.getText(R.string.app_name)).toString();
        APP_PNAME = ((CharSequence)res.getText(R.string.app_package)).toString();
        
        // Increment launch counter
        int launch_count = settings.getInt(Key.RATER_LAUNCHCOUNT) + 1;
        settings.set(Key.RATER_LAUNCHCOUNT, launch_count);

        // Get date of first launch
        Long date_firstLaunch = settings.getLong(Key.RATHER_DATEFIRSTLAUNCH);
        if (date_firstLaunch == 0) {
            date_firstLaunch = System.currentTimeMillis();
            settings.set(Key.RATHER_DATEFIRSTLAUNCH, date_firstLaunch);
        }
        
        // Wait at least n days before opening
        if (launch_count >= LAUNCHES_UNTIL_PROMPT) {
            if (System.currentTimeMillis() >= date_firstLaunch + 
                    (DAYS_UNTIL_PROMPT * 24 * 60 * 60 * 1000)) {
                showRateDialog(mContext, settings);
            }
        }

    }   
    
    public static void showRateDialog(final Context mContext, final AppSettings settings) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(mContext);
        if(android.os.Build.VERSION.SDK_INT > Build.VERSION_CODES.GINGERBREAD_MR1) {
        	dialog = new AlertDialog.Builder(mContext, AlertDialog.THEME_HOLO_DARK);
        }
        dialog.setIcon(R.drawable.ic_launcher)
        .setTitle("Rate "+APP_TITLE)
        .setMessage("If you enjoy using " + APP_TITLE + ", please take a moment to rate it. Thanks for your support!")
        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                mContext.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + APP_PNAME)));
                settings.set(Key.RATER_DONTSHOWAGAIN, true);
                dialog.dismiss();
            }
        })
        .setNeutralButton("Remind me later", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
            	dialog.dismiss();
            }
        })
        .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                settings.set(Key.RATER_DONTSHOWAGAIN, true);
                dialog.dismiss();
            }
        })
        .create();
       
        dialog.show();        
    }
}
