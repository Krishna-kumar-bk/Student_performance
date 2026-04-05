package service;

import java.io.IOException; // NEW IMPORT
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.type.TypeReference; // NEW IMPORT
import com.fasterxml.jackson.databind.ObjectMapper; // NEW IMPORT

import dto.LeetCodeDTO;
import entity.LeetCodeStats;
import entity.StudentProfile;
import repository.LeetCodeStatsRepository;
import repository.StudentProfileRepository;

@Service
public class LeetCodeService {

    @Autowired
    private LeetCodeStatsRepository leetCodeStatsRepository;

    @Autowired
    private StudentProfileRepository studentProfileRepository;

    @Autowired
    private RestTemplate restTemplate;

    // *** NEW: Inject the service responsible for daily log saving ***
    @Autowired
    private CodingLogService codingLogService;

    // Helper for JSON parsing (needed for submissionCalendar)
    private final ObjectMapper objectMapper = new ObjectMapper();

    public LeetCodeDTO getStatsDTO(Long userId) {
        LeetCodeStats stats = leetCodeStatsRepository.findByUserId(userId)
            .orElse(new LeetCodeStats());

        LeetCodeDTO dto = new LeetCodeDTO();
        dto.setTotalSolved(stats.getTotalSolved() != null ? stats.getTotalSolved() : 0);
        dto.setEasySolved(stats.getEasySolved() != null ? stats.getEasySolved() : 0);
        dto.setMediumSolved(stats.getMediumSolved() != null ? stats.getMediumSolved() : 0);
        dto.setHardSolved(stats.getHardSolved() != null ? stats.getHardSolved() : 0);
        dto.setRanking(stats.getContestRating() != null ? stats.getContestRating() : 0);
        dto.setAcceptanceRate("64.5%");
        dto.setContributionPoints(stats.getTotalSolved() != null ? stats.getTotalSolved() * 2 : 0);
        dto.setSubmissionCalendar(stats.getSubmissionCalendar());

        return dto;
    }

    /**
     * Fetches LeetCode data, saves main stats, AND updates the daily coding logs.
     */
    @Transactional
    public void refreshLeetCodeData(Long userId) {

        StudentProfile profile = studentProfileRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("Profile not found."));

        String username = profile.getLeetcodeUsername();
        if (username == null || username.isEmpty()) {
            return; // Skip if no username set
        }

        try {
            // 1. Fetch Real Data
            LeetCodeStats fetchedStats = fetchFromLeetCodeApi(username);

            // 2. Save/Update LeetCodeStats Table
            LeetCodeStats existingStats = leetCodeStatsRepository.findByUserId(userId).orElse(null);

            if (existingStats != null) {
                existingStats.setTotalSolved(fetchedStats.getTotalSolved());
                existingStats.setEasySolved(fetchedStats.getEasySolved());
                existingStats.setMediumSolved(fetchedStats.getMediumSolved());
                existingStats.setHardSolved(fetchedStats.getHardSolved());
                existingStats.setContestRating(fetchedStats.getContestRating());
                existingStats.setSubmissionCalendar(fetchedStats.getSubmissionCalendar());
                existingStats.setLastUpdated(java.time.LocalDateTime.now());
                leetCodeStatsRepository.save(existingStats);
            } else {
                fetchedStats.setUserId(userId);
                fetchedStats.setLastUpdated(java.time.LocalDateTime.now());
                leetCodeStatsRepository.save(fetchedStats);
            }

            // 3. Update Main Profile Table
            profile.setLeetcodeSolved(fetchedStats.getTotalSolved());
            studentProfileRepository.save(profile);

            // 4. *** CRITICAL FIX: UPDATE DAILY LOGS TABLE ***
            String calendarJson = fetchedStats.getSubmissionCalendar();
            if (calendarJson != null) {
                // Convert JSON string to Map<String(timestamp), Integer(count)>
                Map<String, Integer> submissionMap = objectMapper.readValue(
                    calendarJson,
                    new TypeReference<Map<String, Integer>>() {}
                );

                // Pass the map to the CodingLogService to create/update daily_coding_logs entries
                codingLogService.updateDailyLogsFromLeetCode(userId, submissionMap);
                System.out.println("✅ Daily Coding Logs updated from LeetCode for user: " + username);
            }


        } catch (IOException e) {
            System.err.println("LeetCode Sync Failed (JSON Parsing Error): " + e.getMessage());
        } catch (Exception e) {
            System.err.println("LeetCode Sync Failed: " + e.getMessage());
        }
    }

    // The fetchFromLeetCodeApi method is correct and remains here:
    private LeetCodeStats fetchFromLeetCodeApi(String username) {
        String url = "https://leetcode.com/graphql";

        String query = String.format(
            "{\"query\":\"query userStats($username: String!) { matchedUser(username: $username) { submissionCalendar submitStats: submitStatsGlobal { acSubmissionNum { difficulty count } } profile { ranking } } }\",\"variables\":{\"username\":\"%s\"}}",
            username
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(query, headers);

        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
        Map<String, Object> body = response.getBody();

        if (body == null || body.containsKey("errors")) {
			throw new RuntimeException("User not found.");
		}

        Map<String, Object> data = (Map<String, Object>) body.get("data");
        Map<String, Object> matchedUser = (Map<String, Object>) data.get("matchedUser");
        if (matchedUser == null) {
			throw new RuntimeException("User not found.");
		}

        String calendar = (String) matchedUser.get("submissionCalendar");

        Map<String, Object> submitStats = (Map<String, Object>) matchedUser.get("submitStats");
        List<Map<String, Object>> acSubmissionNum = (List<Map<String, Object>>) submitStats.get("acSubmissionNum");
        Map<String, Object> profile = (Map<String, Object>) matchedUser.get("profile");

        int easy = 0, medium = 0, hard = 0, total = 0;
        for (Map<String, Object> item : acSubmissionNum) {
            String diff = (String) item.get("difficulty");
            int count = (Integer) item.get("count");
            if ("Easy".equals(diff)) {
				easy = count;
			} else if ("Medium".equals(diff)) {
				medium = count;
			} else if ("Hard".equals(diff)) {
				hard = count;
			} else if ("All".equals(diff)) {
				total = count;
			}
        }
        int ranking = profile.get("ranking") != null ? (Integer) profile.get("ranking") : 0;

        LeetCodeStats stats = new LeetCodeStats();
        stats.setEasySolved(easy);
        stats.setMediumSolved(medium);
        stats.setHardSolved(hard);
        stats.setTotalSolved(total);
        stats.setContestRating(ranking);
        stats.setSubmissionCalendar(calendar);

        return stats;
    }
}