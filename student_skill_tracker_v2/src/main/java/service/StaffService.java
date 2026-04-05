package service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.Comparator; 
import java.util.Map; 
import java.util.HashMap; 

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dto.DashboardDTO;
import dto.StaffSettingsDTO; 
import dto.StudentOverviewDTO;
import entity.LeetCodeStats;
import entity.User;
import entity.HackerRankStats; 
import entity.StudentProfile; 
import entity.Certificate;   
import entity.Resume;
import repository.LeetCodeStatsRepository;
import repository.UserRepository;
import repository.HackerRankStatsRepository; 
import repository.StudentProfileRepository; 
import repository.CertificateRepository;   
import repository.ResumeRepository;

import dto.StudentReportDTO; 
import dto.DepartmentGrowthDTO; 
import dto.StudentMasterDTO; 

@Service
public class StaffService {

    @Autowired private UserRepository userRepository;
    @Autowired private StudentService studentService; 
    @Autowired private LeetCodeStatsRepository leetCodeStatsRepository;
    @Autowired private HackerRankStatsRepository hackerRankStatsRepository; 
    @Autowired private StudentProfileRepository studentProfileRepository; 
    @Autowired private CertificateRepository certificateRepository; 
    @Autowired private PasswordEncoder passwordEncoder; 

    // --- FIX 1: UPDATE OVERVIEW TO INCLUDE GFG DATA ---
    public List<StudentOverviewDTO> getAllStudentsOverview() {
        List<User> students = userRepository.findStudentUsersForReport();

        return students.stream()
            .map(user -> {
                StudentOverviewDTO dto = new StudentOverviewDTO();
                dto.setUserId(user.getUserId());
                dto.setFullName(user.getFullName());
                dto.setDepartment(user.getDepartment());
                dto.setEmail(user.getEmail());

                // Fetch LeetCode
                Optional<LeetCodeStats> stats = leetCodeStatsRepository.findByUserId(user.getUserId());
                dto.setLeetcodeSolved(stats.map(LeetCodeStats::getTotalSolved).orElse(0));

                // Fetch GFG Data from StudentProfile
                studentProfileRepository.findByUserId(user.getUserId()).ifPresent(profile -> {
                    dto.setGfgUsername(profile.getGfgUsername());
                    dto.setGfgSolved(profile.getGfgSolved() != null ? profile.getGfgSolved() : 0);
                    dto.setGfgCodingScore(profile.getGfgCodingScore() != null ? profile.getGfgCodingScore() : 0);
                    dto.setGfgInstituteRank(profile.getGfgInstituteRank() != null ? profile.getGfgInstituteRank() : 0);
                });

                return dto;
            })
            .collect(Collectors.toList());
    }

    // --- FIX 2: UPDATE DASHBOARD METRICS FOR GFG ---
    public DashboardDTO getStaffDashboardMetrics() {
        DashboardDTO dto = new DashboardDTO();

        List<User> students = userRepository.findStudentUsersForReport();
        List<LeetCodeStats> allLcStats = leetCodeStatsRepository.findAll();
        List<Certificate> allCerts = certificateRepository.findAll();
        List<StudentProfile> allProfiles = studentProfileRepository.findAll();

        dto.setTotalStudents(students.size());
        
        // Avg LeetCode Score
        double averageScore = allLcStats.stream()
            .mapToInt(s -> calculateOverallGrade(s.getTotalSolved() != null ? s.getTotalSolved() : 0))
            .average()
            .orElse(0.0);
        dto.setClassAverageCodingScore((int) Math.round(averageScore));

        // NEW: Avg GFG Score for the dashboard
        double averageGfg = allProfiles.stream()
            .mapToInt(p -> p.getGfgCodingScore() != null ? p.getGfgCodingScore() : 0)
            .average()
            .orElse(0.0);
        dto.setClassAverageGfgScore((int) Math.round(averageGfg));

        // Pending Certs
        long pendingCerts = allCerts.stream()
            .filter(c -> c.getStatus() != null && c.getStatus().name().equals("PENDING"))
            .count();
        dto.setPendingCertsCount((int) pendingCerts);

        // GitHub Repos
        dto.setClassTotalGitHubRepos(allProfiles.stream()
            .mapToInt(p -> p.getGithubRepos() != null ? p.getGithubRepos() : 0).sum());

        // Leaderboard (Now uses the updated getAllStudentsOverview with GFG data)
        List<StudentOverviewDTO> studentOverview = getAllStudentsOverview();
        List<StudentOverviewDTO> sortedStudents = studentOverview.stream()
            .sorted(Comparator.comparingInt(StudentOverviewDTO::getLeetcodeSolved).reversed())
            .limit(5) 
            .collect(Collectors.toList());

        dto.setTopPerformers(sortedStudents);
        if (!sortedStudents.isEmpty()) {
            dto.setTopPerformerName(sortedStudents.get(0).getFullName());
        }

        // Skill Distribution
        Map<String, Long> skillDistribution = new HashMap<>();
        skillDistribution.put("Easy", allLcStats.stream().mapToLong(s -> s.getEasySolved() != null ? s.getEasySolved() : 0).sum());
        skillDistribution.put("Medium", allLcStats.stream().mapToLong(s -> s.getMediumSolved() != null ? s.getMediumSolved() : 0).sum());
        skillDistribution.put("Hard", allLcStats.stream().mapToLong(s -> s.getHardSolved() != null ? s.getHardSolved() : 0).sum());
        dto.setSkillDistribution(skillDistribution);

        return dto;
    }

