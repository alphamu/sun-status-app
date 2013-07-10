package com.alimuzaffar.sunalarm.activity;

import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import org.json.JSONArray;
import org.json.JSONObject;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.location.Location;
import android.location.LocationListener;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.widget.SimpleCursorAdapter;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;
import com.actionbarsherlock.widget.SearchView;
import com.actionbarsherlock.widget.SearchView.OnQueryTextListener;
import com.actionbarsherlock.widget.SearchView.OnSuggestionListener;
import com.alimuzaffar.sunalarm.BuildConfig;
import com.alimuzaffar.sunalarm.R;
import com.alimuzaffar.sunalarm.receiver.AlarmReceiver;
import com.alimuzaffar.sunalarm.util.AppRater;
import com.alimuzaffar.sunalarm.util.AppSettings;
import com.alimuzaffar.sunalarm.util.AppSettings.Key;
import com.alimuzaffar.sunalarm.util.ChangeLog;
import com.alimuzaffar.sunalarm.util.JsonHttpHelper;
import com.alimuzaffar.sunalarm.util.OnNoProviderEnabledListener;
import com.alimuzaffar.sunalarm.util.UserLocation;
import com.alimuzaffar.sunalarm.util.UserLocation.OnLocationChangedListener;
import com.alimuzaffar.sunalarm.util.Utils;
import com.luckycatlabs.sunrisesunset.SunriseSunsetCalculator;

public class ShowStatusActivity extends SherlockActivity implements OnCheckedChangeListener, OnQueryTextListener, OnSuggestionListener {
	private static final String TAG = "ShowStatusActivity";
	private static int SETTINGS = 20120808;

	public static SimpleDateFormat TIME_24HRS = new SimpleDateFormat("HH:mm", Locale.getDefault());
	public static SimpleDateFormat TIME_12HRS = new SimpleDateFormat("hh:mm a", Locale.getDefault());

	private TextView duskTime, dawnTime, duskTitle, dawnTitle;
	private View duskTimeProgress, dawnTimeProgress;

	private CompoundButton duskAlarmSet, dawnAlarmSet;

	private EditText delayDawnAlarm, delayDuskAlarm;

	private Calendar todaySunriseCal;
	private Calendar todaySunsetCal;
	private Calendar tomorrowSunriseCal;
	private Calendar tomorrowSunsetCal;
	private Calendar nextSunriseCal;
	private Calendar nextSunsetCal;

	SunriseSunsetCalculator calculator = null;

	LocationListener coarseListener;

	UserLocation mUserLocation;
	private static boolean FOUND_LOCATION = false;

	SearchView mSearchView;
	SimpleCursorAdapter simpleCursorAdapter;
	MenuItem mChangeLocation;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// requestWindowFeature(Window.FEATURE_PROGRESS);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

		setContentView(R.layout.activity_show_status);
		setVolumeControlStream(AudioManager.STREAM_ALARM);

		// initialize preferences
		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

		dawnTime = (TextView) findViewById(R.id.dawnTime);
		duskTime = (TextView) findViewById(R.id.duskTime);

		dawnTimeProgress = findViewById(R.id.dawnTimeProgress);
		duskTimeProgress = findViewById(R.id.duskTimeProgress);

		dawnTitle = (TextView) findViewById(R.id.dawn);
		duskTitle = (TextView) findViewById(R.id.dusk);

		duskAlarmSet = (CompoundButton) findViewById(R.id.duskAlarmSet);
		dawnAlarmSet = (CompoundButton) findViewById(R.id.dawnAlarmSet);

		delayDawnAlarm = (EditText) findViewById(R.id.delayDawnAlarm);
		delayDuskAlarm = (EditText) findViewById(R.id.delayDuskAlarm);

		bindToggleButtons();

		ChangeLog cl = new ChangeLog(this);
		if (cl.firstRun())
			cl.getLogDialog().show();

		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

		mUserLocation = new UserLocation(this, null);

		AppRater.app_launched(this);

