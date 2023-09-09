package com.example.myobc

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.lang.Exception

@SuppressLint("Recycle")
@RequiresApi(Build.VERSION_CODES.Q)
open class CsvLog(private val filename: String, private val context: Context) {
    private val separator = "," // CSV separator
    private lateinit var logFile: File
    var path: File

    init {

        val externalStorageVolumes = ContextCompat.getExternalFilesDirs(
            context, Environment.DIRECTORY_DOCUMENTS
        )
        path = externalStorageVolumes[0]
        if(isExternalStorageReadable(path)){
            logFile = File(path, filename)
            Log.d("log", "saved in: $logFile")
        }

    }

    private fun isExternalStorageReadable(path: File?): Boolean {
        val state: String = Environment.getExternalStorageState(path)
        if(Environment.MEDIA_MOUNTED != state){
            throw Exception("Extmem not available")
        }else{
            return true
        }
    }

    fun addColumn(columnName: String) {
        try {
            FileWriter(logFile, true).use { writer ->
                writer.append(columnName)
                writer.append(separator)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun appendRow(values: List<String>) {
        try {
            FileWriter(logFile, true).use { writer ->
                for (value in values) {
                    writer.append(value)
                    writer.append(separator)
                }
                writer.append("\n") // Add a new line at the end of the row
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    // Function to initialize the CSV with the default header
    fun makeHeader() {
        this.addColumn("timestamp")
        this.addColumn("RPM")
        this.addColumn("speed")
        this.addColumn("coolant_temp")
        this.addColumn("oil_temp")
        this.addColumn("intake_air")
        this.addColumn("fuel_consumption")
    }

}
