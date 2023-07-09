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
            //should init obd device sending AT commands
            //https://stackoverflow.com/questions/13764442/initialization-of-obd-adapter
            obdConnection.run(ResetAdapterCommand(), delayTime = 1000)
            obdConnection.run(SetEchoCommand(Switcher.OFF), delayTime = 500)
            obdConnection.run(SetLineFeedCommand(Switcher.OFF), delayTime = 500)
            obdConnection.run(SetSpacesCommand(Switcher.OFF), delayTime = 500) //not sure if necessary
            obdConnection.run(SetTimeoutCommand(500), delayTime = 500)
            obdConnection.run(HeadersCommand(Switcher.OFF), delayTime = 500)
            obdConnection.run(SelectProtocolCommand(ObdProtocols.AUTO), delayTime = 500)


            //obdConnection.run(SetBaudRateCommand(38400))


            Log.e("OBD connection", "OBD connection established")
            true
        } catch (e: IOException) {
            // Handle connection error
            e.printStackTrace()
            false
        }
    }
    suspend fun askRPM() {
        withTimeoutOrNull(5000) {
            withContext(Dispatchers.IO) {
                val writer = BufferedWriter(OutputStreamWriter(outputStream))

                try {
                    val response: ObdResponse = obdConnection.run(RPMCommand(), delayTime = 500)
                    //writer.flush()
                    writer.write(response.value)
                    writer.newLine()

                    Log.e("obd value",response.value)
                } catch (e: NoDataException) {
                    e.printStackTrace()
                }
            }

        }
    }

    suspend fun readRPM(): String {
        return withTimeout(5000) {
             val buffer = ByteArray(512)
             val data = StringBuilder()
             try {
                 withContext(Dispatchers.IO) {
                     while (inputStream.available() > 0) {
                         val bytesRead = inputStream.read(buffer)
                         Log.e("buffer",buffer.toString())
                         if (bytesRead > 0) {
                             data.append(String(buffer, 0, bytesRead))
                         }
                     }
                 }
                 if (data.isNotEmpty()) {
                     return@withTimeout data.toString()
                 } else {
                     return@withTimeout "empty"
                 }
             } catch (e: IOException) {
                 e.printStackTrace()
                 "input error"
             }
         } ?: "timeout"
    }


    suspend fun askSpeed() = withContext(Dispatchers.IO) {
        try {
            val response = obdConnection.run(SpeedCommand(), delayTime = 500L)
            Log.e("OBD raw response", response.rawResponse.toString())
            Log.e("OBD formatted", response.value)
            outputStream.write(response.value.toByteArray())
        } catch (e: Exception) {
            // Handle data sending error
            e.printStackTrace()
        }
    }

    suspend fun readSpeed(callback: (String) -> Unit) = withContext(Dispatchers.IO) {
        val buffer = ByteArray(1024)
        var bytesRead: Int
        val data = StringBuilder()
        try {

            bytesRead = inputStream.read(buffer) ?: 0
            if (bytesRead > 0) {
                data.append(String(buffer, 0, bytesRead))
                withContext(Dispatchers.Main) {
                    callback(data.toString())
                }
            }

        } catch (e: IOException) {
            // Handle data reading error
            e.printStackTrace()
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
