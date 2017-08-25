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

    static {
        //System.loadLibrary("OpenCV");                         //导入动态链接库
        //System.loadLibrary("opencv_java3");
    }
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
                //LogUtils.v(bitmap.getConfig());
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
                 * 素描算法(java代码实现的模糊效果在锤子M1上耗时20s左右,用jni调用opencv内置的模糊函数耗时2-4s)
                 */
                sketchImage.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if(currentStatus != 1) {
                            //废弃从控件中获取bitmap的方式 直接用一个变量维护bitmap的值
                            //Bitmap bitmapOrigin = ((BitmapDrawable) browseImage.getDrawable()).getBitmap();
                            //Bitmap bitmapOrigin = ((BitmapDrawable) getResources().getDrawable(R.drawable.sun)).getBitmap();
                            int h = bitmapOrigin.getHeight();
                            int w = bitmapOrigin.getWidth();
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
                            //int[] desaturatePixels = desaturate2(bitmapOrigin);
                            //Bitmap desaturateBitmap = Bitmap.createBitmap(desaturatePixels, width, height, Bitmap.Config.RGB_565);
                            //browseImage.setImageBitmap(desaturateBitmap);

                            //反相
                            //int[] pixels = getPixelsFromBitmap(desaturateBitmap);            //从源图像获取像素值
                            //int[] reverseColorPixels = reverseColor(desaturatePixels,0);      //简单反相
                            //堆栈模糊
                            //int[] stackBlurPixels = stackBlur(reverseColorPixels,width,height,10);
                            //Bitmap bitmap = Bitmap.createBitmap(stackBlurPixels, width, height, Bitmap.Config.ARGB_8888);

                            /*NDKUtils ndk = new NDKUtils();
                            int[] resPixels = new int[w * h];
                            bitmapOrigin.getPixels(resPixels, 0, w, 0, 0, w, h);               //从bitmap中获取原始pixels
                            int[] grayPixels = ndk.gray(resPixels,w,h);                     //通过NDK获取灰度值pixels
                            int[] reversePixels = ndk.reverse(grayPixels,w,h);              //通过NDK获取反相pixels
                            //int[] blurPixels = ndk.blur(reversePixels,w,h);                 //通过NDK获取高斯模糊 pixels
                            //Bitmap reverseBitmap = Bitmap.createBitmap(reversePixels,w,h, Bitmap.Config.RGB_565);
                            Bitmap blurBitmap = RenderScriptGaussianBlur(bitmapOrigin);
                            Log.v( "gray", Integer.toHexString(grayPixels[0]));
                            Log.v( "blur", Integer.toHexString(blurPixels[0]));
                            int[] blurPixels = new int[w*h];
                            blurBitmap.getPixels(blurPixels, 0, w, 0, 0, w, h);
                            int[] resultPixels = ndk.desColor(grayPixels,blurPixels,w,h);   //通过NDK颜色合并
                            Log.v( "desColor", Integer.toHexString(resultPixels[0]));
                            Bitmap resultBitmap = Bitmap.createBitmap(resultPixels,w,h, Bitmap.Config.RGB_565);
                            browseImage.setImageBitmap(blurBitmap);*/

                            NativeBlurProcess nativeBlurProcess = new NativeBlurProcess();
                            browseImage.setImageBitmap(nativeBlurProcess.blur(bitmapOrigin, 20,w,h));
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

}
