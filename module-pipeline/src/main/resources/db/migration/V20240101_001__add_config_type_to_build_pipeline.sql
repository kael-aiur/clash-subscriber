-- 为构建流程增加配置类型字段，支持订阅源模式和配置组合模式
-- Migration: V20240101_001__add_config_type_to_build_pipeline

-- 1. 新增 config_type 字段，默认值为 'subscription'（订阅源模式）
ALTER TABLE build_pipeline ADD COLUMN config_type VARCHAR(20) DEFAULT 'subscription';

-- 2. 新增 config_profile_id 字段，用于配置组合模式
ALTER TABLE build_pipeline ADD COLUMN config_profile_id VARCHAR(64);

-- 3. 更新现有数据，确保所有现有记录的 config_type 都设置为 'subscription'
UPDATE build_pipeline SET config_type = 'subscription' WHERE config_type IS NULL;

-- 4. 创建索引以优化查询性能
CREATE INDEX idx_build_pipeline_config_type ON build_pipeline (config_type);
CREATE INDEX idx_build_pipeline_config_profile_id ON build_pipeline (config_profile_id);

-- 5. 验证数据迁移结果
-- 检查所有记录是否都有 config_type 值
DO $$
DECLARE
    null_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO null_count FROM build_pipeline WHERE config_type IS NULL;
    IF null_count > 0 THEN
        RAISE EXCEPTION '数据迁移失败：仍有 % 条记录的 config_type 为 NULL', null_count;
    END IF;
    RAISE NOTICE '数据迁移成功：所有记录的 config_type 已设置为 subscription';
END $$;
