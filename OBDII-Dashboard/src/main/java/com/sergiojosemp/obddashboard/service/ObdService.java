package com.sergiojosemp.obddashboard.service;

import android.app.IntentService;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.sergiojosemp.obddashboard.R;
import com.sergiojosemp.obddashboard.activity.DashboardActivity;
import com.sergiojosemp.obddashboard.activity.DiagnosticTroubleCodeActivity;
import com.sergiojosemp.obddashboard.activity.Inject;
import com.sergiojosemp.obddashboard.activity.MenuActivity;
import com.sergiojosemp.obddashboard.activity.SettingsActivity;
import com.sergiojosemp.obddashboard.activity.VerboseActivity;
import com.github.pires.obd.commands.protocol.EchoOffCommand;
import com.github.pires.obd.commands.protocol.LineFeedOffCommand;
import com.github.pires.obd.commands.protocol.ObdResetCommand;
import com.github.pires.obd.commands.protocol.SelectProtocolCommand;
import com.github.pires.obd.commands.protocol.TimeoutCommand;
import com.github.pires.obd.commands.temperature.AmbientAirTemperatureCommand;
import com.github.pires.obd.enums.ObdProtocols;
import com.github.pires.obd.exceptions.MisunderstoodCommandException;
import com.github.pires.obd.exceptions.StoppedException;
import com.github.pires.obd.exceptions.UnsupportedCommandException;
import com.github.pires.obd.reader.ObdCommandJob;
import com.github.pires.obd.reader.ObdCommandJob.ObdCommandJobState;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

//Based on github.pires obd-reader  https://github.com/pires/android-obd-reader/

public class ObdService extends IntentService {

    private final String PREFERENCES = "preferences";
    private final IBinder binder = new ObdServiceBinder();
    // Vamos a definir una cola de trabajos como se hace en la aplicación sample de la API obd-java
    protected BlockingQueue<ObdCommandJob> jobsQueue = new LinkedBlockingQueue<>();
    private BlockingQueue<Boolean> obdDeviceQueue = new LinkedBlockingQueue<>();
    protected Long queueCounter = 0L;
    protected String TAG = ObdService.class.getName();
    protected Context ctx = this;
    private BluetoothDevice bluetoothDevice;
    private BluetoothSocket bluetoothSocket;
    private boolean verboseMode = false;
    private Boolean obdDevice = false;

    @Inject
    SharedPreferences preferences;
    private Thread t = new Thread(new Runnable() {
        @Override
        public void run() {
            try {
                Log.d(TAG, getText(R.string.obd_service_working).toString());
                executeQueue();
            } catch (InterruptedException e) {
                t.interrupt();
            }
        }
    });
    private Thread obdAdaptedCheckerThread = new Thread(new Runnable() {
        @Override
        public void run() {
            /*try {
                while (!obdAdaptedCheckerThread.isInterrupted()) {
                    if (obdDeviceQueue.take()) {//Blocks until someone puts a boolean value in the queue
                        ((MenuActivity) ctx).runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                ((MenuActivity) ctx).setObdIndicatorOn();
                            }
                        });
                        startObdConnection();
                    } else {
                        ((MenuActivity) ctx).runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                ((MenuActivity) ctx).setObdIndicatorOff();

                            }
                        });
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            catch (IOException e) {
                e.printStackTrace();
            }*/
        }
    });

    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     *
     * @param name Used to name the worker thread, important only for debugging.
     */
    public ObdService(String name) {
        super(name);
    }
    public ObdService() {
        super("ObdService");
    }


    public SharedPreferences getPreferences() {
        return preferences;
    }

    public void setPreferences(SharedPreferences preferences) {
        this.preferences = preferences;
    }

    public void setVerboseMode(boolean verboseMode) {
        this.verboseMode = verboseMode;
    }

