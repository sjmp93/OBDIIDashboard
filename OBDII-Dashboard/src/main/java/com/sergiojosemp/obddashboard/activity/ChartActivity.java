package com.sergiojosemp.obddashboard.activity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GestureDetectorCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.sergiojosemp.obddashboard.R;
import com.github.pires.obd.commands.ObdCommand;
import com.github.pires.obd.reader.ObdConfig;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import lecho.lib.hellocharts.model.Axis;
import lecho.lib.hellocharts.model.AxisValue;
import lecho.lib.hellocharts.model.Line;
import lecho.lib.hellocharts.model.LineChartData;
import lecho.lib.hellocharts.model.PointValue;
import lecho.lib.hellocharts.model.ValueShape;
import lecho.lib.hellocharts.view.LineChartView;

public class ChartActivity extends AppCompatActivity implements GestureDetector.OnGestureListener {

    private LineChartView lineChartView;
    private SharedPreferences prefs;
    private int maxNumberOfLines = 4;
    private boolean hasAxes = true;
    private boolean hasAxesNames = true;
    private boolean hasLines = true;
    private boolean hasPoints = false;
    private ValueShape shape = ValueShape.CIRCLE;
    private boolean isFilled = false;
    private boolean hasLabels = false;
    private boolean isCubic = false;
    private boolean hasLabelForSelected = false;
    private boolean pointsHaveDifferentColor;
    private boolean hasGradientToTransparent = false;
    private Map<String, Line> parameterCharts = new HashMap<>();
    private Map<String, FloatingActionButton> parameterButtons = new HashMap<>();
    private Map<String, TextView> parameterLabels = new HashMap<>();
    private Map<FloatingActionButton, String> invParameterButtons = new HashMap<>();
    private Map<TextView, String> invParameterLabels = new HashMap<>();
    private String path = "";

    private List<FloatingActionButton> fabButtons = new ArrayList<>(); //Se puede automatizar tanto la creación de fb como de textos
    private List<TextView> fblabels = new ArrayList<>();
    private FloatingActionButton fab1 ,fab2, fab3, fab4, fab5, fab6, fab7, fab8, fab9, fab10, fab11 ,fab12, fab13, fab14, fab15,
            fab16, fab17, fab18, fab19, fab20, fab21 ,fab22, fab23, fab24, fab25, fab26;

    private TextView fblabel1, fblabel2, fblabel3, fblabel4, fblabel5, fblabel6, fblabel7, fblabel8, fblabel9, fblabel10, fblabel11, fblabel12, fblabel13, fblabel14, fblabel15,
            fblabel16, fblabel17, fblabel18, fblabel19, fblabel20,fblabel21, fblabel22, fblabel23, fblabel24, fblabel25, fblabel26;

    private GestureDetectorCompat mDetector;

    private LineChartData data = new LineChartData();
    private List<Line> lines = new ArrayList<Line>();


