package site.kael.clash.mihomo.service.impl;

import org.springframework.stereotype.Service;
import site.kael.clash.mihomo.model.ForwardingPathResult;
import site.kael.clash.mihomo.service.ForwardingPathService;

import java.util.*;

@Service
public class ForwardingPathServiceImpl implements ForwardingPathService {

    @Override
    public ForwardingPathResult resolveForwardingPath(String configYaml, String domain) {
        // TODO: 后续步骤实现
        return new ForwardingPathResult(List.of(), List.of());
    }
}
