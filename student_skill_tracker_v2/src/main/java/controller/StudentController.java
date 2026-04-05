package controller;

import java.security.Principal;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dto.DashboardDTO;
import dto.LeaderboardDTO;
import dto.StudentSettingsDTO;
import entity.User;
import repository.UserRepository;
import service.CodingLogService;
import service.GfgService; // NEW: Import GfgService
import service.GitHubService;
import service.HackerRankService;
import service.LeetCodeService;
import service.LinkedInService;
import service.StudentService;

@RestController
@RequestMapping("/api/student")
public class StudentController {

    @Autowired private StudentService studentService;
    @Autowired private UserRepository userRepository;

    // *** INJECT EXTERNAL SERVICES ***
    @Autowired private LeetCodeService leetCodeService;
    @Autowired private GitHubService gitHubService;
    @Autowired private HackerRankService hackerRankService;
    @Autowired private LinkedInService linkedInService;
    @Autowired private GfgService gfgService; // NEW: Inject GfgService

    @Autowired private CodingLogService codingLogService;


    private User getUser(Principal principal) {
        return userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @GetMapping("/dashboard")
    public ResponseEntity<DashboardDTO> getDashboardData(Principal principal) {
        try {
            User user = getUser(principal);
            return ResponseEntity.ok(studentService.getDashboardData(user.getUserId()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/leaderboard")
    public ResponseEntity<List<LeaderboardDTO>> getLeaderboard() {
        return ResponseEntity.ok(studentService.getLeaderboard());
    }

    @GetMapping("/settings")
    public ResponseEntity<StudentSettingsDTO> getSettings(Principal principal) {
        try {
            User user = getUser(principal);
            return ResponseEntity.ok(studentService.getSettings(user.getUserId()));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/settings")
    public ResponseEntity<?> updateSettings(@RequestBody StudentSettingsDTO settingsDto, Principal principal) {
        try {
            User user = getUser(principal);
            // This now saves GFG Username and Aptitude Score inside StudentService
            studentService.updateSettings(user.getUserId(), settingsDto);
            
            // Immediately trigger a GFG scrape if a username was provided
            if (settingsDto.getGfgUsername() != null && !settingsDto.getGfgUsername().isEmpty()) {
                gfgService.refreshGfgData(user.getUserId());
            }
            
            return ResponseEntity.ok("Settings updated successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error updating settings: " + e.getMessage());
        }
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody Map<String, String> passwordMap, Principal principal) {
        try {
            String currentPass = passwordMap.get("currentPassword");
            String newPass = passwordMap.get("newPassword");

            if (currentPass == null || newPass == null || newPass.length() < 3) {
                return ResponseEntity.badRequest().body("New password must be at least 3 characters.");
            }

            User user = getUser(principal);
            studentService.changePassword(user.getUserId(), currentPass, newPass);

            return ResponseEntity.ok("Password changed successfully.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/refresh-all")
    public ResponseEntity<?> refreshAllStats(Principal principal) {
        try {
            User user = getUser(principal);
            // Refreshing everything including the new GFG service
            leetCodeService.refreshLeetCodeData(user.getUserId());
            gitHubService.refreshGitHubData(user.getUserId());
            hackerRankService.refreshHackerRankData(user.getUserId());
            gfgService.refreshGfgData(user.getUserId()); // NEW: GFG Refresh
            
            return ResponseEntity.ok("Global sync completed!");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Refresh failed: " + e.getMessage());
        }
    }
}