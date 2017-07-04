package paul.sdk_tesseract;

import android.Manifest;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.googlecode.tesseract.android.TessBaseAPI;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

// référence tesseract : http://imperialsoup.com/2016/04/29/simple-ocr-android-app-using-tesseract-tutorial/

public class MainActivity extends AppCompatActivity {

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    private static final String TAG = "SDKSolver - Tesseract";

    private TextView    tv;
    private ImageView   iv;
    private ImageView   iv_square;
    private TessBaseAPI mTess; //Tess API reference
    private String      datapath;

    private int     current_col;
    private int     current_row;
    private Bitmap  originalSdk;
    private Bitmap  editedSdk;
    private Mat     draw;

    Mat     square_color;
    Mat     square_gray;

    private Bitmap  squareRead;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    initMat();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tv = (TextView)findViewById(R.id.tv_result);
        iv = (ImageView)findViewById(R.id.iv_grid);
        iv_square = (ImageView)findViewById(R.id.iv_square);

        initOCR();
        initImg();
    }

    @Override
    public void onResume()
    {
        super.onResume();

        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    private void initOCR(){
        datapath = getFilesDir() + "/SDK_tesseract/";
        checkFile(new File(datapath + "tessdata/"));

        String lang = "eng";

        mTess = new TessBaseAPI();
        mTess.setDebug(true);
        mTess.init(datapath, lang);
    }

    private void initImg(){
        current_col = 0;
        current_row = 0;
        loadOriginalSdk();

    }

    private void initMat(){
        draw = new Mat(originalSdk.getHeight(), originalSdk.getHeight(), CvType.CV_8UC4);
    }

    private void loadOriginalSdk(){
        writeExternalStoragePermission();
        readExternalStoragePermission();

        String path = Environment.getExternalStorageDirectory().getPath() + "/sudokuSolver/sdk.bmp";
        originalSdk = BitmapFactory.decodeFile(path);
        editedSdk = BitmapFactory.decodeFile(path);
        iv.setImageBitmap(originalSdk);

    }

    private int readExternalStoragePermission(){
        int hasPermission = ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.READ_EXTERNAL_STORAGE);
        if (hasPermission != PackageManager.PERMISSION_GRANTED) {

            if(!ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                    Manifest.permission.READ_EXTERNAL_STORAGE)) {

                // TODO : Ajouter un dialogue

            }

            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    1);

            hasPermission = ContextCompat.checkSelfPermission(MainActivity.this,
                    Manifest.permission.READ_EXTERNAL_STORAGE);
        }
        return hasPermission;
    }
    private int writeExternalStoragePermission(){
        int hasPermission = ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (hasPermission != PackageManager.PERMISSION_GRANTED) {

            if(!ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

                // TODO : Ajouter un dialogue

            }

            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    1);

            hasPermission = ContextCompat.checkSelfPermission(MainActivity.this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        return hasPermission;
    }

    private void copyFiles() {
        try {
            //location we want the file to be at
            String filepath = datapath + "/tessdata/eng.traineddata";

            //get access to AssetManager
            AssetManager assetManager = getAssets();

            //open byte streams for reading/writing
            InputStream instream = assetManager.open("tessdata/eng.traineddata");
            OutputStream outstream = new FileOutputStream(filepath);

            //copy the file to the location specified by filepath
            byte[] buffer = new byte[1024];
            int read;
            while ((read = instream.read(buffer)) != -1) {
                outstream.write(buffer, 0, read);
            }
            outstream.flush();
            outstream.close();
            instream.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void checkFile(File dir) {
        //directory does not exist, but we can successfully create it
        if (!dir.exists()&& dir.mkdirs()){
            copyFiles();
        }
        //The directory exists, but there is no data file in it
        if(dir.exists()) {
            String datafilepath = datapath + "/tessdata/eng.traineddata";
            File datafile = new File(datafilepath);
            if (!datafile.exists()) {
                copyFiles();
            }
        }
    }

    public void read(View view){
        if(squareRead == null) {
            squareRead = Bitmap.createBitmap(square_gray.width(), square_gray.height(), Bitmap.Config.ARGB_8888);
        }
        Utils.matToBitmap(square_gray, squareRead);

        mTess.setImage(squareRead);
        String recognizedText = mTess.getUTF8Text();
        tv.setText(recognizedText);
    }

    public void up(View view){
        if(current_row != 0){
            current_row--;
            updateGrid();
        }
    }

    public void down(View view){
        if(current_row != 8){
            current_row++;
            updateGrid();
        }
    }

    public void left(View view){
        if(current_col != 0){
            current_col--;
            updateGrid();
        }
    }

    public void right(View view){
        if(current_col != 8){
            current_col++;
            updateGrid();
        }
    }

    Point   upperLeft;
    Point   lowerRight;
    Rect    roi;
    private void updateGrid(){
        int squareSize;
        squareSize = (int)(draw.size().width / 9);

        float deadSpaceRatio = 0.1f;
        int deadSpaceSize = (int)(squareSize * deadSpaceRatio);

        upperLeft = new Point(squareSize * current_col + deadSpaceSize,
                squareSize * current_row + deadSpaceSize);
        lowerRight = new Point(squareSize * (current_col + 1) - deadSpaceSize,
                squareSize * (current_row + 1) - deadSpaceSize);
        roi = new Rect(upperLeft, lowerRight);

        if(square_color == null){
            square_color = new Mat(roi.size(), CvType.CV_8UC4);
        }

        if(square_gray == null){
            square_gray = new Mat(roi.size(), CvType.CV_8UC1);
        }

        if(squareRead == null){
            squareRead = Bitmap.createBitmap(square_color.width(), square_color.height(), Bitmap.Config.ARGB_8888);
        }

        Utils.bitmapToMat(originalSdk, draw);
        square_color = draw.submat(roi);
        Imgproc.cvtColor(square_color, square_gray, Imgproc.COLOR_BGR2GRAY);

        //Imgproc.GaussianBlur(square_gray, square_gray, new Size(3,3), 3);
        Mat m_erode     = Imgproc.getStructuringElement(Imgproc.MORPH_ERODE, new Size(3,3));
        Mat m_dilate    = Imgproc.getStructuringElement(Imgproc.MORPH_DILATE, new Size(3,3));
        Imgproc.erode(square_gray, square_gray, m_erode);
        Imgproc.dilate(square_gray, square_gray, m_dilate);
        Imgproc.dilate(square_gray, square_gray, m_dilate);
        Imgproc.erode(square_gray, square_gray, m_erode);
        Imgproc.adaptiveThreshold(square_gray, square_gray, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 9, 7);
        Core.bitwise_not(square_gray, square_gray);

        Utils.matToBitmap(square_gray, squareRead);
        iv_square.setImageBitmap(squareRead);

        Imgproc.rectangle(draw, upperLeft, lowerRight, new Scalar(255,0,0), 4);
        Utils.matToBitmap(draw, editedSdk);
        iv.setImageBitmap(editedSdk);
    }

}
