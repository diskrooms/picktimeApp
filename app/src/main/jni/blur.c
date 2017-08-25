#include <jni.h>
#include <string.h>
#include <stdio.h>
#include <android/log.h>
#include <android/bitmap.h>

#define LOG_TAG "libbitmaputils"
#define LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)

#define clamp(a,min,max) \
    ({__typeof__ (a) _a__ = (a); \
      __typeof__ (min) _min__ = (min); \
      __typeof__ (max) _max__ = (max); \
      _a__ < _min__ ? _min__ : _a__ > _max__ ? _max__ : _a__; })

// Based heavily on http://vitiy.info/Code/stackblur.cpp
// See http://vitiy.info/stackblur-algorithm-multi-threaded-blur-for-cpp/
// Stack Blur Algorithm by Mario Klingemann <mario@quasimondo.com>

static unsigned short const stackblur_mul[255] =
{
        512,512,456,512,328,456,335,512,405,328,271,456,388,335,292,512,
        454,405,364,328,298,271,496,456,420,388,360,335,312,292,273,512,
        482,454,428,405,383,364,345,328,312,298,284,271,259,496,475,456,
        437,420,404,388,374,360,347,335,323,312,302,292,282,273,265,512,
        497,482,468,454,441,428,417,405,394,383,373,364,354,345,337,328,
        320,312,305,298,291,284,278,271,265,259,507,496,485,475,465,456,
        446,437,428,420,412,404,396,388,381,374,367,360,354,347,341,335,
        329,323,318,312,307,302,297,292,287,282,278,273,269,265,261,512,
        505,497,489,482,475,468,461,454,447,441,435,428,422,417,411,405,
        399,394,389,383,378,373,368,364,359,354,350,345,341,337,332,328,
        324,320,316,312,309,305,301,298,294,291,287,284,281,278,274,271,
        268,265,262,259,257,507,501,496,491,485,480,475,470,465,460,456,
        451,446,442,437,433,428,424,420,416,412,408,404,400,396,392,388,
        385,381,377,374,370,367,363,360,357,354,350,347,344,341,338,335,
        332,329,326,323,320,318,315,312,310,307,304,302,299,297,294,292,
        289,287,285,282,280,278,275,273,271,269,267,265,263,261,259
};

static unsigned char const stackblur_shr[255] =
{
        9, 11, 12, 13, 13, 14, 14, 15, 15, 15, 15, 16, 16, 16, 16, 17,
        17, 17, 17, 17, 17, 17, 18, 18, 18, 18, 18, 18, 18, 18, 18, 19,
        19, 19, 19, 19, 19, 19, 19, 19, 19, 19, 19, 19, 19, 20, 20, 20,
        20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 21,
        21, 21, 21, 21, 21, 21, 21, 21, 21, 21, 21, 21, 21, 21, 21, 21,
        21, 21, 21, 21, 21, 21, 21, 21, 21, 21, 22, 22, 22, 22, 22, 22,
        22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22,
        22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 23,
        23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23,
        23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23,
        23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23,
        23, 23, 23, 23, 23, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24,
        24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24,
        24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24,
        24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24,
        24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24
};
///Gray algorithm body
void grayJob(unsigned char* src,                    ///< input image data
             unsigned int w,                        ///< image width
             unsigned int h                         ///< image height
             )
{
    for(int i = 0; i < w*h; i ++){
        //计算公式：Y(灰度) = 0.299*R + 0.587*G + 0.114*B
        int grayScale = (int)(src[4*i]*0.299 + src[4*i+1]*0.587 + src[4*i+2]*0.114);
        src[4*i] = grayScale;
        src[4*i+1] = grayScale;
        src[4*i+2] = grayScale;
    }
}

