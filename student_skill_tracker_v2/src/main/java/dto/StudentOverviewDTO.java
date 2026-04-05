package dto;

/**
 * Updated DTO to include GeeksforGeeks metrics and Class-wide averages.
 * This is the bridge that brings the 102 score and 46 solved count to the Staff Panel.
 */
public class StudentOverviewDTO {

    private Long userId;
    private String fullName;
    private String department;
    private String email;
    
    // Coding Metrics
    private Integer leetcodeSolved; 
    private String gfgUsername;
    private Integer gfgSolved;
    private Integer gfgCodingScore;
    private Integer gfgInstituteRank;

    // --- Getters and Setters ---
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public Integer getLeetcodeSolved() { return leetcodeSolved; }
    public void setLeetcodeSolved(Integer leetcodeSolved) { this.leetcodeSolved = leetcodeSolved; }

    // GFG Specific Getters and Setters
    public String getGfgUsername() { return gfgUsername; }
    public void setGfgUsername(String gfgUsername) { this.gfgUsername = gfgUsername; }

    public Integer getGfgSolved() { return gfgSolved; }
    public void setGfgSolved(Integer gfgSolved) { this.gfgSolved = gfgSolved; }

    public Integer getGfgCodingScore() { return gfgCodingScore; }
    public void setGfgCodingScore(Integer gfgCodingScore) { this.gfgCodingScore = gfgCodingScore; }

    public Integer getGfgInstituteRank() { return gfgInstituteRank; }
    public void setGfgInstituteRank(Integer gfgInstituteRank) { this.gfgInstituteRank = gfgInstituteRank; }
}