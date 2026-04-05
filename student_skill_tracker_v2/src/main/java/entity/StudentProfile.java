package entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "student_profiles")
public class StudentProfile {

    @Id
    @Column(name = "user_id")
    private Long userId;
    
    @OneToOne
    @MapsId
    @JoinColumn(name = "user_id", referencedColumnName = "user_id")
    private User user;

    private String leetcodeUsername;
    private String githubLink;
    private String hackerrankLink;
    private String linkedinLink;

    // --- GFG ANALYTICS FIELDS ---
    @Column(name = "gfg_username")
    private String gfgUsername;

    @Column(name = "gfg_solved")
    private Integer gfgSolved = 0;

    @Column(name = "gfg_coding_score")
    private Integer gfgCodingScore = 0;

    @Column(name = "gfg_institute_rank")
    private Integer gfgInstituteRank = 0;

    @Column(name = "gfg_articles")
    private Integer gfgArticles = 0;

    // --- APTITUDE & SOCIAL STATS ---
    @Column(name = "aptitude_score")
    private Integer aptitudeScore = 0;

    @Column(name = "linkedin_connections")
    private Integer linkedinConnections = 0;

    @Column(name = "linkedin_followers")
    private Integer linkedinFollowers = 0;

    // --- AGGREGATED STATS ---
    private Integer leetcodeSolved = 0;
    private Integer githubRepos = 0;
    private Integer hackerrankBadges = 0;
    private Integer certificatesCount = 0;

    @Column(name = "resume_path")
    private String resumePath;

    private String resumeStatus = "Not Uploaded";

    // --- Constructors ---
    public StudentProfile() {}

    public StudentProfile(User user, String leetcodeUsername, String githubLink, String hackerrankLink, String linkedinLink) {
        this.user = user;
        this.leetcodeUsername = leetcodeUsername;
        this.githubLink = githubLink;
        this.hackerrankLink = hackerrankLink;
        this.linkedinLink = linkedinLink;
    }

    // --- Getters and Setters ---
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getLeetcodeUsername() { return leetcodeUsername; }
    public void setLeetcodeUsername(String leetcodeUsername) { this.leetcodeUsername = leetcodeUsername; }

    public String getGithubLink() { return githubLink; }
    public void setGithubLink(String githubLink) { this.githubLink = githubLink; }

    public String getHackerrankLink() { return hackerrankLink; }
    public void setHackerrankLink(String hackerrankLink) { this.hackerrankLink = hackerrankLink; }

    public String getLinkedinLink() { return linkedinLink; }
    public void setLinkedinLink(String linkedinLink) { this.linkedinLink = linkedinLink; }

    public String getGfgUsername() { return gfgUsername; }
    public void setGfgUsername(String gfgUsername) { this.gfgUsername = gfgUsername; }

    public Integer getGfgSolved() { return gfgSolved; }
    public void setGfgSolved(Integer gfgSolved) { this.gfgSolved = gfgSolved; }

    public Integer getGfgCodingScore() { return gfgCodingScore; }
    public void setGfgCodingScore(Integer gfgCodingScore) { this.gfgCodingScore = gfgCodingScore; }

    public Integer getGfgInstituteRank() { return gfgInstituteRank; }
    public void setGfgInstituteRank(Integer gfgInstituteRank) { this.gfgInstituteRank = gfgInstituteRank; }

    public Integer getGfgArticles() { return gfgArticles; }
    public void setGfgArticles(Integer gfgArticles) { this.gfgArticles = gfgArticles; }

    public Integer getAptitudeScore() { return aptitudeScore; }
    public void setAptitudeScore(Integer aptitudeScore) { this.aptitudeScore = aptitudeScore; }

    public Integer getLinkedinConnections() { return linkedinConnections; }
    public void setLinkedinConnections(Integer linkedinConnections) { this.linkedinConnections = linkedinConnections; }

    public Integer getLinkedinFollowers() { return linkedinFollowers; }
    public void setLinkedinFollowers(Integer linkedinFollowers) { this.linkedinFollowers = linkedinFollowers; }

    public Integer getLeetcodeSolved() { return leetcodeSolved; }
    public void setLeetcodeSolved(Integer leetcodeSolved) { this.leetcodeSolved = leetcodeSolved; }

    public Integer getGithubRepos() { return githubRepos; }
    public void setGithubRepos(Integer githubRepos) { this.githubRepos = githubRepos; }

    public Integer getHackerrankBadges() { return hackerrankBadges; }
    public void setHackerrankBadges(Integer hackerrankBadges) { this.hackerrankBadges = hackerrankBadges; }

    public Integer getCertificatesCount() { return certificatesCount; }
    public void setCertificatesCount(Integer certificatesCount) { this.certificatesCount = certificatesCount; }

    public String getResumePath() { return resumePath; }
    public void setResumePath(String resumePath) { this.resumePath = resumePath; }

    public String getResumeStatus() { return resumeStatus; }
    public void setResumeStatus(String resumeStatus) { this.resumeStatus = resumeStatus; }
}