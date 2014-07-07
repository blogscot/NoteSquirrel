package com.diamond.iain.notesquirrel;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class MainActivity extends Activity implements OnClickListener {

	public final static String DEBUG_TAG = "Dbg:";
	public final static String TEXTFILE = "notesquirrel.txt";
	public final static String FILESAVED = "fileSaved";
	public final static String RESET_PASSPOINTS = "ResetPasspoints";

	private SharedPreferences sharedPrefs;
	EditText editText;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		Button save = (Button) findViewById(R.id.bSave);
		Button lock = (Button) findViewById(R.id.bLock);
		save.setOnClickListener(this);
		lock.setOnClickListener(this);
		editText = (EditText) findViewById(R.id.etText);

		sharedPrefs = getPreferences(MODE_PRIVATE);
		boolean fileSaved = sharedPrefs.getBoolean(FILESAVED, false);
		if (fileSaved)
			loadSavedFile();
	}

	private void loadSavedFile() {
		try {
			FileInputStream fis = openFileInput(TEXTFILE);
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					new DataInputStream(fis)));
			String line;
			while ((line = reader.readLine()) != null) {
				editText.append(line);
				editText.append("\n");
			}
			fis.close();
		} catch (FileNotFoundException e) {
			Log.d(DEBUG_TAG, "Loading: FileNotFoundException");
		} catch (IOException e) {
			Log.d(DEBUG_TAG, "Loading: IOException");
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
		if (id == R.id.settings_reset) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		switch (item.getItemId()) {
		case R.id.settings_reset:
			ImageActivity.resetPasspoints();
			finish();
			return true;
		default:
			return super.onMenuItemSelected(featureId, item);
		}
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.bSave:
			String text = editText.getText().toString();

			try {
				FileOutputStream fos = openFileOutput(TEXTFILE,
						Context.MODE_PRIVATE);
				fos.write(text.getBytes());
				fos.close();
				Toast.makeText(this, R.string.file_saved_successfully,
						Toast.LENGTH_LONG).show();
			} catch (FileNotFoundException e) {
				Log.d(DEBUG_TAG, "Saving: FileNotFoundException");
			} catch (IOException e) {
				Log.d(DEBUG_TAG, "Saving: IOException");
			}

			sharedPrefs = getPreferences(MODE_PRIVATE);
			SharedPreferences.Editor editor = sharedPrefs.edit();
			editor.putBoolean(FILESAVED, true);
			editor.commit();
			break;
		case R.id.bLock:
			finish();
			break;
		}
	}
}
