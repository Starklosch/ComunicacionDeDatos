package com.starklosch.comunicacion

import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.IOException
import kotlin.time.Duration.Companion.seconds

interface LEDRepository {
    fun setColor(color: Color)
    fun getColor(): Color
    fun setOn(on: Boolean)
    fun isOn(): Boolean
    fun isConnected(): Boolean
    fun isDeviceSelected(): Boolean
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
    override fun isDeviceSelected(): Boolean = true
}

private object EmptyLEDRepository : LEDRepository {
    override fun setColor(color: Color) {
        TODO("Not yet implemented")
    }

    override fun getColor(): Color {
        TODO("Not yet implemented")
    }

    override fun setOn(on: Boolean) {
        TODO("Not yet implemented")
    }

    override fun isOn(): Boolean {
        TODO("Not yet implemented")
    }

    override fun isConnected(): Boolean = false
    override fun isDeviceSelected(): Boolean = false
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
    override fun isDeviceSelected(): Boolean = device != null
}

//class TestMainViewModel : MainViewModel(TestLEDRepository())
//class RealMainViewModel : MainViewModel(DeviceLEDRepository(null))

open class MainViewModel : ViewModel() {

    private var repository : LEDRepository = EmptyLEDRepository

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private val _errorUnread = MutableStateFlow(true)
    val errorUnread = _errorUnread.asStateFlow()

    private val _color = MutableStateFlow(Color.Red)
    val color = _color.asStateFlow()

    private val _on = MutableStateFlow(false)
    val on = _on.asStateFlow()

    private val _connected = MutableStateFlow(false)
    val connected = _connected.asStateFlow()

    fun connect(ip: String) = viewModelScope.launch(Dispatchers.IO){
        mutex.withLock {
            repository = DeviceLEDRepository(ControllerDevice(ip, 10000))
            if (repository.isConnected()) {
                getColor()
                getOn()
                keepUpdated()
            }
            else {
                setError("No se pudo conectar")
            }
        }
    }

    private var updateJob : Job? = null

    private fun keepUpdated() {
        updateJob?.cancel()
        updateJob = viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                getOn()
                getColor()
                delay(2.seconds)
            }
        }
    }

    fun setColor(color: Color) = doAction { repository.setColor(color); _color.value = color }

    private fun getColor() = doAction { _color.value = repository.getColor() }

    fun setOn(on: Boolean) = doAction { repository.setOn(on); _on.value = on }

    private fun getOn() = doAction { _on.value = repository.isOn() }

    private fun setError(error: String?){
        if (error != null) {
            _error.value = error
            _errorUnread.value = true
        }
    }

    fun markErrorAsRead(){
        _errorUnread.value = false
    }

    private val mutex = Mutex()

    private fun doAction(action: () -> Unit) = viewModelScope.launch(Dispatchers.IO) {
        mutex.withLock {
            try {
                action()
                _connected.value = true
            } catch (e: IOException) {
                _connected.value = false
            } catch (e: NotImplementedError) {

            } catch (e: Exception) {
                setError(e.message)
                e.let { Log.d("F", it.toString()) }
            }
        }
    }
}