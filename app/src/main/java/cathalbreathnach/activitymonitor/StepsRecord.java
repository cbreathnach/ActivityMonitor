package cathalbreathnach.activitymonitor;

import android.support.annotation.NonNull;

// Records the step data as an object that can be uploaded to the database
// Implements Comparable to allow sorting in correct order
public class StepsRecord implements Comparable<StepsRecord>{

    private int stepCount;
    private long time;

    StepsRecord(int stepCount, long time){
        this.stepCount = stepCount;
        this.time = time;
    }

    public int getStepCount(){
        return  this.stepCount;
    }

    public long getTime(){
        return this.time;
    }

    @Override
    public int compareTo(@NonNull StepsRecord stepsRecord) {
        if (this.time > stepsRecord.time){
            return 1;
        }
        else if (this.time < stepsRecord.time){
            return -1;
        }
        else{
            return 0;
        }
    }
}
