#include <jni.h>
#include <string>
#include <iostream>
#include <stdio.h>
#include <stdlib.h>
#include <opencv2/opencv.hpp>
#include <android/log.h>
#include <android/bitmap.h>

// 定义了log日志宏函数，方便打印日志在logcat中查看调试
#define  TAG    "picktime"
#define LOGV(...) __android_log_print(ANDROID_LOG_VERBOSE, TAG, __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG , TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO , TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN , TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR , TAG, __VA_ARGS__)

#define thresHold 100
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


extern "C"{
    //rgba图像处理去色
    //operator_flag 操作标识
    //1反相 2边缘检测
    JNIEXPORT jbyteArray JNICALL Java_tech_startech_picktime_NDKUtils_reverse2(JNIEnv *env, jclass object, jobject bitmap, int w, int h){
              AndroidBitmapInfo  info;
              void*              pixels = NULL;

              CV_Assert( AndroidBitmap_getInfo(env, bitmap, &info) >= 0 );
              //CV_Assert( src.dims == 2 && info.height == (uint32_t)src.rows && info.width == (uint32_t)src.cols );
              //判断来源  CV_8UC1-单通道灰度图像
              CV_Assert( AndroidBitmap_lockPixels(env, bitmap, &pixels) >= 0 );
              CV_Assert( pixels );
              for(int i = 0; i < w*h; i ++){
                  //对于一个int四字节，其彩色值存储方式为：BGRA 注意存储顺序
                  int rScale = (int)(*((uchar* )pixels+i*4));     //r通道
                  int gScale = (int)(*((uchar* )pixels+i*4+1));       //g通道
                  int bScale = (int)(*((uchar* )pixels+i*4+2));     //b通道
                  int alpha = (int)(*((uchar* )pixels+i*4+3));     //alpha通道
                  //LOGD("alpha %x",alpha);
                  //LOGD("b通道%x",bScale);
                  //LOGD("g通道%x",gScale);
                  //LOGD("r通道%x",rScale);
                  //LOGD("整数显示%x",*((int* )pixels+i)); //以整数显示是逆序的 比如rgba在内存中为64c8d2ff 但是整数显示为ffd2c864
                  int grayScale = rScale*0.299 + gScale*0.587 + bScale * 0.114;
                  //*((uchar*)pixels+i*4) = 255 - grayScale;
                  //*((uchar*)pixels+i*4+1) = 255 - grayScale;
                  //*((uchar*)pixels+i*4+2) = 255 - grayScale;
              }
              //初始化Mat数据结构
              Mat tmp(info.height, info.width, CV_8UC4, pixels);
              Mat gray(info.height, info.width, CV_8U);
              cvtColor(tmp,gray,CV_RGB2GRAY);       //(r+g+b)/3

              Mat result(info.height, info.width, CV_8U);
              //第一种访问mat的方式
              /*if(tmp.isContinuous()){
                //LOGD("行数%d",tmp.rows);
                //LOGD("列数%d",tmp.cols);
                //LOGD("通道数%d",tmp.channels());
                for(int i = 0;i < tmp.rows; i++){
                    uchar* pRow = (uchar*) tmp.ptr<uchar>(i);
                    for(int j=0;j < tmp.cols; pRow=pRow+4,j++){
                        //LOGD("列数%x",*pRow++);
                        int gray = (*pRow * 0.299 + (*pRow+1)*0.587 + (*pRow+2) * 0.114);
                        //LOGD("灰度%d",gray);
                        *pRow = gray;
                        *(pRow + 1) = gray;
                        *(pRow + 2) = gray;
                    }
                }
              }*/
              //第二种访问mat的方式
              /*for(int i=0;i < gray.rows;i++){
                uchar* ptr = result.ptr<uchar>(i);
                for(int j=0;j < gray.cols;j++){
                    jboolean above = abs(gray.at<uchar>(i,j) - gray.at<uchar>(i,j-1)) > thresHold;
                    jboolean bottom = abs(gray.at<uchar>(i,j) - gray.at<uchar>(i,j+1)) > thresHold;
                    jboolean left = abs(gray.at<uchar>(i,j) - gray.at<uchar>(i-1,j)) > thresHold;
                    jboolean right = abs(gray.at<uchar>(i,j) - gray.at<uchar>(i+1,j)) > thresHold;
                    jboolean aboveLeft = abs(gray.at<uchar>(i,j) - gray.at<uchar>(i-1,j-1)) > thresHold;
                    jboolean aboveRight = abs(gray.at<uchar>(i,j) - gray.at<uchar>(i+1,j-1)) > thresHold;
                    jboolean bottomRight = abs(gray.at<uchar>(i,j) - gray.at<uchar>(i+1,j+1)) > thresHold;
                    jboolean bottomLeft = abs(gray.at<uchar>(i,j) - gray.at<uchar>(i-1,j+1)) > thresHold;
                    if(above || bottom || left || right || aboveLeft || aboveRight || bottomRight || bottomLeft){
                        ptr[j] = 1;
                    } else {
                        ptr[j] = 0;
                    }
                }
              }*/
              Mat disColor;
              gray.copyTo(disColor);
              //灰度图反相(如果图像在内存中连续分布)
              if(gray.isContinuous()){
                  //LOGD("图像在内存中连续分布");
                  for(int i = 0;i < gray.rows;i++){
                    for(int j = 0;j < gray.cols;j++){
                        //LOGD("黑白%d",gray.data[i*gray.rows + j]);
                        gray.data[i*gray.cols + j] = 255 - gray.data[i*gray.cols + j];    //i要和cols匹配
                        //LOGD("黑白%d",gray.data[i*gray.rows + j]);
                    }
                  }
              } else {
                  //LOGD("图像在内存中不连续分布");

              }
              //高斯模糊
              GaussianBlur(gray, gray, Size(15,15), 0, BORDER_DEFAULT); //高斯模糊的核要确保是正奇数 否则会报 ksize Assertion failed 错误
              //颜色减淡公式  混合色 = 基色 + (基色 * 叠加色) / (255 - 叠加色);
              for(int i = 0;i < gray.rows;i++){
                  for(int j = 0;j < gray.cols;j++){
                      int base = disColor.data[i*gray.cols + j];
                      int add = gray.data[i*gray.cols + j];
                      int mix = base + (base * add) / (255 - add);
                      mix = (mix > 255) ? 255 : (mix < 0? 0 : mix);
                      gray.data[i*gray.cols + j] = mix;
                  }
              }
              jbyteArray resBuf = env->NewByteArray(gray.rows * gray.cols);
              env->SetByteArrayRegion(resBuf, 0, gray.rows * gray.cols, (jbyte*)gray.data);
              AndroidBitmap_unlockPixels(env, bitmap);
              return resBuf;
    }


}