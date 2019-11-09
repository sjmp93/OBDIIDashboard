package com.example.sergiojosemp.canbt.activity;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.FloatingActionButton;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.example.sergiojosemp.canbt.R;
import com.example.sergiojosemp.canbt.service.ObdService;
import com.github.pires.obd.commands.control.TroubleCodesCommand;
import com.github.pires.obd.commands.protocol.ResetTroubleCodesCommand;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

//Based on github.pires obd-reader https://github.com/pires/android-obd-reader/
public class DiagnosticTroubleCodeActivity extends Activity {


    private static final String TAG = DiagnosticTroubleCodeActivity.class.getName();
    private final static String PREFERENCES = "preferences";

    private String troubleCodes = "";
    private ObdService obdService;
    @Inject
    SharedPreferences preferences;


    Map<String, String> getDict(int keyId, int valId) {
        String[] keys = getResources().getStringArray(keyId);
        String[] vals = getResources().getStringArray(valId);

        Map<String, String> dict = new HashMap<>();
        for (int i = 0, l = keys.length; i < l; i++) {
            dict.put(keys[i], vals[i]);
        }

        return dict;
    }

    private void fillView(String res) {
        ListView lv =  findViewById(R.id.listView);
        Map<String, String> dtcVals = getDict(R.array.dtc_keys, R.array.dtc_values);

        ArrayList<String> dtcCodes = new ArrayList<>();
        if (res != null) {
            for (String dtcCode : res.split("\n")) {
                dtcCodes.add(dtcCode + " : " + dtcVals.get(dtcCode));
                Log.d(TAG, dtcCode + " : " + dtcVals.get(dtcCode));
            }
        } else {
            dtcCodes.add(getText(R.string.text_noerrors).toString());
        }
        ArrayAdapter<String> myarrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, dtcCodes);
        lv.setAdapter(myarrayAdapter);
        lv.setTextFilterEnabled(true);
    }


    public static class ModifiedTroubleCodesObdCommand extends TroubleCodesCommand {
        @Override
        public String getResult() {
            // remove unwanted response from output since this results in erroneous error codes
            return rawData.replace("SEARCHING...", "").replace("NODATA", "");
        }
    }

    public class ClearDTC extends ResetTroubleCodesCommand {
        @Override
        public String getResult() {
            return rawData;
        }
    }


    private void getDtc (){
        if (obdService != null && obdService.getbluetoothSocket() != null && obdService.getbluetoothSocket().isConnected() && obdService.queueEmpty()) {
            troubleCodes = obdService.getTroubleCodes();
            fillView(troubleCodes);
        }
    }

    private ServiceConnection serviceConn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder binder) {
            obdService = ((ObdService.ObdServiceBinder) binder).getService();
            obdService.setContext(DiagnosticTroubleCodeActivity.this);
            obdService.setDtc(true);
            Log.d(TAG, getText(R.string.dashboard_linking_log_text).toString());
            obdService.startService();
        }

        @Override
        protected Object clone() throws CloneNotSupportedException {
            return super.clone();
        }

        // This method is *only* called when the connection to the obdService is lost unexpectedly
        // and *not* when the client unbinds (http://developer.android.com/guide/components/bound-services.html)
        // So the isServiceBound attribute should also be set to false when we unbind from the obdService.
        @Override
        public void onServiceDisconnected(ComponentName className) {
        }
    };

    public void startFullScreen() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE
                        // Set the content to appear under the system bars so that the
                        // content doesn't resize when the system bars hide and show.
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        // Hide the nav bar and status bar
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
    }

    public void stopFullScreen() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE
        );
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            startFullScreen();
        }
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferences = getSharedPreferences(PREFERENCES,
                Context.MODE_MULTI_PROCESS);
        setContentView(R.layout.dtc_options_menu);
        FloatingActionButton getDtcButton = findViewById(R.id.getDtcButton);
        FloatingActionButton clearDtcButton = findViewById(R.id.clearDtcButton);
        final TextView outputText = findViewById(R.id.outputText);

        clearDtcButton.setOnClickListener(new View.OnClickListener() {
        @Override
            public void onClick(View v) {

                if (obdService != null && obdService.getbluetoothSocket() != null && obdService.getbluetoothSocket().isConnected() && obdService.queueEmpty()) {
                    try {
                        Log.d(TAG, getText(R.string.trying_reset).toString());
                        outputText.setText(getText(R.string.trying_reset).toString());
                        ResetTroubleCodesCommand clear = new ResetTroubleCodesCommand();
                        clear.run(obdService.getbluetoothSocket().getInputStream(), obdService.getbluetoothSocket().getOutputStream());
                        String result = clear.getFormattedResult();
                        Log.d(TAG, getText(R.string.reset_result).toString() + result);
                        outputText.setText(getText(R.string.reset_result).toString() + result);
                    } catch (IOException e) {
                        outputText.setText(getText(R.string.error_establishing_connection).toString());
                        Log.e(
                                TAG,
                                getText(R.string.error_establishing_connection).toString() + " -> "
                                        + e.getMessage()
                        );
                    } catch (InterruptedException ie) {
                        outputText.setText(getText(R.string.error_cleaning_dtc).toString());
                        Log.e(
                                TAG,
                                getText(R.string.error_cleaning_dtc).toString() +" -> "
                                        + ie.getMessage()
                        );
                    }

                    Intent refresh = new Intent(getApplicationContext(), DiagnosticTroubleCodeActivity.class);
                    startActivity(refresh);
                }
            }
        });

        getDtcButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setContentView(R.layout.diagnostic_trouble_code_activity);
                getDtc();
            }
        });

        startFullScreen();

        Intent serviceIntent = new Intent(DiagnosticTroubleCodeActivity.this, ObdService.class);
        bindService(serviceIntent, serviceConn, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    protected void onResume() {
        super.onResume();
        Intent serviceIntent = new Intent(DiagnosticTroubleCodeActivity.this, ObdService.class);
        bindService(serviceIntent, serviceConn, Context.BIND_AUTO_CREATE);
    }

    protected void onPause() {
        super.onPause();
        unbindService(serviceConn);
    }
}
