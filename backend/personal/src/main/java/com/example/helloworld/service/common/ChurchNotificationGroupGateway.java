package com.example.helloworld.service.common;

import java.util.List;

public interface ChurchNotificationGroupGateway {
    List<NotificationTargetGroup> getActiveChurchGroups();
}
