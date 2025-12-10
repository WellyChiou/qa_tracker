package com.example.helloworld.controller.church;

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
    public ResponseEntity<Map<String, Object>> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam("type") String type) {
        Map<String, Object> response = new HashMap<>();

        try {
            String imageUrl = fileUploadService.uploadImage(file, type);
            response.put("success", true);
            response.put("url", imageUrl);
            response.put("message", "圖片上傳成功");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "圖片上傳失敗: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 刪除圖片
     * 
     * @param url 圖片的 URL
     * @return 刪除結果
     */
    @DeleteMapping("/image")
    public ResponseEntity<Map<String, Object>> deleteImage(@RequestParam("url") String url) {
        Map<String, Object> response = new HashMap<>();

        try {
            boolean deleted = fileUploadService.deleteImage(url);
            if (deleted) {
                response.put("success", true);
                response.put("message", "圖片刪除成功");
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "圖片不存在或無法刪除");
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "圖片刪除失敗: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
}

