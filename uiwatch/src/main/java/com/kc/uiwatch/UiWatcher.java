package com.kc.uiwatch;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.Choreographer;

import java.lang.ref.WeakReference;


/**
 * 用于观察Android Ui的卡顿情况
 * 原理：通过监听渲染信号之间的时间差来确定是否卡顿,并采用高频采样确保数据的准确性
 * 功能：
 * <p>
 * 1.可限定帧率检测阈值
 * 2.设置日志输出TAG
 * 3.设置存储的堆栈数量
 * 4.设置是否开启缓存本地
 * 5.设置本地缓存文件夹地址、按照天拆分文件夹
 * <p>
 * 注意：此工具类要求api>=16
 * 细节处理：
 * 1.对于监控的内容采用高频采样,然后根据设置是否开启缓存,缓存到本地,方便后的定位(包含时间戳和分割)
 * 2.限制最大堆的数量
 * 3.采样间隔,16ms 默认获取采样卡顿前10帧的堆栈数据
 */
public class UiWatcher implements Application.ActivityLifecycleCallbacks {

    public static interface BlockListener {
        public void onUiBlock(String message);
    }


    /**
     * 帧率阈值,默认为1(超出1帧时间视为卡顿)
     */
    private int minSkipFrameTime = 17;

    /**
     * 堆栈信息最多保存条数,最少1至多无限制,默认10
     */
    private int cacheDataSize = 10;
    private int cacheDelayTime = 30;

    /**
     * 卡顿时输出日志的log,默认为 UiWatcher
     */
    public static String TAG = "UiWatcher";

    /**
     * 是否开启缓存到文件,默认为true
     */
    private boolean isNeedCacheToFile = true;

    /**
     * 缓存的文件夹地址
     */
    private String cacheFolder = "uiWatcher";

    /**
     * 待筛选的关键词(用于剔除不重要信息,可选,不填入则不剔除)
     */
    private String[] keyWords = null;

    /**
     * 帧率回调
     */
    private UiWatchFrameCallback frameCallback;

    /**
     * 是否观察中
     */
    private boolean isWatching = false;

    /**
     * 单例对象
     */
    private static volatile UiWatcher instance;

    private BlockListener listener;
    private Application app = null;

    /**
     * 私有化
     */
    private UiWatcher(Application app) {
        mMainHandler = new Handler(Looper.getMainLooper());
        this.app = app;
    }

    //-----------------对外静态方法---------------------

    /**
     * 创建UiWatcher
     */
    public static UiWatcher getInstance(Application app) {
        if (instance == null) {
            synchronized (UiWatcher.class) {
                if (instance == null) {
                    instance = new UiWatcher(app);
                }
            }
        }
        return instance;
    }


    //-----------------对外公有方法---------------------

    /**
     * 帧率阈值(默认1),当跳帧超出此阈值时报警,输出日志并缓存(视开关情况)
     *
     * @param minSkipFrameTime 帧率阈值
     */
    public UiWatcher minSkipFrameTime(int minSkipFrameTime) {
        this.minSkipFrameTime = minSkipFrameTime;
        return this;
    }

    /**
     * 是否需要缓存日志到文件
     *
     * @param isNeedCacheToFile true:缓存 false:不缓存
     */
    public UiWatcher isNeedCacheToFile(boolean isNeedCacheToFile) {
        this.isNeedCacheToFile = isNeedCacheToFile;
        return this;
    }

    /**
     * 缓存文件夹
     *
     * @param cacheFolder 文件夹
     */
    public UiWatcher cacheFolder(String cacheFolder) {
        this.cacheFolder = cacheFolder;
        return this;
    }

    /**
     * 缓存堆栈数量
     *
     * @param cacheDataSize 缓存帧率数量
     */
    public UiWatcher cacheSize(int cacheDataSize, int delayTime) {
        this.cacheDataSize = cacheDataSize;
        this.cacheDelayTime = delayTime;
        return this;
    }

    /**
     * 待筛选的关键词
     *
     * @param keyWords 关键词
     */
    public UiWatcher keyWords(String... keyWords) {
        this.keyWords = keyWords;
        return this;
    }

    public UiWatcher listen(BlockListener listener) {
        this.listener = listener;
        return this;
    }

