package com.example.ledonoff;

import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

public class LEDOnOff extends Activity {
  private static final String TAG = "LEDOnOff";

  Button btnOn, btnOff;

  private static final int REQUEST_ENABLE_BT = 1;
  private BluetoothAdapter btAdapter = null;
  private BluetoothSocket btSocket = null;
  private OutputStream outStream = null;

  // Well known SPP UUID
  private static final UUID MY_UUID =
      UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

  // Insert your bluetooth devices MAC address
  private static String address = "00:00:00:00:00:00";

  /**
   * Called when the activity is first created.
   */
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    Log.d(TAG, "In onCreate()...");

    setContentView(R.layout.main);

    btnOn = (Button) findViewById(R.id.btnOn);
    btnOff = (Button) findViewById(R.id.btnOff);

    if (address.equals("00:00:00:00:00:00")) {
      String msg = "Update your server address from 00:00:00:00:00:00 to the correct address on line 34 in the java code";
      errorExit("Fatal Error", msg);
    }

    btAdapter = BluetoothAdapter.getDefaultAdapter();

    Log.d(TAG, "...Checking BT State...");

    // Emulator doesn't support Bluetooth and will return null
    if (btAdapter == null) {
      errorExit("Fatal Error", "Bluetooth Not supported. Aborting.");
    } else {
      if (btAdapter.isEnabled()) {
        Log.d(TAG, "...Bluetooth is enabled...");
      } else {
        Log.d(TAG, "...Need to enable Bluetooth...");
        //Prompt user to turn on Bluetooth
        Intent enableBtIntent = new Intent(btAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
      }
    }

    btnOn.setOnClickListener(new OnClickListener() {
      public void onClick(View v) {
        sendData("1");
        Toast msg = Toast.makeText(getBaseContext(),
            "You have clicked On", Toast.LENGTH_SHORT);
        msg.show();
      }
    });

    btnOff.setOnClickListener(new OnClickListener() {
      public void onClick(View v) {
        sendData("0");
        Toast msg = Toast.makeText(getBaseContext(),
            "You have clicked Off", Toast.LENGTH_SHORT);
        msg.show();
      }
    });
  }

  @Override
  public void onResume() {
    super.onResume();

    Log.d(TAG, "...In onResume");

    if (btAdapter.isEnabled()) {
      Log.d(TAG, "Attempting client connect...");

      // Set up a pointer to the remote node using it's address.
      BluetoothDevice device = btAdapter.getRemoteDevice(address);

      // Two things are needed to make a connection:
      //   A MAC address, which we got above.
      //   A Service ID or UUID.  In this case we are using the
      //     UUID for SPP.
      try {
        btSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
      } catch (IOException e) {
        errorExit("Fatal Error", "In onResume() and socket create failed: " + e.getMessage() + ".");
      }

      // Discovery is resource intensive.  Make sure it isn't going on
      // when you attempt to connect and pass your message.
      btAdapter.cancelDiscovery();

      // Establish the connection.  This will block until it connects.
      Log.d(TAG, "...Connecting to Remote...");
      try {
        btSocket.connect();
        Log.d(TAG, "...Connection established and data link opened...");
      } catch (IOException e) {
        try {
          Log.d(TAG, "...Closing Socket...");
          btSocket.close();
        } catch (IOException e2) {
          errorExit("Fatal Error", "In onResume() and unable to close socket during connection failure" + e2.getMessage() + ".");
        }
      }

      // Create a data stream so we can talk to server.
      Log.d(TAG, "...Creating Socket...");

      try {
        outStream = btSocket.getOutputStream();
      } catch (IOException e) {
        errorExit("Fatal Error", "In onResume() and output stream creation failed:" + e.getMessage() + ".");
      }
    }
  }

  @Override
  public void onPause() {
    super.onPause();

    Log.d(TAG, "In onPause()...");

    if (btAdapter.isEnabled()) {
      if (outStream != null) {
        try {
          outStream.flush();
        } catch (IOException e) {
          errorExit("Fatal Error", "In onPause() and failed to flush output stream: " + e.getMessage() + ".");
        }
      }

      try {
        btSocket.close();
      } catch (IOException e2) {
        errorExit("Fatal Error", "In onPause() and failed to close socket." + e2.getMessage() + ".");
      }
    }
  }

  private void errorExit(String title, String message) {
    Log.d(TAG, "In errorExit...  " + message);
    Toast.makeText(getBaseContext(), title + " - " + message, Toast.LENGTH_LONG).show();
    finish();
  }

  private void sendData(String message) {
    byte[] msgBuffer = message.getBytes();

    Log.d(TAG, "In sendData, sending data: " + message + "...");

    try {
      outStream.write(msgBuffer);
    } catch (IOException e) {
      String msg = "In onResume() and an exception occurred during write: " + e.getMessage();
      msg = msg + ".\n\nCheck that the SPP UUID: " + MY_UUID.toString() + " exists on server.\n\n";

      errorExit("Fatal Error", msg);
    }
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == REQUEST_ENABLE_BT) {
      if (resultCode == RESULT_OK) {
        Toast.makeText(getBaseContext(), "Bluetooth is now Enabled", Toast.LENGTH_SHORT).show();
      } else if (resultCode == RESULT_CANCELED) {
        errorExit("Fatal Error", "User cancelled Bluetooth Enable...");
      }
    }
  }
}