package service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import entity.StudentProfile;
import repository.StudentProfileRepository;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class GfgService {
    private static final Logger logger = LoggerFactory.getLogger(GfgService.class);

    @Autowired private StudentProfileRepository studentProfileRepository;
    @Autowired private CodingLogService codingLogService;

    @Transactional
    public void refreshGfgData(Long userId) {
        StudentProfile profile = studentProfileRepository.findById(userId).orElseThrow();
        String handle = profile.getGfgUsername().trim().replace("@", "");

        // KEY DISCOVERY: The data is on the "Coding Score" tab
        String url = "https://www.geeksforgeeks.org/profile/" + handle + "?tab=activity";

        try {
            Document doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                           "AppleWebKit/537.36 (KHTML, like Gecko) " +
                           "Chrome/124.0.0.0 Safari/537.36")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "en-US,en;q=0.9")
                .header("Accept-Encoding", "gzip, deflate, br")
                .header("Cache-Control", "no-cache")
                .referrer("https://www.google.com")
                .ignoreHttpErrors(true)
                .followRedirects(true)
                .timeout(20000)
                .get();

            String html = doc.html();
            String text = doc.text();

            logger.info("GFG page fetched for '{}'. Title: '{}'. HTML length: {}",
                handle, doc.title(), html.length());

            // === STRATEGY 1: Look for data in script tags (JSON state) ===
            int score  = 0;
            int solved = 0;
            int rank   = 0;

            Elements scripts = doc.select("script");
            for (Element script : scripts) {
                String src = script.data();
                if (src.contains("codingScore") || src.contains("totalProblemsSolved") 
                        || src.contains("instituteRank")) {

                    score  = extractInt(src, "codingScore");
                    solved = extractInt(src, "totalProblemsSolved");
                    rank   = extractInt(src, "instituteRank");

                    if (score > 0 || solved > 0) {
                        logger.info("STRATEGY 1 SUCCESS - Score:{} Solved:{} Rank:{}", 
                            score, solved, rank);
                        break;
                    }
                }
            }

            // === STRATEGY 2: Scrape visible text using patterns ===
            // The page shows "Coding Score\n102" and "Problems Solved\n46"
            if (score == 0 && solved == 0) {
                logger.info("Strategy 1 missed. Trying text pattern matching...");

                // Pattern: label followed by the number on next token
                score  = extractAfterLabel(text, "Coding Score");
                solved = extractAfterLabel(text, "Problems Solved");
                rank   = extractAfterLabel(text, "Institute Rank");

                if (score > 0 || solved > 0) {
                    logger.info("STRATEGY 2 SUCCESS - Score:{} Solved:{} Rank:{}", 
                        score, solved, rank);
                }
            }

            // === STRATEGY 3: Regex on raw HTML for any number pattern near label ===
            if (score == 0 && solved == 0) {
                logger.info("Strategy 2 missed. Trying raw HTML regex...");

                score  = extractInt(html, "codingScore");
                if (score == 0) score  = regexFind(html, 
                    "Coding\\s*Score[^\\d]{0,30}(\\d+)");

                solved = regexFind(html, 
                    "Problems\\s*Solved[^\\d]{0,30}(\\d+)");
                rank   = regexFind(html, 
                    "Institute\\s*Rank[^\\d]{0,30}(\\d+)");

                if (score > 0 || solved > 0) {
                    logger.info("STRATEGY 3 SUCCESS - Score:{} Solved:{} Rank:{}", 
                        score, solved, rank);
                }
            }

            // === FINAL: Save or log debug info ===
            if (score > 0 || solved > 0) {
                profile.setGfgCodingScore(score);
                profile.setGfgSolved(solved);
                profile.setGfgInstituteRank(rank);
                studentProfileRepository.saveAndFlush(profile);
                logger.info("SUCCESS! Saved GFG data for userId={} -> " +
                    "Score:{} Solved:{} Rank:{}", userId, score, solved, rank);
            } else {
                // === DEBUG: Log page text so we can see what GFG returned ===
                logger.warn("ALL strategies failed for handle='{}'", handle);
                logger.warn("Page title: '{}'", doc.title());
                logger.warn("First 300 chars of text: '{}'", 
                    text.substring(0, Math.min(300, text.length())));
                logger.warn("First 500 chars of HTML: '{}'", 
                    html.substring(0, Math.min(500, html.length())));
            }

        } catch (Exception e) {
            logger.error("GFG sync exception for userId={}: {}", userId, e.getMessage(), e);
        }
    }

    /** Extract integer value after a JSON key like "codingScore":102 */
    private int extractInt(String text, String key) {
        Pattern p = Pattern.compile("\"" + key + "\"\\s*:\\s*(\\d+)");
        Matcher m = p.matcher(text);
        if (m.find()) return Integer.parseInt(m.group(1));
        return 0;
    }

    /**
     * Extract number that appears right after a visible label in page text.
     * GFG renders: "Coding Score 102 Problems Solved 46 Institute Rank 200"
     */
    private int extractAfterLabel(String text, String label) {
        // Escape label for regex
        String escaped = label.replace(" ", "\\s+");
        Pattern p = Pattern.compile(escaped + "\\s+(\\d+)", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(text);
        if (m.find()) return Integer.parseInt(m.group(1));
        return 0;
    }

    /** General regex finder on HTML */
    private int regexFind(String html, String regex) {
        try {
            Pattern p = Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
            Matcher m = p.matcher(html);
            if (m.find()) return Integer.parseInt(m.group(1));
        } catch (Exception ignored) {}
        return 0;
    }
}