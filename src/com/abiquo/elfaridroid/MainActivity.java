package com.abiquo.elfaridroid;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends ActionBarActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
		String passphrase = pref.getString("passphrase",null);
		String nickname = pref.getString("nickname",null);
		String elfariapiendpoint = pref.getString("apiendpoint","http://elfariapibot.herokuapp.com");
		String last_volume = pref.getString("last_volume","0");
		Log.d("elFariDroid","Last volume value: "+last_volume);

		if (passphrase == null ||  passphrase.matches("") ||
				nickname == null || nickname.matches("") ||
				elfariapiendpoint.matches("")) {
			Intent i = new Intent(MainActivity.this, SettingsActivity.class);
			startActivity(i);
		}

		SeekBar volumeControl = (SeekBar) findViewById(R.id.volume_bar);
		Button volume_button = (Button) findViewById(R.id.volume_button);
		Button message_button = (Button) findViewById(R.id.message_button);
		final TextView volume_value = (TextView) findViewById(R.id.volume_value);
		volume_value.setText(last_volume); // Applying default volume
		final TextView fari_message = (TextView) findViewById(R.id.fari_message);

		try {
			volumeControl.setProgress(Integer.parseInt(volume_value.getText().toString()));
		} catch(NumberFormatException nfe) {
			Log.d("elFariDroid","ERROR: Could not parse " + nfe);
		} 

		volume_button.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				new MyAsyncTask().execute("volumen "+volume_value.getText().toString());
				savePreferences("last_volume",volume_value.getText().toString());
				Log.d("elFariDroid","INFO: Volume set to " + volume_value.getText().toString());
			}
		});

		message_button.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (fari_message.getText().toString().matches("")) {
					Toast.makeText(MainActivity.this, "Type a message first!", Toast.LENGTH_SHORT)
					.show();		    			
				} else {
					new MyAsyncTask().execute(fari_message.getText().toString());
					fari_message.setText("");
				}
			}
		});

		volumeControl.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			TextView volume = (TextView) findViewById(R.id.volume_value);

			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				volume.setText(String.valueOf(progress));			
			}

			public void onStartTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub
			}

			public void onStopTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub
			}
		});

	}

	private void savePreferences(String key, String value) {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		Editor editor = sharedPreferences.edit();
		editor.putString(key, value);
		editor.commit();
	}


	protected void onResume()
	{
		super.onResume();
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
		String passphrase = pref.getString("passphrase",null);
		String nickname = pref.getString("nickname",null);

		if (passphrase == null || nickname == null || passphrase.matches("") || nickname.matches("")) {
			// c.getResources().getString(R.string.error_missing_settings)
			Toast.makeText(MainActivity.this, this.getResources().getString(R.string.error_missing_settings), Toast.LENGTH_SHORT).show();
			Intent i = new Intent(MainActivity.this, SettingsActivity.class);
			startActivity(i);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			Intent i = new Intent(MainActivity.this, SettingsActivity.class);
			startActivity(i);
		}
		return super.onOptionsItemSelected(item);
	}

	public void postToFariAPI(String postData) {

		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
		String passphrase = pref.getString("passphrase",null);
		String nickname = pref.getString("nickname",null);
		String elfariapiendpoint = pref.getString("apiendpoint","http://elfariapibot.herokuapp.com");
		Log.d("elFariDroid","API endpoint: "+elfariapiendpoint);

		if  (passphrase == null ||  passphrase.matches("") ||
				nickname == null || nickname.matches("") ||
				elfariapiendpoint.matches("")) {
			Toast.makeText(MainActivity.this, this.getResources().getString(R.string.error_missing_settings), Toast.LENGTH_SHORT).show();
		} 
		else {

			String hashedpassphrase = hashpassphrase(passphrase);
			String postURI = elfariapiendpoint+"/send/"+nickname+"/"+hashedpassphrase;
			HttpClient httpclient = new DefaultHttpClient();
			HttpPost httppost = new HttpPost(postURI);
			try {
				List nameValuePairs = new ArrayList();
				nameValuePairs.add(new BasicNameValuePair("data", postData));
				httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
				HttpResponse response = httpclient.execute(httppost);
				switch(response.getStatusLine().getStatusCode()) {
				case 202:
					AsyncTaskResponse(this.getResources().getString(R.string.info_message_processed));
					break;
				case 401:
					AsyncTaskResponse(this.getResources().getString(R.string.error_wrong_passphrase));
					break;
				case 404:
					AsyncTaskResponse(this.getResources().getString(R.string.error_generic));
					break;
				}
			} catch (ClientProtocolException e) {
				AsyncTaskResponse(this.getResources().getString(R.string.error_api_communication));
			} catch (IOException e) {
				AsyncTaskResponse(this.getResources().getString(R.string.error_api_communication));
			}
		}
	}

	private class MyAsyncTask extends AsyncTask<String, Integer, Double>{
		@Override
		protected Double doInBackground(String... params) {
			try {
				postToFariAPI(params[0]);
			} catch (Exception e) {
				AsyncTaskResponse("ERROR: Can't contact elFariDroid API");
			}	  
			return null;
		}
	}

	private String hashpassphrase(String pp) {
		try {
			// Create MD5 Hash
			MessageDigest digest = java.security.MessageDigest
					.getInstance("MD5");
			digest.update(pp.getBytes());
			byte messageDigest[] = digest.digest();

			// Create Hex String
			StringBuilder hexString = new StringBuilder();
			for (byte aMessageDigest : messageDigest) {
				String h = Integer.toHexString(0xFF & aMessageDigest);
				while (h.length() < 2)
					h = "0" + h;
				hexString.append(h);
			}
			return hexString.toString();

		} catch (NoSuchAlgorithmException e) {
			AsyncTaskResponse(this.getResources().getString(R.string.error_api_communication));
		}
		return "";
	}

	public void AsyncTaskResponse(final String message) {
		runOnUiThread(new Runnable() {
			public void run() {
				Toast.makeText(MainActivity.this, message , Toast.LENGTH_SHORT).show();
			}
		});
	}
}
