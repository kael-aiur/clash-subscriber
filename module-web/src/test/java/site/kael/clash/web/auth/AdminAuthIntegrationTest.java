package site.kael.clash.web.auth;

import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "data.path=target/test-data/auth")
@Execution(ExecutionMode.SAME_THREAD)
class AdminAuthIntegrationTest {
    private static final Path ADMIN_FILE = Path.of("target/test-data/auth/admin/admin.json");
    private static final String SETUP_JSON = "{\"username\":\"admin\",\"password\":\"password123\",\"confirmPassword\":\"password123\"}";
    private static final String LOGIN_JSON = "{\"username\":\"admin\",\"password\":\"password123\"}";

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    @AfterEach
    void cleanAdminFile() throws Exception {
        Files.deleteIfExists(ADMIN_FILE);
    }

    @Test
    void statusReturnsUninitializedWhenAdminFileMissing() throws Exception {
        mockMvc.perform(get("/api/auth/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.initialized", is(false)))
                .andExpect(jsonPath("$.authenticated", is(false)));
    }

    @Test
    void setupCreatesAdminWithoutLoggingIn() throws Exception {
        MockHttpSession session = new MockHttpSession();

        mockMvc.perform(post("/api/auth/setup")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(SETUP_JSON))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/auth/status").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.initialized", is(true)))
                .andExpect(jsonPath("$.authenticated", is(false)));
    }

    @Test
    void setupRejectsShortPassword() throws Exception {
        mockMvc.perform(post("/api/auth/setup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"short\",\"confirmPassword\":\"short\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void setupRejectsDuplicateInitialization() throws Exception {
        setupAdmin();

        mockMvc.perform(post("/api/auth/setup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin2\",\"password\":\"password456\",\"confirmPassword\":\"password456\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void loginCreatesAuthenticatedSessionAndStatusUsesSameSession() throws Exception {
        setupAdmin();

        MvcResult login = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(LOGIN_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated", is(true)))
                .andExpect(jsonPath("$.username", is("admin")))
                .andReturn();

        MockHttpSession session = sessionFrom(login);
        mockMvc.perform(get("/api/auth/status").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.initialized", is(true)))
                .andExpect(jsonPath("$.authenticated", is(true)))
                .andExpect(jsonPath("$.username", is("admin")));
    }

    @Test
    void logoutClearsAuthenticatedStatus() throws Exception {
        setupAdmin();
        MockHttpSession session = loginSession();

        mockMvc.perform(post("/api/auth/logout").session(session))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/auth/status").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.initialized", is(true)))
                .andExpect(jsonPath("$.authenticated", is(false)));
    }

    @Test
    void loginRejectsUninitializedSystem() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(LOGIN_JSON))
                .andExpect(status().isConflict());
    }

    @Test
    void loginRejectsWrongPasswordWithUnifiedMessage() throws Exception {
        setupAdmin();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"wrongpass\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", is("用户名或密码错误")));
    }

    @Test
    void protectedApiRequiresLoginAndAllowsAuthenticatedSession() throws Exception {
        setupAdmin();

        mockMvc.perform(get("/api/subscriptions"))
                .andExpect(status().isUnauthorized());

        MockHttpSession session = loginSession();
        mockMvc.perform(get("/api/subscriptions").session(session))
                .andExpect(status().isOk());
    }

    @Test
    void protectedApiRejectsAuthenticatedSessionWhenAdminFileIsMissing() throws Exception {
        setupAdmin();
        MockHttpSession session = loginSession();
        Files.deleteIfExists(ADMIN_FILE);

        mockMvc.perform(get("/api/subscriptions").session(session))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void corruptedAdminFileMakesStatusFailAndCannotBeOverwritten() throws Exception {
        Files.createDirectories(ADMIN_FILE.getParent());
        Files.writeString(ADMIN_FILE, "not-json");

        mockMvc.perform(get("/api/auth/status"))
                .andExpect(status().isInternalServerError());

        mockMvc.perform(post("/api/auth/setup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(SETUP_JSON))
                .andExpect(status().isConflict());
    }

    private void setupAdmin() throws Exception {
        mockMvc.perform(post("/api/auth/setup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(SETUP_JSON))
                .andExpect(status().isOk());
    }

    private MockHttpSession loginSession() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(LOGIN_JSON))
                .andExpect(status().isOk())
                .andReturn();
        return sessionFrom(result);
    }

    private MockHttpSession sessionFrom(MvcResult result) {
        HttpSession session = result.getRequest().getSession(false);
        assertNotNull(session, "登录成功后应存在 Session");
        return assertInstanceOf(MockHttpSession.class, session);
    }

}
