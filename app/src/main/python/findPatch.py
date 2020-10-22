import numpy as np
import cv2
# import base64
from aruco import ArUco
from warping import warping
from segmentation import find_length

def detect_patch(javaImageBytes):
    # Find red patch and return contour+length from cv2 image
    # black = np.full(frame.shape, 0.)
    # conversion from image jarray of bytes to opencv mat:
    # https://github.com/chaquo/chaquopy/issues/303
    frame = cv2.imdecode(np.asarray(javaImageBytes), cv2.IMREAD_COLOR)
    height = frame.shape[0]
    width = frame.shape[1]
    Aruco = ArUco()
    rectangle, ids = Aruco.detect(frame)
    cnt_img = frame
    warped = None
    patch = None
    length = 0
    if rectangle is None:
        rectangle = [0]
    else:
        cnt_img = cv2.drawContours(cnt_img, rectangle, -1, (0, 255, 0), 3)
        warped = warping(frame, rectangle)
        rectangle = rectangle.reshape(-1)
        # rectangle[0]/=width
        # rectangle[1]/=height
        if warped is not None:
            rect, length = find_length(warped)



    # conversion from opencv image to b64 string in jpg format:
    # https://numpy.org/doc/stable/reference/generated/numpy.ndarray.tobytes.html
    finalBytes = cv2.imencode('.jpg',cnt_img)[1].tobytes()
    return rectangle, f'{length:.0f}px', finalBytes