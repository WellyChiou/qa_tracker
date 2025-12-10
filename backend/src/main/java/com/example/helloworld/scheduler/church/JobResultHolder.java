package com.example.helloworld.scheduler.church;

/**
 * 用於在線程間傳遞 Job 執行結果
 * 使用 ThreadLocal 確保線程安全
 */
public class JobResultHolder {
    private static final ThreadLocal<String> resultHolder = new ThreadLocal<>();

    /**
     * 設置執行結果
     */
    public static void setResult(String result) {
        resultHolder.set(result);
    }

    /**
     * 獲取執行結果
     */
    public static String getResult() {
        return resultHolder.get();
    }

    /**
     * 清除執行結果（避免內存洩漏）
     */
    public static void clear() {
        resultHolder.remove();
    }
}