		// needed for 2.3 < devices.
		setSupportProgressBarIndeterminateVisibility(false);
	}

	@Override
	protected void onResume() {
		super.onResume();

		final AppSettings settings = AppSettings.getInstance(getApplicationContext());

		mUserLocation.setOnLocationChangedListener(new OnLocationChangedListener() {

			@Override
			public void onLocationChanged(Location location) {
				// do something here to save this new location
				FOUND_LOCATION = true;
				calculator = new SunriseSunsetCalculator(new com.luckycatlabs.sunrisesunset.dto.Location(location.getLatitude(), location.getLongitude()), TimeZone.getDefault().getID());
				calculate(TimeZone.getDefault().getID());
				settings.set(Key.LAST_LATITUDE, location.getLatitude());
				settings.set(Key.LAST_LONGITUDE, location.getLongitude());
				settings.set(Key.TIMEZONE_ID, TimeZone.getDefault().getID());

				dawnTimeProgress.setVisibility(View.INVISIBLE);
				duskTimeProgress.setVisibility(View.INVISIBLE);
				dawnTime.setVisibility(View.VISIBLE);
				duskTime.setVisibility(View.VISIBLE);
			}
		});

		mUserLocation.setOnNoProviderEnabledListener(new OnNoProviderEnabledListener() {

			@Override
			public void onNoProviderEnabled() {
				Utils.buildAlertMessageNoGps(ShowStatusActivity.this, new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {

						if (settings.getDouble(Key.LAST_LATITUDE) != 0 && settings.getDouble(Key.LAST_LONGITUDE) != 0) {
							// use saved location.
							String timeZoneId = settings.getString(Key.TIMEZONE_ID, TimeZone.getDefault().getID());
							calculator = new SunriseSunsetCalculator(new com.luckycatlabs.sunrisesunset.dto.Location(settings.getDouble(Key.LAST_LATITUDE), settings.getDouble(Key.LAST_LONGITUDE)), timeZoneId);
							calculate(timeZoneId);
							dawnTimeProgress.setVisibility(View.INVISIBLE);
							duskTimeProgress.setVisibility(View.INVISIBLE);
							dawnTime.setVisibility(View.VISIBLE);
							duskTime.setVisibility(View.VISIBLE);
						} else {
							// disable everything, no saved
							// location, no way to get it.
							dawnTime.setVisibility(View.INVISIBLE);
							duskTime.setVisibility(View.INVISIBLE);
							dawnTimeProgress.setVisibility(View.INVISIBLE);
							duskTimeProgress.setVisibility(View.INVISIBLE);
							duskAlarmSet.setEnabled(false);
							dawnAlarmSet.setEnabled(false);
							delayDawnAlarm.setEnabled(false);
							delayDuskAlarm.setEnabled(false);
						}
					}

				});

			}
		});

		if (!settings.getBoolean(Key.MANUAL_LOCATION, false)) {
			mUserLocation.registerLocationListener();
			getSupportActionBar().setSubtitle("Automatically detect location.");
		} else {
			String location = settings.getString(Key.MANUAL_LOCATION_NAME);
			getSupportActionBar().setSubtitle(location);
			FOUND_LOCATION = false;
		}
		
		// enable everything
		duskAlarmSet.setEnabled(true);
		dawnAlarmSet.setEnabled(true);
		delayDawnAlarm.setEnabled(true);
		delayDuskAlarm.setEnabled(true);

		LinearLayout myLayout = (LinearLayout) findViewById(R.id.focussucker);
		myLayout.requestFocus();

		Log.d(TAG, "found_loc="+FOUND_LOCATION);
		// if no loaction is available, initialize with saved settings.
		if (!FOUND_LOCATION && settings.getDouble(Key.LAST_LATITUDE) != 0 && settings.getDouble(Key.LAST_LATITUDE) != 0) {
			String timeZoneId = settings.getString(Key.TIMEZONE_ID, TimeZone.getDefault().getID());
			calculator = new SunriseSunsetCalculator(new com.luckycatlabs.sunrisesunset.dto.Location(settings.getDouble(Key.LAST_LATITUDE), settings.getDouble(Key.LAST_LONGITUDE)), timeZoneId);
			calculate(timeZoneId);
			dawnTimeProgress.setVisibility(View.INVISIBLE);
			duskTimeProgress.setVisibility(View.INVISIBLE);
			dawnTime.setVisibility(View.VISIBLE);
			duskTime.setVisibility(View.VISIBLE);
		}

		Log.d(TAG, "Time Zone Id: " + TimeZone.getDefault().getID());

		String[] from = { "text" };
		int[] to = { android.R.id.text1 };
		if (simpleCursorAdapter == null)
			simpleCursorAdapter = new SimpleCursorAdapter(ShowStatusActivity.this, android.R.layout.simple_list_item_1, new MatrixCursor(new String[] { "_id", "text" }), from, to, 0);

	}

	@Override
	protected void onPause() {
		super.onPause();

		mUserLocation.unRegisterLocationListener();
		mUserLocation.setOnLocationChangedListener(null);
		mUserLocation.setOnNoProviderEnabledListener(null);
	}

	private void calculate(String timeZoneId) {
		if (calculator != null) {
			TimeZone tz = TimeZone.getTimeZone(timeZoneId);
			Calendar cal = Calendar.getInstance(tz);

			todaySunriseCal = Utils.getSunrise(this, calculator, cal);
			todaySunsetCal = Utils.getSunset(this, calculator, cal);

			cal.add(Calendar.DATE, 1);
			tomorrowSunriseCal = Utils.getSunrise(this, calculator, cal);
			tomorrowSunsetCal = Utils.getSunset(this, calculator, cal);

			if (todaySunriseCal == null || todaySunsetCal == null || tomorrowSunriseCal == null || tomorrowSunsetCal == null) {
				Toast.makeText(this, "Something went wrong. Cannot calculate timing.", Toast.LENGTH_LONG).show();
				return;
			}

			String dawnText = null;
			boolean dawnToday, duskToday = false;
			if (todaySunriseCal.before(Calendar.getInstance(tz))) {
				nextSunriseCal = tomorrowSunriseCal;
				TIME_12HRS.setTimeZone(nextSunriseCal.getTimeZone());
				dawnText = TIME_12HRS.format(nextSunriseCal.getTime());
				dawnToday = false;
			} else {
				nextSunriseCal = todaySunriseCal;
				TIME_12HRS.setTimeZone(nextSunriseCal.getTimeZone());
				dawnText = TIME_12HRS.format(nextSunriseCal.getTime());
				dawnToday = true;
			}

			String duskText = null;
			if (todaySunsetCal.before(Calendar.getInstance(tz))) {
				nextSunsetCal = tomorrowSunsetCal;
				TIME_12HRS.setTimeZone(nextSunsetCal.getTimeZone());
				duskText = TIME_12HRS.format(nextSunsetCal.getTime());
				duskToday = false;
			} else {
				nextSunsetCal = todaySunsetCal;
				TIME_12HRS.setTimeZone(nextSunsetCal.getTimeZone());
				duskText = TIME_12HRS.format(nextSunsetCal.getTime());
				duskToday = true;
			}

			if (dawnToday) {
				dawnTitle.setText(getString(R.string.s_dawn, getString(R.string.today)));
			} else {
				dawnTitle.setText(getString(R.string.s_dawn, getString(R.string.tomorrow)));
			}

			if (duskToday) {
				duskTitle.setText(getString(R.string.s_dusk, getString(R.string.today)));
			} else {
				duskTitle.setText(getString(R.string.s_dusk, getString(R.string.tomorrow)));
			}

			dawnTime.setText(dawnText);
			duskTime.setText(duskText);
		}
	}

	private void bindToggleButtons() {

		AppSettings settings = AppSettings.getInstance(ShowStatusActivity.this.getApplicationContext());

		dawnAlarmSet.setChecked(settings.getBoolean(Key.DAWN_ALARM));
		duskAlarmSet.setChecked(settings.getBoolean(Key.DUSK_ALARM));

		delayDawnAlarm.setText(String.valueOf(settings.getInt(Key.DAWN_DELAY)));
		delayDuskAlarm.setText(String.valueOf(settings.getInt(Key.DUSK_DELAY)));

		duskAlarmSet.setOnCheckedChangeListener(this);
		dawnAlarmSet.setOnCheckedChangeListener(this);

		delayDawnAlarm.addTextChangedListener(new TextWatcher() {

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}

			@Override
			public void afterTextChanged(Editable s) {
				AppSettings settings = AppSettings.getInstance(getApplicationContext());
				int num = 0;
				try {
					num = Integer.parseInt(s.toString());
				} catch (NumberFormatException nfe) {
					num = 0;
				}
				settings.set(Key.DAWN_DELAY, num);
				if (settings.getBoolean(Key.DAWN_ALARM)) {
					updateAlarms(true, false);
				}
			}
		});

		delayDuskAlarm.addTextChangedListener(new TextWatcher() {

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}

			@Override
			public void afterTextChanged(Editable s) {
				AppSettings settings = AppSettings.getInstance(getApplicationContext());
				int num = 0;
				try {
					num = Integer.parseInt(s.toString());
				} catch (NumberFormatException nfe) {
					num = 0;
				}
				settings.set(Key.DUSK_DELAY, num);
				if (settings.getBoolean(Key.DUSK_ALARM)) {
					updateAlarms(false, true);
				}
			}
		});

		if (AppSettings.DEBUG) {
			bindTestButtons();
		} else {
			removeTestButtons();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getSupportMenuInflater().inflate(R.menu.show_status, menu);
		mChangeLocation = menu.findItem(R.id.menu_changeloc);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.menu_settings) {
			Intent intent = new Intent(this, SettingsActivity.class);
			startActivityForResult(intent, SETTINGS);

		} else if (item.getItemId() == R.id.menu_feedback) {
			final Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);

			/* Fill it with Data */
			emailIntent.setType("plain/text");
			emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[] { "ali@muzaffar.me" });
			emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Sehri and Iftar Alarm - Feedback");
			emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, "I'd like to report a bug or request a feature.");

			/* Send it off to the Activity-Chooser */
			startActivity(Intent.createChooser(emailIntent, "Send mail..."));

		} else if (item.getItemId() == R.id.menu_changeloc) {
			if (mSearchView == null) {
				mSearchView = (SearchView) item.getActionView();
				mSearchView.setQueryHint("City Name");
				mSearchView.setOnQueryTextListener(this);
				mSearchView.setOnSuggestionListener(this);

				mSearchView.setSuggestionsAdapter(simpleCursorAdapter);
			}
			if(!isOnline()) {
				Toast.makeText(this, "No internet connection detected.", Toast.LENGTH_SHORT).show();
				item.collapseActionView();
			}
		} else if (item.getItemId() == R.id.menu_clear_location) {
			clearSetLocation();
		}

		return false;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == SETTINGS) {
			AppSettings settings = AppSettings.getInstance(getApplicationContext());
			String timeZoneId = settings.getString(Key.TIMEZONE_ID, TimeZone.getDefault().getID());
			calculate(timeZoneId);
			updateAlarms(true, true);

		}
	}

	private void updateAlarms(boolean dawn, boolean dusk) {
		AppSettings settings = AppSettings.getInstance(getApplicationContext());
		if (dawn && settings.getBoolean(Key.DAWN_ALARM) && nextSunriseCal != null) {
			Utils.stopAlarm(getApplicationContext(), Key.DAWN_ALARM.toString());
			Utils.setAlarm(getApplicationContext(), nextSunriseCal, Key.DAWN_ALARM.toString());
		}

		if (dusk && settings.getBoolean(Key.DUSK_ALARM) && nextSunsetCal != null) {
			Utils.stopAlarm(getApplicationContext(), Key.DUSK_ALARM.toString());
			Utils.setAlarm(getApplicationContext(), nextSunsetCal, Key.DUSK_ALARM.toString());
		}

		if (nextSunriseCal == null || nextSunsetCal == null) {
			Toast.makeText(this, "ERROR: Alarm has not have been set.\nUnable to determine alarm times.\nEnable GPS and try again.", Toast.LENGTH_LONG).show();
		}
	}

	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		AppSettings settings = AppSettings.getInstance(ShowStatusActivity.this.getApplicationContext());
		if (buttonView.getId() == R.id.dawnAlarmSet && nextSunriseCal != null) {
			settings.set(Key.DAWN_ALARM, isChecked);
			if (isChecked)
				Utils.setAlarm(getApplicationContext(), nextSunriseCal, Key.DAWN_ALARM.toString());
			else
				Utils.stopAlarm(getApplicationContext(), Key.DAWN_ALARM.toString());

		} else if (buttonView.getId() == R.id.duskAlarmSet && nextSunsetCal != null) {
			settings.set(Key.DUSK_ALARM, isChecked);
			if (isChecked)
				Utils.setAlarm(getApplicationContext(), nextSunsetCal, Key.DUSK_ALARM.toString());
			else
				Utils.stopAlarm(getApplicationContext(), Key.DUSK_ALARM.toString());
		}

		if (nextSunriseCal == null || nextSunsetCal == null) {
			Toast.makeText(this, "ERROR: Alarm has not have been set.\nUnable to determine alarm times.\nEnable GPS and try again.", Toast.LENGTH_LONG).show();
		}
	}

	private void bindTestButtons() {
		Button testDawn = ((Button) findViewById(R.id.testDawn));
		testDawn.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent intent = new Intent(ShowStatusActivity.this, AlarmReceiver.class);
				intent.putExtra("alarm_type", Key.DAWN_ALARM.toString());//
				sendBroadcast(intent);
			}
		});

		Button testDusk = ((Button) findViewById(R.id.testDusk));
		testDusk.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent intent = new Intent(ShowStatusActivity.this, AlarmReceiver.class);
				intent.putExtra("alarm_type", Key.DUSK_ALARM.toString());
				sendBroadcast(intent);
			}
		});
	}

	private void removeTestButtons() {
		TableRow tableRow7 = ((TableRow) findViewById(R.id.tableRow7));
		tableRow7.setVisibility(View.GONE);
	}

	private void clearSetLocation() {
		final AppSettings settings = AppSettings.getInstance(getApplicationContext());
		mUserLocation.registerLocationListener();
		settings.set(Key.MANUAL_LOCATION, false);
		settings.set(Key.MANUAL_LOCATION_NAME, "Automatically detect location.");
		getSupportActionBar().setSubtitle("Automatically detect location.");

	}

	@Override
	public boolean onQueryTextSubmit(String query) {
		return false;
	}

	@Override
	public boolean onQueryTextChange(String newText) {

		if (newText.length() > 1) {
			simpleCursorAdapter.changeCursor(new MatrixCursor(new String[] { "_id", "text" }));

			setSupportProgressBarIndeterminateVisibility(true);
			(new FetchLocationsAsyncTask() {
				protected void onPostExecute(MatrixCursor cursor) {
					// dismiss UI progress indicator
					// process the result
					// ...

					setSupportProgressBarIndeterminateVisibility(false);

					String[] from = { "text" };
					int[] to = { android.R.id.text1 };
					if (simpleCursorAdapter == null)
						simpleCursorAdapter = new SimpleCursorAdapter(ShowStatusActivity.this, android.R.layout.simple_list_item_1, cursor, from, to, 0);
					else
						simpleCursorAdapter.changeCursor(cursor);

					mSearchView.setSuggestionsAdapter(simpleCursorAdapter);
				}
			}).execute(newText, getResources().getString(R.string.places_api_key)); // start the background processing
		}
		return false;
	}

	@Override
	public boolean onSuggestionSelect(int position) {
		return false;
	}

	@Override
	public boolean onSuggestionClick(int position) {

		mUserLocation.unRegisterLocationListener();
		
		final AppSettings settings = AppSettings.getInstance(getApplicationContext());

		Cursor c = mSearchView.getSuggestionsAdapter().getCursor();
		c.moveToPosition(position);
		String suggestion = c.getString(1);
		mChangeLocation.collapseActionView();
		// Log.d(TAG, "NEW LOCATION >>>> " + suggestion);
		Toast.makeText(this, "Setting your location to \"" + suggestion + "\"", Toast.LENGTH_SHORT).show();

		settings.set(Key.MANUAL_LOCATION, true);
		settings.set(Key.MANUAL_LOCATION_NAME, suggestion);
		getSupportActionBar().setSubtitle(suggestion);

		(new FetchGeoLocationAsyncTask() {
			protected void onPostExecute(JSONObject result) {
				// set lat lng & timezone
				try {
					double lat = Double.parseDouble(result.getString("lat"));
					double lng = Double.parseDouble(result.getString("lng"));
					settings.set(Key.LAST_LATITUDE, lat);
					settings.set(Key.LAST_LONGITUDE, lng);
					String timeZoneId = result.getString("timeZoneId");
					settings.set(Key.TIMEZONE_ID, timeZoneId);

					calculator = new SunriseSunsetCalculator(new com.luckycatlabs.sunrisesunset.dto.Location(lat, lng), timeZoneId);
					calculate(timeZoneId);

				} catch (Exception e) {
					Log.e(TAG, e.getMessage(), e);
					Toast.makeText(ShowStatusActivity.this, "Something went wrong. Could not set location.", Toast.LENGTH_LONG).show();
				}
			}
		}).execute(suggestion);

		return false;
	}

	public boolean isOnline() {
		ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo netInfo = cm.getActiveNetworkInfo();
		if (netInfo != null && netInfo.isConnected()) {
			return true;
		}
		return false;
	}
}

