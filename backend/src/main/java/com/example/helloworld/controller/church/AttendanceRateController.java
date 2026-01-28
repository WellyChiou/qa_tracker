package com.example.helloworld.controller.church;

import com.example.helloworld.dto.common.ApiResponse;
import com.example.helloworld.dto.common.PageResponse;
import com.example.helloworld.dto.church.checkin.AttendanceRateDto;
import com.example.helloworld.dto.church.checkin.SessionDetailResult;
import com.example.helloworld.service.church.AttendanceRateService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/church/attendance")
@CrossOrigin(origins = "*")
public class AttendanceRateController {
    private final AttendanceRateService attendanceRateService;

    public AttendanceRateController(AttendanceRateService attendanceRateService) {
        this.attendanceRateService = attendanceRateService;
    }

    /**
     * 計算指定人員在指定年度各類別的出席率
     */
    @GetMapping("/person/{personId}")
    public ResponseEntity<ApiResponse<PageResponse<AttendanceRateDto>>> getPersonAttendance(
            @PathVariable Long personId,
            @RequestParam Integer year,
            @RequestParam(required = false, defaultValue = "false") Boolean includeHistorical,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            List<AttendanceRateDto> allResults = attendanceRateService.calculateAttendanceByCategory(personId, year, includeHistorical);
            PageResponse<AttendanceRateDto> pageResponse = buildPagedResponse(allResults, page, size);
            return ResponseEntity.ok(ApiResponse.ok(pageResponse));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("獲取出席率失敗：" + e.getMessage()));
        }
    }

    /**
     * 計算指定小組所有成員在指定年度的出席率
     */
    @GetMapping("/group/{groupId}")
    public ResponseEntity<ApiResponse<PageResponse<AttendanceRateDto>>> getGroupAttendance(
            @PathVariable Long groupId,
            @RequestParam Integer year,
            @RequestParam(required = false, defaultValue = "false") Boolean includeHistorical,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            List<AttendanceRateDto> allResults = attendanceRateService.calculateGroupAttendance(groupId, year, includeHistorical);
            PageResponse<AttendanceRateDto> pageResponse = buildPagedResponse(allResults, page, size);
            return ResponseEntity.ok(ApiResponse.ok(pageResponse));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("獲取小組出席率失敗：" + e.getMessage()));
        }
    }

    /**
     * 計算所有人員在指定年度各類別的出席率
     */
    @GetMapping("/all")
    public ResponseEntity<ApiResponse<PageResponse<AttendanceRateDto>>> getAllAttendance(
            @RequestParam Integer year,
            @RequestParam(required = false) String category,
            @RequestParam(required = false, defaultValue = "false") Boolean includeHistorical,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            List<AttendanceRateDto> allResults = attendanceRateService.calculateAllPersonsAttendance(year, category, includeHistorical);
            PageResponse<AttendanceRateDto> pageResponse = buildPagedResponse(allResults, page, size);
            return ResponseEntity.ok(ApiResponse.ok(pageResponse));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("獲取所有出席率失敗：" + e.getMessage()));
        }
    }

    /**
     * 獲取指定人員在指定年度某個類別下的所有場次詳細資訊
     */
    @GetMapping("/person/{personId}/sessions")
    public ResponseEntity<ApiResponse<List<SessionDetailResult>>> getPersonSessionDetails(
            @PathVariable Long personId,
            @RequestParam String category,
            @RequestParam Integer year,
            @RequestParam(required = false, defaultValue = "false") Boolean includeHistorical) {
        try {
            List<SessionDetailResult> results = attendanceRateService.getSessionDetails(personId, category, year, includeHistorical);
            return ResponseEntity.ok(ApiResponse.ok(results));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("獲取場次詳細資訊失敗：" + e.getMessage()));
        }
    }

    /**
     * 構建分頁響應
     */
    private PageResponse<AttendanceRateDto> buildPagedResponse(List<AttendanceRateDto> allResults, int page, int size) {
        int totalElements = allResults.size();
        int totalPages = (int) Math.ceil((double) totalElements / size);
        int start = page * size;
        int end = Math.min(start + size, totalElements);
        
        List<AttendanceRateDto> content = (start < totalElements) 
            ? allResults.subList(start, end) 
            : new ArrayList<>();
        
        return new PageResponse<>(content, totalElements, totalPages, page, size);
    }
}

