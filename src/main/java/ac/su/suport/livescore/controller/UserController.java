package ac.su.suport.livescore.controller;

import ac.su.suport.livescore.constant.UserRole;
import ac.su.suport.livescore.domain.User;
import ac.su.suport.livescore.dto.UserDTO;
import ac.su.suport.livescore.dto.LoginRequest;
import ac.su.suport.livescore.exception.CustomException;
import ac.su.suport.livescore.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final HttpSession session;

    public UserController(UserService userService, HttpSession session) {
        this.userService = userService;
        this.session = session;
    }

    @PostMapping("/register")
    public ResponseEntity<String> registerUser(@RequestBody UserDTO userDTO) {
        User registeredUser = userService.registerUser(userDTO);
        return ResponseEntity.ok("회원가입이 정상적으로 완료되었습니다, " + registeredUser.getUsername() + " 님.");
    }

    @GetMapping("/checkEmail")
    public ResponseEntity<Boolean> checkEmail(@RequestParam String email) {
        return ResponseEntity.ok(userService.isEmailTaken(email));
    }

    @GetMapping("/checkNickname")
    public ResponseEntity<Boolean> checkNickname(@RequestParam String nickname) {
        return ResponseEntity.ok(userService.isNicknameTaken(nickname));
    }

    @PostMapping("/sendVerificationCode")
    public ResponseEntity<String> sendVerificationCode(@RequestParam String email) {
        try {
            userService.sendVerificationCode(email);
            return ResponseEntity.ok("인증 코드가 전송되었습니다.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/verifyCode")
    public ResponseEntity<Boolean> verifyCode(@RequestParam String email, @RequestParam String code) {
        return ResponseEntity.ok(userService.verifyCode(email, code));
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody LoginRequest loginRequest) {
        User user = userService.authenticate(loginRequest.getEmail(), loginRequest.getPassword());
        if (user == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "유효하지 않은 이메일 혹은 비밀번호입니다."));
        }

        session.setAttribute("currentUser", user);

        Map<String, String> response = new HashMap<>();
        response.put("message", "로그인 성공");
        response.put("role", user.getRole().name());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout() {
        session.invalidate();
        return ResponseEntity.ok("로그아웃 성공");
    }

    @DeleteMapping("/delete")
    public ResponseEntity<String> deleteUser() {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return ResponseEntity.status(401).body("사용자가 인증되지 않았습니다.");
        }

        userService.deleteUser(currentUser);
        session.invalidate();
        return ResponseEntity.ok("회원탈퇴 성공");
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser() {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return ResponseEntity.status(401).build();
        }

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("username", currentUser.getUsername());
        userInfo.put("email", currentUser.getEmail());
        userInfo.put("nickname", currentUser.getNickname());
        userInfo.put("role", currentUser.getRole().name());

        return ResponseEntity.ok(userInfo);
    }

    @PostMapping("/update")
    public ResponseEntity<String> updateUser(@RequestBody UserDTO userDTO) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return ResponseEntity.status(401).body("사용자가 인증되지 않았습니다.");
        }

        try {
            userService.updateUser(userDTO, currentUser);
            session.setAttribute("currentUser", currentUser); // 업데이트 후 세션 갱신
            return ResponseEntity.ok("사용자 정보가 업데이트되었습니다.");
        } catch (CustomException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/forgotPassword")
    public ResponseEntity<String> forgotPassword(@RequestParam String email) {
        try {
            userService.resetPassword(email);
            return ResponseEntity.ok("새로운 비밀번호가 이메일로 전송되었습니다.");
        } catch (CustomException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
