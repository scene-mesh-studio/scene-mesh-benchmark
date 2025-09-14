package com.scene.mesh.benchmark.n.impl;

import com.scene.mesh.benchmark.n.spec.ITemplateEngine;
import com.scene.mesh.benchmark.n.spec.ITemplateProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class DefaultTemplateProcessor implements ITemplateProcessor {

    private final ITemplateEngine templateEngine;

    @Autowired
    public DefaultTemplateProcessor(ITemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    @Override
    public Map<String, Object> processDataTemplate(Map<String, Object> template) {
        Map<String, Object> result = new HashMap<>();

        for (Map.Entry<String, Object> entry : template.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (value instanceof String) {
                String stringValue = (String) value;
                result.put(key, processStringTemplate(stringValue));
            } else if (value instanceof Map) {
                // 递归处理嵌套的 Map
                @SuppressWarnings("unchecked")
                Map<String, Object> nestedMap = (Map<String, Object>) value;
                result.put(key, processDataTemplate(nestedMap));
            } else {
                result.put(key, value);
            }
        }

        return result;
    }

    @Override
    public String processStringTemplate(String template) {
        // 尝试使用可用的模板引擎处理
        try {
            String result = templateEngine.processTemplate(template, new HashMap<>());
            if (!result.equals(template)) {
                log.debug("处理模板: {} -> {}", template, result);
                return result;
            }
        } catch (Exception e) {
            log.warn("处理模板失败: {}, 尝试下一个引擎", e.getMessage());
        }

        return template;
    }
}