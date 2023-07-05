package com.example.myobc
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.util.Log
import android.widget.Toast
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*

class ConnectBluetoothTask(private val device: BluetoothDevice, private val btAdapter : BluetoothAdapter) : Thread() {

    companion object {
        private const val TAG = "ConnectBluetoothTask"
        private val SERVICE_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }


    private var socket: BluetoothSocket? = null

    private val mmSocket: BluetoothSocket? by lazy(LazyThreadSafetyMode.NONE) {
        device.createRfcommSocketToServiceRecord(SERVICE_UUID)
    }


    override fun run() {
        btAdapter?.cancelDiscovery()


        mmSocket?.let { socket ->
            // Connect to the remote device through the socket. This call blocks
            // until it succeeds or throws an exception.
            socket.connect()
            Log.e(TAG, "Connected?")
            val mmInStream: InputStream = socket.inputStream
            val mmOutStream: OutputStream = socket.outputStream

        }

    }

    fun cancel() {
        try {
            socket?.close()
        } catch (e: IOException) {
            Log.e(TAG, "Could not close the client socket", e)
        }
    }
}