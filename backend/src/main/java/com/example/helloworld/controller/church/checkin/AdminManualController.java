package com.example.helloworld.controller.church.checkin;

import com.example.helloworld.dto.common.ApiResponse;
import com.example.helloworld.repository.church.checkin.CheckinRepository;
import com.example.helloworld.service.church.checkin.CheckinService;
import com.example.helloworld.service.church.checkin.CsvService;
import com.example.helloworld.service.church.checkin.ExcelService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/church/checkin/admin/manual-checkins")
public class AdminManualController {

    private final CheckinRepository checkinRepo;
    private final CheckinService checkinService;
    private final CsvService csvService;
    private final ExcelService excelService;

    public AdminManualController(CheckinRepository checkinRepo, CheckinService checkinService, CsvService csvService, ExcelService excelService) {
        this.checkinRepo = checkinRepo;
        this.checkinService = checkinService;
        this.csvService = csvService;
        this.excelService = excelService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<?>>> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(defaultValue = "false") boolean includeCanceled
    ) {
        try {
            List<?> rows = checkinRepo.findManualRows(q, parseStart(from), parseEnd(to), includeCanceled);
            return ResponseEntity.ok(ApiResponse.ok(rows));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("獲取補登記錄失敗: " + e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> create(@RequestBody Map<String, String> body) {
        try {
            Long sessionId = Long.valueOf(body.get("sessionId"));
            String memberNo = body.getOrDefault("memberNo", "").trim().toUpperCase();
            String note = body.getOrDefault("note", "").trim();
            checkinService.adminManualCheckin(sessionId, memberNo, note);
            return ResponseEntity.ok(ApiResponse.ok(null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("補登失敗: " + e.getMessage()));
        }
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<Void>> cancel(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        try {
            String note = body == null ? null : body.getOrDefault("note", null);
            checkinService.cancelManualCheckin(id, note);
            return ResponseEntity.ok(ApiResponse.ok(null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("取消補登失敗: " + e.getMessage()));
        }
    }

    @GetMapping("/export.csv")
    public ResponseEntity<byte[]> exportCsv(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(defaultValue = "false") boolean includeCanceled
    ) {
        byte[] bytes = csvService.exportManualCheckins(q, parseStart(from), parseEnd(to), includeCanceled);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"manual-checkins.csv\"")
                .contentType(new MediaType("text", "csv"))
                .body(bytes);
    }

    @GetMapping("/export.xlsx")
    public ResponseEntity<byte[]> exportExcel(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(defaultValue = "false") boolean includeCanceled
    ) {
        try {
            byte[] bytes = excelService.exportManualCheckins(q, parseStart(from), parseEnd(to), includeCanceled);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"manual-checkins.xlsx\"")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(bytes);
        } catch (IOException e) {
            throw new RuntimeException("匯出 Excel 失敗", e);
        }
    }

    private LocalDateTime parseStart(String s) {
        if (s == null || s.isBlank()) return null;
        LocalDate d = LocalDate.parse(s, DateTimeFormatter.ISO_LOCAL_DATE);
        return d.atStartOfDay();
    }

    private LocalDateTime parseEnd(String s) {
        if (s == null || s.isBlank()) return null;
        LocalDate d = LocalDate.parse(s, DateTimeFormatter.ISO_LOCAL_DATE);
        return d.atTime(23, 59, 59);
    }
}

