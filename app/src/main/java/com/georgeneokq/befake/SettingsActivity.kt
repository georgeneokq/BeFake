package com.georgeneokq.befake

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.activity.OnBackPressedCallback
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
    private lateinit var radioGroup: RadioGroup
    private lateinit var radioMinimizeLatency: RadioButton
    private lateinit var radioMaximizeQuality: RadioButton

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

        radioGroup = findViewById(R.id.radioGroup)
        radioMinimizeLatency = findViewById(R.id.radioMinimizeLatency)
        radioMaximizeQuality = findViewById(R.id.radioMaximizeQuality)

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

        // Retrieve and set the radio button state based on saved setting
        val highQuality = prefs.getBoolean("highQuality", true)
        if (highQuality) {
            radioMaximizeQuality.isChecked = true
        } else {
            radioMinimizeLatency.isChecked = true
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                startActivity(Intent(this@SettingsActivity, MainActivity::class.java))
                finish()
            }
        })
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

        // Get the selected performance mode
        val selectedRadioButtonId = radioGroup.checkedRadioButtonId
        val highQuality = selectedRadioButtonId == R.id.radioMaximizeQuality

        // Save to SharedPreferences
        val editor = prefs.edit()
        editor.putString("watermarkText", watermarkText)
        editor.putString("watermarkColor", watermarkColor)
        editor.putInt("watermarkAlpha", watermarkAlpha)
        editor.putInt("watermarkSize", watermarkSize)
        editor.putString("borderColor", borderColor)
        editor.putInt("borderAlpha", borderAlpha)
        editor.putBoolean("highQuality", highQuality)
        editor.apply()

        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
