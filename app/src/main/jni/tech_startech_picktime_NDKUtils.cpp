#include <jni.h>
#include <string>
#include <iostream>
#include <stdio.h>
#include <stdlib.h>
#include <opencv2/opencv.hpp>
#include <android/log.h>

#include

// 定义了log日志宏函数，方便打印日志在logcat中查看调试
#define  TAG    "picktime"
#define LOGV(...) __android_log_print(ANDROID_LOG_VERBOSE, TAG, __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG , TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO , TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN , TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR , TAG, __VA_ARGS__)

using namespace cv;

/*extern "C"

  JNIEXPORT jintArray JNICALL Java_tech_startech_picktime_NDKUtils_gray(JNIEnv *env, jclass object,jintArray buf, int w, int h) {

      jint *cbuf;
      cbuf = env->GetIntArrayElements(buf, JNI_FALSE );
      if (cbuf == NULL) {
          return 0;
      }

      Mat imgData(h, w, CV_8UC4, (unsigned char *) cbuf);

      //获取图像数据首字节指针
      uchar* ptr = imgData.ptr(0);
      for(int i = 0; i < w*h; i ++){
          //计算公式：Y(亮度) = 0.299*R + 0.587*G + 0.114*B
          //对于一个int四字节，其彩色值存储方式为：BGRA 注意存储顺序
          int grayScale = (int)(ptr[4*i+2]*0.299 + ptr[4*i+1]*0.587 + ptr[4*i+0]*0.114);
          ptr[4*i+1] = grayScale;
          ptr[4*i+2] = grayScale;
          ptr[4*i] = grayScale;
          ptr[4*i+3] = 255;
      }

      int size = w * h;
      jintArray result = env->NewIntArray(size);
      env->SetIntArrayRegion(result, 0, size, cbuf);
      env->ReleaseIntArrayElements(buf, cbuf, 0);
      return result;
  }

//反相
extern "C"
JNIEXPORT jintArray JNICALL Java_tech_startech_picktime_NDKUtils_reverse(JNIEnv *env, jclass object, jintArray buf, int w, int h){
          jint * cbuf2 = env->GetIntArrayElements(buf, JNI_FALSE );
          if (cbuf2 == NULL) {
              return 0;
          }
          jintArray cbuf = env->NewIntArray(w * h);
          env->SetIntArrayRegion(cbuf, 0, w * h, cbuf2);
          jint * p_cbuf = env->GetIntArrayElements(cbuf, JNI_FALSE );
          Mat imgData(h, w, CV_8UC4, (unsigned char*)p_cbuf);
          //std::cout << imgData;
          //获取图像数据首字节指针
          uchar* ptr = imgData.ptr(0);
          //LOGV("ptr首地址%x",ptr);
          //LOGV("cbuf首地址%x",cbuf);

          for(int i = 0; i < w*h; i ++){
              //对于一个int四字节，其彩色值存储方式为：BGRA 注意存储顺序
              int rScale = (int)(255 - ptr[4*i+2]);     //r通道
              int gScale = (int)(255 - ptr[4*i+1]);     //g通道
              int bScale = (int)(255 - ptr[4*i]);       //b通道
              ptr[4*i+2] = rScale;
              ptr[4*i+1] = gScale;
              ptr[4*i] = bScale;

          }
          env->ReleaseIntArrayElements(buf, cbuf2, 0);
          return cbuf;
}

//高斯模糊
extern "C"
  JNIEXPORT jintArray JNICALL Java_tech_startech_picktime_NDKUtils_blur(JNIEnv *env, jclass object, jintArray buf, int w, int h){
            // 获取java中传入的像素数组值，jintArray转化成jint指针数组
            jint *c_pixels = env->GetIntArrayElements(buf, JNI_FALSE);
            if(c_pixels == NULL){
                return 0;
            }
            //LOGE("图片宽度：%d, 高度：%d", w, h);
            // 把兼容c语言的图片数据格式转化成opencv的图片数据格式
            // 使用Mat创建图片
            Mat mat_image_src(h, w, CV_8UC4, (unsigned char*) c_pixels);
            // 选择和截取一段行范围的图片
            //Mat temp = mat_image_src.rowRange(h / 3, 2 * w / 3);
            // 方框滤波
            // boxFilter(temp, temp, -1, Size(85, 85));
            // 均值滤波
            //blur(temp, temp, Size(85, 85));
            // 使用opencv的高斯模糊滤波
            GaussianBlur(mat_image_src, mat_image_src, Size(45, 13), 0, 0);
            // 将opencv图片转化成c图片数据，RGBA转化成灰度图4通道颜色数据
            cvtColor(mat_image_src, mat_image_src, CV_RGBA2GRAY, 4);

            //复制图像数据并返回
            int size = w * h;
            jintArray result = env->NewIntArray(size);
            env->SetIntArrayRegion(result, 0, size, c_pixels);
            // 更新java图片数组和释放c++中图片数组的值
            env->ReleaseIntArrayElements(buf, c_pixels, JNI_FALSE);
            return result;
  }

//颜色混合
extern "C"
JNIEXPORT jintArray JNICALL Java_tech_startech_picktime_NDKUtils_desColor(JNIEnv *env, jclass object, jintArray beforeArray, jintArray afterArray,jint w, jint h){
    // 获取java中传入的像素数组值，jintArray转化成jint指针数组
    jint *p_beforeBuffer = env->GetIntArrayElements(beforeArray, JNI_FALSE);
    jint *p_afterBuffer = env->GetIntArrayElements(afterArray, JNI_FALSE);
    jint len_afterBuffer = env->GetArrayLength(afterArray);

    if(p_beforeBuffer == NULL ||  p_afterBuffer == NULL || (len_afterBuffer < w*h)){
        return 0;
    }
    Mat imgData(h, w, CV_8UC4, (unsigned char *) p_beforeBuffer);
    Mat imgData2(h, w, CV_8UC4, (unsigned char *) p_afterBuffer);
    //打印指针的地址

    //获取图像数据首字节指针 base + (base * mix) / (255 - mix)
    uchar* ptr = imgData.ptr(0);
    uchar* ptr2 = imgData2.ptr(0);
    for(int i = 0; i < w*h; i ++){
        //int rScale = (int)(ptr[4*i+2] + (ptr[4*i+2] * ptr2[4*i+2]) / (255 - ptr2[4*i+2]));
        //rScale = rScale > 255 ? 255 : rScale;       //r通道

        //int gScale = (int)(ptr[4*i+1] + (ptr[4*i+1] * ptr2[4*i+1]) / (255 - ptr2[4*i+1]));
        //gScale = gScale > 255 ? 255 : gScale;       //g通道

        int bScale = (int)(ptr[4*i] + (ptr[4*i] * ptr2[4*i]) / (255 - ptr2[4*i]));
        bScale = bScale > 255 ? 255 : bScale;       //b通道
        ptr[4*i+3] = 255;
        ptr[4*i+2] = bScale;
        ptr[4*i+1] = bScale;
        ptr[4*i] = bScale;
    }
    //复制图像数据并返回
    int size = w * h;
    jintArray result = env->NewIntArray(size);
    env->SetIntArrayRegion(result, 0, size, p_beforeBuffer);
    // 更新java图片数组和释放c++中图片数组的值
    env->ReleaseIntArrayElements(beforeArray, p_beforeBuffer, JNI_FALSE);
    env->ReleaseIntArrayElements(afterArray, p_afterBuffer, JNI_FALSE);
    return result;
}*/

