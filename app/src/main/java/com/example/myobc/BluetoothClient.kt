package com.example.myobc
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.util.Log
import com.github.eltonvs.obd.command.AdaptiveTimingMode
import com.github.eltonvs.obd.command.NoDataException
import com.github.eltonvs.obd.command.NonNumericResponseException
import com.github.eltonvs.obd.command.ObdProtocols
import com.github.eltonvs.obd.command.ObdResponse
import com.github.eltonvs.obd.command.Switcher
import com.github.eltonvs.obd.command.at.ResetAdapterCommand
import com.github.eltonvs.obd.command.at.SelectProtocolCommand
import com.github.eltonvs.obd.command.at.SetAdaptiveTimingCommand
import com.github.eltonvs.obd.command.at.SetEchoCommand
import com.github.eltonvs.obd.command.at.SetLineFeedCommand
import com.github.eltonvs.obd.command.at.SetSpacesCommand
import com.github.eltonvs.obd.command.engine.LoadCommand
import com.github.eltonvs.obd.command.engine.SpeedCommand
import com.github.eltonvs.obd.command.temperature.AirIntakeTemperatureCommand
import com.github.eltonvs.obd.command.temperature.EngineCoolantTemperatureCommand
import com.github.eltonvs.obd.command.temperature.OilTemperatureCommand
import com.github.eltonvs.obd.connection.ObdDeviceConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

class BluetoothClient(private val device: BluetoothDevice) {

    // Standard UUID for SPP (Serial Port Profile)
    private val uuid: UUID =
        UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var bluetoothSocket: BluetoothSocket
    private lateinit var inputStream: InputStream
    private lateinit var outputStream: OutputStream

    private lateinit var obdConnection: ObdDeviceConnection


    //AT Initialization procedure reversed engineered from "Torque" app
    private suspend fun torqueATInit(){
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
            Log.d("BT connection", "BT socket created: ${bluetoothSocket.isConnected}")
            inputStream = bluetoothSocket.inputStream
            outputStream = bluetoothSocket.outputStream
            obdConnection = ObdDeviceConnection(inputStream, outputStream)
            torqueATInit()
            delay(1000)
            Log.d("OBD connection", "OBD connection established")
            true
        } catch (e: IOException) {
            // Handle connection error
            e.printStackTrace()
            false
        }
    }

    suspend fun askRPM(): String {
        return withContext(Dispatchers.IO) {
            try {
                //using custom command (RPMCommandFix() )because the one in the library is broken
                val aux: ObdResponse = obdConnection.run(RPMCommandFix(), delayTime = 100)
                Log.w("debugRPM", (aux.value.toInt()/4).toString())
                yield()
                (aux.value.toInt()/4).toString() // Return the value of aux.value
            } catch (e: NonNumericResponseException) {
                e.printStackTrace()
                "?NaN"
            } catch(e:NoDataException){
                e.printStackTrace()
                "!DATA"
            }
        }
    }

    suspend fun askSpeed(): String {
        return withContext(Dispatchers.IO) {
            try {
                val aux: ObdResponse = obdConnection.run(SpeedCommand(), delayTime = 100)
                yield()
                aux.value // Return the value of aux.value
            } catch (e: NonNumericResponseException) {
                e.printStackTrace()
                "?NaN"
            } catch(e:NoDataException){
                e.printStackTrace()
                "!DATA"
            }finally {
                // Close any resources if needed
            }
        }
    }

    suspend fun askCoolantTemp(): String {
        return withContext(Dispatchers.IO) {
            try {
                val aux: ObdResponse =
                    obdConnection.run(EngineCoolantTemperatureCommand(), delayTime = 100)
                yield()
                aux.value // Return the value of aux.value
            } catch (e: NonNumericResponseException) {
                e.printStackTrace()
                "?NaN"
            } catch(e:NoDataException){
                e.printStackTrace()
                "!DATA"
            }
        }
    }

    suspend fun askOilTemp(): String {
        return withContext(Dispatchers.IO) {
            try {
                val aux: ObdResponse =
                    obdConnection.run(OilTemperatureCommand(), delayTime = 100)
                yield()
                aux.value // Return the value of aux.value
            } catch (e: NonNumericResponseException) {
                e.printStackTrace()
                "?NaN"
            } catch(e:NoDataException){
                e.printStackTrace()
                "!DATA"
            }
        }
    }

    suspend fun askIntakeTemp(): String {
        return withContext(Dispatchers.IO) {
            try {
                val aux: ObdResponse =
                    obdConnection.run(AirIntakeTemperatureCommand(), delayTime = 100)
                yield()
                aux.value // Return the value of aux.value
            } catch (e: NonNumericResponseException) {
                e.printStackTrace()
                "?NaN"
            } catch(e:NoDataException){
                e.printStackTrace()
                "!DATA"
            }
        }
    }

    suspend fun askEngineLoad(): String {
        return withContext(Dispatchers.IO) {
            try {
                val aux: ObdResponse =
                    obdConnection.run(LoadCommand(), delayTime = 100)
                yield()
                aux.value // Return the value of aux.value
            } catch (e: NonNumericResponseException) {
                e.printStackTrace()
                "?NaN"
            } catch(e:NoDataException){
                e.printStackTrace()
                "!DATA"
            } 
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
