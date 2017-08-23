package tech.startech.picktime;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.os.Handler;
import android.os.Message;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.apkfuns.logutils.LogUtils;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.GlideBitmapDrawable;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.GlideDrawableImageViewTarget;
import com.bumptech.glide.request.target.SimpleTarget;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;


public class BrowseImageActivity extends BaseActivity {
    private Bitmap bitmapOrigin;                              //维护一个存储原图Bitmap的变量
    private static final int LOAD_SOURCE_IMAGE_FINISHED = 1;  //消息类型 原图加载完成
    private ImageView browseImage;
    private ImageView resourceImage;
    private ImageView sketchImage;
    private ImageView rasterizeImage;
    private ImageView waterColorImage;

    private TextView resourceText;
    private Button saveImage;

    //各种状态变量
    private int currentStatus = 0;       //当前风格状态
    private final static String[] statusDesc = {"原图","素描","栅格化"};
    private TextView[] statusText = new TextView[3];

    /*private Handler handler = new Handler(){
        public void handleMessage(Message msg){
            if(msg.what == LOAD_SOURCE_IMAGE_FINISHED){
                browseImage.setImageBitmap(srcImageBitmap);
                //图片加载完成后绑定图片处理动作
                *//*desaturate.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Toast.makeText(BrowseImageActivity.this,"hello",Toast.LENGTH_LONG).show();
                    }
                });*//*
            }
        }
    };*/
    private MyApp myApplication;                         //维护一个application实例

    private void initUI(){
        browseImage = (ImageView) findViewById(R.id.browseImage);   //用于展示图片的imageview控件
        resourceImage = (ImageView) findViewById(R.id.resourceImage);       //原图
        sketchImage = (ImageView) findViewById(R.id.sketchImage);           //素描
        rasterizeImage = (ImageView) findViewById(R.id.rasterizeImage);     //栅格化
        waterColorImage = (ImageView) findViewById(R.id.waterColorImage);   //水彩画
        saveImage = (Button) findViewById(R.id.save);

        resourceText = (TextView) findViewById(R.id.resourceText);
        statusText[0] = resourceText;

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        myApplication = (MyApp) getApplication();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.browse_image);
        initUI();
        Intent intent = getIntent();
        String path = intent.getStringExtra("path");
        //加载已经预存的 bitmap
        //Bitmap bitmap = MainActivity.imagesBitmapMap.get(path);
        //Bitmap bitmap = myApplication.getImageFromMemoryCache(path);
        //browseImage.setImageBitmap(bitmap);
        //loadSrcImage(path);
        Glide.with(BrowseImageActivity.this).load(path).asBitmap().into(new SimpleTarget<Bitmap>() {
            @Override
            public void onResourceReady(final Bitmap bitmap, GlideAnimation anim) {
                browseImage.setImageBitmap(bitmap);
                bitmapOrigin = bitmap;

                //在这里添加一些图片加载完成的操作
                browseImage.setOnTouchListener(new View.OnTouchListener(){
                    public boolean onTouch(View v, MotionEvent event){
                        //Toast.makeText(BrowseImageActivity.this,event.getX()+"",Toast.LENGTH_SHORT).show();
                        //Bitmap bitmapOrigin = ((GlideBitmapDrawable)browseImage.getDrawable()).getBitmap();
                        int width = bitmapOrigin.getWidth();

                        int x = (int)event.getX();
                        int y = (int)event.getY();

                        int[] pixels = getPixelsFromBitmap(bitmapOrigin);
                        //Toast.makeText(BrowseImageActivity.this,y*width+x+"",Toast.LENGTH_SHORT).show();
                        return true;
                    }
                });

                /**
                 * 加载原图
                 */
                resourceImage.setOnClickListener(new View.OnClickListener(){
                    public void onClick(View v){
                        if(currentStatus != 0){
                            browseImage.setImageBitmap(bitmapOrigin);
                            currentStatus = 0;
                        }
                    }
                });

                /**
                 * 素描算法
                 */
                sketchImage.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if(currentStatus != 1) {
                            //废弃从控件中获取bitmap的方式 直接用一个变量维护bitmap的值
                            //Bitmap bitmapOrigin = ((GlideBitmapDrawable) browseImage.getDrawable()).getBitmap();
                            int height = bitmapOrigin.getHeight();
                            int width = bitmapOrigin.getWidth();
                            //定义灰度图像
                            /*Bitmap bmpGrayscale = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
                            Canvas canvas = new Canvas(bmpGrayscale);
                            Paint paint = new Paint();
                            ColorMatrix cm = new ColorMatrix();
                            cm.setSaturation(0);
                            ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);
                            paint.setColorFilter(f);
                            canvas.drawBitmap(bitmapOrigin, 0, 0, paint);
                            browseImage.setImageBitmap(bmpGrayscale);*/

                            int[] desaturatePixels = desaturate2(bitmapOrigin);
                            //Bitmap desaturateBitmap = Bitmap.createBitmap(desaturatePixels, width, height, Bitmap.Config.RGB_565);
                            //browseImage.setImageBitmap(desaturateBitmap);

                            //反相
                            //int[] pixels = getPixelsFromBitmap(desaturateBitmap);   //从源图像获取像素值
                            int[] reverseColorPixels = reverseColor(desaturatePixels,0);      //简单反相
                            //堆栈模糊
                            int[] stackBlurPixels = stackBlur(reverseColorPixels,width,height,10);

                            Bitmap bitmap = Bitmap.createBitmap(stackBlurPixels, width, height, Bitmap.Config.ARGB_8888);
                            browseImage.setImageBitmap(bitmap);
                            //修改状态
                            currentStatus = 1;
                        }
                    }
                });

