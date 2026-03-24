package com.example.helloworld.controller.invest;

import com.example.helloworld.dto.common.ApiResponse;
import com.example.helloworld.dto.common.PageResponse;
import com.example.helloworld.dto.invest.SystemUserDto;
import com.example.helloworld.dto.invest.SystemUserEnabledRequestDto;
import com.example.helloworld.dto.invest.SystemUserResetPasswordRequestDto;
import com.example.helloworld.dto.invest.SystemUserUpsertRequestDto;
import com.example.helloworld.service.invest.system.InvestSystemUserService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/invest/system/users")
@CrossOrigin(origins = "*")
public class SystemUserController {

    private final InvestSystemUserService investSystemUserService;

    public SystemUserController(InvestSystemUserService investSystemUserService) {
        this.investSystemUserService = investSystemUserService;
    }

    @GetMapping("/paged")
    public ResponseEntity<ApiResponse<PageResponse<SystemUserDto>>> getPaged(
        @RequestParam(required = false) String username,
        @RequestParam(required = false) String email,
        @RequestParam(required = false) Boolean isEnabled,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        try {
            Page<SystemUserDto> result = investSystemUserService.getPaged(username, email, isEnabled, page, size);
            PageResponse<SystemUserDto> response = new PageResponse<>(
                result.getContent(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.getNumber(),
                result.getSize()
            );
            return ResponseEntity.ok(ApiResponse.ok(response));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("查詢用戶列表失敗：" + e.getMessage()));
        }
    }

    @GetMapping("/{uid}")
    public ResponseEntity<ApiResponse<SystemUserDto>> getDetail(@PathVariable String uid) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(investSystemUserService.getDetail(uid)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("查詢用戶明細失敗：" + e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SystemUserDto>> create(@RequestBody SystemUserUpsertRequestDto request) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(investSystemUserService.create(request)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("新增用戶失敗：" + e.getMessage()));
        }
    }

    @PutMapping("/{uid}")
    public ResponseEntity<ApiResponse<SystemUserDto>> update(
        @PathVariable String uid,
        @RequestBody SystemUserUpsertRequestDto request
    ) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(investSystemUserService.update(uid, request)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("更新用戶失敗：" + e.getMessage()));
        }
    }

    @PutMapping("/{uid}/enabled")
    public ResponseEntity<ApiResponse<SystemUserDto>> setEnabled(
        @PathVariable String uid,
        @RequestBody SystemUserEnabledRequestDto request
    ) {
        try {
            if (request == null || request.getEnabled() == null) {
                return ResponseEntity.badRequest().body(ApiResponse.fail("enabled 為必填"));
            }
            return ResponseEntity.ok(ApiResponse.ok(investSystemUserService.setEnabled(uid, request.getEnabled())));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("更新啟用狀態失敗：" + e.getMessage()));
        }
    }

    @PostMapping("/{uid}/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
        @PathVariable String uid,
        @RequestBody SystemUserResetPasswordRequestDto request
    ) {
        try {
            if (request == null) {
                return ResponseEntity.badRequest().body(ApiResponse.fail("request body 不可為空"));
            }
            investSystemUserService.resetPassword(uid, request.getNewPassword());
            return ResponseEntity.ok(ApiResponse.ok(null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("重設密碼失敗：" + e.getMessage()));
        }
    }
}
