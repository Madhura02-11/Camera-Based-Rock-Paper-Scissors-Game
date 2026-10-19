import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;
import org.opencv.highgui.HighGui;

public class CameraTest {

    public static void main(String[] args) {

        // Load OpenCV
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

        // Open webcam using DirectShow
        VideoCapture camera =
                new VideoCapture(0, Videoio.CAP_DSHOW);

        if (!camera.isOpened()) {
            System.out.println("❌ Camera could not be opened.");
            return;
        }

        // Set camera resolution
        camera.set(Videoio.CAP_PROP_FRAME_WIDTH, 640);
        camera.set(Videoio.CAP_PROP_FRAME_HEIGHT, 480);


        System.out.println("✅ Camera opened successfully!");

        Mat frame = new Mat();

        while (true) {

            boolean success = camera.read(frame);

            if (!success || frame.empty()) {
                System.out.println("❌ Frame not received.");
                continue;
            }

            System.out.println(
                    "Frame received: "
                    + frame.width() + " x " + frame.height()
            );

            // Show camera frame
            HighGui.imshow("Camera Test", frame);

            // Wait 30 milliseconds
            int key = HighGui.waitKey(30);

            // Press ESC to exit
            if (key == 27) {
                break;
            }
        }

        camera.release();
        HighGui.destroyAllWindows();

        System.out.println("Camera closed.");
    }
}