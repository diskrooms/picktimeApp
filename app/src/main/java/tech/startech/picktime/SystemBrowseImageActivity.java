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

public class SystemBrowseImageActivity extends AppCompatActivity implements View.OnClickListener{
    static {
        System.loadLibrary("OpenCV");                         //导入动态链接库
        System.loadLibrary("opencv_java3");
    }
    private Bitmap originBitmap;
    private ImageView showImage;
    private CircularImageView sketch;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_system_browse_image);
        Intent intent = getIntent();
        String path = intent.getStringExtra("path");
        //加载图片
        originBitmap = BitmapFactory.decodeFile(path);
        showImage = (ImageView)findViewById(R.id.browseImage);
        sketch = (CircularImageView)findViewById(R.id.sketchImage);
        showImage.setImageBitmap(originBitmap);
        sketch.setOnClickListener(this);
    }

    public void onClick(View v){
        if(v.getId() == R.id.sketchImage){
            NDKUtils ndk = new NDKUtils();
            int w = originBitmap.getWidth();
            int h = originBitmap.getHeight();
            byte[] sketch = ndk.reverse2(originBitmap,w,h);
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
        }
    }
}
