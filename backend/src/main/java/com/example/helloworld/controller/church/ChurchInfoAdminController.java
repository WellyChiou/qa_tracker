package com.example.helloworld.controller.church;

import com.example.helloworld.dto.common.ApiResponse;
import com.example.helloworld.dto.common.PageResponse;
import com.example.helloworld.entity.church.ChurchInfo;
import com.example.helloworld.service.church.ChurchInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/church/admin/church-info")
@CrossOrigin(origins = "*")
public class ChurchInfoAdminController {

    @Autowired
    private ChurchInfoService churchInfoService;

    /**
     * 獲取所有教會資訊（管理用，包含未啟用的，支援分頁）
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ChurchInfo>>> getAllChurchInfo(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            // 獲取所有資訊（包含未啟用的）
            Pageable pageable = PageRequest.of(page, size, Sort.by("displayOrder").ascending());
            Page<ChurchInfo> infoPage = churchInfoService.getAllInfo(pageable);
            PageResponse<ChurchInfo> pageResponse = new PageResponse<>(
                infoPage.getContent(),
                infoPage.getTotalElements(),
                infoPage.getTotalPages(),
                infoPage.getNumber(),
                infoPage.getSize()
            );
            return ResponseEntity.ok(ApiResponse.ok(pageResponse));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("獲取教會資訊失敗: " + e.getMessage()));
        }
    }

    /**
     * 根據 key 獲取教會資訊
     */
    @GetMapping("/{infoKey}")
    public ResponseEntity<ApiResponse<ChurchInfo>> getChurchInfoByKey(@PathVariable String infoKey) {
        try {
            Optional<ChurchInfo> info = churchInfoService.getInfoByKey(infoKey);
            if (info.isPresent()) {
                return ResponseEntity.ok(ApiResponse.ok(info.get()));
            } else {
                return ResponseEntity.status(org.springframework.http.HttpStatus.NOT_FOUND).body(ApiResponse.fail("找不到指定的教會資訊"));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("獲取教會資訊失敗: " + e.getMessage()));
        }
    }

    /**
     * 創建或更新教會資訊
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ChurchInfo>> saveOrUpdateChurchInfo(@RequestBody ChurchInfo churchInfo) {
        try {
            ChurchInfo saved = churchInfoService.saveOrUpdateInfo(churchInfo);
            return ResponseEntity.ok(ApiResponse.ok(saved));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("儲存教會資訊失敗: " + e.getMessage()));
        }
    }

    /**
     * 批量更新教會資訊
     */
    @PutMapping("/batch")
    public ResponseEntity<ApiResponse<Void>> batchUpdateChurchInfo(@RequestBody List<ChurchInfo> churchInfoList) {
        try {
            for (ChurchInfo info : churchInfoList) {
                churchInfoService.saveOrUpdateInfo(info);
            }
            return ResponseEntity.ok(ApiResponse.ok(null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("批量更新失敗: " + e.getMessage()));
        }
    }
}
