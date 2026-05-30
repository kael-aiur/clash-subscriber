package site.kael.clash.pipeline.repository;

import site.kael.clash.pipeline.model.BuildRecord;

import java.util.List;
import java.util.Optional;

public interface BuildRecordRepository {
    BuildRecord save(BuildRecord record);
    Optional<BuildRecord> findById(String id);
    List<BuildRecord> findByBuildPipelineId(String buildPipelineId);
}
