package site.kael.clash.mihomo.service;

import site.kael.clash.mihomo.model.ForwardingPathResult;

/**
 * 转发路径解析服务
 * <p>
 * 从 Mihomo 配置中解析规则匹配和代理组路由，构建转发路径流程图数据。
 */
public interface ForwardingPathService {

    /**
     * 查询指定域名的转发路径
     *
     * @param rulesJson   Mihomo rules API 返回的 JSON 数据
     * @param proxiesJson Mihomo proxies API 返回的 JSON 数据
     * @param domain      用户输入的域名
     * @return Vue Flow 格式的流程图数据
     */
    ForwardingPathResult resolveForwardingPath(String rulesJson, String proxiesJson, String domain);
}
