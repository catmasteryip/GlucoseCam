package com.edmondstudio.glucosecam;

import android.annotation.SuppressLint;
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
import android.view.Surface;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
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
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.otaliastudios.cameraview.CameraView;
import com.otaliastudios.cameraview.controls.PictureFormat;
import com.otaliastudios.cameraview.controls.Preview;
import com.otaliastudios.cameraview.frame.Frame;
import com.otaliastudios.cameraview.frame.FrameProcessor;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

//import static com.camerakit.CameraKitView.*;

public class MainActivity extends AppCompatActivity {

    private ImageView imageView, imageView2;
    private TextView glucoseLevelText;
    private TextureView textureView;
    private float x = 0f;
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
        final LineChart chart = findViewById(R.id.chart);

        long[] lengthArray;
        final List<Entry> chartList = new ArrayList<Entry>();

        final Button clearBtn = findViewById(R.id.button);
        final boolean[] clear = {false};

        clearBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (clear[0]){

                    clearBtn.setText("Clear");
                    clear[0] = false;
                }else{
                    chart.clear();
                    if (chartList.size()>0){
                        chartList.clear();
                    }
                    x = 0f;
                    clearBtn.setText("Start");
                    clear[0] = true;
                }
            }
        });

        final long[] start_time = {System.currentTimeMillis()};
        final long[] app_count = {System.currentTimeMillis()};

        camera.addFrameProcessor(new FrameProcessor() {
            @SuppressLint("LongLogTag")
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            @WorkerThread
            public void process(@NonNull Frame frame) {
                long time = System.currentTimeMillis();
                int frameWidth = frame.getSize().getWidth();
                int frameHeight = frame.getSize().getHeight();
                if (frame.getDataClass() == byte[].class) {
                    // Process byte array...
                    try {
                        ByteArrayOutputStream out = new ByteArrayOutputStream();

                        byte[] data = frame.getData();
                        YuvImage yuvImage = new YuvImage(data, ImageFormat.NV21, frameWidth, frameHeight, null);
                        yuvImage.compressToJpeg(new Rect(0, 0, frame.getSize().getWidth(), frame.getSize().getHeight()), 90, out);
                        byte[] frameBytes = out.toByteArray();

                        PyResults pyResults = runpython(frameBytes);
                        final String lengthCM = pyResults.length;

                        final Bitmap pythonBitmap = pyResults.bitmap;
                        final Bitmap uprightImageBitmap = rotate90(pythonBitmap);
                        final Bitmap patchBitmap = pyResults.patchbitmap;
                        final Bitmap finalPatchBitmap = rotate90(patchBitmap);

                        String lengthString = lengthCM.substring(0, 3);

                        float lengthFloat = Float.parseFloat(lengthString);
                        LineData chartData = null;
                        if ((lengthFloat > 0) && ((time - start_time[0]) > 1000) && !clear[0]) {
                            x += 1f;
                            Entry newX = new Entry(x, lengthFloat);
                            chartList.add(newX);
                            LineDataSet chartDataset = new LineDataSet(chartList, "Length");
                            chartData = new LineData(chartDataset);
                            chartData.setDrawValues(false);
                            chart.setData(chartData);
                            chart.invalidate();
                            start_time[0] = time;
                        }

                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                glucoseLevelText.setText(lengthCM);
                                imageView2.setImageBitmap(uprightImageBitmap);
                                imageView.setImageBitmap(finalPatchBitmap);
                            }
                        });

                    } catch(Exception e){
                        Log.e("addFrameProcessor Exception", e.toString());
                    }

                } else if(frame.getDataClass()==Image .class){
                    Image image = frame.getData();
                    // Process android.media.Image...
                }
            }
        });
    }

//    private byte[] bitmap2byte(Bitmap bitmap){
//
//        int size = bitmap.getRowBytes() * bitmap.getHeight();
//        ByteBuffer byteBuffer = ByteBuffer.allocate(size);
//        bitmap.copyPixelsToBuffer(byteBuffer);
//        byte[] byteArray = byteBuffer.array();
//        return byteArray;
//    }
//
//    public static Bitmap loadBitmapFromView(View v) {
//        Bitmap b = Bitmap.createBitmap( v.getLayoutParams().width, v.getLayoutParams().height, Bitmap.Config.ARGB_8888);
//        Canvas c = new Canvas(b);
//        v.layout(0, 0, v.getLayoutParams().width, v.getLayoutParams().height);
//        v.draw(c);
//        return b;
//    }

    private Bitmap rotate90(Bitmap bitmapOrg){
        Matrix matrix = new Matrix();
        matrix.postRotate(90);
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmapOrg, bitmapOrg.getWidth(), bitmapOrg.getHeight(), true);
        Bitmap rotatedBitmap = Bitmap.createBitmap(scaledBitmap, 0, 0, scaledBitmap.getWidth(), scaledBitmap.getHeight(), matrix, true);

        return rotatedBitmap;
    }

    public class PyResults {
        private String length;
        private Bitmap patchbitmap;
        private Bitmap bitmap;

        public PyResults(String length, Bitmap patchbitmap, Bitmap bitmap) {
            this.length = length;
            this.patchbitmap = patchbitmap;
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
                PyObject lengthPy = (PyObject) PyResultList.get(1);
                String length = (String) lengthPy.toString();
//                Log.i("length", PyResultList.get(1).getClass().getName());
                PyObject pyPatchBytes = (PyObject) PyResultList.get(0);
                byte[] pyPatchByteArray = pyPatchBytes.toJava(byte[].class);
                Bitmap patchbitmap = BitmapFactory.decodeByteArray(pyPatchByteArray, 0, pyPatchByteArray.length);

                PyObject finalImageBytes = (PyObject) PyResultList.get(2);
                byte[] finalImageByteArray = finalImageBytes.toJava(byte[].class);
                Bitmap bitmap = BitmapFactory.decodeByteArray(finalImageByteArray, 0, finalImageByteArray.length);

                pyResults.length = length;
                pyResults.patchbitmap = patchbitmap;
                pyResults.bitmap = bitmap;

            }catch (Exception e){
                Log.e("runpython Exception",e.toString());
            }
        }
        return pyResults;
    }

}