///Reverse algorithm body
void reverseJob(unsigned char* src,                    ///< input image data
                unsigned int w,                        ///< image width
                unsigned int h                         ///< image height
                )
{
    for(int i = 0; i < w*h; i ++){
        /*LOGI("第一个字节%x",src[0]);
        LOGI("第二个字节%x",src[1]);
        LOGI("第三个字节%x",src[2]);
        LOGI("第四个字节%x",src[3]);
        LOGI("第五个字节%x",src[4]);
        LOGI("第六个字节%x",src[5]);
        LOGI("第七个字节%x",src[6]);
        LOGI("第八个字节%x",src[7]);
        LOGI("第九个字节%x",src[8]);
        LOGI("第十个字节%x",src[9]);
        LOGI("第十一个字节%x",src[10]);
        break;*/
        int rScale = (255 - src[4*i]);         //r通道
        int gScale = (255 - src[4*i+1]);     //g通道
        int bScale = (255 - src[4*i+2]);     //b通道

        src[4*i] = rScale;
        src[4*i+1] = gScale;
        src[4*i+2] = bScale;
        /*LOGI("第一个字节%x",src[0]);
        LOGI("第二个字节%x",src[1]);
        LOGI("第三个字节%x",src[2]);
        LOGI("第四个字节%x",src[3]);
        LOGI("第五个字节%x",src[4]);
        LOGI("第六个字节%x",src[5]);
        LOGI("第七个字节%x",src[6]);
        LOGI("第八个字节%x",src[7]);
        LOGI("第九个字节%x",src[8]);
        LOGI("第十个字节%x",src[9]);
        LOGI("第十一个字节%x",src[10]);
        break;*/
    }

}

