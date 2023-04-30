package com.example.objectdetectionimages;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.ImageView;
import org.tensorflow.lite.examples.detection.tflite.Detector;
import org.tensorflow.lite.examples.detection.tflite.TFLiteObjectDetectionAPIModel;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    private Detector detector;
    private static final String ESP32_CAM_IP_ADDRESS = "http://192.168.244.243/";
    private static final String ESP32_CAM_STREAM_PATH = "capture";
    private ImageView innerImage;
    private Bitmap image_received;
    private boolean mIsReceivingImages = false;
    private Handler mHandler = new Handler();
    private Runnable mRunnable;

//    Code added for Image Classification.
    private static final String MODEL_PATH = "CurrencyDetector.tflite";
    private static final boolean QUANT = true;
    private static final String LABEL_PATH = "currencyLabels.txt";
    private static final int INPUT_SIZE = 224;
    private Executor executor = Executors.newSingleThreadExecutor();
    private TextView textViewResult;
    private Button btnDetectObject, btnToggleCamera;
    private ImageView imageViewResult;
//    private CameraView cameraView;

    private Classifier classifier;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageViewResult = findViewById(R.id.imageView2);
        textViewResult = findViewById(R.id.textView2);
        textViewResult.setMovementMethod(new ScrollingMovementMethod());
        btnToggleCamera = findViewById(R.id.btnToggleCamera);
        btnDetectObject = findViewById(R.id.btnDetectObject);
        initTensorFlowAndLoadModel();

//

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                new DownloadImageTask().execute(ESP32_CAM_IP_ADDRESS + ESP32_CAM_STREAM_PATH);
                mHandler.postDelayed(this, 30);
            }
        }, 30);
    }

        public void  doInference(){
        //TODO convert image into bitmap and show image
        Bitmap bitmap = image_received;
        if(image_received== null)
            return;
//        innerImage.setImageBitmap(bitmap);

        //TODO pass image to model and get results
//        List<Detector.Recognition> list = detector.recognizeImage(bitmap);

        bitmap = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, false);
//
        imageViewResult.setImageBitmap(bitmap);
//
        final List<Classifier.Recognition> results = classifier.recognizeImage(bitmap);
        textViewResult.setText(results.toString());


        //TODO make bitmap mutable and get canvas to draw rectangles
//        final Bitmap mutableBmp = bitmap.copy(Bitmap.Config.ARGB_8888, true);
//        final Canvas canvas = new Canvas(mutableBmp);
//        Paint p = new Paint();
//        p.setColor(Color.RED);
//        p.setStyle(Paint.Style.STROKE);
//        p.setStrokeWidth(mutableBmp.getWidth() / 95);

//        Paint paintText = new Paint();
//        paintText.setColor(Color.BLUE);
//        paintText.setTextSize(mutableBmp.getWidth() / 10);
//        paintText.setFakeBoldText(true);
//
//        for(int i =0;i<list.size();i++) {
//            float CONFIDENCE_THRESHOLD = 0.3f;
//            if (list.get(i).getConfidence() > CONFIDENCE_THRESHOLD) {
//                canvas.drawText(list.get(i).getTitle(), list.get(i).getLocation().left, list.get(i).getLocation().top, paintText);
//                canvas.drawRect(list.get(i).getLocation(), p);
//            }
//        }
//        innerImage.setImageBitmap(mutableBmp);
    }



        private void initTensorFlowAndLoadModel() {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        classifier = TensorFlowImageClassifier.create(
                                getAssets(),
                                MODEL_PATH,
                                LABEL_PATH,
                                INPUT_SIZE,
                                QUANT);
                    } catch (final Exception e) {
                        throw new RuntimeException("Error initializing TensorFlow!", e);
                    }
                }
            });
        }

    @Override
        protected void onResume() {
            super.onResume();
            startReceivingImages();
        }

        @Override
        protected void onPause() {
            super.onPause();
            stopReceivingImages();
        }

        private void startReceivingImages() {
            if (!mIsReceivingImages) {
                mHandler.post(mRunnable);
                mIsReceivingImages = true;
            }
        }

        private void stopReceivingImages() {
            if (mIsReceivingImages) {
                mHandler.removeCallbacks(mRunnable);
                mIsReceivingImages = false;
            }
        }

        private class DownloadImageTask extends AsyncTask<String, Void, byte[]> {

            @Override
            protected byte[] doInBackground(String... urls) {
                String imageUrl = urls[0];
                InputStream inputStream = null;
                ByteArrayOutputStream outputStream = null;
                try {
                    URL url = new URL(imageUrl);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setDoInput(true);
                    connection.connect();
                    inputStream = connection.getInputStream();
                    outputStream = new ByteArrayOutputStream();
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                    return outputStream.toByteArray();
                } catch (IOException e) {
                    Log.e("Nothing", "Error downloading image from URL: " + imageUrl, e);
                    return null;
                } finally {
                    if (inputStream != null) {
                        try {
                            inputStream.close();
                        } catch (IOException e) {
                            Log.e("Nothing", "Error closing input stream", e);
                        }
                    }
                    if (outputStream != null) {
                        try {
                            outputStream.close();
                        } catch (IOException e) {
                            Log.e("Nothing man", "Error closing output stream", e);
                        }
                    }
                }
            }

            @Override
            protected void onPostExecute(byte[] imageBytes) {
                if (imageBytes != null) {
                    image_received= BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
                    if (image_received!= null)
                        doInference();
                    else
                        Log.d("tryLog", getString(R.string.error_for_image_null));
//                    in.setImageBitmap(bitmap);
                }
            }
        }
}