package com.kc.uiwatch;


import android.os.Build;

import androidx.annotation.RequiresApi;

import java.util.Objects;

public class LogStackInfo {
    private String stackMessage;
    private int count = 0;
//    private long startTime = 0;
//    private long endTime = 0;

    public String getStackMessage() {
        return stackMessage;
    }

    public void setStackMessage(String stackMessage) {
        this.stackMessage = stackMessage;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

//    public long getStartTime() {
//        return startTime;
//    }
//
//    public void setStartTime(long startTime) {
//        this.startTime = startTime;
//    }
//
//    public long getEndTime() {
//        return endTime;
//    }
//
//    public void setEndTime(long endTime) {
//        this.endTime = endTime;
//    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LogStackInfo that = (LogStackInfo) o;
        return Objects.equals(stackMessage, that.stackMessage);
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public int hashCode() {
        return Objects.hash(stackMessage);
    }
}
