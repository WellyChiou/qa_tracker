package com.example.helloworld.service.personal;

import com.example.helloworld.entity.personal.LineGroup;
import com.example.helloworld.entity.personal.LineGroupMember;
import com.example.helloworld.repository.personal.LineGroupMemberRepository;
import com.example.helloworld.repository.personal.LineGroupRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class PersonalLineGroupService {
    private static final Logger log = LoggerFactory.getLogger(PersonalLineGroupService.class);

    private final LineGroupRepository lineGroupRepository;
    private final LineGroupMemberRepository lineGroupMemberRepository;

    public PersonalLineGroupService(
            LineGroupRepository lineGroupRepository,
            LineGroupMemberRepository lineGroupMemberRepository) {
        this.lineGroupRepository = lineGroupRepository;
        this.lineGroupMemberRepository = lineGroupMemberRepository;
    }

    @Transactional(transactionManager = "primaryTransactionManager")
    public void handleGroupJoinEvent(String groupId) {
        if (groupId == null || groupId.trim().isEmpty()) {
            log.warn("⚠️ 群組 ID 為空，無法處理加入事件");
            return;
        }

        log.info("📥 處理群組加入事件，群組 ID: {}", groupId);

        Optional<LineGroup> groupOpt = lineGroupRepository.findByGroupId(groupId);
        if (groupOpt.isPresent()) {
            LineGroup group = groupOpt.get();
            group.setIsActive(true);
            lineGroupRepository.save(group);
            log.info("✅ 群組已存在，已重新啟用: {}", groupId);
            return;
        }

        LineGroup newGroup = new LineGroup();
        newGroup.setGroupId(groupId);
        newGroup.setGroupName("未命名群組");
        newGroup.setIsActive(true);
        lineGroupRepository.save(newGroup);
        log.info("✅ 已記錄新群組: {}", groupId);
    }

    @Transactional(transactionManager = "primaryTransactionManager")
    public void handleGroupLeaveEvent(String groupId) {
        if (groupId == null || groupId.trim().isEmpty()) {
            log.warn("⚠️ 群組 ID 為空，無法處理離開事件");
            return;
        }

        log.info("📤 處理群組離開事件，群組 ID: {}", groupId);

        Optional<LineGroup> groupOpt = lineGroupRepository.findByGroupId(groupId);
        if (groupOpt.isPresent()) {
            LineGroup group = groupOpt.get();
            group.setIsActive(false);
            lineGroupRepository.save(group);
            log.info("✅ 群組已停用: {}", groupId);
        } else {
            log.warn("⚠️ 群組不存在: {}", groupId);
        }
    }

    @Transactional(transactionManager = "primaryTransactionManager")
    public LineGroup ensureActiveGroup(String groupId) {
        Optional<LineGroup> groupOpt = lineGroupRepository.findByGroupId(groupId);
        if (groupOpt.isPresent()) {
            return groupOpt.get();
        }

        LineGroup newGroup = new LineGroup();
        newGroup.setGroupId(groupId);
        newGroup.setGroupName("未命名群組");
        newGroup.setIsActive(true);
        newGroup.setMemberCount(1);
        return lineGroupRepository.save(newGroup);
    }

    @Transactional(transactionManager = "primaryTransactionManager")
    public void ensureActiveMember(LineGroup group, String userId) {
        if (userId == null || userId.isEmpty()) {
            return;
        }

        Optional<LineGroupMember> memberOpt = lineGroupMemberRepository.findByLineGroupAndUserId(group, userId);
        if (memberOpt.isEmpty()) {
            log.info("👤 [群組訊息] 記錄新成員: {}", userId);
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
            log.info("👤 [群組訊息] 成員 {} 重新加入群組", userId);
            member.setIsActive(true);
        }
        lineGroupMemberRepository.save(member);
    }

    @Transactional(readOnly = true, transactionManager = "primaryTransactionManager")
    public Optional<LineGroupMember> findMember(LineGroup group, String userId) {
        return lineGroupMemberRepository.findByLineGroupAndUserId(group, userId);
    }

    @Transactional(readOnly = true, transactionManager = "primaryTransactionManager")
    public Optional<LineGroup> findGroup(String groupId) {
        return lineGroupRepository.findByGroupId(groupId);
    }

    @Transactional(readOnly = true, transactionManager = "primaryTransactionManager")
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

    @Transactional(readOnly = true, transactionManager = "primaryTransactionManager")
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
