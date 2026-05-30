package site.kael.clash.scheduler.service;

import site.kael.clash.scheduler.model.ScheduledTask;

import java.util.List;
import java.util.Optional;

/**
 * 定时任务调度服务接口
 * <p>
 * 提供定时任务的 CRUD、启停控制及手动触发功能。
 */
public interface SchedulerService {

    /**
     * 创建定时任务（自动生成 ID）
     *
     * @param task 待创建的任务（id 字段会被覆盖）
     * @return 持久化后的任务
     */
    ScheduledTask create(ScheduledTask task);

    /**
     * 更新已有的定时任务
     *
     * @param task 包含更新数据的任务（id 必须存在）
     * @return 更新后的任务
     */
    ScheduledTask update(ScheduledTask task);

    /**
     * 按 ID 查询定时任务
     *
     * @param id 任务 ID
     * @return 定时任务
     */
    Optional<ScheduledTask> findById(String id);

    /**
     * 查询所有定时任务
     *
     * @return 所有定时任务列表
     */
    List<ScheduledTask> findAll();

    /**
     * 按 ID 删除定时任务，同时取消已注册的 cron 调度
     *
     * @param id 任务 ID
     */
    void deleteById(String id);

    /**
     * 启用定时任务，注册 cron 调度
     *
     * @param taskId 任务 ID
     */
    void enable(String taskId);

    /**
     * 停用定时任务，取消 cron 调度
     *
     * @param taskId 任务 ID
     */
    void disable(String taskId);

    /**
     * 手动触发定时任务立即执行
     *
     * @param taskId 任务 ID
     */
    void trigger(String taskId);

    /**
     * 注册构建流程的 cron 定时任务
     *
     * @param pipelineId     构建流程 ID
     * @param cronExpression cron 表达式
     * @param callback       触发时的回调
     */
    void registerBuildPipelineCron(String pipelineId, String cronExpression, Runnable callback);

    /**
     * 取消构建流程的 cron 定时任务
     *
     * @param pipelineId 构建流程 ID
     */
    void cancelBuildPipelineCron(String pipelineId);
}
