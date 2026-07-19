package com.sergiojosemp.obddashboard.activity

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.GestureDetectorCompat
import androidx.core.view.ViewCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.sergiojosemp.obddashboard.R
import com.sergiojosemp.obddashboard.databinding.LineChartActivityBinding
import com.sergiojosemp.obddashboard.vm.ChartViewModel
import lecho.lib.hellocharts.model.Axis
import lecho.lib.hellocharts.model.AxisValue
import lecho.lib.hellocharts.model.Line
import lecho.lib.hellocharts.model.LineChartData
import lecho.lib.hellocharts.model.PointValue
import lecho.lib.hellocharts.view.LineChartView
import java.text.DecimalFormat
import java.util.Random

class ChartActivity : AppCompatActivity(), GestureDetector.OnGestureListener {

    private lateinit var binding: LineChartActivityBinding
    private lateinit var viewModel: ChartViewModel

    private lateinit var lineChartView: LineChartView
    private val parameterCharts = HashMap<String, Line>()
    private val parameterButtons = HashMap<String, FloatingActionButton>()
    private val parameterLabels = HashMap<String, TextView>()
    private val invParameterButtons = HashMap<FloatingActionButton, String>()
    private val invParameterLabels = HashMap<TextView, String>()

    private val fabButtons = ArrayList<FloatingActionButton>()
    private val fblabels = ArrayList<TextView>()
    private var chartLabel: TextView? = null

    private lateinit var mDetector: GestureDetectorCompat

