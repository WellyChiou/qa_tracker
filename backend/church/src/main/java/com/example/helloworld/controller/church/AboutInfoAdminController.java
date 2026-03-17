package com.example.helloworld.controller.church;

import com.example.helloworld.dto.common.ApiResponse;
import com.example.helloworld.dto.common.PageResponse;
import com.example.helloworld.entity.church.AboutInfo;
import com.example.helloworld.service.church.AboutInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/church/admin/about-info")
@CrossOrigin(origins = "*")
public class AboutInfoAdminController {

    @Autowired
    private AboutInfoService aboutInfoService;

    /**
     * 獲取所有關於我們資訊（管理用，包含未啟用的，支持分頁和過濾）
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AboutInfo>>> getAllAboutInfo(
            @RequestParam(required = false) String sectionKey,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            Page<AboutInfo> infoPage = aboutInfoService.getAllInfo(sectionKey, title, isActive, page, size);
            PageResponse<AboutInfo> pageResponse = new PageResponse<>(
                    infoPage.getContent(),
                    infoPage.getTotalElements(),
                    infoPage.getTotalPages(),
                    infoPage.getNumber(),
                    infoPage.getSize()
            );
            return ResponseEntity.ok(ApiResponse.ok(pageResponse));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.fail("獲取關於我們資訊失敗: " + e.getMessage()));
        }
    }

    /**
     * 根據 ID 獲取關於我們資訊
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AboutInfo>> getAboutInfoById(@PathVariable Long id) {
        try {
            AboutInfo info = aboutInfoService.getInfoById(id);
            return ResponseEntity.ok(ApiResponse.ok(info));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.fail(e.getMessage()));
        }
    }

    /**
     * 創建關於我們資訊
     */
    @PostMapping
    public ResponseEntity<ApiResponse<AboutInfo>> createAboutInfo(@RequestBody AboutInfo aboutInfo) {
        try {
            AboutInfo created = aboutInfoService.createInfo(aboutInfo);
            return ResponseEntity.ok(ApiResponse.ok(created));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.fail("建立關於我們資訊失敗: " + e.getMessage()));
        }
    }

    /**
     * 更新關於我們資訊
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<AboutInfo>> updateAboutInfo(@PathVariable Long id, @RequestBody AboutInfo aboutInfo) {
        try {
            AboutInfo updated = aboutInfoService.updateInfo(id, aboutInfo);
            return ResponseEntity.ok(ApiResponse.ok(updated));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.fail(e.getMessage()));
        }
    }

    /**
     * 刪除關於我們資訊
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deleteAboutInfo(@PathVariable Long id) {
        try {
            aboutInfoService.deleteInfo(id);
            return ResponseEntity.ok(ApiResponse.ok("關於我們資訊已刪除"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.fail("刪除關於我們資訊失敗: " + e.getMessage()));
        }
    }
}
