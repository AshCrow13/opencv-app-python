package com.example.opencv_app_python;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.util.ArrayList;
import java.util.Set;

public class SettingsActivity extends AppCompatActivity {

    private static final int REQUEST_DIRECTORY_PICKER = 1;
    private Switch switchAutoRecognition;
    private Button btnSelectStorage, btnConnectBluetooth;
    private Spinner spinnerBluetoothDevices;
    private SharedPreferences sharedPreferences;
    private BluetoothAdapter bluetoothAdapter;
    private boolean bluetoothConnected = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Inicializar vistas
        switchAutoRecognition = findViewById(R.id.switchAutoRecognition);
        btnSelectStorage = findViewById(R.id.btnSelectStorage);
        spinnerBluetoothDevices = findViewById(R.id.spinnerBluetoothDevices);
        btnConnectBluetooth = findViewById(R.id.btnConnectBluetooth);

        // Configuración de SharedPreferences
        sharedPreferences = getSharedPreferences("AppSettings", MODE_PRIVATE);
        switchAutoRecognition.setChecked(sharedPreferences.getBoolean("autoRecognition", false));

        // Listener para guardar cambios en Switch
        switchAutoRecognition.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("autoRecognition", isChecked);
            editor.apply();
            Toast.makeText(this, "Reconocimiento Automático " + (isChecked ? "Activado" : "Desactivado"), Toast.LENGTH_SHORT).show();
        });

        // Configurar botón para seleccionar carpeta de almacenamiento
        btnSelectStorage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            startActivityForResult(intent, REQUEST_DIRECTORY_PICKER);
        });

        // Configurar Bluetooth
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth no disponible en este dispositivo", Toast.LENGTH_SHORT).show();
        } else {
            loadPairedDevices();
        }

        // Conectar al dispositivo Bluetooth seleccionado
        btnConnectBluetooth.setOnClickListener(v -> connectToSelectedDevice());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_DIRECTORY_PICKER && resultCode == Activity.RESULT_OK && data != null) {
            String path = data.getData().toString();
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("storagePath", path);
            editor.apply();
            Toast.makeText(this, "Carpeta de almacenamiento seleccionada", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadPairedDevices() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        ArrayList<String> deviceNames = new ArrayList<>();
        for (BluetoothDevice device : pairedDevices) {
            deviceNames.add(device.getName() + " (" + device.getAddress() + ")");
        }

        if (deviceNames.isEmpty()) {
            deviceNames.add("No hay dispositivos emparejados");
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, deviceNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerBluetoothDevices.setAdapter(adapter);
    }

    private void connectToSelectedDevice() {
        String selectedDevice = (String) spinnerBluetoothDevices.getSelectedItem();
        if (selectedDevice != null && !selectedDevice.equals("No hay dispositivos emparejados")) {
            bluetoothConnected = true;
            Toast.makeText(this, "Conectando a " + selectedDevice, Toast.LENGTH_SHORT).show();
            // Aquí deberías agregar la lógica real de conexión Bluetooth con el HC-05
            sendBluetoothStatus();
        } else {
            Toast.makeText(this, "Seleccione un dispositivo válido", Toast.LENGTH_SHORT).show();
        }
    }

    private void sendBluetoothStatus() {
        Intent resultIntent = new Intent();
        resultIntent.putExtra("bluetoothStatus", bluetoothConnected);
        setResult(Activity.RESULT_OK, resultIntent);
        finish();
    }
}
