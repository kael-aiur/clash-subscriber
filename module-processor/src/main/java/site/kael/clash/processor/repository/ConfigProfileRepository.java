package site.kael.clash.processor.repository;

import site.kael.clash.processor.model.ConfigProfile;

import java.util.List;
import java.util.Optional;

/**
 * 配置组合仓储接口
 */
public interface ConfigProfileRepository {

    /**
     * 保存配置组合
     *
     * @param profile 配置组合
     * @return 保存后的配置组合
     */
    ConfigProfile save(ConfigProfile profile);

    /**
     * 根据 ID 查找配置组合
     *
     * @param id 配置组合 ID
     * @return 配置组合
     */
    Optional<ConfigProfile> findById(String id);

    /**
     * 根据名称查找配置组合
     *
     * @param name 配置组合名称
     * @return 配置组合
     */
    Optional<ConfigProfile> findByName(String name);

    /**
     * 查找所有配置组合
     *
     * @return 所有配置组合列表
     */
    List<ConfigProfile> findAll();

    /**
     * 根据 ID 删除配置组合
     *
     * @param id 配置组合 ID
     */
    void deleteById(String id);

    /**
     * 检查指定名称的配置组合是否存在
     *
     * @param name 配置组合名称
     * @return 是否存在
     */
    boolean existsByName(String name);
}
