package com.example.helloworld.controller.church;

import com.example.helloworld.dto.common.ApiResponse;
import com.example.helloworld.entity.church.ContactSubmission;
import com.example.helloworld.service.church.ContactSubmissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/church/public/contact-submissions")
@CrossOrigin(origins = "*")
public class ContactSubmissionController {

    @Autowired
    private ContactSubmissionService contactSubmissionService;

    /**
     * 提交聯絡表單（公開訪問）
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ContactSubmission>> submitContactForm(@RequestBody ContactSubmission submission) {
        try {
            ContactSubmission created = contactSubmissionService.createSubmission(submission);
            return ResponseEntity.ok(ApiResponse.ok(created));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("提交失敗: " + e.getMessage()));
        }
    }
}
