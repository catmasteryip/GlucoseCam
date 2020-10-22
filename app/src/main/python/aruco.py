import cv2
import numpy as np


class ArUco():
    '''
        ArUco detector
        Args:
        Attributes:
            dictionary: cv2 aruco dictionary, using 4x4 50bit code dictionary
            parameters: cv2 parameter module for aruco detection
            order_corners(List(int)): order of points taken from each aruco, 
                starting from top-left corner clockwise
    '''

    def __init__(self):
        # Initialize the detector parameters using default values
        self.dictionary = cv2.aruco.Dictionary_get(cv2.aruco.DICT_4X4_50)
        self.parameters = cv2.aruco.DetectorParameters_create()

    def detect(self, img):
        '''
            Return the detection kit rectangle coordinates
            Args:
                img(ndarray): BGR image
            Returns:
                rectangle(ndarray): contour of the rectangle
                markerIds(nparray(int)): marker ids detected
        '''
        # Detect the markers in the image
        markerCorners, markerIds, rejectedCandidates = cv2.aruco.detectMarkers(
            img, self.dictionary, parameters=self.parameters)
        # return None, markerIds
        rectangle = None
        if markerIds is not None:
            if len(markerIds) == 2:
                markerIds = markerIds.flatten()
                rectangle = np.full((1, 4, 2), 0)
                for markerId, markerCorner in zip(markerIds, markerCorners):
                    corners = markerCorner.reshape(-1, 2)
                    if markerId == 1:
                        rectangle[0][0] = corners[3]
                        rectangle[0][1] = corners[2]
                    elif markerId == 2:
                        rectangle[0][2] = corners[1]
                        rectangle[0][3] = corners[0]
        return rectangle, markerIds
