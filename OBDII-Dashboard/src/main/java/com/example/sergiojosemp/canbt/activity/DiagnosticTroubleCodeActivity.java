package com.example.sergiojosemp.canbt.activity;

import android.app.Activity;
import android.bluetooth.BluetoothSocket;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.example.sergiojosemp.canbt.R;
import com.example.sergiojosemp.canbt.service.ObdService;
import com.github.pires.obd.commands.control.TroubleCodesCommand;
import com.github.pires.obd.commands.protocol.ResetTroubleCodesCommand;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class DiagnosticTroubleCodeActivity extends Activity {


    private static final String TAG = DiagnosticTroubleCodeActivity.class.getName();
    private static final int NO_BLUETOOTH_DEVICE_SELECTED = 0;
    private static final int CANNOT_CONNECT_TO_DEVICE = 1;
    private static final int NO_DATA = 3;
    private static final int DATA_OK = 4;
    private static final int CLEAR_DTC = 5;
    private static final int OBD_COMMAND_FAILURE = 10;
    private static final int OBD_COMMAND_FAILURE_IO = 11;
    private static final int OBD_COMMAND_FAILURE_UTC = 12;
    private static final int OBD_COMMAND_FAILURE_IE = 13;
    private static final int OBD_COMMAND_FAILURE_MIS = 14;
    private static final int OBD_COMMAND_FAILURE_NODATA = 15;
    @Inject
    SharedPreferences prefs;
    private BluetoothSocket sock = null;
    private Handler mHandler = new Handler(new Handler.Callback() {


        public boolean handleMessage(Message msg) {
            Log.d(TAG, "Message received on handler");
            switch (msg.what) {
                case NO_BLUETOOTH_DEVICE_SELECTED:
                    makeToast(getString(R.string.text_bluetooth_nodevice));
                    finish();
                    break;
                case CANNOT_CONNECT_TO_DEVICE:
                    makeToast(getString(R.string.text_bluetooth_error_connecting));
                    finish();
                    break;

                case OBD_COMMAND_FAILURE:
                    makeToast(getString(R.string.text_obd_command_failure));
                    finish();
                    break;
                case OBD_COMMAND_FAILURE_IO:
                    makeToast(getString(R.string.text_obd_command_failure) + " IO");
                    finish();
                    break;
                case OBD_COMMAND_FAILURE_IE:
                    makeToast(getString(R.string.text_obd_command_failure) + " IE");
                    finish();
                    break;
                case OBD_COMMAND_FAILURE_MIS:
                    makeToast(getString(R.string.text_obd_command_failure) + " MIS");
                    finish();
                    break;
                case OBD_COMMAND_FAILURE_UTC:
                    makeToast(getString(R.string.text_obd_command_failure) + " UTC");
                    finish();
                    break;
                case OBD_COMMAND_FAILURE_NODATA:
                    makeToastLong(getString(R.string.text_noerrors));
                    //finish();
                    break;

                case NO_DATA:
                    makeToast(getString(R.string.text_dtc_no_data));
                    ///finish();
                    break;
                case DATA_OK:
                    dataOk((String) msg.obj);
                    break;
            }
            return false;
        }
    });

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case 1://R.id.action_clear_codes:
                try {
                    //          sock = BluetoothManager.connect(dev);
                } catch (Exception e) {
                    Log.e(
                            TAG,
                            "There was an error while establishing connection. -> "
                                    + e.getMessage()
                    );
                    Log.d(TAG, "Message received on handler here");
                    mHandler.obtainMessage(CANNOT_CONNECT_TO_DEVICE).sendToTarget();
                    return true;
                }
                try {

                    Log.d("TESTRESET", "Trying reset");
                    //new ObdResetCommand().run(sock.getInputStream(), sock.getOutputStream());
                    ResetTroubleCodesCommand clear = new ResetTroubleCodesCommand();
                    clear.run(sock.getInputStream(), sock.getOutputStream());
                    String result = clear.getFormattedResult();
                    Log.d("TESTRESET", "Trying reset result: " + result);
                } catch (Exception e) {
                    Log.e(
                            TAG,
                            "There was an error while establishing connection. -> "
                                    + e.getMessage()
                    );
                }
                // Refresh main activity upon close of dialog box
                Intent refresh = new Intent(this, DiagnosticTroubleCodeActivity.class);
                startActivity(refresh);
                this.finish(); //
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    Map<String, String> getDict(int keyId, int valId) {
        String[] keys = getResources().getStringArray(keyId);
        String[] vals = getResources().getStringArray(valId);

        Map<String, String> dict = new HashMap<String, String>();
        for (int i = 0, l = keys.length; i < l; i++) {
            dict.put(keys[i], vals[i]);
        }

        return dict;
    }

    public void makeToast(String text) {
        Toast toast = Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT);
        toast.show();
    }

    public void makeToastLong(String text) {
        Toast toast = Toast.makeText(getApplicationContext(), text, Toast.LENGTH_LONG);
        toast.show();
    }

    private void dataOk(String res) {
        ListView lv = (ListView) findViewById(R.id.listView);

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



























    private final static String PREFERENCES = "preferences";

    private String troubleCodes = "";
    private ObdService obdService;
    @Inject
    private SharedPreferences preferences; //Toda la configuración se almacena en este objeto

    private void getDtc (){
            if (obdService != null && obdService.getbluetoothSocket() != null && obdService.getbluetoothSocket().isConnected() && obdService.queueEmpty()) {
                troubleCodes = obdService.getTroubleCodes();
                if(!troubleCodes.equals("")){
                    Toast.makeText(getApplicationContext(),"There are DTC" + troubleCodes,Toast.LENGTH_LONG).show();
                }else{
                    Toast.makeText(getApplicationContext(),"There are not DTC",Toast.LENGTH_LONG).show();
                }
            }
        }


    private ServiceConnection serviceConn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder binder) {
            obdService = ((ObdService.ObdServiceBinder) binder).getService();
            obdService.setContext(DiagnosticTroubleCodeActivity.this);
            obdService.setDtc(true);
            try {
                Log.d(TAG, getText(R.string.dashboard_linking_log_text).toString());
                obdService.startService();
            } catch (IOException ioe) {
            }
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


    // Inicio: Entra en el modo inmersivo
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
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT); // Make to run your application only in LANDSCAPE mode
    }

    // Inicio: Sale del modo inmersivo
    public void stopFullScreen() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE
        );
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT); // Make to run your application only in LANDSCAPE mode
    }

    // Inicio: cada vez que se vuelve a la pantalla principal (despues de elegir obciones de barras de progreso, o al iniciar, por ejemplo) se establece la pantalla completa
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            startFullScreen();
        }
    }
    // Fin: cada vez que se vuelve a la pantalla principal (despues de elegir opciones de barras de progreso, o al iniciar, por ejemplo) se establece la pantalla completa

    protected void onCreate(Bundle savedInstanceState) {
        //Se carga y configura el nuevo layout
        super.onCreate(savedInstanceState);
        //Preferences
        preferences = getSharedPreferences(PREFERENCES,
                Context.MODE_MULTI_PROCESS);
        setContentView(R.layout.diagnostic_trouble_code_activity);
        Button jb = findViewById(R.id.button);
        jb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getDtc();
            }
        });
        //Inicialización de componentes de la vista

        startFullScreen();
        //FrontEnd
        findViewById(R.id.listView).setBackgroundColor(Color.BLACK);
        Intent serviceIntent = new Intent(DiagnosticTroubleCodeActivity.this, ObdService.class);
        bindService(serviceIntent, serviceConn, Context.BIND_AUTO_CREATE);

        //new Handler().post(queueCommandsThread);
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
