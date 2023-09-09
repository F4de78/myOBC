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
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import java.time.Duration


class MainActivity : AppCompatActivity(), View.OnClickListener {
    //used for dynamically change menu entry title
    var menu: Menu? = null
    //displays gauges
    private lateinit var connection_status: TextView
    private lateinit var speed_display: TextView
    private lateinit var RPM_display: TextView
    private lateinit var coolant_display: TextView
    private lateinit var oil_temp_display: TextView
    private lateinit var intake_temp_display: TextView
    private lateinit var engine_load_display: TextView

    private var address: String = ""

    private lateinit var mBluetoothAdapter: BluetoothAdapter

    private lateinit var bluetoothClient: BluetoothClient

    // since some value takes a lot to change or can be the same for a long time,
    // (eg. the engine is idling) we check if the new value is the same as the one read
    // before and only update it if is different to save resources
    private var lastRPMValue: String = ""
    private var lastSpeedValue: String = ""
    private var lastOilValue: String = ""
    private var lastCoolantValue: String = ""
    private var lastIntakeValue: String = ""
    private var lastEngineLoad: String = ""

    private var connected: Boolean = false
    private var read: Boolean = true
    private var log: Boolean = false

    private lateinit var stop: Button
    private lateinit var file: CsvLog

    private lateinit var job: Job

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        //keeps the screen always on
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
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
        engine_load_display = findViewById<TextView>(R.id.fuel_consumption_display)

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
                    menu?.findItem(R.id.rec)?.title = "Stop logging";
                    try{
                        file = CsvLog(
                            "OBDOBC_log_${System.currentTimeMillis() / 1000}.csv",
                            applicationContext
                        )
                        file.makeHeader()
                        log = true
                    }catch(e:Exception){
                        Toast.makeText(this,"Error: $e",Toast.LENGTH_LONG).show()
                    }

                }else{
                    menu?.findItem(R.id.rec)?.title = "Start logging";
                    log = false
                    Toast.makeText(this,"Stop logging\nFile saved in ${file.path}" +
                            ".",Toast.LENGTH_LONG).show()
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
                    val device: BluetoothDevice = mBluetoothAdapter.getRemoteDevice(address)
                    bluetoothClient = BluetoothClient(device)
                    runOnUiThread{
                        connection_status.text = getString(R.string.connecting,address)
                    }
                    // Connect to selected device
                    connected = bluetoothClient.connect()
                    read = true
                    display()

                } catch (e: Exception) {
                    connection_status.text = getString(R.string.connection_error)
                    e.printStackTrace()
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private suspend fun display() {
        if (connected) {
            connection_status.text = getString(R.string.connected)
            coroutineScope{
                job = launch {
                    while (read){
                        yield()
                        updateUI()
                    }
                }
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
        val fuelconsumption_ret = engineLoad()
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
            lastEngineLoad = fuelconsumption_ret
            runOnUiThread {
                engine_load_display.text = fuelconsumption_ret
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

    private suspend fun engineLoad(): String{
        return withContext(Dispatchers.IO){
            bluetoothClient.askEngineLoad()
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
        RPM_display.text = getString(R.string.default_display)
        speed_display.text = getString(R.string.default_display)
        coolant_display.text = getString(R.string.default_display)
        oil_temp_display.text = getString(R.string.default_display)
        intake_temp_display.text = getString(R.string.default_display)
        engine_load_display.text = getString(R.string.default_display)
    }

    private fun resetLastState(){
        lastRPMValue = ""
        lastSpeedValue = ""
        lastOilValue = ""
        lastCoolantValue = ""
        lastIntakeValue= ""
        lastEngineLoad= ""
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun stop(){
        if(read){
            read = false
            if (log)
                Toast.makeText(this,"Stop logging\nFile saved in ${file?.path}" +
                        ".",Toast.LENGTH_LONG).show()
            log = false
            CoroutineScope(Dispatchers.Default).launch{
                job.cancel()
                job.join()
                bluetoothClient.disconnect()
                delay(500)
            }
            resetDisplays()
            resetLastState()
            connection_status.text = getString(R.string.not_connected)
        }
    }
    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onClick(view: View?) {
        when(view?.id){
            R.id.stop->{
                stop()
                Toast.makeText(this,"Disconnected from OBD.",Toast.LENGTH_SHORT).show()
            }
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        read = false
        log = false
    }


}