    public DashboardDTO getStudentDashboardData(Long studentId) {
        User user = userRepository.findById(studentId).orElseThrow(() -> new RuntimeException("Student not found."));
        DashboardDTO dto = new DashboardDTO();
        dto.setUserId(studentId);
        dto.setFullName(user.getFullName());
        dto.setDepartment(user.getDepartment());

        leetCodeStatsRepository.findByUserId(studentId).ifPresent(lc -> {
            dto.setLeetCodeStats(lc);
            dto.setOverallGrade(calculateOverallGrade(lc.getTotalSolved() != null ? lc.getTotalSolved() : 0));
        });
        hackerRankStatsRepository.findByUserId(studentId).ifPresent(dto::setHackerRankStats);
        studentProfileRepository.findByUserId(studentId).ifPresent(dto::setStudentProfile);
        dto.setCertificateList(certificateRepository.findByUserId(studentId));
        
        if (dto.getOverallGrade() == null) dto.setOverallGrade(calculateOverallGrade(0)); 
        return dto;
    }

    private Integer calculateOverallGrade(int leetcodeSolved) {
        return Math.min(leetcodeSolved * 2, 1000);
    }

    // [Rest of your reporting methods...]
    public List<StudentReportDTO> getMonthlySkillReportData() {
        List<User> students = userRepository.findStudentUsersForReport();
        return students.stream().map(user -> {
            StudentReportDTO dto = new StudentReportDTO(); 
            dto.setFullName(user.getFullName());
            dto.setDepartment(user.getDepartment());
            int totalSolved = leetCodeStatsRepository.findByUserId(user.getUserId()).map(s -> s.getTotalSolved()).orElse(0);
            dto.setLeetcodeSolved(totalSolved);
            dto.setVerifiedCertificates((int) certificateRepository.findByUserId(user.getUserId()).stream().filter(c -> c.getStatus().name().equals("VERIFIED")).count());
            dto.setOverallGrade(calculateOverallGrade(totalSolved));
            return dto;
        }).collect(Collectors.toList());
    }

    public List<DepartmentGrowthDTO> getDepartmentGrowthData() {
        List<User> students = userRepository.findStudentUsersForReport();
        Map<String, List<User>> studentsByDept = students.stream().collect(Collectors.groupingBy(User::getDepartment));
        return studentsByDept.entrySet().stream().map(entry -> {
            DepartmentGrowthDTO dto = new DepartmentGrowthDTO();
            dto.setDepartmentName(entry.getKey());
            dto.setTotalStudents(entry.getValue().size());
            double avg = entry.getValue().stream().mapToInt(u -> calculateOverallGrade(leetCodeStatsRepository.findByUserId(u.getUserId()).map(s -> s.getTotalSolved()).orElse(0))).average().orElse(0.0);
            dto.setCurrentAverageGrade((int) Math.round(avg));
            return dto;
        }).collect(Collectors.toList());
    }

    public List<StudentMasterDTO> getFullStudentMasterData() {
        List<User> students = userRepository.findStudentUsersForReport();
        return students.stream().map(user -> {
            StudentMasterDTO dto = new StudentMasterDTO(); 
            dto.setFullName(user.getFullName());
            dto.setDepartment(user.getDepartment());
            dto.setEmail(user.getEmail());
            leetCodeStatsRepository.findByUserId(user.getUserId()).ifPresent(lc -> dto.setTotalLeetCodeSolved(lc.getTotalSolved() != null ? lc.getTotalSolved() : 0));
            studentProfileRepository.findByUserId(user.getUserId()).ifPresent(p -> {
                dto.setTotalGitHubRepos(p.getGithubRepos() != null ? p.getGithubRepos() : 0);
                dto.setResumeStatus(p.getResumeStatus() != null ? p.getResumeStatus() : "Not Uploaded");
            });
            return dto;
        }).collect(Collectors.toList());
    }

    public StaffSettingsDTO getStaffProfile(String username) {
        User user = userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("Staff not found."));
        return new StaffSettingsDTO(user.getFullName(), user.getEmail());
    }

    @Transactional
    public void updateStaffProfile(String username, StaffSettingsDTO settingsDto) {
        User user = userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("Staff not found."));
        if (settingsDto.getFullName() != null) user.setFullName(settingsDto.getFullName());
        if (settingsDto.getEmail() != null) { user.setEmail(settingsDto.getEmail()); user.setUsername(settingsDto.getEmail()); }
        if (settingsDto.getPassword() != null && settingsDto.getPassword().length() >= 6) {
            user.setPassword(passwordEncoder.encode(settingsDto.getPassword()));
        }
        userRepository.save(user);
    }
}