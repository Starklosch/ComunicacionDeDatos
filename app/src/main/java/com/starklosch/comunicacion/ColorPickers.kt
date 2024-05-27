package com.starklosch.comunicacion

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun RingPicker(
    hue: Float,
    setHue: (hue: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val gradient = remember {
        Brush.sweepGradient(
//            (0..360 step 60).map { Color.hsv(it.toFloat(), saturation, value) }
            listOf(
                Color.Red,
                Color.Yellow,
                Color.Green,
                Color.Cyan,
                Color.Blue,
                Color.Magenta,
                Color.Red
            )
        )
    }

    var radius by remember {
        mutableFloatStateOf(0f)
    }
    var center by remember {
        mutableStateOf(Pair(0f, 0f))
    }

    var pointerCenter by remember {
        mutableStateOf(Pair(0f, 0f))
    }
    val pointerColor = Color.hsv(hue, 1f, 1f)

    Box {

        Canvas(
            modifier
                .onGloballyPositioned {
                    radius =
                        if (it.size.width > it.size.height) it.size.height / 2f else it.size.width / 2f
                    center = Pair(it.size.width / 2f, it.size.height / 2f)
                    val rad = Math
                        .toRadians(hue.toDouble())
                        .toFloat()
                    pointerCenter = Pair(radius * cos(rad), radius * sin(rad))
                }
                .pointerInput(true) {
                    detectTapGestures {
                        val newPointerPosition = it - toOffset(center)
                        val distance = newPointerPosition.getDistance()
                        if (distance in radius - 20f..radius + 20f) {
                            setHue(angle(newPointerPosition.x, newPointerPosition.y))
                        }
                    }
                }
                .pointerInput(true) {
                    detectDragGestures { change, _ ->
                        val newPointerPosition = change.position - toOffset(center)
                        val distance = newPointerPosition.getDistance()
                        if (distance > 1f) {
                            setHue(angle(newPointerPosition.x, newPointerPosition.y))
                        }
                    }
                }
        ) {
            drawCircle(gradient, style = Stroke(10.dp.toPx()))
        }

        if (pointerCenter != Pair(0f, 0f)) {
            val x = with(LocalDensity.current) { pointerCenter.first.toDp() }
            val y = with(LocalDensity.current) { pointerCenter.second.toDp() }
            Pointer(color = pointerColor, modifier.offset(x, y))
        }
    }
}

@Composable
fun Pointer(color: Color, modifier: Modifier = Modifier, border: Float = 8f, radius: Float = 32f) {
    val paint = remember {
        Paint().apply {
            style = PaintingStyle.Stroke
            strokeWidth = 16f
        }
    }

    val frameworkPaint = remember {
        paint.asFrameworkPaint()
    }

    val transparent = color
        .copy(alpha = 0f)
        .toArgb()

    frameworkPaint.color = transparent

    frameworkPaint.setShadowLayer(
        4f,
        0f,
        0f,
        Color.DarkGray
            .copy(alpha = .5f)
            .toArgb()
    )

    Canvas(modifier = modifier) {
        drawIntoCanvas {
            it.drawCircle(
                paint = paint,
                radius = radius,
                center = center
            )
        }
        drawCircle(color, radius = radius)
        drawCircle(
            Color.White,
            radius = radius,
            style = Stroke(border)
        )
    }
}

@Composable
fun BrightnessPicker(
    hue: Float,
    saturation: Float,
    brightness: Float,
    setBrightness: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val color = Color.hsv(hue, saturation, brightness)
    val gradient = Brush.horizontalGradient(
        0f to Color.Black,
//        0.25f to Color.hsv(hue, saturation, 0.5f),
        1f to Color.hsv(hue, saturation, 1f)
    )

    HorizontalSlider(brightness, setBrightness, gradient, color, modifier)
}

@Composable
fun SaturationPicker(
    hue: Float,
    saturation: Float,
    brightness: Float,
    setSaturation: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val color = Color.hsv(hue, saturation, brightness)
    val gradient = Brush.horizontalGradient(
        0f to Color.hsv(hue, 0f, brightness),
//        0.25f to Color.hsv(hue, saturation, 0.5f),
        1f to Color.hsv(hue, 1f, brightness)
    )

    HorizontalSlider(saturation, setSaturation, gradient, color, modifier)
}

@Composable
fun HorizontalSlider(
    value: Float,
    setValue: (Float) -> Unit,
    brush: Brush,
    pointerColor: Color,
    modifier: Modifier = Modifier
) {
    var size by remember {
        mutableStateOf(IntSize.Zero)
    }

    Box(contentAlignment = Alignment.TopStart) {
        Canvas(modifier = modifier
            .onGloballyPositioned {
                size = it.size
            }
            .pointerInput(true) {
                detectTapGestures {
                    val v = it.x / size.width.toFloat()
                    setValue(v.coerceIn(0f..1f))
                }
            }
            .pointerInput(true) {
                detectDragGestures { change, _ ->
                    change.consume()
                    val v = change.position.x / size.width.toFloat()
                    setValue(v.coerceIn(0f..1f))
                }
            }) {
            val corner = 4.dp.toPx()

            drawRoundRect(brush, cornerRadius = CornerRadius(corner, corner))
        }

        if (size.width != 0) {
            val x = with(LocalDensity.current) { (size.width * value).toDp() }
            val y = with(LocalDensity.current) { (size.height / 2f).toDp() }
            Pointer(pointerColor, Modifier.offset(x, y))
        }
    }
}

@Composable
fun CirclePicker(
    hue: Float,
    saturation: Float,
    setColor: (hue: Float, saturation: Float) -> Unit,
    modifier: Modifier
) {
    var radius by remember {
        mutableFloatStateOf(0f)
    }

    var center by remember {
        mutableStateOf(Pair(0f, 0f))
    }

    var pointerCenter by remember {
        mutableStateOf(Pair(0f, 0f))
    }

    Box {
        Canvas(modifier = modifier
            .onGloballyPositioned {
                radius =
                    if (it.size.width > it.size.height) it.size.height / 2f else it.size.width / 2f
                center = Pair(it.size.width / 2f, it.size.height / 2f)
                val rad = Math
                    .toRadians(hue.toDouble())
                    .toFloat()
                pointerCenter = Pair(saturation * radius * cos(rad), saturation * radius * sin(rad))
            }
            .pointerInput(true) {
                detectTapGestures { offset ->
                    val centerRelative = offset - toOffset(center)
                    val hue = angle(centerRelative.x, centerRelative.y)
                    val saturation = distance(centerRelative.x, centerRelative.y, radius)
                    setColor(hue, saturation)
                }
            }
            .pointerInput(true) {
                detectDragGestures { change, _ ->
                    change.consume()
                    val pointer = change.position - toOffset(center)
                    val hue = angle(pointer.x, pointer.y)
                    val saturation = distance(pointer.x, pointer.y, radius)
                    setColor(hue, saturation)
                }
            }) {

            val gradient = Brush.sweepGradient(
                listOf(
                    Color.Red,
                    Color.Yellow,
                    Color.Green,
                    Color.Cyan,
                    Color.Blue,
                    Color.Magenta,
                    Color.Red
                ),
            )

            drawCircle(gradient, radius)
            drawCircle(
                Brush.radialGradient(0.0f to Color.White, 1.0f to Color.Transparent),
                radius
            )
        }

        val x = with(LocalDensity.current) { pointerCenter.first.toDp() }
        val y = with(LocalDensity.current) { pointerCenter.second.toDp() }
        Pointer(color = Color.hsv(hue, saturation, 1f), modifier.offset(x, y))
    }
}