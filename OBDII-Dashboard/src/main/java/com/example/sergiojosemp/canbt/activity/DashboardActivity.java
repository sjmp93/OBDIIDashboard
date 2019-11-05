package com.example.sergiojosemp.canbt.activity;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.constraint.ConstraintLayout;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.akexorcist.roundcornerprogressbar.IconRoundCornerProgressBar;
import com.example.sergiojosemp.canbt.R;
import com.example.sergiojosemp.canbt.service.ObdService;
import com.github.pires.obd.commands.ObdCommand;
import com.github.pires.obd.enums.AvailableCommandNames;
import com.github.pires.obd.reader.LogCSVWriter;
import com.github.pires.obd.reader.ObdCommandJob;
import com.github.pires.obd.reader.ObdConfig;
import com.github.pires.obd.reader.ObdReading;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import az.plainpie.PieView;

import static java.lang.Math.sqrt;

public class DashboardActivity extends AppCompatActivity /*implements GestureDetector.OnGestureListener*/ {


    private static final String TAG = MainActivity.class.getName();
    private static final int NO_BLUETOOTH_ID = 0;
    private static final int BLUETOOTH_DISABLED = 1;
    private static final int START_LIVE_DATA = 2;
    private static final int STOP_LIVE_DATA = 3;
    private static final int SETTINGS = 4;
    private static final int GET_DTC = 5;
    private static final int TABLE_ROW_MARGIN = 7;
    private static final int NO_ORIENTATION_SENSOR = 8;
    private static final int NO_GPS_SUPPORT = 9;
    private static final int TRIPS_LIST = 10;
    private static final int SAVE_TRIP_NOT_AVAILABLE = 11;
    private static final int REQUEST_ENABLE_BT = 1234;
    private static boolean bluetoothDefaultIsEnable = false;


