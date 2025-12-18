package com.example.helloworld.dto.church;

public class ReplyResult {
    private final boolean ok;
    private final String text;

    // ✅ 不要把所有欄位塞進來，用 type/payload 承載「特定功能」需要的資料
    private final String type;     // e.g. "SERVICE_UPDATE"
    private final Object payload;  // e.g. ServiceUpdatePayload

    private ReplyResult(boolean ok, String text, String type, Object payload) {
        this.ok = ok;
        this.text = text;
        this.type = type;
        this.payload = payload;
    }

    public boolean isOk() { return ok; }
    public String getText() { return text; }
    public String getType() { return type; }
    public Object getPayload() { return payload; }

    public static ReplyResult ok(String text) {
        return new ReplyResult(true, text, null, null);
    }

    public static ReplyResult fail(String text) {
        return new ReplyResult(false, text, null, null);
    }

    public static <T> ReplyResult ok(String text, String type, T payload) {
        return new ReplyResult(true, text, type, payload);
    }
}