/// Stackblur algorithm body
void stackblurJob(unsigned char* src,                ///< input image data
                  unsigned int w,                    ///< image width
                  unsigned int h,                    ///< image height
                  unsigned int radius,               ///< blur intensity (should be in 2..254 range)
                  int cores,                         ///< total number of working threads
                  int core,                          ///< current thread number
                  int step                           ///< step of processing (1,2)
                  )
{
    unsigned int x, y, xp, yp, i;
    unsigned int sp;
    unsigned int stack_start;
    unsigned char* stack_ptr;

    unsigned char* src_ptr;
    unsigned char* dst_ptr;

    unsigned long sum_r;
    unsigned long sum_g;
    unsigned long sum_b;
    unsigned long sum_in_r;
    unsigned long sum_in_g;
    unsigned long sum_in_b;
    unsigned long sum_out_r;
    unsigned long sum_out_g;
    unsigned long sum_out_b;

    unsigned int wm = w - 1;
    unsigned int hm = h - 1;
    unsigned int w4 = w * 4;
    unsigned int div = (radius * 2) + 1;
    unsigned int mul_sum = stackblur_mul[radius];
    unsigned char shr_sum = stackblur_shr[radius];
    unsigned char stack[div * 3];

    if (step == 1)
    {
        int minY = core * h / cores;
        int maxY = (core + 1) * h / cores;

        for(y = minY; y < maxY; y++)
        {
            sum_r = sum_g = sum_b =
            sum_in_r = sum_in_g = sum_in_b =
            sum_out_r = sum_out_g = sum_out_b = 0;

            src_ptr = src + w4 * y; // start of line (0,y)

            for(i = 0; i <= radius; i++)
            {
                stack_ptr    = &stack[ 3 * i ];
                stack_ptr[0] = src_ptr[0];
                stack_ptr[1] = src_ptr[1];
                stack_ptr[2] = src_ptr[2];
                sum_r += src_ptr[0] * (i + 1);
                sum_g += src_ptr[1] * (i + 1);
                sum_b += src_ptr[2] * (i + 1);
                sum_out_r += src_ptr[0];
                sum_out_g += src_ptr[1];
                sum_out_b += src_ptr[2];
            }


            for(i = 1; i <= radius; i++)
            {
                if (i <= wm) src_ptr += 4;
                stack_ptr = &stack[ 3 * (i + radius) ];
                stack_ptr[0] = src_ptr[0];
                stack_ptr[1] = src_ptr[1];
                stack_ptr[2] = src_ptr[2];
                sum_r += src_ptr[0] * (radius + 1 - i);
                sum_g += src_ptr[1] * (radius + 1 - i);
                sum_b += src_ptr[2] * (radius + 1 - i);
                sum_in_r += src_ptr[0];
                sum_in_g += src_ptr[1];
                sum_in_b += src_ptr[2];
            }


            sp = radius;
            xp = radius;
            if (xp > wm) xp = wm;
            src_ptr = src + 4 * (xp + y * w); //   img.pix_ptr(xp, y);
            dst_ptr = src + y * w4; // img.pix_ptr(0, y);
            for(x = 0; x < w; x++)
            {
                int alpha = dst_ptr[3];
                dst_ptr[0] = clamp((sum_r * mul_sum) >> shr_sum, 0, alpha);
                dst_ptr[1] = clamp((sum_g * mul_sum) >> shr_sum, 0, alpha);
                dst_ptr[2] = clamp((sum_b * mul_sum) >> shr_sum, 0, alpha);
                dst_ptr += 4;

                sum_r -= sum_out_r;
                sum_g -= sum_out_g;
                sum_b -= sum_out_b;

                stack_start = sp + div - radius;
                if (stack_start >= div) stack_start -= div;
                stack_ptr = &stack[3 * stack_start];

                sum_out_r -= stack_ptr[0];
                sum_out_g -= stack_ptr[1];
                sum_out_b -= stack_ptr[2];

                if(xp < wm)
                {
                    src_ptr += 4;
                    ++xp;
                }

                stack_ptr[0] = src_ptr[0];
                stack_ptr[1] = src_ptr[1];
                stack_ptr[2] = src_ptr[2];

                sum_in_r += src_ptr[0];
                sum_in_g += src_ptr[1];
                sum_in_b += src_ptr[2];
                sum_r    += sum_in_r;
                sum_g    += sum_in_g;
                sum_b    += sum_in_b;

                ++sp;
                if (sp >= div) sp = 0;
                stack_ptr = &stack[sp*3];

                sum_out_r += stack_ptr[0];
                sum_out_g += stack_ptr[1];
                sum_out_b += stack_ptr[2];
                sum_in_r  -= stack_ptr[0];
                sum_in_g  -= stack_ptr[1];
                sum_in_b  -= stack_ptr[2];
            }

        }
    }

    // step 2
    if (step == 2)
    {
        int minX = core * w / cores;
        int maxX = (core + 1) * w / cores;

        for(x = minX; x < maxX; x++)
        {
            sum_r =    sum_g =    sum_b =
            sum_in_r = sum_in_g = sum_in_b =
            sum_out_r = sum_out_g = sum_out_b = 0;

            src_ptr = src + 4 * x; // x,0
            for(i = 0; i <= radius; i++)
            {
                stack_ptr    = &stack[i * 3];
                stack_ptr[0] = src_ptr[0];
                stack_ptr[1] = src_ptr[1];
                stack_ptr[2] = src_ptr[2];
                sum_r           += src_ptr[0] * (i + 1);
                sum_g           += src_ptr[1] * (i + 1);
                sum_b           += src_ptr[2] * (i + 1);
                sum_out_r       += src_ptr[0];
                sum_out_g       += src_ptr[1];
                sum_out_b       += src_ptr[2];
            }
            for(i = 1; i <= radius; i++)
            {
                if(i <= hm) src_ptr += w4; // +stride

                stack_ptr = &stack[3 * (i + radius)];
                stack_ptr[0] = src_ptr[0];
                stack_ptr[1] = src_ptr[1];
                stack_ptr[2] = src_ptr[2];
                sum_r += src_ptr[0] * (radius + 1 - i);
                sum_g += src_ptr[1] * (radius + 1 - i);
                sum_b += src_ptr[2] * (radius + 1 - i);
                sum_in_r += src_ptr[0];
                sum_in_g += src_ptr[1];
                sum_in_b += src_ptr[2];
            }

            sp = radius;
            yp = radius;
            if (yp > hm) yp = hm;
            src_ptr = src + 4 * (x + yp * w); // img.pix_ptr(x, yp);
            dst_ptr = src + 4 * x;               // img.pix_ptr(x, 0);
            for(y = 0; y < h; y++)
            {
                int alpha = dst_ptr[3];
                dst_ptr[0] = clamp((sum_r * mul_sum) >> shr_sum, 0, alpha);
                dst_ptr[1] = clamp((sum_g * mul_sum) >> shr_sum, 0, alpha);
                dst_ptr[2] = clamp((sum_b * mul_sum) >> shr_sum, 0, alpha);
                dst_ptr += w4;

                sum_r -= sum_out_r;
                sum_g -= sum_out_g;
                sum_b -= sum_out_b;

                stack_start = sp + div - radius;
                if(stack_start >= div) stack_start -= div;
                stack_ptr = &stack[3 * stack_start];

                sum_out_r -= stack_ptr[0];
                sum_out_g -= stack_ptr[1];
                sum_out_b -= stack_ptr[2];

                if(yp < hm)
                {
                    src_ptr += w4; // stride
                    ++yp;
                }

                stack_ptr[0] = src_ptr[0];
                stack_ptr[1] = src_ptr[1];
                stack_ptr[2] = src_ptr[2];

                sum_in_r += src_ptr[0];
                sum_in_g += src_ptr[1];
                sum_in_b += src_ptr[2];
                sum_r    += sum_in_r;
                sum_g    += sum_in_g;
                sum_b    += sum_in_b;

                ++sp;
                if (sp >= div) sp = 0;
                stack_ptr = &stack[sp*3];

                sum_out_r += stack_ptr[0];
                sum_out_g += stack_ptr[1];
                sum_out_b += stack_ptr[2];
                sum_in_r  -= stack_ptr[0];
                sum_in_g  -= stack_ptr[1];
                sum_in_b  -= stack_ptr[2];
            }
        }
    }
}

