# 登录功能与首次管理员初始化 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 Clash 订阅管理中心增加单管理员登录保护，并在首次打开时通过统一认证页面引导初始化管理员账号。

**Architecture:** 后端在 `module-web` 内实现轻量自定义认证：JSON 文件保存单管理员账号，PBKDF2 保存密码哈希，HTTP Session 保存浏览器会话级登录态，Spring MVC `HandlerInterceptor` 保护 `/api/**` 管理接口。前端新增 `/auth` 页面、认证 API、路由守卫和全局 401 处理，登录后保持原后台页面结构并提供退出入口。

**Tech Stack:** Java 21、Spring Boot 3.2.5、Spring MVC、Jackson、JDK PBKDF2、JUnit 5、MockMvc、Vue 3、Vue Router、Element Plus、Axios、TypeScript、Vite。

---

## File Structure

后端新增文件：

- Create: `module-web/src/main/java/site/kael/clash/web/auth/model/PasswordHash.java` — 保存 PBKDF2 算法、salt、hash、迭代次数和 key 长度。
- Create: `module-web/src/main/java/site/kael/clash/web/auth/model/AdminAccount.java` — 单管理员账号模型。
- Create: `module-web/src/main/java/site/kael/clash/web/auth/repository/AdminAccountRepository.java` — 管理员账号仓库接口。
- Create: `module-web/src/main/java/site/kael/clash/web/auth/repository/AdminAccountReadException.java` — 管理员文件存在但无法读取/解析时的异常。
- Create: `module-web/src/main/java/site/kael/clash/web/auth/repository/JsonFileAdminAccountRepository.java` — 读写 `data/admin/admin.json`。
- Create: `module-web/src/main/java/site/kael/clash/web/auth/security/PasswordHasher.java` — PBKDF2 生成与验证。
- Create: `module-web/src/main/java/site/kael/clash/web/auth/dto/AuthStatusResponse.java` — 状态响应。
- Create: `module-web/src/main/java/site/kael/clash/web/auth/dto/SetupRequest.java` — 初始化请求。
- Create: `module-web/src/main/java/site/kael/clash/web/auth/dto/LoginRequest.java` — 登录请求。
- Create: `module-web/src/main/java/site/kael/clash/web/auth/service/AdminAuthService.java` — 初始化、登录、登出、状态查询。
- Create: `module-web/src/main/java/site/kael/clash/web/auth/controller/AuthController.java` — `/api/auth/**` 控制器。
- Create: `module-web/src/main/java/site/kael/clash/web/auth/interceptor/AuthInterceptor.java` — 管理 API Session 门禁。
- Test: `module-web/src/test/java/site/kael/clash/web/auth/AdminAuthIntegrationTest.java` — 端到端认证行为测试。

后端修改文件：

- Modify: `module-web/src/main/java/site/kael/clash/web/config/WebConfig.java` — 注入并注册认证拦截器。

前端新增文件：

- Create: `module-web/frontend/src/api/auth.ts` — 认证 API 封装和认证接口路径识别。
- Create: `module-web/frontend/src/views/AuthView.vue` — 统一初始化/登录页面。
- Create: `module-web/frontend/src/auth/session.ts` — 轻量前端认证状态缓存。

前端修改文件：

- Modify: `module-web/frontend/src/router/index.ts` — 新增 `/auth` 路由与认证守卫。
- Modify: `module-web/frontend/src/api/index.ts` — Axios 401 处理。
- Modify: `module-web/frontend/src/App.vue` — 登录页不显示后台布局，后台 header 增加用户名与退出按钮。
- Modify: `module-web/frontend/src/style.css` — 认证页布局样式。

项目文件：

- Modify: `.gitignore` — 增加 `.superpowers/`，避免提交 brainstorming 视觉辅助产物。

---

### Task 1: 后端认证模型、仓库与密码哈希

**Files:**
- Create: `module-web/src/main/java/site/kael/clash/web/auth/model/PasswordHash.java`
- Create: `module-web/src/main/java/site/kael/clash/web/auth/model/AdminAccount.java`
- Create: `module-web/src/main/java/site/kael/clash/web/auth/repository/AdminAccountRepository.java`
- Create: `module-web/src/main/java/site/kael/clash/web/auth/repository/AdminAccountReadException.java`
- Create: `module-web/src/main/java/site/kael/clash/web/auth/repository/JsonFileAdminAccountRepository.java`
- Create: `module-web/src/main/java/site/kael/clash/web/auth/security/PasswordHasher.java`
- Test later in Task 6 through `AdminAuthIntegrationTest`

- [ ] **Step 1: 创建 `PasswordHash` 模型**

Create `module-web/src/main/java/site/kael/clash/web/auth/model/PasswordHash.java`:

