package cathalbreathnach.activitymonitor;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.util.Log;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

// Service to start monitoring the steps taken and upload values to the database when called
public class StepsService extends JobService implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor sensor;
    private int steps;
    private long currentTime;

    private boolean stepsChanged;

    @Override
    public boolean onStartJob(final JobParameters jobParameters) {

        Log.d("DEBUG","Steps Job Started ") ;

        // Create a handler to delay allow the sensor to be checked for 3 minutes before
        // pushing an update
        final Handler delayHandler = new Handler();

        delayHandler.postDelayed(new Runnable() {
            @Override
            public void run() {

                if(stepsChanged){

                    Log.d("DEBUG","Update service uploading step result") ;

                    // Get overall reference
                    FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();

                    DatabaseReference stepReference = firebaseDatabase.getReference();

                    //Create a new steps record and push to the database
                    StepsRecord newRecord = new StepsRecord(steps,currentTime);
                    stepReference.child("steps").child(Long.toString(currentTime)).setValue(newRecord);

                    // Set the current step record value too
                    firebaseDatabase.getReference("currentStep").setValue(steps);

                    //broadcast intent of new update
                    //received by the StepsActivity Class if running to dynamically update the table
                    Intent stepsUpdate = new Intent("stepsUpdate");
                    stepsUpdate.putExtra("Steps", steps);
                    stepsUpdate.putExtra("Time", currentTime);
                    sendBroadcast(stepsUpdate);

                }

                // Stop this service thread once a value has been updated
                jobFinished(jobParameters, false);

            }
        }, 1000 * 60 * 2 ); // 2 minutes

        // Let task finish completely
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        Log.d("DEBUG","Steps Job Stopped") ;
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

        stepsChanged = false;

        // Create Sensor Manager to monitor the step sensor
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        // Use Android Step Counter, which monitors the step count
        // Counts the number of steps since the last system reboot
        // Use normal delay as speed is not needed

        if (sensorManager != null) {
            sensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
        }

        Log.d("DEBUG","Steps Update Service Created ") ;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        Log.d("DEBUG","Steps Update Service Stopped") ;

        // Do not unregister - causes steps to stop counting
        // sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {

        sensor = sensorEvent.sensor;

        if (sensor.getType() == Sensor.TYPE_STEP_COUNTER) {

            // Get the step count and current system time
            steps = (int) sensorEvent.values[0];
            currentTime = System.currentTimeMillis();

            stepsChanged = true;
            Log.d("DEBUG","Steps Value Update: " + steps) ;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
}
