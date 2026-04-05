package service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dto.LoginDTO;
import dto.RegisterDTO;
import entity.StudentProfile;
import entity.User;
import repository.StudentProfileRepository;
import repository.UserRepository;
import security.JwtUtil;

@Service
public class AuthService {

    @Autowired private UserRepository userRepository;
    @Autowired private StudentProfileRepository studentProfileRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtUtil jwtUtil;

    @Autowired private LeetCodeService leetCodeService;
    @Autowired private GfgService gfgService; // NEW: Inject GfgService

    /**
     * Registers a new student and creates their associated profile.
     */
    @Transactional
    public User registerUser(RegisterDTO dto) {

        // 1. Validation
        if (userRepository.existsByUsername(dto.getUsername())) {
            throw new RuntimeException("Error: Username is already taken.");
        }
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new RuntimeException("Error: Email already in use.");
        }

        // 2. Create User Entity
        User user = new User();
        user.setFullName(dto.getFullname());
        user.setDepartment(dto.getDepartment());
        user.setEmail(dto.getEmail());
        user.setUsername(dto.getUsername());
        user.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        user.setDateOfBirth(dto.getDob());
        
        // Handle Role (Default to STUDENT if null)
        user.setRole(dto.getRole() != null ? User.Role.valueOf(dto.getRole()) : User.Role.STUDENT);
        user.setRegistrationDate(LocalDateTime.now());

        User savedUser = userRepository.save(user);

        // 3. Create StudentProfile entity (Updated with GFG and Aptitude)
        StudentProfile profile = new StudentProfile();
        profile.setUser(savedUser);
        profile.setLeetcodeUsername(dto.getLeetcodeUsername());
        profile.setGithubLink(dto.getGithubLink());
        profile.setHackerrankLink(dto.getHackerrankUsername());
        profile.setLinkedinLink(dto.getLinkedinLink());
        
        // NEW: Map GFG and Aptitude from DTO to Profile
        profile.setGfgUsername(dto.getGfgUsername());
        profile.setAptitudeScore(dto.getAptitudeScore() != null ? dto.getAptitudeScore() : 0);

        studentProfileRepository.save(profile);

        // NEW: Initial Scrape for GFG data immediately after registration
        if (profile.getGfgUsername() != null && !profile.getGfgUsername().isEmpty()) {
            try {
                gfgService.refreshGfgData(savedUser.getUserId());
            } catch (Exception e) {
                System.err.println("Initial GFG fetch failed: " + e.getMessage());
            }
        }

        return savedUser;
    }

    /**
     * Authenticates a user and returns a JWT token, Role, and UserID.
     */
    public Map<String, Object> loginUser(LoginDTO loginDTO) {

        User user = userRepository.findByUsername(loginDTO.getUsername())
            .orElseThrow(() -> new RuntimeException("Invalid username or password."));

        if (!passwordEncoder.matches(loginDTO.getPassword(), user.getPasswordHash())) {
            throw new RuntimeException("Invalid username or password.");
        }

        // Auto-refresh data on login
        if (user.getRole() == User.Role.STUDENT) {
            try {
                leetCodeService.refreshLeetCodeData(user.getUserId());
                gfgService.refreshGfgData(user.getUserId()); // NEW: Refresh GFG on login
                System.out.println("Auto-refreshed coding stats for user: " + user.getUsername());
            } catch (Exception e) {
                System.err.println("Warning: Could not auto-refresh coding data: " + e.getMessage());
            }
        }

        // Generate Token
        String token = jwtUtil.generateToken(user.getUsername(), user.getRole().name());

        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("role", user.getRole().name());
        response.put("userId", user.getUserId());

        return response;
    }
}