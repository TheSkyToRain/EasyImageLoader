package com.ebaryice.easyimageloader;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.StatFs;
import android.util.Log;
import android.util.LruCache;
import android.widget.ImageView;

import com.jakewharton.disklrucache.DiskLruCache;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class EasyImageLoader {
    private static final String TAG = "EasyImageLoader";

    public static final int MESSAGE_POST_RESULT = 1;

    // 配置线程池的var
    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();
    private static final int CORE_POOL_SIZE = CPU_COUNT + 1;
    private static final int MAXIMUM_POOL_SIZE = CPU_COUNT * 2 + 1;
    private static final long KEEP_ALIVE = 10L;

    // 标识
    private static final int TAG_KEY_URL = R.id.easy_image_loader_id;
    private Context context;
    private ImageResizer imageResizer;
    private LruCache<String,Bitmap> memoryCache;
    private DiskLruCache diskLruCache;
    private int DISK_CACHE_SIZE = 50 * 1024 * 1024 ;// 50 M
    private int IO_BUFFERED_SIZE = 8 * 1024;
    private int DISK_CACHE_INDEX = 0;
    private boolean isDiskLruCacheCreated = false;

    private static final ThreadFactory sThreadFactory = new ThreadFactory() {
        private final AtomicInteger count = new AtomicInteger();

        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r,"EasyImageLoader#" + count.getAndIncrement());
        }
    };

    public static final Executor THREAD_POOL_EXECUTOR = new ThreadPoolExecutor(
            CORE_POOL_SIZE,MAXIMUM_POOL_SIZE,
            KEEP_ALIVE,TimeUnit.SECONDS,
            new LinkedBlockingDeque<Runnable>(),sThreadFactory
    );

    private Handler mainHandler = new Handler(Looper.getMainLooper()){
        @Override
        public void handleMessage(Message msg) {
            LoaderResult result = (LoaderResult) msg.obj;
            ImageView imageView = result.imageView;
            String url = (String) imageView.getTag(TAG_KEY_URL);
            if (url.equals(result.url)){
                imageView.setImageBitmap(result.bitmap);
            }else {
                Log.w(TAG,"set image bitmap,but url has changed, ignored.");
            }
        }
    };

    private EasyImageLoader(Context context) {
        this.context = context.getApplicationContext();
        imageResizer = new ImageResizer();
        int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        int cacheSize = maxMemory / 8;
        memoryCache = new LruCache<String,Bitmap>(cacheSize){
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                return bitmap.getRowBytes() * bitmap.getHeight() / 1024;
            }
        };
        File diskCacheDir = getDiskCacheDir(context,"bitmap");
        if (!diskCacheDir.exists()){
            diskCacheDir.mkdirs();
        }
        // 判断硬盘可用空间是否大于EasyImageLoader需要的50M
        if (getUsableSpace(diskCacheDir) > DISK_CACHE_SIZE){
            try{
                diskLruCache = DiskLruCache.open(diskCacheDir,1,1,DISK_CACHE_SIZE);
                isDiskLruCacheCreated = true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static EasyImageLoader build(Context context){
        return new EasyImageLoader(context);
    }

    private long getUsableSpace(File path) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            return path.getUsableSpace();
        }
        final StatFs statFs = new StatFs(path.getPath());
        long blockSize, availableBlocks;
        if (Build.VERSION.SDK_INT > 17) {
            blockSize = statFs.getBlockSizeLong();
            availableBlocks = statFs.getAvailableBlocksLong();
        } else {
            blockSize = statFs.getBlockSize();
            availableBlocks = statFs.getAvailableBlocks();
        }
        return blockSize * availableBlocks;
    }

    public File getDiskCacheDir(Context context, String uniqueName) {
        boolean externalStorageAvailable = Environment
                .getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
        final String cachePath;
        if (externalStorageAvailable){
            cachePath = context.getExternalCacheDir().getPath();
        }else {
            cachePath = context.getCacheDir().getPath();
        }
        return new File(cachePath + File.separator + uniqueName);
    }

    private void addBitmapToMemoryCache(String key,Bitmap bitmap){
        if (getBitmapFromMemoryCache(key) == null){
            memoryCache.put(key,bitmap);
        }
    }

    private Bitmap getBitmapFromMemoryCache(String key){
        return memoryCache.get(key);
    }

    private Bitmap loadBitmapFromHttp(String url, int reqWidth, int reqHeight) throws IOException{
        if (Looper.myLooper() == Looper.getMainLooper()){
            throw new RuntimeException("cannot visit network from UI Thread.");
        }
        if (diskLruCache == null){
            return null;
        }

        String key = hashKeyFromUrl(url);
        DiskLruCache.Editor editor = diskLruCache.edit(key);
        if (editor != null){
            OutputStream outputStream = editor.newOutputStream(DISK_CACHE_INDEX);
            if (downloadUrlToStream(url,outputStream)){
                editor.commit();
            }else {
                editor.abort();
            }
            diskLruCache.flush();
        }
        return loadBitmapFromDiskCache(url,reqWidth,reqHeight);
    }

    private boolean downloadUrlToStream(String urlString, OutputStream outputStream) throws IOException {
        HttpURLConnection urlConnection = null;
        BufferedOutputStream out = null;
        BufferedInputStream in = null;
        URL url = null;
        try {
            url = new URL(urlString);
            urlConnection = (HttpURLConnection) url.openConnection();
            in = new BufferedInputStream(urlConnection.getInputStream()
                    ,IO_BUFFERED_SIZE);
            out = new BufferedOutputStream(outputStream,IO_BUFFERED_SIZE);

            int b;
            while ((b = in.read()) != -1){
            out.write(b);
            }
            return true;
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            if (urlConnection != null){
                urlConnection.disconnect();
            }
            MyUtils.close(in);
            MyUtils.close(out);
        }
        return false;
    }

    private Bitmap loadBitmapFromDiskCache(String url,int reqWidth,int reqHeight) throws IOException{
        if (Looper.myLooper() == Looper.getMainLooper()){
            Log.w(TAG,"load bitmap from UI Thread,not recommended!");
        }
        if (diskLruCache == null){
            return null;
        }
        Bitmap bitmap = null;
        String key = hashKeyFromUrl(url);
        DiskLruCache.Snapshot snapshot = diskLruCache.get(key);
        if (snapshot != null){
            FileInputStream fileInputStream = (FileInputStream) snapshot.getInputStream(DISK_CACHE_INDEX);
            FileDescriptor fileDescriptor = fileInputStream.getFD();
            bitmap = imageResizer.decodeSampledBitmapFromFileDescriptor(fileDescriptor,reqWidth,reqHeight);
            if (bitmap != null){
                addBitmapToMemoryCache(key,bitmap);
            }
        }
        return bitmap;
    }

    // 同步加载
    public Bitmap loadBitmap(String url,int reqWidth,int reqHeight) throws IOException {
        Bitmap bitmap = loadBitmapFromMemoryCache(url);
        if (bitmap != null){
            Log.d(TAG,"load bitmap from memory cache,url:"+url);
            return bitmap;
        }
        try{
            bitmap = loadBitmapFromDiskCache(url,reqWidth,reqHeight);
            if (bitmap != null){
                Log.d(TAG,"load bitmap from disk,url:"+url);
                return bitmap;
            }
            bitmap = loadBitmapFromHttp(url,reqWidth,reqHeight);
            Log.d(TAG,"load bitmap from http,url:"+url);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (bitmap == null && !isDiskLruCacheCreated){
            Log.w(TAG,"DiskLruCache is not created.");
            bitmap = downloadBitmapFromUrl(url);
        }
        return bitmap;
    }

    private Bitmap downloadBitmapFromUrl(String urlString) throws IOException {
        Bitmap bitmap = null;
        HttpURLConnection connection = null;
        BufferedInputStream in = null;
        try{
            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            in = new BufferedInputStream(connection.getInputStream(),IO_BUFFERED_SIZE);
            bitmap = BitmapFactory.decodeStream(in);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            if (connection != null){
                connection.disconnect();
            }
            in.close();
        }
        return bitmap;
    }

    private Bitmap loadBitmapFromMemoryCache(String url) {
        final String key = hashKeyFromUrl(url);
        Bitmap bitmap = getBitmapFromMemoryCache(key);
        return bitmap;
    }

    private String hashKeyFromUrl(String url) {
        String cacheKey;
        try{
            MessageDigest digest = MessageDigest.getInstance("MD5");
            digest.update(url.getBytes());
            cacheKey = bytesToHexString(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            cacheKey = String.valueOf(url.hashCode());
        }
        return cacheKey;
    }

    private String bytesToHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0;i < bytes.length; i++){
            String hex = Integer.toHexString(0xFF & bytes[i]);
            if (hex.length() == 1){
                sb.append('0');
            }
            sb.append(hex);
        }
        return sb.toString();
    }

    public void bindBitmap(final String url, final ImageView imageView,final int reqWidth,final int reqHeight){
        imageView.setTag(TAG_KEY_URL,url);
        Bitmap bitmap = loadBitmapFromMemoryCache(url);
        if (bitmap != null){
            imageView.setImageBitmap(bitmap);
            return;
        }

        Runnable loadBitmapTask = new Runnable() {
            @Override
            public void run() {
                Bitmap bitmap = null;
                try {
                    bitmap = loadBitmap(url,reqWidth,reqHeight);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (bitmap != null){
                    LoaderResult result = new LoaderResult(imageView,url,bitmap);
                    mainHandler.obtainMessage(MESSAGE_POST_RESULT,result).sendToTarget();
                }
            }
        };
        THREAD_POOL_EXECUTOR.execute(loadBitmapTask);
    }

    public void bindBitmap(String url,ImageView imageView){
        bindBitmap(url,imageView,0,0);
    }

    class LoaderResult{
        public ImageView imageView;
        public String url;
        public Bitmap bitmap;

        LoaderResult(ImageView imageView,String url,Bitmap bitmap){
            this.imageView = imageView;
            this.url = url;
            this.bitmap = bitmap;
        }
    }
}
