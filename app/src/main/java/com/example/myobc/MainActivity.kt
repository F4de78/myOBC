package com.example.myobc

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class MainActivity : AppCompatActivity() {
    /*displays an gauges*/
    private lateinit var connection_status: TextView
    private lateinit var speed_display: TextView
    private lateinit var RPM_display: TextView

    private var address: String = ""

    private lateinit var mBluetoothAdapter: BluetoothAdapter

    private lateinit var bluetoothClient: BluetoothClient


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        connection_status = findViewById<TextView>(R.id.connection_indicator)
        speed_display = findViewById<TextView>(R.id.speed_display)
        RPM_display = findViewById<TextView>(R.id.RPM_display)

        connection_status.text = "Disconnected"
    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu , menu);
        return true
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

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
            val device: BluetoothDevice = mBluetoothAdapter.getRemoteDevice(address)
            bluetoothClient = BluetoothClient(device)
            connection_status.text = "Connected to $address, connecting to ECU..."

            CoroutineScope(Dispatchers.Main).launch {
                val connected = withContext(Dispatchers.IO) {
                    bluetoothClient.connect()
                }
                if (connected) {
                    connection_status.text = "Connected to ECU."
                    while (true) {
                        bluetoothClient.askRPM()
                        val data = withContext(Dispatchers.Default) {
                            bluetoothClient.readRPM()
                        }
                        Log.d("Bluetooth", "Received data: $data")
                        RPM_display.text = data
                        delay(1000)
                    }
                }
            }
        }
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

    }


}