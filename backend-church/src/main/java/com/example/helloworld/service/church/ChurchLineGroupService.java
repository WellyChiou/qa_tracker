package com.example.helloworld.service.church;

import com.example.helloworld.entity.church.LineGroup;
import com.example.helloworld.entity.church.LineGroupMember;
import com.example.helloworld.repository.church.LineGroupMemberRepository;
import com.example.helloworld.repository.church.LineGroupRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ChurchLineGroupService {
    private static final Logger log = LoggerFactory.getLogger(ChurchLineGroupService.class);

    private final LineGroupRepository lineGroupRepository;
    private final LineGroupMemberRepository lineGroupMemberRepository;

    public ChurchLineGroupService(
            LineGroupRepository lineGroupRepository,
            LineGroupMemberRepository lineGroupMemberRepository) {
        this.lineGroupRepository = lineGroupRepository;
        this.lineGroupMemberRepository = lineGroupMemberRepository;
    }

    @Transactional(transactionManager = "churchTransactionManager")
    public void handleGroupJoinEvent(String groupId) {
        if (groupId == null || groupId.trim().isEmpty()) {
            log.warn("⚠️ 群組 ID 為空，無法處理加入事件");
            return;
        }

        Optional<LineGroup> groupOpt = lineGroupRepository.findByGroupId(groupId);
        if (groupOpt.isPresent()) {
            LineGroup group = groupOpt.get();
            group.setIsActive(true);
            lineGroupRepository.save(group);
            return;
        }

        LineGroup newGroup = new LineGroup();
        newGroup.setGroupId(groupId);
        newGroup.setGroupName("未命名群組");
        newGroup.setIsActive(true);
        lineGroupRepository.save(newGroup);
    }

    @Transactional(transactionManager = "churchTransactionManager")
    public void handleGroupLeaveEvent(String groupId) {
        if (groupId == null || groupId.trim().isEmpty()) {
            log.warn("⚠️ 群組 ID 為空，無法處理離開事件");
            return;
        }

        lineGroupRepository.findByGroupId(groupId).ifPresent(group -> {
            group.setIsActive(false);
            lineGroupRepository.save(group);
        });
    }

    @Transactional(transactionManager = "churchTransactionManager")
    public LineGroup ensureActiveGroup(String groupId) {
        return lineGroupRepository.findByGroupId(groupId).orElseGet(() -> {
            LineGroup newGroup = new LineGroup();
            newGroup.setGroupId(groupId);
            newGroup.setGroupName("未命名群組");
            newGroup.setIsActive(true);
            newGroup.setMemberCount(1);
            return lineGroupRepository.save(newGroup);
        });
    }

    @Transactional(transactionManager = "churchTransactionManager")
    public void ensureActiveMember(LineGroup group, String userId) {
        if (userId == null || userId.isEmpty()) {
            return;
        }

        Optional<LineGroupMember> memberOpt = lineGroupMemberRepository.findByLineGroupAndUserId(group, userId);
        if (memberOpt.isEmpty()) {
            LineGroupMember newMember = new LineGroupMember();
            newMember.setLineGroup(group);
            newMember.setUserId(userId);
            newMember.setIsAdmin(false);
            newMember.setIsActive(true);
            newMember.setDisplayName("Line User");
            lineGroupMemberRepository.save(newMember);
            updateGroupMemberCount(group);
            return;
        }

        LineGroupMember member = memberOpt.get();
        if (!member.getIsActive()) {
            member.setIsActive(true);
        }
        lineGroupMemberRepository.save(member);
    }

    @Transactional(readOnly = true, transactionManager = "churchTransactionManager")
    public Optional<LineGroupMember> findMember(LineGroup group, String userId) {
        return lineGroupMemberRepository.findByLineGroupAndUserId(group, userId);
    }

    @Transactional(readOnly = true, transactionManager = "churchTransactionManager")
    public Optional<LineGroup> findGroup(String groupId) {
        return lineGroupRepository.findByGroupId(groupId);
    }

    @Transactional(readOnly = true, transactionManager = "churchTransactionManager")
    public List<LineGroup> getAllGroups() {
        return lineGroupRepository.findAll();
    }

    @Transactional(readOnly = true, transactionManager = "churchTransactionManager")
    public List<LineGroupMember> getActiveMembers(String groupId) {
        LineGroup group = lineGroupRepository.findByGroupId(groupId)
                .orElseThrow(() -> new RuntimeException("找不到指定群組: " + groupId));
        return lineGroupMemberRepository.findByLineGroupAndIsActiveTrue(group);
    }

    @Transactional(transactionManager = "churchTransactionManager")
    public LineGroup createGroup(LineGroup group) {
        if (group.getGroupId() == null || group.getGroupId().trim().isEmpty()) {
            throw new RuntimeException("群組 ID 不能為空");
        }
        if (lineGroupRepository.existsById(group.getGroupId())) {
            throw new RuntimeException("群組 ID 已存在: " + group.getGroupId());
        }

        if (group.getIsActive() == null) {
            group.setIsActive(true);
        }
        if (group.getMemberCount() == null) {
            group.setMemberCount(0);
        }

        return lineGroupRepository.save(group);
    }

    @Transactional(transactionManager = "churchTransactionManager")
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

    @Transactional(transactionManager = "churchTransactionManager")
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

    @Transactional(transactionManager = "churchTransactionManager")
    public LineGroupMember addMember(String groupId, LineGroupMember member) {
        LineGroup group = lineGroupRepository.findByGroupId(groupId)
                .orElseThrow(() -> new RuntimeException("找不到指定群組: " + groupId));

        Optional<LineGroupMember> existing = lineGroupMemberRepository.findByLineGroupAndUserId(group, member.getUserId());
        if (existing.isPresent()) {
            throw new RuntimeException("成員已存在於此群組: " + member.getUserId());
        }

        member.setLineGroup(group);
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

    @Transactional(transactionManager = "churchTransactionManager")
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

    @Transactional(transactionManager = "churchTransactionManager")
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

    @Transactional(readOnly = true, transactionManager = "churchTransactionManager")
    public String getGroupInfo(String groupId) {
        Optional<LineGroup> groupOpt = lineGroupRepository.findByGroupId(groupId);
        if (groupOpt.isPresent()) {
            LineGroup group = groupOpt.get();
            String groupName = group.getGroupName() != null && !group.getGroupName().trim().isEmpty()
                    ? group.getGroupName()
                    : "未命名群組";
            String status = group.getIsActive() ? "✅ 啟用" : "❌ 停用";
            return String.format("📋 群組資訊：\n\n群組 ID：\n%s\n\n群組名稱：%s\n\n狀態：%s", groupId, groupName, status);
        }

        return String.format("❓ 找不到群組資訊\n\n群組 ID：%s\n\n💡 提示：請確認 Bot 已經加入該群組，或該群組 ID 是否正確。", groupId);
    }

    @Transactional(readOnly = true, transactionManager = "churchTransactionManager")
    public boolean isGroupActive(String groupId) {
        return lineGroupRepository.findByGroupId(groupId)
                .map(LineGroup::getIsActive)
                .orElse(false);
    }

    private void updateGroupMemberCount(LineGroup group) {
        try {
            long count = lineGroupMemberRepository.countByLineGroupAndIsActiveTrue(group);
            group.setMemberCount((int) count);
            lineGroupRepository.save(group);
        } catch (Exception e) {
            log.error("❌ 更新群組成員計數失敗: {}", e.getMessage());
        }
    }
}
