package dto;

import java.util.List;
import java.util.Map;
import entity.LeetCodeStats;
import entity.HackerRankStats;
import entity.StudentProfile; 
import entity.Certificate;   

public class DashboardDTO {
    
    // --- 1. Personal & Identity Info ---
    private Long userId; 
    private String fullName;
    private String studentName; 
    private String department;
    private String lastSynced;
    private String gfgUsername;

    // --- 2. Individual Coding Stats ---
    private Integer leetcodeSolved = 0;
    private Integer gfgSolved = 0;           
    private Integer gfgCodingScore = 0;      
    private Integer gfgInstituteRank = 0;    
    private Integer gfgArticles = 0;         
    private Integer hackerRankBadges = 0;
    private Integer githubRepos = 0;
    private Integer totalDailyCoding = 0; 
    private Integer aptitudeScore = 0;       
    
    // --- 3. Professional Status ---
    private Integer certificatesCount = 0;
    private String resumeStatus = "Not Uploaded";
    private Integer overallGrade = 0; 

    // --- 4. Nested Detailed Objects ---
    private LeetCodeStats leetCodeStats;
    private HackerRankStats hackerRankStats;
    private StudentProfile studentProfile; 
    private List<Certificate> certificateList; 
    private DailyCodingDTO dailyCodingData; 

    // --- 5. Class/Leaderboard Stats ---
    private Integer totalStudents = 0;
    private Integer classTotalLeetCode = 0;
    private Integer classTotalCertificates = 0;
    private Integer classTotalGitHubRepos = 0;
    private String topPerformerName = "N/A";
    private Integer myRank = 0;
    private String mostActiveDept = "N/A";
    
    // --- 6. Staff/Admin Metrics (FIXED HERE) ---
    private Integer classAverageCodingScore = 0;
    private Integer classAverageGfgScore = 0;  // ADDED: For Staff Dashboard
    private Integer classAverageLcSolved = 0;  // ADDED: For Staff Dashboard
    private Integer pendingCertsCount = 0;
    private List<StudentOverviewDTO> topPerformers; 
    private Map<String, Long> skillDistribution;

    // --- Constructors ---
    public DashboardDTO() {}

    // --- Getters and Setters ---
    
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    
    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    
    public String getLastSynced() { return lastSynced; }
    public void setLastSynced(String lastSynced) { this.lastSynced = lastSynced; }
    
    public String getGfgUsername() { return gfgUsername; }
    public void setGfgUsername(String gfgUsername) { this.gfgUsername = gfgUsername; }

    public Integer getLeetcodeSolved() { return leetcodeSolved; }
    public void setLeetcodeSolved(Integer leetcodeSolved) { this.leetcodeSolved = leetcodeSolved; }

    public Integer getGfgSolved() { return gfgSolved; }
    public void setGfgSolved(Integer gfgSolved) { this.gfgSolved = gfgSolved; }

    public Integer getGfgCodingScore() { return gfgCodingScore; }
    public void setGfgCodingScore(Integer gfgCodingScore) { this.gfgCodingScore = gfgCodingScore; }

    public Integer getGfgInstituteRank() { return gfgInstituteRank; }
    public void setGfgInstituteRank(Integer gfgInstituteRank) { this.gfgInstituteRank = gfgInstituteRank; }

    public Integer getGfgArticles() { return gfgArticles; }
    public void setGfgArticles(Integer gfgArticles) { this.gfgArticles = gfgArticles; }

    public Integer getGithubRepos() { return githubRepos; }
    public void setGithubRepos(Integer githubRepos) { this.githubRepos = githubRepos; }
    
    public Integer getHackerRankBadges() { return hackerRankBadges; }
    public void setHackerRankBadges(Integer hackerRankBadges) { this.hackerRankBadges = hackerRankBadges; }

    public Integer getCertificatesCount() { return certificatesCount; }
    public void setCertificatesCount(Integer certificatesCount) { this.certificatesCount = certificatesCount; }

    public String getResumeStatus() { return resumeStatus; }
    public void setResumeStatus(String resumeStatus) { this.resumeStatus = resumeStatus; }

