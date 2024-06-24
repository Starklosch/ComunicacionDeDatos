package com.starklosch.comunicacion

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.starklosch.comunicacion.ui.theme.ComunicacionTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ComunicacionTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background
                ) {
                    App()
                }
            }
        }
    }
}

@Composable
fun App(viewModel: MainViewModel = viewModel(), findDevicesViewModel: FindDevicesViewModel = viewModel()) {
    val context = LocalContext.current
    val connected by viewModel.connected.collectAsState()
    val error by viewModel.error.collectAsState()
    val color by viewModel.color.collectAsState()
    val on by viewModel.on.collectAsState()
    val errorUnread by viewModel.errorUnread.collectAsState()

    val services by findDevicesViewModel.services.collectAsState()

    var selecting by rememberSaveable {
        mutableStateOf(true)
    }

    if (selecting)
        DevicePicker({ selecting = false; viewModel.connect(it) }, services)
    else
        Device(color, on, connected, { viewModel.setOn(!on) }, { viewModel.setColor(it) })

    LaunchedEffect(error, errorUnread) {
        if (errorUnread && !error.isNullOrBlank()) {
            Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
            viewModel.markErrorAsRead()
        }
    }
}

@Composable
fun DevicePicker(onDeviceSelected: (String) -> Unit, services: List<ServiceInfo>) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Selecciona un dispositivo", fontSize = 24.sp)
        Spacer(modifier = Modifier.height(16.dp))
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (services.isEmpty())
                item { Text(text = "No se encontró ningún dispositivo") }

            items(services) {
                Card(onClick = {
                    onDeviceSelected(it.addresses.first())
                }, Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(24.dp)) {
                        val name = it.name
                        val addresses = it.addresses.first()
                        Text(text = name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(text = addresses)
                    }
                }
            }
        }
    }
}