/**
 * 模糊
 */
JNIEXPORT void JNICALL Java_tech_startech_picktime_NativeBlurProcess_functionToBlur(JNIEnv* env, jclass clzz, jobject bitmapOut, jint radius, jint threadCount, jint threadIndex, jint round) {
    // Properties
    AndroidBitmapInfo   infoOut;
    void*               pixelsOut;
    int ret;

    // Get image info
    if ((ret = AndroidBitmap_getInfo(env, bitmapOut, &infoOut)) != 0) {
        LOGE("AndroidBitmap_getInfo() failed ! error=%d", ret);
        return;
    }

    // Check image
    if (infoOut.format != ANDROID_BITMAP_FORMAT_RGBA_8888) {
        LOGE("Bitmap format is not RGBA_8888!");
        LOGE("==> %d", infoOut.format);
        return;
    }

    // Lock all images
    if ((ret = AndroidBitmap_lockPixels(env, bitmapOut, &pixelsOut)) != 0) {
        LOGE("AndroidBitmap_lockPixels() failed ! error=%d", ret);
        return;
    }

    int h = infoOut.height;
    int w = infoOut.width;
    //去色(多线程处理去色会出现莫名其妙的问题)
    //grayJob((unsigned char*)pixelsOut, w, h);
    //反相(多线程处理反相会出现莫名其妙的问题)
    //reverseJob((unsigned char*)pixelsOut, w, h);
    //堆栈模糊
    stackblurJob((unsigned char*)pixelsOut, w, h, radius, threadCount, threadIndex, round);

    // Unlocks everything
    AndroidBitmap_unlockPixels(env, bitmapOut);
}

/**
 * 反相
 */
