package com.example.myobc

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.github.eltonvs.obd.command.NonNumericResponseException
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream


class MainActivity : AppCompatActivity() {
    /*displays gauges*/
    private lateinit var connection_status: TextView
    private lateinit var speed_display: TextView
    private lateinit var RPM_display: TextView
    private lateinit var coolant_display: TextView
    private lateinit var oil_temp_display: TextView
    private lateinit var intake_temp_display: TextView
    private lateinit var ambient_temp_display: TextView

    private var address: String = ""

    private var mBluetoothAdapter: BluetoothAdapter? = null

    private lateinit var bluetoothClient: BluetoothClient

    private lateinit var rpm_job: Job
    private lateinit var speed_job:Job
    private lateinit var coolant_job:Job
    private lateinit var oiltemp_job:Job
    private lateinit var intaketemp_job:Job
    private lateinit var ambienttemp_job:Job

    // since some value takes a lot to change or can be the same for a long time,
    // (eg. the engine is idling) we check if the new value is the same as the one read
    // before and only update it if is different to save resources
    private var lastRPMValue: Int = 0
    private var lastSpeedValue: String = ""
    private var lastOilValue: String = ""
    private var lastCoolantValue: String = ""
    private var lastIntakeValue: String = ""
    private var lastAmbientValue: String = ""

    private var connected: Boolean = false

    val mutexRPM = Mutex()
    val mutexSpeed = Mutex()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        //toolbar setup
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        //texts and display setup
        connection_status = findViewById<TextView>(R.id.connection_indicator)
        speed_display = findViewById<TextView>(R.id.speed_display)
        RPM_display = findViewById<TextView>(R.id.RPM_display)
        coolant_display = findViewById<TextView>(R.id.coolant_display)
        oil_temp_display = findViewById<TextView>(R.id.oil_temp_display)
        intake_temp_display = findViewById<TextView>(R.id.intake_air_display)
        ambient_temp_display = findViewById<TextView>(R.id.air_temp_display)

