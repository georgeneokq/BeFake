package com.georgeneokq.befake

import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.georgeneokq.befake.util.Util

class SettingsActivity : AppCompatActivity() {
    private lateinit var prefs: SharedPreferences
    private lateinit var btnResetSettings: Button
    private lateinit var btnConfirm: Button
    private lateinit var editWatermarkText: EditText
    private lateinit var editWatermarkColor: EditText
    private lateinit var editWatermarkAlpha: EditText
    private lateinit var editWatermarkSize: EditText
    private lateinit var editBorderColor: EditText
    private lateinit var editBorderAlpha: EditText
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        prefs = getSharedPreferences(Globals.SHARED_PREFERENCES_NAME, MODE_PRIVATE)

        editWatermarkText = findViewById(R.id.editWatermarkText)
        editWatermarkColor = findViewById(R.id.editWatermarkColor)
        editWatermarkAlpha = findViewById(R.id.editWatermarkAlpha)
        editWatermarkSize = findViewById(R.id.editWatermarkSize)
        editBorderColor = findViewById(R.id.editBorderColor)
        editBorderAlpha = findViewById(R.id.editBorderAlpha)
        btnResetSettings = findViewById(R.id.btnResetSettings)
        btnConfirm = findViewById(R.id.btnConfirm)

        btnResetSettings.setOnClickListener {
            Util.vibrateTapLight(this)
            resetSettings()
        }
        btnConfirm.setOnClickListener { confirm() }

        // Fill values from shared preferences
        val watermarkText = prefs.getString("watermarkText", "")
        val watermarkColor = prefs.getString("watermarkColor", "")
        val watermarkAlpha = prefs.getInt("watermarkAlpha", 100)
        val watermarkSize = prefs.getInt("watermarkSize", 58)
        val borderColor = prefs.getString("borderColor", "")
        val borderAlpha = prefs.getInt("borderAlpha", 100)

        editWatermarkText.setText(watermarkText)
        editWatermarkColor.setText(watermarkColor)
        editWatermarkAlpha.setText(watermarkAlpha.toString())
        editWatermarkSize.setText(watermarkSize.toString())
        editBorderColor.setText(borderColor)
        editBorderAlpha.setText(borderAlpha.toString())
    }

    private fun resetSettings() {
        Util.resetSettings(this)
        finish()
    }

    private fun confirm() {
        // Get settings' values
        val watermarkText = editWatermarkText.text.toString()
        val watermarkColor = editWatermarkColor.text.toString()
        val watermarkAlpha = editWatermarkAlpha.text.toString().toInt()
        val watermarkSize = editWatermarkSize.text.toString().toInt()
        val borderColor = editBorderColor.text.toString()
        val borderAlpha = editBorderAlpha.text.toString().toInt()

        // Save to SharedPreferences
        val editor = prefs.edit()
        editor.putString("watermarkText", watermarkText)
        editor.putString("watermarkColor", watermarkColor)
        editor.putInt("watermarkAlpha", watermarkAlpha)
        editor.putInt("watermarkSize", watermarkSize)
        editor.putString("borderColor", borderColor)
        editor.putInt("borderAlpha", borderAlpha)
        editor.apply()
        finish()
    }
}