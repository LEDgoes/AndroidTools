package com.ledgoes.android;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.UUID;

public class BluetoothLogService {
  private static final boolean D = true;
  private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
  private static final UUID MY_UUID_SECURE = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");
  private static final String NAME_INSECURE = "BluetoothLogInsecure";
  private static final String NAME_SECURE = "BluetoothLogSecure";
  public static final int STATE_CONNECTED = 3;
  public static final int STATE_CONNECTING = 2;
  public static final int STATE_LISTEN = 1;
  public static final int STATE_NONE = 0;
  private static final String TAG = "BluetoothLogService";
  private final BluetoothAdapter mAdapter;
  private ConnectThread mConnectThread;
  private ConnectedThread mConnectedThread;
  private final Handler mHandler;
  private AcceptThread mInsecureAcceptThread;
  private AcceptThread mSecureAcceptThread;
  private int mState = STATE_NONE;

  public BluetoothLogService(Context paramContext, Handler paramHandler) {
    mHandler = paramHandler;
    mAdapter = BluetoothAdapter.getDefaultAdapter();
  }

  private void connectionFailed() {
    Message localMessage = mHandler.obtainMessage(MessageActivity.MESSAGE_TOAST);
    Bundle localBundle = new Bundle();
    localBundle.putString("toast", "Unable to connect device");
    localMessage.setData(localBundle);
    mHandler.sendMessage(localMessage);
    start();
  }

  private void connectionLost() {
    Message localMessage = this.mHandler.obtainMessage(MessageActivity.MESSAGE_TOAST);
    Bundle localBundle = new Bundle();
    localBundle.putString("toast", "Device connection was lost");
    localMessage.setData(localBundle);
    this.mHandler.sendMessage(localMessage);
    start();
  }

  /**
   * Set the current state of the chat connection
   * @param state  An integer defining the current connection state
   */
  private synchronized void setState(int state) {
      if (D) Log.d(TAG, "setState() " + mState + " -> " + state);
      mState = state;

      // Give the new state to the Handler so the UI Activity can update
      mHandler.obtainMessage(MessageActivity.MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
  }
  
  /**
   * Start the ConnectThread to initiate a connection to a remote device.
   * @param device  The BluetoothDevice to connect
   * @param secure Socket Security type - Secure (true) , Insecure (false)
   */
  public synchronized void connect(BluetoothDevice device, boolean secure) {
      Log.d(TAG, "connect to: " + device);

      // Cancel any thread attempting to make a connection
      if (mState == STATE_CONNECTING) {
          if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}
      }

      // Cancel any thread currently running a connection
      if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}

