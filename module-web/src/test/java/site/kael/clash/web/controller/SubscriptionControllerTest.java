package site.kael.clash.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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
import site.kael.clash.subscription.model.Subscription;
import site.kael.clash.subscription.service.SubscriptionService;
import site.kael.clash.web.config.GlobalExceptionHandler;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * SubscriptionController 单元测试
 */
@ExtendWith(MockitoExtension.class)
class SubscriptionControllerTest {

    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @Mock
    private SubscriptionService subscriptionService;

    @InjectMocks
    private SubscriptionController subscriptionController;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        mockMvc = MockMvcBuilders.standaloneSetup(subscriptionController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    void findAll_返回订阅列表() throws Exception {
        Subscription sub = new Subscription();
        sub.setId("1");
        sub.setName("测试订阅");
        when(subscriptionService.findAll()).thenReturn(List.of(sub));

        mockMvc.perform(get("/api/subscriptions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("1"))
                .andExpect(jsonPath("$[0].name").value("测试订阅"));
    }

    @Test
    void create_创建订阅源() throws Exception {
        Subscription input = new Subscription();
        input.setName("新订阅");

        Subscription created = new Subscription();
        created.setId("1");
        created.setName("新订阅");
        when(subscriptionService.create(any(Subscription.class))).thenReturn(created);

        mockMvc.perform(post("/api/subscriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("1"))
                .andExpect(jsonPath("$.name").value("新订阅"));
    }

    @Test
    void findById_存在时返回订阅() throws Exception {
        Subscription sub = new Subscription();
        sub.setId("1");
        sub.setName("测试订阅");
        when(subscriptionService.findById("1")).thenReturn(Optional.of(sub));

        mockMvc.perform(get("/api/subscriptions/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("测试订阅"));
    }

    @Test
    void findById_不存在时返回404() throws Exception {
        when(subscriptionService.findById("999")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/subscriptions/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void update_更新订阅源() throws Exception {
        Subscription input = new Subscription();
        input.setName("更新后的订阅");

        Subscription updated = new Subscription();
        updated.setId("1");
        updated.setName("更新后的订阅");
        when(subscriptionService.update(any(Subscription.class))).thenReturn(updated);

        mockMvc.perform(put("/api/subscriptions/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("更新后的订阅"));
    }

    @Test
    void deleteById_删除订阅源() throws Exception {
        doNothing().when(subscriptionService).deleteById("1");

        mockMvc.perform(delete("/api/subscriptions/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void fetch_获取订阅配置() throws Exception {
        ClashConfig config = new ClashConfig("测试配置");
        when(subscriptionService.fetch("1")).thenReturn(config);

        mockMvc.perform(post("/api/subscriptions/1/fetch"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("测试配置"));
    }
}
