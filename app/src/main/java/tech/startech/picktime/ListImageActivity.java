package tech.startech.picktime;

import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.apkfuns.logutils.LogUtils;
import com.felipecsl.asymmetricgridview.library.Utils;
import com.felipecsl.asymmetricgridview.library.model.AsymmetricItem;
import com.felipecsl.asymmetricgridview.library.widget.AsymmetricGridView;
import com.felipecsl.asymmetricgridview.library.widget.AsymmetricGridViewAdapter;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

public class ListImageActivity extends BaseActivity {

    //UI控件及控件相关
    private RecyclerView recyclerView;                                  //recycler布局
    private MyAdapter myAdapter;

    //图片相关变量
    CustomGridViewAdapter customGridViewAdapter;                        //gridview 数据适配器
    private static final int countPerRow = 3;                           //每一行排列的图片数量
    private static final int pixPadding = 10;                           //每张图片的间距
    private static final int GET_EXTERNAL_IMAGES_FINISHED = 1;          //消息类型 读取完所有SD卡上的图片
    private List<Bitmap> imagesBitmap = new ArrayList<Bitmap>();             //存放查询出的图片资源 list方式存储
    private List<String> imagesPath = new ArrayList<String>();               //存放查询出的图片路径

    private Handler handler = new Handler() {
        public void handleMessage(Message msg){
            if(msg.what == GET_EXTERNAL_IMAGES_FINISHED){
                //获得消息通知后更新  customGridViewAdapter
                /*for(int i = 0;i < imagesPath.size(); i++){

                }*/
                customGridViewAdapter.notifyDataSetChanged();
                //myAdapter.notifyDataSetChanged();
            }
        }
    };

