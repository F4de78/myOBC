package com.example.myobc
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.util.Log
import com.github.eltonvs.obd.command.NoDataException
import com.github.eltonvs.obd.command.ObdProtocols
import com.github.eltonvs.obd.command.ObdResponse
import com.github.eltonvs.obd.command.Switcher
import com.github.eltonvs.obd.command.at.ResetAdapterCommand
import com.github.eltonvs.obd.command.at.SelectProtocolCommand
import com.github.eltonvs.obd.command.at.SetEchoCommand
import com.github.eltonvs.obd.command.at.SetLineFeedCommand
import com.github.eltonvs.obd.command.at.SetSpacesCommand
import com.github.eltonvs.obd.command.at.SetTimeoutCommand
import com.github.eltonvs.obd.command.engine.RPMCommand
import com.github.eltonvs.obd.command.engine.SpeedCommand
import com.github.eltonvs.obd.command.pressure.IntakeManifoldPressureCommand
import com.github.eltonvs.obd.command.temperature.AirIntakeTemperatureCommand
import com.github.eltonvs.obd.command.temperature.AmbientAirTemperatureCommand
import com.github.eltonvs.obd.command.temperature.EngineCoolantTemperatureCommand
import com.github.eltonvs.obd.command.temperature.OilTemperatureCommand
import com.github.eltonvs.obd.connection.ObdDeviceConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.util.UUID

class BluetoothClient(private val device: BluetoothDevice) {

    private val uuid: UUID =
        UUID.fromString("00001101-0000-1000-8000-00805F9B34FB") // Standard UUID for SPP (Serial Port Profile)

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var bluetoothSocket: BluetoothSocket
    private lateinit var inputStream: InputStream
    private lateinit var outputStream: OutputStream

    private lateinit var obdConnection: ObdDeviceConnection

    suspend fun connect(): Boolean = withContext(Dispatchers.IO) {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        if (!bluetoothAdapter.isEnabled) {
            // Bluetooth is not enabled, handle this case
            return@withContext false
        }

        return@withContext try {
            bluetoothSocket = device.createRfcommSocketToServiceRecord(uuid)
            bluetoothSocket.connect()
            Log.e("BT connection", "BT socket created: ${bluetoothSocket.isConnected}")
            inputStream = bluetoothSocket.inputStream
            outputStream = bluetoothSocket.outputStream
            obdConnection = ObdDeviceConnection(inputStream, outputStream)
            //initialize OBD adapter sending AT command
            //TODO: should init ELM like Torque/OBDAutodoc
            //https://stackoverflow.com/questions/13764442/initialization-of-obd-adapter
            obdConnection.run(ResetAdapterCommand(), delayTime = 1000)
            obdConnection.run(SetEchoCommand(Switcher.OFF), delayTime = 500)
            obdConnection.run(SetLineFeedCommand(Switcher.OFF), delayTime = 500)
            obdConnection.run(SetSpacesCommand(Switcher.OFF), delayTime = 500) //not sure if necessary
            obdConnection.run(SetTimeoutCommand(500), delayTime = 500)
            obdConnection.run(HeadersCommand(Switcher.OFF), delayTime = 500)
            obdConnection.run(SelectProtocolCommand(ObdProtocols.AUTO), delayTime = 500)

            Log.e("OBD connection", "OBD connection established")
            true
        } catch (e: IOException) {
            // Handle connection error
            e.printStackTrace()
            false
        }
    }

    suspend fun askRPM(outputStream: OutputStream)= withTimeoutOrNull(5000) {
        try {
            //using custom command (RPMCommandFix() )because the one in the library fails
            val aux: ObdResponse = obdConnection.run(RPMCommandFix(), delayTime = 500)
            Log.d("RPM value", aux.value)
            outputStream.write(aux.value.toByteArray())
            outputStream.flush()
        } catch (e: NoDataException) {
            e.printStackTrace()
        } finally {
            outputStream.close()
        }
    }

    suspend fun askSpeed(outputStream: OutputStream)= withTimeoutOrNull(5000) {
        try {
            val aux: ObdResponse = obdConnection.run(SpeedCommand(), delayTime = 500)
            outputStream.write(aux.value.toByteArray())
            outputStream.flush()
        } catch (e: NoDataException) {
            outputStream.write("!DATA".toByteArray())
            e.printStackTrace()
        } finally {
            outputStream.close()
        }
    }

    suspend fun askCoolantTemp(outputStream: OutputStream)= withTimeoutOrNull(5000) {
        try {
            val aux: ObdResponse =
                obdConnection.run(EngineCoolantTemperatureCommand(), delayTime = 500)
            outputStream.write(aux.value.toByteArray())
            outputStream.flush()
        } catch (e: NoDataException) {
            outputStream.write("!DATA".toByteArray())
            e.printStackTrace()
        } finally {
            outputStream.close()
        }
    }

    suspend fun askOilTemp(outputStream: OutputStream)= withTimeoutOrNull(5000) {
        try {
            val aux: ObdResponse =
                obdConnection.run(OilTemperatureCommand(), delayTime = 500)
            Log.e("oil response",aux.value)

            outputStream.write(aux.value.toByteArray())
            outputStream.flush()

        } catch (e: NoDataException) {
            outputStream.write("!DATA".toByteArray())
            e.printStackTrace()
        } finally {
            outputStream.close()
        }
    }

    suspend fun askIntakeTemp(outputStream: OutputStream)= withTimeoutOrNull(5000) {
        try {
            val aux: ObdResponse =
                obdConnection.run(AirIntakeTemperatureCommand(), delayTime = 500)

            outputStream.write(aux.value.toByteArray())
            outputStream.flush()
        } catch (e: NoDataException) {
            outputStream.write("!DATA".toByteArray())
            e.printStackTrace()
        } finally {
            outputStream.close()
        }
    }

    suspend fun askAmbientTemp(outputStream: OutputStream)= withTimeoutOrNull(5000) {
        try {
            val aux: ObdResponse =
                obdConnection.run(AmbientAirTemperatureCommand(), delayTime = 500)
            Log.e("ambient", aux.value)

            outputStream.write(aux.value.toByteArray())
            outputStream.flush()
        } catch (e: NoDataException) {
            outputStream.write("!DATA".toByteArray())
            e.printStackTrace()
        } finally {
            outputStream.close()
        }
    }

    suspend fun disconnect() = withContext(Dispatchers.IO) {
        try {
            inputStream.close()
            outputStream.close()
            bluetoothSocket.close()
        } catch (e: IOException) {
            // Handle disconnection error
            e.printStackTrace()
        }
    }
    }
