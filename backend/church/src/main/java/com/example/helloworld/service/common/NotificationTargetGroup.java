package com.example.helloworld.service.common;

public class NotificationTargetGroup {
    private final String groupId;
    private final String groupName;

    public NotificationTargetGroup(String groupId, String groupName) {
        this.groupId = groupId;
        this.groupName = groupName;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getGroupName() {
        return groupName;
    }
}
