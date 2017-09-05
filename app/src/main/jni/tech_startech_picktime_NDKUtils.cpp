#include <jni.h>
#include <string>
#include <iostream>
#include <vector>
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
    //打印Mat数据
    void LogCV_8U(const Mat& mat){
        LOGD("打印Mat高度:%d",mat.rows);
        LOGD("打印Mat宽度:%d",mat.cols);
        uchar* pData = mat.data;
        for(int i = 0; i < mat.rows; i++){
             for(int j = 0; j < mat.cols; j++){
                 LOGD("打印Mat:%d",pData[i*(mat.cols) + j]);
             }
        }
    }

    void LogCV_8S(const Mat& mat){
        LOGD("打印Mat高度:%d",mat.rows);
        LOGD("打印Mat宽度:%d",mat.cols);
        char* pData = (char*)mat.data;            //重要 uchar* 指针转为其他类型指针*才能访问
        for(int i = 0; i < mat.rows; i++){
             for(int j = 0; j < mat.cols; j++){
                 LOGD("打印Mat:%x",pData[i*(mat.cols) + j]);
             }
        }
        /*switch(mat.type()){
            case CV_8S:
                 pData = (char*)mat.data;            //重要 uchar* 指针转为其他类型指针*才能访问

                 break;
            case CV_16U:
                 pData = (ushort*)mat.data;
                 break;
            case CV_16S:
                 pData = (short*)mat.data;
                 break;
            case CV_32S:
                 pData = (int*)mat.data;
                 break;
            case CV_32F:
                 pData = (float*)mat.data;
                 break;
            case CV_64F:
                 pData = (double*)mat.data;
                 break;
            default:

                 break;
        }*/
    }

    void LogCV_16U(const Mat& mat){
        LOGD("打印Mat高度:%d",mat.rows);
        LOGD("打印Mat宽度:%d",mat.cols);
        ushort* pData = (ushort*)mat.data;
        for(int i = 0; i < mat.rows; i++){
             LOGD("打印Mat:第%d行",i+1);
             for(int j = 0; j < mat.cols; j++){
                 LOGD("打印Mat:%d",pData[i*(mat.cols) + j]);
             }
        }
    }

    void LogCV_16S(const Mat& mat){
        LOGD("打印Mat高度:%d",mat.rows);
        LOGD("打印Mat宽度:%d",mat.cols);
        short* pData = (short*)mat.data;
        for(int i = 0; i < mat.rows; i++){
             LOGD("打印Mat:第%d行",i+1);
             for(int j = 0; j < mat.cols; j++){
                 LOGD("打印Mat:%d",pData[i*(mat.cols) + j]);
             }
        }
    }

    void LogCV_32S(const Mat& mat){
        LOGD("打印Mat高度:%d",mat.rows);
        LOGD("打印Mat宽度:%d",mat.cols);
        int* pData = (int*)mat.data;
        for(int i = 0; i < mat.rows; i++){
             LOGD("打印Mat:第%d行",i+1);
             for(int j = 0; j < mat.cols; j++){
                 LOGD("打印Mat:%d",pData[i*(mat.cols) + j]);
             }
        }
    }

    void LogCV_32F(const Mat& mat){
        LOGD("打印Mat高度:%d",mat.rows);
        LOGD("打印Mat宽度:%d",mat.cols);
        float* pData = (float*)mat.data;
        for(int i = 0; i < mat.rows; i++){
             LOGD("打印Mat:第%d行",i+1);
             for(int j = 0; j < mat.cols; j++){
                 LOGD("打印Mat:%f",pData[i*(mat.cols) + j]);
             }
        }
    }

    void LogCV_64F(const Mat& mat){
        LOGD("打印Mat高度:%d",mat.rows);
        LOGD("打印Mat宽度:%d",mat.cols);
        double* pData = (double*)mat.data;
        for(int i = 0; i < mat.rows; i++){
             LOGD("打印Mat:第%d行",i+1);
             for(int j = 0; j < mat.cols; j++){
                 LOGD("打印Mat:%f",pData[i*(mat.cols) + j]);
             }
        }
    }



    //rgba图像处理去色
    //operator_flag 操作标识
    //1反相 2边缘检测
    JNIEXPORT jbyteArray JNICALL Java_tech_startech_picktime_NDKUtils_sketch(JNIEnv *env, jclass object, jobject bitmap, int w, int h){
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

    /**
     * 素描算法2
     * C++ 层的函数返回值要与java层的保持一致 否则会报一个 JNI invalid jobject error
     * 自定义卷积核计算梯度
     */
    JNIEXPORT  jbyteArray JNICALL Java_tech_startech_picktime_NDKUtils_sketch2(JNIEnv *env, jclass object, jobject bitmap) {
        AndroidBitmapInfo info;
        void *pixels = NULL;

        CV_Assert( AndroidBitmap_getInfo(env, bitmap, &info) >= 0 );
        CV_Assert( AndroidBitmap_lockPixels(env, bitmap, &pixels) >= 0 );
        CV_Assert( pixels );
        Mat tmp(info.height, info.width, CV_8UC4, pixels);
        Mat gray(info.height, info.width, CV_8U);
        cvtColor(tmp,gray,CV_RGB2GRAY);

        //自定义方向卷积 1.用数组字面量初始化mat 注意数组元素类型要与CvType保持一致 2.用at方法单点赋值进行mat初始化
        int x[9] = {-2,0,2,-2,0,2,-2,0,2};
        int y[9] = {-1,-1,-1,-1,9,-1,-1,-1,-1};
        Mat xKernel(3,3,CV_8S,x);
        Mat yKernel(3,3,CV_8S,y);
        /*xGradiant.at<int>(0,0) = -1;
        xGradiant.at<int>(0,1) = 0;
        xGradiant.at<int>(0,2) = 1;
        xGradiant.at<int>(1,0) = -1;
        xGradiant.at<int>(1,1) = 0;
        xGradiant.at<int>(1,2) = 1;
        xGradiant.at<int>(2,0) = -1;
        xGradiant.at<int>(2,1) = 0;
        xGradiant.at<int>(2,2) = 1;*/
        //LogCV_32S(xGradiant);
        Mat xGradiant;
        Mat yGradiant;
        Mat medianBlurMat;
        //filter2D(gray, xGradiant, gray.type(), xKernel);
        //filter2D(gray, yGradiant, gray.type(), yKernel);
        medianBlur(gray,medianBlurMat,7);                       //中值滤波
        Laplacian(medianBlurMat, medianBlurMat, CV_8U, 5);      //拉普拉斯边缘检测
        threshold(medianBlurMat, medianBlurMat, 127, 255, THRESH_BINARY_INV);   //反二进制阈值操作
        jbyteArray resBuf = env->NewByteArray(gray.rows * gray.cols);
        env->SetByteArrayRegion(resBuf, 0, gray.rows * gray.cols, (jbyte*)medianBlurMat.data);
        AndroidBitmap_unlockPixels(env, bitmap);
        return resBuf;
    }

    //逆时针90°操作mat
    void rorate90(const Mat& src,Mat & dst){
        transpose(src,dst);
        flip(dst,dst,0);
    }

    //逆时针180°操作mat
    void rorate180(const Mat& src,Mat & dst){
        flip(src,dst,-1);
    }
    //逆时针270°操作mat
    void rorate270(const Mat& src,Mat & dst){
        transpose(src,dst);
        flip(dst,dst,1);
    }

    //响应函数
    static std::vector<Mat>& getResponse(const Mat& gradientImg, std::vector<Mat>& G_i) {
        int maxIdx = 0;
        const int rows = G_i[0].rows;
        const int cols = G_i[0].cols;
        const int size = G_i.size();
        const float* gradientData = (float*)gradientImg.data;
        float** data = new float*[size];
        for (int i = 0; i < size; ++i)
            data[i] = (float*)G_i[i].data;

        int tmp;
        for (int r = 0; r < rows; ++r) {
            for (int c = 0; c < cols; ++c) {
                maxIdx = 0;
                tmp = r * cols + c;
                data[maxIdx][tmp] = gradientData[tmp];
                for (int i = 0; i < size; ++i) {
                    if (data[i][tmp] > data[maxIdx][tmp]) {
                        data[maxIdx][tmp] = 0;
                        maxIdx = i;
                        data[i][tmp] = gradientData[tmp];
                    } else {
                        data[i][tmp] = 0;
                    }
                }
            }
        }

        return G_i;
    }

    /**
     * 素描算法3
     */
    JNIEXPORT  jbyteArray JNICALL Java_tech_startech_picktime_NDKUtils_sketch3(JNIEnv *env, jclass object, jobject bitmap) {
        AndroidBitmapInfo info;
        void *pixels = NULL;

        CV_Assert( AndroidBitmap_getInfo(env, bitmap, &info) >= 0 );
        CV_Assert( AndroidBitmap_lockPixels(env, bitmap, &pixels) >= 0 );
        CV_Assert( pixels );
        const int h = info.height;
        const int w = info.width;
        Mat tmp(h, w, CV_8UC4, pixels);
        Mat gray;
        cvtColor(tmp,gray,CV_RGB2GRAY);
        //生成梯度图 1.用自定义卷积核实现
        float x[2] = {-1,1};
        float y[2] = {-1,1};
        Mat kernel_x(1, 2, CV_32FC1, x);
        Mat kernel_y(2, 1, CV_32FC1, y);
        kernel_x.at<float>(0, 0) = kernel_y.at<float>(0, 0) = -1;
        kernel_x.at<float>(0, 1) = kernel_y.at<float>(1, 0) = 1;
        LogCV_32F(kernel_x);
        LogCV_32F(kernel_y);
        Mat img_x;
        Mat img_y;
        Mat resGradient;
        filter2D(gray, img_x, -1, kernel_x);
        filter2D(gray, img_y, -1, kernel_y);
        pow(img_x, 2, img_x);
        pow(img_y, 2, img_y);
        resGradient = img_x + img_y;
        pow(resGradient, 0.5, resGradient);

        //2.手动循环像素点实现 注意边缘点
        /*Mat xGradient(h,w,CV_32S);
        Mat yGradient(h,w,CV_32S);
        Mat resGradient(h,w,CV_8U);
            //x方向梯度
        for(int i = 0; i < h; i++){
            //可读性更强代码
            for(int j = 0; j < w;j++){
                if(j == 0){
                    //处理左边缘点
                    xGradient.at<int>(i,j) = abs(gray.at<uchar>(i,j+1) -  gray.at<uchar>(i,j)) * 2;    //绝对值
                } else if(j == w-1){
                    //处理右边缘点
                    xGradient.at<int>(i,j) = abs(gray.at<uchar>(i,j) -  gray.at<uchar>(i,j-1)) * 2;
                } else {
                    xGradient.at<int>(i,j) = abs(gray.at<uchar>(i,j+1) -  gray.at<uchar>(i,j-1)) * 2;
                }
            }
            //效率更高代码(不需要判断分支)
            //xGradient.at<int>(i,0) = abs(gray.at<uchar>(i,1) -  gray.at<uchar>(i,0)) * 2;
            //for(int j = 1; j < w-1; j++){
                //xGradient.at<int>(i,j) = abs(gray.at<uchar>(i,j+1) -  gray.at<uchar>(i,j-1)) * 2;
            //}
            //xGradient.at<int>(i,w-1) = abs(gray.at<uchar>(i,w-1) -  gray.at<uchar>(i,w-2)) * 2;
        }
            //y方向梯度
        for(int i = 0; i < w; i++){
            //可读性更强代码
            for(int j = 0; j < h;j++){
                if(j == 0){
                    //处理上边缘点
                    yGradient.at<int>(j,i) = abs(gray.at<uchar>(j+1,i) -  gray.at<uchar>(j,i)) * 2;    //绝对值
                } else if(j == h-1){
                    //处理下边缘点
                    yGradient.at<int>(j,i) = abs(gray.at<uchar>(j,i) -  gray.at<uchar>(j-1,i)) * 2;
                } else {
                    yGradient.at<int>(j,i) = abs(gray.at<uchar>(j+1,i) -  gray.at<uchar>(j-1,i)) * 2;
                }
            }
            //效率更高代码(不需要判断分支)
            //yGradient.at<int>(0,i) = abs(gray.at<uchar>(1,i) -  gray.at<uchar>(0,i)) * 2;
            //for(int j = 1; j < w-1; j++){
                //yGradient.at<int>(j,i) = abs(gray.at<uchar>(j+1,i) -  gray.at<uchar>(j-1,i)) * 2;
            //}
            //yGradient.at<int>(h-1,i) = abs(gray.at<uchar>(h-1,i) -  gray.at<uchar>(h-2,i)) * 2;
        }
            //叠加x方向和y方向的梯度 然后转换成无符号数
        convertScaleAbs(min(xGradient + yGradient, 255), resGradient);*/

        //卷积核尺寸
        int kernel_len = 5;
        Mat kernel_90;
        Mat kernel_135;
        Mat kernel_180;
        Mat kernel_225;
        Mat kernel_270;
        Mat kernel_315;
        float scalar[25] = {0,-1,0,1,0,-1,-2,0,2,1,-2,-4,0,4,2,-1,-2,0,2,1,0,-1,0,1,0};
        float scalar2[25] = {0,0,1,1,2,0,0,2,4,1,-1,-2,0,2,1,-1,-4,-2,0,0,-2,-1,-1,0,0};
        Mat kernel_0(kernel_len, kernel_len, CV_32F, scalar); // 0° 方向
        Mat kernel_45(kernel_len, kernel_len, CV_32F, scalar2); // 45° 方向
        rorate90(kernel_0,kernel_90);           //90°方向
        rorate90(kernel_45,kernel_135);          //135°方向
        rorate180(kernel_0,kernel_180);         //180方向
        rorate180(kernel_45,kernel_225);          //225°方向
        rorate270(kernel_0,kernel_270);         //270°方向
        rorate270(kernel_45,kernel_315);         //315°方向
        //LogCV_32F(kernel_135);
        std::vector<Mat> directEight(8, Mat());         //8个Mat存放8个方向的卷积
        filter2D(resGradient, directEight[0], CV_8U, kernel_0);   //0°方向卷积
        filter2D(resGradient, directEight[1], -1, kernel_45);   //45°方向卷积
        filter2D(resGradient, directEight[2], -1, kernel_90);   //45°方向卷积

        jbyteArray resBuf = env->NewByteArray(h * w);
        env->SetByteArrayRegion(resBuf, 0, h * w, (jbyte*)resGradient.data);
        AndroidBitmap_unlockPixels(env, bitmap);
        return resBuf;

        //生成线条图
        /*int kernel_len = w < h ? w : h;
        //kernel_len = (kernel_len / 60) * 2 + 1;
        kernel_len = 7;
        Mat kernel_hori(1, kernel_len, CV_32FC1, cv::Scalar(0)); // 0 or 180
        Mat kernel_veri(kernel_len, 1, CV_32FC1, cv::Scalar(0)); // 90 or -90
        Mat kernel_diag(kernel_len, kernel_len, CV_32FC1, cv::Scalar(0)); // -135, -45, 45, 135
        kernel_hori.colRange(kernel_len / 2, kernel_len) = cv::Scalar(1);	// 0
        kernel_veri.rowRange(kernel_len / 2, kernel_len) = cv::Scalar(1);	// 90
        for (int i = 0; i < kernel_len / 2; ++i){	// -135
            kernel_diag.at<float>(i, i) = 1;
        }
        // 0, 45, 90, 135, 180, -135, -90, -45
        LogCV_32F(kernel_diag);
        std::vector<Mat> G_i(8, Mat());
        cv::filter2D(resGradient, G_i[0], CV_32FC1, kernel_hori);	// 0
        cv::flip(kernel_hori, kernel_hori, 1);
        cv::filter2D(resGradient, G_i[4], CV_32FC1, kernel_hori);	// 180
        cv::filter2D(resGradient, G_i[2], CV_32FC1, kernel_veri);	// 90
        cv::flip(kernel_veri, kernel_veri, 0);
        cv::filter2D(resGradient, G_i[6], CV_32FC1, kernel_veri);	// -90
        cv::filter2D(resGradient, G_i[5], CV_32FC1, kernel_diag);	// -135
        cv::flip(kernel_diag, kernel_diag, 1);
        cv::filter2D(resGradient, G_i[7], CV_32FC1, kernel_diag);	// -45
        cv::flip(kernel_diag, kernel_diag, 0);
        cv::filter2D(resGradient, G_i[1], CV_32FC1, kernel_diag);	// 45
        cv::flip(kernel_diag, kernel_diag, 1);
        cv::filter2D(resGradient, G_i[3], CV_32FC1, kernel_diag);	// 135

        std::vector<Mat>& C_i = getResponse(resGradient, G_i);
        cv::filter2D(C_i[4], C_i[4], -1, kernel_hori);	// 180
        cv::flip(kernel_hori, kernel_hori, 1);
        cv::filter2D(C_i[0], C_i[0], -1, kernel_hori);	// 0
        cv::filter2D(C_i[6], C_i[6], -1, kernel_veri);	// -90
        cv::flip(kernel_veri, kernel_veri, 0);
        cv::filter2D(C_i[2], C_i[2], -1, kernel_veri);	// 90
        cv::filter2D(C_i[3], C_i[3], -1, kernel_diag);	// 135
        cv::flip(kernel_diag, kernel_diag, 0);
        cv::filter2D(C_i[5], C_i[5], -1, kernel_diag);	// -135
        cv::flip(kernel_diag, kernel_diag, 1);
        cv::filter2D(C_i[7], C_i[7], -1, kernel_diag);	// -45
        cv::flip(kernel_diag, kernel_diag, 0);
        cv::filter2D(C_i[1], C_i[1], -1, kernel_diag);	// 45

        Mat S_ = Mat::zeros(resGradient.rows, resGradient.cols, CV_32FC1);
        for (int i = 0; i < C_i.size(); ++i){
            S_ += C_i[i];
        }
        double minVal, maxVal;
        cv::minMaxLoc(S_, &minVal, &maxVal);
        Mat pencilStroke;
        pencilStroke = (S_ - (float)minVal) / ((float)maxVal - (float)minVal);
        pencilStroke = 1 - pencilStroke;

        jbyteArray resBuf = env->NewByteArray(h * w);
        env->SetByteArrayRegion(resBuf, 0, h * w, (jbyte*)pencilStroke.data);
        AndroidBitmap_unlockPixels(env, bitmap);
        return resBuf;*/
    }


}