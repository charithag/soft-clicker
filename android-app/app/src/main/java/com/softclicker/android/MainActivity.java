package com.softclicker.android;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;

import com.softclicker.android.apn.ApnAdapter;
import com.softclicker.android.apn.ApnData;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    WifiManager mainWifiObj;
    private ApnAdapter apnAdapter;
    private ListView listView;
    private ProgressBar loader;
    private List<ScanResult> wifiScanList;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // initialize progressBar
        loader = (ProgressBar) findViewById(R.id.progressBar);
        loader.setVisibility(View.VISIBLE);

        listView = (ListView) findViewById(R.id.apnListView);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final ScanResult selectedSSID = wifiScanList.get(position);
                boolean isSec = false;

                if (selectedSSID.capabilities.contains("WPA2")) {
                    //do something
                    isSec = true;
                } else if (selectedSSID.capabilities.contains("WPA")) {
                    //do something
                    isSec = true;
                } else if (selectedSSID.capabilities.contains("WEP")) {
                    //do something
                    isSec = true;
                }

                LayoutInflater li = LayoutInflater.from(MainActivity.this);
                View promptsView = li.inflate(R.layout.prompt_apn_pw, null);

                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                        MainActivity.this);
                alertDialogBuilder.setView(promptsView);

                final EditText userInput = (EditText) promptsView.findViewById(R.id.apnPwPromptText);

                alertDialogBuilder
                        .setCancelable(false)
                        .setPositiveButton("OK",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        // TODO connect with APN
                                        ConnectToApn(selectedSSID, userInput.getText().toString());
                                    }
                                })
                        .setNegativeButton("Cancel",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        dialog.cancel();
                                    }
                                });

                AlertDialog alertDialog = alertDialogBuilder.create();
                if (isSec) {
                    alertDialog.show();
                } else {
                    ConnectToApn(selectedSSID, null);
                }
            }
        });

        /** TODO : Load student information from the localstorage */
        // Temporarily bind dummy data
        Spinner studentSelector = (Spinner) findViewById(R.id.spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.student_list, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        studentSelector.setAdapter(adapter);

        /** Load APNs */
        mainWifiObj = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        // Switch on WiFi
        if (!mainWifiObj.isWifiEnabled()) {
            mainWifiObj.setWifiEnabled(true);
        }
        MyWifiReceiver broadcastReceiver = new MyWifiReceiver(this);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        registerReceiver(broadcastReceiver, intentFilter);
        mainWifiObj.startScan();
    }

    private void ConnectToApn(ScanResult apn, String p) {
        // after connection is successful

        WifiConfiguration wifiConfig = new WifiConfiguration();
        wifiConfig.SSID = String.format("\"%s\"", apn.SSID);

        if (p != null) {
            wifiConfig.preSharedKey = String.format("\"%s\"", p);
        }

        //remember id
        int netId = mainWifiObj.addNetwork(wifiConfig);
        mainWifiObj.disconnect();
        mainWifiObj.enableNetwork(netId, true);
        mainWifiObj.reconnect();

        // WifiInfo connectionInfo = mainWifiObj.getConnectionInfo();
        Intent i = new Intent(MainActivity.this, ListenerActivity.class);
        i.putExtra(ListenerActivity.SSID, apn.SSID);

        startActivity(i);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mainWifiObj.setWifiEnabled(false);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.refresh_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.refresh_menu:
                mainWifiObj.startScan();
                return true;
            default:
                return false;
        }
    }

    // This methods binds WiFi APN data to the ListView
    public void setData() {
        loader.setVisibility(View.VISIBLE);

        WifiScanReceiver wifiReceiver = new WifiScanReceiver();
        registerReceiver(wifiReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        wifiScanList = mainWifiObj.getScanResults();
        ArrayList<ApnData> apnArray = new ArrayList<>();
        for (int i = 0; i < wifiScanList.size(); i++) {
            apnArray.add(new ApnData(wifiScanList.get(i).SSID, wifiScanList.get(i).BSSID));
        }
        apnAdapter = new ApnAdapter(this, apnArray);
        listView.setAdapter(apnAdapter);

        loader.setVisibility(View.GONE);
    }
}

class WifiScanReceiver extends BroadcastReceiver {
    public void onReceive(Context c, Intent intent) {
    }
}