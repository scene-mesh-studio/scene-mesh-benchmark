package com.scene.mesh.benchmark.n.impl;

import com.scene.mesh.benchmark.n.model.TestSuite;
import com.scene.mesh.benchmark.n.model.TestSuiteReport;
import com.scene.mesh.benchmark.n.spec.ITestSuiteExecutor;
import com.scene.mesh.sdk.client.TerminalClient;
import com.scene.mesh.sdk.client.TerminalClientBuilder;
import com.scene.mesh.sdk.model.TerminalAction;
import com.scene.mesh.sdk.model.TerminalEvent;
import com.scene.mesh.sdk.model.TerminalProtocolType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Component
public class DefaultTestSuiteExecutor implements ITestSuiteExecutor {

    @Value("${scene-mesh.facade.url.mqtt}")
    private String mqttUrl;

    @Value("${scene-mesh.facade.url.websocket}")
    private String webSocketUrl;

    @Override
    public TestSuiteReport execute(TestSuite testSuite) {
        log.info("开始执行测试套件: {} (ID: {})", testSuite.getName(), testSuite.getId());

        LocalDateTime startTime = LocalDateTime.now();

        try {
            // 1. 创建终端客户端连接
            TerminalClient terminalClient = createTerminalClient(testSuite);
            if (terminalClient == null) {
                throw new RuntimeException("无法创建终端客户端连接");
            }

            // 2. 连接并设置动作收集器
            terminalClient.connect();
            SimpleActionCollector actionCollector = new SimpleActionCollector(testSuite.getId());
            terminalClient.onAction(actionCollector::collectAction);

            // 3. 发送所有事件
            List<String> sentEventIds = sendEvents(testSuite, terminalClient);

            // 4. 等待收集动作
            long waitTime = testSuite.getDurationOfWaitingActions() != null ?
                    testSuite.getDurationOfWaitingActions() :  20000L; // 等待10秒收集动作
            log.debug("等待 {}ms 收集动作...", waitTime);
            Thread.sleep(waitTime);

            // 5. 获取收集到的动作
            List<TerminalAction> receivedActions = actionCollector.getReceivedActions();
            log.info("收集到 {} 个动作", receivedActions.size());

            // 6. 构建执行报告
            TestSuiteReport report = buildReport(testSuite, sentEventIds, receivedActions, startTime);

            log.info("测试套件执行完成: {} - 状态: {}, 分数: {:.2f}",
                    testSuite.getName(), report.getStatus(), report.getMatchScore());

            return report;

        } catch (Exception e) {
            log.error("测试套件执行失败: {}", testSuite.getName(), e);

            return TestSuiteReport.builder()
                    .testSuiteId(testSuite.getId())
                    .testSuiteName(testSuite.getName())
                    .productId(testSuite.getProductId())
                    .status(TestSuiteReport.ExecutionStatus.FAILED)
                    .startTime(startTime)
                    .endTime(LocalDateTime.now())
                    .errorMessage(e.getMessage())
                    .matchScore(0.0)
                    .scoreLevel(TestSuiteReport.ScoreLevel.FAILED)
                    .passed(false)
                    .build();
        }
    }

