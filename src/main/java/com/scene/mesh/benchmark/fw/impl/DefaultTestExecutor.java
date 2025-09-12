package com.scene.mesh.benchmark.fw.impl;

import com.scene.mesh.benchmark.fw.config.ScoreThresholdConfig;
import com.scene.mesh.benchmark.fw.model.*;
import com.scene.mesh.benchmark.fw.sdk.client.TerminalClient;
import com.scene.mesh.benchmark.fw.sdk.client.TerminalClientBuilder;
import com.scene.mesh.benchmark.fw.sdk.model.TerminalAction;
import com.scene.mesh.benchmark.fw.sdk.model.TerminalEvent;
import com.scene.mesh.benchmark.fw.sdk.model.TerminalProtocolType;
import com.scene.mesh.benchmark.fw.spec.IResultEvaluator;
import com.scene.mesh.benchmark.fw.spec.ITestExecutor;
import com.scene.mesh.benchmark.fw.spec.ITestSuiteManager;
import com.scene.mesh.model.event.Event;
import com.scene.mesh.model.product.Product;
import com.scene.mesh.model.protocol.ProtocolType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
public class DefaultTestExecutor implements ITestExecutor {

    private final ITestSuiteManager testSuiteManager;

    private final IResultEvaluator resultEvaluator;

    private final ScoreThresholdConfig scoreThresholdConfig;

    private final String mqttUrl;

    private final String webSocketUrl;

    public DefaultTestExecutor(ITestSuiteManager testSuiteManager, IResultEvaluator resultEvaluator, 
                              ScoreThresholdConfig scoreThresholdConfig, String mqttUrl, String webSocketUrl) {
        this.mqttUrl = mqttUrl;
        this.webSocketUrl = webSocketUrl;
        this.testSuiteManager = testSuiteManager;
        this.resultEvaluator = resultEvaluator;
        this.scoreThresholdConfig = scoreThresholdConfig;
    }

    @Override
    public void executeAllSuites(List<TestSuite> testSuites){
        log.info("开始执行所有测试套件...");
        if (testSuites.isEmpty()) {
            log.warn("没有找到任何测试套件");
            return;
        }

        log.info("找到 {} 个测试套件", testSuites.size());

        for (TestSuite testSuite : testSuites) {
            log.info("执行测试套件: {} ({})", testSuite.getName(), testSuite.getId());
            executeSuite(testSuite);

            // 为每个测试套件生成报告
            TestSuiteReport report = resultEvaluator.evaluateTestSuite(testSuite);
            
            // 输出详细报告
            report.logReport();
            
            // 输出简化摘要
            report.logSummary();
        }
    }

    @Override
    public void executeAllSuites() {
        List<TestSuite> testSuites = testSuiteManager.getTestSuites();
        this.executeAllSuites(testSuites);
    }

    private void executeSuite(TestSuite testSuite) {

            if (testSuite.getScenarios() == null || testSuite.getScenarios().isEmpty()) {
                log.warn("测试套件 {} 没有测试场景", testSuite.getName());
                return;
            }

            for (TestScenario testScenario : testSuite.getScenarios()) {
                log.info("执行测试场景: {} ({})", testScenario.getName(), testScenario.getId());
                executeScenario(testScenario);
            }
    }

    private void executeScenario(TestScenario testScenario) {

        if (testScenario.getTestCases() == null || testScenario.getTestCases().isEmpty()) {
            log.warn("测试场景 {} 没有测试用例", testScenario.getName());
            return;
        }
        
        for (TestCase testCase : testScenario.getTestCases()) {
            log.info("执行测试用例: {} ({})", testCase.getName(), testCase.getId());
            executeTestCase(testScenario.getProduct().getId(),testCase);
        }
    }

    private void executeTestCase(String productId, TestCase testCase) {

        try {
            log.debug("开始执行测试用例: {}", testCase.getName());
            
            // 执行测试单元
            boolean passed = executeTestUnits(productId, testCase);

            log.info("测试用例 {} 执行{}: {}", 
                    testCase.getName(), 
                    passed ? "通过" : "失败",
                    passed ? "✓" : "✗");
            
        } catch (Exception e) {
            log.error("测试用例 {} 执行异常: {}", testCase.getName(), e.getMessage(), e);
        }
    }

