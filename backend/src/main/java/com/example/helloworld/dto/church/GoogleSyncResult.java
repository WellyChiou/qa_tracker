package com.example.helloworld.dto.church;

public class GoogleSyncResult {
    private final boolean success;
    private final String message;

    public GoogleSyncResult(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public static GoogleSyncResult ok(String msg) {
        return new GoogleSyncResult(true, msg);
    }

    public static GoogleSyncResult fail(String msg) {
        return new GoogleSyncResult(false, msg);
    }
}
