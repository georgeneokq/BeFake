package com.georgeneokq.befake;

import androidx.appcompat.app.AppCompatActivity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

import com.georgeneokq.befake.util.Util;

public class SettingsActivity extends AppCompatActivity {

    private SharedPreferences prefs;

    private Button btnResetSettings, btnConfirm;
    private EditText editWatermarkText, editWatermarkColor, editWatermarkAlpha, editBorderColor, editBorderAlpha;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        prefs = getSharedPreferences(Globals.SHARED_PREFERENCES_NAME, MODE_PRIVATE);

        editWatermarkText = findViewById(R.id.editWatermarkText);
        editWatermarkColor = findViewById(R.id.editWatermarkColor);
        editWatermarkAlpha = findViewById(R.id.editWatermarkAlpha);
        editBorderColor = findViewById(R.id.editBorderColor);
        editBorderAlpha = findViewById(R.id.editBorderAlpha);
        btnResetSettings = findViewById(R.id.btnResetSettings);
        btnConfirm = findViewById(R.id.btnConfirm);

        btnResetSettings.setOnClickListener(v -> resetSettings());
        btnConfirm.setOnClickListener(v -> confirm());

        // Fill values from shared preferences
        String watermarkText = prefs.getString("watermarkText", "");
        String watermarkColor = prefs.getString("watermarkColor", "");
        int watermarkAlpha = prefs.getInt("watermarkAlpha", 100);
        String borderColor = prefs.getString("borderColor", "");
        int borderAlpha = prefs.getInt("borderAlpha", 100);

        editWatermarkText.setText(watermarkText);
        editWatermarkColor.setText(watermarkColor);
        editWatermarkAlpha.setText(String.valueOf(watermarkAlpha));
        editBorderColor.setText(borderColor);
        editBorderAlpha.setText(String.valueOf(borderAlpha));
    }

    private void resetSettings() {
        Util.resetSettings(this);
        finish();
    }

    private void confirm() {
        // Get settings' values
        String watermarkText = editWatermarkText.getText().toString();
        String watermarkColor = editWatermarkColor.getText().toString();
        int watermarkAlpha = Integer.parseInt(editWatermarkAlpha.getText().toString());
        String borderColor = editBorderColor.getText().toString();
        int borderAlpha = Integer.parseInt(editBorderAlpha.getText().toString());

        // Save to SharedPreferences
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("watermarkText", watermarkText);
        editor.putString("watermarkColor", watermarkColor);
        editor.putInt("watermarkAlpha", watermarkAlpha);
        editor.putString("borderColor", borderColor);
        editor.putInt("borderAlpha", borderAlpha);
        editor.apply();

        finish();
    }
}