    public Map<String, String> commandResult = new HashMap<String, String>(); //Resultados de cada consulta al OBD
    boolean mGpsIsStarted = false;
    PieView pie_speed;
    PieView pie_rpm;
    IconRoundCornerProgressBar progress_1;
    IconRoundCornerProgressBar progress_2;
    IconRoundCornerProgressBar progress_3;
    IconRoundCornerProgressBar progress_4;
    IconRoundCornerProgressBar progress_5;
    TextView progress_text_1;
    TextView progress_text_2;
    TextView progress_text_3;
    TextView progress_text_4;
    TextView progress_text_5;
    TextView voltage;
    ImageView dtc_button;
    HashMap<Integer, IconRoundCornerProgressBar> progress_map;
    HashMap<IconRoundCornerProgressBar, Integer> inverse_progress_map;
    HashMap<Integer, TextView> text_map;
    HashMap<TextView, Integer> inverse_text_map;
    private LocationManager mLocService;
    private LocationProvider mLocProvider;
    private LogCSVWriter myCSVWriter;  //--------------->Implementar
    private Location mLastLocation;
    private ObdService service;
    /// the trip log
    //private TripLog triplog;   --------------->Implementar
    //private TripRecord currentTrip;   --------------->Implementar
    private GestureDetectorCompat mDetector;
    private TextView compass; //Inicializa un objeto de clase TextView que se enlaza directamente con el texto del Layout main
    private final SensorEventListener orientListener = new SensorEventListener() {

        public void onSensorChanged(SensorEvent event) {
            float x = event.values[0];
            String dir = "";
            if (x >= 337.5 || x < 22.5) {
                dir = "N";
            } else if (x >= 22.5 && x < 67.5) {
                dir = "NE";
            } else if (x >= 67.5 && x < 112.5) {
                dir = "E";
            } else if (x >= 112.5 && x < 157.5) {
                dir = "SE";
            } else if (x >= 157.5 && x < 202.5) {
                dir = "S";
            } else if (x >= 202.5 && x < 247.5) {
                dir = "SW";
            } else if (x >= 247.5 && x < 292.5) {
                dir = "W";
            } else if (x >= 292.5 && x < 337.5) {
                dir = "NW";
            }
            updateTextView(compass, dir);
        }

        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // do nothing
        }
    };
    private TextView g_force;
    private final SensorEventListener accelerometerListener = new SensorEventListener() {

        public void onSensorChanged(SensorEvent event) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];
            double mod = sqrt(x * x + y * y + z * z) / 9.81; //G
            updateTextView(g_force, new String(mod + "").substring(0, 3));
        }

        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // do nothing
        }
    };
    private TextView btStatusTextView;
    private TextView obdStatusTextView;
    private ConstraintLayout vv;
    @Inject
    private SensorManager sensorManager;
    @Inject
    private PowerManager powerManager;
    @Inject
    private SharedPreferences prefs; //Toda la configuración se almacena en este objeto
    private long start = 0L;
    private long end = 0L;
    private float time = 0.0f;

    private final Runnable queueCommandsThread = new Runnable() {
        @Override
        public void run() {
            if (service != null/* && service.isRunning()*/ && service.queueEmpty()) {
                queueCommands();

                double lat = 0;
                double lon = 0;
                double alt = 0;
                final int posLen = 7;
                if (mGpsIsStarted && mLastLocation != null) {
                    lat = mLastLocation.getLatitude();
                    lon = mLastLocation.getLongitude();
                    alt = mLastLocation.getAltitude();

                    StringBuilder sb = new StringBuilder();
                    sb.append("Lat: ");
                    sb.append(String.valueOf(mLastLocation.getLatitude()).substring(0, posLen));
                    sb.append(" Lon: ");
                    sb.append(String.valueOf(mLastLocation.getLongitude()).substring(0, posLen));
                    sb.append(" Alt: ");
                    sb.append(String.valueOf(mLastLocation.getAltitude()));
                }
                if (prefs.getBoolean(SettingsActivity.UPLOAD_DATA_KEY, false)) {
                    // Upload the current reading by http
                    final String vin = prefs.getString(SettingsActivity.VEHICLE_ID_KEY, "UNDEFINED_VIN");
                    Map<String, String> temp = new HashMap<String, String>();
                    temp.putAll(commandResult); //Se almacenan las respuestas del OBD en un objeto que guarda los datos temporales
                    //ObdReading reading = new ObdReading(lat, lon, alt, System.currentTimeMillis(), vin, temp);
                    //new UploadAsyncTask().execute(reading);
                } else if (prefs.getBoolean(SettingsActivity.ENABLE_FULL_LOGGING_KEY, false)) {
                    // Write the current reading to CSV
                    final String vin = prefs.getString(SettingsActivity.VEHICLE_ID_KEY, "UNDEFINED_VIN");
                    Map<String, String> temp = new HashMap<String, String>();
                    temp.putAll(commandResult); //Se almacenan las respuestas del OBD en un objeto que guarda los datos temporales
                    if(commandResult.size()!=0) { //Solo se escribe en el CSV si hay comandos de vuelta
                        if(end == 0L){
                            start = System.currentTimeMillis();
                            end = System.currentTimeMillis();
                        }else {
                            end = System.currentTimeMillis();
                        }
                        time = ((float) (end - start)) / 1000f;
                        ObdReading reading = new ObdReading(lat, lon, alt, time, vin, temp);
                        if (reading != null) myCSVWriter.writeLineCSV(reading);
                    }
                }
                commandResult.clear();
            }
            // run again in period defined in preferences
            new Handler().postDelayed(queueCommandsThread, SettingsActivity.getObdUpdatePeriod(prefs));
        }
    };
    private boolean isServiceBound;
    private Sensor orientSensor = null; // Se usa para recibir la orientación a través del sensor de orientación del dispositivo
    private Sensor accelerometerSensor = null;
    private PowerManager.WakeLock wakeLock = null; // Se usa para evitar el bloqueo automático de pantalla
    private boolean preRequisites = true;
    private ServiceConnection serviceConn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder binder) {
            service = ((ObdService.ObdThingsBinder) binder).getService();
            service.setContext(DashboardActivity.this);
            try {
                System.out.println("Enlazando DashBoard con el servicio");
                service.startService();
            } catch (IOException ioe) {
            }
        }

        @Override
        protected Object clone() throws CloneNotSupportedException {
            return super.clone();
        }

        // This method is *only* called when the connection to the service is lost unexpectedly
        // and *not* when the client unbinds (http://developer.android.com/guide/components/bound-services.html)
        // So the isServiceBound attribute should also be set to false when we unbind from the service.
        @Override
        public void onServiceDisconnected(ComponentName className) {
        }
    };

    public static String LookUpCommand(String txt) {
        for (AvailableCommandNames item : AvailableCommandNames.values()) {
            if (item.getValue().equals(txt)) return item.name();
        }
        return txt;
    }

    //SE70043
    public void updateTextView(final TextView view, final String txt) {
        new Handler().post(new Runnable() {
            public void run() {
                view.setText(txt);
            }
        });
    }

    private void queueCommands() {
        for (ObdCommand Command : ObdConfig.getCommands()) {
            if (prefs.getBoolean(Command.getName(), true))
                service.queueJob(new ObdCommandJob(Command));
        }

    }
    // Fin: Entra en el modo inmersivo

    public void stateUpdate(final ObdCommandJob job) {
        final String cmdName = job.getCommand().getName();
        String cmdResult = "";
        final String cmdID = LookUpCommand(cmdName);

        if (job.getState().equals(ObdCommandJob.ObdCommandJobState.EXECUTION_ERROR)) {
            cmdResult = job.getCommand().getResult();
            if (cmdResult != null && service != null) {
                obdStatusTextView.setText(cmdResult.toLowerCase());
            }
        } else if (job.getState().equals(ObdCommandJob.ObdCommandJobState.BROKEN_PIPE)) {
            if (service != null)
                System.out.println("Revisar Implementar");
            //stopLiveData();
        } else if (job.getState().equals(ObdCommandJob.ObdCommandJobState.NOT_SUPPORTED)) {
            cmdResult = getString(R.string.status_obd_no_support);
        } else {
            cmdResult = job.getCommand().getFormattedResult();
            if (service != null)
                obdStatusTextView.setText(cmdName);
        }

        // Inicio: Aquí se reciben los comandos, se busca el int equivalente en el array obddata y con ese int se determina qué dato va a qué barra haciendo uso de los diccionarios
        setProgressForCommand(job);
        Log.d(TAG,cmdName + ": " + cmdResult); // Para debugging
        if (cmdID.equals(AvailableCommandNames.SPEED.name())) {
            if (!job.getCommand().getCalculatedResult().matches("[a-zA-Z]")) { //Si contiene caracteres no numéricos, entonces no interesa el resultado
                pie_speed.setPieAngle(Integer.parseInt(job.getCommand().getCalculatedResult()));
                pie_speed.setInnerText(job.getCommand().getCalculatedResult() + " KM/H");
            } else {
                pie_speed.setInnerText(job.getCommand().getCalculatedResult()); // Se muestra el valor recogido en caso de no ser numérico
            }
        }
        if (cmdID.equals(AvailableCommandNames.ENGINE_RPM.name())) {
            if (!job.getCommand().getCalculatedResult().matches("[a-zA-Z]")) { // Si contiene caracteres no numéricos, entonces no interesa el resultado
                pie_rpm.setPieAngle((Integer.parseInt(job.getCommand().getCalculatedResult()) * 360) / SettingsActivity.getMaxRPM(prefs)); // Máx RPM 7500
                pie_rpm.setInnerText(job.getCommand().getCalculatedResult() + " RPM");
            } else {
                pie_rpm.setInnerText(job.getCommand().getCalculatedResult()); // Se muestra el valor recogido en caso de no ser numérico
            }
        }

        if (cmdID.equals(AvailableCommandNames.CONTROL_MODULE_VOLTAGE.name())) {
            voltage.setText(job.getCommand().getFormattedResult());
        }
        commandResult.put(cmdID, cmdResult);
        //updateTripStatistic(job, cmdID);
    }

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
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE); // Make to run your application only in LANDSCAPE mode
    }

    public void updateProgress(final IconRoundCornerProgressBar progressBar, final float value) {
        new Handler().post(new Runnable() {
            public void run() {
                progressBar.setProgress(value);
            }
        });
    }
    // Fin: Sale del modo inmersivo

    // Inicio: Sale del modo inmersivo
    public void stopFullScreen() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE
        );
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE); // Make to run your application only in LANDSCAPE mode
    }
    // Fin: cada vez que se vuelve a la pantalla principal (despues de elegir opciones de barras de progreso, o al iniciar, por ejemplo) se establece la pantalla completa

    // Inicio: cada vez que se vuelve a la pantalla principal (despues de elegir obciones de barras de progreso, o al iniciar, por ejemplo) se establece la pantalla completa
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            startFullScreen();
        }
    }
    // Fin: Método que establece el progreso del comando en Job en la barra correspondiente

    // Inicio: Método que establece el progreso del comando en Job en la barra correspondiente
    public void setProgressForCommand(ObdCommandJob job) {
        //Parámetros a mostrar en las progress_bar
        final String[] obdData = this.getResources().getStringArray(R.array.obddata);

        int progress_index = Arrays.asList(obdData).indexOf(job.getCommand().getName());
        IconRoundCornerProgressBar temp_progress = progress_map.get(progress_index);
        TextView temp_text = text_map.get(progress_index);
        if (temp_progress != null) {
            temp_progress.setProgress(Float.parseFloat(job.getCommand().getCalculatedResult()));
            temp_text.setText(job.getCommand().getFormattedResult());
        }
    }
    // Fin: Definición de diccionarios para configuración de las barras de progreso

    // Inicio: Definición de diccionarios para configuración de las barras de progreso
    public void parametersLayoutMapDefinition() {
        progress_map = new HashMap<Integer, IconRoundCornerProgressBar>();
        text_map = new HashMap<Integer, TextView>();
        progress_map.put(0, progress_1);
        progress_map.put(1, progress_2);
        progress_map.put(2, progress_3);
        progress_map.put(3, progress_4);
        progress_map.put(4, progress_5);
        text_map.put(0, progress_text_1);
        text_map.put(1, progress_text_2);
        text_map.put(2, progress_text_3);
        text_map.put(3, progress_text_4);
        text_map.put(4, progress_text_5);
        inverse_progress_map = new HashMap<IconRoundCornerProgressBar, Integer>();
        inverse_text_map = new HashMap<TextView, Integer>();
        inverse_progress_map.put(progress_1, 0);
        inverse_progress_map.put(progress_2, 1);
        inverse_progress_map.put(progress_3, 2);
        inverse_progress_map.put(progress_4, 3);
        inverse_progress_map.put(progress_5, 4);
        inverse_text_map.put(progress_text_1, 0);
        inverse_text_map.put(progress_text_2, 1);
        inverse_text_map.put(progress_text_3, 2);
        inverse_text_map.put(progress_text_4, 3);
        inverse_text_map.put(progress_text_5, 4);
    }

    protected void onCreate(Bundle savedInstanceState) {
        //Se carga y configura el nuevo layout
        super.onCreate(savedInstanceState);
        //Preferences
        prefs = getSharedPreferences("preferences",
                Context.MODE_MULTI_PROCESS);
        //FrontEnd
        setContentView(R.layout.dashboard);

        //Inicialización de componentes de la vista
        compass = (TextView) findViewById(R.id.compass_text);
        g_force = (TextView) findViewById(R.id.g_force);
        btStatusTextView = (TextView) findViewById(R.id.bt_status_text);
        obdStatusTextView = (TextView) findViewById(R.id.obd_status_text);
        vv = (ConstraintLayout) findViewById(R.id.vehicle_view);
        pie_speed = (PieView) findViewById(R.id.pieView);
        pie_rpm = (PieView) findViewById(R.id.rpm);
        progress_1 = (IconRoundCornerProgressBar) findViewById(R.id.progress_1);
        progress_2 = (IconRoundCornerProgressBar) findViewById(R.id.progress_2);
        progress_3 = (IconRoundCornerProgressBar) findViewById(R.id.progress_3);
        progress_4 = (IconRoundCornerProgressBar) findViewById(R.id.progress_4);
        progress_5 = (IconRoundCornerProgressBar) findViewById(R.id.progress_5);
        progress_text_1 = (TextView) findViewById(R.id.progress_text_1);
        progress_text_2 = (TextView) findViewById(R.id.progress_text_2);
        progress_text_3 = (TextView) findViewById(R.id.progress_text_3);
        progress_text_4 = (TextView) findViewById(R.id.progress_text_4);
        progress_text_5 = (TextView) findViewById(R.id.progress_text_5);
        voltage = (TextView) findViewById(R.id.voltage);
        dtc_button = (ImageView) findViewById(R.id.dtc);


        parametersLayoutMapDefinition();
        // Inicio: Configuración de elementos visuales de la interfaz -> Barras de progreso y contador RPM
        progress_1.setMax(120);
        progress_1.setProgressColor(ContextCompat.getColor(DashboardActivity.this, R.color.engine_coolant_progress));
        progress_1.setIconBackgroundColor(ContextCompat.getColor(DashboardActivity.this, R.color.engine_coolant_iconbg));
        progress_1.setIconImageResource(R.drawable.coolant_512);
        progress_text_1.setText("0C");

        progress_2.setMax(130);
        progress_2.setProgressColor(ContextCompat.getColor(DashboardActivity.this, R.color.engine_oil_progress));
        progress_2.setIconBackgroundColor(ContextCompat.getColor(DashboardActivity.this, R.color.engine_oil_iconbg));
        progress_2.setIconImageResource(R.drawable.oil_512);
        progress_text_2.setText("0C");


        progress_3.setMax(100);
        progress_3.setProgressColor(ContextCompat.getColor(DashboardActivity.this, R.color.engine_load_progress));
        progress_3.setIconBackgroundColor(ContextCompat.getColor(DashboardActivity.this, R.color.engine_load_iconbg));
        progress_3.setIconImageResource(R.drawable.engine_load_512);
        progress_text_3.setText("0%");

        progress_4.setMax(100);
        progress_4.setProgressColor(ContextCompat.getColor(DashboardActivity.this, R.color.throttle_position_progress));
        progress_4.setIconBackgroundColor(ContextCompat.getColor(DashboardActivity.this, R.color.throttle_position_iconbg));
        progress_4.setIconImageResource(R.drawable.throttle_512);
        progress_text_4.setText("0%");

        progress_5.setMax(120);
        progress_5.setProgressColor(ContextCompat.getColor(DashboardActivity.this, R.color.ait_intake_progress));
        progress_5.setIconBackgroundColor(ContextCompat.getColor(DashboardActivity.this, R.color.air_intake_iconbg));
        progress_5.setIconImageResource(R.drawable.air_intake_512);
        progress_text_5.setText("0C");

        pie_rpm.setPercentageBackgroundColor(ContextCompat.getColor(DashboardActivity.this, R.color.fuel_consumption_rate_iconbg));

        dtc_button.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                //if (isServiceBound) { // En presión larga, iniciar recogida de DTC.
                //
                // getTroubleCodes();
                Log.d(TAG, "Getting Trouble Codes");
                //}
                return false;
            }
        });

