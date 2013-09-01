package com.ledgoes.android;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothSocket;

public class MyApp {
	public static BluetoothLogService mLogService = null;
	public static BluetoothSocket btSocket = null;
	public static BluetoothAdapter mBluetoothAdapter = null;
    static String[] messages;               // Set up enough strings to contain the desired # of messages
}
