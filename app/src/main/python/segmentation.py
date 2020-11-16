import numpy as np
import cv2


def OtsuSegmentation(bgr):
    '''
        Carry out ostu's segmentation on image
        Args:
            bgr(ndarray): BGR image
        Returns:
            th3(ndarray): a segmented image
    '''
    hsv = cv2.cvtColor(bgr, cv2.COLOR_BGR2HSV)
    img = hsv[:, :, 1]

    # blur = cv2.GaussianBlur(img,(5,5),0)
    ret3,th3 = cv2.threshold(img,0,255,cv2.THRESH_BINARY+cv2.THRESH_OTSU)

    return th3


def find_contour(bgr):
    '''
        Return biggest contour from BGR image
        Args:
            bgr(ndarray): BGR image
        Returns:
            max_area_cnt(List(ndarray)): Biggest contour detected
    '''
    seg_img = OtsuSegmentation(bgr)
    seg_img = cv2.Canny(seg_img, 50, 100)
    cnts, hier = cv2.findContours(seg_img, cv2.RETR_EXTERNAL,
                                  cv2.CHAIN_APPROX_SIMPLE)
    area_cnts = sorted(cnts, key=cv2.contourArea, reverse=True)
    if len(area_cnts) > 0:
        max_area_cnt = area_cnts[0]
    else:
        max_area_cnt = None
    return max_area_cnt


def find_length(bgr):
    '''
        Return contour and maximum length of contour from a BGR image
        Args:
            bgr(ndarray): BGR image
        Returns:
            cnt(List(ndarray)): Biggest contour detected
            max(w,h): Maximum length of the contour
    '''
    cnt = find_contour(bgr)
    if cnt is not None:
        x, y, w, h = cv2.boundingRect(cnt)
        return (x, y, w, h), max(w, h)
    else:
        return None, None
