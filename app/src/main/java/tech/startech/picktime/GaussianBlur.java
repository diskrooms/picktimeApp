package tech.startech.picktime;

import android.content.Context;
import android.graphics.Bitmap;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.support.v7.app.AppCompatActivity;

/**
 * Created by jsb-hdp-0 on 2017/8/24.
 * 这里存放效果欠佳的高斯模糊
 */

public class GaussianBlur extends AppCompatActivity{

    /**
     * 效果欠佳
     * 高斯模糊
     */
    private Bitmap gaussianBlur(Bitmap bitmapOrigin){
        final int RADIUS = 2;     //定义滤波矩阵半径
        /*final double[] filterMatrix = new double[]{
                0.0947416,0.118318,0.0947416,
                0.118318,0.147761,0.118318,
                0.0947416,0.118318,0.0947416};*/
        //final int[] filterMatrix = new int[]{1,1,1,1,-7,1,1,1,1};
        /*final double[] filterMatrix = new double[]{
                0.1111111,0.1111111,0.1111111,
                0.1111111,0.1111111,0.1111111,
                0.1111111,0.1111111,0.1111111};*/
        final double[] filterMatrix = new double[]{
                0.003663,0.014652,0.025641,0.014652,0.003663,
                0.014652,0.058608,0.095238,0.058608,0.014652,
                0.025641,0.095238,0.150183,0.095238,0.025641,
                0.014652,0.058608,0.095238,0.058608,0.014652,
                0.003663,0.014652,0.025641,0.014652,0.003663};

        int picHeight = bitmapOrigin.getHeight();
        int picWidth = bitmapOrigin.getWidth();
        int[] pixels = new int[picWidth * picHeight];
        int[] pixelsRes = new int[picWidth * picHeight];
        bitmapOrigin.getPixels(pixels, 0, picWidth, 0, 0, picWidth, picHeight);

        //只计算离图像边缘大于等于滤波矩阵半径的像素点
        for(int y = RADIUS;y < picHeight-RADIUS; y++){
            for(int x = RADIUS;x < picWidth-RADIUS; x++){

                int filterMatrixIndex = 0;       //在滤波矩阵中的索引
                int sumR = 0;                    //存放R通道滤波积和
                int sumG = 0;                    //存放G通道滤波积和
                int sumB = 0;                    //存放B通道滤波积和
                for(int tempY = y-RADIUS; tempY <= y + RADIUS; tempY++){
                    for(int tempX = x - RADIUS; tempX <= x + RADIUS; tempX++){
                        //sum += pixels[tempY * picWidth + tempX] * filterMatrix[filterMatrixIndex];
                        //filterMatrixIndex++;
                        int color = pixels[tempY * picWidth + tempX];
                        /*if(x ==100 && y ==100){
                            LogUtils.v(Integer.toHexString(color));
                        }*/
                        sumR += ((color & 0x00ff0000) >> 16) * filterMatrix[filterMatrixIndex];
                        sumG += ((color & 0x0000ff00) >> 8) * filterMatrix[filterMatrixIndex];
                        sumB += (color & 0x000000ff) * filterMatrix[filterMatrixIndex];
                        filterMatrixIndex++;
                    }
                }

                /*int r = sumR / (int)Math.pow(2*RADIUS+1,2);         //R滤波通道均值
                int g = sumG / (int)Math.pow(2*RADIUS+1,2);         //G滤波通道均值
                int b = sumB / (int)Math.pow(2*RADIUS+1,2);         //B滤波通道均值*/
                int r = (sumR > 255) ? 255 : sumR;
                int g = (sumG > 255) ? 255 : sumG;
                int b = (sumB > 255) ? 255 : sumB;

                pixelsRes[y*picWidth + x] = 255 << 24 | sumR << 16 | sumG << 8 | sumB;
                /*if(x ==100 && y ==100){
                    LogUtils.v(Integer.toHexString(b));
                }*/
                //LogUtils.v(pixelsRes[y*picWidth + x]);
                //break;
            }
            // break;
        }

        Bitmap bitmap = Bitmap.createBitmap(pixelsRes, picWidth, picHeight,
                Bitmap.Config.ARGB_8888);
        return bitmap;
    }

