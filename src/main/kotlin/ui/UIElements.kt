package ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.ProgressIndicatorDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val SIColorObjects = Color.White
val SIBorderColor = Color(170, 170, 173)
val SIBorderStroke = 3.dp
val FuturaCondensed = FontFamily(
    Font(resource = "/font/futura_condensed_plain.ttf"),
    Font(resource = "/font/futura_condensed.ttf")
)
//val SIBlue = Color(0, 5, 60)
val SIGradient = Brush.linearGradient(
    colors = listOf(Color.Black, Color(13, 11, 160), Color.Black)
)

@Composable
fun textSI(text: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(shape = RectangleShape)
            .background(Color.Transparent)
            .border(BorderStroke(SIBorderStroke, SolidColor(SIBorderColor)))
            .fillMaxWidth(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = text,
            modifier = modifier
                .clip(shape = RectangleShape)
                .padding(5.dp)
                .background(Color.Transparent)
                .fillMaxWidth(),
            fontSize = 30.sp,
            fontFamily = FuturaCondensed,
            color = SIColorObjects,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun textButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    textSI(text, modifier = modifier.clickable { onClick() })
}

@Composable
fun selectableText(text: String) {
    SelectionContainer {
        textSI(text)
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