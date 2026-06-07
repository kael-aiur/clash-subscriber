-- 回滚脚本：移除构建流程的配置类型字段
-- Rollback: V20240101_001__add_config_type_to_build_pipeline

-- 1. 删除索引
DROP INDEX IF EXISTS idx_build_pipeline_config_type;
DROP INDEX IF EXISTS idx_build_pipeline_config_profile_id;

-- 2. 删除新增的字段
ALTER TABLE build_pipeline DROP COLUMN IF EXISTS config_type;
ALTER TABLE build_pipeline DROP COLUMN IF EXISTS config_profile_id;

-- 3. 验证回滚结果
DO $$
BEGIN
    -- 检查字段是否已删除
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'build_pipeline'
        AND column_name IN ('config_type', 'config_profile_id')
    ) THEN
        RAISE EXCEPTION '回滚失败：字段未成功删除';
    END IF;

    -- 检查索引是否已删除
    IF EXISTS (
        SELECT 1 FROM pg_indexes
        WHERE tablename = 'build_pipeline'
        AND indexname IN ('idx_build_pipeline_config_type', 'idx_build_pipeline_config_profile_id')
    ) THEN
        RAISE EXCEPTION '回滚失败：索引未成功删除';
    END IF;

    RAISE NOTICE '回滚成功：已移除 config_type 和 config_profile_id 字段及相关索引';
END $$;
