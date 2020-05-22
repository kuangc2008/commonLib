package com.kc.uiwatch;

import java.util.LinkedList;
import java.util.List;


public class LogStackQueue {


    /**
     * log堆栈信息队列,只保存限制的条数,防止内存占用过大
     */
    private List<LogStackInfo> logStackInfoQueue = new LinkedList<>();

    public void add(String currentStackInfo) {

        //获取上一个堆栈信息比较是否相同
        LogStackInfo lastInfo = logStackInfoQueue.isEmpty() ? null : logStackInfoQueue.get(logStackInfoQueue.size() - 1);
        //不相同且有有效内容

        if (lastInfo != null && lastInfo.getStackMessage() != null && lastInfo.getStackMessage().equals(currentStackInfo)) {
            int count = lastInfo.getCount();
            lastInfo.setCount(++count);
//            lastInfo.setEndTime(System.currentTimeMillis());
        } else {
            LogStackInfo info = new LogStackInfo();
            info.setCount(1);
            info.setStackMessage(currentStackInfo);
//            info.setStartTime(System.currentTimeMillis());
            logStackInfoQueue.add(info);
        }
    }

    public boolean isEmpty() {
        return logStackInfoQueue.isEmpty();
    }

    public List<LogStackInfo> getLogStackInfoQueue() {
        return logStackInfoQueue;
    }

    public int size() {
        int size = 0;
        for (LogStackInfo info : logStackInfoQueue) {
            size += info.getCount();
        }

        return size;
    }



}
