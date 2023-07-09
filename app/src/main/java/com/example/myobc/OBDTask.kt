package com.example.myobc

import android.util.Log
import com.github.eltonvs.obd.command.engine.SpeedCommand
import com.github.eltonvs.obd.connection.ObdDeviceConnection
import java.io.InputStream
import java.io.OutputStream

private const val TAG = "MY_APP_DEBUG_TAG"

class OBDTask(inputStream: InputStream, outputStream: OutputStream){

    val ins = inputStream
    val outs = outputStream
    private lateinit var obdConnection: ObdDeviceConnection

    fun connect() {
        obdConnection =  ObdDeviceConnection(ins, outs)
    }

    suspend fun getSpeed(){
        val response = obdConnection.run(SpeedCommand())
        Log.e(TAG,"Speed: $response")
    }
}