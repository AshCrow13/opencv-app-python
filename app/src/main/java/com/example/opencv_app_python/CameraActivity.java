package com.example.opencv_app_python;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
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

        // Inicializar vistas
        previewView = findViewById(R.id.viewFinder);
        btnCapture = findViewById(R.id.btnCapture);
        imgPreview = findViewById(R.id.imgPreview);
        txtResult = findViewById(R.id.txtResult);
        inputWidth = findViewById(R.id.inputWidth);
        inputHeight = findViewById(R.id.inputHeight);

        // Verificar si imgPreview fue correctamente vinculado
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

        // Executor para cámara
        cameraExecutor = Executors.newSingleThreadExecutor();

        // Listener del botón
        btnCapture.setOnClickListener(v -> takePhoto());
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

                        // Mostrar la imagen recortada en la vista previa
                        imgPreview.setImageBitmap(croppedBitmap);

                        // Procesar la imagen recortada con Python
                        processImageWithPython(capturedFile);
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        Log.e("CameraX", "Error al capturar imagen", exception);
                        txtResult.setText("Error al capturar la imagen.");
                    }
                });
    }


    /*private void takePhoto() {
        if (imageCapture == null) return;

        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String fileName = "Pieza_" + timestamp + ".png";
        File tempFile = new File(getExternalFilesDir(null), fileName);  // aún no se guarda

        ImageCapture.OutputFileOptions options = new ImageCapture.OutputFileOptions.Builder(tempFile).build();

        imageCapture.takePicture(options, ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        Bitmap bitmap = BitmapFactory.decodeFile(tempFile.getAbsolutePath());
                        if (bitmap == null) {
                            txtResult.setText("Error al cargar la imagen capturada.");
                            return;
                        }

                        // Analizar imagen completa sin guardar aún
                        processImageBeforeCrop(bitmap, fileName, tempFile);
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        Log.e("CameraX", "Error al capturar imagen", exception);
                        txtResult.setText("Error al capturar la imagen.");
                    }
                });
    }*/


    /*private void processImageBeforeCrop(Bitmap fullBitmap, String fileName, File tempFile) {
        Log.d("PYTHON_FLOW", "⏳ Iniciando análisis con Python...");

        Python py = Python.getInstance();
        PyObject shapeModule = py.getModule("image_processor");

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        fullBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        String encodedImage = Base64.encodeToString(stream.toByteArray(), Base64.DEFAULT);

        PyObject classificationResult = shapeModule.callAttr("classify_shape", encodedImage);
        String classification = classificationResult.toString();
        Log.d("PYTHON_FLOW", "✅ Clasificación: " + classification);

        PyObject matchModule = py.getModule("myhelper");
        List<String> paths = databaseHelper.getAllImagePaths();
        Log.d("PYTHON_FLOW", "📦 Cantidad de imágenes para comparar: " + paths.size());

        // ✅ CORRECTO: convertir ArrayList a lista de Python
        PyObject pyPaths = Python.getInstance().getBuiltins().callAttr("list", paths);
        PyObject matchResult = matchModule.callAttr("compare_with_templates", encodedImage, pyPaths);

        Log.d("PYTHON_FLOW", "✅ Resultado de Python: " + matchResult.toString());

        PyObject pathObj = matchResult.get("path");
        String matchName = matchResult.containsKey("match_name") ? matchResult.get("match_name").toString() : "";

        if (matchName.isEmpty() || matchName.equals("None")) {
            Log.d("PYTHON_FLOW", "🆕 No se encontró coincidencia, procediendo a guardar imagen...");

            int cropWidth = 200;
            int cropHeight = 200;

            try {
                cropWidth = Integer.parseInt(inputWidth.getText().toString());
                cropHeight = Integer.parseInt(inputHeight.getText().toString());
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Usando tamaño por defecto (200x200)", Toast.LENGTH_SHORT).show();
            }

            int centerX = fullBitmap.getWidth() / 2;
            int centerY = fullBitmap.getHeight() / 2;
            int left = Math.max(centerX - cropWidth / 2, 0);
            int top = Math.max(centerY - cropHeight / 2, 0);
            Rect roi = new Rect(left, top, cropWidth, cropHeight);

            Bitmap croppedBitmap = cropImage(fullBitmap, roi);

            try (FileOutputStream out = new FileOutputStream(tempFile)) {
                croppedBitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            } catch (IOException e) {
                txtResult.setText("Error al guardar imagen recortada.");
                Log.e("PYTHON_FLOW", "❌ Error al guardar imagen: " + e.getMessage());
                return;
            }

            saveToDatabase(tempFile, classification);
            imgPreview.setImageBitmap(croppedBitmap);
            txtResult.setText("Clasificación: " + classification + "\nImagen guardada correctamente.");
            Log.d("PYTHON_FLOW", "✅ Imagen guardada como: " + tempFile.getAbsolutePath());

        } else {
            Log.d("PYTHON_FLOW", "🔁 Imagen similar ya existe: " + matchName);
            if (tempFile.exists()) tempFile.delete();

            String bestPath = pathObj.toString();
            int similarity = matchResult.get("similarity").toInt();
            String tipo = databaseHelper.getClassificationByPath(bestPath);

            txtResult.setText("Clasificación: " + classification + "\n" +
                    "Coincidencia con: " + tipo + "\nArchivo: " + matchName + "\nSimilitud: " + similarity + " matches");

            Bitmap matchedBitmap = BitmapFactory.decodeFile(bestPath);
            if (matchedBitmap != null) {
                imgPreview.setImageBitmap(matchedBitmap);
            } else {
                Toast.makeText(this, "No se pudo cargar la imagen coincidente.", Toast.LENGTH_SHORT).show();
                Log.e("PYTHON_FLOW", "❌ No se pudo cargar imagen desde: " + bestPath);
            }
        }
    }*/





    private void processImageWithPython(File file) {
        Python py = Python.getInstance();

        // Módulo de clasificación
        PyObject shapeModule = py.getModule("image_processor");
        String encodedImage = encodeImageToBase64(file);
        PyObject classificationResult = shapeModule.callAttr("classify_shape", encodedImage);

        // Módulo de comparación
        PyObject matchModule = py.getModule("myhelper");
        List<String> paths = databaseHelper.getAllImagePaths();
        String capturedPath = file.getAbsolutePath();
        paths.remove(capturedPath); // Excluir la imagen actual

        // Llamar a la función de comparación en Python
        PyObject matchResult = matchModule.callAttr("compare_with_templates", encodedImage, paths.toArray());

        // Depuración: Imprimir el resultado de Python
        System.out.println("Resultado de Python: " + matchResult.toString());

        // Manejar errores
        if (matchResult.containsKey("error")) {
            String errorMsg = matchResult.get("error").toString();
            txtResult.setText("Clasificación: " + classificationResult.toString() + "\n" +
                    "Error al comparar: " + errorMsg);
            return;
        }

        PyObject pathObj = matchResult.get("path");

        // Si no hay coincidencia, guardar imagen como nueva
        if (pathObj == null || pathObj.toString().isEmpty()) {
            saveToDatabase(file, classificationResult.toString());
            txtResult.setText("Clasificación: " + classificationResult + "\nNo se encontró coincidencia válida.");
            return;
        }

        // Coincidencia encontrada (incluso si la similitud es baja)
        String bestPath = pathObj.toString();
        double similarity = matchResult.get("similarity").toDouble(); // Puntaje entre 0 y 1.0
        String matchName = matchResult.get("match_name").toString();
        String tipo = databaseHelper.getClassificationByPath(bestPath);

        // Mostrar resultados en la interfaz
        txtResult.setText("Clasificación: " + classificationResult.toString() + "\n" +
                "Coincidencia con: " + tipo + "\n" +
                "Archivo: " + matchName + "\n" +
                "Similitud: " + String.format("%.2f", similarity * 100) + "%"); // Mostrar similitud como porcentaje

        // Mostrar la imagen parecida
        File imgFile = new File(bestPath);
        if (!imgFile.exists()) {
            System.out.println("La imagen no existe en: " + bestPath);
            return;
        }

        // Cargar y mostrar la imagen coincidente
        Bitmap matchedBitmap = BitmapFactory.decodeFile(bestPath);
        if (matchedBitmap != null) {
            imgPreview.setImageBitmap(matchedBitmap);
        } else {
            System.out.println("No se pudo decodificar la imagen: " + bestPath);
        }
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

        if (inserted) {
            Toast.makeText(this, "Pieza guardada", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Error al guardar", Toast.LENGTH_SHORT).show();
        }
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
