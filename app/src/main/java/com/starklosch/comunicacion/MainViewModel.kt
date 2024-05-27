package com.starklosch.comunicacion

import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.IOException

interface LEDRepository {
    fun setColor(color: Color)
    fun getColor(): Color
    fun setOn(on: Boolean)
    fun isOn(): Boolean
    fun isConnected(): Boolean
}

class TestLEDRepository : LEDRepository {
    private var on: Boolean = true
    private var color: Color = Color.White

    override fun setColor(color: Color) {
        this.color = color
    }

    override fun setOn(on: Boolean) {
        this.on = on
    }

    override fun getColor(): Color = color
    override fun isOn(): Boolean = on
    override fun isConnected(): Boolean = true
}

private class DeviceLEDRepository(val device: ControllerDevice?) : LEDRepository {
    override fun setColor(color: Color) {
        device?.setColor(color)
    }

    override fun setOn(on: Boolean) {
        device?.setOn(on)
    }

    override fun getColor(): Color = device?.getColor() ?: throw Exception("Dispositivo no seleccionado")
    override fun isOn(): Boolean = device?.getOn() ?: throw Exception("Dispositivo no seleccionado")
    override fun isConnected(): Boolean = device?.testConnection() ?: throw Exception("Dispositivo no seleccionado")
}

class MainViewModel : ViewModel() {

    private var repository : LEDRepository = DeviceLEDRepository(null)

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private val _color = MutableStateFlow(Color.Red)
    val color = _color.asStateFlow()

    private val _on = MutableStateFlow(false)
    val on = _on.asStateFlow()

    private val _connected = MutableStateFlow(false)
    val connected = _connected.asStateFlow()

    fun connect(ip: String) {
        repository = DeviceLEDRepository(ControllerDevice(ip, 10000))
        if (repository.isConnected()) {
            getColor()
            getOn()
        }
        else {
            _error.value = "No se pudo conectar"
        }
    }

    fun setColor(color: Color) = doAction { repository.setColor(color); _color.value = color }

    fun getColor() = doAction { _color.value = repository.getColor() }

    fun setOn(on: Boolean) = doAction { repository.setOn(on); _on.value = on }

    fun getOn() = doAction { _on.value = repository.isOn() }

    private val mutex = Mutex()

    private fun doAction(action: () -> Unit) = viewModelScope.launch(Dispatchers.IO) {
        mutex.withLock {
            try {
                action()
                _connected.value = true
            } catch (e: IOException) {
                _connected.value = false
            } catch (e: Exception) {
                _error.value = e.message
                e.let { Log.d("F", it.toString()) }
            }
        }
    }
}