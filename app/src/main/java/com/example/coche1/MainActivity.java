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

public class MainActivity extends AppCompatActivity {
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket bluetoothSocket;
    private OutputStream outputStream;

    private EditText etDeviceAddress;
    private Button btnConnect;
    // UUID estándar para dispositivos serie Bluetooth
    private final UUID UUID_BT = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    // Código para la solicitud de permisos
    private static final int REQUEST_BLUETOOTH_PERMISSIONS = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Inicializar Bluetooth
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        etDeviceAddress = findViewById(R.id.etDeviceAddress);
        btnConnect = findViewById(R.id.btnConnect);

        // Verificar si el dispositivo tiene Bluetooth
        if (bluetoothAdapter == null) {
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
                connectToDevice(deviceAddress);
            }
        });

        // Configurar botones
        Button buttonUp = findViewById(R.id.buttonUp);
        Button buttonDown = findViewById(R.id.buttonDown);
        Button buttonLeft = findViewById(R.id.buttonLeft);
        Button buttonRight = findViewById(R.id.buttoRight);
        Button buttonCenter = findViewById(R.id.buttoCenter);

        buttonUp.setOnClickListener(v -> sendData("0000004#"));
        buttonDown.setOnClickListener(v -> sendData("0000002#"));
        buttonLeft.setOnClickListener(v -> sendData("0000003#"));
        buttonRight.setOnClickListener(v -> sendData("0000001#"));
        buttonCenter.setOnClickListener(v -> sendData("0000005#"));

        // Solicitar permisos y conectar al dispositivo
        if (hasBluetoothPermissions()) {
            enableBluetooth();
        } else {
            requestBluetoothPermissions();
        }

        // Cosas de la interfaz no tocar
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    // Habilitar Bluetooth si está desactivado
    private void enableBluetooth() {
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth no está disponible en este dispositivo", Toast.LENGTH_SHORT).show();
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { // Android 12 y superior
            if (ActivityCompat.checkSelfPermission(this, "android.permission.BLUETOOTH_CONNECT") != PackageManager.PERMISSION_GRANTED) {
                requestBluetoothPermissions();
                return;
            }
        }

        try {
            if (!bluetoothAdapter.isEnabled()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Toast.makeText(this, "Por favor, activa Bluetooth en Configuración", Toast.LENGTH_LONG).show();
                } else {
                    bluetoothAdapter.enable();
                    Toast.makeText(this, "Activando Bluetooth...", Toast.LENGTH_SHORT).show();
                }
            }
        } catch (SecurityException e) {
            Toast.makeText(this, "No se puede habilitar Bluetooth: permisos denegados", Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    private void requestBluetoothPermissions() {
        ActivityCompat.requestPermissions(
                this,
                new String[]{"android.permission.BLUETOOTH_CONNECT", "android.permission.BLUETOOTH_SCAN"},
                REQUEST_BLUETOOTH_PERMISSIONS
        );
    }

    private boolean hasBluetoothPermissions() {
        return ActivityCompat.checkSelfPermission(this, "android.permission.BLUETOOTH_CONNECT") == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_BLUETOOTH_PERMISSIONS) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (allGranted) {
                enableBluetooth();
            } else {
                Toast.makeText(this, "Permisos de Bluetooth no otorgados", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }


    private void connectToDevice(String deviceAddress) {
        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(deviceAddress);

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ActivityCompat.checkSelfPermission(this, "android.permission.BLUETOOTH_CONNECT") != PackageManager.PERMISSION_GRANTED) {
                    requestBluetoothPermissions(); // Solicitar permisos si no están concedidos
                    return;
                }
            }

            bluetoothSocket = device.createRfcommSocketToServiceRecord(UUID_BT);
            bluetoothSocket.connect();

            outputStream = bluetoothSocket.getOutputStream();
            Toast.makeText(this, "Conectado al Arduino", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error al conectar con el dispositivo Bluetooth", Toast.LENGTH_LONG).show();
            closeConnection();
        }
    }

    private void sendData(String data) {
        if (bluetoothSocket != null && bluetoothSocket.isConnected()) {
            try {
                outputStream.write(data.getBytes());
                Toast.makeText(this, "Enviado: " + data, Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                Toast.makeText(this, "Error al enviar datos", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        } else {
            Toast.makeText(this, "No hay conexión Bluetooth activa", Toast.LENGTH_SHORT).show();
        }
    }

    private void closeConnection() {
        try {
            if (outputStream != null) {
                outputStream.close();
            }
            if (bluetoothSocket != null) {
                bluetoothSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        closeConnection();
    }






}