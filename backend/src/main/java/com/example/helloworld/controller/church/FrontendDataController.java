package com.example.helloworld.controller.church;

import com.example.helloworld.dto.church.frontend.AboutResult;
import com.example.helloworld.dto.common.ApiResponse;
import com.example.helloworld.dto.church.frontend.ActivityResult;
import com.example.helloworld.dto.church.frontend.AnnouncementResult;
import com.example.helloworld.dto.church.frontend.GroupResult;
import com.example.helloworld.dto.church.frontend.PrayerRequestResult;
import com.example.helloworld.dto.church.frontend.SiteResult;
import com.example.helloworld.dto.church.frontend.SundayMessageResult;
import com.example.helloworld.entity.church.AboutInfo;
import com.example.helloworld.entity.church.Activity;
import com.example.helloworld.entity.church.Announcement;
import com.example.helloworld.entity.church.Group;
import com.example.helloworld.entity.church.GroupPerson;
import com.example.helloworld.entity.church.Person;
import com.example.helloworld.entity.church.PrayerRequest;
import com.example.helloworld.service.church.AboutInfoService;
import com.example.helloworld.service.church.ActivityService;
import com.example.helloworld.service.church.AnnouncementService;
import com.example.helloworld.service.church.ChurchInfoService;
import com.example.helloworld.service.church.GroupService;
import com.example.helloworld.service.church.PrayerRequestService;
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

    @Autowired
    private GroupService groupService;

    @Autowired
    private PrayerRequestService prayerRequestService;

    @Autowired
    private AnnouncementService announcementService;

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
                            a.getStartTime(),         // 活動開始時間
                            a.getEndTime(),           // 活動結束時間
                            a.getActivitySessions(),  // 活動時間段（多個上課時間）
                            a.getTags(),
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

    /**
     * 獲取所有啟用的小組列表（公開訪問）
     */
    @GetMapping("/groups")
    public ResponseEntity<ApiResponse<List<GroupResult>>> getGroups() {
        try {
            List<Group> groups = groupService.getActiveGroups();

            List<GroupResult> results = groups.stream()
                    .map(group -> {
                        List<GroupPerson> groupPersons = groupService.getGroupMembersWithRole(group.getId());
                        // 只返回小組長和實習小組長
                        List<GroupResult.MemberInfo> memberInfos = groupPersons.stream()
                                .filter(gp -> {
                                    String role = gp.getRole() != null ? gp.getRole() : "MEMBER";
                                    return role.equals("LEADER") || role.equals("ASSISTANT_LEADER");
                                })
                                .map(gp -> new GroupResult.MemberInfo(
                                        gp.getPerson().getId(),
                                        gp.getPerson().getPersonName(),
                                        gp.getPerson().getDisplayName(),
                                        gp.getPerson().getMemberNo(),
                                        gp.getRole() != null ? gp.getRole() : "MEMBER"
                                ))
                                .toList();

                        return new GroupResult(
                                group.getId(),
                                group.getGroupName(),
                                group.getDescription(),
                                group.getMeetingFrequency(),
                                group.getCategory(),
                                group.getMeetingLocation(),
                                group.getIsActive(),
                                memberInfos
                        );
                    })
                    .toList();

            return ResponseEntity.ok(ApiResponse.ok(results));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.fail("獲取小組列表失敗: " + e.getMessage()));
        }
    }

    /**
     * 獲取單一小組詳情（公開訪問）
     */
    @GetMapping("/groups/{id}")
    public ResponseEntity<ApiResponse<GroupResult>> getGroupById(@PathVariable Long id) {
        try {
            Group group = groupService.getGroupById(id)
                    .orElseThrow(() -> new RuntimeException("小組不存在: " + id));

            List<GroupPerson> groupPersons = groupService.getGroupMembersWithRole(id);
            // 只返回小組長和實習小組長
            List<GroupResult.MemberInfo> memberInfos = groupPersons.stream()
                    .filter(gp -> {
                        String role = gp.getRole() != null ? gp.getRole() : "MEMBER";
                        return role.equals("LEADER") || role.equals("ASSISTANT_LEADER");
                    })
                    .map(gp -> new GroupResult.MemberInfo(
                            gp.getPerson().getId(),
                            gp.getPerson().getPersonName(),
                            gp.getPerson().getDisplayName(),
                            gp.getPerson().getMemberNo(),
                            gp.getRole() != null ? gp.getRole() : "MEMBER"
                    ))
                    .toList();

            GroupResult result = new GroupResult(
                    group.getId(),
                    group.getGroupName(),
                    group.getDescription(),
                    group.getMeetingFrequency(),
                    group.getCategory(),
                    group.getMeetingLocation(),
                    group.getIsActive(),
                    memberInfos
            );

            return ResponseEntity.ok(ApiResponse.ok(result));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.fail("獲取小組詳情失敗: " + e.getMessage()));
        }
    }

    /**
     * 獲取代禱事項列表（公開訪問）
     */
    @GetMapping("/prayer-requests")
    public ResponseEntity<ApiResponse<List<PrayerRequestResult>>> getPrayerRequests() {
        try {
            List<PrayerRequest> prayerRequests = prayerRequestService.getAllActivePrayerRequests();

            List<PrayerRequestResult> results = prayerRequests.stream()
                    .map(pr -> new PrayerRequestResult(
                            pr.getId(),
                            pr.getTitle(),
                            pr.getContent(),
                            pr.getCategory(),
                            pr.getIsUrgent(),
                            pr.getIsActive(),
                            pr.getCreatedAt()
                    ))
                    .toList();

            return ResponseEntity.ok(ApiResponse.ok(results));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.fail("獲取代禱事項失敗: " + e.getMessage()));
        }
    }

    /**
     * 獲取公告列表（公開訪問）
     */
    @GetMapping("/announcements")
    public ResponseEntity<ApiResponse<List<AnnouncementResult>>> getAnnouncements() {
        try {
            List<Announcement> announcements = announcementService.getValidAnnouncements();

            List<AnnouncementResult> results = announcements.stream()
                    .map(a -> new AnnouncementResult(
                            a.getId(),
                            a.getTitle(),
                            a.getContent(),
                            a.getCategory(),
                            a.getPublishDate(),
                            a.getExpireDate(),
                            a.getIsPinned(),
                            a.getIsActive(),
                            a.getCreatedAt()
                    ))
                    .toList();

            return ResponseEntity.ok(ApiResponse.ok(results));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.fail("獲取公告失敗: " + e.getMessage()));
        }
    }
}

