package paul.sdk_tesseract;

import android.Manifest;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.os.Environment;
import android.provider.ContactsContract;
import android.provider.Settings;
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
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Range;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.features2d.FeatureDetector;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

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


    public void detect(View v) {
        Utils.bitmapToMat(originalSdk, draw);
        detectLetter(draw.getNativeObjAddr());
        Utils.matToBitmap(draw, editedSdk);
        iv.setImageBitmap(editedSdk);
        readGrid(draw);
    }

    void readGrid(Mat input){
        String result = "";
        Size size   = new Size(input.size().width/9, input.size().height/9);
        Mat inputbw = new Mat(input.size(), CvType.CV_8UC1);
        Mat submat  = new Mat(size, CvType.CV_8UC1);
        Bitmap bmp ;

        Imgproc.cvtColor(input, inputbw, Imgproc.COLOR_BGR2GRAY);
        bmp = Bitmap.createBitmap((int)size.width, (int)size.height, Bitmap.Config.ARGB_8888);



        for(int i = 0; i < 9; i++){
            if (i != 0){
                result += System.getProperty("line.separator");
            }
            for(int j = 0; j < 9; j++){
                submat = inputbw.submat((int)size.width * i, (int)size.width * (i + 1), (int)size.height * j, (int)size.height * (j + 1));
                if(Core.countNonZero( submat ) < 1){
                    result += "0";
                }
                else{
                    Utils.matToBitmap(submat, bmp);
                    mTess.setImage(bmp);
                    result += mTess.getUTF8Text();
                }
            }
        }

        result = result.replaceAll("[^\\d\\r\\n]", "");
        tv.setText(result);
    }

    public native void detectLetter(long drawAddr);
}
