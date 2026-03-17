package com.example.helloworld.service.church.checkin;

import com.example.helloworld.entity.church.Person;
import com.example.helloworld.entity.church.checkin.Checkin;
import com.example.helloworld.entity.church.checkin.Session;
import com.example.helloworld.exception.church.checkin.BizException;
import com.example.helloworld.repository.church.PersonRepository;
import com.example.helloworld.repository.church.checkin.CheckinRepository;
import com.example.helloworld.repository.church.checkin.SessionRepository;
import com.example.helloworld.util.church.CurrentUser;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class CheckinService {

    private final PersonRepository personRepo;
    private final SessionRepository sessionRepo;
    private final CheckinRepository checkinRepo;
    private final TokenService tokenService;

    public CheckinService(PersonRepository personRepo, SessionRepository sessionRepo, CheckinRepository checkinRepo, TokenService tokenService) {
        this.personRepo = personRepo;
        this.sessionRepo = sessionRepo;
        this.checkinRepo = checkinRepo;
        this.tokenService = tokenService;
    }

    @Transactional
    public String publicCheckin(String sessionCode, String memberNo, String token, HttpServletRequest req) {
        Session s = sessionRepo.findBySessionCode(sessionCode)
                .orElseThrow(() -> new BizException("SESSION_NOT_FOUND", "Session not found."));

        LocalDateTime now = LocalDateTime.now();
        if (s.getOpenAt() != null && now.isBefore(s.getOpenAt())) {
            throw new BizException("TIME_WINDOW_CLOSED", "Check-in not open yet.");
        }
        if (s.getCloseAt() != null && now.isAfter(s.getCloseAt())) {
            throw new BizException("TIME_WINDOW_CLOSED", "Check-in is closed.");
        }

        if (token == null || token.isBlank() || !tokenService.validate(s.getId(), token)) {
            throw new BizException("TOKEN_INVALID", "Token invalid or expired.");
        }

        Person p = personRepo.findByMemberNo(memberNo)
                .orElseThrow(() -> new BizException("MEMBER_NOT_FOUND", "Member not found."));

        // 檢查是否已存在簽到記錄
        Optional<Checkin> existingCheckin = checkinRepo.findBySessionIdAndMemberId(s.getId(), p.getId());
        
        Checkin c;
        if (existingCheckin.isPresent()) {
            c = existingCheckin.get();
            
            // 如果記錄已取消
            if (Boolean.TRUE.equals(c.getCanceled())) {
                // 如果是補登且已取消，自動恢復並改為自助簽到
                if (Boolean.TRUE.equals(c.getManual())) {
                    c.setCanceled(false);
                    c.setCanceledAt(null);
                    c.setCanceledBy(null);
                    c.setCanceledNote(null);
                    c.setCheckedInAt(now); // 更新簽到時間
                    c.setIp(req.getRemoteAddr());
                    c.setUserAgent(req.getHeader("User-Agent"));
                    c.setManual(false); // 改為自助簽到
                    c.setManualBy(null);
                    c.setManualNote(null);
                } else {
                    // 如果是自助簽到且已取消，恢復簽到
                    c.setCanceled(false);
                    c.setCanceledAt(null);
                    c.setCanceledBy(null);
                    c.setCanceledNote(null);
                    c.setCheckedInAt(now); // 更新簽到時間
                    c.setIp(req.getRemoteAddr());
                    c.setUserAgent(req.getHeader("User-Agent"));
                    c.setManual(false);
                }
            } else {
                // 如果記錄未取消
                if (Boolean.TRUE.equals(c.getManual())) {
                    // 如果是補登且未取消，提示已簽到(補登)
                    throw new BizException("ALREADY_CHECKED_IN_MANUAL", "已簽到(補登)");
                } else {
                    // 如果是自助簽到且未取消，提示已簽到
                    throw new BizException("ALREADY_CHECKED_IN", "Member has already checked in.");
                }
            }
        } else {
            // 如果不存在記錄，則創建新記錄
            c = new Checkin();
            c.setSessionId(s.getId());
            c.setMemberId(p.getId());
            c.setCheckedInAt(now);
            c.setIp(req.getRemoteAddr());
            c.setUserAgent(req.getHeader("User-Agent"));
            c.setManual(false);
            c.setCanceled(false);
        }

        checkinRepo.save(c);
        return p.getDisplayName() != null && !p.getDisplayName().isBlank() ? p.getDisplayName() : p.getPersonName();
    }

    @Transactional
    public void adminManualCheckin(Long sessionId, String memberNo, String manualNote) {
        Person p = personRepo.findByMemberNo(memberNo)
                .orElseThrow(() -> new BizException("MEMBER_NOT_FOUND", "Member not found."));

        String operator = CurrentUser.usernameOrSystem();

        // 檢查是否已存在簽到記錄
        Optional<Checkin> existingCheckin = checkinRepo.findBySessionIdAndMemberId(sessionId, p.getId());
        
        Checkin c;
        if (existingCheckin.isPresent()) {
            c = existingCheckin.get();
            
            // 如果記錄已取消，提示從簽到名單恢復
            if (Boolean.TRUE.equals(c.getCanceled())) {
                throw new BizException("CANCELED_EXISTS", "此會員的簽到記錄已存在但已被取消，請從簽到名單中恢復簽到狀態。");
            } else {
                // 如果記錄未取消，提示已簽到(補登)
                throw new BizException("ALREADY_CHECKED_IN_MANUAL", "已簽到(補登)");
            }
        } else {
            // 如果不存在記錄，則創建新記錄
            c = new Checkin();
            c.setSessionId(sessionId);
            c.setMemberId(p.getId());
            c.setCheckedInAt(LocalDateTime.now());
            c.setManual(true);
            c.setManualBy(operator);
            c.setManualNote(manualNote);
            c.setCanceled(false);
        }

        checkinRepo.save(c);
    }

    @Transactional
    public void cancelManualCheckin(Long checkinId, String note) {
        Checkin c = checkinRepo.findById(checkinId)
                .orElseThrow(() -> new BizException("CHECKIN_NOT_FOUND", "Check-in not found."));

        if (!Boolean.TRUE.equals(c.getManual())) {
            throw new BizException("NOT_MANUAL", "Only manual check-in can be canceled here.");
        }
        if (Boolean.TRUE.equals(c.getCanceled())) {
            return;
        }

        String operator = CurrentUser.usernameOrSystem();

        c.setCanceled(true);
        c.setCanceledAt(LocalDateTime.now());
        c.setCanceledBy(operator);
        c.setCanceledNote(note);
        checkinRepo.save(c);
    }

    @Transactional
    public void cancelCheckin(Long checkinId, String note) {
        Checkin c = checkinRepo.findById(checkinId)
                .orElseThrow(() -> new BizException("CHECKIN_NOT_FOUND", "Check-in not found."));

        if (Boolean.TRUE.equals(c.getCanceled())) {
            return; // 已經取消，直接返回
        }

        String operator = CurrentUser.usernameOrSystem();

        c.setCanceled(true);
        c.setCanceledAt(LocalDateTime.now());
        c.setCanceledBy(operator);
        c.setCanceledNote(note);
        checkinRepo.save(c);
    }

    @Transactional
    public void restoreCheckin(Long checkinId) {
        Checkin c = checkinRepo.findById(checkinId)
                .orElseThrow(() -> new BizException("CHECKIN_NOT_FOUND", "Check-in not found."));

        if (!Boolean.TRUE.equals(c.getCanceled())) {
            throw new BizException("NOT_CANCELED", "Check-in is not canceled, cannot restore.");
        }

        c.setCanceled(false);
        c.setCanceledAt(null);
        c.setCanceledBy(null);
        c.setCanceledNote(null);
        checkinRepo.save(c);
    }

    @Transactional
    public void deleteCheckin(Long checkinId) {
        Checkin c = checkinRepo.findById(checkinId)
                .orElseThrow(() -> new BizException("CHECKIN_NOT_FOUND", "Check-in not found."));
        
        checkinRepo.delete(c);
    }

    @Transactional
    public void batchManualCheckin(Long sessionId, List<Map<String, String>> requests) {
        String operator = CurrentUser.usernameOrSystem();
        List<String> errors = new java.util.ArrayList<>();
        int successCount = 0;
        
        for (Map<String, String> req : requests) {
            String memberNo = req.getOrDefault("memberNo", "").trim().toUpperCase();
            String note = req.getOrDefault("note", "").trim();
            
            if (memberNo.isBlank()) {
                continue; // 跳過空的會員編號
            }
            
            try {
                adminManualCheckin(sessionId, memberNo, note);
                successCount++;
            } catch (BizException e) {
                // 收集錯誤信息
                String errorMsg = memberNo;
                if ("CANCELED_EXISTS".equals(e.getCode())) {
                    errorMsg += ": 此會員的簽到記錄已存在但已被取消，請從簽到名單中恢復簽到狀態";
                } else if ("ALREADY_CHECKED_IN_MANUAL".equals(e.getCode())) {
                    errorMsg += ": 已簽到(補登)";
                } else if ("MEMBER_NOT_FOUND".equals(e.getCode())) {
                    errorMsg += ": 查無此會員編號";
                } else {
                    errorMsg += ": " + e.getMessage();
                }
                errors.add(errorMsg);
            }
        }
        
        // 如果有錯誤，拋出包含所有錯誤信息的異常
        if (!errors.isEmpty()) {
            String errorMessage = String.format(
                "批量補登完成：成功 %d 筆，失敗 %d 筆。失敗詳情：%s",
                successCount,
                errors.size(),
                String.join("; ", errors)
            );
            throw new BizException("BATCH_CHECKIN_PARTIAL_FAILURE", errorMessage);
        }
    }
}

