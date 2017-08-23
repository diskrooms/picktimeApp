package tech.startech.picktime;

import android.app.Application;
import android.graphics.Bitmap;
import android.util.LruCache;

import com.apkfuns.logutils.LogUtils;

/**
 * Created by jsb-hdp-0 on 2017/1/16.
 */

public class MyApp  extends Application{
    private LruCache<String,Bitmap> memoryCache;

    public void onCreate(){
        super.onCreate();
        int maxCacheSize = (int)Runtime.getRuntime().maxMemory()/1024/5;

        memoryCache = new LruCache<String, Bitmap>(maxCacheSize){
            //返回每张图片的大小
            protected int sizeOf(Bitmap bitmap){
                return bitmap.getByteCount()/1024;
            }
        };
    }

    //往内存缓存对象中添加图片
    public void putImageToMemoryCache(String key,Bitmap bitmap){
        if(getImageFromMemoryCache(key) == null){
            memoryCache.put(key,bitmap);
        }
    }

    //从内存缓存对象中获取图片
    public Bitmap getImageFromMemoryCache(String key){
        return memoryCache.get(key);
    }
}
