# 图片根据三级缓存原理封装的加载工具

# 使用方法
    ImageLoadCache imageLoadCache = new ImageLoadCache(getBaseContext());//初始化加载器
    imageLoadCache.displayImage(imageView,url);//放入imageview控件和图片的地址
