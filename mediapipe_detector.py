import cv2
import mediapipe as mp
import socket
import struct
import json
import math

from mediapipe.tasks import python
from mediapipe.tasks.python import vision


# =========================================================
# SETTINGS
# =========================================================

MODEL_PATH = "hand_landmarker.task"

HOST = "127.0.0.1"
PORT = 5000

CAMERA_WIDTH = 640
CAMERA_HEIGHT = 480


# =========================================================
# MEDIAPIPE HAND LANDMARKER
# =========================================================

base_options = python.BaseOptions(
    model_asset_path=MODEL_PATH
)

options = vision.HandLandmarkerOptions(
    base_options=base_options,
    running_mode=vision.RunningMode.VIDEO,
    num_hands=1,
    min_hand_detection_confidence=0.5,
    min_hand_presence_confidence=0.5,
    min_tracking_confidence=0.5
)

detector = vision.HandLandmarker.create_from_options(
    options
)


# =========================================================
# SOCKET SERVER
# =========================================================

server = socket.socket(
    socket.AF_INET,
    socket.SOCK_STREAM
)

server.setsockopt(
    socket.SOL_SOCKET,
    socket.SO_REUSEADDR,
    1
)

server.bind(
    (HOST, PORT)
)

server.listen(1)

print("====================================")
print(" MediaPipe Rock Paper Scissors")
print("====================================")
print("Waiting for Java application...")
print("Server:", HOST, PORT)


connection, address = server.accept()

print("Java connected:", address)


# =========================================================
# CAMERA
# =========================================================

camera = cv2.VideoCapture(
    0,
    cv2.CAP_DSHOW
)

camera.set(
    cv2.CAP_PROP_FRAME_WIDTH,
    CAMERA_WIDTH
)

camera.set(
    cv2.CAP_PROP_FRAME_HEIGHT,
    CAMERA_HEIGHT
)


if not camera.isOpened():

    print("ERROR: Could not open webcam.")

    connection.close()
    server.close()

    exit()


# =========================================================
# HELPER FUNCTIONS
# =========================================================

def distance(a, b):

    return math.sqrt(
        (a.x - b.x) ** 2
        +
        (a.y - b.y) ** 2
        +
        (a.z - b.z) ** 2
    )


def finger_is_up(
        landmarks,
        tip,
        pip
):

    return landmarks[tip].y < landmarks[pip].y


def detect_gesture(landmarks):

    # -------------------------------------
    # Finger landmarks
    # -------------------------------------

    INDEX_TIP = 8
    INDEX_PIP = 6

    MIDDLE_TIP = 12
    MIDDLE_PIP = 10

    RING_TIP = 16
    RING_PIP = 14

    PINKY_TIP = 20
    PINKY_PIP = 18

    # -------------------------------------
    # Finger states
    # -------------------------------------

    index_up = finger_is_up(
        landmarks,
        INDEX_TIP,
        INDEX_PIP
    )

    middle_up = finger_is_up(
        landmarks,
        MIDDLE_TIP,
        MIDDLE_PIP
    )

    ring_up = finger_is_up(
        landmarks,
        RING_TIP,
        RING_PIP
    )

    pinky_up = finger_is_up(
        landmarks,
        PINKY_TIP,
        PINKY_PIP
    )

    # -------------------------------------
    # Count fingers
    # -------------------------------------

    fingers = [
        index_up,
        middle_up,
        ring_up,
        pinky_up
    ]

    count = sum(fingers)

    # -------------------------------------
    # ROCK
    # -------------------------------------

    if count == 0:

        return "Rock"

    # -------------------------------------
    # PAPER
    # -------------------------------------

    if count == 4:

        return "Paper"

    # -------------------------------------
    # SCISSORS
    # -------------------------------------

    if (
        index_up
        and middle_up
        and not ring_up
        and not pinky_up
    ):

        return "Scissors"

    return "Unknown"


def send_data(
        connection,
        data
):

    connection.sendall(data)


# =========================================================
# MAIN LOOP
# =========================================================

timestamp = 0


