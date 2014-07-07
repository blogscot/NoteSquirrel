package com.diamond.iain.notesquirrel;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.ImageView;
import android.widget.Toast;

public class ImageActivity extends Activity implements OnTouchListener {

	ImageView image;
	private List<Point> points = new ArrayList<Point>();
	private Database db = new Database(this);

	public final static String PASSPOINT_SET = "PASSPOINT_KEY";
	private final static int NUM_POINTS = 4;
	private final static int CLOSE_ENOUGH = 60;
	SharedPreferences sharedPrefs;
	private static boolean resetPasspoints = false; 

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_image);

		ImageView image = (ImageView) findViewById(R.id.touch_image);
		image.setOnTouchListener(this);

	}
	
	@Override
	protected void onPause() {
		super.onPause();

		Bitmap bm = BitmapFactory.decodeResource(getResources(), R.drawable.squirrel);
		
		NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this)
		.setSmallIcon(R.drawable.smallsquirrel)
		.setLargeIcon(bm)
		.setContentTitle("Click to resume")
		.setContentText("Note Squirrel")
		.setAutoCancel(true);
		
		Intent resultIntent[] = new Intent[1];
		resultIntent[0] = new Intent(this, ImageActivity.class);
		PendingIntent pi = PendingIntent.getActivities(this, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		mBuilder.setContentIntent(pi);
		NotificationManager mNotificationManager = 
				(NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
		mNotificationManager.notify(0, mBuilder.build());		
	}

	@Override
	protected void onResume() {
		super.onResume();

		sharedPrefs = getPreferences(MODE_PRIVATE);
		boolean arePasspointsSet = sharedPrefs.getBoolean(PASSPOINT_SET, false);
		if (!arePasspointsSet || resetPasspoints) {
			showPrompt();
		}

	}

	private void showPrompt() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);

		builder.setPositiveButton("OK", new OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {

			}
		});

		builder.setTitle("Create Your Passpoint Sequence");
		builder.setMessage("Touch four points on the image to define touch lock. Touch the same four points to unlock.");
		AlertDialog dlg = builder.create();
		dlg.show();
	}

	public static void resetPasspoints() {
		resetPasspoints = true;
	}
	
	private void savePasspoints(final List<Point> points) {
		AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {

			@Override
			protected Void doInBackground(Void... params) {
				db.store(points);
				Log.d(MainActivity.DEBUG_TAG, "Points stored.");
				return null;
			}

			@Override
			protected void onPostExecute(Void result) {
				super.onPostExecute(result);
				sharedPrefs = getPreferences(MODE_PRIVATE);
				SharedPreferences.Editor editor = sharedPrefs.edit();
				editor.putBoolean(PASSPOINT_SET, true);
				editor.commit();

				points.clear();
			}

		};
		task.execute();
		Toast.makeText(this, R.string.points_stored, Toast.LENGTH_LONG)
				.show();		
	}
	
	private void verifyPasspoints(final List<Point> userPoints) {
		
		AsyncTask<Void, Void, Boolean> task = new AsyncTask<Void, Void, Boolean>() {

			@Override
			protected Boolean doInBackground(Void... params) {
				
				List<Point> savedPoints = db.getPoints();
				
				Log.d(MainActivity.DEBUG_TAG, "Saved points: " + savedPoints.size());
				
				if(savedPoints.size() != userPoints.size()) {
					Log.d(MainActivity.DEBUG_TAG, "Saved points: " + savedPoints.size());
					Log.d(MainActivity.DEBUG_TAG, "User points: " + userPoints.size());
					return false;
				}
				
				for(int i=0;i<NUM_POINTS;i++){
					Point saved = savedPoints.get(i);
					Point user = userPoints.get(i);
					
					int diffX = Math.abs(saved.x - user.x);
					int diffY = Math.abs(saved.y - user.y);

					Log.d(MainActivity.DEBUG_TAG, "diffX: " + diffX);
					Log.d(MainActivity.DEBUG_TAG, "diffY: " + diffY);
					
					if (diffX > CLOSE_ENOUGH || diffY > CLOSE_ENOUGH) {
						return false;
					}
				}
				return true;
			}

			@Override
			protected void onPostExecute(Boolean passVerified) {
				points.clear();
				
				if(passVerified) {
					Intent intent = new Intent(ImageActivity.this, MainActivity.class);
					startActivity(intent);
				} else {
					Toast.makeText(ImageActivity.this, R.string.access_denied, Toast.LENGTH_LONG).show();
				}
			}
		};
		
		task.execute();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.image, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	public boolean onTouch(View v, MotionEvent event) {
		
		sharedPrefs = getPreferences(MODE_PRIVATE);
		boolean arePasspointsSet = sharedPrefs.getBoolean(PASSPOINT_SET, false);
		
		int posX = Math.round(event.getX());
		int posY = Math.round(event.getY());
		
		String message = String.format(Locale.UK, "Coordinates: (%d, %d)", posX, posY);

		Log.d(MainActivity.DEBUG_TAG, message);

		points.add(new Point(posX, posY));

		if (points.size() == NUM_POINTS) {
			if (arePasspointsSet && !resetPasspoints) {
				verifyPasspoints(points);
			} else {
				savePasspoints(points);
				resetPasspoints = false;
			}
		}
		return false;
	}
}
