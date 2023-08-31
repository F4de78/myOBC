package com.example.myobc

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.Manifest.permission.BLUETOOTH
import android.Manifest.permission.BLUETOOTH_ADMIN
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.AdapterView.OnItemClickListener
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat


class BluetoothActivityLocation : AppCompatActivity() {

    private val TAG = "DeviceListActivity"
    var EXTRA_DEVICE_ADDRESS = "device_address"
    val REQUEST_ENABLE_BT = 1000

    val BLUETOOTH_CONNECT_CODE = 1001
    val BLUETOOTH_SCAN_CODE = 1002
    val BLUETOOTH_CODE = 1003
    val BLUETOOTH_ADMIN_CODE = 1004

    private lateinit var mBtAdapter: BluetoothAdapter
    private lateinit var mNewDevicesArrayAdapter: ArrayAdapter<String>

    // Function to check and request permission.
    private fun checkBtPermissions() {


        val requiredPermissions = arrayOf(
            BLUETOOTH,
            BLUETOOTH_ADMIN,
            ACCESS_FINE_LOCATION,
            ACCESS_COARSE_LOCATION
        )

        val permissionsToRequest = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissionsToRequest.toTypedArray(),
                BLUETOOTH_CODE
            )
        } else {
            initBluetooth()
        }
    }


    private fun initBluetooth() {
        val bluetoothManager: BluetoothManager = getSystemService(BluetoothManager::class.java)
        mBtAdapter = bluetoothManager.adapter

        if (!mBtAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        }

        // Set result CANCELED in case the user backs out
        setResult(RESULT_CANCELED)



        val pairedDevicesArrayAdapter = ArrayAdapter<String>(this, android.R.layout.simple_list_item_1)
        mNewDevicesArrayAdapter = ArrayAdapter<String>(this, android.R.layout.simple_list_item_1)


        // Find and set up the ListView for paired devices
        val pairedListView = findViewById<ListView>(R.id.bt_list_p)
        pairedListView.adapter = pairedDevicesArrayAdapter
        pairedListView.onItemClickListener = mDeviceClickListener


        // Find and set up the ListView for newly discovered devices
        val newDevicesListView = findViewById<ListView>(R.id.bt_list_d)
        newDevicesListView.adapter = mNewDevicesArrayAdapter
        newDevicesListView.onItemClickListener = mDeviceClickListener

        // Register for broadcasts when a device is discovered
        var filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        this.registerReceiver(mReceiver, filter)

        // Register for broadcasts when discovery has finished
        filter = IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        this.registerReceiver(mReceiver, filter)

        // Get the local Bluetooth adapter
        //mBtAdapter = BluetoothAdapter.getDefaultAdapter()



        // Get a set of currently paired devices
        val pairedDevices = mBtAdapter.bondedDevices

        // If there are paired devices, add each one to the ArrayAdapter
        if (pairedDevices.size > 0) {
            for (device in pairedDevices) {
                pairedDevicesArrayAdapter.add(
                    """
                    ${device.name}
                    ${device.address}
                    """.trimIndent()
                )
            }
        } else {
            pairedDevicesArrayAdapter.add("No device")
        }
        doDiscovery()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bluetooth)
        checkBtPermissions()


    }

    override fun onDestroy() {
        super.onDestroy()

        // Make sure we're not doing discovery anymore
        mBtAdapter.cancelDiscovery()

        // Unregister broadcast listeners
        unregisterReceiver(mReceiver)
    }

    /**
     * Start device discover with the BluetoothAdapter
     */
    private fun doDiscovery() {
        Log.d(TAG, "doDiscovery()")

        //checkPermission(BLUETOOTH_SCAN,BLUETOOTH_SCAN_CODE)
        // Indicate scanning in the title
        setSupportProgressBarIndeterminateVisibility(true)
        //setTitle(R.string.scanning)

        // Turn on sub-title for new devices
        //findViewById<View>(R.id.title_new_devices).visibility = View.VISIBLE

        // If we're already discovering, stop it
        if (mBtAdapter.isDiscovering) {
            mBtAdapter.cancelDiscovery()
        }

        // Request discover from BluetoothAdapter
        mBtAdapter.startDiscovery()
    }

    /**
     * The on-click listener for all devices in the ListViews
     */
    private val mDeviceClickListener =
        OnItemClickListener { _, v, _, _ ->
            // Cancel discovery because it's costly and we're about to connect

            mBtAdapter.cancelDiscovery()

            // Get the device MAC address, which is the last 17 chars in the View
            val info = (v as TextView).text.toString()
            val address = info.substring(info.length - 17)

            // Create the result Intent and include the MAC address
            val intent = Intent()
            intent.putExtra(EXTRA_DEVICE_ADDRESS,address)

            // Set result and finish this Activity
            setResult(RESULT_OK, intent)
            finish()
        }

    private val mReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action

            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND == action) {
                // Get the BluetoothDevice object from the Intent
                val device =
                    intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                // If it's already paired, skip it, because it's been listed already
                if (device != null && device.bondState != BluetoothDevice.BOND_BONDED) {
                    mNewDevicesArrayAdapter!!.add(
                        """
                        ${device.name}
                        ${device.address}
                        """.trimIndent()
                    )
                }
                // When discovery is finished, change the Activity title
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED == action) {
                setProgressBarIndeterminateVisibility(false)
                if (mNewDevicesArrayAdapter.count == 0) {
                    mNewDevicesArrayAdapter.add("noDevices")
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == BLUETOOTH_CODE) {
                if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                    initBluetooth()
                } else {
                    Log.e(TAG, "Permission denied")
                }
            }
        }



}