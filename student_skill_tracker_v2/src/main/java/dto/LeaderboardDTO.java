package dto;

public class LeaderboardDTO {
    private String studentName;
    private String department;
    private Integer leetcode;
    private Integer gfg;          // NEW: GFG Field
    private Integer github;
    private Integer hackerrank;
    private Integer certificates;
    private Integer linkedin;    
    private Integer dailyCoding; 
    private Integer totalScore;

    // UPDATED CONSTRUCTOR: Added 'Integer gfg' as the 4th parameter
    public LeaderboardDTO(String name, String dept, Integer lc, Integer gfg, Integer gh, Integer hr, Integer certs, Integer li, Integer daily) {
        this.studentName = name;
        this.department = dept;
        this.leetcode = lc != null ? lc : 0;
        this.gfg = gfg != null ? gfg : 0;      // NEW
        this.github = gh != null ? gh : 0;
        this.hackerrank = hr != null ? hr : 0;
        this.certificates = certs != null ? certs : 0;
        this.linkedin = li != null ? li : 0;
        this.dailyCoding = daily != null ? daily : 0;

        // UPDATED SCORING LOGIC: 
        // Including GFG (example: 2 points per GFG problem)
        this.totalScore = (this.leetcode * 1) + 
                          (this.gfg * 2) +      // Added GFG to the score
                          (this.github * 5) + 
                          (this.hackerrank * 10) + 
                          (this.certificates * 20) + 
                          (this.dailyCoding * 1);
    }

    // --- Getters ---
    public String getStudentName() { return studentName; }
    public String getDepartment() { return department; }
    public Integer getLeetcode() { return leetcode; }
    public Integer getGfg() { return gfg; }           // NEW
    public Integer getGithub() { return github; }
    public Integer getHackerrank() { return hackerrank; }
    public Integer getCertificates() { return certificates; }
    public Integer getLinkedin() { return linkedin; }
    public Integer getDailyCoding() { return dailyCoding; }
    public Integer getTotalScore() { return totalScore; }
}