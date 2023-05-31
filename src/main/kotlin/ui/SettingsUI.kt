package ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import compressor.SettingsState

@Composable
fun settingsView(set: SettingsState, scope: BoxScope, onReady: () -> Unit) = scope.apply {
    Column {
        textButton("ГОТОВО", onClick = onReady)
        settingsRow("FFmpeg set", set.videoCodec)
        settingsRow("max video bitrate kbit/s", set.videoBitrate)
        settingsRow("max audio bitrate kbit/s", set.audioBitrate)
        settingsRow("max cpu cores use", set.cpuCores)
        settingsRow("maximum image resolution", set.imageResolution)
    }
}

@Composable
fun <T>settingsRow(text: String, settings: SettingsState.SettingProperty<T>) {
    Row(
        modifier = Modifier
            .padding(1.dp)
            .fillMaxWidth()
    ) {
        textSI(text, modifier = Modifier.fillMaxWidth(0.8f))
        dropdownSettings(settings.values, settings, Modifier.fillMaxWidth())
    }
}

@Composable
fun <T>dropdownSettings(items: List<String>, settings: SettingsState.SettingProperty<T>, modifier: Modifier) = Box {
    var expanded by remember { mutableStateOf(false) }

    textButton(settings.currentValueString(), modifier = modifier) { expanded = true }
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false },
        modifier = Modifier
            .background(SIGradient)
    ) {
        items.forEachIndexed { index, s ->
            DropdownMenuItem(
                modifier = Modifier.padding(5.dp)
                    .background(Color.Transparent),
                onClick = {
                    expanded = false
                    settings.setCurrentValue(settings.values[index])
                }) {
                textSI(s)
            }
        }
    }
}