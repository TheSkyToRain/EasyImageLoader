package com.ebaryice.easyimageloader;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.OnScrollListener;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.ebaryice.easyimageloader.util.EasyImageLoader;
import com.ebaryice.easyimageloader.util.ImageResizer;
import com.ebaryice.easyimageloader.util.MyUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity{

    List<String> urls = new ArrayList<>();
    EasyImageLoader imageLoader;
    RecyclerView recyclerView;
    ImageListAdapter adapter;

    private int imageWidth = 0;
    private boolean isRecyclerViewIdle = true;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initData();
        initView();
        imageLoader = EasyImageLoader.build(this);
    }

    private void initView() {
        recyclerView = findViewById(R.id.recyclerView);
        adapter = new ImageListAdapter(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
        recyclerView.addOnScrollListener(new OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == RecyclerView.SCROLL_STATE_IDLE){
                    isRecyclerViewIdle = true;
                    adapter.notifyDataSetChanged();
                }else {
                    isRecyclerViewIdle = false;
                }
            }
        });
    }

    private void initData() {
        String[] s = new String[]{
                "https://ss1.bdstatic.com/70cFuXSh_Q1YnxGkpoWK1HF6hhy/it/u=2043305675,3979419376&fm=200&gp=0.jpg",
                "https://ss2.bdstatic.com/70cFvnSh_Q1YnxGkpoWK1HF6hhy/it/u=1962610270,1087353442&fm=27&gp=0.jpg",
                "https://ss2.bdstatic.com/70cFvnSh_Q1YnxGkpoWK1HF6hhy/it/u=3390324521,811178331&fm=26&gp=0.jpg",
                "https://ss3.bdstatic.com/70cFv8Sh_Q1YnxGkpoWK1HF6hhy/it/u=3868748332,1514217555&fm=26&gp=0.jpg",
                "https://ss0.bdstatic.com/70cFvHSh_Q1YnxGkpoWK1HF6hhy/it/u=656102875,1271055545&fm=26&gp=0.jpg",
                "https://ss0.bdstatic.com/70cFvHSh_Q1YnxGkpoWK1HF6hhy/it/u=17969157,1176638871&fm=26&gp=0.jpg",
                "https://ss1.bdstatic.com/70cFvXSh_Q1YnxGkpoWK1HF6hhy/it/u=3723843324,1993306655&fm=26&gp=0.jpg",
                "https://ss1.bdstatic.com/70cFvXSh_Q1YnxGkpoWK1HF6hhy/it/u=1340484728,247406165&fm=26&gp=0.jpg",
                "https://ss1.bdstatic.com/70cFuXSh_Q1YnxGkpoWK1HF6hhy/it/u=4004740585,2826612326&fm=26&gp=0.jpg",
                "https://ss0.bdstatic.com/70cFuHSh_Q1YnxGkpoWK1HF6hhy/it/u=3240488371,3738270313&fm=26&gp=0.jpg",
                "https://ss0.bdstatic.com/70cFvHSh_Q1YnxGkpoWK1HF6hhy/it/u=4108263385,228060825&fm=26&gp=0.jpg",
                "https://ss1.bdstatic.com/70cFvXSh_Q1YnxGkpoWK1HF6hhy/it/u=3814934367,216770080&fm=26&gp=0.jpg",
                "https://ss0.bdstatic.com/70cFuHSh_Q1YnxGkpoWK1HF6hhy/it/u=2933921456,791016686&fm=26&gp=0.jpg",
                "https://ss0.bdstatic.com/70cFuHSh_Q1YnxGkpoWK1HF6hhy/it/u=1566784634,747884912&fm=26&gp=0.jpg",
                "https://ss0.bdstatic.com/70cFuHSh_Q1YnxGkpoWK1HF6hhy/it/u=3548070735,3278363957&fm=26&gp=0.jpg",
                "https://ss3.bdstatic.com/70cFv8Sh_Q1YnxGkpoWK1HF6hhy/it/u=3141379835,3537746252&fm=26&gp=0.jpg",
                "https://ss0.bdstatic.com/70cFuHSh_Q1YnxGkpoWK1HF6hhy/it/u=1951550113,2709741859&fm=26&gp=0.jpg",
                "https://ss2.bdstatic.com/70cFvnSh_Q1YnxGkpoWK1HF6hhy/it/u=3976843922,1266673936&fm=26&gp=0.jpg",
                "https://ss1.bdstatic.com/70cFvXSh_Q1YnxGkpoWK1HF6hhy/it/u=2296111984,1387692813&fm=26&gp=0.jpg",
                "https://ss0.bdstatic.com/70cFuHSh_Q1YnxGkpoWK1HF6hhy/it/u=3240488371,3738270313&fm=26&gp=0.jpg",
                "http://img5.imgtn.bdimg.com/it/u=1292165292,3346438149&fm=26&gp=0.jpg"
        };
        urls.addAll(Arrays.asList(s));

        int screenWidth = MyUtils.getScreenMetrics(this).widthPixels;
        int space = (int) MyUtils.dp2px(this,20f);
        imageWidth = (screenWidth - space) / 3;
    }

    private class ImageListAdapter extends RecyclerView.Adapter<ImageListAdapter.MyViewHolder> {
        private Context context;

        public ImageListAdapter(Context context){
            this.context = context;
        }
        @NonNull
        @Override
        public MyViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_recycle,viewGroup,false);
            return new MyViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull MyViewHolder myViewHolder, int i) {
            if (isRecyclerViewIdle){
                imageLoader.bindBitmap(urls.get(i),
                        myViewHolder.imageView,imageWidth,imageWidth);
            } else {
                myViewHolder.imageView.setImageBitmap(
                        ImageResizer.decodeSampledBitmapFromResource(getResources()
                        ,R.drawable.load,imageWidth,imageWidth));
            }
        }

        @Override
        public int getItemCount() {
            return urls.size();
        }

        class MyViewHolder extends RecyclerView.ViewHolder{
            ImageView imageView;
            public MyViewHolder(@NonNull View itemView) {
                super(itemView);
                imageView = itemView.findViewById(R.id.image_item);
            }
        }
    }
}
