package site.kael.clash.pipeline.repository;

import site.kael.clash.pipeline.model.BuildPipeline;

import java.util.List;
import java.util.Optional;

public interface BuildPipelineRepository {
    BuildPipeline save(BuildPipeline pipeline);
    Optional<BuildPipeline> findById(String id);
    List<BuildPipeline> findAll();
    List<BuildPipeline> findByConfigType(String configType);
    List<BuildPipeline> findByConfigProfileId(String configProfileId);
    void deleteById(String id);
}
