package com.example.helloworld.controller.church.checkin;

import com.example.helloworld.dto.common.ApiResponse;
import com.example.helloworld.dto.common.PageResponse;
import com.example.helloworld.entity.church.Group;
import com.example.helloworld.entity.church.checkin.Session;
import com.example.helloworld.entity.church.checkin.SessionGroup;
import com.example.helloworld.repository.church.GroupRepository;
import com.example.helloworld.repository.church.checkin.CheckinRepository;
import com.example.helloworld.repository.church.checkin.SessionRepository;
import com.example.helloworld.repository.church.checkin.SessionGroupRepository;
import com.example.helloworld.service.church.checkin.CheckinService;
import com.example.helloworld.service.church.checkin.CsvService;
import com.example.helloworld.service.church.checkin.ExcelService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/church/checkin/admin/sessions")
public class AdminSessionController {
    private final SessionRepository sessionRepo;
    private final CheckinRepository checkinRepo;
    private final CsvService csvService;
    private final ExcelService excelService;
    private final CheckinService checkinService;
    private final SessionGroupRepository sessionGroupRepository;
    private final GroupRepository groupRepository;

    public AdminSessionController(SessionRepository sessionRepo, CheckinRepository checkinRepo, CsvService csvService, ExcelService excelService, CheckinService checkinService, SessionGroupRepository sessionGroupRepository, GroupRepository groupRepository) {
        this.sessionRepo = sessionRepo;
        this.checkinRepo = checkinRepo;
        this.csvService = csvService;
        this.excelService = excelService;
        this.checkinService = checkinService;
        this.sessionGroupRepository = sessionGroupRepository;
        this.groupRepository = groupRepository;
    }

    // 獲取所有場次列表（支援篩選和分頁）
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<Session>>> listAll(
            @RequestParam(required = false) String sessionCode,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String sessionType,
            @RequestParam(required = false) Long groupId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<Session> allSessions = sessionRepo.findAll();
        
        // 後端篩選
        List<Session> filtered = new ArrayList<>();
        for (Session session : allSessions) {
            // 場次代碼篩選
            if (sessionCode != null && !sessionCode.isBlank()) {
                if (session.getSessionCode() == null || 
                    !session.getSessionCode().toLowerCase().contains(sessionCode.toLowerCase())) {
                    continue;
                }
            }
            
            // 標題篩選
            if (title != null && !title.isBlank()) {
                if (session.getTitle() == null || 
                    !session.getTitle().toLowerCase().contains(title.toLowerCase())) {
                    continue;
                }
            }
            
            // 狀態篩選
            if (status != null && !status.isBlank()) {
                if (!status.equals(session.getStatus())) {
                    continue;
                }
            }
            
            // 類型篩選
            if (sessionType != null && !sessionType.isBlank()) {
                if (!sessionType.equals(session.getSessionType())) {
                    continue;
                }
            }
            
            // 小組篩選（只對小組類型場次有效）
            if (groupId != null && "WEEKDAY".equals(session.getSessionType())) {
                List<SessionGroup> sessionGroups = sessionGroupRepository.findBySessionId(session.getId());
                boolean found = false;
                for (SessionGroup sg : sessionGroups) {
                    if (sg.getGroupId().equals(groupId)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    continue;
                }
            }
            
            // 開始日期篩選（基於 sessionDate）
            if (startDate != null && !startDate.isBlank()) {
                LocalDate start = LocalDate.parse(startDate);
                if (session.getSessionDate() == null || session.getSessionDate().isBefore(start)) {
                    continue;
                }
            }
            
            // 結束日期篩選（基於 sessionDate）
            if (endDate != null && !endDate.isBlank()) {
                LocalDate end = LocalDate.parse(endDate);
                if (session.getSessionDate() == null || session.getSessionDate().isAfter(end)) {
                    continue;
                }
            }
            
            filtered.add(session);
        }
        
        // 分頁處理
        int totalElements = filtered.size();
        int start = page * size;
        int end = Math.min(start + size, totalElements);
        List<Session> content = (start < totalElements) 
            ? filtered.subList(start, end) 
            : new ArrayList<>();
        
        Page<Session> sessionPage = new PageImpl<>(content, PageRequest.of(page, size), totalElements);
        
        PageResponse<Session> pageResponse = new PageResponse<>(
            content,
            totalElements,
            sessionPage.getTotalPages(),
            page,
            size
        );
        
        return ResponseEntity.ok(ApiResponse.ok(pageResponse));
    }

    // 獲取單一場次
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Session>> getById(@PathVariable Long id) {
        return sessionRepo.findById(id)
                .map(session -> ResponseEntity.ok(ApiResponse.ok(session)))
                .orElse(ResponseEntity.status(org.springframework.http.HttpStatus.NOT_FOUND).body(ApiResponse.fail("場次不存在")));
    }

    // 新增場次
    @PostMapping
    public ResponseEntity<ApiResponse<Session>> create(@RequestBody Session session) {
        try {
            Session created = sessionRepo.save(session);
            return ResponseEntity.ok(ApiResponse.ok(created));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("創建場次失敗: " + e.getMessage()));
        }
    }

