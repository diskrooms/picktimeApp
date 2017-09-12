package tech.startech.picktime.ImageUtils;
import android.graphics.Bitmap;
import android.util.Log;

import com.apkfuns.logutils.LogUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by root on 17-9-12.
 * 图片相关工具类
 */
public class ImageUtils {

    /**
     *  将bitmap保存为图片
     *  @param bitmap 图片资源
     *  @param path  要保存的位置(路径)
     *  @param scale 压缩比例 0-100
     */
    public static boolean saveBitmap(Bitmap bitmap, String path,int scale) throws IOException {
        File file = new File(path);
        if (file.exists()) {
            //file.delete();
            LogUtils.v(path+"===>error,the file exists");
        }
        FileOutputStream out;
        try {
            out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, scale, out);
            out.flush();
            out.close();
            return true;
        } catch (FileNotFoundException e) {
            //文件夹不存在会报这个异常
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }
}