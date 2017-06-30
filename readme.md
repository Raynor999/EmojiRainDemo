## 模仿微信实现表情飘落功能

### 需求分析

* 匹配发送的聊天文本、匹配到服务器配置的口令触发表情下落功能
* 表情从屏幕自上而下飘落，速度、出现的数量、X轴起始位置、随机


### 需求细节分析
* 微信飘落表情大小会进行随机大小比例变化，用平均值为0.0 标准差为1.0的 **高斯分布**的随机数，对表情进行比例放大缩小。
* 表情垂直下落不自然，做相对于出现位置X轴方向左右随机偏移一定大小。

### 实现思路
* 实现一个对象池回收管理ImageView，缓存一屏幕最多可以出现的表情数量。
* 每一个表情，动态向布局中添加ImageView，让ImageView做属性动画、通过设置不同动画时间实现下落速度随机。
* 关键的可配置参数：
	1. 每一波表情出现的频率
	2. 	一波表情的数量
	3. 表情雨持续时长、总共出现的表情数量 =  持续时长/频率 * 每一波表情数量 


### 具体实现

先看 startDropping()这个方法，这里使用[Rxjava](https://mcxiaoke.gitbooks.io/rxdocs/content/topics/Getting-Started.html)的流式Api和操作符比较方便的实现了表情频率、每一波表情数量的控制。 使用Handler定时发送Message来实现也是一样。 使用[Rxjava.interval](https://mcxiaoke.gitbooks.io/rxdocs/content/operators/Interval.html)操作符按照设置的频率发送事件。
使用take操作符取 **动画持续时长 / 频率**个事件 实现动画总时长的控制。 flatMap变换 控制每一波下落多少个表情。然后从缓存池中取ImageView做属性动画

```java
    public void startDropping() {
        RandomUtils.setSeed(SystemClock.elapsedRealtime());
        mWindowHeight = getWindowHeight();
        initPools();
        /* 使用Rxjava控制逻辑， 使用下落频率发射事件，take 操作符控制总时间，
        共取 持续事件/频率 个事件 */
        Subscription subscribe = Observable.interval(0, mDropFrequency, TimeUnit.MILLISECONDS)
                .take(mRainDuration / mDropFrequency)
                //每一波表情，下落多少个
                .flatMap(flow -> Observable.range(0, mPerEmojiCount))
                .observeOn(AndroidSchedulers.mainThread())
                .map(index -> getEmojiView())
                .subscribe(this::startDropAnimation, Throwable::printStackTrace);
        mSubscriptionList.add(subscribe);
    }
    
```
这里使用supportv4包下的 Pools.SynchronizedPool 缓存ImageView对象，减少对象的创建销毁。缓存池大小通过计算最多同时存在的表情数量设置。 创建ImageView的时候，需要实现随机调整表情大小。 最开始使用的 （0.75f ~ 1.25f）之间的随机倍数调整表情大小，实际效果不自然没有微信的效果好。最后使用数学的 **正态分布（高斯分布） 随机数， 均值为0，标准差1.0f，使放大倍数正太分布在 1.0f倍，**同时修正过大倍数的情况，最终达到了比较满意的效果。

```java
    private void initPools() {
        clearDirtyEmojisInPool();

        int maxPoolSize = (int) ((1 + STAND_OFFSET) *
                (mDropAvgDuration / mDropFrequency) * mPerEmojiCount);
        //计算屏幕中最多同时出现的表情数量设置缓存池大小
        mImageViewPools = new Pools.SynchronizedPool<>(maxPoolSize);
        for (int i = 0; i < maxPoolSize; i++) {
            ImageView imageView = createImageView();
            addView(imageView, 0);
            mImageViewPools.release(imageView);
        }
    }
    
    private ImageView createImageView() {
        ImageView imageView = new ImageView(getContext());
        //用平均值为0.0 标准差为1.0的正态分布随机数 控制表情大小的随机变化，
        // 让表情大小的scale倍数集中在 1.0f 倍，使表情大小变化更自然
        int emojiSize = (int) (mEmojiStandSize * (1.0 + 			RandomUtils.positiveGaussian()));
        final int maxEmojiSize = (int) (mEmojiStandSize * 1.8);
        if (emojiSize > maxEmojiSize) { //修正高斯随机数过大的情况
            emojiSize = emojiSize / 2;
        }
        final LayoutParams params = new LayoutParams(emojiSize, emojiSize);
        params.topMargin = -emojiSize;
        //控制表情起始位置，在X轴随机位置  leftMargin = 屏幕宽度 * （0.1 ~ 0.9）
        params.leftMargin = (int) ((-0.5F * emojiSize) +
                getResources().getDisplayMetrics().widthPixels * RandomUtils.floatInRange(0.1f, 0.9f));
        imageView.setLayoutParams(params);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) { //加上x轴方向阴影
            imageView.setElevation(dip2px(5));
        }
        return imageView;
    }
```




