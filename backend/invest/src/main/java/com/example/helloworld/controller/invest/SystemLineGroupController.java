package com.example.helloworld.controller.invest;

import com.example.helloworld.dto.common.ApiResponse;
import com.example.helloworld.entity.invest.LineGroup;
import com.example.helloworld.entity.invest.LineGroupMember;
import com.example.helloworld.service.invest.system.InvestSystemLineGroupService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/invest/system/line-groups")
@CrossOrigin(origins = "*")
public class SystemLineGroupController {

    private final InvestSystemLineGroupService investSystemLineGroupService;

    public SystemLineGroupController(InvestSystemLineGroupService investSystemLineGroupService) {
        this.investSystemLineGroupService = investSystemLineGroupService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<LineGroup>>> getAllGroups() {
        return ResponseEntity.ok(ApiResponse.ok(investSystemLineGroupService.getAllGroups()));
    }

    @GetMapping("/{groupId}")
    public ResponseEntity<ApiResponse<LineGroup>> getGroup(@PathVariable String groupId) {
        return investSystemLineGroupService.findGroup(groupId)
            .map(group -> ResponseEntity.ok(ApiResponse.ok(group)))
            .orElse(ResponseEntity.status(404).body(ApiResponse.fail("找不到指定群組")));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<LineGroup>> createGroup(@RequestBody LineGroup group) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(investSystemLineGroupService.createGroup(group)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
        }
    }

    @PutMapping("/{groupId}")
    public ResponseEntity<ApiResponse<LineGroup>> updateGroup(@PathVariable String groupId, @RequestBody LineGroup group) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(investSystemLineGroupService.updateGroup(groupId, group)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
        }
    }

    @DeleteMapping("/{groupId}")
    public ResponseEntity<ApiResponse<Void>> deleteGroup(@PathVariable String groupId) {
        try {
            investSystemLineGroupService.deleteGroup(groupId);
            return ResponseEntity.ok(ApiResponse.ok(null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
        }
    }

    @GetMapping("/{groupId}/members")
    public ResponseEntity<ApiResponse<List<LineGroupMember>>> getGroupMembers(@PathVariable String groupId) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(investSystemLineGroupService.getActiveMembers(groupId)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
        }
    }

    @PostMapping("/{groupId}/members")
    public ResponseEntity<ApiResponse<LineGroupMember>> addMember(
        @PathVariable String groupId,
        @RequestBody LineGroupMember member
    ) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(investSystemLineGroupService.addMember(groupId, member)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
        }
    }

    @PutMapping("/{groupId}/members/{memberId}")
    public ResponseEntity<ApiResponse<LineGroupMember>> updateMember(
        @PathVariable String groupId,
        @PathVariable Long memberId,
        @RequestBody LineGroupMember member
    ) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(investSystemLineGroupService.updateMember(groupId, memberId, member)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
        }
    }

    @DeleteMapping("/{groupId}/members/{memberId}")
    public ResponseEntity<ApiResponse<Void>> deleteMember(@PathVariable String groupId, @PathVariable Long memberId) {
        try {
            investSystemLineGroupService.deactivateMember(groupId, memberId);
            return ResponseEntity.ok(ApiResponse.ok(null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
        }
    }
}
