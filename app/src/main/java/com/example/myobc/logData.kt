package com.example.myobc

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.ContentValues
import android.os.Build
import android.os.Environment
import android.util.Log
import java.io.File
import java.io.FileWriter
import java.io.IOException

import android.provider.MediaStore
import android.content.Context
import androidx.annotation.RequiresApi


@SuppressLint("Recycle")
@RequiresApi(Build.VERSION_CODES.Q)
class CsvLog(private val filename: String, private val context: Context) {
        private val separator = "," // CSV separator
        private lateinit var logFile: File
        init {
            val resolver = context.contentResolver
            val values = ContentValues()

            values.put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            values.put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS + "/" + "myOBC")
            val uri = resolver.insert(MediaStore.Files.getContentUri("external"), values)
            if (uri != null) {
                resolver.openFile(uri,"w",null)
            }


        }

        fun addColumn(columnName: String) {
            try {
                FileWriter(filename, true).use { writer ->
                    writer.append(columnName)
                    writer.append(separator)
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        fun appendRow(values: List<String>) {
            try {
                FileWriter(filename, true).use { writer ->
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

        //function to initialize the csv with the default header
        fun initCSV(){
            this.addColumn("timestamp")
            this.addColumn("RPM")
            this.addColumn("speed")
            this.addColumn("coolant_temp")
            this.addColumn("oil_temp")
            this.addColumn("intake_air")
            this.addColumn("fuel_consumption")
        }
    }
