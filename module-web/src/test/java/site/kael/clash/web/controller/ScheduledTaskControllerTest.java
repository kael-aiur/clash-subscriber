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
import site.kael.clash.scheduler.model.ScheduledTask;
import site.kael.clash.scheduler.service.SchedulerService;
import site.kael.clash.web.config.GlobalExceptionHandler;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * ScheduledTaskController 单元测试
 */
@ExtendWith(MockitoExtension.class)
class ScheduledTaskControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private SchedulerService schedulerService;

    @InjectMocks
    private ScheduledTaskController scheduledTaskController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(scheduledTaskController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    void findAll_返回任务列表() throws Exception {
        ScheduledTask task = new ScheduledTask();
        task.setId("1");
        task.setName("测试任务");
        when(schedulerService.findAll()).thenReturn(List.of(task));

        mockMvc.perform(get("/api/scheduled-tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("1"))
                .andExpect(jsonPath("$[0].name").value("测试任务"));
    }

    @Test
    void create_创建定时任务() throws Exception {
        ScheduledTask input = new ScheduledTask();
        input.setName("新任务");

        ScheduledTask created = new ScheduledTask();
        created.setId("1");
        created.setName("新任务");
        when(schedulerService.create(any(ScheduledTask.class))).thenReturn(created);

        mockMvc.perform(post("/api/scheduled-tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("1"))
                .andExpect(jsonPath("$.name").value("新任务"));
    }

    @Test
    void findById_存在时返回任务() throws Exception {
        ScheduledTask task = new ScheduledTask();
        task.setId("1");
        task.setName("测试任务");
        when(schedulerService.findById("1")).thenReturn(Optional.of(task));

        mockMvc.perform(get("/api/scheduled-tasks/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("测试任务"));
    }

    @Test
    void findById_不存在时返回404() throws Exception {
        when(schedulerService.findById("999")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/scheduled-tasks/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void update_更新定时任务() throws Exception {
        ScheduledTask input = new ScheduledTask();
        input.setName("更新后的任务");

        ScheduledTask updated = new ScheduledTask();
        updated.setId("1");
        updated.setName("更新后的任务");
        when(schedulerService.update(any(ScheduledTask.class))).thenReturn(updated);

        mockMvc.perform(put("/api/scheduled-tasks/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("更新后的任务"));
    }

    @Test
    void deleteById_删除定时任务() throws Exception {
        doNothing().when(schedulerService).deleteById("1");

        mockMvc.perform(delete("/api/scheduled-tasks/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void enable_启用任务() throws Exception {
        doNothing().when(schedulerService).enable("1");

        mockMvc.perform(post("/api/scheduled-tasks/1/enable"))
                .andExpect(status().isOk());
    }

    @Test
    void disable_停用任务() throws Exception {
        doNothing().when(schedulerService).disable("1");

        mockMvc.perform(post("/api/scheduled-tasks/1/disable"))
                .andExpect(status().isOk());
    }

    @Test
    void trigger_手动触发任务() throws Exception {
        doNothing().when(schedulerService).trigger("1");

        mockMvc.perform(post("/api/scheduled-tasks/1/trigger"))
                .andExpect(status().isOk());
    }
}
