package com.example.sergiojosemp.canbt.service;

import android.app.IntentService;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import com.example.sergiojosemp.canbt.activity.DashboardActivity;
import com.example.sergiojosemp.canbt.activity.Inject;
import com.example.sergiojosemp.canbt.activity.MainMenuActivity;
import com.github.pires.obd.commands.protocol.EchoOffCommand;
import com.github.pires.obd.commands.protocol.LineFeedOffCommand;
import com.github.pires.obd.commands.protocol.ObdResetCommand;
import com.github.pires.obd.commands.protocol.SelectProtocolCommand;
import com.github.pires.obd.commands.protocol.TimeoutCommand;
import com.github.pires.obd.commands.temperature.AmbientAirTemperatureCommand;
import com.github.pires.obd.enums.ObdProtocols;
import com.github.pires.obd.exceptions.MisunderstoodCommandException;
import com.github.pires.obd.exceptions.UnsupportedCommandException;
import com.github.pires.obd.reader.ObdCommandJob;
import com.github.pires.obd.reader.ObdCommandJob.ObdCommandJobState;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static com.example.sergiojosemp.canbt.activity.SettingsActivity.PROTOCOLS_LIST_KEY;

//Parte de la API OBD-Java

public class ObdService extends IntentService {
    private final IBinder binder = new ObdThingsBinder();
    // Vamos a definir una cola de trabajos como se hace en la aplicación sample de la API obd-java
    protected BlockingQueue<ObdCommandJob> jobsQueue = new LinkedBlockingQueue<>();
    protected Long queueCounter = 0L;
    protected String TAG = "";
    protected Context ctx = this;
    @Inject
    SharedPreferences prefs;
    private BluetoothDevice dev = null;
    private BluetoothDevice btd;
    private BluetoothSocket sock;
    private Boolean obdDevice = false;
    private Thread t = new Thread(new Runnable() {
        @Override
        public void run() {
            try {
                Log.d(TAG, "Hilo de fondo - -------------------------");
                executeQueue();
            } catch (InterruptedException e) {
                t.interrupt();
            }
        }
    });
    private BlockingQueue<Boolean> obdDeviceQueue = new LinkedBlockingQueue<>();
    private Thread deviceChecker = new Thread(new Runnable() {
        @Override
        public void run() {
            try {
                while (!deviceChecker.isInterrupted()) {
                    if (obdDeviceQueue.take()) {//Blocks until someone puts a boolean value in the queue
                        ((MainMenuActivity) ctx).runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                ((MainMenuActivity) ctx).setObdIndicatorOn();
                            }
                        });
                        startObdConnection();
                    } else {
                        ((MainMenuActivity) ctx).runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                ((MainMenuActivity) ctx).setObdIndicatorOff();

                            }
                        });
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    });

    public ObdService(String name) {
        super(name);
    }

    public ObdService() {
        super("me");
    }

    public SharedPreferences getPrefs() {
        return prefs;
    }

    public void setPrefs(SharedPreferences prefs) {
        this.prefs = prefs;
    }

    public BlockingQueue<Boolean> getObdDeviceQueue() {
        return obdDeviceQueue;
    }

    public void setObdDeviceQueue(BlockingQueue<Boolean> obdDeviceQueue) {
        this.obdDeviceQueue = obdDeviceQueue;
    }

    public void putObdDeviceQueue(Boolean value) {
        try {
            obdDeviceQueue.put(value);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void checkDevice() {
        if(!obdDevice && !deviceChecker.isAlive()){ //Solo ejecutar el hilo si no se ha puesto en marcha anteriormente o si el que estaba en marcha ha terminado
            deviceChecker.start();
        }
    }

    public synchronized void connect() {
        BluetoothSocket tmp = null;
        // Get a BluetoothSocket to connect with the given BluetoothDevice
        try {
            tmp = (BluetoothSocket) btd.getClass().getMethod("createRfcommSocket", new Class[]{int.class}).invoke(btd, 1);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }

        sock = tmp;

        try {
            sock.connect();
            Log.d(TAG, "Connected to BT device. OBD Device?...");
            //isObdDevice();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(ctx, "Connection error. Is this an OBD adapter?", Toast.LENGTH_SHORT).show();
        }
    }

    public void isObdDevice() throws IOException {

        Log.d(TAG, "Checking if BT device is an OBD adapter or not...");
/*
        try {
            Thread.sleep(500); //Espera para que de tiempo a cargar la MainMenuActivity
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
*/

        if (sock.isConnected()) {
            final Thread obdCheck = new Thread(new Runnable() {
                @Override
                public void run() {
                    ObdCommandJob job = null;
                    job = new ObdCommandJob(new EchoOffCommand());
                    try {
                        Log.d(TAG, "Dummy command to check whether device is an OBD adapter or not.");
                        job.getCommand().run(sock.getInputStream(), sock.getOutputStream()); //Blocking call
                        setObdDevice(true);
                        putObdDeviceQueue(true);
                        //startObdConnection();
                        Log.d(TAG, "BT device is an OBD adapter :)");
                    } catch (InterruptedException | IOException e) {
                        t.interrupt();
                    } catch(MisunderstoodCommandException me){
                        //Toast.makeText(ctx, "Error, please restart", Toast.LENGTH_SHORT).show();
                        t.interrupt();
                    }
                }
            });
            obdCheck.start();
        } else {
            Log.d(TAG, "BT device is disconnected");
            obdDevice = false;
            putObdDeviceQueue(false);
        }
    }

    private void startObdConnection() throws IOException {
        // Let's configure the connection.
        Log.d(TAG, "Queueing jobs for connection configuration..");
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

        /*
         * Will send second-time based on tests.
         *
         * TODO this can be done w/o having to queue jobs by just issuing
         * command.run(), command.getResult() and validate the result.
         */
        queueJob(new ObdCommandJob(new EchoOffCommand()));
        queueJob(new ObdCommandJob(new LineFeedOffCommand()));
        queueJob(new ObdCommandJob(new TimeoutCommand(120)));


        // Get protocol from preferences
        final String protocol = prefs.getString(PROTOCOLS_LIST_KEY, "AUTO");
        queueJob(new ObdCommandJob(new SelectProtocolCommand(ObdProtocols.valueOf(protocol))));

        // Job for returning dummy data
        queueJob(new ObdCommandJob(new AmbientAirTemperatureCommand()));
        queueCounter = 0L;
        Log.d(TAG, "Initialization jobs queued.");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        final BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();

        Log.d(TAG, "Creating service...");
        t.start();
        Log.d(TAG, "Service created.");
        //deviceChecker.start();
        prefs = getSharedPreferences("preferences",
                Context.MODE_MULTI_PROCESS);
    }

    public void queueJob(ObdCommandJob job) {
        queueCounter++;
        Log.d(TAG, "Adding job[" + queueCounter + "] to queue..");
        job.setId(queueCounter);
        try {
            jobsQueue.put(job);
            Log.d(TAG, "Job " + job.getCommand().getName() + " queued successfully.");
        } catch (InterruptedException e) {
            job.setState(ObdCommandJob.ObdCommandJobState.QUEUE_ERROR);
            Log.e(TAG, "Failed to queue job " + job.getCommand().getName());
        }
    }


    protected void executeQueue() throws InterruptedException {
        prefs = getSharedPreferences("preferences",
                Context.MODE_MULTI_PROCESS);
        while (!Thread.currentThread().isInterrupted()) {
            /*if(obdDevice) {
                Log.d(TAG,"Context --> " + ctx.toString());
                if (ctx.getClass().equals(DashboardActivity.class)) { *///Solo se puede entrar aquí si el socket está conectado y el dispositivo al que está conectado es OBD
            Log.d(TAG, "Executing queue...");
            ObdCommandJob job = null; // Un Job tiene un ObdCommand (comando cualquiera) y un estado (NEW, RUNNING,...)
            try {
                job = jobsQueue.take(); //Bloqueo hasta que empiecen a entrar comandos
                Log.d(TAG, "Taking job[" + job.getId() + "] from queue..");

                if (job.getState().equals(ObdCommandJobState.NEW) && ctx.getClass().equals(DashboardActivity.class)) {
                    if (sock.isConnected() && ctx.getClass().equals(DashboardActivity.class)) {
                        Log.d(TAG, "Job state is NEW. Run it..");
                        job.setState(ObdCommandJobState.RUNNING);
                        job.getCommand().run(sock.getInputStream(), sock.getOutputStream());
                        if (job != null) {
                            Log.d(TAG, "Updating graphic dashboard with command " + job.getCommand().getName() + " result = " + job.getCommand().getFormattedResult() + "...");
                            final ObdCommandJob job2 = job;
                            ((DashboardActivity) ctx).runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    ((DashboardActivity) ctx).stateUpdate(job2);
                                }
                            });
                        }

                    } else {
                        job.setState(ObdCommandJobState.EXECUTION_ERROR);
                        Log.e(TAG, "Can't run command on a closed socket.");
                        Thread.currentThread().interrupt();
                    }
                } else {
                    Log.e(TAG, "Job state was not new, so it shouldn't be in queue. BUG ALERT!");
                }
            } catch (InterruptedException i) {
                Thread.currentThread().interrupt();
            } catch (UnsupportedCommandException u) {
                if (job != null) {
                    job.setState(ObdCommandJobState.NOT_SUPPORTED);
                }
                Log.d(TAG, "Command not supported. -> " + u.getMessage());
            } catch (IOException io) {
                if (job != null) {
                    if (io.getMessage().contains("Broken pipe"))
                        job.setState(ObdCommandJobState.BROKEN_PIPE);
                    else
                        job.setState(ObdCommandJobState.EXECUTION_ERROR);
                }
                Log.e(TAG, "IO error. -> " + io.getMessage());
            } catch (Exception e) {
                if (job != null) {
                    job.setState(ObdCommandJobState.EXECUTION_ERROR);
                }
                Log.e(TAG, "Failed to run command. -> " + e.getMessage());
            }
        }/*else if(ctx.getClass().equals(MainMenuActivity.class)){ //Sacar un thread separado y habilitar una BlockingQueue para reducir el consumo
                        Thread.sleep(800);

                        if(obdDevice && !colored){
                            ((MainMenuActivity) ctx).runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        ((MainMenuActivity) ctx).setObdIndicatorOn();
                                    }
                                });
                            colored = true;
                        }else if (!obdDevice){
                            ((MainMenuActivity) ctx).runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        ((MainMenuActivity) ctx).setObdIndicatorOff();

                                    }

                                });
                            colored = false;
                            }
                }*/
        //}
        //}
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }


    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        if (prefs == null) {
            prefs = getSharedPreferences("Preferences",
                    Context.MODE_MULTI_PROCESS);
        }
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "\n\nService done\n\n");
    }

    public Boolean getObdDevice() {
        return obdDevice;
    }

    public void setObdDevice(Boolean obdDevice) {
        this.obdDevice = obdDevice;
    }

    public BluetoothDevice getBluetoothDevice() {
        return btd;
    }

    public void setBluetoothDevice(BluetoothDevice btd) {
        this.btd = btd;
    }

    public boolean queueEmpty() {
        return jobsQueue.isEmpty();
    }

    public BluetoothSocket getSock() {
        return sock;
    }

    public void setContext(Context c) {
        ctx = c;
    }

    public void startService() throws IOException {
        if (prefs == null) {
            prefs = getSharedPreferences("preferences",
                    Context.MODE_MULTI_PROCESS);
        }
        Log.d(TAG, "Starting Service.");
    }

    public void stopService() {
    }

    //Devuelve una instancia de esta misma clase, así quien se enlace con este servicio, puede acceder a los métodos públicos
    // de esta clase
    public class ObdThingsBinder extends Binder {
        public ObdService getService() {
            return ObdService.this;
        }
    }
}
