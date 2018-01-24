package cathalbreathnach.activitymonitor;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

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

// Class to detect and record changes in the step count
// Update to the database every hour
// Broadcasts the current step count to display in the MainActivity
// Also allows the user to reset their step count
public class StepsActivity extends AppCompatActivity {

    private ArrayList<StepsRecord> downloadedStepValues = new ArrayList<>();
    private TableLayout table;
    private ScrollView scroll;
    private BroadcastReceiver receiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_steps);

        // Get the TableLayout and ScrollLayout References
        table = findViewById(R.id.table);
        scroll = findViewById(R.id.scroll);

        initialiseTable();
    }

    // BroadCast Receiver Methods
    // Alert of a new update to the uploaded step values
    @Override
    protected void onResume() {
        super.onResume();
        if(receiver == null){
            receiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {

                    long time = (long)intent.getExtras().get("Time");
                    int steps = (int)intent.getExtras().get("Steps");
                    Date date = new Date(time);
                    String text = DateFormat.getDateTimeInstance().format(date);

                    addNewLine(text, steps);
                }
            };
        }
        registerReceiver(receiver,new IntentFilter("stepsUpdate"));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(receiver != null){
            unregisterReceiver(receiver);
        }
    }

    // Adds new line to the TableLayout
    private void addNewLine(String timeAndDate, int steps) {

        String stepsString = Integer.toString(steps);

        TableRow newRow = new TableRow(this);

        // Create the Left hand side
        TextView left = new TextView(this);
        left.setText(timeAndDate);
        left.setGravity(Gravity.START);
        left.setPadding(10,5, 10,5);
        left.setTextSize(15);

        // Create the right hand side
        TextView right = new TextView(this);
        right.setText(stepsString);
        right.setGravity(Gravity.END);
        right.setPadding(10,5,10,5);
        right.setTextSize(15);

        // Add the TextView Components to the row
        newRow.addView(left);
        newRow.addView(right);

        // Add row to the TableLayout
        table.addView(newRow,new TableLayout.LayoutParams(TableLayout.LayoutParams.WRAP_CONTENT,
                TableLayout.LayoutParams.WRAP_CONTENT));

        // Create Line to separate rows and add to end of the new row
        View line = new View(this);
        line.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT,1));
        line.setBackgroundColor(Color.BLACK);
        table.addView(line);

        // Scroll to the newly added row
        scrollTo(newRow);
    }


    // Scrolls the Table to a given view
    private void scrollTo(final View view) {

        new Handler().post(new Runnable() {
            @Override
            public void run() {
                scroll.smoothScrollTo(0, view.getBottom());
            }
        });
    }

    // Method to Initialise the Table
    // Gets a snapshot of the database and downloads values
    private void initialiseTable() {

        // Get database reference and listen for single event to use for download
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference().child("steps");
        databaseReference.addListenerForSingleValueEvent(

                new ValueEventListener() {

                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {

                        Map<String,Object> currentSteps =  (Map<String,Object>)dataSnapshot.getValue();

                        // Add each downloaded value to the Arraylist
                        for (Map.Entry<String,Object> entry : currentSteps.entrySet()){
                            Map stepEntry = (Map) entry.getValue();
                            int steps = ((Long)stepEntry.get("stepCount")).intValue();
                            Long timeMillis = Long.valueOf(entry.getKey());
                            downloadedStepValues.add(new StepsRecord(steps,timeMillis));
                        }

                        // Sort to ensure the correct order in time
                        Collections.sort(downloadedStepValues);

                        // Add each entry to the table
                        for(int i = 0; i < downloadedStepValues.size(); i++){
                            Date date = new Date(downloadedStepValues.get(i).getTime());
                            String markerText = DateFormat.getDateTimeInstance().format(date);
                            addNewLine(markerText, downloadedStepValues.get(i).getStepCount());
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                }
        );
    }

}
