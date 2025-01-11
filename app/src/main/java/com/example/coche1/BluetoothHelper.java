package com.example.coche1;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class BluetoothHelper {
    private final Context context;
    private final BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket bluetoothSocket;
    private OutputStream outputStream;

    // UUID estándar para dispositivos serie Bluetooth
    private final UUID UUID_BT = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    // Código para la solicitud de permisos
    private static final int REQUEST_BLUETOOTH_PERMISSIONS = 1;

    public BluetoothHelper(Context context) {
        this.context = context;
        BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        this.bluetoothAdapter = bluetoothManager.getAdapter();
    }

    public boolean isBluetoothAvailable() {
        return bluetoothAdapter != null;
    }

    public void enableBluetooth() {
        if (bluetoothAdapter == null) {
            Toast.makeText(context, "Bluetooth no está disponible en este dispositivo", Toast.LENGTH_SHORT).show();
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(context, "android.permission.BLUETOOTH_CONNECT") != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(context, "Permisos de Bluetooth no otorgados", Toast.LENGTH_LONG).show();
                return;
            }
        }

        if (!bluetoothAdapter.isEnabled()) {
            bluetoothAdapter.enable();
            Toast.makeText(context, "Activando Bluetooth...", Toast.LENGTH_SHORT).show();
        }
    }

    public boolean hasBluetoothPermissions() {
        return ActivityCompat.checkSelfPermission(context, "android.permission.BLUETOOTH_CONNECT") == PackageManager.PERMISSION_GRANTED;
    }

    public void requestBluetoothPermissions(@NonNull String[] permissions) {
        if (context instanceof ActivityCompat.OnRequestPermissionsResultCallback) {
            ActivityCompat.requestPermissions(
                    (android.app.Activity) context,
                    permissions,
                    REQUEST_BLUETOOTH_PERMISSIONS
            );
        } else {
            Toast.makeText(context, "No se puede solicitar permisos desde este contexto", Toast.LENGTH_SHORT).show();
        }
    }

    public void handlePermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
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
                Toast.makeText(context, "Permisos de Bluetooth no otorgados", Toast.LENGTH_LONG).show();
            }
        }
    }

    public void connectToDevice(String deviceAddress) {
        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(deviceAddress);

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ActivityCompat.checkSelfPermission(context, "android.permission.BLUETOOTH_CONNECT") != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(context, "Permisos de Bluetooth no otorgados", Toast.LENGTH_LONG).show();
                    return;
                }
            }

            bluetoothSocket = device.createRfcommSocketToServiceRecord(UUID_BT);
            bluetoothSocket.connect();
            outputStream = bluetoothSocket.getOutputStream();
            Toast.makeText(context, "Conectado al dispositivo", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(context, "Error al conectar con el dispositivo Bluetooth", Toast.LENGTH_LONG).show();
            closeConnection();
        }
    }

    public void sendData(String data) {
        if (bluetoothSocket != null && bluetoothSocket.isConnected()) {
            try {
                outputStream.write(data.getBytes());
            } catch (IOException e) {
                Toast.makeText(context, "Error al enviar datos", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        } else {
            Toast.makeText(context, "No hay conexión Bluetooth activa", Toast.LENGTH_SHORT).show();
        }
    }

    public InputStream getInputStream() throws IOException {
        if (bluetoothSocket == null || !bluetoothSocket.isConnected()) {
            throw new IOException("El socket Bluetooth no está conectado.");
        }
        return bluetoothSocket.getInputStream();
    }

    public void closeConnection() {
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
}