      // Start the thread to connect with the given device
      mConnectThread = new ConnectThread(device, secure);
      mConnectThread.start();
      setState(STATE_CONNECTING);
  }
  
  /**
   * Start the ConnectedThread to begin managing a Bluetooth connection
   * @param socket  The BluetoothSocket on which the connection was made
   * @param device  The BluetoothDevice that has been connected
   */
  public synchronized void connected(BluetoothSocket socket, BluetoothDevice
          device, final String socketType) {
      if (D) Log.d(TAG, "connected, Socket Type:" + socketType);

      // Cancel the thread that completed the connection
      if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}

      // Cancel any thread currently running a connection
      if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}

      // Cancel the accept thread because we only want to connect to one device
      if (mSecureAcceptThread != null) {
          mSecureAcceptThread.cancel();
          mSecureAcceptThread = null;
      }
      if (mInsecureAcceptThread != null) {
          mInsecureAcceptThread.cancel();
          mInsecureAcceptThread = null;
      }

      // Start the thread to manage the connection and perform transmissions
      mConnectedThread = new ConnectedThread(socket, socketType);
      mConnectedThread.start();

      // Send the name of the connected device back to the UI Activity
      Message msg = mHandler.obtainMessage(MessageActivity.MESSAGE_DEVICE_NAME);
      Bundle bundle = new Bundle();
      bundle.putString(MessageActivity.DEVICE_NAME, device.getName());
      msg.setData(bundle);
      mHandler.sendMessage(msg);

      setState(STATE_CONNECTED);
  }
  
  /**
   * Return the current connection state. */
  public synchronized int getState() {
      return mState;
  }

  /**
   * Start the chat service. Specifically start AcceptThread to begin a
   * session in listening (server) mode. Called by the Activity onResume() */
  public synchronized void start() {
      Log.d(TAG, "start");

      // Cancel any thread attempting to make a connection
      if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}

      // Cancel any thread currently running a connection
      if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}

      setState(STATE_LISTEN);

      // Start the thread to listen on a BluetoothServerSocket
      if (mSecureAcceptThread == null) {
          mSecureAcceptThread = new AcceptThread(true);
          mSecureAcceptThread.start();
      }
      if (mInsecureAcceptThread == null) {
          mInsecureAcceptThread = new AcceptThread(false);
          mInsecureAcceptThread.start();
      }
  }
  
  /**
   * Stop all threads
   */
  public synchronized void stop() {
      if (D) Log.d(TAG, "stop");

      if (mConnectThread != null) {
          mConnectThread.cancel();
          mConnectThread = null;
      }

      if (mConnectedThread != null) {
          mConnectedThread.cancel();
          mConnectedThread = null;
      }

      if (mSecureAcceptThread != null) {
          mSecureAcceptThread.cancel();
          mSecureAcceptThread = null;
      }

      if (mInsecureAcceptThread != null) {
          mInsecureAcceptThread.cancel();
          mInsecureAcceptThread = null;
      }
      setState(STATE_NONE);
  }
  
  /**
   * Write to the ConnectedThread in an unsynchronized manner
   * @param out The bytes to write
   * @see ConnectedThread#write(byte[])
   */
  public void write(byte[] out) {
      // Create temporary object
      ConnectedThread r;
      // Synchronize a copy of the ConnectedThread
      synchronized (this) {
          if (mState != STATE_CONNECTED) return;
          r = mConnectedThread;
      }
      // Perform the write unsynchronized
      r.write(out);
  }

  private class AcceptThread extends Thread {
	  
	  private String mSocketType;
	  private final BluetoothServerSocket mmServerSocket;
	
	  public AcceptThread(boolean flag) {
		  BluetoothServerSocket tmp = null;
	      mSocketType = (flag) ? "Secure" : "Insecure";
	      try {
		      if (flag)
		    	  tmp = mAdapter.listenUsingRfcommWithServiceRecord("BluetoothLogSecure", BluetoothLogService.MY_UUID_SECURE);
		      else
		    	  tmp = mAdapter.listenUsingRfcommWithServiceRecord("BluetoothLogInsecure", BluetoothLogService.MY_UUID);
	      } catch (Exception e) {
	    	  Log.e("BluetoothLogService", (new StringBuilder("Socket Type: ")).append(mSocketType).append("listen() failed").toString(), e);
	      }
	      mmServerSocket = tmp;
	  }

      public void cancel() {
    	  try {
	          Log.d("BluetoothLogService", (new StringBuilder("Socket Type")).append(mSocketType).append("cancel ").append(this).toString());
	          mmServerSocket.close();
    	  } catch (Exception e) {
    		  Log.e("BluetoothLogService", (new StringBuilder("Socket Type")).append(mSocketType).append("close() of server failed").toString(), e);
    		  return;
    	  }
      }

	  public void run() {
	      Log.d("BluetoothLogService", (new StringBuilder("Socket Type: ")).append(mSocketType).append("BEGIN mAcceptThread").append(this).toString());
	      setName((new StringBuilder("AcceptThread")).append(mSocketType).toString());
	      BluetoothSocket bs;
	      while (mState != STATE_CONNECTED) {
	          try {
	              // This is a blocking call and will only return on a
	              // successful connection or an exception
	              bs = mmServerSocket.accept();
	          } catch (IOException e) {
	              Log.e(TAG, "Socket Type: " + mSocketType + "accept() failed", e);
	              break;
	          } catch (NullPointerException e) {
	        	  Log.e(TAG, "Bluetooth was not enabled on the device.", e);
	        	  break;
	          }
	
	          // If a connection was accepted
	          if (bs != null) {
	              synchronized (BluetoothLogService.this) {
	                  switch (mState) {
	                  case STATE_LISTEN:
	                  case STATE_CONNECTING:
	                      // Situation normal. Start the connected thread.
	                      connected(bs, bs.getRemoteDevice(),
	                              mSocketType);
	                      break;
	                  case STATE_NONE:
	                  case STATE_CONNECTED:
	                      // Either not ready or already connected. Terminate new socket.
	                      try {
	                          bs.close();
	                      } catch (IOException e) {
	                          Log.e(TAG, "Could not close unwanted socket", e);
	                      }
	                      break;
	                  }
	              }
	          }
	      }
	  }
  }

  
  private class ConnectThread extends Thread {
	  private String mSocketType;
      private final BluetoothDevice mmDevice;
      private final BluetoothSocket mmSocket;
      
	  public void cancel() {
          try {
              mmSocket.close();
          } catch (IOException e) {
              Log.e(TAG, "close() of connect " + mSocketType + " socket failed", e);
          }
      }

      public void run() {
          Log.i("BluetoothLogService", "BEGIN mConnectThread SocketType:" + mSocketType);
          setName("ConnectThread" + mSocketType);

          // Always cancel discovery because it will slow down a connection
          mAdapter.cancelDiscovery();

          // Make a connection to the BluetoothSocket
          try {
              // This is a blocking call and will only return on a
              // successful connection or an exception
              mmSocket.connect();
          } catch (IOException e) {
              // Close the socket
              try {
            	  e.printStackTrace();
                  mmSocket.close();
              } catch (IOException e2) {
                  Log.e(TAG, "unable to close() " + mSocketType +
                          " socket during connection failure", e2);
              }
              connectionFailed();
              return;
          }

          // Reset the ConnectThread because we're done
          synchronized (BluetoothLogService.this) {
              mConnectThread = null;
          }

          // Start the connected thread
          connected(mmSocket, mmDevice, mSocketType);
      }

      public ConnectThread(BluetoothDevice bluetoothdevice, boolean flag) {
          BluetoothSocket bs = null;
          mmDevice = bluetoothdevice;
          mSocketType = flag ? "Secure" : "Insecure";

          // Get a BluetoothSocket for a connection with the
          // given BluetoothDevice
          try {
              if (flag) {
            	  bs = bluetoothdevice.createRfcommSocketToServiceRecord(BluetoothLogService.MY_UUID_SECURE);
              } else {
	              // Option 2: bs = bluetoothdevice.createInsecureRfcommSocketToServiceRecord(BluetoothLogService.MY_UUID);
	        	  bs = bluetoothdevice.createRfcommSocketToServiceRecord(BluetoothLogService.MY_UUID);
              }
          } catch (IOException e) {
			e.printStackTrace();
		}
          mmSocket = bs;
      }
  }

  private class ConnectedThread extends Thread {
      private final InputStream mmInStream;
      private final OutputStream mmOutStream;
      private final BluetoothSocket mmSocket;

      public void cancel() {
          try {
              mmSocket.close();
          } catch (IOException e) {
              Log.e(TAG, "close() of connect socket failed", e);
          }
      }

      public void run()
      {
          Log.i("BluetoothLogService", "\n\n\nBEGIN mConnectedThread");
          byte abyte0[] = new byte[256];
          try {
              do {
                  int i = mmInStream.read(abyte0);
                  Log.i("BluetoothLogService", "\n\n\nRead Buffer:" + new String(abyte0));
                  mHandler.obtainMessage(2, i, -1, abyte0).sendToTarget();
                  // Message message = mHandler.obtainMessage(5);
                  // Bundle bundle = new Bundle();
                  // bundle.putString("toast", "Bluetooth Message Read Length : " + i);
                  // message.setData(bundle);
                  // mHandler.sendMessage(message);
              } while(true);
          }
          catch(IOException ioexception)
          {
              Log.e("BluetoothLogService", "disconnected", ioexception);
          }
          connectionLost();
      }

      /**
       * Write to the connected OutStream.
       * @param buffer  The bytes to write
       */
      public void write(byte[] buffer) {
          try {
              mmOutStream.write(buffer);

              // Share the sent message back to the UI Activity
              mHandler.obtainMessage(MessageActivity.MESSAGE_WRITE, -1, -1, buffer)
                      .sendToTarget();
          } catch (IOException e) {
              Log.e(TAG, "Exception during write", e);
          }
      }

      public ConnectedThread(BluetoothSocket bs, String s) {
          InputStream tmpIn = null;
          OutputStream tmpOut = null;
          Log.d("BluetoothLogService", "create ConnectedThread: " + s);
          mmSocket = bs;
       // Get the BluetoothSocket input and output streams
          try {
              tmpIn = bs.getInputStream();
              tmpOut = bs.getOutputStream();
          } catch (IOException e) {
              Log.e(TAG, "temp sockets not created", e);
          }

          mmInStream = tmpIn;
          mmOutStream = tmpOut;
      }
  }
}