    private boolean executeTestUnits(String productId, TestCase testCase) {
        if (testCase.getTestUnits() == null || testCase.getTestUnits().isEmpty()) {
            log.warn("测试用例 {} 没有测试单元", testCase.getName());
            return false;
        }
        
        boolean allPassed = true;
        
        for (TestUnit testUnit : testCase.getTestUnits()) {
            log.debug("执行测试单元: {} (模式: {})", testUnit.getId(), testUnit.getMode());
            
            try {
                boolean unitSuccessful = executeTestUnit(testUnit);
                if (!unitSuccessful) {
                    allPassed = false;
                    log.warn("测试单元 {} 执行失败", testUnit.getId());
                } else {
                    log.debug("测试单元 {} 执行成功", testUnit.getId());
                }
            } catch (Exception e) {
                log.error("测试单元 {} 执行异常: {}", testUnit.getId(), e.getMessage(), e);
                allPassed = false;
            }
        }
        
        return allPassed;
    }

    private boolean executeTestUnit(TestUnit testUnit) {
        try {
        // 使用 CompletableFuture 实现超时控制
        CompletableFuture<Boolean> future = CompletableFuture.supplyAsync(() ->
                performTestUnitExecution(testUnit));
        
        // 等待执行完成或超时
        Boolean result = future.get(testUnit.getTimeoutMs(), TimeUnit.MILLISECONDS);
        return result != null && result;
        
    } catch (java.util.concurrent.TimeoutException e) {
        log.error("测试单元 {} 执行超时 ({}ms): {}", testUnit.getId(), testUnit.getTimeoutMs(), e.getMessage());
        // 超时情况下，所有测试都应该失败
        return false;
    } catch (Exception e) {
        log.error("测试单元 {} 执行异常: {}", testUnit.getId(), e.getMessage(), e);
        // 异常情况下，所有测试都应该失败
        return false;
    }
    }

