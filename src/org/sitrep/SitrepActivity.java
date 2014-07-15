package org.sitrep;

import android.app.Activity;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.GpsStatus.Listener;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class SitrepActivity extends Activity implements LocationListener, Listener {

	private static final float MIN_ACCURACY = 50/* m */;
	private LocationManager gps;
	private Location currentBestLocation;
	private TextView logView;
	private GpsStatus status;
	private Button reportBtn;
	private boolean reportLocation = false;
	private boolean gpsOn;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_sitrep);
		gps = (LocationManager) getSystemService(LOCATION_SERVICE);
		logView = (TextView) findViewById(R.id.sitrep_log_text);
		reportBtn = (Button) findViewById(R.id.sitrep_report_btn);
		reportBtn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				append("Sending report.");
				if (!gpsOn)
					gpsOn();
				if (currentBestLocation != null) {
					reportLocation();
				} else {
					reportLocation = true;
				}
			}
		});
	}

	protected void reportLocation() {
		append("Report sent.");
		gpsOff();
	}

	@Override
	protected void onResume() {
		super.onResume();
		logView.setText("");
		if (!gps.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
			append("GPS is OFF !!!");
		}
		gpsOn();
	}

	private void gpsOn() {
		gpsOn = true;
		gps.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
		gps.addGpsStatusListener(this);
		currentBestLocation = null;
		append("Acquiring location");
	}

	@Override
	protected void onPause() {
		super.onPause();
		gpsOff();
	}

	private void gpsOff() {
		gps.removeUpdates(this);
		gps.removeGpsStatusListener(this);
		gpsOn = false;
	}

	private void append(String string) {
		logView.setText(string + "\n" + logView.getText());
	}

	@Override
	public void onLocationChanged(Location location) {
		if (location == null)
			return;
		if (isBetterLocation(location, currentBestLocation)) {
			currentBestLocation = location;
			if (currentBestLocation.getAccuracy() > MIN_ACCURACY) {
				return;
			}
			append(String.format("%.5f %.5f", location.getLatitude(), location.getLongitude()));
			if (reportLocation)
				reportLocation();
		}
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
	}

	@Override
	public void onProviderEnabled(String provider) {
	}

	@Override
	public void onProviderDisabled(String provider) {
	}

	private static final int TWO_MINUTES = 1000 * 60 * 2;

	/**
	 * Determines whether one Location reading is better than the current
	 * Location fix
	 * 
	 * @param location
	 *            The new Location that you want to evaluate
	 * @param currentBestLocation
	 *            The current Location fix, to which you want to compare the new
	 *            one
	 */
	protected boolean isBetterLocation(Location location, Location currentBestLocation) {
		if (currentBestLocation == null) {
			// A new location is always better than no location
			return true;
		}

		// Check whether the new location fix is newer or older
		long timeDelta = location.getTime() - currentBestLocation.getTime();
		boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
		boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
		boolean isNewer = timeDelta > 0;

		// If it's been more than two minutes since the current location, use
		// the new location
		// because the user has likely moved
		if (isSignificantlyNewer) {
			return true;
			// If the new location is more than two minutes older, it must be
			// worse
		} else if (isSignificantlyOlder) {
			return false;
		}

		// Check whether the new location fix is more or less accurate
		int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
		boolean isLessAccurate = accuracyDelta > 0;
		boolean isMoreAccurate = accuracyDelta < 0;
		boolean isSignificantlyLessAccurate = accuracyDelta > 200;

		// Check if the old and new location are from the same provider
		boolean isFromSameProvider = isSameProvider(location.getProvider(), currentBestLocation.getProvider());

		// Determine location quality using a combination of timeliness and
		// accuracy
		if (isMoreAccurate) {
			return true;
		} else if (isNewer && !isLessAccurate) {
			return true;
		} else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
			return true;
		}
		return false;
	}

	/** Checks whether two providers are the same */
	private boolean isSameProvider(String provider1, String provider2) {
		if (provider1 == null) {
			return provider2 == null;
		}
		return provider1.equals(provider2);
	}

	@Override
	public void onGpsStatusChanged(int event) {
		status = gps.getGpsStatus(status);
		int used = 0;
		int max = 0;
		for (GpsSatellite sat : status.getSatellites()) {
			if (sat.usedInFix())
				used++;
			max++;
		}
		append("Satelites: " + used + "/" + max);
	}
}
