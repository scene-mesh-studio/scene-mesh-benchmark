package com.scene.mesh.benchmark.n.impl;

import com.scene.mesh.benchmark.n.model.BenchmarkConfig;
import com.scene.mesh.benchmark.n.model.TestSuite;
import com.scene.mesh.benchmark.n.spec.ITemplateProcessor;
import com.scene.mesh.benchmark.n.spec.ITestSuiteGenerator;
import com.scene.mesh.sdk.model.TerminalEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Slf4j
@Component
public class DefaultTestSuiteGenerator implements ITestSuiteGenerator {

    private final ITemplateProcessor templateProcessor;
    private final Random random = new Random();

    @Autowired
    public DefaultTestSuiteGenerator(ITemplateProcessor templateProcessor) {
        this.templateProcessor = templateProcessor;
    }

    @Override
    public TestSuite generateTestSuite(BenchmarkConfig benchmarkConfig) {
        log.info("开始生成测试套件: {}", benchmarkConfig.getName());

        TestSuite testSuite = new TestSuite();

        // 基本信息转换
        testSuite.setId(benchmarkConfig.getId());
        testSuite.setName(benchmarkConfig.getName());
        testSuite.setProductId(benchmarkConfig.getProductId());
        testSuite.setSecretKey(benchmarkConfig.getSecretKey());
        testSuite.setProtocol(benchmarkConfig.getProtocol());
        testSuite.setDurationOfWaitingActions(benchmarkConfig.getDurationOfWaitingActions());
        testSuite.setExpectedActionIds(benchmarkConfig.getExpectedActionIds());

        // 转换事件组为终端事件列表
        List<TerminalEvent> terminalEvents = convertEventGroupsToTerminalEvents(benchmarkConfig.getEventGroups());
        testSuite.setTerminalEvents(terminalEvents);

        log.info("测试套件生成完成，包含 {} 个终端事件", terminalEvents.size());
        terminalEvents.forEach(terminalEvent -> {
           log.debug(terminalEvent.toString());
        });
        return testSuite;
    }

    /**
     * 将事件组转换为终端事件列表
     */
    private List<TerminalEvent> convertEventGroupsToTerminalEvents(List<BenchmarkConfig.EventGroup> eventGroups) {
        List<TerminalEvent> terminalEvents = new ArrayList<>();

        if (eventGroups == null || eventGroups.isEmpty()) {
            log.warn("事件组列表为空");
            return terminalEvents;
        }

        for (BenchmarkConfig.EventGroup eventGroup : eventGroups) {
            List<TerminalEvent> groupEvents = convertEventGroupToTerminalEvents(eventGroup);
            terminalEvents.addAll(groupEvents);
        }

        return terminalEvents;
    }

    /**
     * 将单个事件组转换为终端事件列表
     */
    private List<TerminalEvent> convertEventGroupToTerminalEvents(BenchmarkConfig.EventGroup eventGroup) {
        List<TerminalEvent> terminalEvents = new ArrayList<>();

        if (eventGroup.isAutoGenerate()) {
            // 自动生成模式
            terminalEvents = generateEventsFromTemplate(eventGroup);
        } else {
            // 手动输入模式
            terminalEvents = convertInputEventsToTerminalEvents(eventGroup.getInputEvents());
        }

        log.info("事件组 {} 转换完成，生成 {} 个终端事件", eventGroup.getGroupId(), terminalEvents.size());
        return terminalEvents;
    }

    /**
     * 根据模板生成事件
     */
    private List<TerminalEvent> generateEventsFromTemplate(BenchmarkConfig.EventGroup eventGroup) {
        List<TerminalEvent> terminalEvents = new ArrayList<>();

        if (eventGroup.getEventTemplate() == null) {
            log.error("事件组 {} 缺少事件模板", eventGroup.getGroupId());
            return terminalEvents;
        }

        int count = eventGroup.getCount() != null ? eventGroup.getCount() : 1;

        for (int i = 0; i < count; i++) {
            // 随机选择事件类型
            String eventType = selectRandomEventType(eventGroup.getEventTemplate().getTypeSelector());
            if (eventType == null) {
                log.warn("事件组 {} 第 {} 次生成失败：无法选择事件类型", eventGroup.getGroupId(), i + 1);
                continue;
            }

            // 获取对应的载荷模板
            Map<String, Object> payloadTemplate = eventGroup.getEventTemplate().getPayloadTemplates().get(eventType);
            if (payloadTemplate == null) {
                log.warn("事件组 {} 第 {} 次生成失败：事件类型 {} 缺少payload模板",
                        eventGroup.getGroupId(), i + 1, eventType);
                continue;
            }

            // 使用模板处理器生成payload数据
            Map<String, Object> payload = templateProcessor.processDataTemplate(payloadTemplate);

            // 创建终端事件
            TerminalEvent terminalEvent = new TerminalEvent(eventType, payload);
            terminalEvents.add(terminalEvent);
        }

        return terminalEvents;
    }

    /**
     * 将输入事件转换为终端事件
     */
    private List<TerminalEvent> convertInputEventsToTerminalEvents(List<BenchmarkConfig.InputEvent> inputEvents) {
        List<TerminalEvent> terminalEvents = new ArrayList<>();

        if (inputEvents == null || inputEvents.isEmpty()) {
            return terminalEvents;
        }

        for (BenchmarkConfig.InputEvent inputEvent : inputEvents) {
            TerminalEvent terminalEvent = new TerminalEvent(inputEvent.getType(), inputEvent.getPayload());
            terminalEvents.add(terminalEvent);
        }

        return terminalEvents;
    }

    /**
     * 随机选择事件类型
     */
    private String selectRandomEventType(List<String> typeSelector) {
        if (typeSelector == null || typeSelector.isEmpty()) {
            return null;
        }
        return typeSelector.get(random.nextInt(typeSelector.size()));
    }
}