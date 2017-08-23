package tech.startech.picktime;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;



public class GrayActivity extends AppCompatActivity implements View.OnClickListener{

    private ImageView img;
    private Bitmap resBitmap;
    static {
        System.loadLibrary("OpenCV");
        System.loadLibrary("opencv_java3");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gray);
        resBitmap = ((BitmapDrawable) getResources().getDrawable(R.drawable.tree)).getBitmap();

        img = (ImageView)findViewById(R.id.gray);
        img.setImageBitmap(resBitmap);
        img.setOnClickListener(this);
    }

    public void onClick(View v){
        NDKUtils ndk = new NDKUtils();

        int w = resBitmap.getWidth();
        int h = resBitmap.getHeight();
        int[] resPixels = new int[w * h];
        resBitmap.getPixels(resPixels, 0, w, 0, 0, w, h);               //从bitmap中获取原始pixels
        int[] grayPixels = ndk.gray(resPixels,w,h);                     //通过NDK获取灰度值pixels
        int[] reversePixels = ndk.reverse(grayPixels,w,h);              //通过NDK获取反相pixels
        int[] blurPixels = ndk.blur(reversePixels,w,h);                 //通过NDK获取高斯模糊 pixels
        //Log.v( "gray", Integer.toHexString(grayPixels[0]));
        //Log.v( "blur", Integer.toHexString(blurPixels[0]));
        int[] resultPixels = ndk.desColor(grayPixels,blurPixels,w,h);   //通过NDK颜色合并
        //Log.v( "desColor", Integer.toHexString(resultPixels[0]));
        Bitmap resultBitmap = Bitmap.createBitmap(resultPixels,w,h, Bitmap.Config.RGB_565);
        //resultBitmap.setPixels(resultPixels, 0, w, 0, 0,w, h);
        img.setImageBitmap(resultBitmap);
    }
}
