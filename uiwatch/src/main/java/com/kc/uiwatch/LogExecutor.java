package com.kc.uiwatch;

import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;


import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * 负责日志收集和输出
 */
public class LogExecutor {
    /**
     * 用于处理日志的线程
     */
    private HandlerThread logExecutorThread;
    /**
     * 用于处理日志的Handler
     */
    private Handler logExecutorHandler;
    /**
     * 实例
     */
    private static LogExecutor instance;
    //----------------base config----------------------
    /**
     * 堆栈信息最多保存条数,最少1至多无限制,默认10
     */
    private int cacheDataSize = 10;
    private int delayTime = 30;

    /**
     * 卡顿时输出日志的log,默认为 UiWatcher
     */
    private String tag = "UiWatcher";

    /**
     * 是否开启缓存到文件,默认为true
     */
    private boolean isNeedCacheToFile = true;

    /**
     * 缓存的文件夹地址
     */
    private String cacheFolder = "UiWatcher";

    /**
     * 缓存文件名称
     */
    private String cacheFileName = "UiWatcherLogData";

    /**
     * 关键词
     */
    private String[] keyWords = null;


    //------------- type config-------------------
    /**
     * 数据收集
     */
    private static final int TYPE_COLLECTION = 0;
    /**
     * 数据输出
     */
    private static final int TYPE_OUTPUT = 1;

    //------------- data config-------------------
    /**
     * 日志堆栈信息的构造builder
     */
//    private StringBuilder logStackInfoBuilder;
    private JSONObject logStackOutPutJson;

    /**
     * log堆栈信息队列,只保存限制的条数,防止内存占用过大
     */
    private LogStackQueue mlogStackQueue;
    private UiWatcher.BlockListener mListener = null;


    private LogExecutor() {
        init();
    }

    /**
     * 单例创建实例
     */
    public static LogExecutor getInstance() {
        if (instance == null) {
            synchronized (LogExecutor.class) {
                if (instance == null) {
                    instance = new LogExecutor();
                }
            }
        }
        return instance;
    }

    /**
     * 开启线程
     */
    public void start() {
        if (logExecutorThread == null) {
            init();
        }
        logExecutorThread.start();
        initLogExecutorHandler();
    }

    private void init() {
        logExecutorThread = new HandlerThread("LogExecutor_Thread");
//        logStackInfoBuilder = new JSONObject();
        mlogStackQueue = new LogStackQueue();
    }

