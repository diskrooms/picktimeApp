package tech.startech.picktime;

import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.LruCache;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

public class BaseActivity extends AppCompatActivity {

    private AlertDialog commonDialog;               //维护一个全局的风格统一的dialog
    //protected static LruCache<String,Bitmap> memoryCache;   //手机缓存对象 基类中用 protected修饰 被继承后才能被访问到

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }


    /**
     * 应用统一风格对话框
     */
    public void showDialog(String string){
        //维护一个公共对话框
        commonDialog = new AlertDialog.Builder(BaseActivity.this).setCancelable(false).create();
        Window commonDialogWindow = commonDialog.getWindow();
        commonDialog.show();
        commonDialogWindow.setContentView(R.layout.common_dialog);
        TextView commonDialogContent = (TextView)commonDialogWindow.findViewById(R.id.commonDialogContent);
        commonDialogContent.setText(string);
        TextView sure = (TextView) commonDialogWindow.findViewById(R.id._dialog_sure);
        sure.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                commonDialog.dismiss();
            }
        });
    }

    /**
     * activity退出
     */
    public void stop(){

    }

    /**
     * float 类型数据处理
     * @param string 待处理的字符串
     */
    public float floatVal(String string){
        if(string.replace(".","").length() == 0) {
            //全部为小数点直接返回0
            return 0;
        } else {
            if(string.indexOf('.') != string.lastIndexOf('.')){
                //有两个及两个以前小数点 舍弃第二个小数点及后面的字符
                String temp = string.substring(0,string.indexOf('.',string.indexOf('.')+1));
                if(temp.equals(".")){
                    return 0;
                } else {
                    return Float.parseFloat(temp);
                }
            } else {
                return Float.parseFloat(string);
            }
        }
    }

    public screenProperty getScreen(){
        screenProperty SP = new screenProperty();
        DisplayMetrics metric = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metric);
        SP.width = metric.widthPixels;      // 屏幕宽度（像素）
        SP.height = metric.heightPixels;    // 屏幕高度（像素）
        SP.density = metric.density;        // 屏幕像素密度比（像素）
        SP.densityDpi = metric.densityDpi;  // 屏幕像素密度（像素）
        return SP;
    }

    //屏幕属性内部类
    public class screenProperty{
        public int width;      //屏幕宽度
        public int height;     //屏幕高度
        public float density;  //屏幕密度
        public int densityDpi; //屏幕密度DPI

        public screenProperty(){

        }
    }
}