extern "C" {
JNIEXPORT void JNICALL Java_tech_startech_picktime_NDKUtils_sketch(JNIEnv *env, jclass object, jobject originBitmap){
    AndroidBitmapInfo   infoOut;
    void * pixelsOut;
    int ret;
    // Get image info
    if ((ret = AndroidBitmap_getInfo(env, originBitmap, &infoOut)) != 0) {
        LOGE("AndroidBitmap_getInfo() failed ! error=%d", ret);
        return;
    }
    // Check image
    if (infoOut.format != ANDROID_BITMAP_FORMAT_RGBA_8888) {
        LOGE("Bitmap format is not RGBA_8888!");
        LOGE("==> %x", infoOut.format);
        return;
    }
    int h = infoOut.height;         //图像高度
    int w = infoOut.width;          //图像宽度
    // Lock all images
    if ((ret = AndroidBitmap_lockPixels(env, originBitmap, &pixelsOut)) != 0) {
        LOGE("AndroidBitmap_lockPixels() failed ! error=%d", ret);
        return;
    }

    Mat kernel_x(1, 2, CV_32FC1);   //初始化矩阵 mat(row,col)
    Mat kernel_y(2, 1, CV_32FC1);
    kernel_x.at<float>(0, 0) = kernel_y.at<float>(0, 0) = -1;   //给两个自定义卷积赋值 at(row,col)
    kernel_x.at<float>(0, 1) = kernel_y.at<float>(1, 0) = 1;
    //将originBitmap转换为Mat
    Mat originMat(h,w,CV_8UC4,pixelsOut);

}