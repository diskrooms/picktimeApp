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
            ndk.reverse2(originBitmap,w,h);
            //LogUtils.v(i);
            showImage.setImageBitmap(originBitmap);
        }
    }
}
