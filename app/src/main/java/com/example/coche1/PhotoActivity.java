package com.example.coche1;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothSocket;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.util.Base64;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class PhotoActivity extends AppCompatActivity {
    private BluetoothHelper bluetoothHelper;
    private EditText etDeviceAddress;
    private Button btnConnect;
    private Button btnTakePhoto;
    private ImageView imageView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo);

        etDeviceAddress = findViewById(R.id.etDeviceAddress);
        btnConnect = findViewById(R.id.btnConnect);
        btnTakePhoto = findViewById(R.id.btnTakePhoto);
        imageView = findViewById(R.id.imageView);

        btnTakePhoto.setOnClickListener(v -> {
            bluetoothHelper.sendData("0000006#");
            printPhotoInfo();
        });


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

    private void printPhotoInfo(){
        new Thread(() -> {
            try {
                InputStream inputStream = bluetoothHelper.getInputStream();
                if (inputStream == null) {
                    runOnUiThread(() -> Toast.makeText(this, "No se pudo recibir la foto", Toast.LENGTH_SHORT).show());
                    return;
                }

                // Leer los bytes de la imagen desde el Bluetooth
                byte[] buffer = new byte[1024];
                int bytesRead;
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    byteArrayOutputStream.write(buffer, 0, bytesRead);

                    // Terminar si el flujo se cierra
                    if (inputStream.available() == 0) {
                        break;
                    }
                }

                byte[] imageData = byteArrayOutputStream.toByteArray();
                String imageBase64 = Base64.encodeToString(imageData, Base64.DEFAULT);


                // Decodificar los bytes en un bitmap
                Bitmap bitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.length);

                // Mostrar la imagen en el ImageView
                runOnUiThread(() -> imageView.setImageBitmap(bitmap));
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Error al recibir la foto", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void analyzePhoto(byte[] imageData) {
        // Convertir imagen a Base64
        String imageBase64 = Base64.encodeToString(imageData, Base64.DEFAULT);

        // Enviar a Google Vision API
        VisionApiDescription.sendImageToVisionAPI(imageBase64, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() ->
                        Toast.makeText(PhotoActivity.this, "Error al conectar con la API", Toast.LENGTH_SHORT).show()
                );
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    assert response.body() != null;
                    String responseData = response.body().string();
                    runOnUiThread(() ->
                            // Aqui pillo la respuesta la parseo y lo pongo en un tessto que haiga
                    );
                } else {
                    runOnUiThread(() ->
                            Toast.makeText(PhotoActivity.this, "Error en la respuesta de la API", Toast.LENGTH_SHORT).show()
                    );
                }
            }
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
