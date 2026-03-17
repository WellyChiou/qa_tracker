package com.example.helloworld.controller.church.checkin;

import com.example.helloworld.dto.common.ApiResponse;
import com.example.helloworld.repository.church.checkin.SessionRepository;
import com.example.helloworld.service.church.checkin.CheckinService;
import com.example.helloworld.service.church.checkin.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/church/checkin/public")
public class CheckinController {
    private final CheckinService checkinService;
    private final TokenService tokenService;
    private final SessionRepository sessionRepo;

    public CheckinController(CheckinService checkinService, TokenService tokenService, SessionRepository sessionRepo) {
        this.checkinService = checkinService;
        this.tokenService = tokenService;
        this.sessionRepo = sessionRepo;
    }

    @GetMapping("/sessions/{code}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSession(@PathVariable String code) {
        try {
            var s = sessionRepo.findBySessionCode(code)
                    .orElseThrow(() -> new RuntimeException("Session not found: " + code));
            Map<String, Object> result = new HashMap<>();
            result.put("title", s.getTitle() != null ? s.getTitle() : "");
            result.put("openAt", s.getOpenAt() != null ? s.getOpenAt().toString() : null);
            result.put("closeAt", s.getCloseAt() != null ? s.getCloseAt().toString() : null);
            return ResponseEntity.ok(ApiResponse.ok(result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("獲取場次失敗: " + e.getMessage()));
        }
    }

    @GetMapping("/sessions/{code}/token")
    public ResponseEntity<ApiResponse<Map<String, Object>>> token(@PathVariable String code) {
        try {
            var s = sessionRepo.findBySessionCode(code).orElseThrow();
            var t = tokenService.issue(s.getId());
            Map<String, Object> result = new HashMap<>();
            result.put("token", t.getToken());
            result.put("expiresAt", t.getExpiresAt().toString());
            return ResponseEntity.ok(ApiResponse.ok(result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("獲取 token 失敗: " + e.getMessage()));
        }
    }

    @PostMapping("/sessions/{code}/checkin")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkin(@PathVariable String code, @RequestBody Map<String, String> body, HttpServletRequest req) {
        try {
            String memberNo = body.getOrDefault("memberNo", "").trim().toUpperCase();
            String token = body.getOrDefault("token", "").trim().toUpperCase();
            String name = checkinService.publicCheckin(code, memberNo, token, req);
            Map<String, Object> result = new HashMap<>();
            result.put("status", "OK");
            result.put("name", name);
            return ResponseEntity.ok(ApiResponse.ok(result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("簽到失敗: " + e.getMessage()));
        }
    }
}

