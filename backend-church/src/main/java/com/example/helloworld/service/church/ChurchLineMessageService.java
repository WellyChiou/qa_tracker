package com.example.helloworld.service.church;

import com.example.helloworld.dto.church.admin.GoogleSyncResult;
import com.example.helloworld.dto.church.admin.ReplyResult;
import com.example.helloworld.dto.church.admin.ServiceUpdatePayload;
import com.example.helloworld.entity.church.Person;
import com.example.helloworld.entity.church.Position;
import com.example.helloworld.entity.church.PositionPerson;
import com.example.helloworld.entity.church.ServiceScheduleAssignment;
import com.example.helloworld.entity.church.ServiceScheduleDate;
import com.example.helloworld.entity.church.ServiceSchedulePositionConfig;
import com.example.helloworld.repository.church.PersonRepository;
import com.example.helloworld.repository.church.PositionPersonRepository;
import com.example.helloworld.repository.church.PositionRepository;
import com.example.helloworld.repository.church.ServiceScheduleAssignmentRepository;
import com.example.helloworld.repository.church.ServiceScheduleDateRepository;
import com.example.helloworld.repository.church.ServiceSchedulePositionConfigRepository;
import com.example.helloworld.scheduler.church.ServiceScheduleNotificationScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
public class ChurchLineMessageService {
    private static final Logger log = LoggerFactory.getLogger(ChurchLineMessageService.class);

    private final GoogleSheetsRosterService googleSheetsRosterService;
    private final ServiceScheduleDateRepository serviceScheduleDateRepository;
    private final PositionRepository positionRepository;
    private final PersonRepository personRepository;
    private final PositionPersonRepository positionPersonRepository;
    private final ServiceSchedulePositionConfigRepository serviceSchedulePositionConfigRepository;
    private final ServiceScheduleAssignmentRepository serviceScheduleAssignmentRepository;
    private final ServiceScheduleNotificationScheduler serviceScheduleNotificationScheduler;

    public ChurchLineMessageService(
            GoogleSheetsRosterService googleSheetsRosterService,
            ServiceScheduleDateRepository serviceScheduleDateRepository,
            PositionRepository positionRepository,
            PersonRepository personRepository,
            PositionPersonRepository positionPersonRepository,
            ServiceSchedulePositionConfigRepository serviceSchedulePositionConfigRepository,
            ServiceScheduleAssignmentRepository serviceScheduleAssignmentRepository,
            @Lazy ServiceScheduleNotificationScheduler serviceScheduleNotificationScheduler) {
        this.googleSheetsRosterService = googleSheetsRosterService;
        this.serviceScheduleDateRepository = serviceScheduleDateRepository;
        this.positionRepository = positionRepository;
        this.personRepository = personRepository;
        this.positionPersonRepository = positionPersonRepository;
        this.serviceSchedulePositionConfigRepository = serviceSchedulePositionConfigRepository;
        this.serviceScheduleAssignmentRepository = serviceScheduleAssignmentRepository;
        this.serviceScheduleNotificationScheduler = serviceScheduleNotificationScheduler;
    }

    public void triggerWeeklyServiceNotification(String requesterUid, String groupId, String replyToken, Runnable onFailure) {
        new Thread(() -> {
            try {
                log.info("🔔 [ChurchLine] 用戶 {} 請求發送本週服事表通知", requesterUid);
                serviceScheduleNotificationScheduler.sendWeeklyServiceNotification(groupId, replyToken);
            } catch (Exception e) {
                log.error("❌ [ChurchLine] 執行本週服事表通知失敗", e);
                if (onFailure != null) {
                    onFailure.run();
                }
            }
        }).start();
    }

