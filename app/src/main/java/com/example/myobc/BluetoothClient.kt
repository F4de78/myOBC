package com.example.myobc
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.util.Log
import com.github.eltonvs.obd.command.AdaptiveTimingMode
import com.github.eltonvs.obd.command.NoDataException
import com.github.eltonvs.obd.command.ObdProtocols
import com.github.eltonvs.obd.command.ObdResponse
import com.github.eltonvs.obd.command.Switcher
import com.github.eltonvs.obd.command.at.ResetAdapterCommand
import com.github.eltonvs.obd.command.at.SelectProtocolCommand
import com.github.eltonvs.obd.command.at.SetAdaptiveTimingCommand
import com.github.eltonvs.obd.command.at.SetEchoCommand
import com.github.eltonvs.obd.command.at.SetLineFeedCommand
import com.github.eltonvs.obd.command.at.SetSpacesCommand
import com.github.eltonvs.obd.command.engine.SpeedCommand
import com.github.eltonvs.obd.command.temperature.AirIntakeTemperatureCommand
import com.github.eltonvs.obd.command.temperature.AmbientAirTemperatureCommand
import com.github.eltonvs.obd.command.temperature.EngineCoolantTemperatureCommand
import com.github.eltonvs.obd.command.temperature.OilTemperatureCommand
import com.github.eltonvs.obd.connection.ObdDeviceConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.yield
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

class BluetoothClient(private val device: BluetoothDevice) {

    private val uuid: UUID =
        UUID.fromString("00001101-0000-1000-8000-00805F9B34FB") // Standard UUID for SPP (Serial Port Profile)

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var bluetoothSocket: BluetoothSocket
    private lateinit var inputStream: InputStream
    private lateinit var outputStream: OutputStream

    private lateinit var obdConnection: ObdDeviceConnection

    /***
     * AT Initialization procedure reversed engineered from "Torque" app
     * */
    private suspend fun torqueATInit(connection: ObdDeviceConnection){
        obdConnection.run(ResetAdapterCommand(), delayTime = 1000)          //ATZ
        obdConnection.run(SetEchoCommand(Switcher.OFF), delayTime = 500)    //ATE0
        obdConnection.run(SetEchoCommand(Switcher.OFF), delayTime = 500)    //ATE0
        obdConnection.run(SetMemoryCommand(Switcher.OFF), delayTime = 500)  //ATM0
        obdConnection.run(SetLineFeedCommand(Switcher.OFF), delayTime = 500)//ATL0
        obdConnection.run(SetSpacesCommand(Switcher.OFF), delayTime = 500)  //ATS0
        obdConnection.run(DeviceDescriptorCommand(), delayTime = 500)       //AT@1
        obdConnection.run(DeviceInfoCommand(), delayTime = 500)             //ATI
        obdConnection.run(HeadersCommand(Switcher.OFF), delayTime = 500)    //ATH0
        obdConnection.run(SetAdaptiveTimingCommand(AdaptiveTimingMode.AUTO_1), delayTime = 500)//ATH0
        obdConnection.run(DisplayProtoNumberCommand(), delayTime = 500)     //ATH0
        obdConnection.run(SelectProtocolCommand(ObdProtocols.AUTO), delayTime = 500)//ATSP0
    }
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
            torqueATInit(obdConnection)
            delay(5000)
            Log.d("OBD connection", "OBD connection established")
            true
        } catch (e: IOException) {
            // Handle connection error
            e.printStackTrace()
            false
        }
    }

    suspend fun askRPM(outputStream: OutputStream){
        withContext(Dispatchers.IO) {
            try {
                //using custom command (RPMCommandFix() )because the one in the library is broken
                val aux: ObdResponse = obdConnection.run(RPMCommandFix(), delayTime = 1000)
                outputStream.write(aux.value.toByteArray())
                yield()
                //outputStream.flush()
            } catch (e: NoDataException) {
                e.printStackTrace()
            } finally {
                outputStream.close()
            }
            //delay(500)
        }
        return 0
    }

    suspend fun askSpeed(outputStream: OutputStream)= withTimeoutOrNull(5000) {
        withContext(Dispatchers.IO) {
            try {
                val aux: ObdResponse = obdConnection.run(SpeedCommand(), delayTime = 1000)
                outputStream.write(aux.value.toByteArray())
                //outputStream.flush()
                yield()


            } catch (e: NoDataException) {
                outputStream.write("!DATA".toByteArray())
                e.printStackTrace()
            } finally {
                outputStream.close()
            }
            //delay(500)
        }
    }

    suspend fun askCoolantTemp(outputStream: OutputStream)= withTimeoutOrNull(5000) {
        try {
            val aux: ObdResponse =
                obdConnection.run(EngineCoolantTemperatureCommand(), delayTime = 500)
            withContext(Dispatchers.IO) {
                outputStream.write(aux.value.toByteArray())
            }
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

            withContext(Dispatchers.IO) {
                outputStream.write(aux.value.toByteArray())
            }
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

            withContext(Dispatchers.IO) {
                outputStream.write(aux.value.toByteArray())
            }
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

            withContext(Dispatchers.IO) {
                outputStream.write(aux.value.toByteArray())
            }
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