@Composable
fun Device(
    currentColor: Color, on: Boolean, connected: Boolean, onToggleClick: () -> Unit,
    onColorChanged: (Color) -> Unit
) {

    var hue by rememberSaveable {
        mutableFloatStateOf(0f)
    }

    var saturation by rememberSaveable {
        mutableFloatStateOf(1f)
    }

    var brightness by rememberSaveable {
        mutableFloatStateOf(1f)
    }

    var picking by rememberSaveable {
        mutableStateOf(false)
    }

    LaunchedEffect(key1 = currentColor) {
        if (currentColor == Color.hsv(hue, saturation, brightness))
            return@LaunchedEffect

        val max = maxOf(currentColor.red, currentColor.green, currentColor.blue)
        val min = minOf(currentColor.red, currentColor.green, currentColor.blue)
        val h = if (max == min) 0f else when (max) {
            currentColor.red -> {
                (currentColor.green - currentColor.blue) / (max - min)
            }

            currentColor.green -> {
                2 + (currentColor.blue - currentColor.red) / (max - min)
            }

            currentColor.blue -> {
                4 + (currentColor.red - currentColor.green) / (max - min)
            }

            else -> 0f
        }

        hue = 60f * h + (if (h < 0) 360 else 0)

        saturation = when (max) {
            0f -> 0f
            else -> 1 - min / max
        }
        brightness = max
    }

    val estado = when {
        !connected -> "Desconectado"
        on -> "Encendido"
        !on -> "Apagado"
        else -> "Desconocido"
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(48.dp),
        verticalArrangement = Arrangement.spacedBy(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(estado, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Column(Modifier.weight(1f)) {
            Box(Modifier.size(256.dp)) {
                LED(
                    on, hue, saturation, brightness,
                    Modifier
                        .size(128.dp)
                        .align(Alignment.Center)
                )
                if (picking)
                    RingPicker(
                        hue,
                        { hue = it },
                        Modifier.fillMaxSize()
                    )
            }
            if (picking) {
                Spacer(modifier = Modifier.height(32.dp))
                BrightnessPicker(
                    hue,
                    saturation,
                    brightness,
                    { brightness = it },
                    Modifier.size(256.dp, 16.dp)
                )
                if (brightness != 0f) {
                    Spacer(modifier = Modifier.height(32.dp))
                    SaturationPicker(
                        hue,
                        saturation,
                        brightness,
                        { saturation = it },
                        Modifier.size(256.dp, 16.dp)
                    )
                }
            }
        }
        Column(Modifier.width(IntrinsicSize.Max)) {
            Button(onClick = onToggleClick, Modifier.fillMaxWidth()) {
                Text(text = if (on) "Apagar" else "Encender")
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = {
                if (picking)
                    onColorChanged(Color.hsv(
                        hue,
                        saturation,
                        brightness
                    ))
                picking = !picking
            }, Modifier.fillMaxWidth()) {
                Text(text = if (picking) "Aceptar" else "Cambiar color")
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
fun DevicePreview() {
    var connected by remember {
        mutableStateOf(true)
    }
    var color by remember {
        mutableStateOf(Color.Red)
    }
    var on by remember {
        mutableStateOf(true)
    }

    ComunicacionTheme {
        Device(color, on, connected, { on = !on }, { color = it })
    }
}

@Preview(showBackground = true)
@Composable
fun DevicePickerPreview() {
    val services = listOf(
        ServiceInfo("Dispositivo 1", "Tipo", 80, listOf("192.168.0.2")),
        ServiceInfo("Dispositivo 2", "Tipo", 80, listOf("192.168.0.3"))
    )
    ComunicacionTheme {
        DevicePicker({ }, services)
    }
}

@Composable
fun LED(on: Boolean, hue: Float, saturation: Float, value: Float, modifier: Modifier = Modifier) {
    val baseColor = Color.LightGray
    val shadowColor = Color.hsv(hue, saturation, 1f).copy(value)
    val borderColor = if (on) shadowColor.compositeOver(baseColor) else baseColor
    val border = 8f
    val paint = Paint().apply {
        style = PaintingStyle.Stroke
        strokeWidth = border
    }

    paint.asFrameworkPaint().apply {
        this.color = borderColor.toArgb()
        if (on) {
            setShadowLayer(14f, 0f, 0f, shadowColor.toArgb())
        }
    }

    Canvas(modifier = modifier) {
        val rounded = Path()
        val width = size.width * 0.4f
        val height = size.height * 0.6f - border / 2f
        val baseWidth = width * 1.25f
        val baseHeight = size.height / 8
        val ending = width / 2
        val beginning = -ending
        val rightTerminalX = width / 4
        val leftTerminalX = -rightTerminalX
        val fillColor = borderColor.copy(borderColor.alpha * 0.5f)

        rounded.apply {
            moveTo(beginning, height)
            arcTo(Rect(Offset(beginning, border / 2f), Size(width, width)), 180f, 180f, false)
            lineTo(ending, height)
            close()
        }

        translate(size.width * 0.45f) {
            drawPath(rounded, fillColor)
            drawIntoCanvas {
                it.drawPath(rounded, paint)
            }
            drawLine(
                Color.Gray,
                Offset(rightTerminalX, height),
                Offset(rightTerminalX, size.height),
                strokeWidth = border
            )
            drawLine(
                Color.Gray,
                Offset(leftTerminalX, height),
                Offset(leftTerminalX, size.height * 0.88f),
                strokeWidth = border
            )
            drawRoundRect(
                fillColor,
                Offset(beginning, height),
                Size(baseWidth, baseHeight),
                cornerRadius = CornerRadius(8f, 8f)
            )
            drawRoundRect(
                borderColor,
                Offset(beginning, height),
                Size(baseWidth, baseHeight),
                style = Stroke(border, cap = StrokeCap.Square),
                cornerRadius = CornerRadius(4f, 4f)
            )
        }
    }
}

