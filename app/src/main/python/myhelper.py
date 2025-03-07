import cv2
import numpy as np
import base64

def decode_image(image_data):
    """Decodifica la imagen de Base64 a un arreglo numpy (Mat de OpenCV)."""
    decoded_bytes = base64.b64decode(image_data)
    np_arr = np.frombuffer(decoded_bytes, np.uint8)
    img = cv2.imdecode(np_arr, cv2.IMREAD_GRAYSCALE)
    return img

def compare_images(image_data, template_path):
    """Compara la imagen capturada con la plantilla usando ORB."""
    img1 = decode_image(image_data)
    img2 = cv2.imread(template_path, cv2.IMREAD_GRAYSCALE)

    if img1 is None or img2 is None:
        return "Error: No se pudo cargar la imagen"

    orb = cv2.ORB_create()
    kp1, des1 = orb.detectAndCompute(img1, None)
    kp2, des2 = orb.detectAndCompute(img2, None)

    if des1 is None or des2 is None:
        return "Error: No se pudo extraer descriptores."

    bf = cv2.BFMatcher(cv2.NORM_HAMMING, crossCheck=True)
    matches = bf.match(des1, des2)

    similarity = len(matches)
    if similarity > 50:
        return "Objeto encontrado (Coincidencias: {})".format(similarity)
    else:
        return "Objeto no encontrado (Coincidencias: {})".format(similarity)

def classify_shape(image_data):
    """Clasifica la figura como macho o hembra usando contornos y convexidad."""
    img = decode_image(image_data)
    if img is None:
        return "No se pudo decodificar la imagen"

    # Binarizar
    _, thresh = cv2.threshold(img, 128, 255, cv2.THRESH_BINARY)
    contours, _ = cv2.findContours(thresh, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)

    if not contours:
        return "No se detectó ninguna figura"

    # Tomamos el contorno más grande (por ejemplo)
    cnt = max(contours, key=cv2.contourArea)
    hull = cv2.convexHull(cnt, returnPoints=False)

    if len(hull) < 3:
        return "Figura muy pequeña o inválida"

    defects = cv2.convexityDefects(cnt, hull)

    # Si existen defectos de convexidad, asumimos que hay 'profundidad'
    if defects is not None and len(defects) > 3:
        return "Macho (figura con profundidad)"
    else:
        return "Hembra (figura sin profundidad)"
