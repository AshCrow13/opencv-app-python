package com.example.opencv_app_python;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.*;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.google.common.util.concurrent.ListenableFuture;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class CameraActivity extends AppCompatActivity {

    private PreviewView previewView;
    private Button btnCapture;
    private ImageView imgPreview;

    private TextView txtResult;
    private ImageCapture imageCapture;
    private ExecutorService cameraExecutor;
    private File capturedFile;
    private static final int CAMERA_PERMISSION_CODE = 100;
    private EditText inputWidth;
    private EditText inputHeight;
    private DatabaseHelper databaseHelper;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        Button btnOpenBotonera = findViewById(R.id.btnOpenBotonera);

        btnOpenBotonera.setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setClassName("com.example.javibotonera", "com.example.javibotonera.Dispositivos");
            try {
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(this, "No se pudo encontrar la app javibotonera", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        });


        // Inicializar vistas
        previewView = findViewById(R.id.viewFinder);
        btnCapture = findViewById(R.id.btnCapture);
        imgPreview = findViewById(R.id.imgPreview);
        txtResult = findViewById(R.id.txtResult);
        inputWidth = findViewById(R.id.inputWidth);
        inputHeight = findViewById(R.id.inputHeight);


        // Verificar vinculación
        if (imgPreview == null) {
            Log.e("IMG_PREVIEW", "imgPreview ES NULL 😱");
        } else {
            Log.d("IMG_PREVIEW", "imgPreview fue vinculado correctamente 🎉");
        }

        // Inicializar OpenCV
        if (!OpenCVLoader.initDebug()) {
            Log.e("OpenCV", "Error al iniciar OpenCV");
        }

        // Inicializar base de datos
        databaseHelper = new DatabaseHelper(this);

        // Permisos de cámara
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
        } else {
            startCamera();
        }

        // ✅ Inicializar el executor para tareas en segundo plano
        cameraExecutor = Executors.newSingleThreadExecutor();

        // Botón de captura
        btnCapture.setOnClickListener(v -> takePhoto());
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraExecutor != null && !cameraExecutor.isShutdown()) {
            cameraExecutor.shutdown();
        }
    }



    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

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

        // Generar un nombre único para la imagen capturada
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String fileName = "Pieza_" + timestamp + ".png";
        capturedFile = new File(getExternalFilesDir(null), fileName);

        // Configurar opciones para guardar la imagen
        ImageCapture.OutputFileOptions options = new ImageCapture.OutputFileOptions.Builder(capturedFile).build();

        // Capturar la imagen
        imageCapture.takePicture(options, ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        // Cargar la imagen capturada como un Bitmap
                        Bitmap bitmap = BitmapFactory.decodeFile(capturedFile.getAbsolutePath());
                        if (bitmap == null) {
                            txtResult.setText("Error al cargar la imagen capturada.");
                            return;
                        }

                        // Obtener las dimensiones del recorte
                        int cropWidth = 200;
                        int cropHeight = 200;

                        try {
                            cropWidth = Integer.parseInt(inputWidth.getText().toString());
                            cropHeight = Integer.parseInt(inputHeight.getText().toString());
                        } catch (NumberFormatException e) {
                            Toast.makeText(CameraActivity.this, "Usando tamaño por defecto (200x200)", Toast.LENGTH_SHORT).show();
                        }

                        // Calcular la región de interés (ROI) para el recorte
                        int centerX = bitmap.getWidth() / 2;
                        int centerY = bitmap.getHeight() / 2;
                        int left = Math.max(centerX - cropWidth / 2, 0);
                        int top = Math.max(centerY - cropHeight / 2, 0);
                        Rect roi = new Rect(left, top, cropWidth, cropHeight);

                        // Recortar la imagen
                        Bitmap croppedBitmap = cropImage(bitmap, roi);
                        if (croppedBitmap == null) {
                            txtResult.setText("Error al recortar la imagen.");
                            return;
                        }

                        // Guardar la imagen recortada
                        try (FileOutputStream out = new FileOutputStream(capturedFile)) {
                            croppedBitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                        } catch (Exception e) {
                            txtResult.setText("Error al guardar la imagen recortada.");
                            return;
                        }

                        // Verificar que el archivo existe y no está vacío
                        if (!capturedFile.exists() || capturedFile.length() == 0) {
                            txtResult.setText("Archivo recortado inválido o vacío.");
                            return;
                        }

                        // Mostrar la imagen recortada en la vista previa
                        imgPreview.setImageBitmap(croppedBitmap);

                        // Procesar la imagen con un pequeño retardo para asegurar que el archivo se haya guardado bien
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            processImageWithPython(capturedFile);
                        }, 100); // Espera 100 ms antes de procesar
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        Log.e("CameraX", "Error al capturar imagen", exception);
                        txtResult.setText("Error al capturar la imagen.");
                    }
                });
    }


    private void processImageWithPython(File file) {
        cameraExecutor.execute(() -> {
            Python py = Python.getInstance();

            // Módulo de clasificación
            PyObject shapeModule = py.getModule("image_processor");
            String encodedImage = encodeImageToBase64(file);
            PyObject classificationResult = shapeModule.callAttr("classify_shape", encodedImage);

            // Módulo de comparación
            PyObject matchModule = py.getModule("myhelper");
            List<String> paths = databaseHelper.getAllImagePaths();
            String capturedPath = file.getAbsolutePath();
            paths.remove(capturedPath);

            PyObject matchResultObj = matchModule.callAttr("compare_with_templates", encodedImage, paths.toArray());
            Map<PyObject, PyObject> matchResult = matchResultObj.asMap();

            System.out.println("Resultado de Python: " + matchResultObj.toString());

            if (matchResult.containsKey(PyObject.fromJava("error"))) {
                String errorMsg = matchResult.get(PyObject.fromJava("error")).toString();
                runOnUiThread(() -> txtResult.setText("Clasificación: " + classificationResult.toString() + "\nError al comparar: " + errorMsg));
                return;
            }

            PyObject pathObj = matchResult.get(PyObject.fromJava("path"));
            PyObject similarityObj = matchResult.get(PyObject.fromJava("similarity"));
            PyObject matchNameObj = matchResult.get(PyObject.fromJava("match_name"));

            String pathStr = (pathObj != null) ? pathObj.toString() : null;
            double similarity = (similarityObj != null) ? similarityObj.toDouble() : 0.0;
            String matchName = (matchNameObj != null) ? matchNameObj.toString() : "";

            System.out.println("DEBUG >> pathStr: " + pathStr);
            System.out.println("DEBUG >> similarity: " + similarity);

            double SIMILARITY_THRESHOLD = 0.8;

            if (pathStr == null || pathStr.equals("None") || pathStr.trim().isEmpty() || similarity < SIMILARITY_THRESHOLD) {
                saveToDatabase(file, classificationResult.toString());

                runOnUiThread(() -> {
                    if (similarity < SIMILARITY_THRESHOLD && pathStr != null && !pathStr.trim().isEmpty()) {
                        txtResult.setText("Clasificación: " + classificationResult + "\n" +
                                "Coincidencia con: " + matchName + " (Similitud: " +
                                String.format("%.2f", similarity * 100) + "%, considerada demasiado baja)\n" +
                                "Guardado como nueva pieza.");
                    } else {
                        txtResult.setText("Clasificación: " + classificationResult + "\nNo se encontró coincidencia válida.");
                    }
                });
                return;
            }

            // Coincidencia válida encontrada
            String bestPath = pathStr;
            String tipo = databaseHelper.getClassificationByPath(bestPath);
            File imgFile = new File(bestPath);

            if (!imgFile.exists()) {
                System.out.println("La imagen no existe en: " + bestPath);
                return;
            }

            Bitmap matchedBitmap = BitmapFactory.decodeFile(bestPath);
            if (matchedBitmap != null) {
                final double simFinal = similarity;
                final String tipoFinal = tipo;
                final String matchNameFinal = matchName;
                final String classificationFinal = classificationResult.toString();

                runOnUiThread(() -> {
                    AlertDialog.Builder builder = new AlertDialog.Builder(CameraActivity.this);
                    builder.setTitle("Coincidencia encontrada: " + tipoFinal);

                    ImageView imageView = new ImageView(CameraActivity.this);
                    imageView.setImageBitmap(matchedBitmap);
                    imageView.setAdjustViewBounds(true);
                    imageView.setPadding(20, 20, 20, 20);
                    builder.setView(imageView);

                    builder.setPositiveButton("OK", (dialog, which) -> {
                        dialog.dismiss();
                        txtResult.setText("Clasificación: " + classificationFinal + "\n" +
                                "Coincidencia con: " + tipoFinal + "\n" +
                                "Archivo: " + matchNameFinal + "\n" +
                                "Similitud: " + String.format("%.2f", simFinal * 100) + "%");
                    });

                    builder.show();
                });

            } else {
                runOnUiThread(() -> txtResult.setText("Clasificación: " + classificationResult.toString() + "\n" +
                        "Coincidencia con: " + tipo + "\n" +
                        "Archivo: " + matchName + "\n" +
                        "Similitud: " + String.format("%.2f", similarity * 100) + "% (no se pudo mostrar imagen)"));
            }
        });
    }



    private void saveToDatabase(File file, String classification) {
        String name = file.getName();
        String type = classification;
        String date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
        String imagePath = file.getAbsolutePath();
        Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
        String dimensions = bitmap.getWidth() + "x" + bitmap.getHeight();
        String material = "Madera";

        boolean inserted = databaseHelper.insertPiece(name, type, date, imagePath, dimensions, material);

        // Mostrar el resultado del guardado en el hilo principal
        runOnUiThread(() -> {
            if (inserted) {
                Toast.makeText(CameraActivity.this, "Pieza guardada", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(CameraActivity.this, "Error al guardar", Toast.LENGTH_SHORT).show();
            }
        });
    }


    private String encodeImageToBase64(File file) {
        try {
            FileInputStream fileInputStream = new FileInputStream(file);
            byte[] bytes = new byte[(int) file.length()];
            fileInputStream.read(bytes);
            fileInputStream.close();
            return Base64.encodeToString(bytes, Base64.DEFAULT);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private Bitmap cropImage(Bitmap image, Rect roi) {
        if (image == null) {
            Log.e("CropImage", "La imagen de entrada es nula.");
            return null;
        }

        Mat imgMat = new Mat();
        Utils.bitmapToMat(image, imgMat);

        // Ajustar la región de interés para que esté dentro de los límites de la imagen
        int x = Math.max(roi.x, 0);
        int y = Math.max(roi.y, 0);
        int width = Math.min(roi.width, imgMat.cols() - x);
        int height = Math.min(roi.height, imgMat.rows() - y);

        if (width <= 0 || height <= 0) {
            Log.e("CropImage", "La región de interés no es válida.");
            return null;
        }

        Rect validRoi = new Rect(x, y, width, height);
        Mat cropped = new Mat(imgMat, validRoi);

        Bitmap result = Bitmap.createBitmap(cropped.cols(), cropped.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(cropped, result);

        // Liberar la memoria de la matriz de OpenCV
        imgMat.release();
        cropped.release();

        return result;
    }
}
