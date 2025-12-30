package com.example.helloworld.controller.church;

import com.example.helloworld.dto.church.frontend.AboutResult;
import com.example.helloworld.dto.church.ApiResponse;
import com.example.helloworld.dto.church.frontend.ActivityResult;
import com.example.helloworld.dto.church.frontend.SiteResult;
import com.example.helloworld.dto.church.frontend.SundayMessageResult;
import com.example.helloworld.entity.church.AboutInfo;
import com.example.helloworld.entity.church.Activity;
import com.example.helloworld.service.church.AboutInfoService;
import com.example.helloworld.service.church.ActivityService;
import com.example.helloworld.service.church.ChurchInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/church/public")
@CrossOrigin(origins = "*")
public class FrontendDataController {

    @Autowired
    private ChurchInfoService churchInfoService;

    @Autowired
    private AboutInfoService aboutInfoService;

    @Autowired
    private ActivityService activityService;

    @Autowired
    private com.example.helloworld.service.church.SundayMessageService sundayMessageService;

    /**
     * 獲取教會基本資訊（公開訪問）
     */
    @GetMapping("/church-info")
    public ResponseEntity<ApiResponse<SiteResult>> getSite() {
        try {
            Map<String, String> kv = churchInfoService.getAllActiveInfo();

            SiteResult result = new SiteResult();

            result.getContact().setAddress(kv.get("address"));
            result.getContact().setPhone(kv.get("phone"));
            result.getContact().setEmail(kv.get("email"));

            result.getOfficeHours().setWeekday(kv.get("service_hours_weekday"));
            result.getOfficeHours().setWeekend(kv.get("service_hours_weekend"));

            result.getHome().setWelcomeTitle(kv.get("home_welcome_title"));
            result.getHome().setWelcomeSubtitle(kv.get("home_welcome_subtitle"));

            result.getHome().getMainService().setTime(kv.get("home_main_service_time"));
            result.getHome().getMainService().setLocation(kv.get("home_main_service_location"));

            result.getHome().getSaturdayService().setTime(kv.get("home_saturday_service_time"));
            result.getHome().getSaturdayService().setLocation(kv.get("home_saturday_service_location"));

            return ResponseEntity.ok(ApiResponse.ok(result));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.fail("獲取站台資訊失敗: " + e.getMessage()));
        }
    }

    /**
     * 獲取活動資訊（公開訪問）
     */
    @GetMapping("/about-info")
    public ResponseEntity<ApiResponse<AboutResult>> getAbout() {
        try {
            List<AboutInfo> list = aboutInfoService.getAllActiveInfo();

            List<AboutResult.Section> sections = list.stream()
                    .map(x -> new AboutResult.Section(
                            x.getSectionKey(),
                            x.getTitle(),
                            x.getContent(),
                            x.getDisplayOrder()
                    ))
                    .toList();

            return ResponseEntity.ok(
                    ApiResponse.ok(new AboutResult(sections))
            );
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.fail("獲取關於我們失敗: " + e.getMessage()));
        }
    }
    /**
     * URL: /api/church/public/activities2
     */
    @GetMapping("/activities")
    public ResponseEntity<ApiResponse<List<ActivityResult>>> getActivities2() {
        try {
            List<Activity> activities = activityService.getAllActiveActivities();

            List<ActivityResult> results = activities.stream()
                    .map(a -> new ActivityResult(
                            a.getId(),
                            a.getTitle(),
                            a.getDescription(),
                            a.getLocation(),
                            a.getActivityDate(),
                            a.getImageUrl(),
                            a.getIsActive()
                    ))
                    .toList();

            return ResponseEntity.ok(ApiResponse.ok(results));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.fail("獲取活動資訊失敗: " + e.getMessage()));
        }
    }


    /**
     * 獲取主日信息（公開訪問）
     */
    @GetMapping("/sunday-messages")
    public ResponseEntity<ApiResponse<List<SundayMessageResult>>> getSundayMessages2() {
        try {
            List<com.example.helloworld.entity.church.SundayMessage> messages =
                    sundayMessageService.getAllActiveMessages();

            List<SundayMessageResult> results = messages.stream()
                    .map(m -> new SundayMessageResult(
                            m.getId(),
                            m.getTitle(),
                            m.getSpeaker(),
                            m.getScripture(),
                            m.getServiceDate(),
                            m.getImageUrl(),
                            m.getIsActive()
                    ))
                    .toList();

            return ResponseEntity.ok(ApiResponse.ok(results));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.fail("獲取主日信息失敗: " + e.getMessage()));
        }
    }
}

