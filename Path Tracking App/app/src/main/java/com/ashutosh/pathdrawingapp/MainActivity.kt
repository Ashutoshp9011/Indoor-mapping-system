package com.ashutosh.pathdrawingapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.ashutosh.pathdrawingapp.sensors.DirectionSensor
import com.ashutosh.pathdrawingapp.sensors.StepDetector
import com.ashutosh.pathdrawingapp.ui.screens.IndoorMapScreen
import com.ashutosh.pathdrawingapp.ui.theme.IndoorMapperTheme

class MainActivity : ComponentActivity() {

    private val viewModel: IndoorMapViewModel by viewModels()

    private lateinit var directionSensor: DirectionSensor
    private lateinit var stepDetector:    StepDetector

    /**
     * Always holds the latest stabilised heading from DirectionSensor.
     * StepDetector callback reads this when a step fires so both
     * values (step length + heading) are in sync.
     */
    private var currentAngle = 0f

    companion object {
        private const val REQ_ACTIVITY = 1001
    }

    // ─────────────────────────────────────────────────────────
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            IndoorMapperTheme {
                IndoorMapScreen(viewModel = viewModel)
            }
        }

        initSensors()
        requestActivityPermission()
    }

    // ─────────────────────────────────────────────────────────

    private fun initSensors() {

        // 1. Direction sensor — HeadingStabiliser pipeline inside
        directionSensor = DirectionSensor(this) { stableAngle ->
            currentAngle = stableAngle
            viewModel.updateAngle(stableAngle)
        }

        // 2. Step detector — Weinberg adaptive step length inside
        //    Passes both the per-step canvas pixels AND the latest heading
        //    so ViewModel can apply the path-history hint correctly.
        stepDetector = StepDetector(this) { stepLengthPx ->
            viewModel.onStepDetected(stepLengthPx, currentAngle)
        }
    }

    // ─────────────────────────────────────────────────────────
    override fun onResume() {
        super.onResume()
        directionSensor.start()
        if (hasActivityPermission()) stepDetector.start()
    }

    override fun onPause() {
        super.onPause()
        directionSensor.stop()
        stepDetector.stop()
    }

    // ── Permissions ──────────────────────────────────────────

    private fun hasActivityPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACTIVITY_RECOGNITION
            ) == PackageManager.PERMISSION_GRANTED
        } else true
    }

    private fun requestActivityPermission() {
        if (!hasActivityPermission() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACTIVITY_RECOGNITION),
                REQ_ACTIVITY,
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_ACTIVITY) {
            if (grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                stepDetector.start()
                Toast.makeText(this, "Step tracking enabled ✓", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(
                    this,
                    "Step tracking needs Physical Activity permission",
                    Toast.LENGTH_LONG,
                ).show()
            }
        }
    }
}
