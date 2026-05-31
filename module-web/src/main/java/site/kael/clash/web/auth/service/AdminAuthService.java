package site.kael.clash.web.auth.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;
import site.kael.clash.common.exception.BusinessException;
import site.kael.clash.web.auth.dto.AuthStatusResponse;
import site.kael.clash.web.auth.dto.LoginRequest;
import site.kael.clash.web.auth.dto.SetupRequest;
import site.kael.clash.web.auth.model.AdminAccount;
import site.kael.clash.web.auth.model.PasswordHash;
import site.kael.clash.web.auth.repository.AdminAccountRepository;
import site.kael.clash.web.auth.security.PasswordHasher;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class AdminAuthService {
    public static final String SESSION_AUTHENTICATED = "ADMIN_AUTHENTICATED";
    public static final String SESSION_USERNAME = "ADMIN_USERNAME";

    private final AdminAccountRepository repository;
    private final PasswordHasher passwordHasher;

    public AdminAuthService(AdminAccountRepository repository, PasswordHasher passwordHasher) {
        this.repository = repository;
        this.passwordHasher = passwordHasher;
    }

    public AuthStatusResponse status(HttpSession session) {
        Optional<AdminAccount> account = repository.find();
        boolean initialized = account.isPresent();
        boolean authenticated = session != null && Boolean.TRUE.equals(session.getAttribute(SESSION_AUTHENTICATED));
        String username = initialized && authenticated ? (String) session.getAttribute(SESSION_USERNAME) : null;
        return new AuthStatusResponse(initialized, initialized && authenticated, username);
    }

    public synchronized void setup(SetupRequest request) {
        if (request == null) {
            throw new BusinessException(400, "请求不能为空");
        }
        if (repository.exists()) {
            throw new BusinessException(409, "管理员已初始化");
        }
        String username = trimToEmpty(request.getUsername());
        if (username.isEmpty()) {
            throw new BusinessException(400, "用户名不能为空");
        }
        String password = request.getPassword();
        if (password == null || password.length() < 8) {
            throw new BusinessException(400, "密码至少需要 8 位");
        }
        if (!password.equals(request.getConfirmPassword())) {
            throw new BusinessException(400, "两次输入的密码不一致");
        }
        LocalDateTime now = LocalDateTime.now();
        PasswordHash passwordHash = passwordHasher.hash(password);
        repository.save(new AdminAccount(username, passwordHash, now, now));
    }

    public AuthStatusResponse login(LoginRequest request, HttpServletRequest servletRequest) {
        if (request == null) {
            throw new BusinessException(400, "请求不能为空");
        }
        AdminAccount account = repository.find()
                .orElseThrow(() -> new BusinessException(409, "请先初始化管理员"));
        String username = trimToEmpty(request.getUsername());
        String password = request.getPassword() == null ? "" : request.getPassword();
        if (!account.getUsername().equals(username) || !passwordHasher.matches(password, account.getPasswordHash())) {
            throw new BusinessException(401, "用户名或密码错误");
        }
        HttpSession session = servletRequest.getSession(true);
        servletRequest.changeSessionId();
        session.setAttribute(SESSION_AUTHENTICATED, Boolean.TRUE);
        session.setAttribute(SESSION_USERNAME, account.getUsername());
        return new AuthStatusResponse(true, true, account.getUsername());
    }

    public void logout(HttpSession session) {
        if (session != null) {
            session.invalidate();
        }
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
