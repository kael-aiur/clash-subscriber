package site.kael.clash.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import site.kael.clash.common.model.ClashConfig;
import site.kael.clash.processor.model.PipelineConfig;
import site.kael.clash.processor.repository.PipelineConfigRepository;
import site.kael.clash.processor.service.PipelineService;
import site.kael.clash.web.config.GlobalExceptionHandler;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * PipelineController 单元测试
 */
@ExtendWith(MockitoExtension.class)
class PipelineControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private PipelineConfigRepository pipelineConfigRepository;

    @Mock
    private PipelineService pipelineService;

    @InjectMocks
    private PipelineController pipelineController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(pipelineController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    void findAll_返回Pipeline列表() throws Exception {
        PipelineConfig config = new PipelineConfig();
        config.setId("1");
        config.setName("测试Pipeline");
        when(pipelineConfigRepository.findAll()).thenReturn(List.of(config));

        mockMvc.perform(get("/api/pipelines"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("1"))
                .andExpect(jsonPath("$[0].name").value("测试Pipeline"));
    }

    @Test
    void create_创建Pipeline配置() throws Exception {
        PipelineConfig input = new PipelineConfig();
        input.setName("新Pipeline");

        PipelineConfig saved = new PipelineConfig();
        saved.setId("test-id");
        saved.setName("新Pipeline");
        when(pipelineConfigRepository.save(any(PipelineConfig.class))).thenReturn(saved);

        mockMvc.perform(post("/api/pipelines")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("test-id"))
                .andExpect(jsonPath("$.name").value("新Pipeline"));
    }

    @Test
    void create_已有ID时保留原ID() throws Exception {
        PipelineConfig input = new PipelineConfig();
        input.setId("custom-id");
        input.setName("自定义ID的Pipeline");

        when(pipelineConfigRepository.save(any(PipelineConfig.class))).thenReturn(input);

        mockMvc.perform(post("/api/pipelines")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("custom-id"));
    }

    @Test
    void findById_存在时返回配置() throws Exception {
        PipelineConfig config = new PipelineConfig();
        config.setId("1");
        config.setName("测试Pipeline");
        when(pipelineConfigRepository.findById("1")).thenReturn(Optional.of(config));

        mockMvc.perform(get("/api/pipelines/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("测试Pipeline"));
    }

    @Test
    void findById_不存在时返回404() throws Exception {
        when(pipelineConfigRepository.findById("999")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/pipelines/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void update_更新Pipeline配置() throws Exception {
        PipelineConfig input = new PipelineConfig();
        input.setName("更新后的Pipeline");

        PipelineConfig saved = new PipelineConfig();
        saved.setId("1");
        saved.setName("更新后的Pipeline");
        when(pipelineConfigRepository.save(any(PipelineConfig.class))).thenReturn(saved);

        mockMvc.perform(put("/api/pipelines/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("更新后的Pipeline"));
    }

    @Test
    void deleteById_删除Pipeline配置() throws Exception {
        doNothing().when(pipelineConfigRepository).deleteById("1");

        mockMvc.perform(delete("/api/pipelines/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void execute_执行Pipeline() throws Exception {
        PipelineConfig pipeline = new PipelineConfig();
        pipeline.setId("1");
        pipeline.setName("测试Pipeline");

        ClashConfig input = new ClashConfig("输入配置");
        ClashConfig output = new ClashConfig("输出配置");

        when(pipelineConfigRepository.findById("1")).thenReturn(Optional.of(pipeline));
        when(pipelineService.execute(eq(pipeline), any(ClashConfig.class))).thenReturn(output);

        mockMvc.perform(post("/api/pipelines/1/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("输出配置"));
    }

    @Test
    void execute_Pipeline不存在时返回404() throws Exception {
        ClashConfig input = new ClashConfig("输入配置");
        when(pipelineConfigRepository.findById("999")).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/pipelines/999/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isNotFound());
    }
}
