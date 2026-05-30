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
    List<BuildRecord> findRecords(String pipelineId);
    BuildRecord findRecordById(String recordId);
}
