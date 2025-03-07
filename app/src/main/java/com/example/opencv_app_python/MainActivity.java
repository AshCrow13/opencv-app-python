package com.example.opencv_app_python;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.*;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.opencv.android.OpenCVLoader;
import org.opencv.core.Rect;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import android.graphics.Bitmap;
import java.io.FileOutputStream;


import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import androidx.camera.view.PreviewView;

public class MainActivity extends AppCompatActivity {

    private PreviewView previewView;
    private TextView txtResult;
    private Button btnCapture, btnCompare, btnSaveTemplate;

    private ImageCapture imageCapture;
    private ExecutorService cameraExecutor;
    private File capturedFile;

    private static final int CAMERA_PERMISSION_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Cargar OpenCV
        if (!OpenCVLoader.initDebug()) {
            Log.e("OpenCV", "Error: OpenCV no se pudo inicializar.");
        } else {
            Log.d("OpenCV", "OpenCV cargado correctamente.");
        }

        previewView = findViewById(R.id.viewFinder);
        txtResult = findViewById(R.id.txtResult);
        btnCapture = findViewById(R.id.btnCapture);
        btnCompare = findViewById(R.id.btnCompare);
        btnSaveTemplate = findViewById(R.id.btnSaveTemplate);

        btnSaveTemplate.setVisibility(View.GONE);
        btnSaveTemplate.setOnClickListener(v -> saveTemplate());

        // Verificar permisos en Android 6.0+
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
            } else {
                startCamera();
            }
        } else {
            startCamera();
        }

        cameraExecutor = Executors.newSingleThreadExecutor();

        btnCapture.setOnClickListener(v -> takePhoto());
        btnCompare.setOnClickListener(v -> compareWithTemplate());
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                Preview preview = new Preview.Builder().build();

                imageCapture = new ImageCapture.Builder()
                        .setTargetRotation(getWindowManager().getDefaultDisplay().getRotation())
                        .build();

                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                        .build();

                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);

            } catch (Exception e) {
                Log.e("CameraX", "Error al iniciar la cámara", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void takePhoto() {
        if (imageCapture == null) return;

        capturedFile = new File(getExternalFilesDir(null), "captura.jpg");
        ImageCapture.OutputFileOptions options = new ImageCapture.OutputFileOptions.Builder(capturedFile).build();

        imageCapture.takePicture(options, ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        Bitmap bitmap = BitmapFactory.decodeFile(capturedFile.getAbsolutePath());

                        // Definir la región de interés (ROI) para recortar
                        Rect roi = new Rect(50, 50, 200, 200); // Ajusta estos valores según necesites
                        Bitmap croppedBitmap = ImageProcessor.cropImage(bitmap, roi);

                        // Sobreescribir el archivo con la imagen recortada
                        try (FileOutputStream out = new FileOutputStream(capturedFile)) {
                            croppedBitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                            txtResult.setText("Imagen recortada guardada.");
                        } catch (Exception e) {
                            txtResult.setText("Error al guardar la imagen recortada.");
                        }
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        Log.e("CameraX", "Error al capturar imagen", exception);
                    }
                });
    }


    private void saveTemplate() {
        if (capturedFile == null || !capturedFile.exists()) {
            txtResult.setText("No hay imagen capturada para guardar.");
            return;
        }

        File templateFile = new File(getExternalFilesDir(null), "template.png");

        if (capturedFile.renameTo(templateFile)) {
            txtResult.setText("Plantilla guardada correctamente.");
            btnSaveTemplate.setVisibility(View.GONE);
        } else {
            txtResult.setText("Error al guardar la plantilla.");
        }
    }

    // Permisos en Android 6.0+
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                Toast.makeText(this, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void compareWithTemplate() {
        if (capturedFile == null || !capturedFile.exists()) {
            txtResult.setText("Primero captura una imagen.");
            return;
        }

        // Verificar si hay plantillas guardadas en la carpeta de almacenamiento interno
        File templatesDir = getExternalFilesDir(null);
        File[] templates = templatesDir.listFiles((dir, name) -> name.endsWith(".png"));

        if (templates == null || templates.length == 0) {
            txtResult.setText("No hay plantillas guardadas. ¿Deseas guardar esta imagen como plantilla?");
            btnSaveTemplate.setVisibility(View.VISIBLE);
            return;
        }

        // Obtener la instancia de Python
        Python py = Python.getInstance();
        PyObject myModule = py.getModule("myhelper");

        // Convertir la imagen capturada a Base64
        String encodedImage = getEncodedImage(capturedFile);

        // Comparar la imagen capturada con cada plantilla guardada
        for (File template : templates) {
            PyObject result = myModule.callAttr("compare_images", encodedImage, template.getAbsolutePath());

            if (result.toString().contains("Objeto encontrado")) {
                txtResult.setText("Objeto encontrado en plantilla: " + template.getName());
                return;
            }
        }

        txtResult.append("\nNo se encontró coincidencia. ¿Deseas guardar esta imagen como nueva plantilla?");
        btnSaveTemplate.setVisibility(View.VISIBLE);
    }

    private String getEncodedImage(File file) {
        try {
            Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 85, byteArrayOutputStream);
            byte[] byteArray = byteArrayOutputStream.toByteArray();
            return Base64.encodeToString(byteArray, Base64.DEFAULT);
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }


}
