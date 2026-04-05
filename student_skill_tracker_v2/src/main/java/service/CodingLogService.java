package service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import dto.CodingLogsResponseDTO;
import dto.DailyCodingDTO;
import entity.CodingLog;
import entity.LeetCodeStats;
import entity.User;
import repository.CodingLogRepository;
import repository.LeetCodeStatsRepository;
import repository.StudentProfileRepository;
import repository.UserRepository;

@Service
public class CodingLogService {

    @Autowired private CodingLogRepository codingLogRepository;
    @Autowired private LeetCodeStatsRepository leetCodeStatsRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private StudentProfileRepository studentProfileRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final ZoneId DEFAULT_ZONE = ZoneId.systemDefault();

    /**
     * UPDATED: Fetches clean, non-duplicated stats for the Student Dashboard.
     * Uses strict Calendar Month logic.
     */
    public DailyCodingDTO getStudentStats(Long userId) {
        DailyCodingDTO dto = new DailyCodingDTO();
        LocalDate today = LocalDate.now();
        
        // Define Strict Boundaries
        LocalDate startOfWeek = today.minusDays(6); // Rolling 7 days
        LocalDate startOfMonth = today.withDayOfMonth(1); // Calendar Month (April 1st)

        // Fetch logs ONLY from DB to avoid double-counting LeetCode JSON
        List<CodingLog> dbLogs = codingLogRepository.findByUserId(userId);

        int todaySum = 0;
        int weekSum = 0;
        int monthSum = 0;

        for (CodingLog log : dbLogs) {
            LocalDate date = log.getLogDate();
            int count = log.getSolvedCount();

            // Today
            if (date.isEqual(today)) {
                todaySum += count;
            }
            
            // This Week (Last 7 days)
            if (!date.isBefore(startOfWeek) && !date.isAfter(today)) {
                weekSum += count;
            }
            
            // This Month (Calendar Month: 1st to Now)
            if (!date.isBefore(startOfMonth) && !date.isAfter(today)) {
                monthSum += count;
            }
        }

        // Sort history for the UI table (Newest first)
        dbLogs.sort(Comparator.comparing(CodingLog::getLogDate).reversed());

        dto.setTodayCount(todaySum);
        dto.setWeekCount(weekSum);
        dto.setMonthCount(monthSum);
        dto.setHistory(dbLogs);

        return dto;
    }

    @Transactional
    public void logDailyCoding(Long userId, Integer solvedCount) {
        if (solvedCount == null || solvedCount <= 0) return;
        LocalDate today = LocalDate.now();
        Optional<CodingLog> existingLog = codingLogRepository.findByUserIdAndLogDate(userId, today);

        if (existingLog.isPresent()) {
            CodingLog log = existingLog.get();
            log.setSolvedCount(log.getSolvedCount() + solvedCount);
            codingLogRepository.save(log);
        } else {
            CodingLog newLog = new CodingLog();
            newLog.setUserId(userId);
            newLog.setSolvedCount(solvedCount);
            newLog.setLogDate(today);
            codingLogRepository.save(newLog);
        }
    }

    @Transactional
    public void updateDailyLogsFromLeetCode(Long userId, Map<String, Integer> submissionMap) {
        submissionMap.forEach((timestampStr, solvedCount) -> {
            if (solvedCount == null || solvedCount <= 0) return;

            try {
                long timestampSeconds = Long.parseLong(timestampStr);
                LocalDate logDate = Instant.ofEpochSecond(timestampSeconds)
                                           .atZone(DEFAULT_ZONE)
                                           .toLocalDate();

                Optional<CodingLog> existingLog = codingLogRepository.findByUserIdAndLogDate(userId, logDate);

                if (existingLog.isPresent()) {
                    CodingLog log = existingLog.get();
                    log.setSolvedCount(solvedCount); // Sync to exact LeetCode count
                    codingLogRepository.save(log);
                } else {
                    CodingLog newLog = new CodingLog();
                    newLog.setUserId(userId);
                    newLog.setLogDate(logDate);
                    newLog.setSolvedCount(solvedCount);
                    codingLogRepository.save(newLog);
                }
            } catch (Exception e) {
                System.err.println("Error syncing LeetCode timestamp: " + timestampStr);
            }
        });
    }

