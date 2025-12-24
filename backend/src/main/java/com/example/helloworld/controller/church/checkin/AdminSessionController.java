package com.example.helloworld.controller.church.checkin;

import com.example.helloworld.entity.church.checkin.Session;
import com.example.helloworld.repository.church.checkin.CheckinRepository;
import com.example.helloworld.repository.church.checkin.SessionRepository;
import com.example.helloworld.service.church.checkin.CheckinService;
import com.example.helloworld.service.church.checkin.CsvService;
import com.example.helloworld.service.church.checkin.ExcelService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/church/checkin/admin/sessions")
public class AdminSessionController {
    private final SessionRepository sessionRepo;
    private final CheckinRepository checkinRepo;
    private final CsvService csvService;
    private final ExcelService excelService;
    private final CheckinService checkinService;

    public AdminSessionController(SessionRepository sessionRepo, CheckinRepository checkinRepo, CsvService csvService, ExcelService excelService, CheckinService checkinService) {
        this.sessionRepo = sessionRepo;
        this.checkinRepo = checkinRepo;
        this.csvService = csvService;
        this.excelService = excelService;
        this.checkinService = checkinService;
    }

    // 獲取所有場次列表
    @GetMapping
    public List<Session> listAll() {
        return sessionRepo.findAll();
    }

    // 獲取單一場次
    @GetMapping("/{id}")
    public Session getById(@PathVariable Long id) {
        return sessionRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Session not found: " + id));
    }

    // 新增場次
    @PostMapping
    public Session create(@RequestBody Session session) {
        return sessionRepo.save(session);
    }

    // 更新場次
    @PutMapping("/{id}")
    public Session update(@PathVariable Long id, @RequestBody Session session) {
        Session existing = sessionRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Session not found: " + id));
        existing.setSessionType(session.getSessionType());
        existing.setTitle(session.getTitle());
        existing.setSessionDate(session.getSessionDate());
        existing.setOpenAt(session.getOpenAt());
        existing.setCloseAt(session.getCloseAt());
        existing.setStatus(session.getStatus());
        existing.setSessionCode(session.getSessionCode());
        return sessionRepo.save(existing);
    }

    // 刪除場次
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        if (!sessionRepo.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        sessionRepo.deleteById(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/today")
    public Object today() {
        return sessionRepo.findBySessionDate(LocalDate.now());
    }

    @GetMapping("/{id}/stats")
    public Map<String, Object> stats(@PathVariable Long id) {
        long checked = checkinRepo.countBySessionIdAndCanceledFalse(id);
        return Map.of("checked", checked);
    }

    @GetMapping("/{id}/checkins")
    public Object list(@PathVariable Long id, @RequestParam(defaultValue = "false") boolean includeCanceled) {
        return checkinRepo.findSessionRows(id, includeCanceled);
    }

    @GetMapping("/{id}/checkins/export.csv")
    public ResponseEntity<byte[]> exportCsv(@PathVariable Long id) {
        byte[] bytes = csvService.exportSessionCheckins(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"session-checkins-" + id + ".csv\"")
                .contentType(new MediaType("text", "csv"))
                .body(bytes);
    }

    @GetMapping("/{id}/checkins/export.xlsx")
    public ResponseEntity<byte[]> exportExcel(@PathVariable Long id) {
        try {
            byte[] bytes = excelService.exportSessionCheckins(id);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"session-checkins-" + id + ".xlsx\"")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(bytes);
        } catch (IOException e) {
            throw new RuntimeException("匯出 Excel 失敗", e);
        }
    }

    // 取消簽到記錄（軟取消，保留稽核）
    @PatchMapping("/{sessionId}/checkins/{checkinId}/cancel")
    public ResponseEntity<?> cancelCheckin(@PathVariable Long sessionId, @PathVariable Long checkinId, @RequestBody(required = false) Map<String, String> body) {
        String note = body == null ? null : body.getOrDefault("note", null);
        checkinService.cancelCheckin(checkinId, note);
        return ResponseEntity.ok().build();
    }

    // 刪除簽到記錄（硬刪除，永久刪除）
    @DeleteMapping("/{sessionId}/checkins/{checkinId}")
    public ResponseEntity<?> deleteCheckin(@PathVariable Long sessionId, @PathVariable Long checkinId) {
        checkinService.deleteCheckin(checkinId);
        return ResponseEntity.ok().build();
    }

    // 恢復簽到記錄（只對已取消的記錄）
    @PatchMapping("/{sessionId}/checkins/{checkinId}/restore")
    public ResponseEntity<?> restoreCheckin(@PathVariable Long sessionId, @PathVariable Long checkinId) {
        checkinService.restoreCheckin(checkinId);
        return ResponseEntity.ok().build();
    }

    // 獲取尚未簽到的人員列表
    @GetMapping("/{id}/unchecked-persons")
    public Object getUncheckedPersons(@PathVariable Long id) {
        return checkinRepo.findUncheckedPersons(id);
    }

    // 批量補登
    @PostMapping("/{id}/batch-checkin")
    public ResponseEntity<?> batchCheckin(@PathVariable Long id, @RequestBody List<Map<String, String>> requests) {
        checkinService.batchManualCheckin(id, requests);
        return ResponseEntity.ok().build();
    }
}