```java
package site.kael.clash.web.auth.model;

public class PasswordHash {
    private String algorithm;
    private String salt;
    private String hash;
    private int iterations;
    private int keyLength;

    public PasswordHash() {
    }

    public PasswordHash(String algorithm, String salt, String hash, int iterations, int keyLength) {
        this.algorithm = algorithm;
        this.salt = salt;
        this.hash = hash;
        this.iterations = iterations;
        this.keyLength = keyLength;
    }

    public String getAlgorithm() { return algorithm; }
    public void setAlgorithm(String algorithm) { this.algorithm = algorithm; }
    public String getSalt() { return salt; }
    public void setSalt(String salt) { this.salt = salt; }
    public String getHash() { return hash; }
    public void setHash(String hash) { this.hash = hash; }
    public int getIterations() { return iterations; }
    public void setIterations(int iterations) { this.iterations = iterations; }
    public int getKeyLength() { return keyLength; }
    public void setKeyLength(int keyLength) { this.keyLength = keyLength; }
}
```

- [ ] **Step 2: 创建 `AdminAccount` 模型**

Create `module-web/src/main/java/site/kael/clash/web/auth/model/AdminAccount.java`:

```java
package site.kael.clash.web.auth.model;

import java.time.LocalDateTime;

public class AdminAccount {
    private String username;
    private PasswordHash passwordHash;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public AdminAccount() {
    }

    public AdminAccount(String username, PasswordHash passwordHash, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public PasswordHash getPasswordHash() { return passwordHash; }
    public void setPasswordHash(PasswordHash passwordHash) { this.passwordHash = passwordHash; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
```

- [ ] **Step 3: 创建仓库接口和读取异常**

Create `module-web/src/main/java/site/kael/clash/web/auth/repository/AdminAccountRepository.java`:

```java
package site.kael.clash.web.auth.repository;

import site.kael.clash.web.auth.model.AdminAccount;

import java.util.Optional;

public interface AdminAccountRepository {
    Optional<AdminAccount> find();

    boolean exists();

    AdminAccount save(AdminAccount account);
}
```

Create `module-web/src/main/java/site/kael/clash/web/auth/repository/AdminAccountReadException.java`:

```java
package site.kael.clash.web.auth.repository;

public class AdminAccountReadException extends RuntimeException {
    public AdminAccountReadException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

- [ ] **Step 4: 创建 JSON 文件仓库实现**

Create `module-web/src/main/java/site/kael/clash/web/auth/repository/JsonFileAdminAccountRepository.java`:

```java
package site.kael.clash.web.auth.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import site.kael.clash.web.auth.model.AdminAccount;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

@Repository
public class JsonFileAdminAccountRepository implements AdminAccountRepository {
    private final ObjectMapper objectMapper;
    private final File accountFile;

    public JsonFileAdminAccountRepository(@Value("${data.path:data}") String dataPath) {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        File dir = new File(dataPath + "/admin");
        dir.mkdirs();
        this.accountFile = new File(dir, "admin.json");
    }

    @Override
    public Optional<AdminAccount> find() {
        if (!accountFile.exists()) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(accountFile, AdminAccount.class));
        } catch (IOException e) {
            throw new AdminAccountReadException("管理员账号文件无法读取", e);
        }
    }

    @Override
    public boolean exists() {
        return accountFile.exists();
    }

    @Override
    public AdminAccount save(AdminAccount account) {
        try {
            objectMapper.writeValue(accountFile, account);
            return account;
        } catch (IOException e) {
            throw new RuntimeException("保存管理员账号失败", e);
        }
    }
}
```

- [ ] **Step 5: 创建 PBKDF2 密码工具**

Create `module-web/src/main/java/site/kael/clash/web/auth/security/PasswordHasher.java`:

```java
package site.kael.clash.web.auth.security;