    /**
     * 创建终端客户端连接
     */
    private TerminalClient createTerminalClient(TestSuite testSuite) {
        try {
            TerminalProtocolType protocolType;
            String serverUrl;
            // 确定协议类型和服务器URL
            if ("MQTT".equals(testSuite.getProtocol())) {
                protocolType = TerminalProtocolType.MQTT;
                serverUrl = mqttUrl;

            }else if ("WS".equals(testSuite.getProtocol())) {
                protocolType = TerminalProtocolType.WEBSOCKET;
                serverUrl = webSocketUrl;
            }else {
                throw new RuntimeException("Illegal protocol: " + testSuite.getProtocol());
            }

            // 创建终端客户端
            TerminalClient terminalClient = TerminalClientBuilder.builder()
                    .productId(testSuite.getProductId())
                    .terminalId("Test-" + testSuite.getProductId() + "-" + testSuite.getId())
                    .secretKey(testSuite.getSecretKey())
                    .protocol(protocolType)
                    .protocolVersion("v1")
                    .serverUrl(serverUrl)
                    .build();

            return terminalClient;

        } catch (Exception e) {
            log.error("创建终端客户端失败: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 发送测试套件中的所有事件
     */
    private List<String> sendEvents(TestSuite testSuite, TerminalClient terminalClient) {
        List<String> sentEventIds = new ArrayList<>();

        log.info("开始发送 {} 个事件", testSuite.getTerminalEvents().size());

        for (var event : testSuite.getTerminalEvents()) {
            try {
                // 发送事件到 Scene Mesh 服务端
                TerminalEvent terminalEvent = new TerminalEvent(event.getType(), event.getPayload());
                CompletableFuture<Boolean> sendFuture = terminalClient.sendEvent(terminalEvent);
                Boolean sendResult = sendFuture.get(3000, TimeUnit.MILLISECONDS);

                if (sendResult == null || !sendResult) {
                    log.error("事件发送失败: {} - {}", event.getType(), event.getPayload());
                } else {
                    sentEventIds.add(event.getId());
                    log.debug("事件发送成功: {}", event.getId());
                }

            } catch (Exception e) {
                log.error("事件发送异常: {}", event.getId(), e);
            }
        }

        log.info("事件发送完成，成功发送 {}/{} 个事件", sentEventIds.size(), testSuite.getTerminalEvents().size());
        return sentEventIds;
    }

    /**
     * 构建执行报告
     */
    private TestSuiteReport buildReport(TestSuite testSuite,
                                        List<String> sentEventIds,
                                        List<TerminalAction> receivedActions,
                                        LocalDateTime startTime) {
        LocalDateTime endTime = LocalDateTime.now();
        long executionTimeMs = java.time.Duration.between(startTime, endTime).toMillis();

        // 事件统计
        int totalEventsSent = testSuite.getTerminalEvents().size();
        int successfulEvents = sentEventIds.size();
        int failedEvents = totalEventsSent - successfulEvents;

        // 动作匹配分析
        List<String> expectedActions = testSuite.getExpectedActionIds();
        List<String> actualActionIds = receivedActions.stream()
                .map(TerminalAction::getMetaActionId)
                .collect(Collectors.toList());

        // 去重处理
        Set<String> expectedSet = new HashSet<>(expectedActions);
        Set<String> actualSet = new HashSet<>(actualActionIds);

        // 计算匹配的动作
        Set<String> matchedActions = new HashSet<>(expectedSet);
        matchedActions.retainAll(actualSet);

        // 计算缺失的动作
        Set<String> missedActions = new HashSet<>(expectedSet);
        missedActions.removeAll(actualSet);

        // 计算意外的动作
        Set<String> unexpectedActions = new HashSet<>(actualSet);
        unexpectedActions.removeAll(expectedSet);

        // 计算匹配分数
        double matchScore = calculateMatchScore(expectedActions, actualActionIds);

        // 确定分数等级
        TestSuiteReport.ScoreLevel scoreLevel = determineScoreLevel(matchScore);

        // 确定执行状态
        TestSuiteReport.ExecutionStatus status = determineExecutionStatus(
                totalEventsSent, successfulEvents, matchScore);

        return TestSuiteReport.builder()
                .testSuiteId(testSuite.getId())
                .testSuiteName(testSuite.getName())
                .productId(testSuite.getProductId())
                .status(status)
                .startTime(startTime)
                .endTime(endTime)
                .executionTimeMs(executionTimeMs)
                .totalEventsSent(totalEventsSent)
                .successfulEvents(successfulEvents)
                .failedEvents(failedEvents)
                .expectedActions(expectedActions)
                .actualActions(new ArrayList<>(actualSet))
                .matchedActions(new ArrayList<>(matchedActions))
                .missedActions(new ArrayList<>(missedActions))
                .unexpectedActions(new ArrayList<>(unexpectedActions))
                .matchScore(matchScore)
                .scoreLevel(scoreLevel)
                .passed(matchScore >= 0.75) // 默认阈值
                .build();
    }

    /**
     * 计算匹配分数
     * 分数 = 匹配的动作数 / 期望的动作数
     */
    private double calculateMatchScore(List<String> expectedActions, List<String> actualActions) {
        if (expectedActions.isEmpty()) {
            return actualActions.isEmpty() ? 1.0 : 0.0;
        }

        Set<String> expectedSet = new HashSet<>(expectedActions);
        Set<String> actualSet = new HashSet<>(actualActions);

        long matchedCount = actualSet.stream()
                .mapToLong(action -> expectedSet.contains(action) ? 1 : 0)
                .sum();

        return (double) matchedCount / expectedSet.size();
    }

    private TestSuiteReport.ScoreLevel determineScoreLevel(double score) {
        if (score >= 0.95) return TestSuiteReport.ScoreLevel.EXCELLENT;
        if (score >= 0.85) return TestSuiteReport.ScoreLevel.GOOD;
        if (score >= 0.75) return TestSuiteReport.ScoreLevel.ACCEPTABLE;
        if (score >= 0.60) return TestSuiteReport.ScoreLevel.POOR;
        return TestSuiteReport.ScoreLevel.FAILED;
    }

    private TestSuiteReport.ExecutionStatus determineExecutionStatus(
            int totalEvents, int successfulEvents, double matchScore) {
        if (totalEvents == 0) return TestSuiteReport.ExecutionStatus.SKIPPED;
        if (successfulEvents == totalEvents && matchScore >= 0.75) {
            return TestSuiteReport.ExecutionStatus.SUCCESS;
        }
        if (successfulEvents > 0 && matchScore > 0) {
            return TestSuiteReport.ExecutionStatus.PARTIAL_SUCCESS;
        }
        return TestSuiteReport.ExecutionStatus.FAILED;
    }

    /**
     * 简化的动作收集器，用于收集测试套件的所有动作
     */
    private static class SimpleActionCollector {
        private final String testSuiteId;
        private final List<TerminalAction> receivedActions = new ArrayList<>();
        private final Set<String> collectedActionIds = new HashSet<>();

        public SimpleActionCollector(String testSuiteId) {
            this.testSuiteId = testSuiteId;
        }

        public void collectAction(TerminalAction action) {
            // 避免重复收集
            if (collectedActionIds.contains(action.getId())) {
                log.debug("测试套件 {} 忽略重复动作: {}", testSuiteId, action.getId());
                return;
            }

            receivedActions.add(action);
            collectedActionIds.add(action.getId());
            log.debug("测试套件 {} 收集到动作: {} (时间戳: {})",
                    testSuiteId, action, action.getReceivedTimestamp());
        }

        public List<TerminalAction> getReceivedActions() {
            return new ArrayList<>(receivedActions);
        }

        public String getTestSuiteId() {
            return testSuiteId;
        }
    }
}