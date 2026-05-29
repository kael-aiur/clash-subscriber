package site.kael.clash.scheduler.repository;

import site.kael.clash.scheduler.model.ScheduledTask;

import java.util.List;
import java.util.Optional;

/**
 * 定时任务仓储接口
 */
public interface ScheduledTaskRepository {

    /**
     * 保存定时任务
     *
     * @param task 定时任务
     * @return 保存后的任务
     */
    ScheduledTask save(ScheduledTask task);

    /**
     * 根据 ID 查找定时任务
     *
     * @param id 任务 ID
     * @return 定时任务
     */
    Optional<ScheduledTask> findById(String id);

    /**
     * 查找所有定时任务
     *
     * @return 所有定时任务列表
     */
    List<ScheduledTask> findAll();

    /**
     * 根据 ID 删除定时任务
     *
     * @param id 任务 ID
     */
    void deleteById(String id);
}
