import cv2
import numpy as np
import base64

def decode_image(image_data):
    """Decodifica la imagen de Base64 a un arreglo numpy."""
    decoded_bytes = base64.b64decode(image_data)
    np_arr = np.frombuffer(decoded_bytes, np.uint8)
    img = cv2.imdecode(np_arr, cv2.IMREAD_GRAYSCALE)
    return img

def classify_shape(image_data):
    """Clasifica la imagen como 'macho' o 'hembra' analizando los contornos."""
    img = decode_image(image_data)
    if img is None:
        return "Error: No se pudo decodificar la imagen."

    _, thresh = cv2.threshold(img, 128, 255, cv2.THRESH_BINARY)
    contours, _ = cv2.findContours(thresh, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)

    if not contours:
        return "No se detectó ninguna figura."

    cnt = max(contours, key=cv2.contourArea)
    hull = cv2.convexHull(cnt, returnPoints=False)

    defects = cv2.convexityDefects(cnt, hull)

    if defects is not None and len(defects) > 3:
        return "Macho (figura con profundidad)"
    else:
        return "Hembra (figura sin profundidad)"
