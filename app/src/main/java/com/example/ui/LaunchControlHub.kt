package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import kotlin.random.Random
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.LaunchLog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

// Theme Palette definitions for Tactical Control Room Dashboard (Professional Polish Theme)
val TechGreen = Color(0xFFB1D18A)       // #B1D18A - Sage/Moss Green
val CyberDark = Color(0xFF0F1113)       // #0F1113 - Body black-gray background
val PanelDark = Color(0xFF1A1C1E)       // #1A1C1E - Panel container background
val PanelAction = Color(0xFF1D2023)     // #1D2023 - Interactive component background
val PanelActionHover = Color(0xFF2A2D31)// #2A2D31 - Pressed component background
val GlowYellow = Color(0xFFFFA726)      // #FFA726 - Alert highlights/deviations
val DangerRed = Color(0xFF8C1D18)       // #8C1D18 - Dark red/crimson for launch trigger
val SoftRed = Color(0xFFFFB4AB)         // #FFB4AB - Light/soft warning accents
val IceBlue = Color(0xFFC4E7FF)         // #C4E7FF - Ice blue accents for gauges/Huds
val ArmedBg = Color(0xFF354930)         // #354930 - Safe armed badge background
val TextGray = Color(0xFF909094)        // #909094 - Secondary text gray
val TextLight = Color(0xFFE2E2E6)       // #E2E2E6 - Primary bright gray-white text
val AirBlue = Color(0xFFC4E7FF)         // Keep AirBlue referencing IceBlue for compatibility

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LaunchControlHub(viewModel: LaunchViewModel) {
    val activeScreen by viewModel.activeScreen.collectAsState()
    val logs by viewModel.launchLogs.collectAsState()
    val selectedIndex by viewModel.selectedTargetIndex.collectAsState()
    val alignmentHeading by viewModel.siloHeading.collectAsState()
    val targetHeading = viewModel.getDesiredHeading(selectedIndex)

    // Window insets & screen transitions state
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberDark)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        // Starry digital backdrop with top-right tactical radial coast glow and grid overlay
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // 1. Tactical radial background glow on top right
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF1B3524).copy(alpha = 0.35f), Color(0xFF0F1113)),
                    center = Offset(width, 0f),
                    radius = width * 1.6f
                )
            )

            // 2. CSS-inspired tactical grid lines
            val gridStep = 40.dp.toPx()
            val gridColor = Color(0xFF44474E).copy(alpha = 0.12f)
            
            var x = 0f
            while (x < width) {
                drawLine(
                    color = gridColor,
                    start = Offset(x, 0f),
                    end = Offset(x, height),
                    strokeWidth = 1f
                )
                x += gridStep
            }
            var y = 0f
            while (y < height) {
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = 1f
                )
                y += gridStep
            }
        }

        // Routing Screen switcher
        when (activeScreen) {
            SimulationScreen.PRE_LAUNCH -> PreLaunchScreen(viewModel)
            SimulationScreen.COUNTDOWN -> CountdownScreen(viewModel)
            SimulationScreen.CINEMATIC_FLIGHT -> FlightScreen(viewModel)
            SimulationScreen.REENTRY_GUIDANCE -> ReentryGuidanceScreen(viewModel)
            SimulationScreen.RESULTS -> ImpactResultsScreen(viewModel)
            SimulationScreen.HISTORY -> LaunchHistoryScreen(viewModel)
        }
    }
}

