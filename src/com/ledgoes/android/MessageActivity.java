package com.ledgoes.android;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class MessageActivity extends Activity {

	public static final int MESSAGE_STATE_CHANGE = 1;
	public static final int MESSAGE_READ = 2;
	public static final int MESSAGE_WRITE = 3;
	public static final int MESSAGE_DEVICE_NAME = 4;
	public static final int MESSAGE_TOAST = 5;
	public static final int DEVICE_SELECT = 2;

	public static final String DEVICE_NAME = "device_name";
	private Context ctx = this;
	private ArrayAdapter<String> mConversationArrayAdapter;
	private String mConnectedDeviceName = null;
	private boolean started = false;
	private Button connectBluetooth;
	private Button send;
	public TextView connectedTo;

	// Here's a handler that handles Bluetooth events
	private Handler mHandler = new Handler() {
		public void handleMessage(Message m) {
			if (m.what == MESSAGE_STATE_CHANGE) {
				Log.i("M360PICKUPMGR", "MESSAGE_STATE_CHANGE: " + m.arg1);
				if (m.arg1 == 0 || m.arg1 == 1 || m.arg2 == 1)
					connectedTo.setText(R.string.title_not_connected);
				else if (m.arg1 == 2)
					connectedTo.setText(R.string.title_connecting);
				else if (m.arg1 == 3) {
					connectedTo.setText(R.string.title_connected_to);
					started = true;
					connectBluetooth.setText("Disconnect");
					if (mConnectedDeviceName == null)
						mConnectedDeviceName = "(No Name)";
					connectedTo.append(" - " + mConnectedDeviceName);
			        MyApp.messages = new String[1];
					DirectDrive.writeReady();
				}
			} else if (m.what == MESSAGE_WRITE) {
				String s2 = new String((byte[]) m.obj);
				// mConversationArrayAdapter.add("Me:  " + s2);
			} else if (m.what == MESSAGE_READ) {
				// There will not be any reads taking place from the BT serial adapter yet
			} else if (m.what == MESSAGE_DEVICE_NAME) {
				mConnectedDeviceName = m.getData().getString("device_name");
			} else if (m.what == MESSAGE_TOAST) {
				String t = m.getData().getString("toast");
				Toast.makeText(ctx, t, Toast.LENGTH_SHORT).show();
			}
			super.handleMessage(m);
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_message);
		
		connectBluetooth = (Button) findViewById(R.id.button1);
		send = (Button) findViewById(R.id.button2);
		connectedTo = (TextView) findViewById(R.id.lblConnectedTo);
		
		// Do Bluetooth
		Log.e("M360PICKUPMGR", "+++ ON CREATE +++");
		MyApp.mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if (MyApp.mBluetoothAdapter == null) {
			AlertDialog deleteAlert = new AlertDialog.Builder(ctx).create();
			deleteAlert.setTitle("Error");
			deleteAlert.setMessage("Bluetooth is not available on your device.");
			deleteAlert.setButton(DialogInterface.BUTTON_NEUTRAL, "OK", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}
			});
			deleteAlert.show();
			finish();
		}
		if (!MyApp.mBluetoothAdapter.isEnabled()) {
			MyApp.mBluetoothAdapter.enable();
		}
		Log.e("M360PICKUPMGR", "+++ DONE IN ON CREATE, GOT LOCAL BT ADAPTER +++");
		
		MyApp.mLogService = new BluetoothLogService(this, this.mHandler);

		/* ************************************************************
		 * CONNECT TO BLUETOOTH
		 * ************************************************************/
		connectBluetooth.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if(started) {
					started = false;
					MyApp.mLogService.stop();
					connectBluetooth.setText("Connect to Bluetooth");
				} else {
					Intent intent = new Intent(MessageActivity.this, com.ledgoes.android.DeviceListActivity.class);
					startActivityForResult(intent, DEVICE_SELECT);
				}
			}			
		});

		/* ************************************************************
		 * SEND A MESSAGE
		 * ************************************************************/
		send.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if(started) {
					DirectDrive.writeSingle( ((EditText) findViewById(R.id.editText1)).getText().toString() );
				}
			}			
		});

	}
	
	public void onActivityResult(int reqCode, int resultCode, Intent intent) {
		System.out.println("M360PICKUPMGR onActivityResult: (" + reqCode + ", " + resultCode + ")");
		if (reqCode == 1)
			return;
		else if (reqCode == DEVICE_SELECT)
			if (resultCode == DeviceListActivity.DEVICE_SELECTED)
				connectDevice(intent, false);
		else if (reqCode == 3)
			if (resultCode == -1)
				connectDevice(intent, false);
		else if (reqCode == 4) {
			if (resultCode == -1) {
				//setupLog();
			} else {
				System.out.println("M360PICKUPMGR BT not enabled");
				Toast.makeText(this, 0x7f040004, 0).show();
				finish();
			}
		} else if (reqCode == 5) {
			MyApp.mLogService.stop();
		} else if (reqCode == 6) {
			// mConversationArrayAdapter.clear();
		}
	}

	private void connectDevice(Intent i, boolean flag) {
		String str = i.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
		BluetoothDevice localBluetoothDevice = MyApp.mBluetoothAdapter.getRemoteDevice(str);
		MyApp.mLogService.connect(localBluetoothDevice, flag);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.message, menu);
		return true;
	}
	
    /**
     * Called when a menu item is selected.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	// Get the menu item selected
        switch (item.getItemId()) {
        case R.id.action_settings:
        	// User selected the Settings menu item, so show the Settings screen
            startActivity(new Intent(this, PreferencesActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
