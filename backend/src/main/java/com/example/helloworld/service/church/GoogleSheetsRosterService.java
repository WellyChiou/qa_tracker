package com.example.helloworld.service.church;

import com.example.helloworld.dto.church.GoogleSyncResult;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.BatchUpdateValuesRequest;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Service
public class GoogleSheetsRosterService {
    private static final Logger log = LoggerFactory.getLogger(GoogleSheetsRosterService.class);

    private static final DateTimeFormatter SHEET_DATE_FMT = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    private static final String HDR_DATE = "日期";
    private static final String HDR_AUDIO = "音控";
    private static final String HDR_LIVE = "直播";
    private static final String HDR_CAMERA = "攝像";

    // ===== DB keys =====
    private static final String KEY_SHEET_PROD = "google.sheets.church.service_schedules";
    private static final String KEY_SHEET_TEST = "google.sheets.church.service_schedules_test";
    private static final String KEY_TEST_MODE = "google.sheets.church.service_schedules_test_mode";

    private static final String KEY_CONNECT_TIMEOUT = "google.sheets.church.service_schedules.http.connect-timeout-ms";
    private static final String KEY_READ_TIMEOUT = "google.sheets.church.service_schedules.http.read-timeout-ms";
    private static final String KEY_SERVICE_ACCOUNT_JSON = "google.sheets.service_account.json";

    // ===== Loaded config (fixed after init, except testMode can change) =====
    private String spreadsheetIdProd;
    private String spreadsheetIdTest;

    // testMode 可能會被更新，所以用 volatile 讓多執行緒讀取安全
    private volatile boolean testMode;

    private String serviceAccountJson;
    private GoogleCredentials credentials;
    private int connectTimeoutMs;
    private int readTimeoutMs;


    /**
     * Google Sheets API client
     */
    private Sheets sheets;

    @Autowired
    @Qualifier("churchSystemSettingService")
    private SystemSettingService systemSettingService;


    // ================== 初始化 ==================
    @PostConstruct
    public void init() throws Exception {

        // 1) 從 DB 讀「固定值」（prod/test spreadsheetId + timeout）
        this.spreadsheetIdProd = systemSettingService.getSettingValue(KEY_SHEET_PROD, "").trim();
        this.spreadsheetIdTest = systemSettingService.getSettingValue(KEY_SHEET_TEST, "").trim();

        String connectTimeoutStr = systemSettingService.getSettingValue(KEY_CONNECT_TIMEOUT, "3000").trim();
        String readTimeoutStr = systemSettingService.getSettingValue(KEY_READ_TIMEOUT, "5000").trim();

        this.connectTimeoutMs = parseIntOrDefault(connectTimeoutStr, 3000);
        this.readTimeoutMs = parseIntOrDefault(readTimeoutStr, 5000);

        // 2) 從 DB 讀「可變值」（test_mode）— 初始化先讀一次
        this.testMode = systemSettingService.getSettingValueAsBoolean(KEY_TEST_MODE, Boolean.TRUE);

        // 3) service account json
        this.serviceAccountJson = systemSettingService.getSettingValue(KEY_SERVICE_ACCOUNT_JSON, null);

        if (this.spreadsheetIdProd.isBlank()) {
            throw new IllegalStateException("DB setting missing: " + KEY_SHEET_PROD);
        }
        if (this.spreadsheetIdTest.isBlank()) {
            throw new IllegalStateException("DB setting missing: " + KEY_SHEET_TEST);
        }
        if (this.serviceAccountJson.isBlank()) {
            throw new IllegalStateException("DB setting missing: " + KEY_SERVICE_ACCOUNT_JSON);
        }
        log.info("⏹️ [KEY_SERVICE_ACCOUNT_JSON] : {}", KEY_SERVICE_ACCOUNT_JSON);
        log.info("⏹️ [serviceAccountJson]: {}", this.serviceAccountJson);

        // 4) 建立 Sheets client（只建一次）
        try (ByteArrayInputStream in =
                     new ByteArrayInputStream(serviceAccountJson.getBytes(StandardCharsets.UTF_8))) {

            this.credentials = GoogleCredentials.fromStream(in)
                    .createScoped(List.of(SheetsScopes.SPREADSHEETS));
        }

        HttpCredentialsAdapter credentialsAdapter = new HttpCredentialsAdapter(credentials);

        HttpRequestInitializer requestInitializer = (HttpRequest request) -> {
            credentialsAdapter.initialize(request);
            request.setConnectTimeout(this.connectTimeoutMs);
            request.setReadTimeout(this.readTimeoutMs);
        };

        this.sheets = new Sheets.Builder(GoogleNetHttpTransport.newTrustedTransport(), GsonFactory.getDefaultInstance(), requestInitializer).setApplicationName("church-roster-sync").build();

        log.info("GoogleSheetsRosterService initialized from DB. testMode={}, prodSheetId={}, testSheetId={}, connectTimeoutMs={}, readTimeoutMs={}, serviceAccountJson={}", this.testMode, maskSheetId(this.spreadsheetIdProd), maskSheetId(this.spreadsheetIdTest), this.connectTimeoutMs, this.readTimeoutMs, this.serviceAccountJson);
    }