                //自定义算法去色
                /*desaturate2.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //直接从ImageView控件中获取bitmap
                        Bitmap bitmapOrigin = ((GlideBitmapDrawable)browseImage.getDrawable()).getBitmap();
                        //Bitmap bitmap = desaturate(bitmapOrigin);
                        //LogUtils.v(browseImage.getWidth());
                        //LogUtils.v(browseImage.getHeight());
                        int[] pixels = getPixelsFromBitmap(bitmapOrigin);   //从源图像获取像素值
                        //LogUtils.v(Integer.toHexString(pixels[100]));
                        desaturate_(pixels);
                        //LogUtils.v(Integer.toHexString(pixels[100]));
                        Bitmap bitmap = Bitmap.createBitmap(pixels, bitmapOrigin.getWidth(), bitmapOrigin.getHeight(),
                                Bitmap.Config.ARGB_8888);
                        browseImage.setImageBitmap(bitmap);
                    }
                });

                //素描
                sketch.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Bitmap bitmapOrigin = ((GlideBitmapDrawable)browseImage.getDrawable()).getBitmap();
                        int[] pixels = getPixelsFromBitmap(bitmapOrigin);   //从源图像获取像素值
                        int[] desaturate_pixels = desaturate_(pixels);         //1.去色
                        //LogUtils.v(Integer.toHexString(pixels[100]));
                        int[] reversecolor_pixels = reverseColor(pixels);      //2.反相
                        //LogUtils.v(Integer.toHexString(pixels[100]));
                        gaussianBlur_(pixels,bitmapOrigin.getWidth(),bitmapOrigin.getHeight(),10);    //3.高斯模糊
                        mixColor(pixels,desaturate_pixels);
                        Bitmap bitmap = Bitmap.createBitmap(pixels, bitmapOrigin.getWidth(), bitmapOrigin.getHeight(),
                                Bitmap.Config.ARGB_8888);
                        browseImage.setImageBitmap(bitmap);
                    }
                });

                //高斯模糊
                gaussian.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Bitmap bitmapOrigin = ((GlideBitmapDrawable)browseImage.getDrawable()).getBitmap();
                        //Bitmap bitmap = createBitmap();
                        Bitmap bitmap2 = gaussianBlur5(bitmapOrigin);
                        browseImage.setImageBitmap(bitmap2);
                    }
                });*/

