package com.example.myobc

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat


class BluetoothActivity : AppCompatActivity() {

    private lateinit var bluetoothManager: BluetoothManager
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var devicesArrayAdapter: ArrayAdapter<String>
    private val discoveredDevices = mutableSetOf<BluetoothDevice>()

    private lateinit var bluetoothLeScanner: BluetoothLeScanner
    private lateinit var scanCallback: ScanCallback

    companion object {
        private const val REQUEST_ENABLE_BT = 1000
        private const val REQUEST_PERMISSION_ACCESS_FINE_LOCATION = 1002
        private val BLUETOOTH_SCAN_CODE = 1003
        private val BLUETOOTH_CONNECT_CODE = 1004
        private val REQUEST_PERMISSION_ACCESS_COARSE_LOCATION = 1005
        private val BLUETOOTH_CONNECTION_CODE = 1005
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bluetooth)

        // Initialize Bluetooth adapter like Android BT API docs
        bluetoothManager = getSystemService(BluetoothManager::class.java)
        bluetoothAdapter = bluetoothManager.adapter

        // Initialize the list view and its adapter
        val devicesListView = findViewById<ListView>(R.id.bt_list)
        devicesArrayAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1)
        devicesListView.adapter = devicesArrayAdapter

        // Check if Bluetooth is enabled
        if (!bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            //TODO: same thing for localization
            requestEnableBluetooth.launch(enableBtIntent)
        } else {
            // Bluetooth is enabled, start scanning for devices
            startScanning()
        }

        // Request permission for location access on Android 11 and above
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_PERMISSION_ACCESS_FINE_LOCATION
            )
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        //bt-scanning is a heavy battery consuming, so we stop to scan on destroy
        bluetoothLeScanner.stopScan(scanCallback)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSION_ACCESS_FINE_LOCATION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, start scanning for devices
                startScanning()
            } else {
                Toast.makeText(
                    this,
                    "Permission denied. Unable to scan for devices.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private val requestEnableBluetooth =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                // Bluetooth is enabled, start scanning for devices
                startScanning()
            } else {
                Toast.makeText(this, "Bluetooth is required to scan for devices.", Toast.LENGTH_SHORT).show()
                finish()
            }
        }

    private fun startScanning() {
        // Clear the list view and discovered devices set
        devicesArrayAdapter.clear()
        discoveredDevices.clear()

        // Start scanning for devices
        bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner
        scanCallback = object : ScanCallback() {
            @RequiresApi(Build.VERSION_CODES.S)
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val device = result.device
                if (!discoveredDevices.contains(device)) {
                    discoveredDevices.add(device)
                    //askForPermission(applicationContext, Manifest.permission.BLUETOOTH_CONNECT, BLUETOOTH_CONNECTION_CODE)
                    devicesArrayAdapter.add("${device.name} (${device.address})" ?: "Unnamed Device")
                }
            }
        }

        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        bluetoothLeScanner.startScan(null, scanSettings, scanCallback)
    }

}