class FetchLocationsAsyncTask extends AsyncTask<String, Integer, MatrixCursor> {

	// web key used.
	private static String PLACES_URL = "https://maps.googleapis.com/maps/api/place/autocomplete/json?input=%s&types=(cities)&sensor=true&key=%s";

	@Override
	protected MatrixCursor doInBackground(String... params) {
		String[] columnNames = { "_id", "text" };
		MatrixCursor cursor = new MatrixCursor(columnNames);

		try {
			String url = String.format(PLACES_URL, URLEncoder.encode(params[0], "UTF-8"), params[1]);
			if(BuildConfig.DEBUG)
				Log.d("FetchLocationsAsyncTask", "Places API = "+url);
			JSONObject jsonObject = JsonHttpHelper.getJson(url);
			JSONArray predictions = jsonObject.optJSONArray("predictions");

			if (predictions != null) {

				List<String> results = new ArrayList<String>();

				for (int i = 0; i < predictions.length(); i++) {
					JSONObject item = predictions.getJSONObject(i);
					String description = item.getString("description");
					results.add(description);
				}

				String[] temp = new String[2];
				int id = 0;
				for (String item : results) {
					temp[0] = Integer.toString(id++);
					temp[1] = item;
					cursor.addRow(temp);
				}

			}

		} catch (Exception e) {
			Log.e("FetchLocationsAsyncTask", e.getMessage(), e);
		}
		return cursor;
	}

}

