package service;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dto.DailyCodingDTO;
import dto.DashboardDTO;
import dto.LeaderboardDTO;
import dto.StudentSettingsDTO;
import entity.StudentProfile;
import entity.User;
import repository.CertificateRepository;
import repository.CodingLogRepository;
import repository.LeetCodeStatsRepository;
import repository.StudentProfileRepository;
import repository.UserRepository;

@Service
public class StudentService {

    @Autowired private UserRepository userRepository;
    @Autowired private StudentProfileRepository studentProfileRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private LeetCodeStatsRepository leetCodeStatsRepository;
    @Autowired private CertificateRepository certificateRepository;
    @Autowired private CodingLogRepository codingLogRepository;
    @Autowired private CodingLogService codingLogService;

    @Autowired private LeetCodeService leetCodeService;
    @Autowired private HackerRankService hackerRankService;
    @Autowired private GitHubService gitHubService;
    @Autowired private GfgService gfgService; 

    /**
     * Fetches all data required for the Student Dashboard.
     * Maps the new GFG Analytics fields to the DTO.
     */
    public DashboardDTO getDashboardData(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found."));
        StudentProfile profile = studentProfileRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("Profile not found."));

        DashboardDTO dto = new DashboardDTO();

        // 1. Personal Identity Data
        dto.setStudentName(user.getFullName());
        dto.setUserId(userId);
        dto.setDepartment(user.getDepartment());
        
        // --- GFG ANALYTICS MAPPING ---
        dto.setGfgUsername(profile.getGfgUsername());      
        dto.setGfgSolved(profile.getGfgSolved());          
        dto.setGfgCodingScore(profile.getGfgCodingScore());    // Maps new Score
        dto.setGfgInstituteRank(profile.getGfgInstituteRank());// Maps new Rank
        dto.setGfgArticles(profile.getGfgArticles());          // Maps new Articles
        dto.setAptitudeScore(profile.getAptitudeScore());  
        
        // 2. Coding Stats
        int myLc = leetCodeStatsRepository.findByUserId(userId).map(s -> s.getTotalSolved()).orElse(0);
        dto.setLeetcodeSolved(myLc);
        dto.setGithubRepos(profile.getGithubRepos());
        dto.setHackerRankBadges(profile.getHackerrankBadges());
        dto.setCertificatesCount(certificateRepository.findByUserId(userId).size());
        dto.setResumeStatus(profile.getResumeStatus());

        DailyCodingDTO dailyStats = codingLogService.getStudentStats(userId);
        dto.setTotalDailyCoding(dailyStats.getTodayCount());

        // 3. Class-Wide Metrics
        dto.setTotalStudents((int) studentProfileRepository.count());
        int classLc = leetCodeStatsRepository.findAll().stream().mapToInt(s -> s.getTotalSolved()).sum();
        dto.setClassTotalLeetCode(classLc);
        dto.setClassTotalCertificates((int) certificateRepository.count());

        // 4. Leaderboard & Ranking logic (uses user.getFullName() for match)
        List<LeaderboardDTO> leaderboard = getLeaderboard();
        if (!leaderboard.isEmpty()) {
            dto.setTopPerformerName(leaderboard.get(0).getStudentName());
        } else {
            dto.setTopPerformerName("N/A");
        }

        int rank = 0;
        for (int i = 0; i < leaderboard.size(); i++) {
            if (leaderboard.get(i).getStudentName().equals(user.getFullName())) {
                rank = i + 1;
                break;
            }
        }
        dto.setMyRank(rank > 0 ? rank : leaderboard.size() + 1);
        dto.setMostActiveDept("CSE");

