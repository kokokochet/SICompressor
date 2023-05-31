@file:Suppress("unused", "unused", "unused")

package ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.res.useResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import compressor.CompressState
import compressor.compress
import kotlinx.coroutines.launch
import java.awt.FileDialog
import java.io.File
import java.net.URI
import java.util.*

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun packSelectField(siFileSet: (File?) -> Unit) {
    var isDragging by remember { mutableStateOf(false) }

    textButton(
        "ВЫБОР ПАКА (или перетащи на меня пак)",
        modifier = Modifier.padding(10.dp)
            .onExternalDrag(
                onDragStart = { isDragging = true },
                onDragExit = { isDragging = false },
                onDrag = {},
                onDrop = { state ->
                    val dragData = state.dragData
                    if (dragData is DragData.FilesList) {
                        siFileSet(
                            File(URI(dragData.readFiles().first()))
                        )
                    }
                    isDragging = false
                }
            )
    ) {
        siFileSet(openFileDialog(ComposeWindow(), "выбор пака"))
    }
}

@Composable
fun compressView(compressState: CompressState, fileCompress: (File) -> Unit) {
    Column(
        modifier = Modifier.padding(4.dp).fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (compressState.hasPreviousPath()) selectableText(compressState.previousFilePath!!)

        if (!compressState.compressInProgress) {
            packSelectField { receivedFile ->
                if (receivedFile != null) fileCompress(receivedFile)
            }
        } else {
            textSI(compressState.stringStatus.uppercase(Locale.getDefault()))
        }
    }
    loadingBar(compressState.progress)
}

@Composable
fun mainView(state: CompressState, fileCompress: (File) -> Unit) = Box(
    modifier = Modifier
        .fillMaxSize()
        .background(
            SIGradient
        )
) {
    var openSettings by remember { mutableStateOf(false) }

    if (openSettings) {
        settingsView(state.settings, scope = this) {
            openSettings = false
        }
    } else {
        if (!state.compressInProgress) {
            textButton("НАСТРОЙКИ") {
                openSettings = true
            }
        }

        compressView(state, fileCompress)
    }
}


fun app() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "SICompressor",
        icon = BitmapPainter(useResource("/icon.jpg", ::loadImageBitmap))
    ) {
        var state by remember { mutableStateOf(CompressState()) }
        val scope = rememberCoroutineScope()

        mainView(state) {
            scope.launch {
                compress(state.settings, it, state) { newState ->
                    state = newState
                    newState
                }
            }
        }
    }
}

fun openFileDialog(window: ComposeWindow, title: String): File? {
    var resFile: File? = null
    try {
        resFile = FileDialog(window, title, FileDialog.LOAD).apply {
            isMultipleMode = false
            val allowedExtensions = listOf(".siq")
            // windows
            file = allowedExtensions.joinToString(";") { "*$it" } // e.g. '*.jpg'

            // linux
            setFilenameFilter { _, name ->
                allowedExtensions.any {
                    name.endsWith(it)
                }
            }

            isVisible = true
        }.files.first()
    } catch (_: Exception) {
    }
    return resFile
}