    @Transactional(transactionManager = "churchTransactionManager")
    public ReplyResult processServiceUpdate(String dateStr, String positionName, String personName) {
        try {
            LocalDate date;
            try {
                date = LocalDate.parse(dateStr, DateTimeFormatter.BASIC_ISO_DATE);
            } catch (Exception e) {
                return ReplyResult.fail("❌ 日期格式錯誤，請使用 yyyyMMdd 格式（例如：20260101）。");
            }

            if (date.isBefore(LocalDate.now())) {
                return ReplyResult.fail("❌ 無法更新過去的服事表，請輸入未來的日期。");
            }

            java.time.DayOfWeek dayOfWeek = date.getDayOfWeek();
            String dayType;
            String dayText;
            if (dayOfWeek == java.time.DayOfWeek.SATURDAY) {
                dayType = "saturday";
                dayText = "週六";
            } else if (dayOfWeek == java.time.DayOfWeek.SUNDAY) {
                dayType = "sunday";
                dayText = "週日";
            } else {
                return ReplyResult.fail("❌ 該日期不是週六或週日，請輸入週末的日期。");
            }

            Optional<Position> positionOpt = positionRepository.findByPositionName(positionName);
            if (positionOpt.isEmpty()) {
                List<Position> allPositions = positionRepository.findByIsActiveTrueOrderBySortOrderAsc();
                StringBuilder sb = new StringBuilder("❌ 找不到崗位「" + positionName + "」。\n\n可用崗位：\n");
                for (Position position : allPositions) {
                    sb.append("• ").append(position.getPositionName()).append("\n");
                }
                return ReplyResult.fail(sb.toString());
            }
            Position position = positionOpt.get();

            Optional<Person> personOpt = personRepository.findByPersonName(personName);
            if (personOpt.isEmpty()) {
                List<PositionPerson> availablePersons = positionPersonRepository.findByPositionIdAndDayTypeOrdered(position.getId(), dayType);
                StringBuilder sb = new StringBuilder("❌ 系統中找不到人員「" + personName + "」。\n\n");
                sb.append("該崗位在").append(dayText).append("的可用人員：\n");
                if (availablePersons.isEmpty()) {
                    sb.append("(無可用人員)");
                } else {
                    for (PositionPerson pp : availablePersons) {
                        Person p = pp.getPerson();
                        String displayName = p.getDisplayName();
                        String personNameValue = p.getPersonName();
                        String showName = (displayName != null && !displayName.trim().isEmpty()) ? displayName : personNameValue;
                        sb.append("• ").append(showName);
                        if (displayName != null && !displayName.trim().isEmpty() && !displayName.equals(personNameValue)) {
                            sb.append(" (").append(personNameValue).append(")");
                        }
                        sb.append("\n");
                    }
                }
                return ReplyResult.fail(sb.toString());
            }

            Person person = personOpt.get();
            Optional<PositionPerson> ppOpt = positionPersonRepository.findByPositionIdAndPersonIdAndDayType(position.getId(), person.getId(), dayType);
            if (ppOpt.isEmpty()) {
                List<PositionPerson> availablePersons = positionPersonRepository.findByPositionIdAndDayTypeOrdered(position.getId(), dayType);
                StringBuilder sb = new StringBuilder("❌ 人員「" + personName + "」未被分配到「" + positionName + "」的" + dayText + "列表。\n\n");
                sb.append("該崗位在").append(dayText).append("的可用人員：\n");
                if (availablePersons.isEmpty()) {
                    sb.append("(無可用人員)");
                } else {
                    for (PositionPerson pp : availablePersons) {
                        sb.append("• ").append(pp.getPerson().getPersonName()).append("\n");
                    }
                }
                return ReplyResult.fail(sb.toString());
            }

            Optional<ServiceScheduleDate> scheduleDateOpt = serviceScheduleDateRepository.findByDate(date);
            if (scheduleDateOpt.isEmpty()) {
                return ReplyResult.fail("❌ 找不到 " + dateStr + " 的服事表，請先在後台建立該年度的服事表。");
            }
            ServiceScheduleDate scheduleDate = scheduleDateOpt.get();

            Optional<ServiceSchedulePositionConfig> configOpt =
                    serviceSchedulePositionConfigRepository.findByServiceScheduleDateAndPosition(scheduleDate, position);

            ServiceSchedulePositionConfig config;
            String originalPersonName = "(無)";

            if (configOpt.isPresent()) {
                config = configOpt.get();
                List<ServiceScheduleAssignment> assignments =
                        serviceScheduleAssignmentRepository.findByServiceSchedulePositionConfig(config);

                if (!assignments.isEmpty()) {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < assignments.size(); i++) {
                        if (i > 0) {
                            sb.append("、");
                        }
                        sb.append(assignments.get(i).getPerson().getPersonName());
                    }
                    originalPersonName = sb.toString();
                }

                serviceScheduleAssignmentRepository.deleteAll(assignments);
            } else {
                config = new ServiceSchedulePositionConfig();
                config.setServiceScheduleDate(scheduleDate);
                config.setPosition(position);
                config.setPersonCount(1);
                config = serviceSchedulePositionConfigRepository.save(config);
            }

            ServiceScheduleAssignment assignment = new ServiceScheduleAssignment();
            assignment.setServiceSchedulePositionConfig(config);
            assignment.setPerson(person);
            assignment.setSortOrder(0);
            serviceScheduleAssignmentRepository.save(assignment);

            config.setPersonCount(1);
            serviceSchedulePositionConfigRepository.save(config);

            String showName = (person.getDisplayName() != null && !person.getDisplayName().trim().isEmpty())
                    ? person.getDisplayName().trim()
                    : person.getPersonName().trim();

            GoogleSyncResult googleSyncMsg;
            try {
                googleSyncMsg = googleSheetsRosterService.syncWithRetry(date, positionName, showName);
            } catch (Exception e) {
                log.error("Google Sheet sync error", e);
                googleSyncMsg = GoogleSyncResult.fail("⚠️ Google PLC 服事表 同步失敗：" + e.getMessage());
            }

            String dateText = date.toString();
            String resultText = String.format(
                    "✅ 服事更新成功\n\n" +
                            "📅 日期：%s（%s）\n" +
                            "🎯 崗位：%s\n" +
                            "🔁 變更：%s → %s\n\n" +
                            "%s",
                    dateText, dayText, positionName, originalPersonName, showName, googleSyncMsg.getMessage()
            );

            ServiceUpdatePayload payload = new ServiceUpdatePayload(
                    dateText,
                    dayText,
                    positionName,
                    originalPersonName,
                    showName,
                    googleSyncMsg.getMessage(),
                    googleSheetsRosterService.isTestMode()
            );

            return ReplyResult.ok(resultText, "SERVICE_UPDATE", payload);
        } catch (Exception e) {
            log.error("❌ 處理服事更新失敗", e);
            return ReplyResult.fail("❌ 更新失敗，系統發生錯誤：" + e.getMessage());
        }
    }
}
