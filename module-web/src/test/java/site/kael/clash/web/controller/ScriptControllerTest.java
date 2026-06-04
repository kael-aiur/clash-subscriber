package site.kael.clash.web.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import site.kael.clash.web.config.GlobalExceptionHandler;

import java.nio.file.Path;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * ScriptController 单元测试
 */
class ScriptControllerTest {

    private MockMvc mockMvc;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        ScriptController controller = new ScriptController(tempDir.toString(), null, null);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();
    }

    @Test
    void listScripts_无脚本时返回空列表() throws Exception {
        mockMvc.perform(get("/api/scripts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void saveAndReadScript_保存并读取脚本() throws Exception {
        // 保存脚本
        String body = """
                {"name": "test-script", "content": "console.log('hello');"}
                """;
        mockMvc.perform(post("/api/scripts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        // 读取脚本
        mockMvc.perform(get("/api/scripts/test-script"))
                .andExpect(status().isOk())
                .andExpect(content().string("\"console.log('hello');\""));

        // 列出脚本
        mockMvc.perform(get("/api/scripts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("test-script"));

        // 删除脚本
        mockMvc.perform(delete("/api/scripts/test-script"))
                .andExpect(status().isNoContent());
    }

    @Test
    void saveScript_名称为空时返回400() throws Exception {
        String body = """
                {"name": "", "content": "console.log('hello');"}
                """;
        mockMvc.perform(post("/api/scripts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void readScript_不存在时返回404() throws Exception {
        mockMvc.perform(get("/api/scripts/nonexistent"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteScript_不存在时返回404() throws Exception {
        mockMvc.perform(delete("/api/scripts/nonexistent"))
                .andExpect(status().isNotFound());
    }
}