/*
        // Inicio: setup detector de gestos -> swipe desde los bordes hacia el centro para abrir el menú
        mDetector = new GestureDetectorCompat(this,this);
        // Fin: setup detector de gestos -> swipe desde los bordes hacia el centro para abrir el menú
*/

        // Inicio: Configuración de las barras de progreso, se asigna un onLongClick Listener que permite al usuario elegir el parámetro que quiere mostrar en la barra
        View.OnLongClickListener progressListener = new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                final IconRoundCornerProgressBar temp_bar = (IconRoundCornerProgressBar) v;
                final TextView temp_text = text_map.get(inverse_progress_map.get(temp_bar));

                AlertDialog.Builder builder = new AlertDialog.Builder(DashboardActivity.this);

                builder.setTitle(R.string.obd_parameters).setItems(R.array.obddata, new DialogInterface.OnClickListener() {
                    // Método que establece la configuración de la IconRoundCornerProgressBar que se selecciona
                    public void layoutUpdate(int resourceColor, int resourceIcon, int max, int index) {
                        temp_bar.setMax(max);
                        temp_bar.setProgressColor(ContextCompat.getColor(DashboardActivity.this, resourceColor));
                        temp_bar.setIconBackgroundColor(ContextCompat.getColor(DashboardActivity.this, resourceColor));
                        temp_bar.setIconImageResource(resourceIcon);
                        temp_text.setText("");
                        text_map.remove((inverse_text_map.get(temp_text)));
                        text_map.put(index, temp_text);
                        inverse_text_map.remove(temp_text);
                        inverse_text_map.put(temp_text, index);
                        progress_map.remove(inverse_progress_map.get(temp_bar));
                        progress_map.put(index, temp_bar);
                        inverse_progress_map.remove(temp_bar);
                        inverse_progress_map.put(temp_bar, index);
                    }

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //variable que indica el parámetro a mostrar por una barra de progreso = wich
                        switch (which) {
                            // Para todos los casos, configura la barra de progreso con el parámetro correspondiente según los diccionarios progress_map y text_map y,
                            // en caso de repetir parámetros, muestra un mensaje y no cambia la configuración
                            case 0: // Engine Coolant Temperature
                                if (!text_map.containsKey(which)) {
                                    layoutUpdate(R.color.engine_coolant_progress, R.drawable.coolant_512, 120, which);
                                } else {
                                    Toast.makeText(DashboardActivity.this, R.string.already, Toast.LENGTH_LONG).show();
                                }
                                break;
                            case 1: // Engine oil temperature
                                if (!text_map.containsKey(which)) {
                                    layoutUpdate(R.color.engine_oil_progress, R.drawable.oil_512, 130, which);
                                } else {
                                    Toast.makeText(DashboardActivity.this, R.string.already, Toast.LENGTH_LONG).show();
                                }
                                break;
                            case 2: // Engine Load
                                if (!text_map.containsKey(which)) {
                                    layoutUpdate(R.color.engine_load_progress, R.drawable.engine_load_512, 100, which);
                                } else {
                                    Toast.makeText(DashboardActivity.this, R.string.already, Toast.LENGTH_LONG).show();
                                }
                                break;
                            case 3: // Throttle Position
                                if (!text_map.containsKey(which)) {
                                    layoutUpdate(R.color.throttle_position_progress, R.drawable.throttle_512, 100, which);
                                } else {
                                    Toast.makeText(DashboardActivity.this, R.string.already, Toast.LENGTH_LONG).show();
                                }
                                break;
                            case 4: // Air Intake Temperature
                                if (!text_map.containsKey(which)) {
                                    layoutUpdate(R.color.ait_intake_progress, R.drawable.air_intake_512, 100, which);
                                } else {
                                    Toast.makeText(DashboardActivity.this, R.string.already, Toast.LENGTH_LONG).show();
                                }
                                break;
                            case 5: // Ambient Air Temperature
                                if (!text_map.containsKey(which)) {
                                    layoutUpdate(R.color.ambient_air_progress, R.drawable.ambient_air_512, 70, which);
                                } else {
                                    Toast.makeText(DashboardActivity.this, R.string.already, Toast.LENGTH_LONG).show();
                                }
                                break;
                            case 6: // Intake Manifold Pressure (presión colector admisión)
                                if (!text_map.containsKey(which)) {
                                    layoutUpdate(R.color.intake_manifold_presure_progress, R.drawable.intake_manifold_512, 100000, which);
                                } else {
                                    Toast.makeText(DashboardActivity.this, R.string.already, Toast.LENGTH_LONG).show();
                                }
                                break;
                            case 7: // Fuel Rail Pressure
                                if (!text_map.containsKey(which)) {
                                    layoutUpdate(R.color.fuel_rail_pressure_progress, R.drawable.fuel_rail_512, 100000, which);
                                } else {
                                    Toast.makeText(DashboardActivity.this, R.string.already, Toast.LENGTH_LONG).show();
                                }
                                break;
                            case 8: // Fuel Pressure
                                if (!text_map.containsKey(which)) {
                                    layoutUpdate(R.color.fuel_pressure_progress, R.drawable.fuel_pressure_512, 100000, which);
                                } else {
                                    Toast.makeText(DashboardActivity.this, R.string.already, Toast.LENGTH_LONG).show();
                                }
                                break;
                            case 9: // Barometric Pressure
                                if (!text_map.containsKey(which)) {
                                    layoutUpdate(R.color.barometric_pressure_progress, R.drawable.barometric_512, 5000, which);
                                } else {
                                    Toast.makeText(DashboardActivity.this, R.string.already, Toast.LENGTH_LONG).show();
                                }
                                break;
                            case 10: // Fuel Level
                                if (!text_map.containsKey(which)) {
                                    layoutUpdate(R.color.fuel_level_progress, R.drawable.fuel_level_512, 100, which);
                                } else {
                                    Toast.makeText(DashboardActivity.this, R.string.already, Toast.LENGTH_LONG).show();
                                }
                                break;
                            case 11: // Fuel Consumption Rate
                                if (!text_map.containsKey(which)) {
                                    layoutUpdate(R.color.fuel_consumption_rate_progress, R.drawable.fuel_consumption_512, 100, which);
                                } else {
                                    Toast.makeText(DashboardActivity.this, R.string.already, Toast.LENGTH_LONG).show();
                                }
                                break;
                            case 12: // Mass Air Flow
                                if (!text_map.containsKey(which)) {
                                    layoutUpdate(R.color.mass_air_flow_progress, R.drawable.mass_air_flow_512, 150, which);
                                } else {
                                    Toast.makeText(DashboardActivity.this, R.string.already, Toast.LENGTH_LONG).show();
                                }
                                break;
                            default:
                                break;
                        }
                        //se establece el color según el parámetro
                    }
                });
                AlertDialog dialog = builder.create();
                dialog.show();
                return false;
            }
        };
        // Fin: Configuración de las barras de progreso, se asigna un onLongClick Listener que permite al usuario elegir el parámetro que quiere mostrar en la barra


        progress_1.setOnLongClickListener(progressListener);
        progress_2.setOnLongClickListener(progressListener);
        progress_3.setOnLongClickListener(progressListener);
        progress_4.setOnLongClickListener(progressListener);
        progress_5.setOnLongClickListener(progressListener);
        // Fin: Configuración IconRoundProgressBar

        startFullScreen();
        //FrontEnd

        //BackEnd

        //BackEnd
        Intent serviceIntent = new Intent(DashboardActivity.this, ObdService.class);
        if (bindService(serviceIntent, serviceConn, Context.BIND_AUTO_CREATE)) {

        }

        //wakeLock.acquire();


        if (prefs.getBoolean(SettingsActivity.ENABLE_FULL_LOGGING_KEY, false)) {

            // Create the CSV Logger
            long mils = System.currentTimeMillis();
            SimpleDateFormat sdf = new SimpleDateFormat("_dd_MM_yyyy_HH_mm_ss");

            try {
                myCSVWriter = new LogCSVWriter("Log" + sdf.format(new Date(mils)).toString() + ".csv",
                        prefs.getString(SettingsActivity.DIRECTORY_FULL_LOGGING_KEY,
                                getString(R.string.default_dirname_full_logging)), prefs
                );
            } catch (FileNotFoundException | RuntimeException e) {
                Log.e(TAG, "Can't enable logging to file.", e);
            }
        }


        new Handler().post(queueCommandsThread);

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        //mBluetoothAdapter.startDiscovery();
    }

    protected void onResume() {
        super.onResume();
        Intent serviceIntent = new Intent(DashboardActivity.this, ObdService.class);
        if (bindService(serviceIntent, serviceConn, Context.BIND_AUTO_CREATE)) {

        }
    }

    protected void onPause() {
        super.onPause();
        myCSVWriter.closeLogCSVWriter();
        unbindService(serviceConn);
    }

    /*// Inicio: Gestos para abrir el menú
    @Override
    public boolean onTouchEvent(MotionEvent event){
        if (this.mDetector.onTouchEvent(event)) {
            return true;
        }
        return super.onTouchEvent(event);
    }
    // Inicio: Muestra el menú si se hace swipe desde cualquier borde hacia el centro.
    @Override
    public boolean onDown(MotionEvent event) {
        //startActivity(new Intent(MainActivity.this , ConfigActivity.class));
        Toast.makeText(DashboardActivity.this, "Se abre el menú", Toast.LENGTH_LONG).show();
        return true;
    }
    // Fin: Muestra el menú si se hace swipe desde cualquier borde hacia el centro.
    @Override
    public void onShowPress(MotionEvent e) {

    }
    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        return false;
    }
    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {

    }
    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        return false;
    }
    // Fin: Gestos para abrir el menú*/
}
