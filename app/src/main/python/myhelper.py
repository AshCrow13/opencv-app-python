import cv2
import numpy as np
import base64
import io
import os
from os.path import dirname, join
from PIL import Image

def opencv_img_process(data):
    decoded_data = base64.b64decode(data)
    img_decoded_bytes = base64.b64decode(data.encode('utf-8'))
    imgpath = join(os.environ["EXTERNAL_STORAGE"], "DCIM/")
    imgfile = join(imgpath, 'received_img.png')

    file = open(imgfile, "wb")
    file.write(img_decoded_bytes)
    file.close()

    imgFromSavedPNGInGray = cv2.imread(imgfile, 0)

    return imgFromSavedPNGInGray.shape
