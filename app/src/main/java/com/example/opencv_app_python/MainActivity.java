package com.example.opencv_app_python;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Base64;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;

import java.io.ByteArrayOutputStream;

public class MainActivity extends AppCompatActivity {

    private ImageView imgWidget1;
    private TextView textWidget1;
    private Button btnGrayscale;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inicializar vistas
        imgWidget1 = findViewById(R.id.img_widget1);
        textWidget1 = findViewById(R.id.text_widget1);
        btnGrayscale = findViewById(R.id.btn_grayscale);

        // Configurar el botón
        btnGrayscale.setOnClickListener(v -> processImage());
    }

    private void processImage() {
        try {
            // Obtener el Bitmap de la ImageView
            Bitmap bitmap = getBitmapFromDrawable(imgWidget1.getDrawable());
            if (bitmap == null) {
                textWidget1.setText("Error: No se pudo obtener la imagen.");
                return;
            }

            // Convertir el Bitmap a una cadena codificada en Base64
            String encodedImage = getEncodedImage(bitmap);

            // Llamar a la función de Python para procesar la imagen
            Python py = Python.getInstance();
            PyObject myModule = py.getModule("myhelper"); // Nombre más claro
            PyObject myFnCall = myModule.get("opencv_img_process");
            String result = myFnCall.call(encodedImage).toString();

            // Mostrar el resultado en el TextView
            textWidget1.setText("Dimensiones: " + result);

        } catch (Exception ex) {
            textWidget1.setText("Error: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    //Método optimizado para convertir un Bitmap a Base64
    private String getEncodedImage(Bitmap bmp) {
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
            bmp.compress(Bitmap.CompressFormat.PNG, 85, byteArrayOutputStream);
            return Base64.encodeToString(byteArrayOutputStream.toByteArray(), Base64.DEFAULT);
        } catch (Exception e) {
            return "";
        }
    }

    // Método optimizado para convertir un Drawable a Bitmap
    @NonNull
    private Bitmap getBitmapFromDrawable(@NonNull Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        }

        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(),
                Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }
}