import org.springframework.stereotype.Component;
import site.kael.clash.web.auth.model.PasswordHash;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class PasswordHasher {
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 210_000;
    private static final int KEY_LENGTH = 256;
    private static final int SALT_LENGTH = 16;

    private final SecureRandom secureRandom = new SecureRandom();

    public PasswordHash hash(String password) {
        byte[] salt = new byte[SALT_LENGTH];
        secureRandom.nextBytes(salt);
        byte[] hash = pbkdf2(password, salt, ITERATIONS, KEY_LENGTH, ALGORITHM);
        return new PasswordHash(
                ALGORITHM,
                Base64.getEncoder().encodeToString(salt),
                Base64.getEncoder().encodeToString(hash),
                ITERATIONS,
                KEY_LENGTH
        );
    }

    public boolean matches(String password, PasswordHash stored) {
        byte[] salt = Base64.getDecoder().decode(stored.getSalt());
        byte[] expected = Base64.getDecoder().decode(stored.getHash());
        byte[] actual = pbkdf2(password, salt, stored.getIterations(), stored.getKeyLength(), stored.getAlgorithm());
        return MessageDigest.isEqual(expected, actual);
    }

    private byte[] pbkdf2(String password, byte[] salt, int iterations, int keyLength, String algorithm) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, keyLength);
            return SecretKeyFactory.getInstance(algorithm).generateSecret(spec).getEncoded();
        } catch (Exception e) {
            throw new IllegalStateException("密码哈希计算失败", e);
        }
    }
}
```

- [ ] **Step 6: 编译后端模块**

Run:

```bash
mvn -pl module-web -am test -DskipTests
```

Expected: Maven 编译成功，新增类没有语法错误。

- [ ] **Step 7: Commit**

```bash
git add module-web/src/main/java/site/kael/clash/web/auth/model module-web/src/main/java/site/kael/clash/web/auth/repository module-web/src/main/java/site/kael/clash/web/auth/security
git commit -m "feat: 添加管理员认证基础模型"
```

---

### Task 2: 后端认证服务、DTO 与认证接口

**Files:**
- Create: `module-web/src/main/java/site/kael/clash/web/auth/dto/AuthStatusResponse.java`
- Create: `module-web/src/main/java/site/kael/clash/web/auth/dto/SetupRequest.java`
- Create: `module-web/src/main/java/site/kael/clash/web/auth/dto/LoginRequest.java`
- Create: `module-web/src/main/java/site/kael/clash/web/auth/service/AdminAuthService.java`
- Create: `module-web/src/main/java/site/kael/clash/web/auth/controller/AuthController.java`
- Uses existing: `module-common/src/main/java/site/kael/clash/common/exception/BusinessException.java`

- [ ] **Step 1: 创建认证 DTO**

Create `module-web/src/main/java/site/kael/clash/web/auth/dto/AuthStatusResponse.java`:

```java
package site.kael.clash.web.auth.dto;

public class AuthStatusResponse {
    private boolean initialized;
    private boolean authenticated;
    private String username;

    public AuthStatusResponse() {
    }

    public AuthStatusResponse(boolean initialized, boolean authenticated, String username) {
        this.initialized = initialized;
        this.authenticated = authenticated;
        this.username = username;
    }

    public boolean isInitialized() { return initialized; }
    public void setInitialized(boolean initialized) { this.initialized = initialized; }
    public boolean isAuthenticated() { return authenticated; }
    public void setAuthenticated(boolean authenticated) { this.authenticated = authenticated; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
}
```

Create `module-web/src/main/java/site/kael/clash/web/auth/dto/SetupRequest.java`:

```java
package site.kael.clash.web.auth.dto;

public class SetupRequest {
    private String username;
    private String password;
    private String confirmPassword;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getConfirmPassword() { return confirmPassword; }
    public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }
}
```

Create `module-web/src/main/java/site/kael/clash/web/auth/dto/LoginRequest.java`:

```java
package site.kael.clash.web.auth.dto;

