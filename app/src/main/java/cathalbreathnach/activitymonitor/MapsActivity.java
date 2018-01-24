package cathalbreathnach.activitymonitor;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Map;

// Displays Map for user with the recorded location markers
// Receives Broadcast from the LocationService to update the markers if open during an update
public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private ArrayList<LocationRecord> downloadedMarkers = new ArrayList<>();
    private BroadcastReceiver receiver;
    Marker currentMarker;
    Marker prevMarker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    // Initialise the map with the markers when map is ready
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        initialiseMarkers();
    }


    // Download Location and Add to Map
    private void initialiseMarkers() {

        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference().child("locations");
        databaseReference.addListenerForSingleValueEvent(

                new ValueEventListener() {

                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {

                        Map<String,Object> currentLocations =  (Map<String,Object>) dataSnapshot.getValue();

                        // Add each downloaded as new object
                        assert currentLocations != null;
                        for (Map.Entry<String,Object> entry : currentLocations.entrySet()){
                            Map locationEntry = (Map) entry.getValue();
                            Double markerLat = (Double)locationEntry.get("latitude");
                            Double markerLong = (Double)locationEntry.get("longitude");
                            Long timeMillis = Long.valueOf(entry.getKey());
                            downloadedMarkers.add(new LocationRecord(markerLat,markerLong,timeMillis));
                        }

                        //Sort Markers in order of time arrived
                        Collections.sort(downloadedMarkers);

                        //Add Markers and draw lines
                        for (int i = 0; i < downloadedMarkers.size(); i++){

                            Date date = new Date(downloadedMarkers.get(i).getTime());
                            String markerText = DateFormat.getDateTimeInstance().format(date);
                            LatLng markerLocation = new LatLng(downloadedMarkers.get(i).getLatitude(),
                                    downloadedMarkers.get(i).getLongitude());

                            Marker marker = mMap.addMarker(new MarkerOptions().position(markerLocation)
                                    .title(markerText).icon(BitmapDescriptorFactory
                                            .defaultMarker(BitmapDescriptorFactory.HUE_GREEN)).zIndex(1.0f));

                            // Draw lines after more than one marker is plotted
                            if(prevMarker != null){
                                mMap.addPolyline((new PolylineOptions())
                                        .add(markerLocation, prevMarker.getPosition())
                                        .width(5)
                                        .color(Color.BLUE).geodesic(true));
                            }
                            prevMarker = marker;
                        }

                        //get last location, store and mark as current
                        currentMarker = prevMarker;

                        currentMarker.setIcon(BitmapDescriptorFactory
                                .defaultMarker(BitmapDescriptorFactory.HUE_RED));

                        currentMarker.setZIndex(prevMarker.getZIndex()+1);

                        CameraPosition cameraPosition = new CameraPosition.Builder()
                                .target(currentMarker.getPosition()).zoom(16).build();

                        mMap.animateCamera(CameraUpdateFactory
                                .newCameraPosition(cameraPosition));

                    }
                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                    }
                }
        );
    }

    // Add a new marker as the current location
    private void addMarker(double latitude, double longitude, long time){

        prevMarker = currentMarker;
        Date date = new Date(time);
        String markerText = DateFormat.getDateTimeInstance().format(date);

        currentMarker = mMap.addMarker(new MarkerOptions().position(new LatLng(latitude, longitude))
                .title(markerText).icon(BitmapDescriptorFactory
                        .defaultMarker(BitmapDescriptorFactory.HUE_RED)).zIndex(1.0f));

        prevMarker.setIcon(BitmapDescriptorFactory
                .defaultMarker(BitmapDescriptorFactory.HUE_GREEN));

        // Draw line from previous marker
        mMap.addPolyline((new PolylineOptions())
                .add(currentMarker.getPosition(), prevMarker.getPosition())
                .width(5)
                .color(Color.BLUE).geodesic(true));

        //Ensure Current Marker is at the front
        currentMarker.setZIndex(prevMarker.getZIndex()+1);

        // Move Camera to new current locations
        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(currentMarker.getPosition()).zoom(16).build();

        mMap.animateCamera(CameraUpdateFactory
                .newCameraPosition(cameraPosition));
    }

    // Broadcast Receiver Methods
    // Received when a new location is detected in the app
    // Only will be called if a new location is detected while the MapsActivity is open
    // Avoids having to wait for the database to update
    @Override
    protected void onResume() {
        super.onResume();
        if(receiver == null){
            receiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    double Lat = (double)intent.getExtras().get("Lat");
                    double Long = (double)intent.getExtras().get("Long");
                    long time = (long)intent.getExtras().get("Time");
                    addMarker(Lat, Long,time);
                }
            };
        }
        registerReceiver(receiver,new IntentFilter("newLocation"));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(receiver != null){
            unregisterReceiver(receiver);
        }
    }
}
