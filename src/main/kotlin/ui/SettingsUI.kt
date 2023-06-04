package ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.unit.dp
import compressor.SettingsState

data class Explanation(
    val actual: String? = null,
    val default: String = "тут будут пояснения если навести на настроки"
)

@Composable
fun settingsView(set: SettingsState, scope: BoxScope, onReady: () -> Unit) = scope.apply {
    var explanations by remember {
        mutableStateOf(Explanation())
    }

    Column {
        textButton("ГОТОВО", onClick = onReady)
        Column {
            settingsRow("FFmpeg encoder", set.videoCodec) {
                explanations = Explanation(it)
            }
            settingsRow("макс битрейт видео", set.videoBitrate) {
                explanations = Explanation(it)
            }
            settingsRow("макс битрейт аудио kbit/s", set.audioBitrate) {
                explanations = Explanation(it)
            }
            settingsRow("число ядер под FFmpeg", set.cpuCores) {
                explanations = Explanation(it)
            }
            settingsRow("макс разрешение изображения", set.imageResolution) {
                explanations = Explanation(it)
            }
        }
        textSI(explanations.actual?:explanations.default)
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun <T> settingsRow(text: String, settings: SettingsState.SettingProperty<T>, explainSetting: (String?) -> Unit) {
    Row(
        modifier = Modifier
            .padding(1.dp)
            .fillMaxWidth()
            .onPointerEvent(PointerEventType.Enter) {
                explainSetting(settings.explanations)
            }
            .onPointerEvent(PointerEventType.Exit) {
                explainSetting(null)
            }
    ) {
        textSI(text, modifier = Modifier.fillMaxWidth(0.8f))
        dropdownSettings(settings.values, settings, Modifier.fillMaxWidth())
    }
}

@Composable
fun <T> dropdownSettings(items: List<String>, settings: SettingsState.SettingProperty<T>, modifier: Modifier) = Box {
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