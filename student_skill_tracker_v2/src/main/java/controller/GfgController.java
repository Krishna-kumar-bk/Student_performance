package controller;

import java.security.Principal;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import entity.User;
import entity.StudentProfile;
import repository.UserRepository;
import repository.StudentProfileRepository;
import service.GfgService;

@RestController
@RequestMapping("/api/gfg")
public class GfgController {

    @Autowired private GfgService gfgService;
    @Autowired private UserRepository userRepository;
    @Autowired private StudentProfileRepository studentProfileRepository;

    @PostMapping("/refresh/{userId}")
    public ResponseEntity<?> refreshGfg(@PathVariable Long userId, Principal principal) {
        try {
            User currentUser = userRepository.findByUsername(principal.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            if (!currentUser.getUserId().equals(userId)) {
                return ResponseEntity.status(403).body("You can only refresh your own data.");
            }

            gfgService.refreshGfgData(userId);
            return ResponseEntity.ok(Map.of("message", "Sync attempted. Check console for status."));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", "Sync Failed: " + e.getMessage()));
        }
    }

    /**
     * THE BRIDGE: Receives real data from the browser and saves it.
     * Use this when the backend scraper is blocked by GFG.
     */
    @PostMapping("/manual-sync/{userId}")
    public ResponseEntity<?> manualSync(
            @PathVariable Long userId,
            @RequestBody Map<String, Integer> stats,
            Principal principal) {
        try {
            User currentUser = userRepository.findByUsername(principal.getName()).orElseThrow();
            if (!currentUser.getUserId().equals(userId))
                return ResponseEntity.status(403).build();

            StudentProfile profile = studentProfileRepository.findById(userId).orElseThrow();

            if (stats.containsKey("score"))    profile.setGfgCodingScore(stats.get("score"));
            if (stats.containsKey("solved"))   profile.setGfgSolved(stats.get("solved"));
            if (stats.containsKey("rank"))     profile.setGfgInstituteRank(stats.get("rank"));
            if (stats.containsKey("articles")) profile.setGfgArticles(stats.get("articles"));

            studentProfileRepository.save(profile);
            return ResponseEntity.ok(Map.of("message", "GFG stats saved!"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }
}