    /**
     * 初始化logExecutorHandler
     */
    private void initLogExecutorHandler() {
        if (logExecutorThread == null) {
            return;
        }
        logExecutorHandler = new Handler(logExecutorThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                try {
                    handleLogExecutorMessage(msg);
                } catch (Exception e) {
                }
            }
        };
    }

    /**
     * 用于分发LogExecutorMessage
     *
     * @param msg 消息
     */
    private void handleLogExecutorMessage(Message msg) {
        int type = msg.what;
        Object stackInfo = msg.obj;
        switch (type) {
            case TYPE_COLLECTION:
                handleLogExecutorCollectionMessage(stackInfo);
                break;
            case TYPE_OUTPUT:
                handleLogExecutorOutputMessage(msg.arg1);
                break;
            default:
                break;
        }
    }

    /**
     * 用于处理输出消息
     */
    private void handleLogExecutorOutputMessage(int skipFrame) {
        //输出堆栈信息
        startOutputLogTask(skipFrame);
    }

    /**
     * 开始输出日志任务
     */
    private void startOutputLogTask(int skipFrame) {

        //校验缓存信息的队列
        if (mlogStackQueue == null || mlogStackQueue.isEmpty()) {
            return;
        }
        //将获取的队列内的内容遍历获取
        StringBuilder logStackInfoBuilder = new StringBuilder();

        logStackInfoBuilder.append("block:");
        logStackInfoBuilder.append(skipFrame);
        logStackInfoBuilder.append(" \n");
        logStackInfoBuilder.append("delay:");
        logStackInfoBuilder.append(delayTime);
        logStackInfoBuilder.append(" \n");
        logStackInfoBuilder.append("time:");
        logStackInfoBuilder.append(TimeUtils.getCurrentFormatTime());
        logStackInfoBuilder.append(" \n");
        logStackInfoBuilder.append(" \n");



        for (LogStackInfo stackInfo :  mlogStackQueue.getLogStackInfoQueue()) {
            logStackInfoBuilder.append("~~~");
            logStackInfoBuilder.append("\n");
            if (stackInfo.getCount() > 1) {
                logStackInfoBuilder.append("count:");
                logStackInfoBuilder.append(stackInfo.getCount());
                logStackInfoBuilder.append("\n");
            }

//            logStackInfoBuilder.append("startP:");
//            logStackInfoBuilder.append(stackInfo.getStartTime());
//            logStackInfoBuilder.append("\n");
//
//            if (stackInfo.getEndTime() != 0) {
//                logStackInfoBuilder.append("endP:");
//                logStackInfoBuilder.append(stackInfo.getEndTime());
//                logStackInfoBuilder.append("\n");
//            }

            logStackInfoBuilder.append(stackInfo.getStackMessage());
            logStackInfoBuilder.append("\n");
            logStackInfoBuilder.append("~~~");
            logStackInfoBuilder.append("\n");
            logStackInfoBuilder.append("\n");
        }
        //清除原队列数据
        mlogStackQueue.getLogStackInfoQueue().clear();
        //获取全部的堆栈信息
        String allStackInfo = logStackInfoBuilder.toString();
        //输出信息并视情况缓存

        if (mListener != null) {
            mListener.onUiBlock(allStackInfo);
        }

//        if (SystemInfo.DEBUG) {
            LogUtils.printLog(tag, allStackInfo);
//        }


        //检测是否需要存储到本地
//        if (isNeedCacheToFile) {
//            saveAllStackInfoToFile(allStackInfo);
//        }



//        //校验缓存信息的队列
//        if (mlogStackQueue == null || mlogStackQueue.isEmpty()) {
//            return;
//        }
//
//        Log.i("kcc", "skipFrame-> " + skipFrame);
//
//        //将获取的队列内的内容遍历获取
//        logStackOutPutJson  = new JSONObject();
//        try {
//            logStackOutPutJson.put("skipCount", skipFrame);
//
//            JSONArray stackInfos = new JSONArray();
//
//            for (LogStackInfo info : mlogStackQueue.getLogStackInfoQueue()) {
//                JSONObject object = new JSONObject();
//                object.put("stack", info.getStackMessage());
//                object.put("count", info.getCount());
//                object.put("startP", info.getStartTime());
//                if (info.getEndTime() != 0) {
//                    object.put("endP", info.getEndTime());
//                }
//                stackInfos.put(object);
//            }
//            logStackOutPutJson.put("info", stackInfos);
//
//
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }
//
//
//        //清除原队列数据
//        mlogStackQueue.getLogStackInfoQueue().clear();
//        //获取全部的堆栈信息
//        String allStackInfo = logStackOutPutJson.toString();
//        //输出信息并视情况缓存
//        if (SystemInfo.DEBUG) {
//            LogUtils.printLog(tag, allStackInfo);
//        }
//        //检测是否需要存储到本地
//        if (isNeedCacheToFile) {
//            saveAllStackInfoToFile(allStackInfo);
//        }
    }




    /**
     * 保存所有的堆栈信息到文件
     *
     * @param allStackInfo 所有的堆栈信息
     */
    private void saveAllStackInfoToFile(String allStackInfo) {
        RandomAccessFile rfile = null;
        //获取文件通道
        FileChannel channel;
        try {
            //根据配置生成文件夹地址
            final String finalFileRootFolderPath = Environment.getExternalStorageDirectory() + "/" + cacheFolder;
            final String finalFileFolderPath = finalFileRootFolderPath + "/" + TimeUtils.getFileFolderNameByTime();
            //校验文件夹是否存在,不存在则创建
            File fileFolder = new File(finalFileFolderPath);
            if (!fileFolder.exists()) {
                fileFolder.mkdirs();
            }
            //校验文件是否存在
            String cacheFilePath = finalFileFolderPath + "/" + cacheFileName + ".txt";
            File cacheFile = new File(cacheFilePath);
            if (!cacheFile.exists()) {
                cacheFile.createNewFile();
            }
            //追加文件写入新的堆栈信息
            //获取文件
            rfile = new RandomAccessFile(cacheFilePath, "rw");
            //获取文件通道
            channel = rfile.getChannel();
            channel.position(channel.size());
            //写入缓冲区
            byte[] allStackInfoBytes = allStackInfo.getBytes();
            ByteBuffer buff = ByteBuffer.wrap(allStackInfoBytes);
            buff.put(allStackInfoBytes);
            buff.flip();
            //写入文件
            channel.write(buff);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            //关闭流,刷新到文件
            if (rfile != null) {
                try {
                    rfile.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    /**
     * 用于处理收集消息
     *
     * @param stackInfo 待缓存堆栈信息
     */
    private void handleLogExecutorCollectionMessage(Object stackInfo) {
        //收集堆栈信息
        startCollectionLogTask(stackInfo);
    }

    /**
     * 发送给LogExecutor 消息
     *
     * @param msg
     */
    public void sendLogExecutorMessage(Message msg) {
        if (logExecutorHandler == null || msg == null || msg.getTarget() == null) {
            return;
        }
        logExecutorHandler.sendMessage(msg);
    }

    /**
     * 创建收集的消息
     *
     * @param stackInfo 堆栈消息
     */
    public Message obtainCollectionMessage(Object stackInfo) {
        if (stackInfo == null) {
            return null;
        }
        Message collectionMsg = Message.obtain(logExecutorHandler, TYPE_COLLECTION, stackInfo);
        return collectionMsg;
    }

    /**
     * 创建输出的消息
     */
    public Message obtainOutputMessage() {
        Message outputMsg = Message.obtain(logExecutorHandler, TYPE_OUTPUT, null);
        return outputMsg;
    }


    private int mum = 0;

    /**
     * 开始日志收集任务
     *
     * @param stackInfo 堆栈信息对象
     */
    private void startCollectionLogTask(Object stackInfo) {
        //校验数据类型是否正确
        if (!(stackInfo instanceof StackTraceElement[])) {
            return;
        }
        StackTraceElement[] stackTraceElements = (StackTraceElement[]) stackInfo;

        //检验堆栈信息
        if (stackTraceElements == null || stackTraceElements.length == 0) {
            return;
        }

        StringBuilder logStackInfoBuilder = new StringBuilder();
        int i = 1;
        for (StackTraceElement mStackInfo : stackTraceElements) {
            String info = mStackInfo.toString();
            logStackInfoBuilder.append(info);
            if (i != stackTraceElements.length) {
                logStackInfoBuilder.append("\n");
            }
            i++;
        }


        //获取当前堆栈信息,存储到队列
        String currentStackInfo = logStackInfoBuilder.toString();
        mlogStackQueue.add(currentStackInfo);

        if (mlogStackQueue.size()  - mlogStackQueue.getLogStackInfoQueue().get(0).getCount() >= cacheDataSize) {
            mlogStackQueue.getLogStackInfoQueue().remove(0);
        }
    }

    /**
     * 关闭执行
     */
    public void stop() {
        if (logExecutorHandler != null) {
            logExecutorHandler.removeCallbacksAndMessages(null);
            logExecutorHandler = null;
        }
        if (logExecutorThread != null) {
            logExecutorThread.quit();
            logExecutorThread = null;
        }
    }

    //----------------基础信息的配置函数----------------

    /**
     * 设置缓存数量
     *
     * @param cacheDataSize 缓存数量
     */
    public void setCacheDataSize(int cacheDataSize, int delayTime) {
        this.cacheDataSize = cacheDataSize;
        this.delayTime = delayTime;
    }

    /**
     * 设置日志输出TAG
     *
     * @param tag tag
     */
    public void setTag(String tag) {
        this.tag = tag;
    }

    /**
     * 设置是否需要缓存到本地文件
     *
     * @param needCacheToFile true:需要 false:不需要
     */
    public void setNeedCacheToFile(boolean needCacheToFile) {
        isNeedCacheToFile = needCacheToFile;
    }

    /**
     * 设置缓存文件夹的名称
     *
     * @param cacheFolder 缓存文件夹名称
     */
    public void setCacheFolder(String cacheFolder) {
        this.cacheFolder = cacheFolder;
    }

    /**
     * 设置缓存文件名称
     *
     * @param cacheFileName 缓存文件名
     */
    public void setCacheFileName(String cacheFileName) {
        this.cacheFileName = cacheFileName;
    }

    /**
     * 设置过滤关键词 （排除不是关键词内的内容）
     *
     * @param keyWords 关键词
     */
    public void setKeyWords(String[] keyWords) {
        this.keyWords = keyWords;
    }

    public void setListener(UiWatcher.BlockListener listener) {
        this.mListener = listener;
    }
}
