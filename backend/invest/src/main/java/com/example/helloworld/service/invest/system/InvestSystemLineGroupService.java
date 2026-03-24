package com.example.helloworld.service.invest.system;

import com.example.helloworld.entity.invest.LineGroup;
import com.example.helloworld.entity.invest.LineGroupMember;
import com.example.helloworld.repository.invest.LineGroupMemberRepository;
import com.example.helloworld.repository.invest.LineGroupRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(transactionManager = "investTransactionManager")
public class InvestSystemLineGroupService {

    private static final Logger log = LoggerFactory.getLogger(InvestSystemLineGroupService.class);

    private final LineGroupRepository lineGroupRepository;
    private final LineGroupMemberRepository lineGroupMemberRepository;

    public InvestSystemLineGroupService(
        LineGroupRepository lineGroupRepository,
        LineGroupMemberRepository lineGroupMemberRepository
    ) {
        this.lineGroupRepository = lineGroupRepository;
        this.lineGroupMemberRepository = lineGroupMemberRepository;
    }

    @Transactional(transactionManager = "investTransactionManager", readOnly = true)
    public Optional<LineGroup> findGroup(String groupId) {
        return lineGroupRepository.findByGroupId(groupId);
    }

    @Transactional(transactionManager = "investTransactionManager", readOnly = true)
    public List<LineGroup> getAllGroups() {
        return lineGroupRepository.findAll();
    }

    @Transactional(transactionManager = "investTransactionManager", readOnly = true)
    public List<LineGroupMember> getActiveMembers(String groupId) {
        LineGroup group = lineGroupRepository.findByGroupId(groupId)
            .orElseThrow(() -> new RuntimeException("找不到指定群組: " + groupId));
        return lineGroupMemberRepository.findByLineGroupAndIsActiveTrue(group);
    }

    public LineGroup createGroup(LineGroup group) {
        if (group.getGroupId() == null || group.getGroupId().trim().isEmpty()) {
            throw new RuntimeException("群組 ID 不能為空");
        }

        String groupId = group.getGroupId().trim();
        if (lineGroupRepository.existsById(groupId)) {
            throw new RuntimeException("群組 ID 已存在: " + groupId);
        }

        group.setGroupId(groupId);
        if (group.getIsActive() == null) {
            group.setIsActive(true);
        }
        if (group.getMemberCount() == null) {
            group.setMemberCount(0);
        }

        return lineGroupRepository.save(group);
    }

    public LineGroup updateGroup(String groupId, LineGroup groupDetails) {
        LineGroup group = lineGroupRepository.findByGroupId(groupId)
            .orElseThrow(() -> new RuntimeException("找不到指定群組: " + groupId));

        group.setGroupName(groupDetails.getGroupName());
        group.setGroupCode(groupDetails.getGroupCode());
        if (groupDetails.getIsActive() != null) {
            group.setIsActive(groupDetails.getIsActive());
        }
        if (groupDetails.getMemberCount() != null) {
            group.setMemberCount(groupDetails.getMemberCount());
        }

        return lineGroupRepository.save(group);
    }

    public void deleteGroup(String groupId) {
        LineGroup group = lineGroupRepository.findByGroupId(groupId)
            .orElseThrow(() -> new RuntimeException("找不到指定群組: " + groupId));

        group.setIsActive(false);
        lineGroupRepository.save(group);

        List<LineGroupMember> members = lineGroupMemberRepository.findByLineGroup(group);
        for (LineGroupMember member : members) {
            member.setIsActive(false);
            lineGroupMemberRepository.save(member);
        }

        updateGroupMemberCount(group);
    }

    public LineGroupMember addMember(String groupId, LineGroupMember member) {
        LineGroup group = lineGroupRepository.findByGroupId(groupId)
            .orElseThrow(() -> new RuntimeException("找不到指定群組: " + groupId));

        if (member.getUserId() == null || member.getUserId().trim().isEmpty()) {
            throw new RuntimeException("LINE User ID 不能為空");
        }

        String userId = member.getUserId().trim();
        Optional<LineGroupMember> existing = lineGroupMemberRepository.findByLineGroupAndUserId(group, userId);
        if (existing.isPresent()) {
            throw new RuntimeException("成員已存在於此群組: " + userId);
        }

        member.setLineGroup(group);
        member.setUserId(userId);
        if (member.getIsAdmin() == null) {
            member.setIsAdmin(false);
        }
        if (member.getIsActive() == null) {
            member.setIsActive(true);
        }
        if (member.getDisplayName() == null || member.getDisplayName().trim().isEmpty()) {
            member.setDisplayName("Line User");
        }

        LineGroupMember saved = lineGroupMemberRepository.save(member);
        updateGroupMemberCount(group);
        return saved;
    }

    public LineGroupMember updateMember(String groupId, Long memberId, LineGroupMember memberDetails) {
        LineGroupMember member = lineGroupMemberRepository.findById(memberId)
            .orElseThrow(() -> new RuntimeException("找不到指定成員: " + memberId));

        if (!member.getLineGroup().getGroupId().equals(groupId)) {
            throw new RuntimeException("成員不屬於指定群組");
        }

        member.setDisplayName(memberDetails.getDisplayName());
        if (memberDetails.getIsAdmin() != null) {
            member.setIsAdmin(memberDetails.getIsAdmin());
        }
        if (memberDetails.getIsActive() != null) {
            member.setIsActive(memberDetails.getIsActive());
        }

        LineGroupMember updated = lineGroupMemberRepository.save(member);
        updateGroupMemberCount(member.getLineGroup());
        return updated;
    }

    public void deactivateMember(String groupId, Long memberId) {
        LineGroupMember member = lineGroupMemberRepository.findById(memberId)
            .orElseThrow(() -> new RuntimeException("找不到指定成員: " + memberId));

        if (!member.getLineGroup().getGroupId().equals(groupId)) {
            throw new RuntimeException("成員不屬於指定群組");
        }

        member.setIsActive(false);
        lineGroupMemberRepository.save(member);
        updateGroupMemberCount(member.getLineGroup());
    }

    private void updateGroupMemberCount(LineGroup group) {
        try {
            long count = lineGroupMemberRepository.countByLineGroupAndIsActiveTrue(group);
            group.setMemberCount((int) count);
            lineGroupRepository.save(group);
        } catch (Exception e) {
            log.error("更新群組成員數量失敗: {}", e.getMessage());
        }
    }
}
