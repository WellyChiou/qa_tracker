package com.example.helloworld.scheduler.personal;

/**
 * Personal 系統定時任務結果持有者
 * 用於在定時任務執行過程中保存結果訊息
 */
public class JobResultHolder {
    private static final ThreadLocal<String> resultHolder = new ThreadLocal<>();

    public static void setResult(String result) {
        resultHolder.set(result);
    }

    public static String getResult() {
        return resultHolder.get();
    }

    public static void clear() {
        resultHolder.remove();
    }
}