                //保存Bitmap
                saveImage.setOnClickListener(new View.OnClickListener(){
                    public void onClick(View v){
                        Bitmap bitmapOrigin = ((BitmapDrawable)browseImage.getDrawable()).getBitmap();
                        try {
                            saveBitmap(bitmapOrigin, "123.png");
                        } catch (IOException e){
                            e.printStackTrace();
                        }
                    }
                });
            }
        });

        //加载图片到原图按钮
        Glide.with(BrowseImageActivity.this).load(path).into(resourceImage);
    }

    /**
     * 从图像Bitmap 获取像素值数组
     * bitmapOrigin 源图像
     * @return pixels 返回的像素值数组
     */
    private int[] getPixelsFromBitmap(Bitmap bitmapOrigin){
        int picHeight = bitmapOrigin.getHeight();
        int picWidth = bitmapOrigin.getWidth();
        //LogUtils.v(picHeight);
        //LogUtils.v(picWidth);
        int[] pixels = new int[picWidth * picHeight];
        bitmapOrigin.getPixels(pixels, 0, picWidth, 0, 0, picWidth, picHeight);
        return pixels;
    }

    /**
     * 盛行的 0.299-0.587-0.114去色算法
     * bitmapOrigin Bitmap 源图像
     * @return 处理过的bitmap
     */
    private Bitmap desaturate(Bitmap bitmapOrigin){
        int picHeight = bitmapOrigin.getHeight();
        int picWidth = bitmapOrigin.getWidth();

        int[] pixels = new int[picWidth * picHeight];
        bitmapOrigin.getPixels(pixels, 0, picWidth, 0, 0, picWidth, picHeight);

        for (int i = 0; i < picHeight; ++i) {
            for (int j = 0; j < picWidth; ++j) {
                int index = i * picWidth + j;
                int color = pixels[index];
                int r = (color & 0x00ff0000) >> 16;     //R值
                int g = (color & 0x0000ff00) >> 8;      //G值
                int b = (color & 0x000000ff);           //B值
                int grey = (int) (r * 0.299 + g * 0.587 + b * 0.114);
                pixels[index] = grey << 16 | grey << 8 | grey | 0xff000000;
            }
        }
        Bitmap bitmap = Bitmap.createBitmap(pixels, picWidth, picHeight,
                Bitmap.Config.ARGB_8888);
        return bitmap;
    }

    /**
     * 盛行的 0.299-0.587-0.114去色算法
     * bitmapOrigin Bitmap 源图像
     * @return int[] 处理后的像素值数组
     */

    private int[] desaturate2(Bitmap bitmapOrigin){
        int picHeight = bitmapOrigin.getHeight();
        int picWidth = bitmapOrigin.getWidth();

        int[] pixels = new int[picWidth * picHeight];
        bitmapOrigin.getPixels(pixels, 0, picWidth, 0, 0, picWidth, picHeight);

        for (int i = 0; i < picHeight; ++i) {
            for (int j = 0; j < picWidth; ++j) {
                int index = i * picWidth + j;
                int color = pixels[index];
                int r = (color & 0x00ff0000) >> 16;     //R值
                int g = (color & 0x0000ff00) >> 8;      //G值
                int b = (color & 0x000000ff);           //B值
                int grey = (int) (r * 0.299 + g * 0.587 + b * 0.114);
                pixels[index] = grey << 16 | grey << 8 | grey | 0xff000000;
            }
        }
        return pixels;
    }


    /**
     *  反相
     *  bitmapOrigin 源图像
     *  返回 bitmap
     */
    private Bitmap reverseColor(Bitmap bitmapOrigin){
        int picHeight = bitmapOrigin.getHeight();
        int picWidth = bitmapOrigin.getWidth();

        int[] pixels = new int[picWidth * picHeight];
        bitmapOrigin.getPixels(pixels, 0, picWidth, 0, 0, picWidth, picHeight);
        for (int i = 0; i < picHeight; ++i) {
            for (int j = 0; j < picWidth; ++j) {
                int index = i * picWidth + j;
                int color = pixels[index];
                int r = 255 - (color & 0x00ff0000) >> 16;     //R值与255的差值
                int g = 255 - (color & 0x0000ff00) >> 8;      //G值与255的差值
                int b = 255 - (color & 0x000000ff);           //B值与255的差值
                pixels[index] = r << 16 | g << 8 | b | 0xff000000;
            }
        }
        Bitmap bitmap = Bitmap.createBitmap(pixels, picWidth, picHeight,
                Bitmap.Config.ARGB_8888);
        return bitmap;
    }

    /**
     * 反相
     * int[] pixels 像素值数组
     * int type     类型 type=1 复杂处理三个通道都进行反相计算   0简单处理 只计算blue通道 然后复制到red和green通道
     * @return 经过处理的像素数组
     */
    private int[] reverseColor(int[] pixels,int type){
        for (int i = 0; i < pixels.length; ++i) {
            int color = pixels[i];
            int r = 255 - ((color & 0x00ff0000) >> 16);     //R值与255的差值 注意运算符执行顺序 >> 运算需要加括号
            int g = 255 - ((color & 0x0000ff00) >> 8);      //G值与255的差值
            int b = 255 - (color & 0x000000ff);           //B值与255的差值
            if(type == 1) {
                pixels[i] = r << 16 | g << 8 | b | 0xff000000;
            } else {
                pixels[i] = b << 16 | b << 8 | b | 0xff000000;
            }
        }
        return pixels;
    }



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
     * 高斯模糊 利用 RenderScript
     * @return
     */
    private Bitmap gaussianBlur5(Bitmap bitmap){
        Bitmap outBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(),
                Bitmap.Config.ARGB_8888);

        // Instantiate a new Renderscript
        RenderScript rs = RenderScript.create(getApplicationContext());//RenderScript是Android在API 11之后加入的

        // Create an Intrinsic Blur Script using the Renderscript
        ScriptIntrinsicBlur blurScript = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));

        // Create the Allocations (in/out) with the Renderscript and the in/out
        // bitmaps
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

    //叠加两个颜色值数组
    private void mixColor(int[] baseColor,int[] mixColor){
        for (int i = 0, length = baseColor.length; i < length; ++i) {
            int bColor = baseColor[i];
            int br = (bColor & 0x00ff0000) >> 16;
            int bg = (bColor & 0x0000ff00) >> 8;
            int bb = (bColor & 0x000000ff);

            int mColor = mixColor[i];
            int mr = (mColor & 0x00ff0000) >> 16;
            int mg = (mColor & 0x0000ff00) >> 8;
            int mb = (mColor & 0x000000ff);

            int nr = mixChannel(br, mr);
            int ng = mixChannel(bg, mg);
            int nb = mixChannel(bb, mb);

            baseColor[i] = nr << 16 | ng << 8 | nb | 0xff000000;
        }
    }

    //叠加两个像素通道 第一个参数为基础 r、g或b任一通道数值  第二个参数为等待混合的 r、g或b任一通道数值 返回混合后的通道数值
    private int mixChannel(int baseChannel,int mixChannel){
        int mixedChannel = baseChannel + (baseChannel * mixChannel) / (255 - mixChannel);
        mixedChannel = mixedChannel > 255 ? 255 : mixedChannel;
        return mixedChannel;
    }

    //构造渐变图
    private Bitmap createBitmap(){
        int[] pixels = new int[256*256];

        for(int i=0;i<256;i++){
            for(int j=0;j<256;j++){
                pixels[i*256+j] = j | 0xff000000;
            }
        }
        Bitmap bitmap = Bitmap.createBitmap(pixels, 256, 256,
                Bitmap.Config.ARGB_8888);
        return bitmap;
    }

    //将bitmap保存为图片
    private void saveBitmap(Bitmap bitmap,String bitName) throws IOException
    {
        File file = new File("/sdcard/DCIM/Camera/"+bitName);

        if(file.exists()){
            //file.delete();
            LogUtils.v("exist");
        }
        FileOutputStream out;
        try{
            out = new FileOutputStream(file);
            if(bitmap.compress(Bitmap.CompressFormat.PNG, 90, out))
            {
                out.flush();
                out.close();
            }
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    //开启新的线程加载原图
    /*private void loadSrcImage(final String srcImagePath){
        new Thread(new Runnable() {
            @Override
            public void run() {
                //加载原图
                bitmapOrigin = BitmapFactory.decodeFile(srcImagePath);
                Message msg = new Message();
                msg.what = LOAD_SOURCE_IMAGE_FINISHED;
                //handler.sendMessage(msg);
            }
        }).start();
    }*/


    /**
     * 效果最佳
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
