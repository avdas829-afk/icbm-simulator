package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.LaunchDatabase
import com.example.data.LaunchLog
import com.example.data.LaunchRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.sqrt
import kotlin.random.Random

enum class SimulationScreen {
    PRE_LAUNCH,
    COUNTDOWN,
    CINEMATIC_FLIGHT,
    REENTRY_GUIDANCE,
    RESULTS,
    HISTORY
}

data class TargetSite(
    val name: String,
    val description: String,
    val x: Float, // coordinates in 0..1000 coordinate space
    val y: Float,
    val targetType: String // "Coastal Outpost", "Naval Fleet Grid", "Deep Sea Trench", "Shore Radar"
)

class LaunchViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: LaunchRepository

    init {
        val database = LaunchDatabase.getDatabase(application)
        repository = LaunchRepository(database.launchLogDao())
    }

    val launchLogs: StateFlow<List<LaunchLog>> = repository.allLogs
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Preset targets located on coastal/sea areas of the img_coastal_map
    val targets = listOf(
        TargetSite("Beach Fortify Outpost X4", "Shore bunker and military supply silos", 420f, 620f, "Shore Radar"),
        TargetSite("Submarine Base Carrier Grid", "Submerged mobile aircraft carrier terminal", 740f, 320f, "Naval Fleet Grid"),
        TargetSite("Deep Sea Reconnaissance Reef", "Scientific radar intercept ocean platform", 880f, 750f, "Deep Sea Trench"),
        TargetSite("Coast Artillery Battery Delta", "High-intensity shoreline defensive array", 550f, 480f, "Coastal Outpost")
    )

    private val _activeScreen = MutableStateFlow(SimulationScreen.PRE_LAUNCH)
    val activeScreen: StateFlow<SimulationScreen> = _activeScreen.asStateFlow()

    // Checklist statuses
    val _selectedTargetIndex = MutableStateFlow(0)
    val selectedTargetIndex: StateFlow<Int> = _selectedTargetIndex.asStateFlow()

    val _fuelPressure = MutableStateFlow(0f) // Desired range: 85 - 95
    val fuelPressure: StateFlow<Float> = _fuelPressure.asStateFlow()

    val _pumpStatus = MutableStateFlow(false) // Toggle to pump active
    val pumpStatus: StateFlow<Boolean> = _pumpStatus.asStateFlow()

    val _siloHeading = MutableStateFlow(0f) // Desired: heading matches selected target azimuth
    val siloHeading: StateFlow<Float> = _siloHeading.asStateFlow()

    val _avionicsSynced = MutableStateFlow(false)
    val avionicsSynced: StateFlow<Boolean> = _avionicsSynced.asStateFlow()

    val _avionicsSyncProgress = MutableStateFlow(0f)
    val avionicsSyncProgress: StateFlow<Float> = _avionicsSyncProgress.asStateFlow()

    val _safetyArmed = MutableStateFlow(false)
    val safetyArmed: StateFlow<Boolean> = _safetyArmed.asStateFlow()

    val _countdown = MutableStateFlow(5)
    val countdown: StateFlow<Int> = _countdown.asStateFlow()

    // Cinematic & Telemetry tracking
    val _flightAltitudeKm = MutableStateFlow(0f)
    val flightAltitudeKm: StateFlow<Float> = _flightAltitudeKm.asStateFlow()

    val _flightVelocityMach = MutableStateFlow(0f)
    val flightVelocityMach: StateFlow<Float> = _flightVelocityMach.asStateFlow()

    val _flightDistanceKm = MutableStateFlow(0f)
    val flightDistanceKm: StateFlow<Float> = _flightDistanceKm.asStateFlow()

    val _flightStageText = MutableStateFlow("Pre-ignition checklist verified")
    val flightStageText: StateFlow<String> = _flightStageText.asStateFlow()

    // Guidance mode tracking
    val _guidanceFuel = MutableStateFlow(100f)
    val guidanceFuel: StateFlow<Float> = _guidanceFuel.asStateFlow()

    val _guidanceX = MutableStateFlow(500f) // Current tracking warhead location on map
    val guidanceX: StateFlow<Float> = _guidanceX.asStateFlow()

    val _guidanceY = MutableStateFlow(500f)
    val guidanceY: StateFlow<Float> = _guidanceY.asStateFlow()

    val _dragOffsetDistance = MutableStateFlow(0f) // Live delta to target
    val dragOffsetDistance: StateFlow<Float> = _dragOffsetDistance.asStateFlow()

    val _activeThrustDirection = MutableStateFlow<String?>(null) // "N", "S", "W", "E" or null
    val activeThrustDirection: StateFlow<String?> = _activeThrustDirection.asStateFlow()

    val _impactReport = MutableStateFlow<LaunchLog?>(null)
    val impactReport: StateFlow<LaunchLog?> = _impactReport.asStateFlow()

    val _seismicIntensity = MutableStateFlow(0f) // Used for screen shake detonation effect
    val seismicIntensity: StateFlow<Float> = _seismicIntensity.asStateFlow()

    val _telemetryLogLines = MutableStateFlow(listOf<String>())
    val telemetryLogLines: StateFlow<List<String>> = _telemetryLogLines.asStateFlow()

    private var simulationJob: Job? = null
    private var pumpJob: Job? = null

    // Target azimuth helper
    fun getDesiredHeading(index: Int): Float {
        val target = targets[index]
        // Pseudo azimuth calculation from silo coordinates (centered at 500, 500)
        val dx = target.x - 150f // launch silo base is around (150, 800) in coordinate space
        val dy = target.y - 800f
        var heading = Math.toDegrees(Math.atan2(dy.toDouble(), dx.toDouble())).toFloat()
        if (heading < 0) heading += 360f
        return Math.round(heading * 10f) / 10f
    }

    fun selectTarget(index: Int) {
        if (_activeScreen.value == SimulationScreen.PRE_LAUNCH) {
            _selectedTargetIndex.value = index
            addTelemetryLog("Silo alignment retargeted: ${targets[index].name}. Desired azimuth: ${getDesiredHeading(index)}°")
        }
    }

    fun startAviationSync() {
        if (_avionicsSynced.value || _activeScreen.value != SimulationScreen.PRE_LAUNCH) return
        viewModelScope.launch {
            _avionicsSyncProgress.value = 0f
            addTelemetryLog("Avionics core synchronization initialized...")
            for (step in 1..20) {
                delay(120)
                _avionicsSyncProgress.value = step * 5f
                if (step == 5) addTelemetryLog("AVIONICS: Establishing links to orbital guide arrays...")
                if (step == 10) addTelemetryLog("AVIONICS: Local coordinates telemetry matrices locked.")
                if (step == 15) addTelemetryLog("AVIONICS: Inertial navigation system calibration completed.")
            }
            _avionicsSynced.value = true
            addTelemetryLog("Avionics system status: FULLY COHERENT / SYNC COMPLETE.")
        }
    }

    fun togglePump(active: Boolean) {
        _pumpStatus.value = active
        if (active) {
            addTelemetryLog("Initiating cryogenic propellant transfer pumps...")
            pumpJob?.cancel()
            pumpJob = viewModelScope.launch {
                while (_pumpStatus.value) {
                    delay(80)
                    val current = _fuelPressure.value
                    if (current < 100f) {
                        _fuelPressure.value = (current + 1.2f).coerceAtMost(100f)
                    }
                    if (_fuelPressure.value >= 90f && _fuelPressure.value <= 95f) {
                        // Ideal indicator
                    }
                }
            }
        } else {
            addTelemetryLog("Cryogenic propellant sequence paused.")
            pumpJob?.cancel()
        }
    }

    fun adjustFuelManual(value: Float) {
        _fuelPressure.value = value
    }

    fun adjustHeadingManual(value: Float) {
        _siloHeading.value = value
    }

    fun toggleSafetyArmed(armed: Boolean) {
        _safetyArmed.value = armed
        if (armed) {
            addTelemetryLog("⚠️ CRITICAL ALERT: SILO SECURITY SHEATH BREACHED. TRIGGER ARMED!")
        } else {
            addTelemetryLog("Silo firing trigger safety engaged.")
        }
    }

    fun isReadyToLaunch(): Boolean {
        val targetIndex = _selectedTargetIndex.value
        val desiredHeading = getDesiredHeading(targetIndex)
        val azimuthDiff = Math.abs(_siloHeading.value - desiredHeading)
        val fuelInSpec = _fuelPressure.value in 82f..98f
        val guidanceSynced = _avionicsSynced.value
        val armed = _safetyArmed.value
        return fuelInSpec && azimuthDiff < 2.5f && guidanceSynced && armed
    }

    fun initiateLaunchSequence() {
        if (!isReadyToLaunch()) return
        _activeScreen.value = SimulationScreen.COUNTDOWN
        _countdown.value = 5
        addTelemetryLog("💥 EMERGENCY FIRESIGNAL RECIEVED. COUNTDOWN SEQUENCE COMMENCED!")
        viewModelScope.launch {
            while (_countdown.value > 0) {
                delay(1000)
                _countdown.value -= 1
                addTelemetryLog("T-minus ${_countdown.value} seconds...")
            }
            startAscentSim()
        }
    }

    private fun startAscentSim() {
        _activeScreen.value = SimulationScreen.CINEMATIC_FLIGHT
        _telemetryLogLines.value = emptyList()
        addTelemetryLog("🚀 LIFTOFF! ICBM booster ignition success. Thrust vectoring: ACTIVE!")

        simulationJob = viewModelScope.launch {
            var altitude = 0f
            var velocity = 0f
            var distance = 0f

            // Ascent Phase: 0 to 140km apogee
            for (step in 1..60) {
                delay(100)
                altitude = (step * 2.33f).coerceAtMost(140f)
                velocity = (step * 0.3f).coerceAtMost(18.0f) // Builds up to Mach 18
                distance = step * 6.5f

                _flightAltitudeKm.value = altitude
                _flightVelocityMach.value = velocity
                _flightDistanceKm.value = distance

                when (step) {
                    5 -> {
                        _flightStageText.value = "Ascending through Dense Troposphere..."
                        addTelemetryLog("BOOSTER: Mach 1 exceeded. Aerodynamic stress parameters within limits.")
                    }
                    15 -> {
                        _flightStageText.value = "MAX-Q (Maximum Aerodynamic Drag)"
                        addTelemetryLog("BOOSTER: Reaching dynamic stress apex. Nozzle servo deflection active.")
                    }
                    25 -> {
                        _flightStageText.value = "Sub-Orbital Booster Stage Sep"
                        addTelemetryLog("BOOSTER: Core Stage 1 separation confirmed. Stage 2 vacuum ignition...")
                    }
                    38 -> {
                        _flightStageText.value = "Atmospheric Exit - Thermosphere"
                        addTelemetryLog("GUIDANCE: GPS and inertial satellite trackers updated. Apex lock established.")
                    }
                    50 -> {
                        _flightStageText.value = "Apogee Peak Trajectory Turn"
                        addTelemetryLog("TELEMETRY: Ballistic summit achieved. Re-entry shroud jettisoned.")
                    }
                }
            }

            // High apogee achieved, start terminal re-entry manual guidance!
            startTerminalReentry()
        }
    }

    private fun startTerminalReentry() {
        _activeScreen.value = SimulationScreen.REENTRY_GUIDANCE
        val target = targets[_selectedTargetIndex.value]

        // Seed initial warhead location somewhere within offset radius of target, representing atmospheric trajectory dispersion
        val initialDispersionRadius = 150f
        val angle = Random.nextDouble(0.0, Math.PI * 2)
        val r = Random.nextDouble(100.0, initialDispersionRadius.toDouble()).toFloat()
        val initialX = target.x + r * Math.cos(angle).toFloat()
        val initialY = target.y + r * Math.sin(angle).toFloat()

        _guidanceX.value = initialX
        _guidanceY.value = initialY
        _guidanceFuel.value = 100f
        _flightVelocityMach.value = 14.5f // Mach speed at re-entry
        _flightAltitudeKm.value = 45000f // 45km altitude in meters for high sensitivity!

        addTelemetryLog("🚨 EXTREME ALERT: Entering dense atmospheric layers. Manual thrusters active!")
        addTelemetryLog("MISSILE HUD: Align flashing red crosshair with matching TARGET epicenter.")

        simulationJob?.cancel()
        simulationJob = viewModelScope.launch {
            // Re-entry guidance game loop (Altitude goes from 45,000m down to 0)
            while (_flightAltitudeKm.value > 0f) {
                delay(30) // ~33 FPS

                // Descent speed varies during terminal air friction, speed declines from Mach 15 to Mach 8 near impact
                val currentAlt = _flightAltitudeKm.value
                val descStep = 180f + (currentAlt / 45000f) * 150f // descends faster in thinner atmosphere
                _flightAltitudeKm.value = (currentAlt - descStep).coerceAtLeast(0f)

                // Velocity decelerates due to heavy air density
                val currentMach = _flightVelocityMach.value
                if (currentMach > 7.8f) {
                    _flightVelocityMach.value = currentMach - 0.005f
                }

                // Wind drift adds custom continuous random turbulences!
                val windDriftX = (Math.sin(System.currentTimeMillis() / 400.0).toFloat() * 1.5f) + (Random.nextFloat() * 1.0f - 0.5f)
                val windDriftY = (Math.cos(System.currentTimeMillis() / 450.0).toFloat() * 1.3f) + (Random.nextFloat() * 1.0f - 0.5f)

                // User burns thrusters which change trajectory coordinates
                _guidanceX.value = (_guidanceX.value + windDriftX).coerceIn(0f, 1000f)
                _guidanceY.value = (_guidanceY.value + windDriftY).coerceIn(0f, 1000f)

                // Live target offset calculation
                val dx = _guidanceX.value - target.x
                val dy = _guidanceY.value - target.y
                _dragOffsetDistance.value = sqrt(dx * dx + dy * dy)
            }

            // Reached altitude 0! Impact!
            triggerImpact()
        }
    }

    fun applyThruster(direction: String) {
        if (_activeScreen.value != SimulationScreen.REENTRY_GUIDANCE || _guidanceFuel.value <= 0f) return
        _activeThrustDirection.value = direction
        val thrustPower = 7.5f // Correction speed per tap
        val fuelConsumption = 1.8f

        _guidanceFuel.value = (_guidanceFuel.value - fuelConsumption).coerceAtLeast(0f)

        when (direction) {
            "N" -> _guidanceY.value = (_guidanceY.value - thrustPower).coerceIn(0f, 1000f)
            "S" -> _guidanceY.value = (_guidanceY.value + thrustPower).coerceIn(0f, 1000f)
            "W" -> _guidanceX.value = (_guidanceX.value - thrustPower).coerceIn(0f, 1000f)
            "E" -> _guidanceX.value = (_guidanceX.value + thrustPower).coerceIn(0f, 1000f)
        }

        viewModelScope.launch {
            delay(100)
            if (_activeThrustDirection.value == direction) {
                _activeThrustDirection.value = null
            }
        }
    }

    private suspend fun triggerImpact() {
        _activeScreen.value = SimulationScreen.RESULTS
        val target = targets[_selectedTargetIndex.value]

        // Shake screen layout
        viewModelScope.launch {
            _seismicIntensity.value = 35f
            addTelemetryLog("💥 DETONATION: Seismic shock wave detected. Thermal flare peak.")
            for (i in 1..15) {
                delay(60)
                _seismicIntensity.value = (35f - (i * 2.3f)).coerceAtLeast(0f)
            }
        }

        // Calculate landing inaccuracies
        // Screen space coordinate scale: let's translate coordinate delta to a realistic military accuracy metric in meters.
        // Let's assume 1 unit on coordinate scale = 0.8 meters, giving realistic 0 to 200m circular error probable.
        val errorMeters = Math.round(_dragOffsetDistance.value * 0.95f * 10f) / 10f

        val ratingStatus = when {
            errorMeters <= 35f -> "DIRECT HIT"
            errorMeters <= 120f -> "NEAR HIT"
            else -> "MISSED TARGET"
        }

        // Create log item
        val newLog = LaunchLog(
            targetName = target.name,
            targetX = target.x,
            targetY = target.y,
            hitX = _guidanceX.value,
            hitY = _guidanceY.value,
            distanceErrorMeters = errorMeters,
            status = ratingStatus,
            missileVelocityMach = _flightVelocityMach.value,
            apogeeAltitudeKm = 140f
        )

        _impactReport.value = newLog
        repository.insert(newLog)
        addTelemetryLog("REPORT: Impact telemetry recorded in archive logs. Accuracy: ${errorMeters} meters.")
    }

    fun reset() {
        simulationJob?.cancel()
        pumpJob?.cancel()
        _activeScreen.value = SimulationScreen.PRE_LAUNCH
        _fuelPressure.value = 0f
        _pumpStatus.value = false
        _siloHeading.value = 0f
        _avionicsSynced.value = false
        _avionicsSyncProgress.value = 0f
        _safetyArmed.value = false
        _impactReport.value = null
        _dragOffsetDistance.value = 0f
        _telemetryLogLines.value = emptyList()
        addTelemetryLog("Control Room reset. Silo cooling vents active.")
    }

    fun switchToHistory() {
        _activeScreen.value = SimulationScreen.HISTORY
    }

    fun exitHistory() {
        _activeScreen.value = SimulationScreen.PRE_LAUNCH
    }

    fun clearLogHistory() {
        viewModelScope.launch {
            repository.clearAll()
            addTelemetryLog("Military telemetry log database purged successfully.")
        }
    }

    fun addTelemetryLog(line: String) {
        val current = _telemetryLogLines.value.toMutableList()
        current.add("[${System.currentTimeMillis() % 100000}] $line")
        if (current.size > 22) current.removeAt(0)
        _telemetryLogLines.value = current
    }

    override fun onCleared() {
        super.onCleared()
        simulationJob?.cancel()
        pumpJob?.cancel()
    }
}

class LaunchViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LaunchViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LaunchViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
