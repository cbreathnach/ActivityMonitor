package cathalbreathnach.activitymonitor;

import android.support.annotation.NonNull;

// Class to hold location record object
// Implements comparable class to allow sorting in order of age
public class LocationRecord implements Comparable<LocationRecord>{

    private double latitude;
    private double longitude;
    private long time;

    LocationRecord(double latitude, double longitude, long time){
        this.latitude = latitude;
        this.longitude = longitude;
        this.time = time;
    }

    public double getLatitude(){
        return this.latitude;
    }

    public double getLongitude(){
        return this.longitude;
    }

    public long getTime(){
        return this.time;
    }

    @Override
    public int compareTo(@NonNull LocationRecord locationRecord) {
        if (this.time > locationRecord.time){
            return 1;
        }
        else if (this.time < locationRecord.time){
            return -1;
        }
        else{
            return 0;
        }
    }
}
