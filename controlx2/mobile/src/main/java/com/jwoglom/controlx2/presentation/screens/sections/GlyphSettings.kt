@file:OptIn(ExperimentalMaterial3Api::class)

package com.jwoglom.controlx2.presentation.screens.sections

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.jwoglom.controlx2.presentation.components.HeaderLine
import it.mattia.glucoseglyph.glyph.PixelFont
import it.mattia.glucoseglyph.model.AppSettings

private val SENSOR_DURATION_OPTIONS = listOf(7, 10, 14, 15, 21)

/**
 * Personalization for the Glyph Toy: which pixel-font style each element draws with, the CGM
 * sensor's configured lifespan (the pump doesn't report this itself, only when the current
 * session started -- see AppSettings.sensorDurationDays), and mg/dL vs mmol/L for the toy's
 * display. Deliberately its own preference, independent of ControlX2's own glucose unit setting
 * -- the Glyph Toy lives in a separate Gradle module with no dependency on :mobile's Prefs.
 */
@Composable
fun GlyphSettings(
    innerPadding: PaddingValues = PaddingValues(),
    navController: NavHostController? = null,
) {
    val context = LocalContext.current
    val settings = remember { AppSettings(context) }

    var arrowStyle by remember { mutableStateOf(settings.arrowStyle) }
    var clockDigitStyle by remember { mutableStateOf(settings.clockDigitStyle) }
    var valueDigitStyle by remember { mutableStateOf(settings.valueDigitStyle) }
    var sensorDurationDays by remember { mutableStateOf(settings.sensorDurationDays) }
    var useMmol by remember { mutableStateOf(settings.useMmol) }

    var showArrowDialog by remember { mutableStateOf(false) }
    var showClockDialog by remember { mutableStateOf(false) }
    var showValueDialog by remember { mutableStateOf(false) }
    var showSensorDialog by remember { mutableStateOf(false) }

    LazyColumn(
        contentPadding = innerPadding,
        verticalArrangement = Arrangement.spacedBy(0.dp),
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 0.dp),
        content = {
            item {
                HeaderLine("Glyph Toy")
                Divider()
            }

            item {
                ListItem(
                    headlineContent = { Text("Unità di misura") },
                    supportingContent = { Text(if (useMmol) "mmol/L" else "mg/dL") },
                    leadingContent = {
                        Icon(Icons.Filled.WaterDrop, contentDescription = "Unità icon")
                    },
                    trailingContent = {
                        Switch(
                            checked = useMmol,
                            onCheckedChange = {
                                useMmol = it
                                settings.useMmol = it
                            }
                        )
                    },
                    modifier = Modifier.clickable {
                        useMmol = !useMmol
                        settings.useMmol = useMmol
                    }
                )
                Divider()
            }

            item {
                ListItem(
                    headlineContent = { Text("Stile freccia di tendenza") },
                    supportingContent = { Text(arrowStyle.label) },
                    leadingContent = {
                        Icon(Icons.Filled.ArrowUpward, contentDescription = "Freccia icon")
                    },
                    modifier = Modifier.clickable { showArrowDialog = true }
                )
                Divider()
            }

            item {
                ListItem(
                    headlineContent = { Text("Stile cifre orologio") },
                    supportingContent = { Text(clockDigitStyle.label) },
                    leadingContent = {
                        Icon(Icons.Filled.Timer, contentDescription = "Orologio icon")
                    },
                    modifier = Modifier.clickable { showClockDialog = true }
                )
                Divider()
            }

            item {
                ListItem(
                    headlineContent = { Text("Stile cifre glucosio") },
                    supportingContent = { Text(valueDigitStyle.label) },
                    leadingContent = {
                        Icon(Icons.Filled.Numbers, contentDescription = "Cifre icon")
                    },
                    modifier = Modifier.clickable { showValueDialog = true }
                )
                Divider()
            }

            item {
                ListItem(
                    headlineContent = { Text("Durata sensore CGM") },
                    supportingContent = { Text("$sensorDurationDays giorni") },
                    leadingContent = {
                        Icon(Icons.Filled.Timer, contentDescription = "Durata icon")
                    },
                    modifier = Modifier.clickable { showSensorDialog = true }
                )
                Divider()
            }
        }
    )

    if (showArrowDialog) {
        EnumPickerDialog(
            title = "Stile freccia di tendenza",
            options = PixelFont.ArrowStyle.entries.filter { it in PixelFont.arrowSets },
            selected = arrowStyle,
            label = { it.label },
            onSelect = {
                arrowStyle = it
                settings.arrowStyle = it
            },
            onDismiss = { showArrowDialog = false }
        )
    }
    if (showClockDialog) {
        EnumPickerDialog(
            title = "Stile cifre orologio",
            options = PixelFont.DigitStyle.entries.filter { it in PixelFont.clockDigitSets },
            selected = clockDigitStyle,
            label = { it.label },
            onSelect = {
                clockDigitStyle = it
                settings.clockDigitStyle = it
            },
            onDismiss = { showClockDialog = false }
        )
    }
    if (showValueDialog) {
        EnumPickerDialog(
            title = "Stile cifre glucosio",
            options = PixelFont.DigitStyle.entries.filter { it in PixelFont.valueDigitSets },
            selected = valueDigitStyle,
            label = { it.label },
            onSelect = {
                valueDigitStyle = it
                settings.valueDigitStyle = it
            },
            onDismiss = { showValueDialog = false }
        )
    }
    if (showSensorDialog) {
        EnumPickerDialog(
            title = "Durata sensore CGM",
            options = SENSOR_DURATION_OPTIONS,
            selected = sensorDurationDays,
            label = { "$it giorni" },
            onSelect = {
                sensorDurationDays = it
                settings.sensorDurationDays = it
            },
            onDismiss = { showSensorDialog = false }
        )
    }
}

@Composable
private fun <T> EnumPickerDialog(
    title: String,
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            LazyColumn {
                items(options) { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = option == selected,
                                onClick = {
                                    onSelect(option)
                                    onDismiss()
                                }
                            )
                            .padding(vertical = 8.dp)
                    ) {
                        RadioButton(selected = option == selected, onClick = null)
                        Text(label(option), modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Chiudi") }
        }
    )
}
