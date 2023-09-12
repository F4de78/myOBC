package com.example.myobc

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.Manifest.permission.BLUETOOTH
import android.Manifest.permission.BLUETOOTH_ADMIN
import android.Manifest.permission.BLUETOOTH_CONNECT
import android.Manifest.permission.BLUETOOTH_SCAN
import android.app.Activity
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
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.AdapterView.OnItemClickListener
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat


class BluetoothActivity : AppCompatActivity(), View.OnClickListener {

    private val TAG = "DeviceListActivity"
    var EXTRA_DEVICE_ADDRESS = "device_address"

    val REQUEST_ENABLE_BT = 1000

    val BLUETOOTH_CODE = 1003

    val LOCATION_REQUEST_CODE = 1005

    private var bluetoothEnabled = false
    private var locationEnabled = false

    private val mReceiver = BluetoothDeviceReceiver()
    private lateinit var mBtAdapter: BluetoothAdapter
    private lateinit var mNewDevicesArrayAdapter: ArrayAdapter<String>

    private lateinit var progress: ProgressBar

    private lateinit var start_bt: Button
    private lateinit var stop_bt: Button

    // Function to check and request permission.
    @RequiresApi(Build.VERSION_CODES.S)
    private fun checkBtPermissions() {
        val requiredPermissions = arrayOf(
            BLUETOOTH,
            BLUETOOTH_ADMIN,
            //those two permission are needed in Android <12
            ACCESS_FINE_LOCATION,
            ACCESS_COARSE_LOCATION,
            //needed in Android 12>
            BLUETOOTH_CONNECT,
            BLUETOOTH_SCAN
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
        }
    }

    private fun isBluetoothEnabled(): Boolean {
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        return bluetoothAdapter != null && bluetoothAdapter.isEnabled
    }
    private fun isLocationEnabled(): Boolean {
        val locationMode: Int = Settings.Secure.getInt(
            contentResolver,
            Settings.Secure.LOCATION_MODE,
            Settings.Secure.LOCATION_MODE_OFF
        )
        return locationMode != Settings.Secure.LOCATION_MODE_OFF
    }

    private fun showLocationAlertDialog() {
        val builder = AlertDialog.Builder(this)
        builder.apply {
            setTitle("Enable Location")
            setMessage("Please turn on geolocation to use this app.")
            setPositiveButton("Settings") { _, _ ->
                val settingsIntent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivityForResult(settingsIntent, LOCATION_REQUEST_CODE)
            }
            setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
                //if dismiss, goes back to start activity
                startActivity(Intent(this@BluetoothActivity, MainActivity::class.java))
            }
        }
        val dialog = builder.create()
        dialog.show()
    }

    private fun showBluetoothAlertDialog() {
        val builder = AlertDialog.Builder(this)
        builder.apply {
            setTitle("Enable Bluetooth")
            setMessage("Please turn on Bluetooth to use this app.")
            setPositiveButton("Turn On") { _, _ ->
                val bluetoothIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                startActivityForResult(bluetoothIntent, REQUEST_ENABLE_BT)
            }
            setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
                //if dismiss, goes back to start activity
                startActivity(Intent(this@BluetoothActivity, MainActivity::class.java))
            }
        }
        val dialog = builder.create()
        dialog.show()
    }

    private fun initBluetooth() {
        // Set result CANCELED in case the user backs out
        setResult(RESULT_CANCELED)
        start_bt.isEnabled = false
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

    private fun doDiscovery() {
        Log.d(TAG, "doDiscovery()")

        progress.visibility = View.VISIBLE

        // If we're already discovering, stop it
        if (mBtAdapter.isDiscovering) {
            mBtAdapter.cancelDiscovery()
            progress.visibility = View.GONE
        }

        // Request discover from BluetoothAdapter
        mBtAdapter.startDiscovery()
        start_bt.isEnabled = false
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val bluetoothManager: BluetoothManager = getSystemService(BluetoothManager::class.java)
        mBtAdapter = bluetoothManager.adapter
        // Check if bluetooth is enabled
        if (!isBluetoothEnabled()) {
            showBluetoothAlertDialog()
        } else {
            bluetoothEnabled = true
        }

        // Check if geolocation is enabled
        if (!isLocationEnabled()) {
            showLocationAlertDialog()
        } else {
            locationEnabled = true
        }
        //if the bluetooh and the geolocation are activated, we can proceed
        setContentView(R.layout.activity_bluetooth)
        progress = findViewById<ProgressBar>(R.id.spinner)
        start_bt = findViewById<Button>(R.id.bt_start)
        stop_bt = findViewById<Button>(R.id.bt_stop)
        start_bt.setOnClickListener(this)
        stop_bt.setOnClickListener(this)
        //check the bluetooth permission
        checkBtPermissions()
        //if we have all the requested permission, we can start to initialize the bluetooth
        //connection and start scanning for OBD interfaces
        initBluetooth()
    }

    override fun onDestroy() {
        super.onDestroy()

        // Make sure we're not doing discovery anymore
        mBtAdapter.cancelDiscovery()

        // Unregister broadcast listeners
        unregisterReceiver(mReceiver)
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

    inner class BluetoothDeviceReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action

            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND == action) {
                // Get the BluetoothDevice object from the Intent
                val device =
                    intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                // If it's already paired, skip it, because it's been listed already
                if (device != null && device.bondState != BluetoothDevice.BOND_BONDED) {
                    mNewDevicesArrayAdapter.add(
                        """
                        ${device.name}
                        ${device.address}
                        """.trimIndent()
                    )
                }
                // When discovery is finished, change the Activity title
                start_bt.isEnabled = true
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED == action) {
                progress.visibility = View.GONE
                start_bt.isEnabled = true
                if (mNewDevicesArrayAdapter.count == 0) {
                    progress.visibility = View.GONE
                    start_bt.isEnabled = true
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
                    Log.d(TAG, "Permissions granted: initializing bt")
                } else {
                    Log.e(TAG, "Permission denied")
                }
            }
        }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_OK) {
                bluetoothEnabled = true
                // If both Bluetooth and location are enabled, initialize the activity
                if (locationEnabled) {
                    // Check the bluetooth permission
                    checkBtPermissions()
                    // If we have all the requested permissions, we can start initializing
                    // the bluetooth connection and start scanning for OBD interfaces
                    initBluetooth()
                }
            }
        } else if (requestCode == LOCATION_REQUEST_CODE) {
            if (isLocationEnabled()) {
                locationEnabled = true
                // If both Bluetooth and location are enabled, initialize the activity
                if (bluetoothEnabled) {
                    // Check the bluetooth permission
                    checkBtPermissions()
                    // If we have all the requested permissions, we can start initializing
                    // the bluetooth connection and start scanning for OBD interfaces
                    initBluetooth()
                }
            }
        }
    }

    override fun onClick(view: View?) {
        when(view?.id){
            R.id.bt_start->{
                initBluetooth()
            }
            R.id.bt_stop->{
                mBtAdapter.cancelDiscovery()
                progress.visibility = View.GONE
                //start_bt.isEnabled = false
            }
        }
    }



}