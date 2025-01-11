package com.example.coche1;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothSocket;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;

public class PhotoActivity extends AppCompatActivity {
    private BluetoothHelper bluetoothHelper;
    private EditText etDeviceAddress;
    private Button btnConnect;
    private Button btnTakePhoto;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo);

        etDeviceAddress = findViewById(R.id.etDeviceAddress);
        btnConnect = findViewById(R.id.btnConnect);
        btnTakePhoto = findViewById(R.id.btnTakePhoto);

        btnTakePhoto.setOnClickListener(v -> bluetoothHelper.sendData("0000006#"));


        // Iniciar clase bluetooth
        bluetoothHelper = new BluetoothHelper(this);

        // Verificar si Bluetooth está disponible
        if (!bluetoothHelper.isBluetoothAvailable()) {
            Toast.makeText(this, "Bluetooth no está disponible en este dispositivo", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Configurar el botón de conexión
        btnConnect.setOnClickListener(v -> {
            String deviceAddress = etDeviceAddress.getText().toString().trim();
            if (deviceAddress.isEmpty()) {
                Toast.makeText(this, "Por favor, introduce una dirección Bluetooth válida", Toast.LENGTH_SHORT).show();
            } else {
                bluetoothHelper.connectToDevice(deviceAddress);
            }
        });

        if (!bluetoothHelper.hasBluetoothPermissions()) {
            bluetoothHelper.requestBluetoothPermissions(new String[]{
                    "android.permission.BLUETOOTH_CONNECT",
                    "android.permission.BLUETOOTH_SCAN"
            });
        } else {
            bluetoothHelper.enableBluetooth();
        }

        // Cosas de la interfaz no tocar
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        bluetoothHelper.handlePermissionsResult(requestCode, permissions, grantResults);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        bluetoothHelper.closeConnection();
    }

}
