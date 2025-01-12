package com.example.coche1;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.os.Bundle;
import android.util.Base64;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class PhotoActivity extends AppCompatActivity {
    private BluetoothHelper bluetoothHelper;
    private EditText etDeviceAddress;
    private TextView photoAnalysis;

    private Button btnConnect;
    private Button btnTakePhoto;
    private Button btnAnalysePhoto;
    private String filePath;

    private ImageView imageView;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo);

        etDeviceAddress = findViewById(R.id.etDeviceAddress);
        btnConnect = findViewById(R.id.btnConnect);
        btnTakePhoto = findViewById(R.id.btnTakePhoto);
        btnAnalysePhoto = findViewById(R.id.btnAnalysePhoto);
        photoAnalysis = findViewById(R.id.photoAnalysis);

        imageView = findViewById(R.id.imageView);

        // Configurar el botón de conexión
        btnConnect.setOnClickListener(v -> {
            String deviceAddress = etDeviceAddress.getText().toString().trim();
            if (deviceAddress.isEmpty()) {
                Toast.makeText(this, "Por favor, introduce una dirección Bluetooth válida", Toast.LENGTH_SHORT).show();
            } else {
                bluetoothHelper.connectToDevice(deviceAddress);
            }
        });

        // Configurar el boton de tomar foto
        btnTakePhoto.setOnClickListener(v -> {
            bluetoothHelper.sendData("0000006#");
            printPhoto();
        });

        // Configurar boton de analizar la fotografia
        btnAnalysePhoto.setOnClickListener(v -> {
            try {
                uploadAndAnalysePhoto(filePath);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        // Iniciar clase bluetooth
        bluetoothHelper = new BluetoothHelper(this);

        // Verificar si Bluetooth está disponible
        if (!bluetoothHelper.isBluetoothAvailable()) {
            Toast.makeText(this, "Bluetooth no está disponible en este dispositivo", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

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

    private void printPhoto(){
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

                // Guardar la imagen en el almacenamiento interno
                filePath = saveImageToInternalStorage(imageData);
                if (filePath == null) {
                    runOnUiThread(() -> Toast.makeText(this, "No se pudo guardar la imagen", Toast.LENGTH_SHORT).show());
                    return;
                }

                // Mostrar la imagen en el ImageView
                Bitmap bitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.length);
                runOnUiThread(() -> imageView.setImageBitmap(bitmap));

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Error al recibir la foto", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private String saveImageToInternalStorage(byte[] imageData) {
        try {
            // Obtiene el directorio de caché interno de la aplicación
            File cacheDir = getApplicationContext().getCacheDir();

            // Crea un archivo temporal en el directorio de caché
            File imageFile = File.createTempFile("received_image", ".jpg", cacheDir);

            // Escribe los datos de la imagen en el archivo
            try (FileOutputStream fileOutputStream = new FileOutputStream(imageFile)) {
                fileOutputStream.write(imageData);
            }

            // Retorna la ruta completa del archivo guardado
            return imageFile.getAbsolutePath();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void uploadAndAnalysePhoto(String filePath) throws IOException {
        // Subir la imagen a Imagga
        ImaggaHandler imaggaHandler = new ImaggaHandler();

        imaggaHandler.uploadImageAsync(filePath, result -> {
            // Este metodo se ejecutará cuando se haya completado la carga de la imagen
            if (result != null) {
                // Llamar a parseImageUploadId para obtener el upload_id de la respuesta
                String uploadId = imaggaHandler.parseImageUploadId(result);

                if (uploadId != null) {
                    // Aquí tienes el upload_id, ahora puedes usarlo para obtener las etiquetas
                    imaggaHandler.getTagsAsync(uploadId, tagsResult -> {
                        // Aquí puedes procesar las etiquetas obtenidas
                        // Puedes mostrar las etiquetas o hacer lo que necesites con ellas
                        String topTags = imaggaHandler.extractTopTags(tagsResult);
                        photoAnalysis.setText(topTags);
                    });
                } else {
                    // Si no se encuentra el upload_id, manejar el error
                    System.out.println("No se pudo obtener el upload_id.");
                }

            } else {
                // Si la respuesta de la carga es null, gestionar el error
                System.out.println("Error en la carga de la imagen.");
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
