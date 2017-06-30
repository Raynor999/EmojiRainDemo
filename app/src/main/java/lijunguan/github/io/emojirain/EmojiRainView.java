package lijunguan.github.io.emojirain;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.SystemClock;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.util.Pools;
import android.util.AttributeSet;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;

import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.internal.util.SubscriptionList;

/**
 * Created by lijunguan on 2017/6/30.
 * email: junguan.li@nai.com phone 15638852047
 */

public class EmojiRainView extends FrameLayout {
    /**
     * 相对标准值的偏西亮
     */
    public static final float STAND_OFFSET = 0.25f;

    private static final int DEFAULT_PER_EMOJI_COUNT = 3;

    private static final int DEFAULT_RAIN_DURATION = 5000;

    private static final int DEFAULT_DROP_AGV_DURATION = 3000;

    private static final int DEFAULT_DROP_FREQUENCY = 500;

    private Pools.SynchronizedPool<ImageView> mImageViewPools;
    /**
     * 掉落频率
     */
    private int mDropFrequency = DEFAULT_DROP_FREQUENCY;
    /**
     * 每次掉落的表情数量
     */
    private int mPerEmojiCount = DEFAULT_PER_EMOJI_COUNT;
    /**
     * 表情掉落平均持续时间
     */
    private int mDropAvgDuration = DEFAULT_DROP_AGV_DURATION;
    /**
     * 表情雨持续时间
     */
    private int mRainDuration = DEFAULT_RAIN_DURATION;

    private int mWindowHeight;


    private Drawable mEmojiDrawable;

    private int mEmojiStandSize;

    /**
     * X 轴偏移的基准量
     */
    private int mXStandOffset;

    private SubscriptionList mSubscriptionList = new SubscriptionList();

    private String TAG = EmojiRainView.class.getSimpleName();


    public EmojiRainView(@NonNull Context context) {
        this(context, null);
    }

    public EmojiRainView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public EmojiRainView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public EmojiRainView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.EmojiRainView);
        mPerEmojiCount = ta.getInt(R.styleable.EmojiRainView_perEmojiCount, DEFAULT_PER_EMOJI_COUNT);
        mDropFrequency = ta.getInt(R.styleable.EmojiRainView_dropFrequency, DEFAULT_DROP_FREQUENCY);
        mRainDuration = ta.getInt(R.styleable.EmojiRainView_rainDuration, DEFAULT_RAIN_DURATION);
        mDropAvgDuration = ta.getInt(R.styleable.EmojiRainView_dropAvgDuration, DEFAULT_DROP_AGV_DURATION);
        mEmojiStandSize = (int) ta.getDimension(R.styleable.EmojiRainView_emojiSize,
                dip2px(36));
        ta.recycle();
    }

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

    private void initPools() {
        clearDirtyEmojisInPool();
        //计算屏幕中最多同时出现的表情数量设置缓存池大小
        int maxPoolSize = (int) ((1 + STAND_OFFSET) *
                (mDropAvgDuration / mDropFrequency) * mPerEmojiCount);
        mImageViewPools = new Pools.SynchronizedPool<>(maxPoolSize);
        for (int i = 0; i < maxPoolSize; i++) {
            ImageView imageView = createImageView();
            addView(imageView, 0);
            mImageViewPools.release(imageView);
        }
    }

    private void clearDirtyEmojisInPool() {
        if (mImageViewPools != null) {
            ImageView dirtyEmoji;
            while ((dirtyEmoji = mImageViewPools.acquire()) != null) {
                removeView(dirtyEmoji);
            }
        }
    }

    public void stopDropping() {
        mSubscriptionList.clear();
    }

    private void startDropAnimation(ImageView view) {
        view.animate()
                .translationY(mWindowHeight + view.getHeight())
                .translationX(RandomUtils.floatInRange(-5, 5) * dip2px(30))
                .setDuration((int) (mDropAvgDuration * RandomUtils.floatAround(1, STAND_OFFSET)))
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationCancel(Animator animation) {
                        super.onAnimationCancel(animation);
                        if (null != mImageViewPools) {
                            mImageViewPools.release(view);
                            view.setTranslationX(0);
                            view.setTranslationY(0);
                        }
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        if (null != mImageViewPools) {
                            mImageViewPools.release(view);
                            view.setTranslationX(0);
                            view.setTranslationY(0);
                        }
                    }


                })
                .start();
    }


    private ImageView getEmojiView() {
        //先从缓存池中取，没有创建新的，并放入缓存池中。
        ImageView convertView = mImageViewPools.acquire();
        if (convertView == null) {
            convertView = createImageView();
            addView(convertView, 0);
        }
        convertView.setImageDrawable(mEmojiDrawable);
        return convertView;
    }


    private ImageView createImageView() {
        ImageView imageView = new ImageView(getContext());
        //用平均值为0.0 标准差为1.0的正态分布随机数 控制表情大小的随机变化，
        // 让表情大小的scale倍数集中在 1.0f 倍，使表情大小变化更自然
        int emojiSize = (int) (mEmojiStandSize * (1.0 + RandomUtils.positiveGaussian()));
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

    public EmojiRainView setPerEmojiCount(int emojiCount) {
        if (emojiCount > 0) {
            this.mPerEmojiCount = emojiCount;
        }
        return this;
    }

    public EmojiRainView setRainDuration(int duration) {
        if (duration > 0) {
            this.mRainDuration = duration;
        }
        return this;
    }

    public EmojiRainView setDropAvgDuration(int duration) {
        if (duration > 0) {
            this.mDropAvgDuration = duration;
        }
        return this;
    }

    public EmojiRainView setDropFrequency(int mills) {
        if (mills > 0) {
            this.mDropFrequency = mills;
        }
        return this;
    }

    public EmojiRainView setEmoji(@NonNull Bitmap emoji) {
        this.mEmojiDrawable = new BitmapDrawable(getResources(), emoji);
        return this;
    }

    public EmojiRainView setEmoji(@DrawableRes int resId) {
        this.mEmojiDrawable = ContextCompat.getDrawable(getContext(), resId);
        return this;
    }

    public EmojiRainView setEmoji(@NonNull Drawable drawable) {
        this.mEmojiDrawable = drawable;
        return this;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mSubscriptionList.clear();
        removeAllViews();
        mImageViewPools = null;
    }

    private int getWindowHeight() {
        final WindowManager windowManager = ((WindowManager) getContext().getApplicationContext()
                .getSystemService(Context.WINDOW_SERVICE));
        Point point = new Point();
        windowManager.getDefaultDisplay().getSize(point);
        return point.y;
    }

    private int dip2px(float dp) {
        return (int) (getResources().getDisplayMetrics().density * dp + 0.5f);
    }
}
