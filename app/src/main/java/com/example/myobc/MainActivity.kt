package com.example.myobc

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class MainActivity : AppCompatActivity(), View.OnClickListener {
    var menu: Menu? = null
    /*displays gauges*/
    private lateinit var connection_status: TextView
    private lateinit var speed_display: TextView
    private lateinit var RPM_display: TextView
    private lateinit var coolant_display: TextView
    private lateinit var oil_temp_display: TextView
    private lateinit var intake_temp_display: TextView
    private lateinit var fuel_consumption_display: TextView

    private var address: String = ""

    private var mBluetoothAdapter: BluetoothAdapter? = null

    private lateinit var bluetoothClient: BluetoothClient

    // since some value takes a lot to change or can be the same for a long time,
    // (eg. the engine is idling) we check if the new value is the same as the one read
    // before and only update it if is different to save resources
    private var lastRPMValue: String = ""
    private var lastSpeedValue: String = ""
    private var lastOilValue: String = ""
    private var lastCoolantValue: String = ""
    private var lastIntakeValue: String = ""
    private var fuelConsumptionValue: String = ""

    private var connected: Boolean = false
    private var read: Boolean = true
    private var log: Boolean = false

    private lateinit var stop: Button

    private var default_text = "_._"

    private lateinit var file: CsvLog
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
        fuel_consumption_display = findViewById<TextView>(R.id.fuel_consumption_display)

        stop = findViewById<Button>(R.id.stop)
        stop.setOnClickListener(this)

        connection_status.text = getString(R.string.not_connected)


    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu , menu)
        this.menu = menu;
        return true
    }


    @RequiresApi(Build.VERSION_CODES.Q)
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
            R.id.rec -> {
                if (!log) {
                    //invalidateOptionsMenu();
                    //findViewById<TextView>(R.id.rec).text = "Start logging"
                    menu?.findItem(R.id.rec)?.title = "Stop logging";
                    file = CsvLog(
                        "OBDOBC_log_${System.currentTimeMillis() / 1000}",
                        applicationContext
                    )
                    file.initCSV()
                    log = true
                }else{
                    //invalidateOptionsMenu();
                    //findViewById<TextView>(R.id.rec).text = "Stop logging"
                    menu?.findItem(R.id.rec)?.title = "Start logging";
                    log = false
                }
            }
        }
        return false
    }
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun connect() {
        if (address != "") {
            CoroutineScope(Dispatchers.Main).launch {
                try {
                    mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
                    val device: BluetoothDevice = mBluetoothAdapter!!.getRemoteDevice(address)
                    bluetoothClient = BluetoothClient(device)
                    connection_status.text = getString(R.string.connecting,address)

                    // Connect to selected device
                    connected = bluetoothClient.connect()
                    read = true
                    display()

                } catch (e: Exception) {
                    // Handle exceptions appropriately
                    e.printStackTrace()
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private suspend fun display() {
        if (connected) {
            connection_status.text = getString(R.string.connected)
            withContext(Dispatchers.Default) {
                while(read)
                    updateUI()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private suspend fun updateUI(){
        //since we have to run all UI update operation in Main thread, we first retrieve information
        //from the IO threads, and then update it sequentially on the Main thread (UI thread)
        val RPM_ret = RPM()
        val speed_ret = speed()
        val coolant_ret = coolant()
        val oil_ret = oiltemp()
        val intake_ret = intakeTemp()
        val fuelconsumption_ret = fuelConsumption()
        //update RPM display
        if (RPM_ret != lastRPMValue) {
            lastRPMValue = RPM_ret
            runOnUiThread {
                RPM_display.text = RPM_ret
            }
        }
        //update speed display
        if (speed_ret != lastSpeedValue) {
            lastSpeedValue = speed_ret
            runOnUiThread {
                speed_display.text = speed_ret
            }
        }

        if (coolant_ret != lastCoolantValue) {
            lastCoolantValue = coolant_ret
            runOnUiThread {
                coolant_display.text = coolant_ret
            }
        }

        if (oil_ret != lastOilValue) {
            lastOilValue = oil_ret
            runOnUiThread {
                oil_temp_display.text = oil_ret
            }
        }

        if (intake_ret != lastIntakeValue) {
            lastIntakeValue = intake_ret
            runOnUiThread {
                intake_temp_display.text = intake_ret
            }
        }

        if (fuelconsumption_ret != lastCoolantValue) {
            fuelConsumptionValue = fuelconsumption_ret
            runOnUiThread {
                fuel_consumption_display.text = fuelconsumption_ret
            }
        }
        //if the user is recording data
        withContext(Dispatchers.IO){
            if(log){
                file.appendRow(listOf(
                    (System.currentTimeMillis()/1000).toString(),//timestamp
                    RPM_ret,
                    speed_ret,
                    coolant_ret,
                    oil_ret,
                    intake_ret,
                    fuelconsumption_ret))
            }
        }

    }

    private suspend fun RPM(): String{
        return withContext(Dispatchers.IO){
           bluetoothClient.askRPM()
        }
    }

    private suspend fun speed(): String{
        return withContext(Dispatchers.IO){
            bluetoothClient.askSpeed()
        }
    }

    private suspend fun coolant(): String{
        return withContext(Dispatchers.IO){
            bluetoothClient.askCoolantTemp()
        }
    }

    private suspend fun oiltemp(): String{
        return withContext(Dispatchers.IO){
            bluetoothClient.askOilTemp()
        }
    }

    private suspend fun intakeTemp(): String{
        return withContext(Dispatchers.IO){
            bluetoothClient.askIntakeTemp()
        }
    }

    private suspend fun fuelConsumption(): String{
        return withContext(Dispatchers.IO){
            bluetoothClient.askFuelConsumptionTemp()
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
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
                connection_status.text = getString(R.string.connecting_address,address)
                connect()
            }
        }
    }

    private fun resetDisplays(){
        RPM_display.text = default_text
        speed_display.text = default_text
        coolant_display.text = default_text
        oil_temp_display.text = default_text
        intake_temp_display.text = default_text
        fuel_consumption_display.text = default_text
    }

    private fun stop(){
        if(read){
            read = false
            log = false
            CoroutineScope(Dispatchers.IO).launch{
                bluetoothClient.disconnect()
                delay(500)
            }
            resetDisplays()
            connection_status.text = getString(R.string.not_connected)
        }
    }
    override fun onClick(view: View?) {
        when(view?.id){
            R.id.stop->{
                stop()
            }
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        read = false
        log = false
    }


}