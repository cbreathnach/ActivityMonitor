package cathalbreathnach.activitymonitor;

import android.app.Activity;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

// Main Activity and deals with the home pages functionality
// Starts the two app services and deals with the button
// Receives a broadcast from the StepsService to update the current step count
public class MainActivity extends Activity{

    private TextView stepDisplay;
    private String stepText;

    private JobScheduler jobScheduler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        stepDisplay = findViewById(R.id.stepDisplay);

        setStepCount();

        // Create job scheduler
        jobScheduler = (JobScheduler)getSystemService(JOB_SCHEDULER_SERVICE);


        //Check for Permissions and then start getting locations
        if(!checkPermission()){
            startLocationService();
        }

        // Start the step service
        startStepService();

        Log.d("DEBUG","Main Method Run");
    }

    // Start the location service
    // Uses the created job scheduler
    // Runs periodically every 15 minutes to check the location
    private void startLocationService(){

        ComponentName locationsName = new ComponentName(this, LocationService.class);
        JobInfo locationsJobInfo = new JobInfo.Builder(1, locationsName)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setPeriodic(1000 * 60 * 15)
                .build();

        jobScheduler.schedule(locationsJobInfo);

    }

    // Start the step service
    // Uses the created job scheduler
    // Runs periodically every 60 minutes to check the current step count
    private void startStepService(){

        ComponentName stepsName = new ComponentName(this, StepsService.class);
        JobInfo stepsJobInfo = new JobInfo.Builder(2, stepsName)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setPeriodic(1000 * 60 * 60)
                .build();

        jobScheduler.schedule(stepsJobInfo);
    }

    // Check Permissions for locations
    private boolean checkPermission() {

        // Below Android SDK 22, Permissions are granted from the Android Manifest
        // Check if permissions are already granted
        if (Build.VERSION.SDK_INT > 22 && ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission
                (this, android.Manifest.permission.ACCESS_COARSE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED) {

            // Else ask for permissions
            requestPermissions(new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION}, 100);

            return true;
        }
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if(requestCode == 100){

            // If granted, start location service
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1]
                    == PackageManager.PERMISSION_GRANTED){
                startLocationService();
            }

            // Else ask again
            else{
                checkPermission();
            }
        }
    }

    public void openMap(View view) {
        Intent intent = new Intent(this, MapsActivity.class);
        startActivity(intent);
    }

    public void openSteps(View view) {
        Intent intent = new Intent(this, StepsActivity.class);
        startActivity(intent);
    }

    private void setStepCount(){

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference databaseReference = database.getReference("currentStep");

        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                int steps = dataSnapshot.getValue(Integer.class);

                stepText = ("Last Recorded Step Count: " + steps);
                stepDisplay.setText(stepText);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d("DEBUG","Main Method Destroyed");
    }

}