@Composable
fun PreLaunchScreen(viewModel: LaunchViewModel) {
    val logs by viewModel.launchLogs.collectAsState()
    val selectedIndex by viewModel.selectedTargetIndex.collectAsState()
    val currentTarget = viewModel.targets[selectedIndex]
    val fuelPressure by viewModel.fuelPressure.collectAsState()
    val pumpActive by viewModel.pumpStatus.collectAsState()
    val alignmentHeading by viewModel.siloHeading.collectAsState()
    val targetHeading = viewModel.getDesiredHeading(selectedIndex)
    val avionicsSynced by viewModel.avionicsSynced.collectAsState()
    val avionicsProgress by viewModel.avionicsSyncProgress.collectAsState()
    val safetyArmed by viewModel.safetyArmed.collectAsState()
    val isReady = viewModel.isReadyToLaunch()

    // Find the drawable images manually generated
    val context = LocalContext.current
    val coastalMapRes = R.drawable.img_coastal_map_1781605686478
    val icbmLauncherRes = R.drawable.img_icbm_launcher_1781605705089

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // App Header Unit
        ControlHubHeader(
            onNavigateToLogs = { viewModel.switchToHistory() },
            isHistoryAvailable = logs.isNotEmpty()
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Hero Row with Aerial Map view and ICBM Vehicle Info card
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Coastal Target Radar Preview Box
            Box(
                modifier = Modifier
                    .weight(1.1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(12.dp))
                    .border(BorderStroke(1.dp, TechGreen.copy(alpha = 0.25f)), RoundedCornerShape(12.dp))
                    .background(PanelDark)
            ) {
                Image(
                    painter = painterResource(id = coastalMapRes),
                    contentDescription = "Shoreline Satellite mapping background",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    alpha = 0.25f
                )

                // Coordinate Crosshair Drawing
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    // Radial radar sweep line
                    drawCircle(
                        color = TechGreen.copy(alpha = 0.15f),
                        center = Offset(w / 2f, h / 2f),
                        radius = h * 0.4f,
                        style = Stroke(2f)
                    )
                    drawCircle(
                        color = TechGreen.copy(alpha = 0.08f),
                        center = Offset(w / 2f, h / 2f),
                        radius = h * 0.2f,
                        style = Stroke(1f)
                    )
                    drawLine(
                        color = TechGreen.copy(alpha = 0.25f),
                        start = Offset(w / 2f, 0f),
                        end = Offset(w / 2f, h),
                        strokeWidth = 1f
                    )
                    drawLine(
                        color = TechGreen.copy(alpha = 0.25f),
                        start = Offset(0f, h / 2f),
                        end = Offset(w, h / 2f),
                        strokeWidth = 1f
                    )
                    // Target indicator dot
                    drawCircle(
                        color = SoftRed,
                        center = Offset(w / 2f + 15f, h / 2f - 25f),
                        radius = 8f
                    )
                }

                // Map header overlay
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CyberDark.copy(alpha = 0.75f))
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "TACTICAL CO-DOMAIN [MAP-COAST]",
                        color = TechGreen,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Live target coordinate overlay bottom
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(CyberDark.copy(alpha = 0.75f))
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "LOCK: GPS X:${currentTarget.x} Y:${currentTarget.y}",
                        color = TechGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Mobile ICBM erector transport trailer specs card
            Box(
                modifier = Modifier
                    .weight(0.9f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(12.dp))
                    .border(BorderStroke(1.dp, IceBlue.copy(alpha = 0.2f)), RoundedCornerShape(12.dp))
                    .background(PanelDark)
            ) {
                Image(
                    painter = painterResource(id = icbmLauncherRes),
                    contentDescription = "ICBM transport erector launcher vehicle profile",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    alpha = 0.3f
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "TEL VEHICLE #321",
                        color = IceBlue,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    Card(
                        colors = CardDefaults.cardColors(containerColor = CyberDark.copy(alpha = 0.85f)),
                        border = BorderStroke(1.dp, IceBlue.copy(alpha = 0.25f)),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(
                                text = "ICBM MODEL: DF-41",
                                color = IceBlue,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "FUEL: CRYOGENIC LQX",
                                color = TextGray,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Normal,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Target Matrix System selector
        Text(
            text = "TARGET SELECTION MATRIX",
            color = TechGreen,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(vertical = 4.dp)
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            viewModel.targets.forEachIndexed { idx, target ->
                val selected = selectedIndex == idx
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.selectTarget(idx) }
                        .testTag("target_card_$idx"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (selected) PanelAction else PanelDark
                    ),
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (selected) TechGreen else Color.White.copy(alpha = 0.05f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selected,
                            onClick = { viewModel.selectTarget(idx) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = TechGreen,
                                unselectedColor = TextGray.copy(alpha = 0.4f)
                            )
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = target.name,
                                    color = if (selected) TechGreen else TextLight,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Badge(
                                    containerColor = if (selected) ArmedBg else PanelAction,
                                    contentColor = if (selected) TechGreen else TextGray
                                ) {
                                    Text(
                                        text = target.targetType,
                                        fontSize = 9.sp,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = target.description,
                                color = TextGray,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Pre-launch flight subsystems configuration dials block
        Text(
            text = "LAUNCH CRITICAL PARAMETER CONTROLS",
            color = TechGreen,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(vertical = 4.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = PanelDark),
            border = BorderStroke(1.dp, IceBlue.copy(alpha = 0.12f))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {

                // Calibration Area 1: Propellant pump
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "PROPEL LQX TEMPERATURE REPRESSURIZER",
                            color = TextLight,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Pump cold oxygen lines. Target corridor: 82% - 98%",
                            color = TextGray,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Switch(
                        checked = pumpActive,
                        onCheckedChange = { viewModel.togglePump(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = TechGreen,
                            checkedTrackColor = ArmedBg,
                            uncheckedThumbColor = TextGray,
                            uncheckedTrackColor = PanelAction
                        )
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Slider tuning
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val inRange = fuelPressure in 82f..98f
                    Text(
                        text = "PRESSURE: ${Math.round(fuelPressure)}%",
                        color = if (inRange) TechGreen else GlowYellow,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.width(108.dp)
                    )

                    Slider(
                        value = fuelPressure,
                        onValueChange = { viewModel.adjustFuelManual(it) },
                        valueRange = 0f..100f,
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = if (inRange) TechGreen else GlowYellow,
                            activeTrackColor = if (inRange) TechGreen else GlowYellow,
                            inactiveTrackColor = PanelAction
                        )
                    )
                }

                HorizontalDivider(
                    color = Color.White.copy(alpha = 0.05f),
                    modifier = Modifier.padding(vertical = 10.dp)
                )

                // Calibration Area 2: Silo Heading Alignment rotation knob simulator
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "AZIMUTH HEADING ALIGNMENT",
                            color = TextLight,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "DESIRED: $targetHeading°",
                            color = IceBlue,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Text(
                        text = "Rotate launcher silo turntable to physical ignition alignment bearing.",
                        color = TextGray,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val headingDiff = Math.abs(alignmentHeading - targetHeading)
                        val headingAligned = headingDiff < 2.5f
                        Text(
                            text = "HEAD: ${Math.round(alignmentHeading)}°",
                            color = if (headingAligned) TechGreen else GlowYellow,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.width(108.dp)
                        )

                        Slider(
                            value = alignmentHeading,
                            onValueChange = { viewModel.adjustHeadingManual(it) },
                            valueRange = 0f..360f,
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(
                                thumbColor = if (headingAligned) TechGreen else GlowYellow,
                                activeTrackColor = if (headingAligned) TechGreen else GlowYellow,
                                inactiveTrackColor = PanelAction
                            )
                        )
                    }
                }

                HorizontalDivider(
                    color = Color.White.copy(alpha = 0.05f),
                    modifier = Modifier.padding(vertical = 10.dp)
                )

                // Avionics Core GPS Link
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "AVIONICS FLIGHT INERTIAL GUIDE LINK",
                            color = TextLight,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        if (avionicsSynced) {
                            Text(
                                text = "CORE LINK SECURED (100% SATELLITE)",
                                color = TechGreen,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        } else {
                            LinearProgressIndicator(
                                progress = avionicsProgress / 100f,
                                modifier = Modifier
                                    .padding(top = 6.dp)
                                    .fillMaxWidth(0.81f)
                                    .height(4.dp),
                                color = IceBlue,
                                trackColor = PanelAction
                            )
                        }
                    }

                    Button(
                        onClick = { viewModel.startAviationSync() },
                        enabled = !avionicsSynced,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = IceBlue,
                            disabledContainerColor = PanelAction.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = if (avionicsSynced) "LINKED" else "SYNC D-NAV",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = CyberDark,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Trigger safety arm status
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = PanelDark),
            border = BorderStroke(1.dp, if (safetyArmed) SoftRed else Color.White.copy(alpha = 0.05f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "LAUNCH TRIGGER PHYSICAL BREAK-SHEATH",
                        color = if (safetyArmed) SoftRed else TextLight,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Unlocks final ignition solenoid commands.",
                        color = TextGray,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Checkbox(
                    checked = safetyArmed,
                    onCheckedChange = { viewModel.toggleSafetyArmed(it) },
                    colors = CheckboxDefaults.colors(
                        checkedColor = SoftRed,
                        uncheckedColor = TextGray
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Big Red Launch Ignition Button
        Button(
            onClick = { viewModel.initiateLaunchSequence() },
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .testTag("launch_ignition_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isReady) DangerRed else PanelAction,
                contentColor = if (isReady) Color.White else TextGray,
                disabledContainerColor = PanelAction.copy(alpha = 0.5f),
                disabledContentColor = TextGray
            ),
            enabled = isReady,
            shape = RoundedCornerShape(12.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = if (isReady) 8.dp else 0.dp),
            border = if (isReady) BorderStroke(1.dp, SoftRed.copy(alpha = 0.4f)) else null
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = if (isReady) Color.White else TextGray
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isReady) "STAGE 1 IGNITION - INITIATE" else "SYSTEMS UNVERIFIED (CHECK DEVIATIONS)",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = if (isReady) Color.White else TextGray
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // System feedback console text for unverified parameters
        if (!isReady) {
            DeviationFeederBox(viewModel, targetHeading)
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
fun DeviationFeederBox(viewModel: LaunchViewModel, targetHeading: Float) {
    val fuelPressure by viewModel.fuelPressure.collectAsState()
    val alignmentHeading by viewModel.siloHeading.collectAsState()
    val avionicsSynced by viewModel.avionicsSynced.collectAsState()
    val safetyArmed by viewModel.safetyArmed.collectAsState()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.6f)),
        border = BorderStroke(1.dp, GlowYellow.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(
                text = "SYSTEM BLOCKED DEVIATIONS:",
                color = GlowYellow,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            if (fuelPressure !in 82f..98f) {
                Text(
                    text = "• Fuel Pressure deviates at $fuelPressure%. Safe threshold: 82% - 98%",
                    color = DangerRed,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
            if (Math.abs(alignmentHeading - targetHeading) >= 2.5f) {
                Text(
                    text = "• Launcher Bearing is $alignmentHeading°. Targeted optimal bearing is $targetHeading° (deviation exceeds 2.5° max tolerance)",
                    color = DangerRed,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
            if (!avionicsSynced) {
                Text(
                    text = "• Flight computer guidance matrix unresolved. Tap SYNC D-NAV",
                    color = DangerRed,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
            if (!safetyArmed) {
                Text(
                    text = "• Launch physical key safety sleeve remains locked.",
                    color = DangerRed,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
fun ControlHubHeader(onNavigateToLogs: () -> Unit, isHistoryAvailable: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(PanelDark)
            .padding(10.dp)
            .border(1.dp, TechGreen.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "STRATEGIC MISSILE SIMULATOR",
                color = TechGreen,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.titleMedium.copy(
                    shadow = Shadow(color = TechGreen, blurRadius = 3f)
                )
            )
            Text(
                text = "SECURE COGNITIVE SIM CORRIDOR - MIL-ID# 4053",
                color = Color.LightGray,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        IconButton(
            onClick = onNavigateToLogs,
            modifier = Modifier.testTag("history_button")
        ) {
            Icon(
                imageVector = Icons.Default.List,
                contentDescription = "Mission logs database",
                tint = if (isHistoryAvailable) TechGreen else Color.LightGray.copy(alpha = 0.3f)
            )
        }
    }
}

// Global color helper for disabled buttons in dark design
val DarkColorTokens_TechMuted = Color(0xFF1E2824)

@Composable
fun CountdownScreen(viewModel: LaunchViewModel) {
    val count by viewModel.countdown.collectAsState()
    val targetIndex by viewModel.selectedTargetIndex.collectAsState()
    val target = viewModel.targets[targetIndex]

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberDark),
        contentAlignment = Alignment.Center
    ) {
        // Red dynamic scanning sweep lines
        val pulseScale by animateFloatAsState(
            targetValue = if (count % 2 == 0) 1.2f else 0.8f,
            animationSpec = tween(500, easing = LinearEasing), label = ""
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = SoftRed.copy(alpha = 0.08f * pulseScale),
                radius = size.minDimension * 0.4f * pulseScale
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "BOOSTER STAGE IGNITING",
                color = SoftRed,
                fontSize = 14.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = count.toString(),
                color = SoftRed,
                fontSize = 110.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.displayLarge.copy(
                    shadow = Shadow(color = DangerRed, blurRadius = 25f)
                )
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "TARGET: ${target.name.uppercase()}",
                color = TextLight,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(TechGreen)
                )
                Text(
                    text = "AZIMUTH COORD MATRIX: LOCKED",
                    color = TechGreen,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun FlightScreen(viewModel: LaunchViewModel) {
    val alt by viewModel.flightAltitudeKm.collectAsState()
    val vel by viewModel.flightVelocityMach.collectAsState()
    val dist by viewModel.flightDistanceKm.collectAsState()
    val stage by viewModel.flightStageText.collectAsState()
    val logs by viewModel.telemetryLogLines.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp)
    ) {
        // Upper telemetry flight banner status
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = PanelDark),
            border = BorderStroke(1.dp, IceBlue.copy(alpha = 0.25f))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "ACTIVE TELEMETRY BOOST ASCENT PHASE",
                    color = IceBlue,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Stage Profile: $stage",
                    color = TextLight,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // High altitude rocket animation visualizer
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.2f)
                .clip(RoundedCornerShape(12.dp))
                .background(CyberDark)
                .border(BorderStroke(1.dp, TechGreen.copy(alpha = 0.2f)), RoundedCornerShape(12.dp))
        ) {
            // Infinite rocket fire stream drawing
            val infiniteTransition = rememberInfiniteTransition(label = "")
            val offsetFire by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 40f,
                animationSpec = infiniteRepeatable(
                    animation = tween(200, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ), label = ""
            )

            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx = size.width / 2f
                val cy = size.height / 2f - (alt * 0.8f).coerceAtMost(size.height * 0.35f)

                // Background space gradient
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(CyberDark, PanelDark, IceBlue.copy(alpha = 0.15f))
                    )
                )

                // Curved Earth outline bottom
                drawArc(
                    color = IceBlue.copy(alpha = 0.25f),
                    startAngle = 10f,
                    sweepAngle = 160f,
                    useCenter = false,
                    size = Size(size.width * 2f, size.height * 0.4f),
                    topLeft = Offset(-size.width * 0.5f, size.height * 0.82f),
                    style = Stroke(3f)
                )

                // Atmospheric color haze representing layer fade
                drawRect(
                    color = IceBlue.copy(alpha = ((140f - alt) / 140f * 0.15f).coerceAtLeast(0f)),
                    size = size
                )

                // Draw simple flying schematic missile icon
                // Missile white tip
                drawRect(
                    color = Color.White,
                    topLeft = Offset(cx - 8f, cy - 30f),
                    size = Size(16f, 50f)
                )
                // Missile engine booster tail
                drawRect(
                    color = DangerRed,
                    topLeft = Offset(cx - 10f, cy + 20f),
                    size = Size(20f, 10f)
                )
                // Flame tail
                drawArc(
                    color = GlowYellow,
                    startAngle = 45f,
                    sweepAngle = 90f,
                    useCenter = true,
                    topLeft = Offset(cx - 15f, cy + 30f),
                    size = Size(30f, 30f + offsetFire)
                )
            }

            // High digital status indicators overlay left
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(CyberDark.copy(alpha = 0.85f))
                    .border(BorderStroke(1.dp, IceBlue.copy(alpha = 0.15f)), RoundedCornerShape(8.dp))
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(modifier = Modifier.size(5.dp).clip(RoundedCornerShape(2.5.dp)).background(TechGreen))
                    Text(
                        text = "ALTITUDE: ${Math.round(alt)} km",
                        color = TechGreen,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(modifier = Modifier.size(5.dp).clip(RoundedCornerShape(2.5.dp)).background(IceBlue))
                    Text(
                        text = "VELOCITY: Mach ${Math.round(vel * 10f) / 10f}",
                        color = IceBlue,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(modifier = Modifier.size(5.dp).clip(RoundedCornerShape(2.5.dp)).background(TextLight))
                    Text(
                        text = "DISTANCE: ${Math.round(dist)} km",
                        color = TextLight,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // System telemetry feed logs
        Text(
            text = "LIVE ROCKET BOOSTER COGNITIVE TELEMETRY:",
            color = TechGreen,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.8f)
                .clip(RoundedCornerShape(6.dp))
                .background(PanelDark)
                .border(1.dp, TechGreen.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                .padding(8.dp)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                reverseLayout = true
            ) {
                items(logs.reversed()) { logLine ->
                    Text(
                        text = logLine,
                        color = TechGreen,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(vertical = 1.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ReentryGuidanceScreen(viewModel: LaunchViewModel) {
    val focusX by viewModel.guidanceX.collectAsState()
    val focusY by viewModel.guidanceY.collectAsState()
    val altMeters by viewModel.flightAltitudeKm.collectAsState()
    val velocityMach by viewModel.flightVelocityMach.collectAsState()
    val targetIndex by viewModel.selectedTargetIndex.collectAsState()
    val target = viewModel.targets[targetIndex]
    val devOffset by viewModel.dragOffsetDistance.collectAsState()
    val fuelRemaining by viewModel.guidanceFuel.collectAsState()
    val activeThrustDir by viewModel.activeThrustDirection.collectAsState()

    val coastalMapRes = R.drawable.img_coastal_map_1781605686478

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        // Guidance control deck header
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = PanelDark),
            border = BorderStroke(1.dp, SoftRed.copy(alpha = 0.35f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "⚠️ WARHEAD ATMOSPHERIC RE-ENTRY INTERACTION STATUS",
                        color = SoftRed,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "MANUAL THRUST ALIGNMENT REQUIRED",
                        color = TextLight,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = CyberDark),
                    border = BorderStroke(1.dp, if (fuelRemaining > 20f) TechGreen.copy(alpha = 0.3f) else SoftRed.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "FUEL: ${Math.round(fuelRemaining)}%",
                        color = if (fuelRemaining > 20f) TechGreen else SoftRed,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Intercept radar simulation view with shoreline map
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .border(BorderStroke(1.dp, SoftRed.copy(alpha = 0.25f)), RoundedCornerShape(12.dp))
                .background(CyberDark)
        ) {
            // Shoreline map background
            Image(
                painter = painterResource(id = coastalMapRes),
                contentDescription = "Target sea coastal map view for steering",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alpha = 0.4f
            )

            // Let's overlay target location and current landing projection location
            Canvas(modifier = Modifier.fillMaxSize()) {
                val scaleX = size.width / 1000f
                val scaleY = size.height / 1000f

                val screenTargetX = target.x * scaleX
                val screenTargetY = target.y * scaleY

                val screenProjX = focusX * scaleX
                val screenProjY = focusY * scaleY

                // 1. Draw Target Site reticle (Red Outer target circle)
                drawCircle(
                    color = SoftRed,
                    center = Offset(screenTargetX, screenTargetY),
                    radius = 28f,
                    style = Stroke(3f)
                )
                // Small inner red bullseye core
                drawCircle(
                    color = SoftRed,
                    center = Offset(screenTargetX, screenTargetY),
                    radius = 6f
                )

                // Label for target
                // We'll draw laser tracking lines linking target to current point
                drawLine(
                    color = TechGreen.copy(alpha = 0.45f),
                    start = Offset(screenTargetX, screenTargetY),
                    end = Offset(screenProjX, screenProjY),
                    strokeWidth = 2f,
                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                )

                // 2. Draw live projection warhead icon (Flashing crosshair)
                // Main reticle
                drawCircle(
                    color = TechGreen,
                    center = Offset(screenProjX, screenProjY),
                    radius = 18f,
                    style = Stroke(2f)
                )
                // Crosshairs
                drawLine(
                    color = TechGreen,
                    start = Offset(screenProjX - 25f, screenProjY),
                    end = Offset(screenProjX + 25f, screenProjY),
                    strokeWidth = 2f
                )
                drawLine(
                    color = TechGreen,
                    start = Offset(screenProjX, screenProjY - 25f),
                    end = Offset(screenProjX, screenProjY + 25f),
                    strokeWidth = 2f
                )
            }

            // Compass directions indicator overlays
            Text("NORTH (G)", color = Color.White.copy(alpha = 0.35f), fontSize = 9.sp, modifier = Modifier.align(Alignment.TopCenter).padding(top = 4.dp), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            Text("SOUTH (V)", color = Color.White.copy(alpha = 0.35f), fontSize = 9.sp, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 4.dp), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            Text("WEST (F)", color = Color.White.copy(alpha = 0.35f), fontSize = 9.sp, modifier = Modifier.align(Alignment.CenterStart).padding(start = 4.dp), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            Text("EAST (H)", color = Color.White.copy(alpha = 0.35f), fontSize = 9.sp, modifier = Modifier.align(Alignment.CenterEnd).padding(end = 4.dp), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)

            // Top HUD Overlay specs
            Card(
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = CyberDark.copy(alpha = 0.85f)),
                border = BorderStroke(1.dp, IceBlue.copy(alpha = 0.15f)),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
            ) {
                Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "ALTITUDE: ${Math.round(altMeters)} m",
                        color = SoftRed,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "SPEED: Mach ${Math.round(velocityMach * 10f) / 10f}",
                        color = TextLight,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "DEVIATION: ${Math.round(devOffset * 0.95f)} m",
                        color = if (devOffset < 50f) TechGreen else GlowYellow,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // D-Pad Manual thruster controls
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = PanelDark),
            border = BorderStroke(1.dp, TechGreen.copy(alpha = 0.15f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "REENTRY ORIENTATION THRUST PADS",
                    color = TechGreen,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // D-PAD GRID layout
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // North
                    Button(
                        onClick = { viewModel.applyThruster("N") },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (activeThrustDir == "N") GlowYellow else IceBlue,
                            contentColor = CyberDark
                        ),
                        modifier = Modifier
                            .size(width = 90.dp, height = 44.dp)
                            .testTag("thrust_up"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.KeyboardArrowUp, contentDescription = "Thrust Up", tint = CyberDark)
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(22.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // West
                        Button(
                            onClick = { viewModel.applyThruster("W") },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (activeThrustDir == "W") GlowYellow else IceBlue,
                                contentColor = CyberDark
                            ),
                            modifier = Modifier
                                .size(width = 90.dp, height = 44.dp)
                                .testTag("thrust_left"),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.KeyboardArrowLeft, contentDescription = "Thrust Left", tint = CyberDark)
                        }

                        // Center indicator bubble
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(CyberDark)
                                .border(1.dp, TechGreen.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(if (activeThrustDir != null) SoftRed else TechGreen)
                            )
                        }

                        // East
                        Button(
                            onClick = { viewModel.applyThruster("E") },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (activeThrustDir == "E") GlowYellow else IceBlue,
                                contentColor = CyberDark
                            ),
                            modifier = Modifier
                                .size(width = 90.dp, height = 44.dp)
                                .testTag("thrust_right"),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.KeyboardArrowRight, contentDescription = "Thrust Right", tint = CyberDark)
                        }
                    }

                    // South
                    Button(
                        onClick = { viewModel.applyThruster("S") },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (activeThrustDir == "S") GlowYellow else IceBlue,
                            contentColor = CyberDark
                        ),
                        modifier = Modifier
                            .size(width = 90.dp, height = 44.dp)
                            .testTag("thrust_down"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.KeyboardArrowDown, contentDescription = "Thrust Down", tint = CyberDark)
                    }
                }
            }
        }
    }
}

@Composable
fun ImpactResultsScreen(viewModel: LaunchViewModel) {
    val report by viewModel.impactReport.collectAsState()
    val shake by viewModel.seismicIntensity.collectAsState()

    // Shake offset modifier based on the seismic intensity flow
    val shakeOffsetX = (Random.nextFloat() * shake - (shake / 2f)).dp
    val shakeOffsetY = (Random.nextFloat() * shake - (shake / 2f)).dp

    Box(
        modifier = Modifier
            .fillMaxSize()
            .offset(x = shakeOffsetX, y = shakeOffsetY)
            .background(CyberDark),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(PanelDark)
                .border(BorderStroke(1.5.dp, if (report?.status == "DIRECT HIT") TechGreen else GlowYellow), RoundedCornerShape(12.dp))
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = if (report?.status == "DIRECT HIT") TechGreen else GlowYellow,
                modifier = Modifier.size(56.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "WARHEAD IMPACT INCIDENT RECORDED",
                color = SoftRed,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 0.75.sp
            )

            Spacer(modifier = Modifier.height(15.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = CyberDark.copy(alpha = 0.6f)),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("TARGET ZONE SITE:", color = TextGray, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        Text(report?.targetName ?: "Unknown", color = TextLight, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("ACCURACY DEPTH DELTA:", color = TextGray, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        Text("${report?.distanceErrorMeters} meters", color = TechGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("IMPACT RATING RATING:", color = TextGray, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        Text(report?.status ?: "Unknown", color = if (report?.status == "DIRECT HIT") TechGreen else GlowYellow, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("TERMINAL SPEEDS AT BURN:", color = TextGray, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        Text("Mach ${report?.missileVelocityMach}", color = TextLight, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("COGNITIVE SEISMIC LEVEL:", color = TextGray, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        Text("Peak: 38.2 Richter (Sim)", color = SoftRed, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { viewModel.switchToHistory() },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("view_logs_results_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = IceBlue, contentColor = CyberDark),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("ARCHIVE ROOM", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = CyberDark, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { viewModel.reset() },
                    modifier = Modifier
                        .weight(1.1f)
                        .testTag("relaunch_sim_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = TechGreen, contentColor = CyberDark),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("RE-ARM MISSILE", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = CyberDark, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun LaunchHistoryScreen(viewModel: LaunchViewModel) {
    val logs by viewModel.launchLogs.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        // Log heading banner
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = { viewModel.exitHistory() },
                colors = ButtonDefaults.buttonColors(containerColor = PanelAction, contentColor = TextLight),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Return", tint = TextLight)
                Spacer(modifier = Modifier.width(4.dp))
                Text("BACK", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = TextLight, fontWeight = FontWeight.Bold)
            }

            Text(
                text = "TELEMETRY HISTORIC FILES",
                color = TechGreen,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )

            IconButton(
                onClick = { viewModel.clearLogHistory() },
                modifier = Modifier.testTag("clear_history_button")
            ) {
                Icon(imageVector = Icons.Default.Delete, contentDescription = "Clear launch logs database", tint = SoftRed)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (logs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(PanelDark)
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "NO COMPLETED ICBM SIMULATION RUNS FOUND IN REGISTER.",
                    color = TextGray,
                    textAlign = TextAlign.Center,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(logs) { log ->
                    LaunchHistoryItem(log)
                }
            }
        }
    }
}

@Composable
fun LaunchHistoryItem(log: LaunchLog) {
    val formatter = remember { SimpleDateFormat("HH:mm:ss (yyyy-MM-dd)", Locale.US) }
    val timeLabel = formatter.format(Date(log.timestamp))

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = PanelDark),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = log.targetName.uppercase(),
                    color = TextLight,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )

                Badge(
                    containerColor = if (log.status == "DIRECT HIT") ArmedBg else PanelAction,
                    contentColor = if (log.status == "DIRECT HIT") TechGreen else GlowYellow
                ) {
                    Text(
                        text = log.status,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Target Offset Variance: ${log.distanceErrorMeters} meters",
                color = IceBlue,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "Impact Velocity: Mach ${log.missileVelocityMach} | Apogee: 140km",
                color = TextGray,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "Timestamp: $timeLabel",
                color = TextGray.copy(alpha = 0.7f),
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
