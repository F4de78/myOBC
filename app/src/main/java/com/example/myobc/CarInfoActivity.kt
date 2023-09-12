package com.example.myobc

import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity


class CarInfoActivity: AppCompatActivity(), View.OnClickListener {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_carinfo)

        findViewById<Button>(R.id.save_button).setOnClickListener(this)
        findViewById<Button>(R.id.reset_button).setOnClickListener(this)

        val sharedPrefFile = "com.example.myobc"
        val mPref = getSharedPreferences(sharedPrefFile, MODE_PRIVATE)

        val mName = mPref.getString("carname","")
        findViewById<EditText>(R.id.car_man).setText(mName)
        val mModel = mPref.getString("carmodel","")
        findViewById<EditText>(R.id.car_model).setText(mModel)
        val mCC = mPref.getString("carcc","")
        findViewById<EditText>(R.id.car_cc).setText(mCC)
    }

    override fun onClick(view: View?) {
        val sharedPrefFile = "com.example.myobc"
        val mPreferences = getSharedPreferences(sharedPrefFile, MODE_PRIVATE)
        when(view?.id){
            R.id.save_button->{
                val preferencesEditor = mPreferences!!.edit()
                preferencesEditor.putString("carname", findViewById<EditText>(R.id.car_man).text.toString())
                preferencesEditor.putString("carmodel", findViewById<EditText>(R.id.car_model).text.toString())
                preferencesEditor.putString("carcc", findViewById<EditText>(R.id.car_cc).text.toString())
                preferencesEditor.apply()
                Toast.makeText(this,"Information saved.", Toast.LENGTH_SHORT).show()

            }
            R.id.reset_button->{
                val preferencesEditor: SharedPreferences.Editor = mPreferences.edit()
                preferencesEditor.clear()
                preferencesEditor.apply()
                findViewById<EditText>(R.id.car_man).setText("")
                findViewById<EditText>(R.id.car_model).setText("")
                findViewById<EditText>(R.id.car_cc).setText("")
                Toast.makeText(this,"Information resetted.", Toast.LENGTH_SHORT).show()

            }

        }
    }
}