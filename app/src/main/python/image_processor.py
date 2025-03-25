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
    """Clasifica la figura como macho o hembra usando análisis de perfiles de intensidad."""
    img = decode_image(image_data)
    if img is None:
        return "Error: No se pudo decodificar la imagen."

    # Umbralizar para obtener binaria e identificar contornos
    _, binary = cv2.threshold(img, 128, 255, cv2.THRESH_BINARY_INV)

    # Encontrar el contorno más grande (la figura)
    contours, _ = cv2.findContours(binary, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
    if not contours:
        return "No se detectó ninguna figura."

    cnt = max(contours, key=cv2.contourArea)
    x, y, w, h = cv2.boundingRect(cnt)
    roi = img[y:y+h, x:x+w]

    # Calcular perfil de intensidad vertical (promedio por fila)
    profile = np.mean(roi, axis=1)
    mid = len(profile) // 2
    top_half = np.mean(profile[:mid])
    bottom_half = np.mean(profile[mid:])

    # Comparar zonas para inferir profundidad (hembra) o relieve (macho)
    if top_half < bottom_half - 10:
        result = "Hembra (figura con profundidad)"
    elif top_half > bottom_half + 10:
        result = "Macho (figura en relieve)"
    else:
        result = "Indeterminado"

    return result
