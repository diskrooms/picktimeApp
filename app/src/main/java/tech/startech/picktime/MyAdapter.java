package tech.startech.picktime;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by jsb-hdp-0 on 2017/8/11.
 */

public class MyAdapter extends RecyclerView.Adapter<MyAdapter.ViewHolder> implements View.OnClickListener {
    private List<ModelData.Model.ItemsBean> data;
    private LayoutInflater inflater;
    private RecyclerView mRecyclerView;
    private OnItemClickLietener clickLietener;

    public void setClickLietener(OnItemClickLietener clickLietener){
        this.clickLietener=clickLietener;
    }

    public MyAdapter(Context context, List<ModelData.Model.ItemsBean> data) {
        this.data = data;
        inflater = LayoutInflater.from(context);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = null;
        switch (viewType) {
            case 1:
                itemView = inflater.inflate(R.layout.list_image_item1, parent, false);
                break;
            case 2:
                itemView = inflater.inflate(R.layout.list_image_item2, parent, false);
                break;
            case 8:
                itemView = inflater.inflate(R.layout.list_image_item3, parent, false);
                break;
            //设置监听
        }
        itemView.setOnClickListener(this);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        switch (getItemViewType(position)) {
            case 1:
                ImageView imageView = (ImageView) holder.getView(R.id.iv_item1);
                TextView textView = (TextView) holder.getView(R.id.iv_item1);
                textView.setText(data.get(position).getTitle());
                String picPath = data.get(position).getCover();
                //textView.setText("测试1标题");
                //String picPath = "测试1内容";

                //ImageLoader.display(imageView, picPath);
                break;
            case 2:
                //TextView item2Title = (TextView) holder.getView(R.id.tv_item2_title);
                //TextView item2Content = (TextView) holder.getView(R.id.tv_item2_content);
                //item2Title.setText(data.get(position).getTitle());
                //item2Content.setText(data.get(position).getContent());
                //item2Title.setText("测试2标题");
                //item2Content.setText("测试2内容");
                break;
            case 8:
                //TextView item3Title = (TextView) holder.getView(R.id.tv_item3_title);
                //TextView item3Content = (TextView) holder.getView(R.id.tv_item3_content);
                //item3Title.setText(data.get(position).getTitle());
                //item3Content.setText(data.get(position).getContent());
                //item3Title.setText("测试3标题");
                //item3Content.setText("测试3内容");
                break;
        }
    }

    @Override
    public int getItemCount() {
        return data != null ? data.size() : 0;
    }

    @Override
    public int getItemViewType(int position) {
        return data.get(position).getShow_type();
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        mRecyclerView=recyclerView;
    }

    public void addRes(List<ModelData.Model.ItemsBean> data) {
        if (data != null) {
            this.data = data;
            notifyDataSetChanged();
        }
    }

    @Override
    public void onClick(View v) {
        int childAdapterPosition = mRecyclerView.getChildAdapterPosition(v);
        if (clickLietener!=null) {
            clickLietener.setItemClickListener(childAdapterPosition);
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        private Map<Integer, View> mCacheView;

        public ViewHolder(View itemView) {
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

    public interface OnItemClickLietener{
        void setItemClickListener(int position);
    }

}
