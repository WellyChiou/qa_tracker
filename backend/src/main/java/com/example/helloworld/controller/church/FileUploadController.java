package com.example.helloworld.controller.church;

import com.example.helloworld.dto.common.ApiResponse;
import com.example.helloworld.service.church.FileUploadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/church/admin/upload")
@CrossOrigin(origins = "*")
public class FileUploadController {

    @Autowired
    private FileUploadService fileUploadService;

    /**
     * 上傳圖片
     * 
     * @param file 上傳的文件
     * @param type 文件類型：activities（活動）或 sunday-messages（主日信息）
     * @return 上傳結果，包含圖片的 URL
     */
    @PostMapping("/image")
    public ResponseEntity<ApiResponse<Map<String, Object>>> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam("type") String type) {
        try {
            String imageUrl = fileUploadService.uploadImage(file, type);
            Map<String, Object> result = new HashMap<>();
            result.put("url", imageUrl);
            return ResponseEntity.ok(ApiResponse.ok(result));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.fail("圖片上傳失敗: " + e.getMessage()));
        }
    }

    /**
     * 刪除圖片
     * 
     * @param url 圖片的 URL
     * @return 刪除結果
     */
    @DeleteMapping("/image")
    public ResponseEntity<ApiResponse<Void>> deleteImage(@RequestParam("url") String url) {
        try {
            boolean deleted = fileUploadService.deleteImage(url);
            if (deleted) {
                return ResponseEntity.ok(ApiResponse.ok(null));
            } else {
                return ResponseEntity.badRequest().body(ApiResponse.fail("圖片不存在或無法刪除"));
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.fail("圖片刪除失敗: " + e.getMessage()));
        }
    }
}

