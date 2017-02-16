package ubicom.mads.apstrengthmeasurer;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.LocationManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private volatile boolean running;


    private static final long WIFI_SCAN_DELAY_MILLIS = TimeUnit.SECONDS.toMillis(1);
    ArrayList<ScanResultWrapper> scanResults;
    private WifiManager wifiManager;
    private WifiScanBroadcastReceiver wifiScanBroadcastReceiver = new WifiScanBroadcastReceiver();
    private WifiManager.WifiLock wifiLock;
    public String room;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        scanResults = new ArrayList<>();
        wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        wifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_SCAN_ONLY, MainActivity.class.getName());
        final CountDownTimerTest timer = new CountDownTimerTest(20000, 5000);
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        final TextView statusTextView = (TextView) findViewById(R.id.statusTextView);
        final EditText roomEditText = (EditText) findViewById(R.id.roomNumberTextField);
        //statusCheck();
        statusTextView.setText("Doing nothing");
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                timer.start();
                statusTextView.setText("Scanner...");
                room = roomEditText.getText().toString();
            }
        });
        Button saveButton = (Button) findViewById(R.id.savebutton);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    writeToCSV();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void saveToCSV() {

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private final class WifiScanBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (!running || !WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equals(intent.getAction())) {
                return;
            }

            List<ScanResult> results = wifiManager.getScanResults();
            Log.d("WIFI BROADCAST RECEIVER", "Scan results length: " + scanResults.size());
            for (ScanResult s : results) {
                scanResults.add(new ScanResultWrapper(s.BSSID, s.level, room, wifiManager.getConnectionInfo().getMacAddress()));
            }


        }

    }

    public class CountDownTimerTest extends CountDownTimer {
        public CountDownTimerTest(long startTime, long interval) {
            super(startTime, interval);
        }

        @Override
        public void onFinish() {
            running = false;

            //unregisterReceiver(wifiScanBroadcastReceiver);

            wifiLock.release();

            TextView statusTextView = (TextView) findViewById(R.id.statusTextView);
            statusTextView.setText("Doing nothing");
        }

        @Override
        public void onTick(long millisUntilFinished) {
            running = true;

            wifiLock.acquire();

            registerReceiver(wifiScanBroadcastReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

            final Handler wifiScanHandler = new Handler();
            Runnable wifiScanRunnable = new Runnable() {

                @Override
                public void run() {
                    if (!running) {
                        return;
                    }

                    if (!wifiManager.startScan()) {
                        Log.w("WIFIMANAGER", "Couldn't start Wi-fi scan!");
                    }
                    wifiScanHandler.postDelayed(this, WIFI_SCAN_DELAY_MILLIS);
                }

            };
            wifiScanHandler.post(wifiScanRunnable);
        }
    }

    //Write to csv
    public void writeToCSV() throws IOException {
        {

            File folder = new File(Environment.getExternalStorageDirectory()
                    + "/WIFICSV");

            boolean var = false;
            if (!folder.exists())
                var = folder.mkdir();

            Log.d(this.toString(), "" + var);
            // System.out.println("" + var);


            final String filename = folder.toString() + "/" + "ApStrengths" +room+ ".csv";

            // show waiting screen
            CharSequence contentTitle = getString(R.string.app_name);
            final ProgressDialog progDailog = ProgressDialog.show(MainActivity.this
                    , contentTitle, "Writing to csv - please wait",
                    true);//please wait
            final Handler handler = new Handler() {
                @Override
                public void handleMessage(Message msg) {

                }
            };

            new Thread() {
                public void run() {
                    try {

                        FileWriter fw = new FileWriter(filename);
                        for (ScanResultWrapper wrapper : scanResults) {
                            fw.append(wrapper.timestampMilis.toString()+", "
                                    +wrapper.APaddress + ", "
                                    +wrapper.deviceAddress + ", "
                                    +wrapper.level + "\n"
                            );
                        }

                        // fw.flush();
                        fw.close();

                    } catch (Exception e) {
                    }
                    handler.sendEmptyMessage(0);
                    progDailog.dismiss();
                }
            }.start();

        }

    }

}
