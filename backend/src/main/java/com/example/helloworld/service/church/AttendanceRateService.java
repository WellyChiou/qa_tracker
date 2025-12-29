package com.example.helloworld.service.church;

import com.example.helloworld.dto.church.checkin.AttendanceRateDto;
import com.example.helloworld.entity.church.Person;
import com.example.helloworld.entity.church.checkin.Checkin;
import com.example.helloworld.entity.church.checkin.Session;
import com.example.helloworld.repository.church.GroupPersonRepository;
import com.example.helloworld.repository.church.PersonRepository;
import com.example.helloworld.repository.church.checkin.CheckinRepository;
import com.example.helloworld.repository.church.checkin.SessionGroupRepository;
import com.example.helloworld.repository.church.checkin.SessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AttendanceRateService {
    private final PersonRepository personRepository;
    private final SessionRepository sessionRepository;
    private final CheckinRepository checkinRepository;
    private final GroupPersonRepository groupPersonRepository;
    private final SessionGroupRepository sessionGroupRepository;

    public AttendanceRateService(
            PersonRepository personRepository,
            SessionRepository sessionRepository,
            CheckinRepository checkinRepository,
            GroupPersonRepository groupPersonRepository,
            SessionGroupRepository sessionGroupRepository) {
        this.personRepository = personRepository;
        this.sessionRepository = sessionRepository;
        this.checkinRepository = checkinRepository;
        this.groupPersonRepository = groupPersonRepository;
        this.sessionGroupRepository = sessionGroupRepository;
    }

    /**
     * 計算指定人員在指定年度各類別的出席率（支援歷史記錄）
     */
    @Transactional(transactionManager = "churchTransactionManager", readOnly = true)
    public List<AttendanceRateDto> calculateAttendanceByCategory(Long personId, Integer year, Boolean includeHistorical) {
        if (includeHistorical == null) {
            includeHistorical = false;
        }
        Person person = personRepository.findById(personId)
                .orElseThrow(() -> new RuntimeException("人員不存在：" + personId));

        LocalDate yearStart = LocalDate.of(year, 1, 1);
        LocalDate yearEnd = LocalDate.of(year, 12, 31);
        LocalDate today = LocalDate.now();

        // 只計算到今天的日期
        LocalDate endDate = yearEnd.isAfter(today) ? today : yearEnd;

        List<AttendanceRateDto> results = new ArrayList<>();

        // 計算各類別的出席率（不包括 WEEKDAY，因為小組類別會細分到具體小組）
        String[] categories = {"SATURDAY", "SUNDAY", "SPECIAL"};
        String[] categoryNames = {"週六晚崇", "週日早崇", "活動"};

        for (int i = 0; i < categories.length; i++) {
            String category = categories[i];
            String categoryName = categoryNames[i];

            // 對於 SATURDAY 和 SUNDAY，使用週次計算
            if ("SATURDAY".equals(category) || "SUNDAY".equals(category)) {
                AttendanceRateDto dto = calculateWeeklyAttendanceByCategory(
                        person, category, categoryName, yearStart, endDate, year);
                if (dto != null) {
                    results.add(dto);
                }
            } else {
                // 對於 SPECIAL，使用原有的場次計算
                List<Session> sessions = sessionRepository.findAll().stream()
                        .filter(s -> category.equals(s.getSessionType()))
                        .filter(s -> s.getSessionDate() != null)
                        .filter(s -> !s.getSessionDate().isBefore(yearStart))
                        .filter(s -> !s.getSessionDate().isAfter(endDate))
                        .collect(Collectors.toList());

                if (sessions.isEmpty()) {
                    continue; // 如果沒有場次，跳過
                }

                // 計算已簽到場次數
                long checkedInCount = sessions.stream()
                        .mapToLong(s -> checkinRepository.findBySessionIdAndMemberId(s.getId(), personId)
                                .map(c -> (c.getCanceled() == null || !c.getCanceled()) ? 1 : 0)
                                .orElse(0))
                        .sum();

                int totalSessions = sessions.size();
                BigDecimal attendanceRate = totalSessions > 0
                        ? BigDecimal.valueOf(checkedInCount * 100.0 / totalSessions)
                                .setScale(2, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO;

                AttendanceRateDto dto = new AttendanceRateDto(
                        person.getId(),
                        person.getPersonName(),
                        person.getDisplayName(),
                        person.getMemberNo(),
                        categoryName,
                        totalSessions,
                        (int) checkedInCount,
                        attendanceRate,
                        year
                );
                results.add(dto);
            }
        }

        // 計算小組類別的細分（按具體小組）
        List<com.example.helloworld.entity.church.GroupPerson> groupPersons;
        if (includeHistorical) {
            groupPersons = groupPersonRepository.findAllGroupsByPersonId(personId);
        } else {
            groupPersons = groupPersonRepository.findActiveGroupsByPersonId(personId);
        }

        for (com.example.helloworld.entity.church.GroupPerson gp : groupPersons) {
            Long groupId = gp.getGroupId() != null ? gp.getGroupId() : gp.getGroup().getId();
            LocalDate joinedAt = gp.getJoinedAt();
            LocalDate leftAt = gp.getLeftAt();
            String groupName = gp.getGroup() != null ? gp.getGroup().getGroupName() : "未知小組";
            Boolean isActive = gp.getIsActive();

            // 確定有效的日期範圍
            LocalDate effectiveStart = joinedAt.isAfter(yearStart) ? joinedAt : yearStart;
            LocalDate effectiveEnd = endDate;
            if (leftAt != null && leftAt.isBefore(effectiveEnd)) {
                effectiveEnd = leftAt;
            }

            // 使用週次計算方法
            AttendanceRateDto dto = calculateGroupWeeklyAttendance(
                    person, groupId, groupName, effectiveStart, effectiveEnd, year,
                    isActive != null && isActive, joinedAt, leftAt);
            if (dto != null) {
                results.add(dto);
            }
        }

        return results;
    }

    /**
     * 計算指定小組所有成員在指定年度的出席率（支援歷史記錄）
     */
    @Transactional(transactionManager = "churchTransactionManager", readOnly = true)
    public List<AttendanceRateDto> calculateGroupAttendance(Long groupId, Integer year, Boolean includeHistorical) {
        if (includeHistorical == null) {
            includeHistorical = false;
        }
        LocalDate yearStart = LocalDate.of(year, 1, 1);
        LocalDate yearEnd = LocalDate.of(year, 12, 31);
        LocalDate today = LocalDate.now();
        LocalDate endDate = yearEnd.isAfter(today) ? today : yearEnd;

        // 獲取小組所有成員
        List<com.example.helloworld.entity.church.GroupPerson> groupPersons;
        if (includeHistorical) {
            groupPersons = groupPersonRepository.findAllMembersByGroupId(groupId);
        } else {
            groupPersons = groupPersonRepository.findActiveMembersByGroupId(groupId);
        }

        List<AttendanceRateDto> results = new ArrayList<>();

        for (com.example.helloworld.entity.church.GroupPerson gp : groupPersons) {
            Person person = gp.getPerson();
            LocalDate joinedAt = gp.getJoinedAt();
            LocalDate leftAt = gp.getLeftAt();
            Boolean isActive = gp.getIsActive();

            // 確定有效的日期範圍
            LocalDate effectiveStart = joinedAt.isAfter(yearStart) ? joinedAt : yearStart;
            LocalDate effectiveEnd = endDate;
            if (leftAt != null && leftAt.isBefore(effectiveEnd)) {
                effectiveEnd = leftAt;
            }

            // 使用週次計算方法
            AttendanceRateDto dto = calculateGroupWeeklyAttendance(
                    person, groupId, gp.getGroup().getGroupName(), effectiveStart, effectiveEnd, year,
                    isActive != null && isActive, joinedAt, leftAt);
            if (dto != null) {
                results.add(dto);
            }
        }

        return results;
    }

    /**
     * 計算所有人員在指定年度各類別的出席率（支援歷史記錄）
     */
    @Transactional(transactionManager = "churchTransactionManager", readOnly = true)
    public List<AttendanceRateDto> calculateAllPersonsAttendance(Integer year, String category, Boolean includeHistorical) {
        if (includeHistorical == null) {
            includeHistorical = false;
        }
        List<Person> persons = personRepository.findByIsActiveTrueOrderByPersonNameAsc();
        List<AttendanceRateDto> results = new ArrayList<>();

        for (Person person : persons) {
            List<AttendanceRateDto> personRates = calculateAttendanceByCategory(person.getId(), year, includeHistorical);
            
            if (category != null && !category.isEmpty()) {
                // 過濾特定類別
                personRates = personRates.stream()
                        .filter(dto -> {
                            if ("WEEKDAY".equals(category)) {
                                // 如果是小組類別，返回所有小組的細分（排除其他類別）
                                // 小組細分的 category 是具體的小組名稱，不是「小組」
                                return !dto.getCategory().equals("週六晚崇") && 
                                       !dto.getCategory().equals("週日早崇") && 
                                       !dto.getCategory().equals("活動");
                            } else {
                                return dto.getCategory().equals(getCategoryName(category));
                            }
                        })
                        .collect(Collectors.toList());
            }
            
            results.addAll(personRates);
        }

        return results;
    }

    private String getCategoryName(String category) {
        return switch (category) {
            case "SATURDAY" -> "週六晚崇";
            case "SUNDAY" -> "週日早崇";
            case "WEEKDAY" -> "小組";
            case "SPECIAL" -> "活動";
            default -> category;
        };
    }

    /**
     * 按自然週計算主日類型的出席率（只計算有場次的週數）
     */
    private AttendanceRateDto calculateWeeklyAttendanceByCategory(
            Person person, String category, String categoryName,
            LocalDate yearStart, LocalDate endDate, Integer year) {
        
        // 查詢該類別的所有場次（年度範圍內，不超過今天）
        List<Session> sessions = sessionRepository.findAll().stream()
                .filter(s -> category.equals(s.getSessionType()))
                .filter(s -> s.getSessionDate() != null)
                .filter(s -> !s.getSessionDate().isBefore(yearStart))
                .filter(s -> !s.getSessionDate().isAfter(endDate))
                .collect(Collectors.toList());

        if (sessions.isEmpty()) {
            return null; // 如果沒有場次，返回 null
        }

        // 按自然週分組（使用週一日期作為週標識）
        Map<LocalDate, List<Session>> sessionsByWeek = sessions.stream()
                .collect(Collectors.groupingBy(s -> {
                    LocalDate sessionDate = s.getSessionDate();
                    // 獲取該週的週一日期
                    return sessionDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                }));

        // 只計算有場次的週數
        int totalWeeks = sessionsByWeek.size();
        if (totalWeeks == 0) {
            return null;
        }

        // 計算有參加的週數
        int attendedWeeks = 0;
        for (Map.Entry<LocalDate, List<Session>> entry : sessionsByWeek.entrySet()) {
            List<Session> weekSessions = entry.getValue();
            // 檢查該人員在該週內是否有參加過任何一次
            boolean attended = weekSessions.stream()
                    .anyMatch(s -> {
                        Optional<Checkin> checkin = checkinRepository.findBySessionIdAndMemberId(
                                s.getId(), person.getId());
                        return checkin.isPresent() &&
                               (checkin.get().getCanceled() == null || !checkin.get().getCanceled());
                    });
            if (attended) {
                attendedWeeks++;
            }
        }

        BigDecimal attendanceRate = BigDecimal.valueOf(attendedWeeks * 100.0 / totalWeeks)
                .setScale(2, RoundingMode.HALF_UP);

        return new AttendanceRateDto(
                person.getId(),
                person.getPersonName(),
                person.getDisplayName(),
                person.getMemberNo(),
                categoryName,
                totalWeeks, // 總週數
                attendedWeeks, // 有參加的週數
                attendanceRate,
                year
        );
    }

    /**
     * 按自然週計算小組類型的出席率（只計算有該小組簽到表的週數）
     */
    private AttendanceRateDto calculateGroupWeeklyAttendance(
            Person person, Long groupId, String groupName,
            LocalDate effectiveStart, LocalDate effectiveEnd, Integer year,
            boolean isActive, LocalDate joinedAt, LocalDate leftAt) {
        
        // 獲取所有與該小組關聯的場次ID（從 session_groups 表）
        List<com.example.helloworld.entity.church.checkin.SessionGroup> sessionGroups = 
                sessionGroupRepository.findByGroupId(groupId);
        List<Long> associatedSessionIds = sessionGroups.stream()
                .map(sg -> sg.getSessionId())
                .collect(Collectors.toList());

        // 查詢該小組的所有小組類型場次（在有效日期範圍內）
        List<Session> groupSessions = sessionRepository.findAll().stream()
                .filter(s -> "WEEKDAY".equals(s.getSessionType()))
                .filter(s -> s.getSessionDate() != null)
                .filter(s -> !s.getSessionDate().isBefore(effectiveStart))
                .filter(s -> !s.getSessionDate().isAfter(effectiveEnd))
                .filter(s -> {
                    // 如果場次已關聯到該小組，則包含
                    if (associatedSessionIds.contains(s.getId())) {
                        return true;
                    }
                    // 如果場次沒有關聯到任何小組，則根據人員所屬的小組判斷（用於處理舊數據）
                    List<com.example.helloworld.entity.church.checkin.SessionGroup> sessionGroupList = 
                            sessionGroupRepository.findBySessionId(s.getId());
                    if (sessionGroupList.isEmpty()) {
                        // 場次沒有關聯到任何小組，如果人員屬於該小組，則包含
                        return true;
                    }
                    return false;
                })
                .collect(Collectors.toList());

        if (groupSessions.isEmpty()) {
            return null; // 如果沒有場次，返回 null
        }

        // 按自然週分組（使用週一日期作為週標識）
        Map<LocalDate, List<Session>> sessionsByWeek = groupSessions.stream()
                .collect(Collectors.groupingBy(s -> {
                    LocalDate sessionDate = s.getSessionDate();
                    // 獲取該週的週一日期
                    return sessionDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                }));

        // 只計算有該小組簽到表的週數
        int totalWeeks = sessionsByWeek.size();
        if (totalWeeks == 0) {
            return null;
        }

        // 計算有參加的週數
        int attendedWeeks = 0;
        for (Map.Entry<LocalDate, List<Session>> entry : sessionsByWeek.entrySet()) {
            List<Session> weekSessions = entry.getValue();
            // 檢查該人員在該週內是否有參加過該小組的任何一次場次
            boolean attended = weekSessions.stream()
                    .anyMatch(s -> {
                        Optional<Checkin> checkin = checkinRepository.findBySessionIdAndMemberId(
                                s.getId(), person.getId());
                        return checkin.isPresent() &&
                               (checkin.get().getCanceled() == null || !checkin.get().getCanceled());
                    });
            if (attended) {
                attendedWeeks++;
            }
        }

        BigDecimal attendanceRate = BigDecimal.valueOf(attendedWeeks * 100.0 / totalWeeks)
                .setScale(2, RoundingMode.HALF_UP);

        // 設置小組狀態
        String groupStatus = isActive ? "CURRENT" : "HISTORICAL";

        return new AttendanceRateDto(
                person.getId(),
                person.getPersonName(),
                person.getDisplayName(),
                person.getMemberNo(),
                groupName,
                totalWeeks, // 總週數
                attendedWeeks, // 有參加的週數
                attendanceRate,
                year,
                groupStatus,
                joinedAt,
                leftAt
        );
    }
}