class FetchGeoLocationAsyncTask extends AsyncTask<String, Integer, JSONObject> {
	private static final String GEO_URL = "http://maps.google.com/maps/api/geocode/json?address=%s&sensor=true";
	private static final String TZ_URL = "https://maps.googleapis.com/maps/api/timezone/json?location=%s&timestamp=" + System.currentTimeMillis() / 1000 + "&sensor=true";

	@Override
	protected JSONObject doInBackground(String... params) {
		try {
			String url = String.format(GEO_URL, URLEncoder.encode(params[0], "UTF-8"));
			JSONObject jsonObject = JsonHttpHelper.getJson(url);

			if(BuildConfig.DEBUG)
				Log.d("FetchLocationsAsyncTask", "GEO API = "+url);
			
			if(BuildConfig.DEBUG)
				Log.d("FetchGeoLocationAsyncTask", jsonObject.toString());

			JSONArray results = jsonObject.getJSONArray("results");
			JSONObject item = results.getJSONObject(0);
			JSONObject geo = item.getJSONObject("geometry").getJSONObject("location");

			JSONObject result = new JSONObject();
			result.put("name", item.getString("formatted_address"));
			String lat = geo.getString("lat");
			result.put("lat", lat);
			String lng = geo.getString("lng");
			result.put("lng", lng);

			String tzUrl = String.format(TZ_URL, URLEncoder.encode(lat + "," + lng, "UTF-8"));
			
			if(BuildConfig.DEBUG)
				Log.d("FetchLocationsAsyncTask", "TIMEZONE API = "+tzUrl);
			
			JSONObject tzObj = JsonHttpHelper.getJson(tzUrl);
			if (tzObj != null) {
				String tzId = tzObj.getString("timeZoneId");
				result.put("timeZoneId", tzId);
			}

			return result;

		} catch (Exception e) {
			Log.e("FetchGeoLocationAsyncTask", e.getMessage(), e);
		}
		return null;
	}
}
