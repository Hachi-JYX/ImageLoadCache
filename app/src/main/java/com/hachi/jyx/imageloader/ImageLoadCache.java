package com.hachi.jyx.imageloader;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Message;
import android.support.v4.util.LruCache;
import android.util.Log;
import android.widget.ImageView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 图片的三级缓存加载工具类
 */
public class ImageLoadCache {
    /**
     * 一级缓存，内部是LinkedHashMap
     */
    private LruCache<String,Bitmap> cache = null;
    private File cacheDir = null;//本地的缓存路径
    private ExecutorService threadpool = null;//线程池
    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 200){
                HashMap<String,Object> map = (HashMap<String, Object>) msg.obj;
                ImageView imageView = (ImageView) map.get("imageview");
                Bitmap bitmap = (Bitmap) map.get("bitmap");
                imageView.setImageBitmap(bitmap);
            }
        }
    };

    public ImageLoadCache(Context context) {
        //创建5个线程
        threadpool = Executors.newFixedThreadPool(5);
        cacheDir = context.getCacheDir();//获取缓存目录
        //获取手机运行时的最大内存,取其8分之一
        long maxSize = Runtime.getRuntime().maxMemory()/8;
        //设置内存能缓存的图片容量大小
        cache = new LruCache<String,Bitmap>((int) maxSize){
            //计算图片大小的计算
            @Override
            protected int sizeOf(String key, Bitmap value) {
                int bytesRow = value.getRowBytes();//获取行字节数
                int rowCount = value.getHeight();//获取行数
                return bytesRow * rowCount;
            }
        };
    }

    public void displayImage(ImageView imageView, String url){
        Bitmap bitmap = getFromCache(url);
        if (bitmap!=null){
            Log.i("Tag","从内存中取得图片");
            imageView.setImageBitmap(bitmap);
            return;
        }

        bitmap = getFromLocal(url);
        if (bitmap!=null){
            Log.i("Tag","从本地中取得图片");
            imageView.setImageBitmap(bitmap);
            return;

        }

        getFromNet(imageView, url);
        return;
    }

    /**
     * 从内存集合中取
     * @param url
     * @return
     */
    private Bitmap getFromCache(String url) {
        return cache.get(url);
    }

    /**
     * 从本地文件中取
     * @param url
     * @return
     */
    private Bitmap getFromLocal(String url) {
        try {
            //去除 :
            String filename = URLEncoder.encode(url,"UTF-8");
            //获得图片文件的路径
            File file = new File(cacheDir.getAbsolutePath() + "/" + filename);
            //使用BitmapFactory将文件转换为Bitmap
            Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
            //将bitmap存到内存集合中，下次访问直接从一级缓存中读取
            cache.put(url,bitmap);
            return bitmap;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private void getFromNet(ImageView imageView, String url) {
        //获取一个线程
        ImageRunnable runnable = new ImageRunnable(imageView, url);
        threadpool.execute(runnable);
        //访问网络
        //从流中
    }

    private class ImageRunnable implements Runnable{

        private String url;
        private ImageView imageView;

        public ImageRunnable(ImageView view,String url) {
            this.url = url;
            this.imageView = view;
        }

        @Override
        public void run() {
            try {
                URL imgUrl = new URL(this.url);
                HttpURLConnection connection = (HttpURLConnection) imgUrl.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000);
                if (200 == connection.getResponseCode()){
                    InputStream inputStream = connection.getInputStream();
                    // 使用工具快速生成bitmap对象
                    byte[] bytes = StreamUtils.readInputStream(inputStream);
                    Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

                    if (bitmap!=null){
                        Message message = Message.obtain();
                        message.what = 200;
                        HashMap<String,Object> map = new HashMap<>();
                        map.put("imageview",imageView);
                        map.put("bitmap",bitmap);
                        message.obj = map;
                        mHandler.sendMessage(message);
                        //保存到集合
                        cache.put(url,bitmap);
                        //保存到文件
                        putToLocal(url,bitmap);
                    }

                }


            } catch (Exception e) {
                e.printStackTrace();
            }
            Message msg = Message.obtain();
            msg.what=404;
            mHandler.sendMessage(msg);
        }
    }

    private void putToLocal(String url, Bitmap bitmap) {
        String filename = null;
        try {
            filename = URLEncoder.encode(url,"UTF-8");
            //获得图片文件的路径
            File file = new File(cacheDir.getAbsolutePath() + "/" + filename);
            if (!file.exists()){
                file.createNewFile();
            }
            //打开一个输出流
            FileOutputStream fos = new FileOutputStream(file);
            //将图片数据写入到文件输出流
            bitmap.compress(Bitmap.CompressFormat.JPEG,80,fos);
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }


    }


}
