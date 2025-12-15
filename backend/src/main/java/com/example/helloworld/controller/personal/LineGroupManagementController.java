package com.example.helloworld.controller.personal;

import com.example.helloworld.entity.personal.LineGroup;
import com.example.helloworld.entity.personal.LineGroupMember;
import com.example.helloworld.repository.personal.LineGroupRepository;
import com.example.helloworld.repository.personal.LineGroupMemberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    public ResponseEntity<List<LineGroup>> getAllGroups() {
        return ResponseEntity.ok(lineGroupRepository.findAll());
    }

    @GetMapping("/{groupId}")
    public ResponseEntity<LineGroup> getGroup(@PathVariable String groupId) {
        return lineGroupRepository.findByGroupId(groupId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> createGroup(@RequestBody LineGroup group) {
        if (lineGroupRepository.existsById(group.getGroupId())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Group ID already exists"));
        }
        // Ensure defaults
        if (group.getIsActive() == null) group.setIsActive(true);
        if (group.getMemberCount() == null) group.setMemberCount(0);
        
        LineGroup saved = lineGroupRepository.save(group);
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/{groupId}")
    public ResponseEntity<?> updateGroup(@PathVariable String groupId, @RequestBody LineGroup groupDetails) {
        return lineGroupRepository.findByGroupId(groupId).map(group -> {
            group.setGroupName(groupDetails.getGroupName());
            group.setIsActive(groupDetails.getIsActive());
            // memberCount is usually updated by system, but allow admin override if needed
            if (groupDetails.getMemberCount() != null) {
                group.setMemberCount(groupDetails.getMemberCount());
            }
            LineGroup updated = lineGroupRepository.save(group);
            return ResponseEntity.ok(updated);
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{groupId}")
    public ResponseEntity<?> deleteGroup(@PathVariable String groupId) {
        return lineGroupRepository.findByGroupId(groupId).map(group -> {
            lineGroupRepository.delete(group);
            return ResponseEntity.ok(Map.of("message", "Group deleted"));
        }).orElse(ResponseEntity.notFound().build());
    }

    // --- Line Group Member Management ---

    @GetMapping("/{groupId}/members")
    public ResponseEntity<List<LineGroupMember>> getGroupMembers(@PathVariable String groupId) {
        Optional<LineGroup> groupOpt = lineGroupRepository.findByGroupId(groupId);
        if (groupOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(lineGroupMemberRepository.findByLineGroup(groupOpt.get()));
    }

    @PostMapping("/{groupId}/members")
    public ResponseEntity<?> addMember(@PathVariable String groupId, @RequestBody LineGroupMember member) {
        Optional<LineGroup> groupOpt = lineGroupRepository.findByGroupId(groupId);
        if (groupOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        LineGroup group = groupOpt.get();
        
        // Check if member already exists in group
        Optional<LineGroupMember> existing = lineGroupMemberRepository.findByLineGroupAndUserId(group, member.getUserId());
        if (existing.isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Member already exists in this group"));
        }

        member.setLineGroup(group);
        if (member.getIsAdmin() == null) member.setIsAdmin(false);
        if (member.getDisplayName() == null) member.setDisplayName("Unknown");
        
        LineGroupMember saved = lineGroupMemberRepository.save(member);
        
        // Update member count
        updateGroupMemberCount(group);
        
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/{groupId}/members/{memberId}")
    public ResponseEntity<?> updateMember(@PathVariable String groupId, @PathVariable Long memberId, @RequestBody LineGroupMember memberDetails) {
        return lineGroupMemberRepository.findById(memberId).map(member -> {
            if (!member.getLineGroup().getGroupId().equals(groupId)) {
                return ResponseEntity.badRequest().body(Map.of("message", "Member does not belong to the specified group"));
            }
            
            member.setDisplayName(memberDetails.getDisplayName());
            member.setIsAdmin(memberDetails.getIsAdmin());
            // userId usually shouldn't change for an existing record, or handle carefully
            
            LineGroupMember updated = lineGroupMemberRepository.save(member);
            return ResponseEntity.ok(updated);
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{groupId}/members/{memberId}")
    public ResponseEntity<?> deleteMember(@PathVariable String groupId, @PathVariable Long memberId) {
        return lineGroupMemberRepository.findById(memberId).map(member -> {
            if (!member.getLineGroup().getGroupId().equals(groupId)) {
                return ResponseEntity.badRequest().body(Map.of("message", "Member does not belong to the specified group"));
            }
            
            LineGroup group = member.getLineGroup();
            lineGroupMemberRepository.delete(member);
            
            // Update member count
            updateGroupMemberCount(group);
            
            return ResponseEntity.ok(Map.of("message", "Member removed"));
        }).orElse(ResponseEntity.notFound().build());
    }
    
    private void updateGroupMemberCount(LineGroup group) {
        long count = lineGroupMemberRepository.countByLineGroup(group);
        group.setMemberCount((int) count);
        lineGroupRepository.save(group);
    }
}

