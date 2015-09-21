package com.symbol.basicscanningtutorial;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.view.KeyEvent;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.symbol.emdk.EMDKManager;
import com.symbol.emdk.EMDKManager.EMDKListener;
import com.symbol.emdk.EMDKManager.FEATURE_TYPE;
import com.symbol.emdk.EMDKResults;
import com.symbol.emdk.barcode.BarcodeManager;
import com.symbol.emdk.barcode.BarcodeManager.DeviceIdentifier;
import com.symbol.emdk.barcode.ScanDataCollection;
import com.symbol.emdk.barcode.ScanDataCollection.LabelType;
import com.symbol.emdk.barcode.ScanDataCollection.ScanData;
import com.symbol.emdk.barcode.Scanner;
import com.symbol.emdk.barcode.Scanner.DataListener;
import com.symbol.emdk.barcode.Scanner.StatusListener;
import com.symbol.emdk.barcode.Scanner.TriggerType;
import com.symbol.emdk.barcode.ScannerConfig;
import com.symbol.emdk.barcode.ScannerException;
import com.symbol.emdk.barcode.ScannerResults;
import com.symbol.emdk.barcode.StatusData;
import com.symbol.emdk.barcode.StatusData.ScannerStates;

import java.util.ArrayList;

public class MainActivity extends Activity implements EMDKListener,
		StatusListener, DataListener {
	// Declare a variable to store EMDKManager object
	private EMDKManager emdkManager = null;

	// Declare a variable to store Barcode Manager object
	private BarcodeManager barcodeManager = null;

	// Declare a variable to hold scanner device to scan
	private Scanner scanner = null;

	// Text view to display status of EMDK and Barcode Scanning Operations
	private TextView statusTextView = null;

	// Edit Text that is used to display scanned barcode data
	private EditText dataView = null;

	// Boolean to explain whether the scanning is in progress or not at any
	// specific point of time
	boolean isScanning = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// Reference to UI elements
		statusTextView = (TextView) findViewById(R.id.textViewStatus);
		dataView = (EditText) findViewById(R.id.editText1);

        Settings.System.putInt(getContentResolver(), Settings.System.ACCELEROMETER_ROTATION, 0);

		// The EMDKManager object will be created and returned in the callback.
		EMDKResults results = EMDKManager.getEMDKManager(
				getApplicationContext(), this);
		// Check the return status of getEMDKManager and update the status Text
		// View accordingly
		if (results.statusCode != EMDKResults.STATUS_CODE.SUCCESS) {
			statusTextView.setText("EMDKManager Request Failed");
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (emdkManager != null) {

			// Clean up the objects created by EMDK manager
			emdkManager.release();
			emdkManager = null;
		}
	}

	@Override
	protected void onStop() {
		// TODO Auto-generated method stub
		super.onStop();
		try {
			if (scanner != null) {
				// releases the scanner hardware resources for other application
				// to use. You must call this as soon as you're done with the
				// scanning.
				scanner.disable();
				scanner = null;
			}
		} catch (ScannerException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onClosed() {
		// TODO Auto-generated method stub
		// The EMDK closed abruptly. // Clean up the objects created by EMDK
		// manager
		if (this.emdkManager != null) {

			this.emdkManager.release();
			this.emdkManager = null;
		}
	}

	@Override
	public void onOpened(EMDKManager emdkManager) {
		// TODO Auto-generated method stub
		this.emdkManager = emdkManager;
		// Method call to set some decoder parameters to scanner
		setScannerParameters();

		// Toast to indicate that the user can now start scanning
		Toast.makeText(MainActivity.this,
				"Press Hard Scan Button to start scanning...",
				Toast.LENGTH_SHORT).show();

	}

	@Override
	public void onData(ScanDataCollection scanDataCollection) {
		// TODO Auto-generated method stub
		// Use the scanned data, process it on background thread using AsyncTask
		// and update the UI thread with the scanned results
		new AsyncDataUpdate().execute(scanDataCollection);

	}

	// Update the scan data on UI
	int dataLength = 0;

	// AsyncTask that configures the scanned data on background
	// thread and updated the result on UI thread with scanned data and type of
	// label
	private class AsyncDataUpdate extends
			AsyncTask<ScanDataCollection, Void, String> {

		@Override
		protected String doInBackground(ScanDataCollection... params) {
			ScanDataCollection scanDataCollection = params[0];

			// Status string that contains both barcode data and type of barcode
			// that is being scanned
			String statusStr = "";

			// The ScanDataCollection object gives scanning result and the
			// collection of ScanData. So check the data and its status
			if (scanDataCollection != null
					&& scanDataCollection.getResult() == ScannerResults.SUCCESS) {

				ArrayList<ScanData> scanData = scanDataCollection.getScanData();

				// Iterate through scanned data and prepare the statusStr
				for (ScanData data : scanData) {
					// Get the scanned data
					String barcodeData = data.getData();
					// Get the type of label being scanned
					LabelType labelType = data.getLabelType();
					// Concatenate barcode data and label type
					statusStr = barcodeData + " " + labelType;
				}
			}

			// Return result to populate on UI thread
			return statusStr;
		}

		@Override
		protected void onPostExecute(String result) {
			// Update the dataView EditText on UI thread with barcode data and
			// its label type
			if (dataLength++ > 50) {
				// Clear the cache after 50 scans
				dataView.getText().clear();
				dataLength = 0;
			}
			dataView.append(result + "\n");
		}

		@Override
		protected void onPreExecute() {
		}

		@Override
		protected void onProgressUpdate(Void... values) {
		}
	}

	@Override
	public void onStatus(StatusData statusData) {
		// TODO Auto-generated method stub
		// process the scan status event on the background thread using
		// AsyncTask and update the UI thread with current scanner state
		new AsyncStatusUpdate().execute(statusData);

	}

	// AsyncTask that configures the current state of scanner on background
	// thread and updates the result on UI thread
	private class AsyncStatusUpdate extends AsyncTask<StatusData, Void, String> {

		@Override
		protected String doInBackground(StatusData... params) {
			// Get the current state of scanner in background
			StatusData statusData = params[0];
			String statusStr = "";
			ScannerStates state = statusData.getState();
			// Different states of Scanner
			switch (state) {
			// Scanner is IDLE
			case IDLE:
				statusStr = "The scanner enabled and its idle";
				isScanning = false;
				break;
			// Scanner is SCANNING
			case SCANNING:
				statusStr = "Scanning..";
				isScanning = true;
				break;
			// Scanner is waiting for trigger press
			case WAITING:
				statusStr = "Waiting for trigger press..";
				break;
			default:
				break;
			}
			// Return result to populate on UI thread
			return statusStr;
		}

		@Override
		protected void onPostExecute(String result) {
			// Update the status text view on UI thread with current scanner
			// state
			statusTextView.setText(result);
		}

		@Override
		protected void onPreExecute() {
		}

		@Override
		protected void onProgressUpdate(Void... values) {
		}
	}

	// This is a callback method when user presses any hardware button on the
	// device
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {

		// check for scanner hard key press.
		if ((keyCode == KeyEvent.KEYCODE_BUTTON_L1)
				|| (keyCode == KeyEvent.KEYCODE_BUTTON_R1)) {

			// Skip the key press if the repeat count is not zero.
			if (event.getRepeatCount() != 0) {
				return true;
			}

			try {
				if (scanner == null) {
					initializeScanner();
				}

				if ((scanner != null) && (isScanning == false)) {
					// Starts an asynchronous Scan. The method will not turn on
					// the scanner. It will, however, put the scanner in a state
					// in which the scanner can be turned ON either by pressing
					// a hardware trigger or can be turned ON automatically.
					scanner.read();
				}

			} catch (Exception e) {
				// Display if there is any exception while performing operation
				statusTextView.setText(e.getMessage());
			}
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	// This is a callback method when user releases any hardware button on the
	// device
	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {

		// check for scanner trigger key press.
		if ((keyCode == KeyEvent.KEYCODE_BUTTON_L1)
				|| (keyCode == KeyEvent.KEYCODE_BUTTON_R1)) {

			// Skip the key press if the repeat count is not zero.
			if (event.getRepeatCount() != 0) {
				return true;
			}

			try {
				if ((scanner != null) && (isScanning == true)) {
					// This Cancels any pending asynchronous read() calls
					scanner.cancelRead();
				}
			} catch (Exception e) {
				statusTextView.setText(e.getMessage());
			}
			return true;
		}
		return super.onKeyUp(keyCode, event);
	}

	// Method to set some decoder parameters in the ScannerConfig object
	public void setScannerParameters() {
		try {

			if (scanner == null) {
				// Method call to initialize the scanner parameters
				initializeScanner();
			}

			ScannerConfig config = scanner.getConfig();
			// Set the code128
			config.decoderParams.code128.enabled = true;
			// set code39
			config.decoderParams.code39.enabled = true;
			// set UPCA
			config.decoderParams.upca.enabled = true;
			scanner.setConfig(config);

		} catch (Exception e) {
			statusTextView.setText(e.getMessage());
		}
	}

	// Method to initialize and enable Scanner and its listeners
	private void initializeScanner() throws ScannerException {

		if (scanner == null) {

			// Get the Barcode Manager object
			barcodeManager = (BarcodeManager) this.emdkManager
					.getInstance(FEATURE_TYPE.BARCODE);

			// Get default scanner defined on the device
			scanner = barcodeManager.getDevice(DeviceIdentifier.DEFAULT);
			// scanner = barcodeManager.getDevice(list.get(0));

			// Add data and status listeners
			scanner.addDataListener(this);
			scanner.addStatusListener(this);

			// The trigger type is set to HARD by default and HARD is not
			// implemented in this release.
			// So set to SOFT_ALWAYS
			scanner.triggerType = TriggerType.SOFT_ALWAYS;

			// Enable the scanner
			scanner.enable();
		}

	}

}
