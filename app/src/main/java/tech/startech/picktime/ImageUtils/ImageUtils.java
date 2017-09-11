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
 */
public class ImageUtils {
    //将bitmap保存为图片
    public static void saveBitmap(Bitmap bitmap, String bitName) throws IOException {
        File file = new File("/sdcard/DCIM/Camera/" + bitName);
        //LogUtils.v("/sdcard/DCIM/Camera/" + bitName);
        if (file.exists()) {
            //file.delete();
            Log.v("error","the file exists");
        }
        FileOutputStream out;
        try {
            out = new FileOutputStream(file);

            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush();
            out.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}