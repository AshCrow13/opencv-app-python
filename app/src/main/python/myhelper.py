import cv2
import numpy as np
import base64
import os

def decode_image(image_data):
    """Decodifica una imagen en base64 a una imagen OpenCV."""
    try:
        decoded_bytes = base64.b64decode(image_data)
        np_arr = np.frombuffer(decoded_bytes, np.uint8)
        img = cv2.imdecode(np_arr, cv2.IMREAD_GRAYSCALE)
        return img
    except Exception as e:
        print(f"[ERROR] No se pudo decodificar la imagen: {str(e)}")
        return None

def compare_with_templates(encoded_image, image_paths):
    """Compara una imagen con una lista de plantillas y devuelve la mejor coincidencia."""
    try:
        image_paths = list(image_paths)

        if not image_paths:
            print("[DEBUG] Lista de imágenes vacía")
            return {"path": "", "similarity": 0.0, "match_name": ""}

        input_img = decode_image(encoded_image)
        if input_img is None:
            return {"error": "No se pudo decodificar la imagen de entrada"}

        best_score = -1
        best_path = ""

        for path in image_paths:
            if not os.path.exists(path):
                print(f"[DEBUG] Ruta no encontrada: {path}")
                continue

            template = cv2.imread(path, cv2.IMREAD_GRAYSCALE)
            if template is None:
                print(f"[DEBUG] No se pudo cargar plantilla: {path}")
                continue

            result = cv2.matchTemplate(input_img, template, cv2.TM_CCOEFF_NORMED)
            _, max_val, _, _ = cv2.minMaxLoc(result)

            print(f"[DEBUG] Comparando con: {os.path.basename(path)}, Similitud: {max_val:.4f}")

            if max_val > best_score:
                best_score = max_val
                best_path = path

        if not best_path or best_score < 0:
            print("[DEBUG] No se encontró ninguna coincidencia válida")
            return {"path": "", "similarity": 0.0, "match_name": ""}

        return {
            "path": best_path,
            "similarity": float(best_score),
            "match_name": os.path.basename(best_path)
        }

    except Exception as e:
        return {"error": f"Ocurrió un error al comparar: {str(e)}"}
