package tech.startech.picktime;

import android.graphics.Bitmap;

/**
 * Created by jsb-hdp-0 on 2017/8/22.
 */

public class NDKUtils {
    public static native int[] gray(int[] buf, int w, int h);       //灰度
    public static native int[] reverse(int[] buf, int w, int h);    //取反
    public static native int[] blur(int[] buf, int w, int h);       //高斯模糊
    public static native int[] desColor(int[] beforeBuffer, int[] afterBuffer, int w, int h);   //颜色减淡
    public static native void sketch(Bitmap bitmap);                 //canny边缘检测


}
