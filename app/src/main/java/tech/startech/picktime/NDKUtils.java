package tech.startech.picktime;

import android.graphics.Bitmap;

/**
 * Created by jsb-hdp-0 on 2017/8/22.
 */

public class NDKUtils {
    public static native int[] gray(int[] buf, int w, int h);       //C语言直接操作像素进行灰度转化
    public static native int[] reverse(int[] buf, int w, int h);    //取反
    public static native byte[] reverse2(Bitmap bitmap, int w, int h);    //取反
    public static native int[] blur(int[] buf, int w, int h);       //高斯模糊
    public static native int[] desColor(int[] beforeBuffer, int[] afterBuffer, int w, int h);   //颜色减淡
    public static native void sketch(Bitmap bitmap);                 //canny边缘检测
    public static native int[] gray2(int[] buf, int w, int h);       //调用 opencv的api进行灰度转化
    public static native Bitmap gradient(Bitmap bitmap);               //图像梯度

}