    public Integer getTotalDailyCoding() { return totalDailyCoding; }
    public void setTotalDailyCoding(Integer totalDailyCoding) { this.totalDailyCoding = totalDailyCoding; }

    public Integer getAptitudeScore() { return aptitudeScore; }
    public void setAptitudeScore(Integer aptitudeScore) { this.aptitudeScore = aptitudeScore; }

    public LeetCodeStats getLeetCodeStats() { return leetCodeStats; }
    public void setLeetCodeStats(LeetCodeStats leetCodeStats) { this.leetCodeStats = leetCodeStats; }

    public HackerRankStats getHackerRankStats() { return hackerRankStats; }
    public void setHackerRankStats(HackerRankStats hackerRankStats) { this.hackerRankStats = hackerRankStats; }

    public StudentProfile getStudentProfile() { return studentProfile; }
    public void setStudentProfile(StudentProfile studentProfile) { this.studentProfile = studentProfile; }
    
    public List<Certificate> getCertificateList() { return certificateList; }
    public void setCertificateList(List<Certificate> certificateList) { this.certificateList = certificateList; }
    
    public Integer getOverallGrade() { return overallGrade; }
    public void setOverallGrade(Integer overallGrade) { this.overallGrade = overallGrade; }

    public DailyCodingDTO getDailyCodingData() { return dailyCodingData; }
    public void setDailyCodingData(DailyCodingDTO dailyCodingData) { this.dailyCodingData = dailyCodingData; }
    
    public Integer getTotalStudents() { return totalStudents; }
    public void setTotalStudents(Integer totalStudents) { this.totalStudents = totalStudents; }

    public Integer getClassTotalLeetCode() { return classTotalLeetCode; }
    public void setClassTotalLeetCode(Integer classTotalLeetCode) { this.classTotalLeetCode = classTotalLeetCode; }

    public Integer getClassTotalCertificates() { return classTotalCertificates; }
    public void setClassTotalCertificates(Integer classTotalCertificates) { this.classTotalCertificates = classTotalCertificates; }

    public String getTopPerformerName() { return topPerformerName; }
    public void setTopPerformerName(String topPerformerName) { this.topPerformerName = topPerformerName; }

    public Integer getMyRank() { return myRank; }
    public void setMyRank(Integer myRank) { this.myRank = myRank; }

    public String getMostActiveDept() { return mostActiveDept; }
    public void setMostActiveDept(String mostActiveDept) { this.mostActiveDept = mostActiveDept; }
    
    public Integer getClassAverageCodingScore() { return classAverageCodingScore; }
    public void setClassAverageCodingScore(Integer classAverageCodingScore) { this.classAverageCodingScore = classAverageCodingScore; }

    // NEW GETTERS AND SETTERS FOR AVERAGES
    public Integer getClassAverageGfgScore() { return classAverageGfgScore; }
    public void setClassAverageGfgScore(Integer classAverageGfgScore) { this.classAverageGfgScore = classAverageGfgScore; }

    public Integer getClassAverageLcSolved() { return classAverageLcSolved; }
    public void setClassAverageLcSolved(Integer classAverageLcSolved) { this.classAverageLcSolved = classAverageLcSolved; }

    public List<StudentOverviewDTO> getTopPerformers() { return topPerformers; }
    public void setTopPerformers(List<StudentOverviewDTO> topPerformers) { this.topPerformers = topPerformers; }

    public Map<String, Long> getSkillDistribution() { return skillDistribution; }
    public void setSkillDistribution(Map<String, Long> skillDistribution) { this.skillDistribution = skillDistribution; }

    public Integer getPendingCertsCount() { return pendingCertsCount; }
    public void setPendingCertsCount(Integer pendingCertsCount) { this.pendingCertsCount = pendingCertsCount; }

    public Integer getClassTotalGitHubRepos() { return classTotalGitHubRepos; }
    public void setClassTotalGitHubRepos(Integer classTotalGitHubRepos) { this.classTotalGitHubRepos = classTotalGitHubRepos; }
}