    //数据源
    public static final String URL_PATH="http://dxy.com/app/i/feed/index/list?hardName=Google%20Nexus%205%20-%205.1.0%20-%20API%2022%20-%201080x1920&u=&bv=2015&ac=d5424fa6-adff-4b0a-8917-4264daf4a348&vc=5.1.9&vs=5.1&mc=00000000600ba4e6ffffffff99d603a9";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.list_image);
        initUI();
        initActivity();
    }

    /**
     *  初始化UI变量
     */
    private void initUI(){
        recyclerView = (RecyclerView) findViewById(R.id.recycler);
        //初始化并设置布局管理器
        GridLayoutManager gridLayoutManager = new GridLayoutManager(this,3);
        gridLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup(){
            public int getSpanSize(int position) {
                int spansize=1;
                switch (customGridViewAdapter.getItemViewType(position)) {
                    case 1:
                        spansize=1;
                        break;
                    case 2:
                        spansize=2;
                        break;
                    case 3:
                        spansize=3;
                        break;
                }
                return spansize;
            }
        });
        recyclerView.setLayoutManager(gridLayoutManager);
    }

    /**
     *  初始化数据变量
     */
    private void initActivity(){
        //myAdapter = new MyAdapter(this, getData());
        //recyclerView.setAdapter(myAdapter);
        getImages();
        customGridViewAdapter = new CustomGridViewAdapter(this,imagesBitmap);
        recyclerView.setAdapter(customGridViewAdapter);
        customGridViewAdapter.setOnItemClickListener(new CustomGridViewAdapter.onRecyclerViewItemClickListener(){
            @Override
            public void onItemClick(View view, int position) {
                //Toast.makeText(ListImageActivity.this,position+"", Toast.LENGTH_SHORT).show();
                Intent loadOneImageIntent = new Intent(ListImageActivity.this,BrowseImageActivity.class);
                String path = imagesPath.get(position);
                loadOneImageIntent.putExtra("path",path);
                startActivity(loadOneImageIntent);
            }
        });
        //设置recyclerItem 间距
        recyclerView.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
                super.getItemOffsets(outRect, view, parent, state);
                outRect.set(0, 0, 0, 0);//设置itemView中内容相对边框左，上，右，下距离
            }
        });
    }

    /**
     * 开启子线程获取SD上的图片地址
     */
    private void getImages(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                Uri imageUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                //LogUtils.v(imageUri);
                ContentResolver contentResolver = getApplicationContext().getContentResolver();
                //根据图片类型查询
                String [] columns = {MediaStore.Images.Media._ID, MediaStore.Images.Media.DATA, MediaStore.Images.Media.ORIENTATION};   //要查询的列
                String where = null;        //没有查询条件 即全部查询出来
                String order = MediaStore.Images.Media.DATE_MODIFIED + " desc limit 100 offset 0 ";       //查询结果排序 按修改日期 逆序
                Cursor cursor = contentResolver.query(imageUri,columns,where,null,order);
                if(cursor == null){
                    LogUtils.v("cursor初始化失败");
                    return;
                }
                assert(cursor!=null);
                //遍历cursor
                while(cursor.moveToNext()){
                    //获取图片的路径
                    String path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
                    imagesPath.add(path);
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inJustDecodeBounds = true;
                    BitmapFactory.decodeFile(path,options);

                    int srcWidth = options.outWidth;
                    int srcHeight = options.outHeight;
                    //横向排列三张图片
                    BaseActivity.screenProperty sp = getScreen();
                    int toWidth = (sp.width - (countPerRow + 1) * pixPadding) / countPerRow;      //希望压缩的宽度值
                    int toHeiht = toWidth;
                    //int toHeight = options.outHeight * toWidth / options.outWidth;
                    options.inJustDecodeBounds = false;
                    options.inPreferredConfig = Bitmap.Config.ARGB_8888;    //ARGB_4444的图像质量太差
                    options.inSampleSize = srcWidth / toWidth;      //压缩比例
                    Bitmap bitmap = BitmapFactory.decodeFile(path,options);
                    bitmap = ThumbnailUtils.extractThumbnail(bitmap, toWidth, toHeiht); //裁剪缩略图
                    imagesBitmap.add(bitmap);
                    //对原图稍加压缩 作为大图展示(需要展示的尺寸即ImageView的尺寸)
                    int viewWidth = sp.width - 20;      //压缩宽度即可 高度自适应
                    options.inSampleSize = srcWidth / viewWidth;      //压缩比例
                    /*Bitmap srcBitmap = BitmapFactory.decodeFile(path,options);
                    if(path != null && srcBitmap != null) {
                        myApplication.putImageToMemoryCache(path, srcBitmap);
                    }*/

                    //LogUtils.v(getImageFromMemoryCache(path));
                    //imagesBitmapMap.put(path,bitmap);
                    //定义待发送的消息体
                    Message msg = new Message();
                    //LogUtils.v(imagesPath);
                    msg.what = GET_EXTERNAL_IMAGES_FINISHED;
                    handler.sendMessage(msg);       //解析完一张图片就通知主线程更新 customGridViewAdapter
                }

            }
        }).start();
    }

    /**
     * 获取网络图片
     * @return
     */
    public List<ModelData.Model.ItemsBean> getData() {
        //Log.e(TAG, "getData: 3" );
        /*HttpUtil.getStringAsync(URL_PATH, new HttpUtil.RequestCallBack() {
            @Override
            public void onFailure() {
                //Log.e(TAG, "onFailure: " );
            }

            @Override
            public void onSuccess(String result) {
                Gson gson = new Gson();
                ModelData modelData = gson.fromJson(result, ModelData.class);
                List<Model> data =modelData.getData().getItems();
                //Log.e(TAG, "onSuccess: "+data );
                adapter.addRes(data);
            }

            @Override
            public void onFinish() {
                //Log.e(TAG, "onFinish: " );
            }
        });*/

        Gson gson = new Gson();
        String result = "{\n" +
                "    \"data\": {\n" +
                "        \"items\": [\n" +
                "            {\n" +
                "                \"title\": \"一人得胃病，传染给全家？可能导致胃癌的细菌，如何除掉它？\",\n" +
                "                \"url\": \"http://dxy.com/column/10208\",\n" +
                "                \"content\": \"网上「一人得胃病，传染给全家」的说法，有些唬人，但是有一定道理。\",\n" +
                "                \"cover\": \"http://dxy.com/attachment/show/1178340\",\n" +
                "                \"show_type\": 1,\n" +
                "                \"from\": \"appIndexAdmin\",\n" +
                "                \"author\": {\n" +
                "                    \"id\": 566,\n" +
                "                    \"name\": \"张岩\",\n" +
                "                    \"url\": \"zhangyan566\",\n" +
                "                    \"avatar\": \"http://img.dxycdn.com/dotcom/2017/01/12/28/w6fnqwk4.jpg\",\n" +
                "                    \"remarks\": \"山东大学齐鲁医院 消化内科\"\n" +
                "                }\n" +
                "            },\n" +
                "            {\n" +
                "                \"title\": \"香港人平均寿命世界第一、赶超日本，主要是因为……\",\n" +
                "                \"url\": \"http://dxy.com/column/9805\",\n" +
                "                \"content\": \"时值香港回归 20 周年，丁香医生也来聊聊香港。一般说到「长寿」，大家很容易先想到的都是日本，认为日本是世界上人均寿命最长的。但是根据 2015 年的统计数据，全球人均预期寿命最长的国家（或地区）其实是中国香港。2015 年香港人均预期寿命 83.74 岁，其中男性 80.91 岁，女性 86.58 岁，无论男女都名列世界第一。这里解释一下「人均预期寿命」，它和人的实际寿命不同，是以当前死亡率为基\",\n" +
                "                \"cover\": \"http://img.dxycdn.com/dotcom/2017/07/03/43/2yqpm0zd.jpg\",\n" +
                "                \"show_type\": 2,\n" +
                "                \"from\": \"pop\",\n" +
                "                \"author\": {\n" +
                "                    \"id\": 94,\n" +
                "                    \"name\": \"庄时利和\",\n" +
                "                    \"url\": \"zhuangshilihe1\",\n" +
                "                    \"avatar\": \"http://img.dxycdn.com/dotcom/2014/11/06/37/8f7evehm.jpg\",\n" +
                "                    \"remarks\": \"神经科学硕士\"\n" +
                "                }\n" +
                "            },\n" +
                "            {\n" +
                "                \"title\": \"怎么吃能健康长寿？来看看这群高龄老人的饮食经验\",\n" +
                "                \"url\": \"http://dxy.com/column/9820\",\n" +
                "                \"content\": \"现在生活条件和医疗条件，不知道比三四十年前好了多少倍。却有很多老人感觉活得很累，甚至很痛苦。为什么？到底出现了什么问题？丁当特别咨询了中国人民解放军陆军总医院营养师于仁文。他对这事儿很有发言权。营养师于仁文主要负责中国人民解放军陆军总医院各类老年病、营养代谢性慢性疾病、手术及放化疗后，病人的膳食营养工作。除此之外，他还担任了北京营养师协会理事、中国烹饪协会专家委委员、中国老年学学会食品营养专业委员\",\n" +
                "                \"cover\": \"http://img.dxycdn.com/dotcom/2017/07/03/26/zvdfklmw.jpg\",\n" +
                "                \"tags_str\": \"隐性营养不良,言语障碍\",\n" +
                "                \"show_type\": 8,\n" +
                "                \"from\": \"pop\",\n" +
                "                \"author\": {\n" +
                "                    \"id\": 717,\n" +
                "                    \"name\": \"于仁文\",\n" +
                "                    \"url\": \"yurenwen\",\n" +
                "                    \"avatar\": \"http://img.dxycdn.com/dotcom/2016/05/26/02/tnnfzeob.jpg\",\n" +
                "                    \"remarks\": \"中国人民解放军陆军总医院 营养师\"\n" +
                "                }\n" +
                "            },\n" +
                "            {\n" +
                "                \"title\": \"冰淇淋、雪糕、冰棍，哪种最健康？\",\n" +
                "                \"url\": \"http://dxy.com/question/39008\",\n" +
                "                \"content\": \"三者最大的不同在于乳脂和蛋白质的含量。\",\n" +
                "                \"tags_str\": \"健康问答\",\n" +
                "                \"show_type\": 2,\n" +
                "                \"from\": \"appIndexAdmin\"\n" +
                "            },\n" +
                "            {\n" +
                "                \"title\": \"私处的颜色深，能说明性生活频繁吗？\",\n" +
                "                \"url\": \"http://dxy.com/question/39009\",\n" +
                "                \"content\": \"无论摩擦还是性生活，都不会让私处的颜色变深。\",\n" +
                "                \"tags_str\": \"健康问答\",\n" +
                "                \"show_type\": 2,\n" +
                "                \"from\": \"appIndexAdmin\"\n" +
                "            },\n" +
                "            {\n" +
                "                \"title\": \"给三高人群提个醒：千万少吃这种食物！\",\n" +
                "                \"url\": \"http://dxy.com/column/10209\",\n" +
                "                \"content\": \"三高人群要少吃什么？要少吃「三高食物」！ \",\n" +
                "                \"cover\": \"http://dxy.com/attachment/show/1178348\",\n" +
                "                \"show_type\": 2,\n" +
                "                \"from\": \"appIndexAdmin\",\n" +
                "                \"author\": {\n" +
                "                    \"id\": 1119,\n" +
                "                    \"name\": \"王兴国\",\n" +
                "                    \"url\": \"wangxingguo\",\n" +
                "                    \"avatar\": \"http://dxy.com/attachment/show/1174609\",\n" +
                "                    \"remarks\": \"大连市中心医院营养科主任\"\n" +
                "                }\n" +
                "            }\n" +
                "        ]\n" +
                "    }\n" +
                "}";
        ModelData modelData = gson.fromJson(result, ModelData.class);
        List<ModelData.Model.ItemsBean> data = modelData.getData().getItems();
        //Log.e(TAG, "onSuccess: "+data );
        if(myAdapter != null) {
            //myAdapter.addRes(data);
        }
        return data;
    }

    /**
     * 简单数据源
     * @return
     */
/*    public List<Model> getData() {
        List<Model> data = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            Model model = new Model();
            model.setName("abc---" + i);
            model.setHeight(((int) (Math.random() * 100 + 200)));
            data.add(model);
        }
        return data;
    }*/
}
