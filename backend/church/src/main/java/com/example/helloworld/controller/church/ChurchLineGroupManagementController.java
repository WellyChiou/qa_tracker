package com.example.helloworld.controller.church;

import com.example.helloworld.dto.common.ApiResponse;
import com.example.helloworld.entity.church.LineGroup;
import com.example.helloworld.entity.church.LineGroupMember;
import com.example.helloworld.service.church.ChurchLineGroupService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/church/admin/line-groups")
@CrossOrigin(origins = "*")
public class ChurchLineGroupManagementController {
    private final ChurchLineGroupService churchLineGroupService;

    public ChurchLineGroupManagementController(ChurchLineGroupService churchLineGroupService) {
        this.churchLineGroupService = churchLineGroupService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<LineGroup>>> getAllGroups() {
        return ResponseEntity.ok(ApiResponse.ok(churchLineGroupService.getAllGroups()));
    }

    @GetMapping("/{groupId}")
    public ResponseEntity<ApiResponse<LineGroup>> getGroup(@PathVariable String groupId) {
        return churchLineGroupService.findGroup(groupId)
                .map(group -> ResponseEntity.ok(ApiResponse.ok(group)))
                .orElse(ResponseEntity.status(404).body(ApiResponse.fail("找不到指定群組")));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<LineGroup>> createGroup(@RequestBody LineGroup group) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(churchLineGroupService.createGroup(group)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
        }
    }

    @PutMapping("/{groupId}")
    public ResponseEntity<ApiResponse<LineGroup>> updateGroup(@PathVariable String groupId, @RequestBody LineGroup group) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(churchLineGroupService.updateGroup(groupId, group)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
        }
    }

    @DeleteMapping("/{groupId}")
    public ResponseEntity<ApiResponse<Void>> deleteGroup(@PathVariable String groupId) {
        try {
            churchLineGroupService.deleteGroup(groupId);
            return ResponseEntity.ok(ApiResponse.ok(null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
        }
    }

    @GetMapping("/{groupId}/members")
    public ResponseEntity<ApiResponse<List<LineGroupMember>>> getGroupMembers(@PathVariable String groupId) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(churchLineGroupService.getActiveMembers(groupId)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
        }
    }

    @PostMapping("/{groupId}/members")
    public ResponseEntity<ApiResponse<LineGroupMember>> addMember(@PathVariable String groupId, @RequestBody LineGroupMember member) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(churchLineGroupService.addMember(groupId, member)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
        }
    }

    @PutMapping("/{groupId}/members/{memberId}")
    public ResponseEntity<ApiResponse<LineGroupMember>> updateMember(
            @PathVariable String groupId,
            @PathVariable Long memberId,
            @RequestBody LineGroupMember member) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(churchLineGroupService.updateMember(groupId, memberId, member)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
        }
    }

    @DeleteMapping("/{groupId}/members/{memberId}")
    public ResponseEntity<ApiResponse<Void>> deleteMember(@PathVariable String groupId, @PathVariable Long memberId) {
        try {
            churchLineGroupService.deactivateMember(groupId, memberId);
            return ResponseEntity.ok(ApiResponse.ok(null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
        }
    }
}