    public void putObdDeviceQueue(Boolean value) {
        try {
            obdDeviceQueue.put(value);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void checkDevice() {
        if(!obdDevice && !obdAdaptedCheckerThread.isAlive()){ //Solo ejecutar el hilo si no se ha puesto en marcha anteriormente o si el que estaba en marcha ha terminado
            obdAdaptedCheckerThread.start();
        }
    }

    public synchronized void connectToDevice() {
        BluetoothSocket tmp = null;
        // Get a BluetoothSocket to connectToDevice with the given BluetoothDevice
        try {
            tmp = (BluetoothSocket) bluetoothDevice.getClass().getMethod("createRfcommSocket", new Class[]{int.class}).invoke(bluetoothDevice, 1);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }

        bluetoothSocket = tmp;

        try {
            bluetoothSocket.connect();
            Log.d(TAG, getText(R.string.connected_to_device).toString());
            //isObdDevice();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(ctx, getText(R.string.connection_error_is_obd_adapter).toString(), Toast.LENGTH_SHORT).show();
        }
    }

    public void isObdDevice() throws IOException {

        Log.d(TAG, getText(R.string.checking_if_obdr).toString());
        if (bluetoothSocket.isConnected()) {
            final Thread obdCheck = new Thread(new Runnable() {
                @Override
                public void run() {
                    ObdCommandJob job = null;
                    job = new ObdCommandJob(new EchoOffCommand());
                    try {
                        Log.d(TAG, getText(R.string.dummy_command).toString());
                        job.getCommand().run(bluetoothSocket.getInputStream(), bluetoothSocket.getOutputStream()); //Blocking call
                        setObdDevice(true);
                        putObdDeviceQueue(true);
                        //startObdConnection();
                        Log.d(TAG, getText(R.string.is_obd).toString());
                    } catch (InterruptedException | IOException e) {
                        t.interrupt();
                    } catch(MisunderstoodCommandException me){
                        //Toast.makeText(ctx, "Error, please restart", Toast.LENGTH_SHORT).show();
                        t.interrupt();
                    } catch (StoppedException se){
                        t.interrupt();
                    }
                }
            });
            obdCheck.start();
        } else {
            Log.d(TAG, getText(R.string.status_bluetooth_error_connecting).toString());
            obdDevice = false;
            putObdDeviceQueue(false);
        }
    }

    private void startObdConnection() throws IOException {
        // Let's configure the connection.
        Log.d(TAG, getText(R.string.queueing_for_config).toString());
        jobsQueue.clear();
        queueJob(new ObdCommandJob(new ObdResetCommand()));
        queueJob(new ObdCommandJob(new ObdResetCommand()));
        queueJob(new ObdCommandJob(new ObdResetCommand()));

        //Below is to give the adapter enough time to reset before sending the commands, otherwise the first startup commands could be ignored.
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        queueJob(new ObdCommandJob(new EchoOffCommand()));
        queueJob(new ObdCommandJob(new LineFeedOffCommand()));
        queueJob(new ObdCommandJob(new TimeoutCommand(120))); //Según pruebas y documentación ELM327

        // Getting protocol from preferences
        final String protocol = preferences.getString(SettingsActivity.PROTOCOLS_LIST_KEY, "AUTO");
        queueJob(new ObdCommandJob(new SelectProtocolCommand(ObdProtocols.valueOf(protocol))));
        // Job for returning dummy data
        queueJob(new ObdCommandJob(new AmbientAirTemperatureCommand()));
        queueCounter = 0L;
        Log.d(TAG, getText(R.string.initialization_jobs).toString());
    }

    @Override
    public void onCreate() {
        super.onCreate();
        final BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();

        Log.d(TAG, getText(R.string.creating_service).toString());
        t.start();
        Log.d(TAG, getText(R.string.service_created).toString());
        preferences = getSharedPreferences(PREFERENCES,
                Context.MODE_MULTI_PROCESS);
    }

    public void queueJob(ObdCommandJob job) {
        queueCounter++;
        Log.d(TAG, getText(R.string.adding_job).toString() + queueCounter + getText(R.string.to_queue).toString());
        job.setId(queueCounter);
        try {
            jobsQueue.put(job);
            Log.d(TAG, getText(R.string.job).toString() + job.getCommand().getName() + getText(R.string.queued_succesfully).toString());
        } catch (InterruptedException e) {
            job.setState(ObdCommandJob.ObdCommandJobState.QUEUE_ERROR);
            Log.e(TAG, getText(R.string.failed_queueing_job).toString() + job.getCommand().getName());
        }
    }


    protected void executeQueue() throws InterruptedException {
        preferences = getSharedPreferences(PREFERENCES,
                Context.MODE_MULTI_PROCESS);

        while (!Thread.currentThread().isInterrupted()) {
            Log.d(TAG, getText(R.string.executing_queue).toString());
            ObdCommandJob job = null; // Un Job tiene un ObdCommand (comando cualquiera) y un estado (NEW, RUNNING,...)
            try {
                job = jobsQueue.take(); //Bloqueo hasta que empiecen a entrar comandos
                Log.d(TAG, getText(R.string.taking_job).toString() + job.getId() + getText(R.string.from_queue).toString());

                if (job.getState().equals(ObdCommandJobState.NEW) && (ctx.getClass().equals(DashboardActivity.class) || ctx.getClass().equals(VerboseActivity.class))) {
                    if (bluetoothSocket.isConnected() && (ctx.getClass().equals(DashboardActivity.class) || ctx.getClass().equals(VerboseActivity.class))) {
                        Log.d(TAG, getText(R.string.job_is_new).toString());
                        job.setState(ObdCommandJobState.RUNNING);
                        job.getCommand().run(bluetoothSocket.getInputStream(), bluetoothSocket.getOutputStream());
                        if (job != null) {
                            Log.d(TAG, getText(R.string.updating_dash).toString() + job.getCommand().getName() + getText(R.string.updating_dash).toString() + job.getCommand().getFormattedResult() + "...");
                            final ObdCommandJob job2 = job;
                            if(ctx != this) {/*
                                if (verboseMode) {
                                    ((VerboseActivity) ctx).runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            ((VerboseActivity) ctx).stateUpdate(job2);
                                        }
                                    });
                                } else {
                                    ((DashboardActivity) ctx).runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            ((DashboardActivity) ctx).stateUpdate(job2);
                                        }
                                    });
                                }*/
                            }
                        }
                    } else {
                        job.setState(ObdCommandJobState.EXECUTION_ERROR);
                        Log.e(TAG, getText(R.string.cant_run_closed_socket).toString());
                        Thread.currentThread().interrupt();
                    }
                } else {
                    Log.e(TAG, getText(R.string.bug_alert).toString());
                }
            } catch (InterruptedException i) {
                Thread.currentThread().interrupt();
            } catch (UnsupportedCommandException u) {
                if (job != null) {
                    job.setState(ObdCommandJobState.NOT_SUPPORTED);
                }
                Log.d(TAG, getText(R.string.command_not_supported).toString() + " -> " + u.getMessage());
            } catch (IOException io) {
                if (job != null) {
                    if (io.getMessage().contains("Broken pipe"))
                        job.setState(ObdCommandJobState.BROKEN_PIPE);
                    else
                        job.setState(ObdCommandJobState.EXECUTION_ERROR);
                }
                Log.e(TAG, getText(R.string.io_error).toString() + " -> " + io.getMessage());
            }catch (Exception e) {
                if (job != null) {
                    job.setState(ObdCommandJobState.EXECUTION_ERROR);
                }
                Log.e(TAG, getText(R.string.failed_to_run_command).toString() + " -> " + e.getMessage());
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }


    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        if (preferences == null) {
            preferences = getSharedPreferences(PREFERENCES,
                    Context.MODE_MULTI_PROCESS);
        }
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, getText(R.string.service_done).toString());
    }

    public void setObdDevice(Boolean obdDevice) {
        this.obdDevice = obdDevice;
    }

    public void setBluetoothDevice(BluetoothDevice btd) {
        this.bluetoothDevice = btd;
    }

    public boolean queueEmpty() {
        return jobsQueue.isEmpty();
    }

    public void emptyQueue() {
        jobsQueue.clear();
    }

    public BluetoothSocket getbluetoothSocket() {
        return bluetoothSocket;
    }

    public void setContext(Context c) {
        ctx = c;
    }

    public void startService() {
        if (preferences == null) {
            preferences = getSharedPreferences(PREFERENCES,
                    Context.MODE_MULTI_PROCESS);
        }
        Log.d(TAG, getText(R.string.starting_obd_service).toString());
        if(ctx != null && ctx.getClass().equals(DiagnosticTroubleCodeActivity.class)) {
            Log.d(TAG,getText(R.string.executing_queue_paused).toString());
            if(bluetoothSocket != null && bluetoothSocket.isConnected()) {
                troubleCodes = requestTroubleCodes();
            }else{
                Toast.makeText(ctx, getText(R.string.error_getting_dtc).toString(), Toast.LENGTH_LONG).show();
            }
        }

    }

    public String getTroubleCodes() {
        return troubleCodes;
    }

    public void setTroubleCodes(String troubleCodes) {
        this.troubleCodes = troubleCodes;
    }

    private String troubleCodes = "";

    public void stopService() {
    }

    //Devuelve una instancia de esta misma clase, así quien se enlace con este servicio, puede acceder a los métodos públicos
    // de esta clase
    public class ObdServiceBinder extends Binder {
        public ObdService getService() {
            return ObdService.this;
        }
    }


    private boolean dtc = false;

    public boolean isDtc() {
        return dtc;
    }

    public void setDtc(boolean dtc) {
        this.dtc = dtc;
    }

    public String requestTroubleCodes(String... params) {
        String result = "";
        synchronized (this) {
            try {
                // Let's configure the connection.
                Log.d(TAG, getText(R.string.queueing_for_config).toString());
                new ObdResetCommand().run(bluetoothSocket.getInputStream(), bluetoothSocket.getOutputStream());
                new EchoOffCommand().run(bluetoothSocket.getInputStream(), bluetoothSocket.getOutputStream());
                new LineFeedOffCommand().run(bluetoothSocket.getInputStream(), bluetoothSocket.getOutputStream());
                new SelectProtocolCommand(ObdProtocols.AUTO).run(bluetoothSocket.getInputStream(), bluetoothSocket.getOutputStream());


                DiagnosticTroubleCodeActivity.ModifiedTroubleCodesObdCommand tcoc = new DiagnosticTroubleCodeActivity.ModifiedTroubleCodesObdCommand();
                tcoc.run(bluetoothSocket.getInputStream(), bluetoothSocket.getOutputStream());
                result = tcoc.getFormattedResult();


            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, e.getMessage());
                return null;
            } catch (InterruptedException e) {
                e.printStackTrace();
                Log.e(TAG, e.getMessage());
                return null;
            }
        }
        return result;
    }

    public Context getDefaultContext(){
        return this;
    }
}
