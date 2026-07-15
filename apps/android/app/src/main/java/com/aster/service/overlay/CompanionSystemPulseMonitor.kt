package com.aster.service.overlay

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.BatteryManager
import android.os.PowerManager
import android.os.SystemClock
import androidx.core.content.ContextCompat
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Process-local system signals for the notch.
 *
 * This monitor lives in Aster's foreground-service process, not React Native. It is
 * active only while the overlay is attached and the display is awake. Therefore a
 * minimized OpenAlly app cannot freeze battery or motion reactions, while screen-off
 * power cost remains zero.
 */
class CompanionSystemPulseMonitor(
    private val context: Context,
    private val emit: (String, Map<String, Any>) -> Unit,
) : SensorEventListener {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val lift = SmartLiftClassifier()
    private var started = false
    private var sensorsActive = false
    private var displayActive = false
    private var liftEnabled = true
    private var shakeEnabled = true
    private var firstBattery = true
    private var shakeWindowStartedAt = 0L
    private var shakePeaks = 0
    private var lastShakePeakAt = 0L
    private var strongestShakeG = 0.0
    private var shakeCooldownUntil = 0L

    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    private val pressure = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)

    init {
        lift.configureSensors(
            hasGyroscope = gyroscope != null,
            hasBarometer = pressure != null,
        )
    }

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val i = intent ?: return
            if (i.action != Intent.ACTION_BATTERY_CHANGED &&
                i.action != Intent.ACTION_POWER_CONNECTED &&
                i.action != Intent.ACTION_POWER_DISCONNECTED &&
                i.action != PowerManager.ACTION_POWER_SAVE_MODE_CHANGED
            ) return
            val sticky = if (i.action == Intent.ACTION_BATTERY_CHANGED) i else
                this@CompanionSystemPulseMonitor.context.registerReceiver(
                    null,
                    IntentFilter(Intent.ACTION_BATTERY_CHANGED),
                ) ?: return
            val level = sticky.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = sticky.getIntExtra(BatteryManager.EXTRA_SCALE, 100).coerceAtLeast(1)
            if (level < 0) return
            val status = sticky.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN)
            val plugged = sticky.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
            val external = plugged != 0
            val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL
            emit(
                "power",
                mapOf(
                    "charging" to charging,
                    "external" to external,
                    "percent" to ((level * 100f) / scale).toInt().coerceIn(0, 100),
                    "initial" to firstBattery,
                ),
            )
            firstBattery = false
        }
    }

    fun start(displayActive: Boolean) {
        if (started) {
            setDisplayActive(displayActive)
            return
        }
        started = true
        firstBattery = true
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
            addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
        }
        ContextCompat.registerReceiver(
            context,
            batteryReceiver,
            filter,
            ContextCompat.RECEIVER_EXPORTED,
        )
        this.displayActive = displayActive
        reconcileSensors()
    }

    fun stop() {
        if (!started) return
        setDisplayActive(false)
        runCatching { context.unregisterReceiver(batteryReceiver) }
        started = false
        lift.reset()
    }

    fun setDisplayActive(active: Boolean) {
        if (displayActive == active) return
        displayActive = active
        reconcileSensors()
    }

    /** Change the native sensor policy without restarting the foreground service. */
    fun configure(liftEnabled: Boolean, shakeEnabled: Boolean) {
        if (this.liftEnabled == liftEnabled && this.shakeEnabled == shakeEnabled) return
        this.liftEnabled = liftEnabled
        this.shakeEnabled = shakeEnabled
        if (!liftEnabled) lift.reset()
        reconcileSensors()
    }

    fun noteInteraction() {
        lift.noteInteraction(SystemClock.elapsedRealtime())
    }

    override fun onSensorChanged(event: SensorEvent) {
        val now = SystemClock.elapsedRealtime()
        when (event.sensor.type) {
            Sensor.TYPE_GYROSCOPE -> if (liftEnabled) lift.pushGyroscope(
                event.values[0].toDouble(),
                event.values[1].toDouble(),
                event.values[2].toDouble(),
            )
            Sensor.TYPE_PRESSURE -> if (liftEnabled) lift.pushPressure(event.values[0].toDouble())
            Sensor.TYPE_ACCELEROMETER -> {
                detectShake(event.values, now)
                if (liftEnabled) when (
                    lift.pushAccelerometer(
                        event.values[0].toDouble(),
                        event.values[1].toDouble(),
                        event.values[2].toDouble(),
                        now,
                    )
                ) {
                    SmartLiftClassifier.Edge.LIFTED -> emit("motion", mapOf("moving" to true))
                    SmartLiftClassifier.Edge.REFIRE -> emit("motion", mapOf("moving" to true, "refire" to true))
                    SmartLiftClassifier.Edge.LANDED -> emit("motion", mapOf("moving" to false))
                    null -> Unit
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private fun detectShake(values: FloatArray, nowMs: Long) {
        if (nowMs < shakeCooldownUntil) return
        val g = sqrt(
            values[0].toDouble() * values[0] +
                values[1].toDouble() * values[1] +
                values[2].toDouble() * values[2],
        ) / 9.80665
        if (g < 2.2 || nowMs - lastShakePeakAt < 70L) return
        if (shakeWindowStartedAt == 0L || nowMs - shakeWindowStartedAt > 700L) {
            shakeWindowStartedAt = nowMs
            shakePeaks = 0
            strongestShakeG = 0.0
        }
        lastShakePeakAt = nowMs
        shakePeaks += 1
        strongestShakeG = max(strongestShakeG, g)
        if (shakePeaks < 3) return
        lift.noteShake(nowMs)
        if (shakeEnabled) {
            emit(
                "shake",
                mapOf("intensity" to ((strongestShakeG - 2.2) / 2.2).coerceIn(0.0, 1.0)),
            )
        }
        shakeCooldownUntil = nowMs + 1_800L
        shakeWindowStartedAt = 0L
        shakePeaks = 0
        strongestShakeG = 0.0
    }

    private fun reconcileSensors() {
        if (sensorsActive) {
            sensorManager.unregisterListener(this)
            sensorsActive = false
        }
        if (!started || !displayActive || (!liftEnabled && !shakeEnabled)) return

        // The accelerometer is required for both features. Gyro and barometer are
        // registered only for the adaptive height classifier, saving two continuous
        // sensors when the user keeps only Shake Tickle enabled.
        if (liftEnabled) {
            gyroscope?.let {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
            }
            pressure?.let {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
            }
        }
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
        sensorsActive = true
    }
}
