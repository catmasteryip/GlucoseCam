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
    cnt_img = frame
    # height = frame.shape[0]
    # width = frame.shape[1]
    Aruco = ArUco()
    rectangle, ids = Aruco.detect(frame)
    warped = None
    patch = np.zeros_like(frame)
    rect_height = 1
    length = 0.0
    if rectangle is None:
        rectangle = [0]
    else:
        cnt_img = cv2.drawContours(cnt_img.copy(), rectangle, -1, (0, 255, 0), 3)
        warped = warping(frame, rectangle)
        # rect_height = np.max(warped.shape)
        rectangle = rectangle.reshape(-1)
        if warped is not None:
            rect, rect_height = find_length(warped)
            if rect is not None:
                x, y, w, h = rect
                start = (x, y)
                end = (x+w, y+h)
                patch = cv2.rectangle(
                    warped.copy(), start, end, (0, 255, 0), 2)
                # cv2.putText(cnt_img, f'{rectangle.reshape(-1).shape}', (25, 25),
                #             cv2.FONT_HERSHEY_SIMPLEX, 0.5, (0, 255, 0), 1)
                length = w/np.max(warped.shape)*10



    # conversion from opencv image to b64 string in jpg format:
    # https://numpy.org/doc/stable/reference/generated/numpy.ndarray.tobytes.html
    finalBytes = cv2.imencode('.jpg',cnt_img)[1].tobytes()
    patchBytes = cv2.imencode('.jpg',patch)[1].tobytes()


    return patchBytes, f'{length:.1f}cm', finalBytes