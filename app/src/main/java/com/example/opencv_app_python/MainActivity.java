package com.example.opencv_app_python;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.util.Log;
import java.util.List;
import android.content.ActivityNotFoundException;




public class MainActivity extends AppCompatActivity {

    private TextView tvBluetoothStatus;
    private Button btnCaptureAnalyze, btnHistory, btnSettings;

    private Button btnAbrirBotonera;


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
        setContentView(R.layout.activity_menu);

        // Asignación de vistas
        tvBluetoothStatus = findViewById(R.id.tvBluetoothStatus);
        btnCaptureAnalyze = findViewById(R.id.btnCaptureAnalyze);
        btnHistory = findViewById(R.id.btnHistory);
        btnSettings = findViewById(R.id.btnSettings);
        btnAbrirBotonera = findViewById(R.id.btnAbrirBotonera); // Nuevo botón

        // Botón: Cámara
        btnCaptureAnalyze.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, CameraActivity.class);
            startActivity(intent);
        });

        // Botón: Historial
        btnHistory.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, HistoryActivity.class);
            startActivity(intent);
        });

        // Botón: Configuración
        btnSettings.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            settingsLauncher.launch(intent);
        });

        btnAbrirBotonera = findViewById(R.id.btnAbrirBotonera);
        btnAbrirBotonera.setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setClassName("com.example.javibotonera", "com.example.javibotonera.Dispositivos");

            try {
                startActivity(intent);
            } catch (ActivityNotFoundException e) {
                Toast.makeText(this, "No se pudo encontrar la app javibotonera instalada", Toast.LENGTH_LONG).show();
                e.printStackTrace();
            }
        });



        updateBluetoothStatus(false);

        // Mostrar todos los paquetes instalados (debug)
        PackageManager pm = getPackageManager();
        List<ApplicationInfo> apps = pm.getInstalledApplications(0);

        for (ApplicationInfo app : apps) {
            Log.d("InstalledApp", "Package: " + app.packageName);
        }

    }

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
