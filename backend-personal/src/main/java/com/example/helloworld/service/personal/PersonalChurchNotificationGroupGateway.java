package com.example.helloworld.service.personal;

import com.example.helloworld.service.common.ChurchNotificationGroupGateway;
import com.example.helloworld.service.common.NotificationTargetGroup;
import com.example.helloworld.repository.personal.LineGroupRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PersonalChurchNotificationGroupGateway implements ChurchNotificationGroupGateway {
    private static final String CHURCH_TECH_CONTROL = "CHURCH_TECH_CONTROL";

    private final LineGroupRepository lineGroupRepository;

    public PersonalChurchNotificationGroupGateway(LineGroupRepository lineGroupRepository) {
        this.lineGroupRepository = lineGroupRepository;
    }

    @Override
    public List<NotificationTargetGroup> getActiveChurchGroups() {
        return lineGroupRepository.findByGroupCodeAndIsActiveTrue(CHURCH_TECH_CONTROL)
                .stream()
                .map(group -> new NotificationTargetGroup(group.getGroupId(), group.getGroupName()))
                .toList();
    }
}
