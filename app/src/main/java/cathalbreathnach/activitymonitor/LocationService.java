package cathalbreathnach.activitymonitor;

import android.annotation.SuppressLint;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

// Service Class to listen for location updates and add entries to the databases
// Also broadcasts location updates for the MapsActivity class to receive
public class LocationService extends JobService implements LocationListener{

    private double currentLatitude;
    private double currentLongitude;
    private long lastTimeUpdate;

    private boolean locationChanged;

    private LocationManager locationManager;

    @Override
    public boolean onStartJob(final JobParameters jobParameters) {

        // Create a handler to delay allow the sensor to be checked for 60 seconds
        // pushing an update
        final Handler delayHandler = new Handler();

        delayHandler.postDelayed(new Runnable() {
            @Override
            public void run() {

                //check that a location update is available
                if (locationChanged){

                    Log.d("DEBUG","Location service uploading location result") ;

                    DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();

                    // Create new record and add to database
                    LocationRecord locData = new LocationRecord(currentLatitude,
                            currentLongitude, lastTimeUpdate);

                    databaseReference.child("locations")
                            .child(Long.toString(lastTimeUpdate)).setValue(locData);

                    // Send Broadcast update with new Location data
                    Intent locUpdate = new Intent("newLocation");
                    locUpdate.putExtra("Lat", currentLatitude);
                    locUpdate.putExtra("Long", currentLongitude);
                    locUpdate.putExtra("Time", lastTimeUpdate);
                    sendBroadcast(locUpdate);
                }

                // Stop this job thread once we have updated the value
                //stopSelf();
                jobFinished(jobParameters, false);

            }
        }, 1000 * 60  ); // 60 seconds

        return true;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        return false;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        // Request Android not to kill the service
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("DEBUG","Location Service Created");

        locationChanged = false;

        //start the location listener
        startGettingLocations();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if(locationManager != null){
            //noinspection MissingPermission
            locationManager.removeUpdates(this);
        }

        Log.d("DEBUG","Location Service Stopped");
    }

    // Method to start getting the locations
    // Permissions are requested in the MainActivity Class and do not need to be requested here
    // @SuppressLint("MissingPermission")
    @SuppressLint("MissingPermission")
    private void startGettingLocations(){

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        boolean isGPS = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean isNetwork = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        // Set change paramters to take less time as the thread called in onStartJob will
        // only take the most recent location for the updates
        // Give the phone a chance to get its location within reason
        long MIN_DISTANCE_CHANGE_FOR_UPDATES = 10; // Distance in meters - 10m
        long MIN_TIME_BW_UPDATES = 1000*10; // Time in milliseconds - 10 seconds


        //Starts requesting location updates
        if (isGPS) {
            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    MIN_TIME_BW_UPDATES,
                    MIN_DISTANCE_CHANGE_FOR_UPDATES, this);

        }
        // Look for Network locations id GPS is not available
        else if (isNetwork) {
            locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    MIN_TIME_BW_UPDATES,
                    MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
        }
    }

    //Called when the Location listener detects a change
    @Override
    public void onLocationChanged(Location location) {

        Log.d("DEBUG","Location Change Detected");

        // Update Values
        lastTimeUpdate = System.currentTimeMillis();
        currentLatitude = location.getLatitude();
        currentLongitude = location.getLongitude();
        locationChanged = true;
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {
    }

    @Override
    public void onProviderEnabled(String s) {
    }

    // If Location Services are not enabled, the user is directed to those
    @Override
    public void onProviderDisabled(String s) {
        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        startActivity(intent);
    }
}
