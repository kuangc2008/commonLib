package com.kc.uiwatch;

/**
 * 日志监控
 * 用于日志的存储和输出
 * LogMonitor通过handler对LogThread 进行操作，具体执行是LogThread
 */
public class LogMonitor {
    /**
     * 日志监听实例
     */
    private static volatile LogMonitor instance = null;

    private LogNotifier logNotifier;


    /**
     * 初始化日志打印类(内部开启两个线程一个用于缓存,一个用于输出)
     */
    private LogMonitor() {
        init();
    }

    private void init() {
        logNotifier = LogNotifier.getInstance();
    }

    public static LogMonitor getInstance() {
        if (instance == null) {
            synchronized (LogMonitor.class) {
                if (instance == null) {
                    instance = new LogMonitor();
                }
            }
        }
        return instance;
    }


    /**
     * 开启Log监听
     */
    public void startMonitor() {
        if (logNotifier == null) {
            return;
        }
        logNotifier.start();
        logNotifier.startCollectionNotifier();
    }

    /**
     * 关闭Log监听
     */
    public void stopMonitor() {
        if (logNotifier != null) {
            logNotifier.stop();
        }
    }


    /**
     * 开始输出并重新开始收集日志
     */
    public void startOutputAndResetCollectionMonitor(int skipFrameCount) {
        if (logNotifier == null) {
            return;
        }
        logNotifier.startOutputAndRestartCollectionNotifier(skipFrameCount);
    }


    //-----------------基础信息---------------------

    /**
     * 设置缓存数量
     *
     * @param cacheDataSize 缓存数量,默认10
     */
    public void setCacheDataSize(int cacheDataSize, int cacheDelayTime) {
        if (logNotifier != null) {
            logNotifier.setCacheDataSize(cacheDataSize, cacheDelayTime);
        }
    }

    /**
     * 设置日志TAG
     *
     * @param tag tag
     */
    public void setTag(String tag) {
        if (logNotifier != null) {
            logNotifier.setTag(tag);
        }
    }

    /**
     * 设置是否需要缓存文件
     *
     * @param needCacheToFile true:缓存本地 false:不缓存到本地
     */
    public void setNeedCacheToFile(boolean needCacheToFile) {
        if (logNotifier != null) {
            logNotifier.setNeedCacheToFile(needCacheToFile);
        }
    }

    /**
     * 设置缓存的文件夹
     *
     * @param cacheFolder 文件夹
     */
    public void setCacheFolder(String cacheFolder) {
        if (logNotifier != null) {
            logNotifier.setCacheFolder(cacheFolder);
        }
    }

    /**
     * 设置筛选关键词
     *
     * @param keyWords 关键词集合
     */
    public void setKeyWords(String[] keyWords) {
        if (logNotifier != null) {
            logNotifier.setKeyWords(keyWords);
        }
    }

    public void setListener(UiWatcher.BlockListener listener) {
        if (logNotifier != null) {
            logNotifier.setListener(listener);
        }
    }
}