    // ===== helpers =====
    private int parseIntOrDefault(String s, int def) {
        try {
            return Integer.parseInt(s);
        } catch (Exception ignore) {
            return def;
        }
    }

    // log 用：不要把整串 spreadsheetId 暴露在 log
    private String maskSheetId(String id) {
        if (id == null) return "null";
        if (id.length() <= 6) return "******";
        return id.substring(0, 3) + "..." + id.substring(id.length() - 3);
    }

    // ================== 非同步入口 ==================

    public boolean isTestMode() {
        return testMode;
    }

    @Async
    public CompletableFuture<GoogleSyncResult> syncAsync(LocalDate date, String positionName, String personName) {
        // 讓 async 也享受到 retry（syncWithRetry 裡會 throw 讓 Retryable 接手）
        try {
            GoogleSyncResult result = syncWithRetry(date, positionName, personName);
            return CompletableFuture.completedFuture(result);
        } catch (Exception e) {
            // @Recover 應該會攔到，這裡只是保險（避免 CompletableFuture 直接丟例外）
            log.error("syncAsync failed unexpectedly", e);
            return CompletableFuture.completedFuture(GoogleSyncResult.fail("📄 Google PLC 服事表 同步失敗：" + e.getMessage()));
        }
    }

    // ================== Retry 包裝 ==================
    @Retryable(value = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2.0))
    public GoogleSyncResult syncWithRetry(LocalDate date, String positionName, String personName) throws Exception {
        return syncOneUpdate(date, positionName, personName);
    }

    // ================== Retry 最終失敗 ==================
    @Recover
    public GoogleSyncResult recover(Exception e, LocalDate date, String positionName, String personName) {
        log.error("Google Sheet sync failed after retries. date={}, position={}, person={}", date, positionName, personName, e);
        return GoogleSyncResult.fail("📄 Google PLC 服事表 同步失敗（已重試 3 次）：" + safeMsg(e));
    }

    // ================== 核心同步邏輯 ==================
    public GoogleSyncResult syncOneUpdate(LocalDate date, String positionName, String personName) throws Exception {


        // 設定目前模式 要使用哪一個 excel
        this.testMode = systemSettingService.getSettingValueAsBoolean(KEY_TEST_MODE, Boolean.TRUE);
        String spreadsheetId = this.testMode ? spreadsheetIdTest : spreadsheetIdProd;

        String sheetName = sheetNameByYear(date);
        String targetDate = SHEET_DATE_FMT.format(date);

        HeaderInfo header = findHeader(spreadsheetId, sheetName);

        int dateCol0 = header.colIndex.get(HDR_DATE);
        int audioCol0 = header.colIndex.get(HDR_AUDIO);
        int liveCol0 = header.colIndex.get(HDR_LIVE);
        int camCol0 = header.colIndex.get(HDR_CAMERA);

        int row1 = findUniqueRowByDate(spreadsheetId, sheetName, header.headerRow1 + 1, dateCol0, targetDate);

        if (row1 == -1) {
            // ✅ 規格：找不到 或 日期重複 -> 不處理 + log
            log.warn("Google Sheet skip: date not found or duplicated. sheet={}, targetDate={}", sheetName, targetDate);
            return GoogleSyncResult.fail("📄 Google PLC 服事表 ：找不到或日期重複，已略過");
        }

        String pos = normalizePosition(positionName);

        List<ValueRange> updates = new ArrayList<>();

        if (pos.equals("電腦") || pos.equals("混音") || pos.equals("燈光")) {
            // 音控欄位本身是 電腦/混音/燈光 的合併字串
            String oldAudio = readCell(spreadsheetId, sheetName + "!" + toA1Col(audioCol0) + row1);

            String merged = mergeAudio(oldAudio, pos.equals("電腦") ? personName : null, pos.equals("混音") ? personName : null, pos.equals("燈光") ? personName : null);

            updates.add(cellUpdate(sheetName, row1, audioCol0, merged));

        } else if (pos.equals("直播")) {
            updates.add(cellUpdate(sheetName, row1, liveCol0, personName));

        } else if (pos.equals("攝像")) {
            updates.add(cellUpdate(sheetName, row1, camCol0, personName));

        } else {
            return GoogleSyncResult.ok("📄 Google PLC 服事表 ：此崗位不需同步");
        }

        try {
            BatchUpdateValuesRequest req = new BatchUpdateValuesRequest().setValueInputOption("USER_ENTERED").setData(updates);

            sheets.spreadsheets().values().batchUpdate(spreadsheetId, req).execute();
            String msg = "📄 Google PLC 服事表 同步成功";
            if (this.testMode) {
                msg += "\n🧪 測試模式（未寫入正式服事表）";
            }
            return GoogleSyncResult.ok(msg);
        } catch (Exception e) {
            log.error("Google Sheet sync error (will retry). date={}, position={}, person={}", date, positionName, personName, e);
            throw e; // ✅ 交給 Spring Retry
        }
    }

    // ================== 工具方法 ==================
    private String sheetNameByYear(LocalDate date) {
        return date.getYear() + "年度服事表";
    }

    private HeaderInfo findHeader(String spreadsheetId, String sheetName) throws Exception {
        ValueRange vr = sheets.spreadsheets().values().get(spreadsheetId, sheetName + "!A1:AN30").setValueRenderOption("FORMATTED_VALUE").execute();

        if (vr.getValues() == null) {
            throw new IllegalStateException("找不到任何資料（可能工作表名稱不對）: " + sheetName);
        }

        for (int r = 0; r < vr.getValues().size(); r++) {
            Map<String, Integer> map = new HashMap<>();
            List<Object> row = vr.getValues().get(r);

            for (int c = 0; c < row.size(); c++) {
                String cell = String.valueOf(row.get(c)).trim();
                if (cell.equals(HDR_DATE)) map.put(HDR_DATE, c);
                if (cell.equals(HDR_AUDIO)) map.put(HDR_AUDIO, c);
                if (cell.equals(HDR_LIVE)) map.put(HDR_LIVE, c);
                if (cell.equals(HDR_CAMERA)) map.put(HDR_CAMERA, c);
            }

            if (map.size() == 4) return new HeaderInfo(r + 1, map);
        }
        throw new IllegalStateException("找不到表頭（日期 / 音控 / 直播 / 攝像），sheet=" + sheetName);
    }

    private int findUniqueRowByDate(String spreadsheetId, String sheet, int startRow1, int col0, String target) throws Exception {
        ValueRange vr = sheets.spreadsheets().values().get(spreadsheetId, sheet + "!" + toA1Col(col0) + startRow1 + ":" + toA1Col(col0)).setValueRenderOption("FORMATTED_VALUE").execute();

        if (vr.getValues() == null) return -1;

        List<Integer> hits = new ArrayList<>();
        for (int i = 0; i < vr.getValues().size(); i++) {
            if (!vr.getValues().get(i).isEmpty() && target.equals(String.valueOf(vr.getValues().get(i).get(0)).trim())) {
                hits.add(startRow1 + i);
            }
        }
        return hits.size() == 1 ? hits.get(0) : -1;
    }

    private String readCell(String spreadsheetId, String a1) throws Exception {
        ValueRange vr = sheets.spreadsheets().values().get(spreadsheetId, a1).setValueRenderOption("FORMATTED_VALUE").execute();
        return (vr.getValues() == null || vr.getValues().isEmpty() || vr.getValues().get(0).isEmpty()) ? "" : String.valueOf(vr.getValues().get(0).get(0));
    }

    private ValueRange cellUpdate(String sheet, int row1, int col0, String value) {
        return new ValueRange().setRange(sheet + "!" + toA1Col(col0) + row1).setValues(List.of(List.of(value == null ? "" : value)));
    }

    /**
     * 音控欄位格式：電腦/混音/燈光
     * 只要任一崗位更新，就更新音控這格，但要保留另外兩段原值
     */
    private String mergeAudio(String old, String c, String m, String l) {
        String[] p = Optional.ofNullable(old).orElse("").split("/", -1);
        String pc = p.length > 0 ? p[0] : "";
        String pm = p.length > 1 ? p[1] : "";
        String pl = p.length > 2 ? p[2] : "";

        if (c != null) pc = c;
        if (m != null) pm = m;
        if (l != null) pl = l;

        return String.join("/", pc, pm, pl);
    }

    private String normalizePosition(String p) {
        if (p == null) return "";
        String t = p.trim();
        if ("攝影".equals(t)) return "攝像"; // 你需求：攝影=攝像(M)
        return t;
    }

    private String toA1Col(int col) {
        StringBuilder sb = new StringBuilder();
        while (col >= 0) {
            sb.insert(0, (char) ('A' + col % 26));
            col = col / 26 - 1;
        }
        return sb.toString();
    }

    private String safeMsg(Throwable t) {
        String m = t.getMessage();
        return (m == null || m.isBlank()) ? t.getClass().getSimpleName() : m;
    }

    private static class HeaderInfo {
        final int headerRow1; // 1-based
        final Map<String, Integer> colIndex; // 0-based

        HeaderInfo(int r, Map<String, Integer> c) {
            this.headerRow1 = r;
            this.colIndex = c;
        }
    }
}
