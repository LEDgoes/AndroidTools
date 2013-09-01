package com.ledgoes.android;

import java.net.InetAddress;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Looper;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.Preference.OnPreferenceClickListener;
import android.util.Log;
import android.widget.Toast;
 
public class PreferencesActivity extends PreferenceActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
		// Get the custom preference
		Preference action_wifiScan = (Preference) findPreference("action_scanWiFi");
		action_wifiScan.setOnPreferenceClickListener(new OnPreferenceClickListener() { 
			public boolean onPreferenceClick(Preference preference) {
	    		WifiScanThread task = new WifiScanThread();
	    		task.execute();
				return true;
			}
		});
	}
	
	private class WifiScanThread extends AsyncTask<Integer, String, String> {

		@Override
		protected String doInBackground(Integer... startingIP) {
			// TODO Auto-generated method stub
			Looper.prepare();
			String[] myIPArray = {"192", "168", "1"};
			InetAddress currentPingAddr;
			String ad = "";
			
			Log.d("SpeechPipe","Scanning...");
			for (int i = 0; i < 256; i++) {
	            try {

	                // build the next IP address
	                currentPingAddr = InetAddress.getByName(myIPArray[0] + "." +
	                        myIPArray[1] + "." +
	                        myIPArray[2] + "." +
	                        Integer.toString(i));
	                ad = currentPingAddr.toString();
	                Log.d("SpeechPipe",ad);

	                // 50ms Timeout for the "ping"
	                if (currentPingAddr.isReachable(50)) {
	                    Log.d("SpeechPipe","Reached IP address " + ad + " (" + currentPingAddr.getHostName() + " )");

	                }
	            //} catch (UnknownHostException ex) {
	            //} catch (IOException ex) {
	            } catch (Exception ex) {
	            	Log.e("SpeechPipe", ex.getMessage());
	            	ex.printStackTrace();
	            	return "";
	            }
	        }
			Log.d("MyApp","End scan");
			return ad;
		}	
	}
}