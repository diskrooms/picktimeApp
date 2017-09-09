package tech.startech.picktime;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import com.apkfuns.logutils.LogUtils;
import com.pkmmte.view.CircularImageView;

import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.lang.reflect.Array;
import java.util.ArrayList;

public class SystemBrowseImageActivity extends AppCompatActivity implements View.OnClickListener{
    static {
        System.loadLibrary("OpenCV");                         //导入动态链接库
        System.loadLibrary("opencv_java3");
    }
    private Bitmap originBitmap;
    private ImageView showImage;
    //分享按钮
    private CircularImageView share;
    //素描画
    private CircularImageView sketch;
    //九宫格
    private CircularImageView rasterize;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_system_browse_image);
        Intent intent = getIntent();
        String path = intent.getStringExtra("path");
        //加载图片
        //LogUtils.v(CommonUtils.getPicDegree(path));
        originBitmap = BitmapFactory.decodeFile(path);
        showImage = (ImageView)findViewById(R.id.browseImage);
        share = (CircularImageView)findViewById(R.id.share);
        sketch = (CircularImageView)findViewById(R.id.sketchImage);
        rasterize = (CircularImageView)findViewById(R.id.rasterizeImage);
        //检测图片旋转角度 如果被旋转了 就再旋转回来
        int degree = CommonUtils.getPicDegree(path);
        Bitmap newBitmap = CommonUtils.rotateBitmapDegree(originBitmap,degree);
        originBitmap = newBitmap;                   //必须 原图已经recycle 所以要重新赋值
        showImage.setImageBitmap(newBitmap);
        sketch.setOnClickListener(this);
        rasterize.setOnClickListener(this);
    }

    public void onClick(View v){
        NDKUtils ndk = new NDKUtils();
        int w = originBitmap.getWidth();
        int h = originBitmap.getHeight();
        if(v.getId() == R.id.sketchImage){
            //素描
            byte[] sketch = ndk.sketch(originBitmap,w,h);
            //byte[] sketch = ndk.sketch2(originBitmap);
            //byte[] sketch = ndk.sketch3(originBitmap);
            //LogUtils.v(sketch);
            int[] sketch_ = new int[w*h];
            for(int i = 0;i < sketch_.length;i++){
                int temp = (int)sketch[i] & 0x000000ff; //byte转换为int  java会自动在高位加1
                sketch_[i] = temp << 16 | temp << 8 | temp |  0xff000000;   //java中的内存分布 argb
            }
            //LogUtils.v("%x",sketch_[0]);
            Bitmap sketchBitmap = Bitmap.createBitmap(sketch_,w,h, Bitmap.Config.ARGB_8888);
            //LogUtils.v(i);
            showImage.setImageBitmap(sketchBitmap);
        } else if(v.getId() == R.id.rasterizeImage){
            //九宫格
            /*int[] goodMorning = ndk.goodMorning_test(originBitmap);
            int length = goodMorning.length;
            int[] goodMorning_ = new int[length];
            int[] originIndex = new int[w*h];
            originBitmap.getPixels(originIndex,0,w,0,0,w,h);
            for(int i=0;i<length;i++){
                //反序
                int r = goodMorning[i] & 0x000000ff;
                int g = (goodMorning[i] & 0x0000ff00) >> 8;
                int b = (goodMorning[i] & 0x00ff0000) >> 16;
                goodMorning_[i] = r << 16 | g << 8 | b |  0xff000000;
                //LogUtils.v("%x",originIndex[i]);
                //LogUtils.v("%x",goodMorning_[i]);
            }

            Bitmap goodMorningBitmap = Bitmap.createBitmap(goodMorning_,1000,1000, Bitmap.Config.ARGB_8888);
            showImage.setImageBitmap(goodMorningBitmap);*/

            //jni返回多个bitmap 不好实现 不得已用 java封装层的api在java层先定义好Mat 然后传入jni
            final int block = 9;                //9宫格

            Mat[] mats = new Mat[block];
            //初始化对象数组
            for(int i = 0;i < block; i++){
                mats[i] = new Mat();
            }
            //存储n_mat地址
            long[] addrs = new long[block];
            for(int i = 0; i < block;i++){
                addrs[i] = mats[i].getNativeObjAddr();
            }
            Mat originMat = new Mat();
            Utils.bitmapToMat(originBitmap,originMat);
            long origin_addr = originMat.getNativeObjAddr();
            ndk.goodMorning(originBitmap,addrs,origin_addr);

            Bitmap originBitmap_ = Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(originMat,originBitmap_); //用一个新的bitmap变量接收 不能用原来的bitmap变量
            showImage.setImageBitmap(originBitmap_);
            share.setVisibility(View.VISIBLE);
            //showImage.setImageBitmap(goodMorning1Bitmap)
            //Bitmap goodMorning1Bitmap = Bitmap.createBitmap(mats[0].cols(),mats[0].rows(), Bitmap.Config.ARGB_8888);;
            //Utils.matToBitmap(mats[0],goodMorning1Bitmap);
            //showImage.setImageBitmap(goodMorning1Bitmap);
        }
    }
}
