package com.example.helloworld.controller.personal;

import com.example.helloworld.dto.common.ApiResponse;
import com.example.helloworld.entity.personal.LineGroup;
import com.example.helloworld.entity.personal.LineGroupMember;
import com.example.helloworld.repository.personal.LineGroupRepository;
import com.example.helloworld.repository.personal.LineGroupMemberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/personal/line-groups")
public class LineGroupManagementController {

    @Autowired
    private LineGroupRepository lineGroupRepository;

    @Autowired
    private LineGroupMemberRepository lineGroupMemberRepository;

    // --- Line Group Management ---

    @GetMapping
    public ResponseEntity<ApiResponse<List<LineGroup>>> getAllGroups() {
        return ResponseEntity.ok(ApiResponse.ok(lineGroupRepository.findAll()));
    }

    @GetMapping("/{groupId}")
    public ResponseEntity<ApiResponse<LineGroup>> getGroup(@PathVariable String groupId) {
        return lineGroupRepository.findByGroupId(groupId)
                .map(group -> ResponseEntity.ok(ApiResponse.ok(group)))
                .orElse(ResponseEntity.status(404).body(ApiResponse.<LineGroup>fail("找不到指定群組")));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<LineGroup>> createGroup(@RequestBody LineGroup group) {
        if (lineGroupRepository.existsById(group.getGroupId())) {
            return ResponseEntity.badRequest().body(ApiResponse.<LineGroup>fail("Group ID already exists"));
        }
        // Ensure defaults
        if (group.getIsActive() == null) group.setIsActive(true);
        if (group.getMemberCount() == null) group.setMemberCount(0);
        
        LineGroup saved = lineGroupRepository.save(group);
        return ResponseEntity.ok(ApiResponse.ok(saved));
    }

    @PutMapping("/{groupId}")
    public ResponseEntity<ApiResponse<LineGroup>> updateGroup(@PathVariable String groupId, @RequestBody LineGroup groupDetails) {
        return lineGroupRepository.findByGroupId(groupId).map(group -> {
            group.setGroupName(groupDetails.getGroupName());
            group.setIsActive(groupDetails.getIsActive());
            group.setGroupCode(groupDetails.getGroupCode());
            // memberCount is usually updated by system, but allow admin override if needed
            if (groupDetails.getMemberCount() != null) {
                group.setMemberCount(groupDetails.getMemberCount());
            }
            LineGroup updated = lineGroupRepository.save(group);
            return ResponseEntity.ok(ApiResponse.ok(updated));
        }).orElse(ResponseEntity.status(404).body(ApiResponse.<LineGroup>fail("找不到指定群組")));
    }

    @DeleteMapping("/{groupId}")
    public ResponseEntity<ApiResponse<Void>> deleteGroup(@PathVariable String groupId) {
        return lineGroupRepository.findByGroupId(groupId).map(group -> {
            lineGroupRepository.delete(group);
            return ResponseEntity.ok(ApiResponse.<Void>ok(null));
        }).orElse(ResponseEntity.status(404).body(ApiResponse.<Void>fail("找不到指定群組")));
    }

    // --- Line Group Member Management ---

    @GetMapping("/{groupId}/members")
    public ResponseEntity<ApiResponse<List<LineGroupMember>>> getGroupMembers(@PathVariable String groupId) {
        Optional<LineGroup> groupOpt = lineGroupRepository.findByGroupId(groupId);
        if (groupOpt.isEmpty()) {
            return ResponseEntity.status(404).body(ApiResponse.<List<LineGroupMember>>fail("找不到指定群組"));
        }
        // 只返回啟用的成員
        return ResponseEntity.ok(ApiResponse.ok(lineGroupMemberRepository.findByLineGroupAndIsActiveTrue(groupOpt.get())));
    }

    @PostMapping("/{groupId}/members")
    public ResponseEntity<ApiResponse<LineGroupMember>> addMember(@PathVariable String groupId, @RequestBody LineGroupMember member) {
        Optional<LineGroup> groupOpt = lineGroupRepository.findByGroupId(groupId);
        if (groupOpt.isEmpty()) {
            return ResponseEntity.status(404).body(ApiResponse.<LineGroupMember>fail("找不到指定群組"));
        }
        
        LineGroup group = groupOpt.get();
        
        // Check if member already exists in group
        Optional<LineGroupMember> existing = lineGroupMemberRepository.findByLineGroupAndUserId(group, member.getUserId());
        if (existing.isPresent()) {
            return ResponseEntity.badRequest().body(ApiResponse.<LineGroupMember>fail("Member already exists in this group"));
        }

        member.setLineGroup(group);
        if (member.getIsAdmin() == null) member.setIsAdmin(false);
        if (member.getDisplayName() == null) member.setDisplayName("Unknown");
        
        LineGroupMember saved = lineGroupMemberRepository.save(member);
        
        // Update member count
        updateGroupMemberCount(group);
        
        return ResponseEntity.ok(ApiResponse.ok(saved));
    }

    @PutMapping("/{groupId}/members/{memberId}")
    public ResponseEntity<ApiResponse<LineGroupMember>> updateMember(@PathVariable String groupId, @PathVariable Long memberId, @RequestBody LineGroupMember memberDetails) {
        return lineGroupMemberRepository.findById(memberId).map(member -> {
            if (!member.getLineGroup().getGroupId().equals(groupId)) {
                return ResponseEntity.badRequest().body(ApiResponse.<LineGroupMember>fail("Member does not belong to the specified group"));
            }
            
            member.setDisplayName(memberDetails.getDisplayName());
            member.setIsAdmin(memberDetails.getIsAdmin());
            // userId usually shouldn't change for an existing record, or handle carefully
            
            LineGroupMember updated = lineGroupMemberRepository.save(member);
            return ResponseEntity.ok(ApiResponse.ok(updated));
        }).orElse(ResponseEntity.status(404).body(ApiResponse.<LineGroupMember>fail("找不到指定成員")));
    }

    @DeleteMapping("/{groupId}/members/{memberId}")
    public ResponseEntity<ApiResponse<Void>> deleteMember(@PathVariable String groupId, @PathVariable Long memberId) {
        return lineGroupMemberRepository.findById(memberId).map(member -> {
            if (!member.getLineGroup().getGroupId().equals(groupId)) {
                return ResponseEntity.badRequest().body(ApiResponse.<Void>fail("Member does not belong to the specified group"));
            }
            
            LineGroup group = member.getLineGroup();
            // 軟刪除：將成員標記為未啟用，而不是直接刪除記錄
            member.setIsActive(false);
            lineGroupMemberRepository.save(member);
            
            // Update member count (只計算啟用的成員)
            updateGroupMemberCount(group);
            
            return ResponseEntity.ok(ApiResponse.<Void>ok(null));
        }).orElse(ResponseEntity.status(404).body(ApiResponse.<Void>fail("找不到指定成員")));
    }
    
    private void updateGroupMemberCount(LineGroup group) {
        // 只計算啟用的成員
        long count = lineGroupMemberRepository.countByLineGroupAndIsActiveTrue(group);
        group.setMemberCount((int) count);
        lineGroupRepository.save(group);
    }
}