JNIEXPORT void JNICALL Java_tech_startech_picktime_NativeBlurProcess_functionToReserve(JNIEnv* env, jclass clzz, jobject bitmapOut){

    AndroidBitmapInfo   infoOut;
    void * pixelsOut;
    int ret;

    // Get image info
    if ((ret = AndroidBitmap_getInfo(env, bitmapOut, &infoOut)) != 0) {
        LOGE("AndroidBitmap_getInfo() failed ! error=%d", ret);
        return;
    }

    // Check image
    if (infoOut.format != ANDROID_BITMAP_FORMAT_RGBA_8888) {
        LOGE("Bitmap format is not RGBA_8888!");
        LOGE("==> %x", infoOut.format);
        return;
    }

    int h = infoOut.height;
    int w = infoOut.width;
    // Lock all images
    if ((ret = AndroidBitmap_lockPixels(env, bitmapOut, &pixelsOut)) != 0) {
        LOGE("AndroidBitmap_lockPixels() failed ! error=%d", ret);
        return;
    }
    //unsigned char* _pixelsOut = (unsigned char*)pixelsOut;
    //LOGI("起始R通道%8x",_pixelsOut[0]);
    //LOGI("起始R通道%8x",_pixelsOut[1]);
    //LOGI("起始R通道%8x",_pixelsOut[2]);
    //LOGI("起始R通道%8x",_pixelsOut[3]);
    //LOGI("起始R通道%8x",_pixelsOut[4]);
    //LOGI("起始R通道%8x",_pixelsOut[5]);
    //LOGI("起始R通道%8x",_pixelsOut[6]);
    //LOGI("起始R通道%8x",_pixelsOut[7]);
    //LOGI("起始R通道%8x",_pixelsOut[8]);
    //LOGI("起始R通道%8x",_pixelsOut[9]);
    //LOGI("起始R通道%8x",_pixelsOut[10]);
    reverseJob((unsigned char*)pixelsOut, w, h);
    // Unlocks everything
    AndroidBitmap_unlockPixels(env, bitmapOut);
}

/**
 * 去色
 */
JNIEXPORT void JNICALL Java_tech_startech_picktime_NativeBlurProcess_functionToGray(JNIEnv* env, jclass clzz, jobject bitmapOut){

    AndroidBitmapInfo   infoOut;
    void * pixelsOut;
    int ret;

    // Get image info
    if ((ret = AndroidBitmap_getInfo(env, bitmapOut, &infoOut)) != 0) {
        LOGE("AndroidBitmap_getInfo() failed ! error=%d", ret);
        return;
    }

    // Check image
    if (infoOut.format != ANDROID_BITMAP_FORMAT_RGBA_8888) {
        LOGE("Bitmap format is not RGBA_8888!");
        LOGE("==> %x", infoOut.format);
        return;
    }

    int h = infoOut.height;
    int w = infoOut.width;
    // Lock all images
    if ((ret = AndroidBitmap_lockPixels(env, bitmapOut, &pixelsOut)) != 0) {
        LOGE("AndroidBitmap_lockPixels() failed ! error=%d", ret);
        return;
    }
    grayJob((unsigned char*)pixelsOut, w, h);
    // Unlocks everything
    AndroidBitmap_unlockPixels(env, bitmapOut);
}

/**
 * 颜色合并
 */
JNIEXPORT jintArray JNICALL Java_tech_startech_picktime_NativeBlurProcess_functionToDescolor(JNIEnv* env, jclass clzz, jintArray beforeArray,jintArray afterArray,jint w,jint h){
    // 获取java中传入的像素数组值，jintArray转化成int指针数组
        jint *p_beforeBuffer = (*env)->GetIntArrayElements(env,beforeArray, JNI_FALSE);
        jint *p_afterBuffer = (*env)->GetIntArrayElements(env,afterArray, JNI_FALSE);
        jint len_afterBuffer = (*env)->GetArrayLength(env,afterArray);

        if(p_beforeBuffer == NULL ||  p_afterBuffer == NULL || (len_afterBuffer < w*h)){
            return 0;
        }

        int* ptr = p_beforeBuffer;
        int* ptr2 = p_afterBuffer;

        for(int i = 0; i < 100; i ++){
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
        return 0;
        //复制图像数据并返回
        int size = w * h;
        jintArray result = (*env)->NewIntArray(env,size);
        (*env)->SetIntArrayRegion(env,result, 0, size, p_beforeBuffer);
        // 更新java图片数组和释放c++中图片数组的值
        (*env)->ReleaseIntArrayElements(env,beforeArray, p_beforeBuffer, JNI_FALSE);
        (*env)->ReleaseIntArrayElements(env,afterArray, p_afterBuffer, JNI_FALSE);
        return result;

}