try:

    while True:

        success, frame = camera.read()

        if not success:

            print(
                "Could not read webcam frame."
            )

            break

        # Mirror camera
        frame = cv2.flip(
            frame,
            1
        )

        # -----------------------------------------
        # Convert OpenCV BGR -> RGB
        # -----------------------------------------

        rgb_frame = cv2.cvtColor(
            frame,
            cv2.COLOR_BGR2RGB
        )

        # -----------------------------------------
        # MediaPipe Image
        # -----------------------------------------

        mp_image = mp.Image(
            image_format=mp.ImageFormat.SRGB,
            data=rgb_frame
        )

        timestamp += 33

        # -----------------------------------------
        # Detect hand
        # -----------------------------------------

        result = detector.detect_for_video(
            mp_image,
            timestamp
        )

        gesture = "Unknown"

        x = 0
        y = 0
        width = 0
        height = 0

        landmarks_data = []

        # =================================================
        # HAND FOUND
        # =================================================

        if result.hand_landmarks:

            landmarks = result.hand_landmarks[0]

            # ---------------------------------------------
            # Gesture
            # ---------------------------------------------

            gesture = detect_gesture(
                landmarks
            )

            # ---------------------------------------------
            # Convert landmarks to pixels
            # ---------------------------------------------

            points = []

            for landmark in landmarks:

                px = int(
                    landmark.x
                    * frame.shape[1]
                )

                py = int(
                    landmark.y
                    * frame.shape[0]
                )

                points.append(
                    (px, py)
                )

                landmarks_data.append({
                    "x": px,
                    "y": py
                })

            # ---------------------------------------------
            # Bounding box
            # ---------------------------------------------

            x_values = [
                p[0] for p in points
            ]

            y_values = [
                p[1] for p in points
            ]

            x = max(
                0,
                min(x_values) - 20
            )

            y = max(
                0,
                min(y_values) - 20
            )

            max_x = min(
                frame.shape[1] - 1,
                max(x_values) + 20
            )

            max_y = min(
                frame.shape[0] - 1,
                max(y_values) + 20
            )

            width = max_x - x
            height = max_y - y

            # =================================================
            # DRAW HAND LANDMARKS
            # =================================================

            connections = [
                (0, 1),
                (1, 2),
                (2, 3),
                (3, 4),

                (0, 5),
                (5, 6),
                (6, 7),
                (7, 8),

                (5, 9),
                (9, 10),
                (10, 11),
                (11, 12),

                (9, 13),
                (13, 14),
                (14, 15),
                (15, 16),

                (13, 17),
                (17, 18),
                (18, 19),
                (19, 20),

                (0, 17)
            ]

            # Draw connections
            for start, end in connections:

                cv2.line(
                    frame,
                    points[start],
                    points[end],
                    (0, 255, 0),
                    2
                )

            # Draw landmark points
            for px, py in points:

                cv2.circle(
                    frame,
                    (px, py),
                    5,
                    (0, 255, 0),
                    -1
                )

            # ---------------------------------------------
            # Bounding rectangle
            # ---------------------------------------------

            cv2.rectangle(
                frame,
                (x, y),
                (x + width, y + height),
                (0, 255, 0),
                2
            )

            # ---------------------------------------------
            # Gesture text
            # ---------------------------------------------

            cv2.putText(
                frame,
                "Gesture: " + gesture,
                (20, 40),
                cv2.FONT_HERSHEY_SIMPLEX,
                1,
                (0, 255, 0),
                2
            )

            # ---------------------------------------------
            # Coordinates
            # ---------------------------------------------

            cv2.putText(
                frame,
                f"X:{x} Y:{y}",
                (20, 75),
                cv2.FONT_HERSHEY_SIMPLEX,
                0.7,
                (0, 255, 0),
                2
            )

            cv2.putText(
                frame,
                f"W:{width} H:{height}",
                (20, 105),
                cv2.FONT_HERSHEY_SIMPLEX,
                0.7,
                (0, 255, 0),
                2
            )

        else:

            cv2.putText(
                frame,
                "Show your hand",
                (20, 40),
                cv2.FONT_HERSHEY_SIMPLEX,
                1,
                (0, 200, 255),
                2
            )

        # =================================================
        # ENCODE IMAGE
        # =================================================

        success_encode, buffer = cv2.imencode(
            ".jpg",
            frame,
            [
                cv2.IMWRITE_JPEG_QUALITY,
                80
            ]
        )

        if not success_encode:

            continue

        image_bytes = buffer.tobytes()

        # =================================================
        # JSON DATA
        # =================================================

        data = {
            "gesture": gesture,
            "x": x,
            "y": y,
            "width": width,
            "height": height,
            "landmarks": landmarks_data
        }

        json_data = json.dumps(
            data
        ).encode("utf-8")

        # =================================================
        # SEND JSON
        # =================================================

        connection.sendall(
            struct.pack(
                ">I",
                len(json_data)
            )
        )

        connection.sendall(
            json_data
        )

        # =================================================
        # SEND IMAGE
        # =================================================

        connection.sendall(
            struct.pack(
                ">I",
                len(image_bytes)
            )
        )

        connection.sendall(
            image_bytes
        )

        # =================================================
        # LOCAL CAMERA PREVIEW
        # =================================================

        cv2.imshow(
            "MediaPipe Hand Detection",
            frame
        )

        key = cv2.waitKey(1) & 0xFF

        if key == 27 or key == ord("q"):

            break


except Exception as e:

    print(
        "Error:",
        e
    )


finally:

    camera.release()

    cv2.destroyAllWindows()

    detector.close()

    connection.close()

    server.close()

    print(
        "MediaPipe server stopped."
    )