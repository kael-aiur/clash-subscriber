package site.kael.clash.web.controller;

import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "data.path=target/test-data/build-record-integration")
class BuildRecordControllerIntegrationTest {

    private static final Path ADMIN_FILE = Path.of("target/test-data/build-record-integration/admin/admin.json");
    private static final String SETUP_JSON = "{\"username\":\"admin\",\"password\":\"password123\",\"confirmPassword\":\"password123\"}";
    private static final String LOGIN_JSON = "{\"username\":\"admin\",\"password\":\"password123\"}";

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    @AfterEach
    void cleanTestData() throws IOException {
        Files.deleteIfExists(ADMIN_FILE);
    }

    @Test
    void progress_shouldReturnSseEmitter() throws Exception {
        // Given
        MockHttpSession session = setupAdminAndLogin();
        String recordId = "test-record-id";

        // When & Then
        // MockMvc 不完整支持 SSE 的异步生命周期，仅验证端点可访问且返回 200
        mockMvc.perform(get("/api/build-records/{id}/progress", recordId)
                .session(session)
                .accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(status().isOk());
    }

    /**
     * 初始化管理员账号并登录，返回已认证的 Session
     */
    private MockHttpSession setupAdminAndLogin() throws Exception {
        // 初始化管理员
        mockMvc.perform(post("/api/auth/setup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(SETUP_JSON))
                .andExpect(status().isOk());

        // 登录获取 Session
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(LOGIN_JSON))
                .andExpect(status().isOk())
                .andReturn();

        HttpSession session = loginResult.getRequest().getSession(false);
        assertNotNull(session, "登录成功后应存在 Session");
        return (MockHttpSession) session;
    }
}
