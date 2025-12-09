package com.example.helloworld.controller.church;

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
    public ResponseEntity<Map<String, Object>> submitContactForm(@RequestBody ContactSubmission submission) {
        try {
            ContactSubmission created = contactSubmissionService.createSubmission(submission);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "感謝您的留言！我們會盡快與您聯繫。");
            response.put("data", created);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "提交失敗: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}
