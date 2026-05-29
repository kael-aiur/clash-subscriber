package site.kael.clash.processor.repository;

import site.kael.clash.processor.model.PipelineConfig;

import java.util.List;
import java.util.Optional;

/**
 * Pipeline 配置仓储接口
 */
public interface PipelineConfigRepository {

    /**
     * 保存 Pipeline 配置
     *
     * @param config Pipeline 配置
     * @return 保存后的配置
     */
    PipelineConfig save(PipelineConfig config);

    /**
     * 根据 ID 查找 Pipeline 配置
     *
     * @param id Pipeline ID
     * @return Pipeline 配置
     */
    Optional<PipelineConfig> findById(String id);

    /**
     * 查找所有 Pipeline 配置
     *
     * @return 所有 Pipeline 配置列表
     */
    List<PipelineConfig> findAll();

    /**
     * 根据 ID 删除 Pipeline 配置
     *
     * @param id Pipeline ID
     */
    void deleteById(String id);
}
