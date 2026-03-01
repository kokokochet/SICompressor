package ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.awt.awtEventOrNull
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.res.useResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import compressor.CompressState
import compressor.compress
import kotlinx.coroutines.launch
import java.awt.FileDialog
import java.awt.dnd.DnDConstants
import java.awt.dnd.DropTarget
import java.awt.dnd.DropTargetAdapter
import java.awt.dnd.DropTargetDropEvent
import java.awt.datatransfer.DataFlavor
import java.io.File
import java.util.*

@Composable
fun packSelectField(siFileSet: (File?) -> Unit) {
    textButton(
        "ВЫБОР ПАКА (или перетащи на меня пак)",
        modifier = Modifier.padding(10.dp)
    ) {
        siFileSet(openFileDialog(ComposeWindow(), "выбор пака"))
    }
}

// Функция для установки drop target на окно
fun setupDropTarget(window: java.awt.Window, onFileDropped: (File) -> Unit) {
    window.dropTarget = DropTarget().apply {
        addDropTargetListener(object : DropTargetAdapter() {
            override fun drop(event: DropTargetDropEvent) {
                event.acceptDrop(DnDConstants.ACTION_COPY)
                val transferable = event.transferable
                if (transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                    @Suppress("UNCHECKED_CAST")
                    val files = transferable.getTransferData(DataFlavor.javaFileListFlavor) as List<File>
                    if (files.isNotEmpty()) {
                        onFileDropped(files.first())
                    }
                }
                event.dropComplete(true)
            }
        })
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
                try {
                    compress(state.settings, it, state) { newState ->
                        state = newState
                        newState
                    }
                } catch (e: Exception) {
                    state = state.copy(
                        stringStatus = "Ошибка: ${e.message ?: "неизвестная ошибка"}",
                        compressInProgress = false
                    )
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