    public CodingLogsResponseDTO generateStaffReport() {
        CodingLogsResponseDTO response = new CodingLogsResponseDTO();
        List<CodingLogsResponseDTO.StudentLogEntry> studentEntries = new ArrayList<>();
        Map<LocalDate, Integer> weeklyClassTotals = initializeWeeklyTotalsMap();

        List<User> allStudents = userRepository.findStudentUsersForReport();

        for (User user : allStudents) {
            try {
                Map<LocalDate, Integer> dailySolvedMap = getDailySolvedMap(user.getUserId());
                StudentStats stats = calculateIndividualStats(dailySolvedMap);
                aggregateClassTotals(dailySolvedMap, weeklyClassTotals);

                studentEntries.add(new CodingLogsResponseDTO.StudentLogEntry(
                    user.getFullName(),
                    stats.todayCount,
                    stats.weekCount,
                    stats.currentStreak,
                    stats.longestStreak,
                    stats.lastLogDate
                ));
            } catch (Exception e) {
                System.err.println("Failed to process logs for user: " + user.getUserId());
            }
        }

        response.setStudentLogs(studentEntries);
        response.setWeeklySummary(convertWeeklyTotalsToDTO(weeklyClassTotals));
        return response;
    }

    private Map<LocalDate, Integer> initializeWeeklyTotalsMap() {
        Map<LocalDate, Integer> map = new LinkedHashMap<>();
        LocalDate today = LocalDate.now();
        for (int i = 6; i >= 0; i--) {
            map.put(today.minusDays(i), 0);
        }
        return map;
    }

    private List<CodingLogsResponseDTO.DailySummaryEntry> convertWeeklyTotalsToDTO(Map<LocalDate, Integer> totalsMap) {
        return totalsMap.entrySet().stream()
            .map(entry -> new CodingLogsResponseDTO.DailySummaryEntry(
                entry.getKey().getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH), 
                entry.getValue()))
            .collect(Collectors.toList());
    }

    private void aggregateClassTotals(Map<LocalDate, Integer> dailySolvedMap, Map<LocalDate, Integer> weeklyClassTotals) {
        LocalDate startOfWeek = LocalDate.now().minusDays(7);
        dailySolvedMap.forEach((date, count) -> {
            if (date.isAfter(startOfWeek) && weeklyClassTotals.containsKey(date)) {
                weeklyClassTotals.put(date, weeklyClassTotals.get(date) + count);
            }
        });
    }

    private Map<LocalDate, Integer> getDailySolvedMap(Long userId) {
        Map<LocalDate, Integer> map = new HashMap<>();
        codingLogRepository.findByUserId(userId).forEach(log -> 
            map.put(log.getLogDate(), map.getOrDefault(log.getLogDate(), 0) + log.getSolvedCount()));
        return map;
    }

    private StudentStats calculateIndividualStats(Map<LocalDate, Integer> dailySolvedMap) {
        StudentStats stats = new StudentStats();
        LocalDate today = LocalDate.now();
        
        List<LocalDate> activeDays = dailySolvedMap.entrySet().stream()
            .filter(entry -> entry.getValue() > 0)
            .map(Map.Entry::getKey)
            .sorted(Comparator.reverseOrder())
            .collect(Collectors.toList());

        if (!activeDays.isEmpty()) stats.lastLogDate = activeDays.get(0);

        LocalDate startOfWeek = today.minusDays(7);
        dailySolvedMap.forEach((d, count) -> {
            if (d.isEqual(today)) stats.todayCount = count;
            if (d.isAfter(startOfWeek)) stats.weekCount += count;
        });

        // Streak logic
        int currentStreak = 0;
        LocalDate checkDate = today;
        if (dailySolvedMap.getOrDefault(today, 0) == 0) checkDate = today.minusDays(1);

        while (dailySolvedMap.getOrDefault(checkDate, 0) > 0) {
            currentStreak++;
            checkDate = checkDate.minusDays(1);
        }

        stats.currentStreak = currentStreak;
        stats.longestStreak = currentStreak; // Simplified for report
        return stats;
    }

    private static class StudentStats {
        public int todayCount = 0;
        public int weekCount = 0;
        public int currentStreak = 0;
        public int longestStreak = 0;
        public LocalDate lastLogDate = null;
    }
}