package com.example.opencv_app_python;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private TextView tvBluetoothStatus;
    private Button btnCaptureAnalyze, btnHistory, btnSettings;

    // ActivityResultLauncher para manejar el retorno desde SettingsActivity
    private final ActivityResultLauncher<Intent> settingsLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    boolean isBluetoothConnected = result.getData().getBooleanExtra("bluetoothStatus", false);
                    updateBluetoothStatus(isBluetoothConnected);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu); // Cargar el nuevo menú principal

        // Asignación de vistas
        tvBluetoothStatus = findViewById(R.id.tvBluetoothStatus);
        btnCaptureAnalyze = findViewById(R.id.btnCaptureAnalyze);
        btnHistory = findViewById(R.id.btnHistory);
        btnSettings = findViewById(R.id.btnSettings);

        // Configurar eventos de los botones
        btnCaptureAnalyze.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, CameraActivity.class);
            startActivity(intent);
        });

        btnHistory.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, HistoryActivity.class);
            startActivity(intent);
        });

        btnSettings.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            settingsLauncher.launch(intent); // Usar el launcher en vez de startActivity
        });

        // Simulación del estado del Bluetooth
        updateBluetoothStatus(false);
    }

    /**
     * Actualiza el estado del Bluetooth en la pantalla.
     * @param isConnected True si está conectado, False si no.
     */
    private void updateBluetoothStatus(boolean isConnected) {
        if (isConnected) {
            tvBluetoothStatus.setText("🔵 Conectado a Arduino");
            tvBluetoothStatus.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
        } else {
            tvBluetoothStatus.setText("🔴 No Conectado a Arduino");
            tvBluetoothStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        }
    }
}
