package com.example.helloworld.controller.invest;

import com.example.helloworld.dto.common.ApiResponse;
import com.example.helloworld.dto.invest.SystemLineTestPushRequestDto;
import com.example.helloworld.service.invest.system.InvestLineTestService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/invest/line/test")
@CrossOrigin(origins = "*")
public class InvestLineTestController {

    private final InvestLineTestService investLineTestService;

    public InvestLineTestController(InvestLineTestService investLineTestService) {
        this.investLineTestService = investLineTestService;
    }

    @PostMapping("/group-push")
    public ResponseEntity<ApiResponse<Map<String, Object>>> sendGroupPush(
        @RequestBody SystemLineTestPushRequestDto request
    ) {
        try {
            if (request == null) {
                return ResponseEntity.badRequest().body(ApiResponse.fail("request body 不可為空"));
            }
            Map<String, Object> result = investLineTestService.sendGroupPush(request.getGroupId(), request.getMessage());
            return ResponseEntity.ok(ApiResponse.ok(result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("LINE 測試訊息發送失敗：" + e.getMessage()));
        }
    }
}
