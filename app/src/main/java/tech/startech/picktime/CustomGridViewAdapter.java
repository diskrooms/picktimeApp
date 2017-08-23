package tech.startech.picktime;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.support.v7.widget.RecyclerView;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.apkfuns.logutils.LogUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static android.R.id.list;
import static android.widget.Toast.LENGTH_LONG;

/**
 * Created by yongbaolinux on 2017/1/6.
 *  自定义gridview adapter
 */

public class CustomGridViewAdapter extends RecyclerView.Adapter<CustomGridViewAdapter.CustomViewHolder> implements View.OnClickListener{

    private Context mContext;
    private List<Bitmap> images;
    private LayoutInflater layoutInflater;
    private RecyclerView mRecyclerView;
    private onRecyclerViewItemClickListener clickLietener = null;

    public CustomGridViewAdapter(Context context, List _images){
        mContext = context;
        this.images = _images;
        layoutInflater = LayoutInflater.from(context);      //布局渲染器
    }

    /**
     * 初始化适配器viewholder最小单元
     * @param parent
     * @param viewType
     * @return
     */
    @Override
    public CustomViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = null;
        switch (viewType) {
            case 1:
                itemView = layoutInflater.inflate(R.layout.list_image_item1, parent, false);
                break;
            case 2:
                itemView = layoutInflater.inflate(R.layout.list_image_item2, parent, false);
                break;
            case 3:
                itemView = layoutInflater.inflate(R.layout.list_image_item3, parent, false);
                break;
            //设置监听
        }
        itemView.setOnClickListener(this);
        return new CustomViewHolder(itemView);
    }


    /**
     * 绑定viewHolder最小单元行为
     * @param holder
     * @param position
     */
    public void onBindViewHolder(CustomViewHolder holder, int position) {
        ImageView imageView = (ImageView) holder.getView(R.id.iv_item1);
        imageView.setImageBitmap(images.get(position));
        holder.itemView.setTag(position);
    }

    public int getItemViewType(int position){
        switch (position){
            case 0:
                return 3;
            case 2:
                return 2;
            default:
                return 1;
        }
    }

    @Override
    public int getItemCount() {
        return images != null ? images.size() : 0;
    }



    @Override
    public long getItemId(int position) {
        return position;
    }


    public static class CustomViewHolder extends RecyclerView.ViewHolder {

        private Map<Integer, View> mCacheView;

        public CustomViewHolder(View itemView) {
            super(itemView);
            mCacheView = new HashMap<>();
        }

        public View getView(int resId) {
            View view;
            if (mCacheView.containsKey(resId)) {
                view = mCacheView.get(resId);
            } else {
                view = itemView.findViewById(resId);
                mCacheView.put(resId, view);
            }
            return view;
        }
    }

    public interface onRecyclerViewItemClickListener{
        void onItemClick(View view,int position);
    }

    @Override
    public void onClick(View v) {
        //int childAdapterPosition = mRecyclerView.getChildAdapterPosition(v);
        if (clickLietener!=null) {
            clickLietener.onItemClick(v,(int)v.getTag());
        }
    }

    public void setOnItemClickListener(onRecyclerViewItemClickListener listener) {
        this.clickLietener = listener;
    }

}