        connection_status.text = "Not connected to any ECU"
    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu , menu)
        return true
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            //open the bluetooth device list, the user can select a device and try to connect with
            R.id.bt_connect ->{
                startActivityForResult(
                    Intent(this@MainActivity, BluetoothActivity::class.java),
                    1
                )
                return true
            }
        }
        return false
    }
    private fun connect() {
        if (address != "") {
            CoroutineScope(Dispatchers.Main).launch {
                try {
                    mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
                    val device: BluetoothDevice = mBluetoothAdapter!!.getRemoteDevice(address)
                    bluetoothClient = BluetoothClient(device)
                    connection_status.text = "Connected to $address, connecting to ECU..."

                    // Connect to selected device
                    connected = bluetoothClient.connect()
                    display()

                } catch (e: Exception) {
                    // Handle exceptions appropriately
                    e.printStackTrace()
                }
            }
        }
    }

    private fun display() {
        if (connected) {
            connection_status.text = "Connected to ECU."
            CoroutineScope(Dispatchers.Default).launch { while (true) RPM() }
            CoroutineScope(Dispatchers.Default).launch { while (true) speed() }
            /*val coolantJob = launch { while (true) coolant() }
            val oiltempJob = launch { while (true) oiltemp() }
            val intaketempJob = launch { while (true) intaketemp() }
            val ambienttempJob = launch { while (true) ambienttemp() }*/
        }
    }

    private suspend fun RPM() {
        try {
            val outputStreamRPM = ByteArrayOutputStream()
            outputStreamRPM.reset()
            bluetoothClient.askRPM(outputStreamRPM)

            val inputStreamRPM = ByteArrayInputStream(outputStreamRPM.toByteArray())
            val bufferedStream = BufferedInputStream(inputStreamRPM)
            val resultString: String = bufferedStream.bufferedReader().use { it.readText() }

            withContext(Dispatchers.IO) {
                inputStreamRPM.close()
                outputStreamRPM.close()
            }

            val intval = resultString.toInt() / 4

            // Use mutex to safely update lastRPMValue
                if (intval != lastRPMValue) {
                    lastRPMValue = intval
                    runOnUiThread {
                        RPM_display.text = intval.toString()
                    }
                }

            // Delay here if needed
            delay(500)
        } catch (e: NonNumericResponseException) {
            // Handle exceptions appropriately
            runOnUiThread {
                RPM_display.text = "!"
            }
            delay(500)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun speed() {
        try {
            val outputStreamRPM = ByteArrayOutputStream()
            outputStreamRPM.reset()
            bluetoothClient.askSpeed(outputStreamRPM)

            val inputStreamRPM = ByteArrayInputStream(outputStreamRPM.toByteArray())
            val bufferedStream = BufferedInputStream(inputStreamRPM)
            val resultString: String = bufferedStream.bufferedReader().use { it.readText() }

            withContext(Dispatchers.IO) {
                inputStreamRPM.close()
                outputStreamRPM.close()
            }

            // Use mutex to safely update lastSpeedValue
            mutexSpeed.withLock {
                if (resultString != lastSpeedValue) {
                    lastSpeedValue = resultString
                    runOnUiThread {
                        speed_display.text = resultString
                    }
                }
            }

            // Delay here if needed
            delay(500)
        } catch (e: NonNumericResponseException) {
            // Handle exceptions appropriately
            runOnUiThread {
                speed_display.text = "!"
            }
            delay(500)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    private suspend fun coolant() {
        val outputStream = ByteArrayOutputStream()
        bluetoothClient.askCoolantTemp(outputStream)
        val inputStream = ByteArrayInputStream(outputStream.toByteArray())
        val resultString: String = inputStream.bufferedReader().use { it.readText() }
        yield()
        inputStream.close()
        outputStream.close()
        if (resultString != lastCoolantValue) {
            lastCoolantValue  = resultString
            runOnUiThread {
                coolant_display.text = resultString
            }
        }
        delay(100)
    }

    private suspend fun oiltemp() {
        val outputStream = ByteArrayOutputStream()
        bluetoothClient.askOilTemp(outputStream)
        val inputStream = ByteArrayInputStream(outputStream.toByteArray())
        val resultString: String = inputStream.bufferedReader().use { it.readText() }
        yield()
        inputStream.close()
        outputStream.close()
        if (resultString != lastOilValue) {
            lastOilValue  = resultString
            runOnUiThread {
                oil_temp_display.text = resultString
            }
        }
        delay(100)
    }

    private suspend fun intaketemp() {
        val outputStream = ByteArrayOutputStream()
        bluetoothClient.askIntakeTemp(outputStream)
        val inputStream = ByteArrayInputStream(outputStream.toByteArray())
        val resultString: String = inputStream.bufferedReader().use { it.readText() }
        yield()
        inputStream.close()
        outputStream.close()
        if (resultString != lastIntakeValue) {
            lastIntakeValue  = resultString
            runOnUiThread {
               intake_temp_display.text = resultString
            }
        }
        delay(100)
    }

    private suspend fun ambienttemp() {
        val outputStream = ByteArrayOutputStream()
        bluetoothClient.askAmbientTemp(outputStream)
        val inputStream = ByteArrayInputStream(outputStream.toByteArray())
        val resultString: String = inputStream.bufferedReader().use { it.readText() }
        Log.e("temp",resultString)
        yield()
        inputStream.close()
        outputStream.close()
        if (resultString != lastAmbientValue) {
            lastAmbientValue  = resultString
            runOnUiThread {
                ambient_temp_display.text = resultString
            }
        }
        delay(100)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(
        requestCode: Int, resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1) {
            if (resultCode == RESULT_OK) {
                // Get the device MAC address
                val extras = data!!.extras
                address= extras?.getString("device_address").toString()
                Log.e("address",address)
                connection_status.text = "Connecting to $address ..."
                connect()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        rpm_job.cancel()
        speed_job.cancel()
        coolant_job.cancel()
        oiltemp_job.cancel()
        intaketemp_job.cancel()
        ambienttemp_job.cancel()
    }


}