    /**
     * 用于开启监听，必执行方法！！
     */
    @SuppressLint("NewApi")
    public void startWatch() {
        if (isWatching) {
            return;
        }
        //开始之前校验参数
        if (minSkipFrameTime < 17) {
            throw new IllegalArgumentException("minSkipFrameTime 必须大于等于17！");
        }
        if (cacheDataSize < 1) {
            throw new IllegalArgumentException("cacheDataSize 必须大于等于1！");
        }
        if (isNeedCacheToFile) {
            if (TextUtils.isEmpty(cacheFolder)) {
                throw new IllegalArgumentException("缓存文件夹不允许为null或者空！");
            }
        }
        //将对应的参数进行配置
        LogMonitor.getInstance().setCacheDataSize(cacheDataSize, cacheDelayTime);
        LogMonitor.getInstance().setCacheFolder(cacheFolder);
        LogMonitor.getInstance().setNeedCacheToFile(isNeedCacheToFile);
        LogMonitor.getInstance().setKeyWords(keyWords);
        LogMonitor.getInstance().setListener(listener);
        LogMonitor.getInstance().setTag(TAG);
        //将当前回调注册到系统
        frameCallback = new UiWatchFrameCallback(minSkipFrameTime);
        Choreographer.getInstance().postFrameCallback(frameCallback);
        app.unregisterActivityLifecycleCallbacks(this);
        app.registerActivityLifecycleCallbacks(this);
    }

    @SuppressLint("NewApi")
    public void stopWatch() {
        //关闭帧率监听
        if (frameCallback != null) {
            frameCallback.setExit(true);
            Choreographer.getInstance().removeFrameCallback(frameCallback);
            frameCallback = null;
        }
        //关闭日志监听以及相关的线程等资源
        LogMonitor.getInstance().stopMonitor();
        app.unregisterActivityLifecycleCallbacks(this);
        //切换当前的状态
        isWatching = false;
    }


    public void pauseWatch() {
        frameCallback.setPause(true);
    }

    public void resumeWatch() {
        frameCallback.setPause(false);
    }

    private String getActivityHash(Activity activity) {
        return activity.getClass().getName() + activity.hashCode();
    }

    private String mCurActivityHash;
    private boolean mIsPaused, mIsForeground;
    private final Handler mMainHandler;
    private static final long CHECK_DELAY = 600;
    private Runnable mCheckRunnable;
    @Override
    public void onActivityResumed(final Activity activity) {
        mIsPaused = false;
        final boolean wasBackground = !mIsForeground;
        mIsForeground = true;
        final String activityHash = getActivityHash(activity);

        mCurActivityHash = activityHash;
        final WeakReference<Activity> mActivityWeakReference = new WeakReference<>(activity);
        mMainHandler.postDelayed(mCheckRunnable = new Runnable() {
            @Override
            public void run() {
                if (wasBackground) {
                    Activity ac = mActivityWeakReference.get();
                    if (null == ac) {
                        return;
                    }
                    resumeWatch();
                }
            }
        }, CHECK_DELAY);
    }

    @Override
    public void onActivityPaused(final Activity activity) {
        mIsPaused = true;
        if (mCheckRunnable != null) {
            mMainHandler.removeCallbacks(mCheckRunnable);
        }

        final WeakReference<Activity> mActivityWeakReference = new WeakReference<>(activity);
        mMainHandler.postDelayed(mCheckRunnable = new Runnable() {
            @Override
            public void run() {
                if (mIsForeground && mIsPaused) {
                    mIsForeground = false;
                    Activity ac = mActivityWeakReference.get();
                    if (null == ac) {
                        return;
                    }
                    pauseWatch();
                }
            }
        }, CHECK_DELAY);
    }

    @Override
    public void onActivityCreated(final Activity activity, Bundle savedInstanceState) {
    }

    @Override
    public void onActivityStarted(Activity activity) {
    }

    @Override
    public void onActivityStopped(Activity activity) {
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
    }

    @Override
    public void onActivityDestroyed(final Activity activity) {
        if (getActivityHash(activity).equals(mCurActivityHash)) {
            mCurActivityHash = null;
        }
    }

}
