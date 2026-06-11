package site.kael.clash.pipeline.service;

import site.kael.clash.pipeline.model.BuildPipeline;
import site.kael.clash.pipeline.model.BuildRecord;

import java.util.List;

public interface BuildPipelineService {
    BuildPipeline create(BuildPipeline pipeline);
    BuildPipeline update(BuildPipeline pipeline);
    BuildPipeline findById(String id);
    List<BuildPipeline> findAll();
    void deleteById(String id);
    BuildRecord execute(String pipelineId);

    /**
     * 异步执行构建流程，立即返回记录 ID
     *
     * @param pipelineId 构建流程 ID
     * @return 构建记录 ID
     */
    String executeAsync(String pipelineId);

    List<BuildRecord> findRecords(String pipelineId);
    BuildRecord findRecordById(String recordId);
}
