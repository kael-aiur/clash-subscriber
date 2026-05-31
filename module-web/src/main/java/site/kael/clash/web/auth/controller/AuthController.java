package site.kael.clash.web.auth.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import site.kael.clash.web.auth.dto.AuthStatusResponse;
import site.kael.clash.web.auth.dto.LoginRequest;
import site.kael.clash.web.auth.dto.SetupRequest;
import site.kael.clash.web.auth.service.AdminAuthService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AdminAuthService authService;

    public AuthController(AdminAuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/status")
    public ResponseEntity<AuthStatusResponse> status(HttpServletRequest request) {
        return ResponseEntity.ok(authService.status(request.getSession(false)));
    }

    @PostMapping("/setup")
    public ResponseEntity<Void> setup(@RequestBody(required = false) SetupRequest request) {
        authService.setup(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/login")
    public ResponseEntity<AuthStatusResponse> login(@RequestBody(required = false) LoginRequest request, HttpServletRequest servletRequest) {
        return ResponseEntity.ok(authService.login(request, servletRequest));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        authService.logout(request.getSession(false));
        return ResponseEntity.ok().build();
    }
}