    // 更新場次
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Session>> update(@PathVariable Long id, @RequestBody Session session) {
        try {
            Session existing = sessionRepo.findById(id)
                    .orElseThrow(() -> new RuntimeException("Session not found: " + id));
            existing.setSessionType(session.getSessionType());
            existing.setTitle(session.getTitle());
            existing.setSessionDate(session.getSessionDate());
            existing.setOpenAt(session.getOpenAt());
            existing.setCloseAt(session.getCloseAt());
            existing.setStatus(session.getStatus());
            existing.setSessionCode(session.getSessionCode());
            Session updated = sessionRepo.save(existing);
            return ResponseEntity.ok(ApiResponse.ok(updated));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("更新場次失敗: " + e.getMessage()));
        }
    }

    // 刪除場次
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        try {
            if (!sessionRepo.existsById(id)) {
                return ResponseEntity.status(org.springframework.http.HttpStatus.NOT_FOUND).body(ApiResponse.fail("場次不存在"));
            }
            sessionRepo.deleteById(id);
            return ResponseEntity.ok(ApiResponse.ok(null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("刪除場次失敗: " + e.getMessage()));
        }
    }

    @GetMapping("/today")
    public ResponseEntity<ApiResponse<List<Session>>> today() {
        try {
            List<Session> sessions = sessionRepo.findBySessionDate(LocalDate.now());
            return ResponseEntity.ok(ApiResponse.ok(sessions));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("獲取今天的場次失敗: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> stats(@PathVariable Long id) {
        try {
            long checked = checkinRepo.countBySessionIdAndCanceledFalse(id);
            Map<String, Object> stats = Map.of("checked", checked);
            return ResponseEntity.ok(ApiResponse.ok(stats));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("獲取統計資訊失敗: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}/checkins")
    public ResponseEntity<ApiResponse<List<?>>> list(@PathVariable Long id, @RequestParam(defaultValue = "false") boolean includeCanceled) {
        try {
            List<?> checkins = checkinRepo.findSessionRows(id, includeCanceled);
            return ResponseEntity.ok(ApiResponse.ok(checkins));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("獲取簽到列表失敗: " + e.getMessage()));
        }
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
    public ResponseEntity<ApiResponse<Void>> cancelCheckin(@PathVariable Long sessionId, @PathVariable Long checkinId, @RequestBody(required = false) Map<String, String> body) {
        try {
            String note = body == null ? null : body.getOrDefault("note", null);
            checkinService.cancelCheckin(checkinId, note);
            return ResponseEntity.ok(ApiResponse.ok(null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("取消簽到失敗: " + e.getMessage()));
        }
    }

    // 刪除簽到記錄（硬刪除，永久刪除）
    @DeleteMapping("/{sessionId}/checkins/{checkinId}")
    public ResponseEntity<ApiResponse<Void>> deleteCheckin(@PathVariable Long sessionId, @PathVariable Long checkinId) {
        try {
            checkinService.deleteCheckin(checkinId);
            return ResponseEntity.ok(ApiResponse.ok(null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("刪除簽到失敗: " + e.getMessage()));
        }
    }

    // 恢復簽到記錄（只對已取消的記錄）
    @PatchMapping("/{sessionId}/checkins/{checkinId}/restore")
    public ResponseEntity<ApiResponse<Void>> restoreCheckin(@PathVariable Long sessionId, @PathVariable Long checkinId) {
        try {
            checkinService.restoreCheckin(checkinId);
            return ResponseEntity.ok(ApiResponse.ok(null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("恢復簽到失敗: " + e.getMessage()));
        }
    }

    // 獲取尚未簽到的人員列表
    @GetMapping("/{id}/unchecked-persons")
    public ResponseEntity<ApiResponse<List<?>>> getUncheckedPersons(@PathVariable Long id) {
        try {
            List<?> persons = checkinRepo.findUncheckedPersons(id);
            return ResponseEntity.ok(ApiResponse.ok(persons));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("獲取未簽到人員列表失敗: " + e.getMessage()));
        }
    }

    // 批量補登
    @PostMapping("/{id}/batch-checkin")
    public ResponseEntity<ApiResponse<Void>> batchCheckin(@PathVariable Long id, @RequestBody List<Map<String, String>> requests) {
        try {
            checkinService.batchManualCheckin(id, requests);
            return ResponseEntity.ok(ApiResponse.ok(null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("批量補登失敗: " + e.getMessage()));
        }
    }

    // 獲取場次關聯的小組
    @GetMapping("/{id}/groups")
    @Transactional(transactionManager = "churchTransactionManager", readOnly = true)
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSessionGroups(@PathVariable Long id) {
        try {
            Session session = sessionRepo.findById(id)
                    .orElseThrow(() -> new RuntimeException("場次不存在：" + id));
            
            List<SessionGroup> sessionGroups = sessionGroupRepository.findBySessionId(id);
            // 使用 Set 來去重，避免重複的小組
            Set<Long> uniqueGroupIds = new HashSet<>();
            List<Map<String, Object>> groups = new ArrayList<>();
            
            for (SessionGroup sg : sessionGroups) {
                // 使用 groupId 直接欄位，避免懶加載問題
                Long groupId = sg.getGroupId();
                
                // 如果已經處理過這個 groupId，跳過
                if (uniqueGroupIds.contains(groupId)) {
                    continue;
                }
                uniqueGroupIds.add(groupId);
                
                Group group = groupRepository.findById(groupId)
                        .orElseThrow(() -> new RuntimeException("小組不存在：" + groupId));
                
                Map<String, Object> groupInfo = new HashMap<>();
                groupInfo.put("id", group.getId());
                groupInfo.put("groupName", group.getGroupName());
                groupInfo.put("description", group.getDescription());
                groups.add(groupInfo);
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("groups", groups);
            return ResponseEntity.ok(ApiResponse.ok(result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("獲取場次關聯的小組失敗：" + e.getMessage()));
        }
    }

    // 更新場次關聯的小組（批量）
    @PutMapping("/{id}/groups")
    @Transactional(transactionManager = "churchTransactionManager")
    public ResponseEntity<ApiResponse<Void>> updateSessionGroups(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        try {
            Session session = sessionRepo.findById(id)
                    .orElseThrow(() -> new RuntimeException("場次不存在：" + id));
            
            @SuppressWarnings("unchecked")
            List<Object> groupIdsRaw = (List<Object>) request.get("groupIds");
            Set<Long> groupIdsSet = new HashSet<>(); // 使用 Set 去重
            
            if (groupIdsRaw != null) {
                // 安全地將 Integer 或 Long 轉換為 Long，並去重
                for (Object item : groupIdsRaw) {
                    Long groupId;
                    if (item instanceof Long) {
                        groupId = (Long) item;
                    } else if (item instanceof Integer) {
                        groupId = ((Integer) item).longValue();
                    } else if (item instanceof Number) {
                        groupId = ((Number) item).longValue();
                    } else {
                        groupId = Long.parseLong(item.toString());
                    }
                    groupIdsSet.add(groupId);
                }
            }
            
            // 刪除現有的關聯
            sessionGroupRepository.deleteBySessionId(id);
            sessionGroupRepository.flush(); // 確保立即執行刪除
            
            // 創建新的關聯（去重後）
            for (Long groupId : groupIdsSet) {
                if (!groupRepository.existsById(groupId)) {
                    throw new RuntimeException("小組不存在：" + groupId);
                }
                
                SessionGroup sessionGroup = new SessionGroup();
                sessionGroup.setSessionId(id);
                sessionGroup.setGroupId(groupId);
                sessionGroupRepository.save(sessionGroup);
            }
            
            return ResponseEntity.ok(ApiResponse.ok(null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("更新場次關聯的小組失敗：" + e.getMessage()));
        }
    }
}

