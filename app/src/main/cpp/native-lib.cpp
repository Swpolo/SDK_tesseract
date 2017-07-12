#include <jni.h>
#include <string>
#include <opencv2/core.hpp>
#include <opencv2/imgproc.hpp>
#include <opencv2/features2d.hpp>

using namespace std;
using namespace cv;

extern "C" {

    bool isInRange(KeyPoint kp, Size size) {
        int range = size.width / 9;
        int kp_x = (int) kp.pt.x % range;
        int kp_y = (int) kp.pt.y % range;

        if (!((kp_x > (range / 4)) && (kp_x < (3 * range / 4)))) {
            return false;
        }
        if (!((kp_y > (range / 4)) && (kp_y < (3 * range / 4)))) {
            return false;
        }
        return true;
    }

    JNIEXPORT void JNICALL
    Java_paul_sdk_1tesseract_MainActivity_detectLetter(
            JNIEnv *env,
            jobject /* this */,
            jlong addDraw) {

        Mat &draw = *(Mat *) addDraw;
        Mat gray = Mat(draw.size(), CV_8UC1);

        cvtColor(draw, gray, CV_BGRA2GRAY);

        int imgSize = (draw.rows / 9);
        imgSize = imgSize - (imgSize / 3);
        if (imgSize % 2 == 0) { imgSize--; }

        adaptiveThreshold(gray, gray, 255, CV_ADAPTIVE_THRESH_MEAN_C, CV_THRESH_BINARY, imgSize,
                          imgSize / 2
        );


        SimpleBlobDetector::Params params;

        params.filterByArea = true;
        params.maxArea = (float) (imgSize * imgSize);
        params.minArea = params.maxArea / 15.0f;

        params.filterByCircularity = false;
        params.filterByColor = false;
        params.filterByConvexity = false;
        params.filterByInertia = false;

        Ptr<SimpleBlobDetector> blobDetector = SimpleBlobDetector::create(params);


        vector<KeyPoint> keypoints;
        blobDetector->detect(gray, keypoints);

        cvtColor(gray, draw, CV_GRAY2BGRA);
        Mat draw2 = Mat(draw.size(), draw.type());
        draw.copyTo(draw2);

        //drawKeypoints(draw, keypoints, draw, Scalar(255,0,0), DrawMatchesFlags::DRAW_OVER_OUTIMG);

        for (int i = 0; i < keypoints.size(); i++) {
            if(isInRange(keypoints.at(i), draw.size())) {
            circle(draw2, keypoints.at(i).pt, imgSize / 2, Scalar(255, 255, 255), -1);

            }
        }

        bitwise_xor(draw, draw2, draw);
    }

}