    /**
     * 效果欠佳
     * 简单高斯模糊
     */
    private void simpleGaussianBlur(int[] data, int width, int height, int radius, float sigma){
        float pa = (float) (1 / (Math.sqrt(2 * Math.PI) * sigma));
        float pb = -1.0f / (2 * sigma * sigma);

        // generate the Gauss Matrix
        float[] gaussMatrix = new float[radius * 2 + 1];
        float gaussSum = 0f;
        for (int i = 0, x = -radius; x <= radius; ++x, ++i) {
            float g = (float) (pa * Math.exp(pb * x * x));
            gaussMatrix[i] = g;
            gaussSum += g;
        }

        for (int i = 0, length = gaussMatrix.length; i < length; ++i) {
            gaussMatrix[i] /= gaussSum;
        }

        // x direction
        for (int y = 0; y < height; ++y) {
            for (int x = 0; x < width; ++x) {
                float r = 0, g = 0, b = 0;
                gaussSum = 0;
                for (int j = -radius; j <= radius; ++j) {
                    int k = x + j;
                    if (k >= 0 && k < width) {
                        int index = y * width + k;
                        int color = data[index];
                        int cr = (color & 0x00ff0000) >> 16;
                        int cg = (color & 0x0000ff00) >> 8;
                        int cb = (color & 0x000000ff);

                        r += cr * gaussMatrix[j + radius];
                        g += cg * gaussMatrix[j + radius];
                        b += cb * gaussMatrix[j + radius];

                        gaussSum += gaussMatrix[j + radius];
                    }
                }

                int index = y * width + x;
                int cr = (int) (r / gaussSum);
                int cg = (int) (g / gaussSum);
                int cb = (int) (b / gaussSum);

                data[index] = cr << 16 | cg << 8 | cb | 0xff000000;
            }
        }

        // y direction
        for (int x = 0; x < width; ++x) {
            for (int y = 0; y < height; ++y) {
                float r = 0, g = 0, b = 0;
                gaussSum = 0;
                for (int j = -radius; j <= radius; ++j) {
                    int k = y + j;
                    if (k >= 0 && k < height) {
                        int index = k * width + x;
                        int color = data[index];
                        int cr = (color & 0x00ff0000) >> 16;
                        int cg = (color & 0x0000ff00) >> 8;
                        int cb = (color & 0x000000ff);

                        r += cr * gaussMatrix[j + radius];
                        g += cg * gaussMatrix[j + radius];
                        b += cb * gaussMatrix[j + radius];

                        gaussSum += gaussMatrix[j + radius];
                    }
                }

                int index = y * width + x;
                int cr = (int) (r / gaussSum);
                int cg = (int) (g / gaussSum);
                int cb = (int) (b / gaussSum);
                data[index] = cr << 16 | cg << 8 | cb | 0xff000000;
            }
        }
    }

    /**
     * 效果欠佳
     * @param bitmapOrigin
     * @return
     */
    private Bitmap gaussianBlur4(Bitmap bitmapOrigin){
        int picHeight = bitmapOrigin.getHeight();
        int picWidth = bitmapOrigin.getWidth();

        int[] pixels = new int[picWidth * picHeight];
        bitmapOrigin.getPixels(pixels, 0, picWidth, 0, 0, picWidth, picHeight);

        int[] guassBlur = new int[pixels.length];

        for (int i = 0; i < picWidth; i++)
        {
            for (int j = 0; j < picHeight; j++)
            {
                int temp = picWidth * (j) + (i);
                if ((i == 0) || (i == picWidth - 1) || (j == 0) || (j == picHeight - 1))
                {
                    guassBlur[temp] = 0;
                }
                else
                {
                    int i0 = picWidth * (j - 1) + (i - 1);
                    int i1 = picWidth * (j - 1) + (i);
                    int i2 = picWidth * (j - 1) + (i + 1);
                    int i3 = picWidth * (j) + (i - 1);
                    int i4 = picWidth * (j) + (i);
                    int i5 = picWidth * (j) + (i + 1);
                    int i6 = picWidth * (j + 1) + (i - 1);
                    int i7 = picWidth * (j + 1) + (i);
                    int i8 = picWidth * (j + 1) + (i + 1);

                    int sum = pixels[i0] + 2 * pixels[i1] + pixels[i2] + 2 * pixels[i3] + 4 * pixels[i4] + 2 * pixels[i5] + pixels[i6] + 2 * pixels[i7] + pixels[i8];

                    sum /= 16;

                    guassBlur[temp] = sum;
                }
            }
        }
        Bitmap bitmap = Bitmap.createBitmap(guassBlur, picWidth, picHeight,
                Bitmap.Config.ARGB_8888);
        return bitmap;
    }

