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
import site.kael.clash.mihomo.model.HealthStatus;
import site.kael.clash.mihomo.model.MihomoInstance;
import site.kael.clash.mihomo.service.MihomoService;
import site.kael.clash.web.config.GlobalExceptionHandler;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * MihomoInstanceController 单元测试
 */
@ExtendWith(MockitoExtension.class)
class MihomoInstanceControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private MihomoService mihomoService;

    @InjectMocks
    private MihomoInstanceController mihomoInstanceController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(mihomoInstanceController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    void findAll_返回实例列表() throws Exception {
        MihomoInstance instance = new MihomoInstance();
        instance.setId("1");
        instance.setName("测试实例");
        when(mihomoService.findAll()).thenReturn(List.of(instance));

        mockMvc.perform(get("/api/mihomo-instances"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("1"))
                .andExpect(jsonPath("$[0].name").value("测试实例"));
    }

    @Test
    void create_创建实例() throws Exception {
        MihomoInstance input = new MihomoInstance();
        input.setName("新实例");

        MihomoInstance created = new MihomoInstance();
        created.setId("1");
        created.setName("新实例");
        when(mihomoService.create(any(MihomoInstance.class))).thenReturn(created);

        mockMvc.perform(post("/api/mihomo-instances")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("1"))
                .andExpect(jsonPath("$.name").value("新实例"));
    }

    @Test
    void findById_存在时返回实例() throws Exception {
        MihomoInstance instance = new MihomoInstance();
        instance.setId("1");
        instance.setName("测试实例");
        when(mihomoService.findById("1")).thenReturn(Optional.of(instance));

        mockMvc.perform(get("/api/mihomo-instances/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("测试实例"));
    }

    @Test
    void findById_不存在时返回404() throws Exception {
        when(mihomoService.findById("999")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/mihomo-instances/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void update_更新实例() throws Exception {
        MihomoInstance input = new MihomoInstance();
        input.setName("更新后的实例");

        MihomoInstance updated = new MihomoInstance();
        updated.setId("1");
        updated.setName("更新后的实例");
        when(mihomoService.update(any(MihomoInstance.class))).thenReturn(updated);

        mockMvc.perform(put("/api/mihomo-instances/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("更新后的实例"));
    }

    @Test
    void deleteById_删除实例() throws Exception {
        doNothing().when(mihomoService).deleteById("1");

        mockMvc.perform(delete("/api/mihomo-instances/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void checkHealth_返回健康状态() throws Exception {
        when(mihomoService.checkHealth("1")).thenReturn(HealthStatus.HEALTHY);

        mockMvc.perform(get("/api/mihomo-instances/1/health"))
                .andExpect(status().isOk())
                .andExpect(content().string("\"HEALTHY\""));
    }

    @Test
    void checkHealthAll_返回所有健康状态() throws Exception {
        when(mihomoService.checkHealthAll()).thenReturn(Map.of("1", HealthStatus.HEALTHY, "2", HealthStatus.UNHEALTHY));

        mockMvc.perform(get("/api/mihomo-instances/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.1").value("HEALTHY"))
                .andExpect(jsonPath("$.2").value("UNHEALTHY"));
    }

    @Test
    void pushConfig_向单个实例推送配置() throws Exception {
        ClashConfig config = new ClashConfig("测试配置");
        doNothing().when(mihomoService).pushConfig(eq("1"), any(ClashConfig.class));

        mockMvc.perform(post("/api/mihomo-instances/1/push")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(config)))
                .andExpect(status().isOk());
    }

    @Test
    void pushConfigAll_向所有实例推送配置() throws Exception {
        ClashConfig config = new ClashConfig("测试配置");
        when(mihomoService.pushConfigAll(any(ClashConfig.class))).thenReturn(Map.of("1", true, "2", false));

        mockMvc.perform(post("/api/mihomo-instances/push")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(config)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.1").value(true))
                .andExpect(jsonPath("$.2").value(false));
    }
}
