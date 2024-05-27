package com.starklosch.comunicacion

import androidx.compose.ui.graphics.Color
import java.io.PrintWriter
import java.net.InetSocketAddress
import java.net.Socket
import kotlin.math.roundToInt

class ControllerDevice(private val ip: String, private val port : Int = 10000) {
    fun testConnection(): Boolean {
        try {
            val s = Socket()
            s.connect(InetSocketAddress(ip, port), 1000)
            s.close()
            return true
        }
        catch (e: Exception){
            return false
        }
    }

    private fun get(message: String): String {
        val s = Socket()
        s.connect(InetSocketAddress(ip, port), 1000)

        val writer = PrintWriter(s.getOutputStream(), true)
        writer.println("GET $message")
        val reader = s.getInputStream().bufferedReader()
        val response = reader.readLine()
        s.close()
        return response
    }

    private fun set(message: String) {
        val s = Socket()
        s.connect(InetSocketAddress(ip, port), 1000)

        val writer = PrintWriter(s.getOutputStream(), true)
        writer.println("SET $message")
        val reader = s.getInputStream().bufferedReader()
        val response = reader.readLine()
        s.close()

        if (response != "OK")
            throw Exception("Error")
    }

    fun getColor(): Color {
        val response = get("COLOR")
        // R, G, B
        val (r, g, b) = response.split(", ").map { it.toInt() }
        return Color(r, g, b)
    }

    fun setColor(color: Color) {
        val red = (color.red * 255).roundToInt()
        val green = (color.green * 255).roundToInt()
        val blue = (color.blue * 255).roundToInt()
        set("COLOR $red, $green, $blue")
    }

    fun getOn(): Boolean {
        return get("ENCENDIDO").toBoolean()
    }

    fun setOn(on: Boolean) {
        set("ENCENDIDO $on")
    }
}
