package com.alimuzaffar.sunalarm.activity;

import android.os.Bundle;
import android.webkit.WebView;

import com.actionbarsherlock.app.SherlockActivity;
import com.alimuzaffar.sunalarm.R;

public class HelpActivity extends SherlockActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_help);
		
		WebView wv;  
        wv = (WebView) findViewById(R.id.webView);  
        wv.loadUrl("file:///android_res/raw/help.html"); 
		
	}
}