    private boolean performTestUnitExecution(TestUnit testUnit) {
        log.debug("执行测试单元: {} - 模式: {}", testUnit.getId(), testUnit.getMode());
        log.debug("输入事件数量: {}",
                testUnit.getInputEvents() != null ? testUnit.getInputEvents().size() : 0);
        log.debug("预期动作数量: {}",
                testUnit.getExpectedActionIds() != null ? testUnit.getExpectedActionIds().size() : 0);

        // 为每个测试单元创建独立的连接
        TerminalClient terminalClient = createTerminalClient(testUnit);
        terminalClient.connect();
        if (terminalClient == null) {
            log.error("无法为TestUnit {} 创建终端客户端", testUnit.getId());
            return false;
        }

        // 创建简化的动作收集器
        SimpleActionCollector actionCollector = new SimpleActionCollector(testUnit.getId());

        try {
            // 设置动作处理器 - 直接收集所有动作（因为连接已隔离）
            terminalClient.onAction(actionCollector::collectAction);

            // 发送所有输入事件
            if (testUnit.getInputEvents() != null) {
                for (Event event : testUnit.getInputEvents()) {
                    TerminalEvent te = new TerminalEvent(event.getType(), event.getPayload());
                    CompletableFuture<Boolean> sendFuture = terminalClient.sendEvent(te);
                    Boolean sendResult = sendFuture.get(3000, TimeUnit.MILLISECONDS);
                    if (sendResult == null || !sendResult) {
                        log.error("事件发送失败: {} - {}", event.getType(), event.getPayload());
                        return false; // 发送失败，测试单元失败
                    }
                }
            }

            // 等待指定时间收集动作
//            long waitTime = testUnit.getTimeoutMs() != null ? testUnit.getTimeoutMs() : 10000L;
            long waitTime = testUnit.getMode().equals(TestMode.NEGATIVE_MATCH) ? 10000L : testUnit.getTimeoutMs() - 10000;
            log.debug("等待 {}ms 收集动作...", waitTime);
            Thread.sleep(waitTime);

            // 获取收集到的动作
            List<TerminalAction> receivedActions = actionCollector.getReceivedActions();
            log.debug("接收到 {} 个动作", receivedActions.size());

            // 使用 ResultEvaluator 评估并存储结果
            TestUnitResult result = resultEvaluator.evaluateTestUnit(testUnit, receivedActions);

            // 基于分数评估结果
            if (result.getMatchScore() != null) {
                TestUnitResult.ScoreThreshold level = result.getScoreLevel();
                double threshold = scoreThresholdConfig.getSuccessThreshold(testUnit.getMode().name());
                boolean isSuccessful = result.isSuccessful(threshold);
                
                log.debug("评估结果: 分数 {} ({}) - 阈值: {} - 结果: {}",
                         result.getMatchScore(), level.getDescription(), threshold, 
                         isSuccessful ? "成功" : "失败");
                return isSuccessful;
            } else {
                log.warn("测试单元 {} 未获得有效分数", testUnit.getId());
                return false;
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("测试单元 {} 执行被中断", testUnit.getId());
            return false;
        } catch (java.util.concurrent.TimeoutException e) {
            log.error("测试单元 {} 事件发送超时", testUnit.getId());
            return false;
        } catch (Exception e) {
            log.error("测试单元 {} 执行异常: {}", testUnit.getId(), e.getMessage(), e);
            return false;
        } finally {
            // 确保连接被正确关闭
            closeTerminalClient(terminalClient, testUnit.getId());
        }
    }


    /**
     * 简化的动作收集器，用于收集测试单元的所有动作
     * 由于使用连接隔离，不需要时间窗口过滤
     */
    private static class SimpleActionCollector {
        private final String testUnitId;
        private final List<TerminalAction> receivedActions = new ArrayList<>();
        private final Set<String> collectedActionIds = new HashSet<>();

        public SimpleActionCollector(String testUnitId) {
            this.testUnitId = testUnitId;
        }

        public void collectAction(TerminalAction action) {
            // 避免重复收集
            if (collectedActionIds.contains(action.getId())) {
                log.debug("测试单元 {} 忽略重复动作: {}", testUnitId, action.getId());
                return;
            }
            
            receivedActions.add(action);
            collectedActionIds.add(action.getId());
            Instant actionTimestamp = action.getReceivedTimestamp();
            log.debug("测试单元 {} 收集到动作: {} (时间戳: {})", 
                    testUnitId, action.getId(), actionTimestamp);
        }

        public List<TerminalAction> getReceivedActions() {
            return new ArrayList<>(receivedActions);
        }

        public String getTestUnitId() {
            return testUnitId;
        }
    }

    /**
     * 为测试单元创建独立的终端客户端连接
     */
    private TerminalClient createTerminalClient(TestUnit testUnit) {
        Product product = testUnit.getProduct();
        try {
            // 获取产品信息
            if (product == null) {
                log.error("找不到产品: {}", product.getName());
                return null;
            }

            // 获取密钥
            String[] sks = product.getSettings().getSecretKey();
            String secretKey;
            if (sks == null || sks.length == 0) {
                log.error("找不到产品的密钥配置: {}", product.getName());
                return null;
            }
            secretKey = sks[0];

            // 确定协议类型和服务器URL
            List<ProtocolType> protocolTypes = product.getSettings().getProtocolConfig().getSupportedProtocolTypes();
            TerminalProtocolType terminalProtocolType;
            String serverUrl;
            
            if (protocolTypes.contains(ProtocolType.MQTT)) {
                terminalProtocolType = TerminalProtocolType.MQTT;
                serverUrl = mqttUrl;
            } else if (protocolTypes.contains(ProtocolType.WEBSOCKET)) {
                terminalProtocolType = TerminalProtocolType.WEBSOCKET;
                serverUrl = webSocketUrl;
            } else {
                log.error("不支持的产品协议类型: {}", protocolTypes);
                return null;
            }

            // 创建终端客户端
            TerminalClient terminalClient = TerminalClientBuilder.builder()
                    .productId(product.getId())
                    .terminalId("Test-" + product.getId() + "-" + testUnit.getId()) // 使用时间戳确保唯一性
                    .secretKey(secretKey)
                    .protocol(terminalProtocolType)
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
     * 关闭终端客户端连接
     */
    private void closeTerminalClient(TerminalClient terminalClient, String testUnitId) {
        if (terminalClient != null) {
            try {
                terminalClient.disconnect();
            } catch (Exception e) {
                log.warn("关闭测试单元 {} 的终端客户端连接时发生异常: {}", testUnitId, e.getMessage());
            }
        }
    }
}