public class LoginRequest {
    private String username;
    private String password;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
```

- [ ] **Step 2: 创建认证服务**

Create `module-web/src/main/java/site/kael/clash/web/auth/service/AdminAuthService.java`:

```java
package site.kael.clash.web.auth.service;

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
        boolean initialized = repository.exists();
        boolean authenticated = Boolean.TRUE.equals(session.getAttribute(SESSION_AUTHENTICATED));
        String username = authenticated ? (String) session.getAttribute(SESSION_USERNAME) : null;
        return new AuthStatusResponse(initialized, initialized && authenticated, username);
    }

    public void setup(SetupRequest request) {
        if (repository.exists()) {
            throw new BusinessException(409, "管理员已初始化");
        }
        String username = trimToEmpty(request.getUsername());
        if (username.isEmpty()) {
            throw new BusinessException(400, "用户名不能为空");
        }
        if (request.getPassword() == null || request.getPassword().length() < 8) {
            throw new BusinessException(400, "密码至少需要 8 位");
        }
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException(400, "两次输入的密码不一致");
        }
        LocalDateTime now = LocalDateTime.now();
        PasswordHash passwordHash = passwordHasher.hash(request.getPassword());
        repository.save(new AdminAccount(username, passwordHash, now, now));
    }

    public AuthStatusResponse login(LoginRequest request, HttpSession session) {
        AdminAccount account = repository.find()
                .orElseThrow(() -> new BusinessException(409, "请先初始化管理员"));
        String username = trimToEmpty(request.getUsername());
        String password = request.getPassword() == null ? "" : request.getPassword();
        if (!account.getUsername().equals(username) || !passwordHasher.matches(password, account.getPasswordHash())) {
            throw new BusinessException(401, "用户名或密码错误");
        }
        session.setAttribute(SESSION_AUTHENTICATED, Boolean.TRUE);
        session.setAttribute(SESSION_USERNAME, account.getUsername());
        return new AuthStatusResponse(true, true, account.getUsername());
    }

    public void logout(HttpSession session) {
        session.invalidate();
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
```

- [ ] **Step 3: 创建认证控制器**

Create `module-web/src/main/java/site/kael/clash/web/auth/controller/AuthController.java`:

```java
package site.kael.clash.web.auth.controller;

import jakarta.servlet.http.HttpSession;
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
    public ResponseEntity<AuthStatusResponse> status(HttpSession session) {
        return ResponseEntity.ok(authService.status(session));
    }

    @PostMapping("/setup")
    public ResponseEntity<Void> setup(@RequestBody SetupRequest request) {
        authService.setup(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/login")
    public ResponseEntity<AuthStatusResponse> login(@RequestBody LoginRequest request, HttpSession session) {
        return ResponseEntity.ok(authService.login(request, session));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpSession session) {
        authService.logout(session);
        return ResponseEntity.ok().build();
    }
}
```

- [ ] **Step 4: 编译认证接口**

Run:

```bash
mvn -pl module-web -am test -DskipTests
```

Expected: 编译成功。

- [ ] **Step 5: Commit**

```bash
git add module-web/src/main/java/site/kael/clash/web/auth/dto module-web/src/main/java/site/kael/clash/web/auth/service module-web/src/main/java/site/kael/clash/web/auth/controller
git commit -m "feat: 添加管理员认证接口"
```

---

### Task 3: 后端管理 API 门禁

**Files:**
- Create: `module-web/src/main/java/site/kael/clash/web/auth/interceptor/AuthInterceptor.java`
- Modify: `module-web/src/main/java/site/kael/clash/web/config/WebConfig.java`

- [ ] **Step 1: 创建认证拦截器**

Create `module-web/src/main/java/site/kael/clash/web/auth/interceptor/AuthInterceptor.java`:

```java
package site.kael.clash.web.auth.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import site.kael.clash.web.auth.service.AdminAuthService;

@Component
public class AuthInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String path = request.getRequestURI();
        if (path.startsWith("/api/auth/")) {
            return true;
        }
        HttpSession session = request.getSession(false);
        if (session != null && Boolean.TRUE.equals(session.getAttribute(AdminAuthService.SESSION_AUTHENTICATED))) {
            return true;
        }
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":401,\"message\":\"未登录\"}");
        return false;
    }
}
```

- [ ] **Step 2: 更新 Web MVC 配置注册拦截器**

Modify `module-web/src/main/java/site/kael/clash/web/config/WebConfig.java` to:

```java
package site.kael.clash.web.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import site.kael.clash.web.auth.interceptor.AuthInterceptor;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final AuthInterceptor authInterceptor;

    public WebConfig(AuthInterceptor authInterceptor) {
        this.authInterceptor = authInterceptor;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*");
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/auth/**");
    }
}
```

- [ ] **Step 3: 编译门禁配置**

Run:

```bash
mvn -pl module-web -am test -DskipTests
```

Expected: 编译成功，Spring context 可注入 `AuthInterceptor`。

- [ ] **Step 4: Commit**

```bash
git add module-web/src/main/java/site/kael/clash/web/auth/interceptor/AuthInterceptor.java module-web/src/main/java/site/kael/clash/web/config/WebConfig.java
git commit -m "feat: 添加管理 API 登录门禁"
```

---

### Task 4: 后端认证集成测试

**Files:**
- Create: `module-web/src/test/java/site/kael/clash/web/auth/AdminAuthIntegrationTest.java`

- [ ] **Step 1: 写集成测试**

Create `module-web/src/test/java/site/kael/clash/web/auth/AdminAuthIntegrationTest.java`:

```java
package site.kael.clash.web.auth;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "data.path=target/test-data/auth")
class AdminAuthIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void statusReturnsUninitializedWhenAdminFileMissing() throws Exception {
        deleteAdminFile();

        mockMvc.perform(get("/api/auth/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.initialized", is(false)))
                .andExpect(jsonPath("$.authenticated", is(false)));
    }

    @Test
    void setupCreatesAdminWithoutLoggingIn() throws Exception {
        deleteAdminFile();

        mockMvc.perform(post("/api/auth/setup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"password123\",\"confirmPassword\":\"password123\"}"))
                .andExpect(status().isOk())
                .andExpect(cookie().doesNotExist("JSESSIONID"));

        mockMvc.perform(get("/api/auth/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.initialized", is(true)))
                .andExpect(jsonPath("$.authenticated", is(false)));
    }

    @Test
    void setupRejectsShortPassword() throws Exception {
        deleteAdminFile();

        mockMvc.perform(post("/api/auth/setup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"short\",\"confirmPassword\":\"short\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("密码至少需要 8 位")));
    }

    @Test
    void setupRejectsDuplicateInitialization() throws Exception {
        deleteAdminFile();
        setupAdmin();

        mockMvc.perform(post("/api/auth/setup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin2\",\"password\":\"password456\",\"confirmPassword\":\"password456\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void loginCreatesSessionAndLogoutInvalidatesIt() throws Exception {
        deleteAdminFile();
        setupAdmin();

        MvcResult login = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"password123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.initialized", is(true)))
                .andExpect(jsonPath("$.authenticated", is(true)))
                .andExpect(jsonPath("$.username", is("admin")))
                .andReturn();

        Cookie session = login.getResponse().getCookie("JSESSIONID");

        mockMvc.perform(get("/api/auth/status").cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated", is(true)));

        mockMvc.perform(post("/api/auth/logout").cookie(session))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/auth/status").cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.initialized", is(true)))
                .andExpect(jsonPath("$.authenticated", is(false)));
    }

    @Test
    void loginRejectsWrongPasswordAndUninitializedLogin() throws Exception {
        deleteAdminFile();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"password123\"}"))
                .andExpect(status().isConflict());

        setupAdmin();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"wrongpass\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", is("用户名或密码错误")));
    }

    @Test
    void protectedApiRequiresLoginAndAllowsAuthenticatedSession() throws Exception {
        deleteAdminFile();
        setupAdmin();

        mockMvc.perform(get("/api/subscriptions"))
                .andExpect(status().isUnauthorized());

        MvcResult login = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"password123\"}"))
                .andExpect(status().isOk())
                .andReturn();

        Cookie session = login.getResponse().getCookie("JSESSIONID");

        mockMvc.perform(get("/api/subscriptions").cookie(session))
                .andExpect(status().isOk());
    }

    @Test
    void corruptedAdminFileIsNotTreatedAsUninitialized() throws Exception {
        Path adminDir = Path.of("target/test-data/auth/admin");
        Files.createDirectories(adminDir);
        Files.writeString(adminDir.resolve("admin.json"), "not-json");

        mockMvc.perform(get("/api/auth/status"))
                .andExpect(status().isInternalServerError());

        mockMvc.perform(post("/api/auth/setup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"password123\",\"confirmPassword\":\"password123\"}"))
                .andExpect(status().isConflict());
    }

    private void setupAdmin() throws Exception {
        mockMvc.perform(post("/api/auth/setup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"password123\",\"confirmPassword\":\"password123\"}"))
                .andExpect(status().isOk());
    }

    private void deleteAdminFile() throws Exception {
        Path adminFile = Path.of("target/test-data/auth/admin/admin.json");
        Files.deleteIfExists(adminFile);
    }
}
```

- [ ] **Step 2: 运行测试并确认失败点**

Run:

```bash
mvn -pl module-web -Dtest=AdminAuthIntegrationTest test
```

Expected after Tasks 1-3: all tests PASS. If `corruptedAdminFileIsNotTreatedAsUninitialized` returns 200, update service status path to call `repository.find()` instead of only `exists()` when `exists()` is true.

- [ ] **Step 3: Fix corrupted-file status if needed**

If Step 2 shows corrupted file returns 200, modify `AdminAuthService.status`:

```java
public AuthStatusResponse status(HttpSession session) {
    boolean initialized = repository.find().isPresent();
    boolean authenticated = Boolean.TRUE.equals(session.getAttribute(SESSION_AUTHENTICATED));
    String username = authenticated ? (String) session.getAttribute(SESSION_USERNAME) : null;
    return new AuthStatusResponse(initialized, initialized && authenticated, username);
}
```

Expected: 损坏 JSON 会通过 `AdminAccountReadException` 进入全局异常处理，返回 500。

- [ ] **Step 4: 运行认证测试**

Run:

```bash
mvn -pl module-web -Dtest=AdminAuthIntegrationTest test
```

Expected: `Tests run: 8, Failures: 0, Errors: 0`。

- [ ] **Step 5: Commit**

```bash
git add module-web/src/test/java/site/kael/clash/web/auth/AdminAuthIntegrationTest.java module-web/src/main/java/site/kael/clash/web/auth/service/AdminAuthService.java
git commit -m "test: 覆盖管理员认证流程"
```

---

### Task 5: 前端认证 API、状态与统一认证页

**Files:**
- Create: `module-web/frontend/src/api/auth.ts`
- Create: `module-web/frontend/src/auth/session.ts`
- Create: `module-web/frontend/src/views/AuthView.vue`

- [ ] **Step 1: 创建认证 API 封装**

Create `module-web/frontend/src/api/auth.ts`:

```ts
import api from './index'

export interface AuthStatus {
  initialized: boolean
  authenticated: boolean
  username?: string
}

export interface SetupPayload {
  username: string
  password: string
  confirmPassword: string
}

export interface LoginPayload {
  username: string
  password: string
}

export const authApi = {
  async status(): Promise<AuthStatus> {
    const response = await api.get<AuthStatus>('/auth/status')
    return response.data
  },
  async setup(payload: SetupPayload): Promise<void> {
    await api.post('/auth/setup', payload)
  },
  async login(payload: LoginPayload): Promise<AuthStatus> {
    const response = await api.post<AuthStatus>('/auth/login', payload)
    return response.data
  },
  async logout(): Promise<void> {
    await api.post('/auth/logout')
  },
}

export function isAuthEndpoint(url?: string): boolean {
  return Boolean(url?.startsWith('/auth/'))
}
```

- [ ] **Step 2: 创建前端认证状态缓存**

Create `module-web/frontend/src/auth/session.ts`:

```ts
import { reactive } from 'vue'

export const authSession = reactive({
  initialized: false,
  authenticated: false,
  username: '',
})

export function updateAuthSession(status: { initialized: boolean; authenticated: boolean; username?: string }) {
  authSession.initialized = status.initialized
  authSession.authenticated = status.authenticated
  authSession.username = status.username || ''
}

export function clearAuthSession() {
  authSession.authenticated = false
  authSession.username = ''
}
```

- [ ] **Step 3: 创建认证页面**

Create `module-web/frontend/src/views/AuthView.vue` with this structure:

```vue
<script setup lang="ts">
import { computed, reactive, ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { authApi } from '@/api/auth'
import { updateAuthSession } from '@/auth/session'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const initialized = ref(true)

const setupForm = reactive({ username: '', password: '', confirmPassword: '' })
const loginForm = reactive({ username: '', password: '' })

const isSetupMode = computed(() => !initialized.value)
const title = computed(() => isSetupMode.value ? '首次使用，创建管理员' : '管理员登录')

const refreshStatus = async () => {
  const status = await authApi.status()
  initialized.value = status.initialized
  updateAuthSession(status)
  if (status.authenticated) {
    await router.replace((route.query.redirect as string) || '/subscriptions')
  }
}

const submitSetup = async () => {
  if (!setupForm.username.trim()) {
    ElMessage.error('用户名不能为空')
    return
  }
  if (setupForm.password.length < 8) {
    ElMessage.error('密码至少需要 8 位')
    return
  }
  if (setupForm.password !== setupForm.confirmPassword) {
    ElMessage.error('两次输入的密码不一致')
    return
  }
  loading.value = true
  try {
    await authApi.setup({ ...setupForm, username: setupForm.username.trim() })
    ElMessage.success('管理员初始化成功，请登录')
    initialized.value = true
    loginForm.username = setupForm.username.trim()
    loginForm.password = ''
  } finally {
    loading.value = false
  }
}

const submitLogin = async () => {
  if (!loginForm.username.trim() || !loginForm.password) {
    ElMessage.error('请输入用户名和密码')
    return
  }
  loading.value = true
  try {
    const status = await authApi.login({ username: loginForm.username.trim(), password: loginForm.password })
    updateAuthSession(status)
    await router.replace((route.query.redirect as string) || '/subscriptions')
  } finally {
    loading.value = false
  }
}

onMounted(refreshStatus)
</script>

<template>
  <div class="auth-page">
    <div class="auth-card">
      <h1>Clash 订阅管理中心</h1>
      <p>{{ title }}</p>

      <el-form v-if="isSetupMode" label-position="top" @submit.prevent>
        <el-form-item label="用户名">
          <el-input v-model="setupForm.username" autocomplete="username" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="setupForm.password" type="password" autocomplete="new-password" show-password />
        </el-form-item>
        <el-form-item label="确认密码">
          <el-input v-model="setupForm.confirmPassword" type="password" autocomplete="new-password" show-password />
        </el-form-item>
        <el-button type="primary" :loading="loading" class="auth-submit" @click="submitSetup">创建管理员</el-button>
      </el-form>

      <el-form v-else label-position="top" @submit.prevent>
        <el-form-item label="用户名">
          <el-input v-model="loginForm.username" autocomplete="username" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="loginForm.password" type="password" autocomplete="current-password" show-password @keyup.enter="submitLogin" />
        </el-form-item>
        <el-button type="primary" :loading="loading" class="auth-submit" @click="submitLogin">登录</el-button>
      </el-form>
    </div>
  </div>
</template>
```

- [ ] **Step 4: 运行前端类型检查**

Run:

```bash
cd module-web/frontend && npm run build
```

Expected: 此时可能因路由未注册而仍能构建；如果 `@/auth/session` 路径解析失败，确认 `vite.config.ts` 已有 `@` alias。若没有 alias，改用相对路径导入。

- [ ] **Step 5: Commit**

```bash
git add module-web/frontend/src/api/auth.ts module-web/frontend/src/auth/session.ts module-web/frontend/src/views/AuthView.vue
git commit -m "feat(frontend): 添加认证页面基础能力"
```

---

### Task 6: 前端路由守卫、Axios 401 与后台退出

**Files:**
- Modify: `module-web/frontend/src/router/index.ts`
- Modify: `module-web/frontend/src/api/index.ts`
- Modify: `module-web/frontend/src/App.vue`
- Modify: `module-web/frontend/src/style.css`

- [ ] **Step 1: 更新 Axios 401 处理**

Modify `module-web/frontend/src/api/index.ts` to:

```ts
import axios from 'axios'
import router from '@/router'
import { isAuthEndpoint } from './auth'
import { clearAuthSession } from '@/auth/session'

const api = axios.create({
  baseURL: '/api',
  timeout: 30000,
})

api.interceptors.response.use(
  (response) => response,
  (error) => {
    const message = error.response?.data?.message || error.message || '请求失败'
    console.error('API 错误:', message)
    if (error.response?.status === 401 && !isAuthEndpoint(error.config?.url)) {
      clearAuthSession()
      const current = router.currentRoute.value
      router.replace({ path: '/auth', query: { redirect: current.fullPath } })
    }
    return Promise.reject(error)
  }
)

export default api
```

- [ ] **Step 2: 更新路由与守卫**

Modify `module-web/frontend/src/router/index.ts` to include `/auth` and guard:

```ts
import { createRouter, createWebHashHistory } from 'vue-router'
import { authApi } from '@/api/auth'
import { updateAuthSession } from '@/auth/session'

const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    { path: '/auth', name: 'auth', component: () => import('@/views/AuthView.vue'), meta: { public: true } },
    { path: '/', redirect: '/subscriptions' },
    { path: '/subscriptions', name: 'subscriptions', component: () => import('@/views/SubscriptionView.vue') },
    { path: '/mihomo-instances', name: 'mihomo-instances', component: () => import('@/views/MihomoInstanceView.vue') },
    { path: '/mihomo-instances/:id', name: 'MihomoInstanceDetail', component: () => import('@/views/MihomoInstanceDetailView.vue'), meta: { title: 'Mihomo 实例详情' } },
    { path: '/scheduled-tasks', name: 'scheduled-tasks', component: () => import('@/views/ScheduledTaskView.vue') },
    { path: '/scripts', name: 'scripts', component: () => import('@/views/ScriptView.vue') },
    { path: '/build-pipelines', name: 'build-pipelines', component: () => import('@/views/BuildPipelineView.vue') },
  ],
})

router.beforeEach(async (to) => {
  if (to.meta.public) {
    return true
  }
  const status = await authApi.status()
  updateAuthSession(status)
  if (!status.initialized || !status.authenticated) {
    return { path: '/auth', query: { redirect: to.fullPath } }
  }
  return true
})

export default router
```

- [ ] **Step 3: 更新后台布局 header**

Modify `module-web/frontend/src/App.vue` script section:

```vue
<script setup lang="ts">
import { useRouter, useRoute } from 'vue-router'
import { computed } from 'vue'
import { ElMessage } from 'element-plus'
import { authSession, clearAuthSession } from '@/auth/session'
import { authApi } from '@/api/auth'

const router = useRouter()
const route = useRoute()

const menuItems = [
  { path: '/subscriptions', label: '订阅源管理', icon: 'Link' },
  { path: '/mihomo-instances', label: 'Mihomo 实例', icon: 'Monitor' },
  { path: '/build-pipelines', label: '构建流程', icon: 'SetUp' },
  { path: '/scripts', label: '脚本管理', icon: 'Document' },
]

const isAuthPage = computed(() => route.path === '/auth')
const currentTitle = computed(() => {
  const item = menuItems.find(m => m.path === route.path)
  return item?.label || 'Clash 订阅管理中心'
})

const handleMenuSelect = (path: string) => {
  router.push(path)
}

const handleLogout = async () => {
  await authApi.logout()
  clearAuthSession()
  ElMessage.success('已退出登录')
  await router.replace('/auth')
}
</script>
```

Modify `module-web/frontend/src/App.vue` template to wrap auth page separately:

```vue
<template>
  <router-view v-if="isAuthPage" />
  <div v-else class="layout">
    <div class="sidebar">
      <div class="logo">Clash 订阅中心</div>
      <el-menu
        :default-active="route.path"
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409eff"
        @select="handleMenuSelect"
      >
        <el-menu-item v-for="item in menuItems" :key="item.path" :index="item.path">
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.label }}</span>
        </el-menu-item>
      </el-menu>
    </div>
    <div class="main">
      <div class="header">
        <span>{{ currentTitle }}</span>
        <div class="header-actions">
          <span class="header-user">{{ authSession.username }}</span>
          <el-button size="small" @click="handleLogout">退出登录</el-button>
        </div>
      </div>
      <div class="content">
        <router-view />
      </div>
    </div>
  </div>
</template>
```

- [ ] **Step 4: 更新样式**

Append to `module-web/frontend/src/style.css`:

```css
.layout .main .header {
  justify-content: space-between;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-user {
  font-size: 14px;
  color: #606266;
}

.auth-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f0f2f5;
}

.auth-card {
  width: 360px;
  padding: 32px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 8px 24px rgba(0, 21, 41, 0.12);
}

.auth-card h1 {
  margin: 0 0 8px;
  font-size: 22px;
  text-align: center;
}

.auth-card p {
  margin: 0 0 24px;
  color: #606266;
  text-align: center;
}

.auth-submit {
  width: 100%;
}
```

- [ ] **Step 5: 运行前端构建**

Run:

```bash
cd module-web/frontend && npm run build
```

Expected: `vue-tsc -b && vite build` 成功。

- [ ] **Step 6: Commit**

```bash
git add module-web/frontend/src/router/index.ts module-web/frontend/src/api/index.ts module-web/frontend/src/App.vue module-web/frontend/src/style.css
git commit -m "feat(frontend): 接入登录守卫和退出入口"
```

---

### Task 7: 全量验证与 OpenSpec 收尾

**Files:**
- Modify: `.gitignore`
- Existing artifacts: `openspec/changes/add-login-admin-init/tasks.md`

- [ ] **Step 1: 忽略 `.superpowers/` 临时产物**

Modify `.gitignore` by adding under `### Claude Code ###`:

```gitignore
.superpowers/
```

- [ ] **Step 2: 运行后端测试**

Run:

```bash
mvn test
```

Expected: 全部 Maven 测试通过。

- [ ] **Step 3: 运行前端构建**

Run:

```bash
cd module-web/frontend && npm run build
```

Expected: TypeScript 检查和 Vite 构建通过。

- [ ] **Step 4: 启动应用进行浏览器验证**

Run:

```bash
mvn -pl module-web -am spring-boot:run
```

Expected: 应用在 `http://localhost:31192` 启动。

Manual checks:

1. 删除或临时移动 `data/admin/admin.json`，打开 `http://localhost:31192`。
2. 看到认证页初始化表单。
3. 输入 `admin` / `password123` / `password123`，提交后看到初始化成功提示并切换登录表单。
4. 不登录时直接访问订阅源页面，应回到认证页。
5. 输入 `admin` / `password123` 登录，进入 `/subscriptions`。
6. 刷新页面后仍保持当前浏览器会话。
7. 点击退出登录后回到认证页。
8. 退出后通过浏览器或 curl 调用 `/api/subscriptions` 返回 401。

- [ ] **Step 5: 更新 OpenSpec tasks 勾选状态**

As each implementation task completes, update `openspec/changes/add-login-admin-init/tasks.md` by changing finished items from `- [ ]` to `- [x]`.

- [ ] **Step 6: Commit**

```bash
git add .gitignore openspec/changes/add-login-admin-init/tasks.md module-web/frontend/dist module-web/src/main/resources/static
git commit -m "chore: 完成登录功能验证"
```

If the frontend build does not update committed static assets in this repository, omit `module-web/frontend/dist` and `module-web/src/main/resources/static` from `git add`.

---

## Self-Review

**Spec coverage:**

- `admin-auth`：Task 1 覆盖模型、仓库、密码哈希；Task 2 覆盖状态、初始化、登录、登出；Task 4 覆盖自动化测试和损坏文件保护。
- `rest-api`：Task 3 覆盖 `/api/**` 管理接口门禁和 `/api/auth/**` 放行；Task 4 覆盖未登录 401 与登录后访问成功。
- `web-ui`：Task 5 覆盖统一认证页、初始化表单、登录表单；Task 6 覆盖路由守卫、401 处理、后台用户名和退出入口。
- 验证：Task 7 覆盖 Maven 测试、前端构建、浏览器手动验证和 `.superpowers/` 忽略。

**Placeholder scan:** 本计划没有 TBD、TODO、未定义占位步骤或“稍后实现”类说明。

**Type consistency:** 后端 Session key 统一使用 `AdminAuthService.SESSION_AUTHENTICATED` 与 `SESSION_USERNAME`；前端认证状态统一使用 `AuthStatus.initialized/authenticated/username` 和 `authSession`。
