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
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
        menuInflater.inflate(R.menu.menu , menu);
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
            // Get the BluetoothDevice object
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            //get bluetooth device
            val device: BluetoothDevice = mBluetoothAdapter!!.getRemoteDevice(address)
            bluetoothClient = BluetoothClient(device)
            connection_status.text = "Connected to $address, connecting to ECU..."

            CoroutineScope(Dispatchers.Main).launch {
                //connect to selected device
                val connected = withContext(Dispatchers.IO) {
                    bluetoothClient.connect()
                }
                if (connected) {
                    connection_status.text = "Connected to ECU."
                    var rpm_job: Job
                    val speed_job:Job
                    val coolant_job:Job
                    val oiltemp_job:Job
                    val intaketemp_job:Job
                    val ambienttemp_job:Job
                    coroutineScope {
                        rpm_job = launch { while(true) RPM() }
                        speed_job = launch { while(true) speed() }
                        coolant_job = launch { while(true) coolant() }
                        oiltemp_job = launch { while(true) oiltemp() }
                        intaketemp_job = launch { while(true) intaketemp() }
                        ambienttemp_job = launch { while(true) ambienttemp() }
                    }
                }
            }
        }
    }

    private suspend fun RPM(){
        val outputStreamRPM = ByteArrayOutputStream();
        bluetoothClient.askRPM(outputStreamRPM)
        val inputStreamRPM = ByteArrayInputStream(outputStreamRPM.toByteArray())
        val bufferedStream = BufferedInputStream(inputStreamRPM)
        val resultString: String = bufferedStream.bufferedReader().use { it.readText() }
        //after we read we tell other cooroutine to do their job
        yield()
        inputStreamRPM.close()
        outputStreamRPM.close()
        /** we need to do this step because the OBD parser fail to compute the correct formula to get
            rpm (RPM = ECU_value/4) **/
        val intval = resultString.toInt()/4
        runOnUiThread {
            RPM_display.text = intval.toString()
        }
        delay(0)
    }

    private suspend fun speed() {
        val outputStreamRPM = ByteArrayOutputStream();
        bluetoothClient.askSpeed(outputStreamRPM)
        val inputStreamSpeed = ByteArrayInputStream(outputStreamRPM.toByteArray())
        val resultString: String = inputStreamSpeed.bufferedReader().use { it.readText() }
        yield()
        inputStreamSpeed.close()
        outputStreamRPM.close()
        runOnUiThread {
            speed_display.text = resultString
        }
        delay(100)
    }

    private suspend fun coolant() {
        val outputStream = ByteArrayOutputStream();
        bluetoothClient.askCoolantTemp(outputStream)
        val inputStream = ByteArrayInputStream(outputStream.toByteArray())
        val resultString: String = inputStream.bufferedReader().use { it.readText() }
        yield()
        inputStream.close()
        outputStream.close()
        runOnUiThread {
            coolant_display.text = resultString
        }
        delay(100)
    }

    private suspend fun oiltemp() {
        val outputStream = ByteArrayOutputStream();
        bluetoothClient.askOilTemp(outputStream)
        val inputStream = ByteArrayInputStream(outputStream.toByteArray())
        val resultString: String = inputStream.bufferedReader().use { it.readText() }
        yield()
        inputStream.close()
        outputStream.close()
        runOnUiThread {
            oil_temp_display.text = resultString
        }
        delay(100)
    }

    private suspend fun intaketemp() {
        val outputStream = ByteArrayOutputStream();
        bluetoothClient.askIntakeTemp(outputStream)
        val inputStream = ByteArrayInputStream(outputStream.toByteArray())
        val resultString: String = inputStream.bufferedReader().use { it.readText() }
        yield()
        inputStream.close()
        outputStream.close()
        runOnUiThread {
            intake_temp_display.text = resultString
        }
        delay(100)
    }

    private suspend fun ambienttemp() {
        val outputStream = ByteArrayOutputStream();
        bluetoothClient.askAmbientTemp(outputStream)
        val inputStream = ByteArrayInputStream(outputStream.toByteArray())
        val resultString: String = inputStream.bufferedReader().use { it.readText() }
        Log.e("temp",resultString)
        yield()
        inputStream.close()
        outputStream.close()
        runOnUiThread {
            ambient_temp_display.text = resultString
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
        //mBluetoothAdapter.cancelDiscovery()
    }


}