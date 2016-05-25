package com.hachi.jyx.imageloader;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ImageView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ImageView imageView = (ImageView) findViewById(R.id.iv);
        //呵呵
        String url = "http://www.k618.cn/ygmp/dzrmp/zrfg/201205/W020120523404414407920.jpg";
        ImageLoadCache imageLoadCache = new ImageLoadCache(getBaseContext());
        imageLoadCache.displayImage(imageView,url);
    }
}
