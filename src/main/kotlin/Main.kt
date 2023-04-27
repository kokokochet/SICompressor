import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.res.useResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import kotlinx.coroutines.launch
import java.awt.FileDialog
import java.io.File
import java.net.URI
import java.util.*

val SIColorObjects = Color.White
val SIBorderColor = Color(170, 170, 173)
val SIBorderStroke = 3.dp
val futuraCondensed = FontFamily(
    Font(resource = "/font/futura_condensed_plain.ttf"),
    Font(resource = "/font/futura_condensed.ttf")
)
val SIBlue = Color(0, 5, 60)
val SIGradient = Brush.linearGradient(
    colors = listOf(Color.Black, Color(13, 11, 160), Color.Black)
)

@Composable
fun SIText(text: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .clip(shape = RectangleShape)
            .background(Color.Transparent)
            .border(BorderStroke(3.dp, SolidColor(SIBorderColor))),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(8.dp)
                .background(Color.Transparent)
                .clickable(onClick = onClick),
            fontSize = 30.sp,
            fontFamily = futuraCondensed,
            color = SIColorObjects,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun SIText(text: String) {
    SelectionContainer {
        SIText(text) {}
    }
}

@Composable
fun loadingBar(progress: Float) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec
    )
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        ProgressIndicatorDefaults.ProgressAnimationSpec
        LinearProgressIndicator(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp),
            progress = animatedProgress,
            color = SIColorObjects,
            backgroundColor = Color.Gray
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun packSelect(siFileSet: (File?) -> Unit) {
    var isDragging by remember { mutableStateOf(false) }

    OutlinedButton(
        modifier = Modifier
            .padding(10.dp)
            .onExternalDrag(
                onDragStart = {
                    isDragging = true
                },
                onDragExit = {
                    isDragging = false
                },
                onDrag = {},
                onDrop = { state ->
                    val dragData = state.dragData
                    if (dragData is DragData.FilesList) {
                        siFileSet(
                            File(URI(dragData.readFiles().first()))
                        )
                    }
                    isDragging = false
                }),
        colors = ButtonDefaults.outlinedButtonColors(backgroundColor = Color.Transparent),
        border = BorderStroke(SIBorderStroke, SIBorderColor),
        shape = RectangleShape,
        onClick = {
            siFileSet(openFileDialog(ComposeWindow(), "выбор пака"))
        }
    ) {
        Text(
            "ВЫБОР ПАКА (или перетащи на меня пак)",
            fontSize = 40.sp,
            fontFamily = futuraCondensed,
            color = SIColorObjects,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun SIDropdown(items: List<String>, setValue: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    var selectedIndex by remember { mutableStateOf(0) }

    SIText(items[selectedIndex]) { expanded = true }
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false },
        modifier = Modifier
            .background(SIBlue)
    ) {
        items.forEachIndexed { index, s ->
            DropdownMenuItem(
                modifier = Modifier.padding(8.dp)
                    .background(Color.Transparent),
                onClick = {
                    selectedIndex = index
                    expanded = false
                    setValue(items[selectedIndex])
                }) {
                Text(
                    text = s,
                    modifier = Modifier.padding(8.dp)
                        .background(Color.Transparent),
                    fontSize = 30.sp,
                    fontFamily = futuraCondensed,
                    color = SIColorObjects,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun compressSettingsUI() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .wrapContentSize(Alignment.TopStart)
            .background(Color.Transparent),
        contentAlignment = Alignment.TopCenter
    ) {
        Row(
            modifier = Modifier
                .padding(8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier
                .padding(1.dp)) {
                SIText("FFmpeg set")
                SIDropdown(Compressor.VIDEO_CODECS) {
                    Compressor.VIDEO_FORMAT = it
                }
            }

            Column(modifier = Modifier
                .padding(1.dp)) {
                SIText("max video bitrate kbit/s")
                SIDropdown(Compressor.VIDEO_BITRATES) {
                    Compressor.VIDEO_BIT_RATE = it.toInt()
                }
            }

            Column(modifier = Modifier
                .padding(1.dp)) {
                SIText("max audio bitrate kbit/s")
                SIDropdown(Compressor.AUDIO_BITRATES) {
                    Compressor.BIT_RATE_MP3 = it.toInt()
                }
            }
        }
    }
}


@Composable
@Preview
fun mainUI() {
    var siFile: File?
    var progress by remember { mutableStateOf(0f) }
    var status by remember { mutableStateOf("") }
    var compressFinal by remember { mutableStateOf<String?>(null) }
    var listOfDone by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    //if (status == "") compressSettingsUI()

    Column(
        modifier = Modifier.padding(4.dp).fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (listOfDone.isNotBlank()) SIText(listOfDone)
        if (status == "") {
            packSelect { receivedFile ->
                if (receivedFile != null) scope.launch {
                    siFile = receivedFile
                    compressFinal = Compressor.compress(siFile) { prgrss, st ->
                        progress = prgrss
                        status = st
                    }
                }
            }
        } else {
            SIText(status.uppercase(Locale.getDefault()))
            if (compressFinal != null) {
                listOfDone = status
                status = ""
                progress = 0f
                compressFinal = null
            }
        }
    }
    loadingBar(progress)
}

@Composable
@Preview
fun AppBackground() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                SIGradient
            )
    ) {
        mainUI()
    }
}

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "SICompressor",
        icon = BitmapPainter(useResource("/icon.jpg", ::loadImageBitmap))
    ) {
        AppBackground()
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