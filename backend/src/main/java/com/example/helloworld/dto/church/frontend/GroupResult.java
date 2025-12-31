package com.example.helloworld.dto.church.frontend;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GroupResult {
    private Long id;
    private String groupName;
    private String description;
    private String meetingFrequency;
    private String category;
    private String meetingLocation;
    private Boolean isActive;
    private List<MemberInfo> members;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MemberInfo {
        private Long id;
        private String personName;
        private String displayName;
        private String memberNo;
        private String role;
    }
}

