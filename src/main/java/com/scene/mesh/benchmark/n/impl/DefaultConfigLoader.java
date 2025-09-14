package com.scene.mesh.benchmark.n.impl;

import com.scene.mesh.benchmark.n.model.BenchmarkConfig;
import com.scene.mesh.benchmark.n.spec.IConfigLoader;
import com.scene.mesh.sdk.util.MessageUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.Charset;

@Component
@Slf4j
public class DefaultConfigLoader implements IConfigLoader {

    @Override
    public BenchmarkConfig loadTestSuiteConfig(Resource resource) {
        String testSuiteConfigJson;
        try {
            testSuiteConfigJson = resource.getContentAsString(Charset.defaultCharset());
            if (testSuiteConfigJson.isEmpty()) {
                log.warn("testSuiteConfigJson from resource:{} is empty",resource.getFilename());
            }
        } catch (IOException e) {
            log.error("load test suite config failed" + e.getMessage());
            return null;
        }

        BenchmarkConfig testSuiteConfig = MessageUtils.fromJson(testSuiteConfigJson, BenchmarkConfig.class);

        // 验证配置
        validateBenchmarkConfig(testSuiteConfig);

        return testSuiteConfig;
    }

    /**
     * 验证基准测试配置
     */
    private void validateBenchmarkConfig(BenchmarkConfig benchmarkConfig) {
        if (benchmarkConfig == null) {
            throw new IllegalArgumentException("基准测试配置不能为空");
        }

        if (benchmarkConfig.getEventGroups() == null || benchmarkConfig.getEventGroups().isEmpty()) {
            throw new IllegalArgumentException("事件组列表不能为空");
        }

        for (BenchmarkConfig.EventGroup eventGroup : benchmarkConfig.getEventGroups()) {
            validateEventGroup(eventGroup);
        }
    }

    /**
     * 验证事件组配置
     */
    private void validateEventGroup(BenchmarkConfig.EventGroup eventGroup) {
        if (eventGroup == null) {
            throw new IllegalArgumentException("事件组不能为空");
        }

        if (eventGroup.getGroupId() == null || eventGroup.getGroupId().trim().isEmpty()) {
            throw new IllegalArgumentException("事件组ID不能为空");
        }

        if (eventGroup.isAutoGenerate()) {
            // 自动生成模式验证
            if (eventGroup.getEventTemplate() == null) {
                throw new IllegalArgumentException(
                        String.format("事件组 %s 设置为自动生成模式，但缺少事件模板", eventGroup.getGroupId()));
            }

            if (eventGroup.getEventTemplate().getTypeSelector() == null ||
                    eventGroup.getEventTemplate().getTypeSelector().isEmpty()) {
                throw new IllegalArgumentException(
                        String.format("事件组 %s 的事件模板缺少类型选择器", eventGroup.getGroupId()));
            }

            if (eventGroup.getEventTemplate().getPayloadTemplates() == null ||
                    eventGroup.getEventTemplate().getPayloadTemplates().isEmpty()) {
                throw new IllegalArgumentException(
                        String.format("事件组 %s 的事件模板缺少payload模板", eventGroup.getGroupId()));
            }

            // 验证每个类型选择器都有对应的payload模板
            for (String eventType : eventGroup.getEventTemplate().getTypeSelector()) {
                if (!eventGroup.getEventTemplate().getPayloadTemplates().containsKey(eventType)) {
                    throw new IllegalArgumentException(
                            String.format("事件组 %s 中事件类型 %s 缺少payload模板", eventGroup.getGroupId(), eventType));
                }
            }
        } else {
            // 手动输入模式验证
            if (eventGroup.getInputEvents() == null || eventGroup.getInputEvents().isEmpty()) {
                throw new IllegalArgumentException(
                        String.format("事件组 %s 设置为手动输入模式，但缺少输入事件列表", eventGroup.getGroupId()));
            }
        }
    }
}