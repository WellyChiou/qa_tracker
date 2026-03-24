package com.example.helloworld.dto.invest;

public class SystemLineTestPushRequestDto {
    private String groupId;
    private String message;

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