    @SuppressLint("RestrictedApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Preferences
        prefs = getSharedPreferences("preferences",
                Context.MODE_MULTI_PROCESS);
        String csv_header = "";
        for (ObdCommand Command : ObdConfig.getCommands()) {
            if (prefs.getBoolean(Command.getName(), true))
                csv_header = (Command.getName());
        }

        path = getIntent().getExtras().getString("path");

        //FrontEnd
        setContentView(R.layout.line_chart_activity);

        final TextView label = (TextView)findViewById(R.id.chartLbl);


        fab1 = findViewById(R.id.chartfb1);
        fab2 = findViewById(R.id.chartfb2);
        fab3 = findViewById(R.id.chartfb3);
        fab4 = findViewById(R.id.chartfb4);
        fab5 = findViewById(R.id.chartfb5);
        fab6 = findViewById(R.id.chartfb6);
        fab7 = findViewById(R.id.chartfb7);
        fab8 = findViewById(R.id.chartfb8);
        fab9 = findViewById(R.id.chartfb9);
        fab10 = findViewById(R.id.chartfb10);
        fab11 = findViewById(R.id.chartfb11);
        fab12 = findViewById(R.id.chartfb12);
        fab13 = findViewById(R.id.chartfb13);
        fab14 = findViewById(R.id.chartfb14);
        fab15 = findViewById(R.id.chartfb15);
        fab16 = findViewById(R.id.chartfb16);
        fab17 = findViewById(R.id.chartfb17);
        fab18 = findViewById(R.id.chartfb18);
        fab19 = findViewById(R.id.chartfb19);
        fab20 = findViewById(R.id.chartfb20);
        fab21 = findViewById(R.id.chartfb21);
        fab22 = findViewById(R.id.chartfb22);
        fab23 = findViewById(R.id.chartfb23);
        fab24 = findViewById(R.id.chartfb24);
        fab25 = findViewById(R.id.chartfb25);
        fab26 = findViewById(R.id.chartfb26);


        fblabel1 = findViewById(R.id.fblabel1);
        fblabel2 = findViewById(R.id.fblabel2);
        fblabel3 = findViewById(R.id.fblabel3);
        fblabel4 = findViewById(R.id.fblabel4);
        fblabel5 = findViewById(R.id.fblabel5);
        fblabel6 = findViewById(R.id.fblabel6);
        fblabel7 = findViewById(R.id.fblabel7);
        fblabel8 = findViewById(R.id.fblabel8);
        fblabel9 = findViewById(R.id.fblabel9);
        fblabel10 = findViewById(R.id.fblabel10);
        fblabel11 = findViewById(R.id.fblabel11);
        fblabel12 = findViewById(R.id.fblabel12);
        fblabel13 = findViewById(R.id.fblabel13);
        fblabel14 = findViewById(R.id.fblabel14);
        fblabel15 = findViewById(R.id.fblabel15);
        fblabel16 = findViewById(R.id.fblabel16);
        fblabel17 = findViewById(R.id.fblabel17);
        fblabel18 = findViewById(R.id.fblabel18);
        fblabel19 = findViewById(R.id.fblabel19);
        fblabel20 = findViewById(R.id.fblabel20);
        fblabel21 = findViewById(R.id.fblabel21);
        fblabel22 = findViewById(R.id.fblabel22);
        fblabel23 = findViewById(R.id.fblabel23);
        fblabel24 = findViewById(R.id.fblabel24);
        fblabel25 = findViewById(R.id.fblabel25);
        fblabel26 = findViewById(R.id.fblabel26);


        //Se añaden los botones y textos a una lista


        fabButtons.add(fab1);
        fabButtons.add(fab2);
        fabButtons.add(fab3);
        fabButtons.add(fab4);
        fabButtons.add(fab5);
        fabButtons.add(fab6);
        fabButtons.add(fab7);
        fabButtons.add(fab8);
        fabButtons.add(fab9);
        fabButtons.add(fab10);
        fabButtons.add(fab11);
        fabButtons.add(fab12);
        fabButtons.add(fab13);
        fabButtons.add(fab14);
        fabButtons.add(fab15);
        fabButtons.add(fab16);
        fabButtons.add(fab17);
        fabButtons.add(fab18);
        fabButtons.add(fab19);
        fabButtons.add(fab20);
        fabButtons.add(fab21);
        fabButtons.add(fab22);
        fabButtons.add(fab23);
        fabButtons.add(fab24);
        fabButtons.add(fab25);
        fabButtons.add(fab26);


        fblabels.add(fblabel1);
        fblabels.add(fblabel2);
        fblabels.add(fblabel3);
        fblabels.add(fblabel4);
        fblabels.add(fblabel5);
        fblabels.add(fblabel6);
        fblabels.add(fblabel7);
        fblabels.add(fblabel8);
        fblabels.add(fblabel9);
        fblabels.add(fblabel10);
        fblabels.add(fblabel11);
        fblabels.add(fblabel12);
        fblabels.add(fblabel13);
        fblabels.add(fblabel14);
        fblabels.add(fblabel15);
        fblabels.add(fblabel16);
        fblabels.add(fblabel17);
        fblabels.add(fblabel18);
        fblabels.add(fblabel19);
        fblabels.add(fblabel20);
        fblabels.add(fblabel21);
        fblabels.add(fblabel22);
        fblabels.add(fblabel23);
        fblabels.add(fblabel24);
        fblabels.add(fblabel25);
        fblabels.add(fblabel26);



        View.OnClickListener fabListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                moveRight();
                Line tempLine = parameterCharts.get(invParameterButtons.get(v));
                if(!data.getLines().contains(tempLine)){
                    lines.add(tempLine); //Aquí se añade la gráfica que indica el botón
                    data.setLines(lines);
                    lineChartView.setLineChartData(data);
                    if(label.getText().equals("Datos OBD")){
                        label.setText(invParameterButtons.get(v));
                    }else{
                        label.setText(label.getText() + ", " + invParameterButtons.get(v));
                    }
                }else{
                    lines.remove(tempLine);
                    data.setLines(lines);
                    lineChartView.setLineChartData(data);
                    String temp = label.getText().toString();
                    String temp1 = temp.replace(invParameterButtons.get(v) + ", ","");
                    String temp2 = temp.replace(", " + invParameterButtons.get(v), "");
                    String temp3 = temp.replace(invParameterButtons.get(v), "");
                    if(!temp.equals(temp1)){
                        temp = temp1;
                    }else if(!temp.equals(temp2)){
                        temp = temp2;
                    }else if(!temp.equals(temp3)){
                        temp = temp3;
                    }
                    if(temp.equals("")){
                        label.setText("Datos OBD");
                    }else{
                        label.setText(temp);
                    }
                }
            }
        };

        for(FloatingActionButton fb : fabButtons){
            fb.setOnClickListener(fabListener);
            //Desactivar cuando no estén asignados a un campo concreto
            fb.setActivated(false);
            //Visibilidad en fb y textos

            fb.setVisibility(View.INVISIBLE);
        }
        //Textos
        for(TextView flb : fblabels){
            flb.setVisibility(View.INVISIBLE);
        }



        moveRight();

        mDetector = new GestureDetectorCompat(getApplicationContext(),this);


        startFullScreen();
        chartSetup();
    }


    @SuppressLint("RestrictedApi")
    public void chartSetup(){
        String decimalPattern = "#.##";
        DecimalFormat decimalFormat = new DecimalFormat(decimalPattern);



        /* Lectura del fichero */

        //Get the text file
        File file = new File(path); /*sdcard no es necesario, le estamos pasando el path completo*/

        //Read text from file
        StringBuilder text = new StringBuilder();

        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String sline;
            //TIME;LATITUDE;LONGITUDE;ALTITUDE;VEHICLE_ID;BAROMETRIC_PRESSURE;ENGINE_COOLANT_TEMP;FUEL_LEVEL;ENGINE_LOAD;AMBIENT_AIR_TEMP;ENGINE_RPM;INTAKE_MANIFOLD_PRESSURE;MAF;Term Fuel Trim Bank 1;FUEL_ECONOMY;Long Term Fuel Trim Bank 2;FUEL_TYPE;AIR_INTAKE_TEMP;FUEL_PRESSURE;SPEED;Short Term Fuel Trim Bank 2;Short Term Fuel Trim Bank 1;ENGINE_RUNTIME;THROTTLE_POS;DTC_NUMBER;TROUBLE_CODES;TIMING_ADVANCE;EQUIV_RATIO
            List<Line> lines = new ArrayList<Line>();


            List<PointValue> values = new ArrayList<PointValue>();

            List<AxisValue> axisValuesForX = new ArrayList<>();
            List<AxisValue> axisValuesForY = new ArrayList<>();
            AxisValue tempAxisValue;


            for (float i = 0.0f; i <= 150.00f; i += 5.00f){
                tempAxisValue = new AxisValue(i);
                tempAxisValue.setLabel(""+i);
                axisValuesForY.add(tempAxisValue);
            }







            PointValue tempPointValue;
            Map<String, List<String>> csv_values = new HashMap<>();
            String[] header = null;
            String[] obdvalues = null;
            boolean isFirstLine = true;
            long x = 0L, y = 0L;
            // Lectura del fichero csv -> se monta un diccionario con los valores
            while ((sline = br.readLine()) != null) {
                if (isFirstLine) { //En la primera linea se recoge la cabecera del CSV para obtener los datos a mostrar
                    header = sline.split(";");
                    for(String h : header){
                        csv_values.put(h,new ArrayList<String>()); //Crea el diccionario con clave = Dato a representar y valor = lista de valores de ese dato
                    }
                    isFirstLine = false;
                }else{
                    obdvalues = sline.split(";");
                    int hidx = 0;
                    for(String v : obdvalues){
                        csv_values.get(header[hidx]).add(v);  // CONTROLAR FICHEROS NO PREPARADOS PARA LA APP
                        hidx++;
                    }
                }
            }
            br.close();
            //Fin de la lectura del fichero csv

            if(csv_values.size() == 0){
                Toast.makeText(ChartActivity.this, R.string.empty_file, Toast.LENGTH_SHORT).show();
                onBackPressed();

                throw new NullPointerException(getText(R.string.empty_file).toString());
            }

            //Arraylist de strings con los valores que no pueden mostrarse en linecharts (fueltype etc)
            List<String> noDisplayParameters = new ArrayList<String>();
            noDisplayParameters.add("TIME");
            noDisplayParameters.add("LATITUDE");
            noDisplayParameters.add("LONGITUDE");
            noDisplayParameters.add("ALTITUDE");
            noDisplayParameters.add("VIN");
            noDisplayParameters.add("VEHICLE_ID");
            noDisplayParameters.add("FUEL_TYPE");
            noDisplayParameters.add("ENGINE_RUNTIME");
            noDisplayParameters.add("DISTANCE_TRAVELED_MIL_ON");
            noDisplayParameters.add("DTC_NUMBER");
            noDisplayParameters.add("TROUBLE_CODES");

            float time = 0.0f;
            float value = 0.0f;
            boolean divide = false;
            int p = 0;

            for(String k : csv_values.keySet()){
                values = new ArrayList<PointValue>();
                if(!noDisplayParameters.contains(k)){//!k.equals("TIME") && !k.equals("LATITUDE") && !k.equals("LONGITUDE") && !k.equals("ALTITUDE") && !k.equals("VEHICLE_ID") && !k.equals("FUELTYPE")){
                    int tidx = 0; // Time index for generating points
                    for(String v : csv_values.get(k)) {
                        time = Float.parseFloat(csv_values.get("TIME").get(tidx));
                        if(!v.equals("null") && !v.isEmpty()) {
                            v = v.replace(",", ".");
                            if(v.contains("RPM")){
                                divide = true;
                            }
                            v = v.replaceAll("[a-zA-Z%/]", "");
                            try {
                                value = Float.parseFloat(v);
                                if(divide) {
                                    value = value / 100f; //Las RPM se mostrarán en valores*100
                                    divide = false;
                                }


                                tempPointValue = new PointValue(time, value);
                                tempPointValue.setLabel(decimalFormat
                                        .format(value) + " " + v.replaceAll("[0-9.]",""));
                                values.add(tempPointValue);
                            }catch(NumberFormatException n){
                                Log.e("","Bad value" + v);
                            }
                        }
                        tidx++;
                    }

                    Random rand = new Random();
                    int r = rand.nextInt(255);
                    int g = rand.nextInt(255);
                    int b = rand.nextInt(255);

                    int randomColor = Color.rgb(r,g,b);
                    Line line = new Line(values)
                            .setColor(randomColor)
                            .setCubic(isCubic)
                            .setHasPoints(true).setHasLabels(hasLabels).setPointRadius(1);





                    parameterCharts.put(k,line);
                    boolean assigned = false;
                    int i = 0;
                    for(FloatingActionButton fb : fabButtons){
                        if(!fb.isActivated() && !assigned) {
                            fb.setActivated(true);
                            fb.setVisibility(View.VISIBLE);
                            fb.setBackgroundTintList(ColorStateList.valueOf(randomColor));
                            parameterButtons.put(k, fb);
                            invParameterButtons.put(fb,k);
                            TextView fblabel = fblabels.get(i);
                            fblabel.setText(k.toLowerCase().replaceAll("[aeiou]","")); // Se dejan solo consonantes para los label
                            fblabel.setVisibility(View.VISIBLE);
                            parameterLabels.put(k, fblabel);
                            invParameterLabels.put(fblabel,k);
                            assigned = true;
                        }
                        i++;
                    }
                    line.setHasLabelsOnlyForSelected(true);

                    lines.add(line);

                }



            }

            float max = Float.parseFloat(csv_values.get("TIME").get(csv_values.get("TIME").size()-1));
            for (float i = 0.0f; i <= max; i += 5.0f){
                tempAxisValue = new AxisValue(i);
                tempAxisValue.setLabel(""+i);
                axisValuesForX.add(tempAxisValue);

            }



            Axis xAxis = new Axis(axisValuesForX);
            Axis yAxis = new Axis(axisValuesForY);
            xAxis.setAutoGenerated(true);
            xAxis.setName("Segundos");
            xAxis.setHasSeparationLine(true);
            xAxis.setHasLines(true);
            yAxis.setHasSeparationLine(true);
            yAxis.setHasLines(true);
            yAxis.setName("Ud. magnitud");

            data.setAxisXBottom(xAxis);
            data.setAxisYLeft(yAxis);
            lineChartView = (LineChartView) findViewById(R.id.chart);
            lineChartView.setClickable(true);

            lineChartView.setOnTouchListener(new View.OnTouchListener() {

                @Override
                public boolean onTouch(View v, MotionEvent event) { //Habilita el gesto de swipe en toda la lineChartView
                    if (mDetector.onTouchEvent(event)) {
                        return false;
                    }
                    return true;
                }
            });

        }catch (IOException e) {
            //You'll need to add proper error handling here
        }catch(NullPointerException npe){

        }
    }





    @SuppressLint("SourceLockedOrientationActivity")
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

    @Override
    public boolean onTouchEvent(MotionEvent event){
        return this.mDetector.onTouchEvent(event);
    }


    @Override
    public boolean onDown(MotionEvent e) {
        return false;
    }

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

        Log.d("","swipe");
        float sensitvity = 25;

        if((e2.getX() - e1.getX()) > sensitvity) { //Mover a la derecha
            if(fab1.getX() != 250)
                moveRight();

        }else if((e1.getX() - e2.getX()) > sensitvity) { //Mover a la izquierda
            if(fab1.getX() != 0)
                moveLeft();
        }

        /*//fab.show();
        fab.animate().translationY(0)
                .setInterpolator(new LinearInterpolator())
                .setDuration(1000); // Cambiar al tiempo deseado*/
        return true;
    }

    public void moveRight(){
        int i = 0;
        float x = 250;
        for(FloatingActionButton fb : fabButtons){
            if(i%5 == 0) {
                x = x + 500; // para mover más a la derecha
            }
            fb.animate().translationX(x)
                    .setInterpolator(new LinearInterpolator())
                    .setDuration(200); // Cambiar al tiempo deseado
            i++;
        }
        i = 0;
        x = 250;
        for(TextView flb : fblabels){
            if(i%5 == 0) {
                x = x + 500; // para mover más a la derecha
            }
            flb.animate().translationX(x)
                    .setInterpolator(new LinearInterpolator())
                    .setDuration(200); // Cambiar al tiempo deseado
            i++;
        }
    }

    public void moveLeft() {

        for (FloatingActionButton fb : fabButtons) {
            fb.animate().translationX(0)
                    .setInterpolator(new LinearInterpolator())
                    .setDuration(200); // Cambiar al tiempo deseado
        }

        for (TextView flb : fblabels) {
            flb.animate().translationX(0)
                    .setInterpolator(new LinearInterpolator())
                    .setDuration(200); // Cambiar al tiempo deseado
        }
    }
}