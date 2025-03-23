package com.example.opencv_app_python;

import android.app.Application;
import com.chaquo.python.android.AndroidPlatform;
import com.chaquo.python.Python;

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        // Inicializar Python
        if (!Python.isStarted()) {
            Python.start(new AndroidPlatform(this));
        }
    }
}

/*import cv2
import numpy as np
import base64
import os

def decode_image(image_data):
    decoded_bytes = base64.b64decode(image_data)
    np_arr = np.frombuffer(decoded_bytes, np.uint8)
    img = cv2.imdecode(np_arr, cv2.IMREAD_GRAYSCALE)
    return img

def compare_with_templates(encoded_image, image_paths):
    # Convertir lista Java a lista Python
    image_paths = [image_paths.get(i) for i in range(image_paths.size())]

    if not image_paths:
        return {"error": "No hay imágenes guardadas para comparar"}

    try:
        # Decodificar imagen de entrada
        image_data = base64.b64decode(encoded_image)
        np_arr = np.frombuffer(image_data, np.uint8)
        input_img = cv2.imdecode(np_arr, cv2.IMREAD_GRAYSCALE)

        best_score = -1
        best_path = None

        for path in image_paths:
            template = cv2.imread(path, cv2.IMREAD_GRAYSCALE)
            if template is None:
                print(f"[DEBUG] No se pudo cargar la plantilla: {path}")
                continue

            result = cv2.matchTemplate(input_img, template, cv2.TM_CCOEFF_NORMED)
            _, max_val, _, _ = cv2.minMaxLoc(result)

            print(f"[DEBUG] Comparando con: {os.path.basename(path)}, Similitud: {max_val:.4f}")

            if max_val > best_score:
                best_score = max_val
                best_path = path

        if best_path is None:
            return {"path": None, "similarity": 0, "match_name": None}

        return {
            "path": best_path,
            "similarity": int(best_score * 100),
            "match_name": os.path.basename(best_path)  # solo el nombre del archivo
        }

    except Exception as e:
        return {"error": f"Ocurrió un error al comparar: {str(e)}"}

def classify_shape(image_data):
    img = decode_image(image_data)
    if img is None:
        return "No se pudo decodificar la imagen"

    _, thresh = cv2.threshold(img, 128, 255, cv2.THRESH_BINARY)
    contours, _ = cv2.findContours(thresh, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)

    if not contours:
        return "No se detectó ninguna figura"

    cnt = max(contours, key=cv2.contourArea)
    hull = cv2.convexHull(cnt, returnPoints=False)

    if len(hull) < 3:
        return "Figura muy pequeña o inválida"

    defects = cv2.convexityDefects(cnt, hull)

    if defects is None or len(defects) == 0:
        return "Hembra (figura sin profundidad)"

    return "Macho (figura con profundidad)"*/