    /**
     * 高斯模糊 利用 RenderScript(闪退 原因未知)
     * @return
     */
    private Bitmap RenderScriptGaussianBlur(Bitmap bitmap){
        Bitmap outBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        // Instantiate a new Renderscript
        RenderScript rs = RenderScript.create(getApplicationContext());//RenderScript是Android在API 11之后加入的
        // Create an Intrinsic Blur Script using the Renderscript
        ScriptIntrinsicBlur blurScript = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));
        // Create the Allocations (in/out) with the Renderscript and the in/out bitmaps
        Allocation allIn = Allocation.createFromBitmap(rs, bitmap);
        Allocation allOut = Allocation.createFromBitmap(rs, outBitmap);
        // Set the radius of the blur
        blurScript.setRadius(25.f);
        // Perform the Renderscript
        blurScript.setInput(allIn);
        blurScript.forEach(allOut);
        // Copy the final bitmap created by the out Allocation to the outBitmap
        allOut.copyTo(outBitmap);
        // recycle the original bitmap
        bitmap.recycle();
        // After finishing everything, we destroy the Renderscript.
        rs.destroy();
        return outBitmap;
    }

    /**
     * 效果最佳(移植到Native JNI方式)
     * (stackblur 堆栈模糊算法)
     * @param pix        需要处理的Bitmap对象
     * @param radius            高斯模糊半径
     * @return
     */
    private int[] stackBlur(int[] pix,int w,int h, int radius) {

        /*Bitmap bitmap;
        if (canReuseInBitmap) {
            bitmap = sentBitmap;        //传递指针
        } else {
            bitmap = sentBitmap.copy(sentBitmap.getConfig(), true);
        }

        if (radius < 1) {
            return (null);
        }

        int w = bitmap.getWidth();
        int h = bitmap.getHeight();

        int[] pix = new int[w * h];
        bitmap.getPixels(pix, 0, w, 0, 0, w, h);*/

        int wm = w - 1;
        int hm = h - 1;
        int wh = w * h;
        int div = radius + radius + 1;

        int r[] = new int[wh];
        int g[] = new int[wh];
        int b[] = new int[wh];
        int rsum, gsum, bsum, x, y, i, p, yp, yi, yw;
        int vmin[] = new int[Math.max(w, h)];

        int divsum = (div + 1) >> 1;
        divsum *= divsum;
        int dv[] = new int[256 * divsum];
        for (i = 0; i < 256 * divsum; i++) {
            dv[i] = (i / divsum);
        }

        yw = yi = 0;

        int[][] stack = new int[div][3];
        int stackpointer;
        int stackstart;
        int[] sir;
        int rbs;
        int r1 = radius + 1;
        int routsum, goutsum, boutsum;
        int rinsum, ginsum, binsum;

        for (y = 0; y < h; y++) {
            rinsum = ginsum = binsum = routsum = goutsum = boutsum = rsum = gsum = bsum = 0;
            for (i = -radius; i <= radius; i++) {
                p = pix[yi + Math.min(wm, Math.max(i, 0))];
                sir = stack[i + radius];
                sir[0] = (p & 0xff0000) >> 16;
                sir[1] = (p & 0x00ff00) >> 8;
                sir[2] = (p & 0x0000ff);
                rbs = r1 - Math.abs(i);
                rsum += sir[0] * rbs;
                gsum += sir[1] * rbs;
                bsum += sir[2] * rbs;
                if (i > 0) {
                    rinsum += sir[0];
                    ginsum += sir[1];
                    binsum += sir[2];
                } else {
                    routsum += sir[0];
                    goutsum += sir[1];
                    boutsum += sir[2];
                }
            }
            stackpointer = radius;

            for (x = 0; x < w; x++) {

                r[yi] = dv[rsum];
                g[yi] = dv[gsum];
                b[yi] = dv[bsum];

                rsum -= routsum;
                gsum -= goutsum;
                bsum -= boutsum;

                stackstart = stackpointer - radius + div;
                sir = stack[stackstart % div];

                routsum -= sir[0];
                goutsum -= sir[1];
                boutsum -= sir[2];

                if (y == 0) {
                    vmin[x] = Math.min(x + radius + 1, wm);
                }
                p = pix[yw + vmin[x]];

                sir[0] = (p & 0xff0000) >> 16;
                sir[1] = (p & 0x00ff00) >> 8;
                sir[2] = (p & 0x0000ff);

                rinsum += sir[0];
                ginsum += sir[1];
                binsum += sir[2];

                rsum += rinsum;
                gsum += ginsum;
                bsum += binsum;

                stackpointer = (stackpointer + 1) % div;
                sir = stack[(stackpointer) % div];

                routsum += sir[0];
                goutsum += sir[1];
                boutsum += sir[2];

                rinsum -= sir[0];
                ginsum -= sir[1];
                binsum -= sir[2];

                yi++;
            }
            yw += w;
        }
        for (x = 0; x < w; x++) {
            rinsum = ginsum = binsum = routsum = goutsum = boutsum = rsum = gsum = bsum = 0;
            yp = -radius * w;
            for (i = -radius; i <= radius; i++) {
                yi = Math.max(0, yp) + x;

                sir = stack[i + radius];

                sir[0] = r[yi];
                sir[1] = g[yi];
                sir[2] = b[yi];

                rbs = r1 - Math.abs(i);

                rsum += r[yi] * rbs;
                gsum += g[yi] * rbs;
                bsum += b[yi] * rbs;

                if (i > 0) {
                    rinsum += sir[0];
                    ginsum += sir[1];
                    binsum += sir[2];
                } else {
                    routsum += sir[0];
                    goutsum += sir[1];
                    boutsum += sir[2];
                }

                if (i < hm) {
                    yp += w;
                }
            }
            yi = x;
            stackpointer = radius;
            for (y = 0; y < h; y++) {
                // Preserve alpha channel: ( 0xff000000 & pix[yi] )
                pix[yi] = (0xff000000 & pix[yi]) | (dv[rsum] << 16) | (dv[gsum] << 8) | dv[bsum];

                rsum -= routsum;
                gsum -= goutsum;
                bsum -= boutsum;

                stackstart = stackpointer - radius + div;
                sir = stack[stackstart % div];

                routsum -= sir[0];
                goutsum -= sir[1];
                boutsum -= sir[2];

                if (x == 0) {
                    vmin[y] = Math.min(y + r1, hm) * w;
                }
                p = x + vmin[y];

                sir[0] = r[p];
                sir[1] = g[p];
                sir[2] = b[p];

                rinsum += sir[0];
                ginsum += sir[1];
                binsum += sir[2];

                rsum += rinsum;
                gsum += ginsum;
                bsum += binsum;

                stackpointer = (stackpointer + 1) % div;
                sir = stack[stackpointer];

                routsum += sir[0];
                goutsum += sir[1];
                boutsum += sir[2];

                rinsum -= sir[0];
                ginsum -= sir[1];
                binsum -= sir[2];

                yi += w;
            }
        }

        //bitmap.setPixels(pix, 0, w, 0, 0, w, h);

        return pix;
    }
}
