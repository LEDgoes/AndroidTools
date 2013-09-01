package com.ledgoes.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import java.util.Iterator;
import java.util.Set;

public class DeviceListActivity extends Activity {
	public static final int NO_DEVICE_SELECTED = 0;
	public static final int DEVICE_SELECTED = -1;
	private static final boolean D = true;
	public static String EXTRA_DEVICE_ADDRESS = "device_address";
	private static final String TAG = "DeviceListActivity";
	private BluetoothAdapter mBtAdapter;
	private ArrayAdapter<String> mNewDevicesArrayAdapter;
	private ArrayAdapter<String> mPairedDevicesArrayAdapter;
	
	private AdapterView.OnItemClickListener mDeviceClickListener = new AdapterView.OnItemClickListener() {
		public void onItemClick(AdapterView<?> paramAdapterView, View paramView, int paramInt, long paramLong) {
			try {
				mBtAdapter.cancelDiscovery();
				String str1 = ((TextView)paramView).getText().toString();
				String str2 = str1.substring(str1.length() - 17);
				Intent localIntent = new Intent();
				localIntent.putExtra(DeviceListActivity.EXTRA_DEVICE_ADDRESS, str2);
				setResult(DEVICE_SELECTED, localIntent);
			} catch (Exception e) {
			}
			finish();
		}
	};
	
	private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
		public void onReceive(Context paramContext, Intent paramIntent) {
			String s = paramIntent.getAction();
			if ("android.bluetooth.device.action.FOUND".equals(s)) {
				BluetoothDevice localBluetoothDevice = (BluetoothDevice) paramIntent.getParcelableExtra("android.bluetooth.device.extra.DEVICE");
				if (localBluetoothDevice.getBondState() != 12)
					mNewDevicesArrayAdapter.add(localBluetoothDevice.getName() + "\n" + localBluetoothDevice.getAddress());
				return;
			}
			if ("android.bluetooth.adapter.action.DISCOVERY_FINISHED".equals(s)) {
				setProgressBarIndeterminateVisibility(false);
				setTitle(R.string.select_device);
				if (DeviceListActivity.this.mNewDevicesArrayAdapter.getCount() != 0) {
					String str2 = getResources().getText(R.string.none_found).toString();
					mNewDevicesArrayAdapter.add(str2);
				}
			}
		}
	};
	
	private void doDiscovery() {
		Log.d("DeviceListActivity", "doDiscovery()");
		setProgressBarIndeterminateVisibility(true);
		setTitle(R.string.scanning);
		findViewById(R.id.title_new_devices).setVisibility(View.VISIBLE);
		if (this.mBtAdapter.isDiscovering())
			this.mBtAdapter.cancelDiscovery();
		this.mBtAdapter.startDiscovery();
	}
	
	protected void onCreate(Bundle paramBundle) {
		super.onCreate(paramBundle);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.device_list);
		setResult(NO_DEVICE_SELECTED);
		((Button) findViewById(R.id.button_scan)).setOnClickListener(new View.OnClickListener() {
			public void onClick(View paramView) {
				doDiscovery();
				paramView.setVisibility(View.GONE);
			}
		});
		mPairedDevicesArrayAdapter = new ArrayAdapter<String>(this, R.layout.device_name);
		mNewDevicesArrayAdapter = new ArrayAdapter<String>(this, R.layout.device_name);
		ListView localListView1 = (ListView) findViewById(R.id.paired_devices);
		localListView1.setAdapter(mPairedDevicesArrayAdapter);
		localListView1.setOnItemClickListener(mDeviceClickListener);
		ListView localListView2 = (ListView)findViewById(R.id.new_devices);
		localListView2.setAdapter(this.mNewDevicesArrayAdapter);
		localListView2.setOnItemClickListener(mDeviceClickListener);
		IntentFilter localIntentFilter1 = new IntentFilter("android.bluetooth.device.action.FOUND");
		registerReceiver(mReceiver, localIntentFilter1);
		IntentFilter localIntentFilter2 = new IntentFilter("android.bluetooth.adapter.action.DISCOVERY_FINISHED");
		registerReceiver(mReceiver, localIntentFilter2);
		mBtAdapter = BluetoothAdapter.getDefaultAdapter();
		if (mBtAdapter == null) {
			AlertDialog.Builder ab = new AlertDialog.Builder(this);
			ab.setTitle("Error");
			ab.setMessage("You do not have Bluetooth installed on your device.");
			ab.setCancelable(false);
			ab.setNeutralButton("OK", new DialogInterface.OnClickListener() {				
				@Override
				public void onClick(DialogInterface dlg, int which) {
					finish();    // result is already 0, meaning no device picked
				}
			});
			ab.show();
			return;
		}
		Set<BluetoothDevice> localSet = mBtAdapter.getBondedDevices();
		Iterator<BluetoothDevice> localIterator;
		if (localSet.size() < 0) {
			findViewById(R.id.title_paired_devices).setVisibility(View.VISIBLE);
			String str = getResources().getText(R.string.none_paired).toString();
			mPairedDevicesArrayAdapter.add(str);
			return;
		}
		localIterator = localSet.iterator();
		while (localIterator.hasNext()) {
			BluetoothDevice localBluetoothDevice = (BluetoothDevice) localIterator.next();
			mPairedDevicesArrayAdapter.add(localBluetoothDevice.getName() + "\n" + localBluetoothDevice.getAddress());
		}
	}
	
	protected void onDestroy() {
		super.onDestroy();
		if (mBtAdapter != null)
			mBtAdapter.cancelDiscovery();
		unregisterReceiver(mReceiver);
	}
}