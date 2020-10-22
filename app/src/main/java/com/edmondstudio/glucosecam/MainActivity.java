package com.edmondstudio.glucosecam;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.WorkerThread;
import androidx.appcompat.app.AppCompatActivity;

//import com.camerakit.CameraKitView;
import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;
import com.otaliastudios.cameraview.CameraView;
import com.otaliastudios.cameraview.controls.PictureFormat;
import com.otaliastudios.cameraview.controls.Preview;
import com.otaliastudios.cameraview.frame.Frame;
import com.otaliastudios.cameraview.frame.FrameProcessor;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.List;

//import static com.camerakit.CameraKitView.*;

public class MainActivity extends AppCompatActivity {

    private ImageView imageView, imageView2;
    private TextView glucoseLevelText;
    private TextureView textureView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final CameraView camera = findViewById(R.id.camera);
        imageView = findViewById(R.id.imageView);
        imageView2 = findViewById(R.id.imageView2);
        glucoseLevelText = findViewById(R.id.glucoseLevel);
        camera.setPictureFormat(PictureFormat.JPEG);
        camera.setLifecycleOwner(this);

        final Handler handler = new Handler();
//        final View container = findViewById(R.id.container);
        camera.addFrameProcessor(new FrameProcessor() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            @WorkerThread
            public void process(@NonNull Frame frame) {
//                long time = frame.getTime();
//                Size size = frame.getSize();
//                int format = frame.getFormat();
//                int userRotation = frame.getRotationToUser();
//                int viewRotation = frame.getRotationToView();
                int frameWidth = frame.getSize().getWidth();
                int frameHeight = frame.getSize().getHeight();
                if (frame.getDataClass() == byte[].class) {
                    // Process byte array...
                    ByteArrayOutputStream out = new ByteArrayOutputStream();

                    byte[] data = frame.getData();
                    YuvImage yuvImage = new YuvImage(data, ImageFormat.NV21, frameWidth, frameHeight, null);
                    yuvImage.compressToJpeg(new Rect(0, 0, frame.getSize().getWidth(), frame.getSize().getHeight()), 90, out);
                    byte[] frameBytes = out.toByteArray();

                    PyResults pyResults = runpython(frameBytes);
                    final String length = pyResults.length;
//                    final float[] rectangleArray = pyResults.rect;

                    final Bitmap pythonBitmap = pyResults.bitmap;
                    final Bitmap uprightImageBitmap = rotate90(pythonBitmap);
//                    final Bitmap bitmap = Bitmap.createBitmap(imageView.getWidth(), imageView.getHeight(),Bitmap.Config.ARGB_8888);
//                    Canvas canvas = new Canvas(bitmap);
//                    Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
//                    paint.setColor(Color.GREEN);
//                    paint.setStyle(Paint.Style.STROKE);
//                    paint.setStrokeWidth(10);
//                    Path path = new Path();
//                    path.addCircle(0,0,50,Path.Direction.CW);
//                    path.addCircle(rectangleArray[0],rectangleArray[1],50,Path.Direction.CW);
//                    Log.i("python result",String.valueOf(rectangleArray[0]));
//                    if (rectangleArray.length == 8){
//                        for (int i = 2; i < rectangleArray.length; i+=2) {
//                            path.lineTo(rectangleArray[i],rectangleArray[i+1]);
//                            path.moveTo(rectangleArray[i],rectangleArray[i+1]);
////                            Log.i("rectangleCoord",String.valueOf(rectangleArray[i]));
//                        }
////                        path.lineTo(rectangleArray[-2],rectangleArray[-1]);
//                    }
//                    canvas.drawPath(path,paint);


                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            glucoseLevelText.setText(length);
                            imageView2.setImageBitmap(uprightImageBitmap);
//                            imageView2.setImageBitmap(pythonBitmap);
                        }
                    });

                } else if (frame.getDataClass() == Image.class) {
                    Image image = frame.getData();
                    // Process android.media.Image...
                }
            }
        });
    }

    private byte[] bitmap2byte(Bitmap bitmap){

        int size = bitmap.getRowBytes() * bitmap.getHeight();
        ByteBuffer byteBuffer = ByteBuffer.allocate(size);
        bitmap.copyPixelsToBuffer(byteBuffer);
        byte[] byteArray = byteBuffer.array();
        return byteArray;
    }

    public static Bitmap loadBitmapFromView(View v) {
        Bitmap b = Bitmap.createBitmap( v.getLayoutParams().width, v.getLayoutParams().height, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        v.layout(0, 0, v.getLayoutParams().width, v.getLayoutParams().height);
        v.draw(c);
        return b;
    }

    private Bitmap rotate90(Bitmap bitmapOrg){
        Matrix matrix = new Matrix();
        matrix.postRotate(90);
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmapOrg, bitmapOrg.getWidth(), bitmapOrg.getHeight(), true);
        Bitmap rotatedBitmap = Bitmap.createBitmap(scaledBitmap, 0, 0, scaledBitmap.getWidth(), scaledBitmap.getHeight(), matrix, true);

        return rotatedBitmap;
    }

    public class PyResults {
        private String length;
        private float[] rect;
        private Bitmap bitmap;

        public PyResults(String length, float[] rect, Bitmap bitmap) {
            this.length = length;
            this.rect = rect;
            this.bitmap = bitmap;
        }

    }

    private PyResults runpython(byte[] imageBytes){
        PyResults pyResults = new PyResults(null, null, null);
        if (!Python.isStarted()) {
            Python.start(new AndroidPlatform(MainActivity.this));
        } else {
            try{
                Python py = Python.getInstance();
                PyObject findPatch = py.getModule("findPatch");
                PyObject PyResultObjects = findPatch.callAttr("detect_patch", imageBytes);
                List PyResultList = PyResultObjects.asList();
                String length = (String) PyResultList.get(1).toString();
                PyObject PyRectangle = (PyObject) PyResultList.get(0);
                float[] rectangleArray = PyRectangle.toJava(float[].class);
//                Log.i("rectangle", String.valueOf(rectangleArray[0]));
                Log.e("python", "is running");

                PyObject finalImageBytes = (PyObject) PyResultList.get(2);
                byte[] finalImageByteArray = finalImageBytes.toJava(byte[].class);
                Bitmap bitmap = BitmapFactory.decodeByteArray(finalImageByteArray, 0, finalImageByteArray.length);

                pyResults.length = length;
                pyResults.rect = rectangleArray;
                pyResults.bitmap = bitmap;

            }catch (Exception e){
                Log.e("runpython Exception",e.toString());
            }
        }
        return pyResults;
    }

}