    private val data = LineChartData()
    private val lines = ArrayList<Line>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.line_chart_activity)
        viewModel = ViewModelProvider(this).get(ChartViewModel::class.java)

        val path = intent.extras?.getString("path") ?: ""

        chartLabel = binding.chartLbl
        setupFabButtons()
        moveRight()
        mDetector = GestureDetectorCompat(this, this)
        startFullScreen()
        setupChartView()

        viewModel.csvValues.observe(this) { csvValues ->
            if (csvValues.isNotEmpty()) {
                buildChartData(csvValues)
            }
        }

        viewModel.isEmptyFile.observe(this) { isEmpty ->
            if (isEmpty) {
                Toast.makeText(this, R.string.empty_file, Toast.LENGTH_SHORT).show()
                onBackPressed()
            }
        }

        viewModel.loadCsvFile(path)
    }

    private fun setupFabButtons() {
        fabButtons.apply {
            add(binding.chartfb1)
            add(binding.chartfb2)
            add(binding.chartfb3)
            add(binding.chartfb4)
            add(binding.chartfb5)
            add(binding.chartfb6)
            add(binding.chartfb7)
            add(binding.chartfb8)
            add(binding.chartfb9)
            add(binding.chartfb10)
            add(binding.chartfb11)
            add(binding.chartfb12)
            add(binding.chartfb13)
            add(binding.chartfb14)
            add(binding.chartfb15)
            add(binding.chartfb16)
            add(binding.chartfb17)
            add(binding.chartfb18)
            add(binding.chartfb19)
            add(binding.chartfb20)
            add(binding.chartfb21)
            add(binding.chartfb22)
            add(binding.chartfb23)
            add(binding.chartfb24)
            add(binding.chartfb25)
            add(binding.chartfb26)
        }

        fblabels.apply {
            add(binding.fblabel1)
            add(binding.fblabel2)
            add(binding.fblabel3)
            add(binding.fblabel4)
            add(binding.fblabel5)
            add(binding.fblabel6)
            add(binding.fblabel7)
            add(binding.fblabel8)
            add(binding.fblabel9)
            add(binding.fblabel10)
            add(binding.fblabel11)
            add(binding.fblabel12)
            add(binding.fblabel13)
            add(binding.fblabel14)
            add(binding.fblabel15)
            add(binding.fblabel16)
            add(binding.fblabel17)
            add(binding.fblabel18)
            add(binding.fblabel19)
            add(binding.fblabel20)
            add(binding.fblabel21)
            add(binding.fblabel22)
            add(binding.fblabel23)
            add(binding.fblabel24)
            add(binding.fblabel25)
            add(binding.fblabel26)
        }

        val fabListener = View.OnClickListener { v ->
            moveRight()
            val paramName = invParameterButtons[v] ?: return@OnClickListener
            val tempLine = parameterCharts[paramName] ?: return@OnClickListener

            if (data.lines?.contains(tempLine) != true) {
                lines.add(tempLine)
                data.lines = lines
                lineChartView.lineChartData = data
                chartLabel?.let { label ->
                    label.text = if (label.text.toString() == "Datos OBD") paramName else "${label.text}, $paramName"
                }
            } else {
                lines.remove(tempLine)
                data.lines = lines
                lineChartView.lineChartData = data
                chartLabel?.let { label ->
                    val currentText = label.text.toString()
                    var newText = currentText.replace("$paramName, ", "")
                        .replace(", $paramName", "")
                        .replace(paramName, "")
                    label.text = if (newText.isEmpty()) "Datos OBD" else newText
                }
            }
        }

        for (fb in fabButtons) {
            fb.setOnClickListener(fabListener)
            fb.isActivated = false
            fb.visibility = View.INVISIBLE
        }

        for (flb in fblabels) {
            flb.visibility = View.INVISIBLE
        }
    }

    private fun setupChartView() {
        lineChartView = binding.chart
        lineChartView.isClickable = true
        lineChartView.setOnTouchListener { v, event ->
            if (mDetector.onTouchEvent(event)) false else true
        }
    }

    @SuppressLint("RestrictedApi")
    private fun buildChartData(csvValues: Map<String, List<String>>) {
        val decimalPattern = "#.##"
        val decimalFormat = DecimalFormat(decimalPattern)

        val noDisplayParameters = listOf(
            "TIME", "LATITUDE", "LONGITUDE", "ALTITUDE", "VIN",
            "VEHICLE_ID", "FUEL_TYPE", "ENGINE_RUNTIME",
            "DISTANCE_TRAVELED_MIL_ON", "DTC_NUMBER", "TROUBLE_CODES"
        )

        val axisValuesForX = mutableListOf<AxisValue>()
        val axisValuesForY = mutableListOf<AxisValue>()

        for (i in 0..30) {
            val fVal = (i * 5).toFloat()
            val tempAxisValue = AxisValue(fVal)
            tempAxisValue.setLabel((fVal.toString()).toCharArray())
            axisValuesForY.add(tempAxisValue)
        }

        for (key in csvValues.keys) {
            if (key !in noDisplayParameters) {
                val values = mutableListOf<PointValue>()
                var tidx = 0

                for (v in csvValues[key] ?: continue) {
                    val timeStr = csvValues["TIME"]?.get(tidx) ?: ""
                    val time = try { timeStr.toFloat() } catch (_: NumberFormatException) { 0f }

                    if (!v.equals("null") && v.isNotEmpty()) {
                        var cleanedValue = v.replace(",", ".")
                        var divide = false
                        if (cleanedValue.contains("RPM")) {
                            divide = true
                        }
                        val unitPart = cleanedValue.replace(Regex("[0-9.]"), "")
                        cleanedValue = cleanedValue.replace(Regex("[a-zA-Z%/]"), "")

                        try {
                            var value = cleanedValue.toFloat()
                            if (divide) {
                                value = value / 100f
                            }

                            val tempPointValue = PointValue(time, value)
                            tempPointValue.setLabel((decimalFormat.format(value) + " $unitPart").toCharArray())
                            values.add(tempPointValue)
                        } catch (_: NumberFormatException) {
                            Log.e("", "Bad value $v")
                        }
                    }
                    tidx++
                }

                if (values.isNotEmpty()) {
                    val rand = Random()
                    val randomColor = Color.rgb(rand.nextInt(255), rand.nextInt(255), rand.nextInt(255))

                    val line = Line(values)
                        .setColor(randomColor)
                        .setHasPoints(true)
                        .setPointRadius(1)
                        .setHasLabelsOnlyForSelected(true)

                    parameterCharts[key] = line

                    var assigned = false
                    for ((i, fb) in fabButtons.withIndex()) {
                        if (!fb.isActivated && !assigned) {
                            fb.isActivated = true
                            fb.visibility = View.VISIBLE
                            fb.backgroundTintList = android.content.res.ColorStateList.valueOf(randomColor)
                            parameterButtons[key] = fb
                            invParameterButtons[fb] = key

                            val fblabel = fblabels[i]
                            fblabel.text = key.lowercase().replace("[aeiou]".toRegex(), "")
                            fblabel.visibility = View.VISIBLE
                            parameterLabels[key] = fblabel
                            invParameterLabels[fblabel] = key
                            assigned = true
                        }
                    }

                    lines.add(line)
                }
            }
        }

        val maxTime = viewModel.getMaxTime()
        val stepsX = ((maxTime / 5f) + 1).toInt()
        for (i in 0..stepsX) {
            val fVal = (i * 5).toFloat()
            if (fVal > maxTime) break
            val tempAxisValue = AxisValue(fVal)
            tempAxisValue.setLabel((fVal.toString()).toCharArray())
            axisValuesForX.add(tempAxisValue)
        }

        val xAxis = Axis(axisValuesForX)

        val yAxis = Axis(axisValuesForY)

        data.axisXBottom = xAxis
        data.axisYLeft = yAxis
        data.lines = lines
        lineChartView.lineChartData = data
    }

    @SuppressLint("SourceLockedOrientationActivity")
    private fun startFullScreen() {
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_IMMERSIVE
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
            )
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return mDetector.onTouchEvent(event)
    }

    override fun onDown(e: MotionEvent): Boolean = false

    override fun onShowPress(e: MotionEvent) {}

    override fun onSingleTapUp(e: MotionEvent): Boolean = false

    override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean = false

    override fun onLongPress(e: MotionEvent) {}

    override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
        if (e1 == null || e2 == null) return true
        Log.d("", "swipe")
        val sensitivity = 25f

        if ((e2.x - e1.x) > sensitivity) {
            moveRight()
        } else if ((e1.x - e2.x) > sensitivity) {
            moveLeft()
        }

        return true
    }

    private fun moveRight() {
        var x = 250f
        fabButtons.forEachIndexed { i, fb ->
            if (i % 5 == 0) {
                x += 500f
            }
            fb.translationX = x
        }

        x = 250f
        fblabels.forEachIndexed { i, flb ->
            if (i % 5 == 0) {
                x += 500f
            }
            flb.translationX = x
        }
    }

    private fun moveLeft() {
        fabButtons.forEach { fb ->
            fb.translationX = 0f
        }

        fblabels.forEach { flb ->
            flb.translationX = 0f
        }
    }
}
