package com.example.myobc

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileOutputStream
import java.io.FileWriter
import java.io.IOException

@SuppressLint("Recycle")
@RequiresApi(Build.VERSION_CODES.Q)
class CsvLog(private val filename: String, private val context: Context) {
    private val separator = "," // CSV separator
    private lateinit var logFile: File

    init {
        val externalStorageVolumes = ContextCompat.getExternalFilesDirs(
            context, Environment.DIRECTORY_DOCUMENTS
        )
        val path = externalStorageVolumes[0]
        logFile = File(path, filename)
        Log.d("log", "saved in: $logFile")
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
    fun initCSV() {
        this.addColumn("timestamp")
        this.addColumn("RPM")
        this.addColumn("speed")
        this.addColumn("coolant_temp")
        this.addColumn("oil_temp")
        this.addColumn("intake_air")
        this.addColumn("fuel_consumption")
    }

}
