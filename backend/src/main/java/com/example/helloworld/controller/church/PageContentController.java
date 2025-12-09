package com.example.helloworld.controller.church;

import com.example.helloworld.entity.church.PageContent;
import com.example.helloworld.service.church.PageContentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/church/pages")
@CrossOrigin(origins = "*")
public class PageContentController {

    @Autowired
    private PageContentService pageContentService;

    /**
     * 獲取頁面內容（公開訪問）
     */
    @GetMapping("/{pageCode}")
    public ResponseEntity<Map<String, Object>> getPageContent(@PathVariable String pageCode) {
        Optional<PageContent> pageContent = pageContentService.getPageContentByCode(pageCode);
        if (pageContent.isPresent()) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", pageContent.get());
            return ResponseEntity.ok(response);
        } else {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "頁面內容不存在或未啟用");
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 獲取所有頁面內容（管理用，需要管理權限）
     */
    @GetMapping
    @PreAuthorize("hasAuthority('PERM_CHURCH_ADMIN')")
    public ResponseEntity<Map<String, Object>> getAllPageContents() {
        try {
            List<PageContent> pageContents = pageContentService.getAllPageContents();
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", pageContents);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "獲取頁面內容失敗: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 根據 ID 獲取頁面內容（管理用）
     */
    @GetMapping("/admin/{id}")
    @PreAuthorize("hasAuthority('PERM_CHURCH_ADMIN')")
    public ResponseEntity<Map<String, Object>> getPageContentById(@PathVariable Long id) {
        Optional<PageContent> pageContent = pageContentService.getPageContentById(id);
        if (pageContent.isPresent()) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", pageContent.get());
            return ResponseEntity.ok(response);
        } else {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "頁面內容不存在");
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 創建頁面內容（管理用）
     */
    @PostMapping
    @PreAuthorize("hasAuthority('PERM_CHURCH_ADMIN')")
    public ResponseEntity<Map<String, Object>> createPageContent(@RequestBody PageContent pageContent) {
        try {
            PageContent created = pageContentService.createPageContent(pageContent);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "頁面內容創建成功");
            response.put("data", created);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "創建失敗: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 更新頁面內容（管理用）
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_CHURCH_ADMIN')")
    public ResponseEntity<Map<String, Object>> updatePageContent(@PathVariable Long id, @RequestBody PageContent pageContent) {
        try {
            PageContent updated = pageContentService.updatePageContent(id, pageContent);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "頁面內容更新成功");
            response.put("data", updated);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "更新失敗: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 刪除頁面內容（管理用）
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_CHURCH_ADMIN')")
    public ResponseEntity<Map<String, Object>> deletePageContent(@PathVariable Long id) {
        try {
            pageContentService.deletePageContent(id);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "頁面內容刪除成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "刪除失敗: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}

