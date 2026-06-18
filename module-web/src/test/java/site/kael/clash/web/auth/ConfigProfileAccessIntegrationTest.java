package site.kael.clash.web.auth;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import site.kael.clash.processor.model.ConfigProfile;
import site.kael.clash.processor.repository.ConfigProfileRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "data.path=target/test-data/config-profile-access")
@Execution(ExecutionMode.SAME_THREAD)
class ConfigProfileAccessIntegrationTest {

    private static final Path TEST_DATA_DIR = Path.of("target/test-data/config-profile-access");
    private static final Path CONFIG_PROFILE_DIR = TEST_DATA_DIR.resolve("config-profiles");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ConfigProfileRepository configProfileRepository;

    @BeforeEach
    @AfterEach
    void cleanTestData() throws IOException {
        if (Files.exists(TEST_DATA_DIR)) {
            try (var paths = Files.walk(TEST_DATA_DIR)) {
                paths.sorted(Comparator.reverseOrder())
                        .forEach(path -> {
                            try {
                                Files.deleteIfExists(path);
                            } catch (IOException e) {
                                throw new RuntimeException("清理测试目录失败: " + path, e);
                            }
                        });
            }
        }
        Files.createDirectories(CONFIG_PROFILE_DIR);
    }

    @Test
    void getClashConfig_allowsAnonymousAccessWhenProfileHasBasicAuthConfigured() throws Exception {
        ConfigProfile profile = new ConfigProfile();
        profile.setId("profile-anonymous");
        profile.setName("anonymous-access");
        profile.setAuthUsername("client");
        profile.setAuthPassword("secret");
        configProfileRepository.save(profile);

        mockMvc.perform(get("/api/config/{name}", profile.getName()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/yaml; charset=utf-8"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("mixed-port: 7890")));
    }

    @Test
    void getClashConfig_returnsNotFoundWhenProfileMissing() throws Exception {
        mockMvc.perform(get("/api/config/{name}", "missing-profile"))
                .andExpect(status().isNotFound());
    }

    @Test
    void protectedApi_stillRequiresLogin() throws Exception {
        mockMvc.perform(get("/api/subscriptions"))
                .andExpect(status().isUnauthorized());
    }
}
