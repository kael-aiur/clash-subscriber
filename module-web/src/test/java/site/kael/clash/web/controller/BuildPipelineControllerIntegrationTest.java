package site.kael.clash.web.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * BuildPipelineController 集成测试
 * <p>
 * 测试构建流程 API 接口，覆盖配置组合模式和订阅源模式的创建功能。
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "data.path=target/test-data/build-pipeline-integration")
@Execution(ExecutionMode.SAME_THREAD)
class BuildPipelineControllerIntegrationTest {

    private static final Path ADMIN_FILE = Path.of("target/test-data/build-pipeline-integration/admin/admin.json");
    private static final Path PIPELINE_DIR = Path.of("target/test-data/build-pipeline-integration/build-pipelines");
    private static final String SETUP_JSON = "{\"username\":\"admin\",\"password\":\"password123\",\"confirmPassword\":\"password123\"}";
    private static final String LOGIN_JSON = "{\"username\":\"admin\",\"password\":\"password123\"}";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    @AfterEach
    void cleanTestData() throws IOException {
        // 清理管理员账号，确保每个测试独立
        Files.deleteIfExists(ADMIN_FILE);
        // 清理构建流程数据
        if (Files.exists(PIPELINE_DIR)) {
            try (var stream = Files.newDirectoryStream(PIPELINE_DIR, "*.json")) {
                for (Path file : stream) {
                    Files.deleteIfExists(file);
                }
            }
        }
    }

    @Test
    @DisplayName("测试创建配置组合模式构建流程")
    void testCreateConfigProfilePipeline() throws Exception {
        MockHttpSession session = setupAdminAndLogin();

        String requestBody = """
                {
                    "name": "测试配置组合流程",
                    "configType": "config-profile",
                    "configProfileId": "profile-001",
                    "targetInstanceId": "instance-001",
                    "enabled": true
                }
                """;

        MvcResult result = mockMvc.perform(post("/api/build-pipelines")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.name").value("测试配置组合流程"))
                .andExpect(jsonPath("$.configType").value("config-profile"))
                .andExpect(jsonPath("$.configProfileId").value("profile-001"))
                .andExpect(jsonPath("$.targetInstanceId").value("instance-001"))
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty())
                .andReturn();

        // 验证响应体可以正确解析
        String responseBody = result.getResponse().getContentAsString();
        JsonNode jsonNode = objectMapper.readTree(responseBody);
        assertNotNull(jsonNode.get("id").asText(), "返回的 ID 不应为空");
        assertEquals("config-profile", jsonNode.get("configType").asText());
    }

    @Test
    @DisplayName("测试创建订阅源模式构建流程")
    void testCreateSubscriptionPipeline() throws Exception {
        MockHttpSession session = setupAdminAndLogin();

        String requestBody = """
                {
                    "name": "测试订阅源流程",
                    "configType": "subscription",
                    "primarySubscriptionId": "sub-001",
                    "targetInstanceId": "instance-001",
                    "enabled": true
                }
                """;

        MvcResult result = mockMvc.perform(post("/api/build-pipelines")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.name").value("测试订阅源流程"))
                .andExpect(jsonPath("$.configType").value("subscription"))
                .andExpect(jsonPath("$.primarySubscriptionId").value("sub-001"))
                .andExpect(jsonPath("$.targetInstanceId").value("instance-001"))
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty())
                .andReturn();

        // 验证响应体可以正确解析
        String responseBody = result.getResponse().getContentAsString();
        JsonNode jsonNode = objectMapper.readTree(responseBody);
        assertNotNull(jsonNode.get("id").asText(), "返回的 ID 不应为空");
        assertEquals("subscription", jsonNode.get("configType").asText());
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
