package controller;

import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import entity.StudentProfile;
import entity.User;
import repository.StudentProfileRepository;
import repository.UserRepository;
import service.HackerRankService;

@RestController
@RequestMapping("/api/data/hackerrank")
public class HackerRankController {

    @Autowired
    private StudentProfileRepository studentProfileRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private HackerRankService hackerRankService;

    // Helper to get User entity
    private User getUser(Principal principal) {
        return userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found."));
    }

    // 1. Student-Specific Stats (For the student dashboard)
    @GetMapping("/stats")
    public ResponseEntity<?> getHackerRankStats(Principal principal) {
        try {
            User user = getUser(principal);
            return ResponseEntity.ok(hackerRankService.getUserStats(user.getUserId()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body("Error fetching HackerRank data: " + e.getMessage());
        }
    }

    /**
     * NEW: Staff Summary API (For the Faculty Overview Cards and Chart)
     * Endpoint: /api/data/hackerrank/stats/summary
     */
    @GetMapping("/stats/summary")
    public ResponseEntity<Map<String, Object>> getStaffHackerRankSummary() {
        // This calls the aggregator method we added to GfgService/HackerRankService
        return ResponseEntity.ok(hackerRankService.getStaffSummary());
    }

    // 2. Staff API for the Leaderboard Table
    @GetMapping("/leaderboard")
    public ResponseEntity<List<Map<String, Object>>> getHackerRankLeaderboard() {
        List<StudentProfile> profiles = studentProfileRepository.findAll();

        profiles.sort((p1, p2) -> {
            int b1 = (p1.getHackerrankBadges() == null) ? 0 : p1.getHackerrankBadges();
            int b2 = (p2.getHackerrankBadges() == null) ? 0 : p2.getHackerrankBadges();
            return Integer.compare(b2, b1);
        });

        List<Map<String, Object>> response = new ArrayList<>();
        int rank = 1;

        for (StudentProfile p : profiles) {
            if (p.getUser() == null) continue;

            int badges = (p.getHackerrankBadges() == null) ? 0 : p.getHackerrankBadges();
            int score = badges * 10;

            Map<String, Object> row = new HashMap<>();
            row.put("rank", rank++);
            row.put("name", p.getUser().getFullName());
            row.put("score", score);
            row.put("badges", badges);
            row.put("star_rating", calculateStars(badges));

            response.add(row);
        }
        return ResponseEntity.ok(response);
    }

    // 3. API for Certificates List
    @GetMapping("/certificates")
    public ResponseEntity<List<Map<String, Object>>> getCertificates() {
        List<StudentProfile> profiles = studentProfileRepository.findAll();
        List<Map<String, Object>> response = new ArrayList<>();

        for (StudentProfile p : profiles) {
            int certCount = (p.getCertificatesCount() == null) ? 0 : p.getCertificatesCount();
            if (certCount > 0 && p.getUser() != null) {
                Map<String, Object> row = new HashMap<>();
                row.put("studentName", p.getUser().getFullName());
                row.put("certificatesEarned", certCount + " Certificates Earned");
                row.put("status", "Verified");
                response.add(row);
            }
        }
        return ResponseEntity.ok(response);
    }

    private int calculateStars(int badges) {
        if (badges >= 10) return 5;
        if (badges >= 7) return 4;
        if (badges >= 4) return 3;
        if (badges >= 1) return 2;
        return 1;
    }
}