package com.starklosch.comunicacion

import android.net.nsd.NsdServiceInfo
import android.os.Build
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.IntSize
import java.net.InetAddress
import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.sqrt

fun angle(x: Float, y: Float): Float {
    val angle: Float = when {
        x > 0 && y >= 0 -> atan(y / x)
        x < 0 -> atan(y / x) + PI.toFloat()
        x > 0 && y < 0 -> atan(y / x) + 2 * PI.toFloat()
        else -> 0f
    }
    return Math.toDegrees(angle.toDouble()).toFloat()
}

fun distance(x: Float, y: Float, radius: Float): Float {
    val d = sqrt(x * x + y * y) / radius
    return if (d > 1) 1f else d
}

fun toOffset(pair: Pair<Float, Float>) = Offset(pair.first, pair.second)
fun toOffset(pair: Size) = Offset(pair.width, pair.height)
fun toOffset(pair: IntSize) = Offset(pair.width.toFloat(), pair.height.toFloat())
fun toPair(offset: Offset) = Pair(offset.x, offset.y)

fun NsdServiceInfo.getAddresses(): List<InetAddress> {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        this.hostAddresses
    } else {
        listOf(this.host)
    }
}