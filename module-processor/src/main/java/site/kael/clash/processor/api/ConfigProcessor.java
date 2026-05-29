package site.kael.clash.processor.api;

import site.kael.clash.common.model.ClashConfig;

public interface ConfigProcessor {
    String getName();
    int getOrder();
    ClashConfig process(ClashConfig input, ProcessingContext context);
}