        return dto;
    }

    /**
     * Generates the Class Leaderboard.
     */
    public List<LeaderboardDTO> getLeaderboard() {
        return studentProfileRepository.findAll().stream()
            .map(profile -> {
                Long uid = profile.getUserId();
                int liveLc = leetCodeStatsRepository.findByUserId(uid).map(s -> s.getTotalSolved()).orElse(0);
                int liveCerts = certificateRepository.findByUserId(uid).size();
                int todayCount = codingLogService.getStudentStats(uid).getTodayCount();

                return new LeaderboardDTO(
                    profile.getUser().getFullName(),
                    profile.getUser().getDepartment(),
                    liveLc,
                    profile.getGfgSolved(), 
                    profile.getGithubRepos(),
                    profile.getHackerrankBadges(),
                    liveCerts,
                    profile.getLinkedinConnections(),
                    todayCount
                );
            })
            .sorted(Comparator.comparingInt(LeaderboardDTO::getTotalScore).reversed())
            .limit(10)
            .collect(Collectors.toList());
    }

    // (Keep your refreshAllStudentsData, getSettings, and updateSettings as they are)
    // ...
    /**
     * Async background sync for all student coding data.
     */
    public void refreshAllStudentsData() {
        List<User> students = userRepository.findStudentUsersForReport();
        List<CompletableFuture<Void>> futures = students.stream()
            .map(user -> CompletableFuture.runAsync(() -> {
                Long userId = user.getUserId();
                try { leetCodeService.refreshLeetCodeData(userId); } catch (Exception e) {}
                try { gitHubService.refreshGitHubData(userId); } catch (Exception e) {}
                try { hackerRankService.refreshHackerRankData(userId); } catch (Exception e) {}
                try { gfgService.refreshGfgData(userId); } catch (Exception e) {} 
            }))
            .collect(Collectors.toList());

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        System.out.println("✅ Global Sync loop finished.");
    }

    // --- Settings Management ---
    public StudentSettingsDTO getSettings(Long userId) {
        User user = userRepository.findById(userId).orElseThrow();
        StudentProfile profile = studentProfileRepository.findById(userId).orElseThrow();
        StudentSettingsDTO dto = new StudentSettingsDTO();

        dto.setFullName(user.getFullName());
        dto.setEmail(user.getEmail());
        dto.setDepartment(user.getDepartment());
        dto.setLeetcodeUsername(profile.getLeetcodeUsername());
        dto.setGithubLink(profile.getGithubLink());
        dto.setHackerrankLink(profile.getHackerrankLink());
        dto.setLinkedinLink(profile.getLinkedinLink());
        dto.setLinkedinConnections(profile.getLinkedinConnections());
        dto.setLinkedinFollowers(profile.getLinkedinFollowers());
        
        dto.setGfgUsername(profile.getGfgUsername());
        dto.setAptitudeScore(profile.getAptitudeScore());

        return dto;
    }

    @Transactional
    public void updateSettings(Long userId, StudentSettingsDTO settingsDto) {
        User user = userRepository.findById(userId).orElseThrow();
        StudentProfile profile = studentProfileRepository.findById(userId).orElseThrow();
        
        user.setFullName(settingsDto.getFullName());
        user.setEmail(settingsDto.getEmail());
        user.setDepartment(settingsDto.getDepartment());
        userRepository.save(user);

        profile.setLeetcodeUsername(settingsDto.getLeetcodeUsername());
        profile.setGithubLink(settingsDto.getGithubLink());
        profile.setHackerrankLink(settingsDto.getHackerrankLink());
        profile.setLinkedinLink(settingsDto.getLinkedinLink());
        
        profile.setGfgUsername(settingsDto.getGfgUsername());
        if(settingsDto.getAptitudeScore() != null) {
            profile.setAptitudeScore(settingsDto.getAptitudeScore());
        }

        if(settingsDto.getLinkedinConnections() != null) {
            profile.setLinkedinConnections(settingsDto.getLinkedinConnections());
        }
        if(settingsDto.getLinkedinFollowers() != null) {
            profile.setLinkedinFollowers(settingsDto.getLinkedinFollowers());
        }
        
        studentProfileRepository.save(profile);
    }

    @Transactional
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        User user = userRepository.findById(userId).orElseThrow();
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new RuntimeException("Current password is incorrect.");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
}