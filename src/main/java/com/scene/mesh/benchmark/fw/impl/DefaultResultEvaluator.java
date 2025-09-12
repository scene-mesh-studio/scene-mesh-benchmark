package com.scene.mesh.benchmark.fw.impl;

import com.scene.mesh.benchmark.fw.config.ScoreThresholdConfig;
import com.scene.mesh.benchmark.fw.model.TestSuite;
import com.scene.mesh.benchmark.fw.model.TestSuiteReport;
import com.scene.mesh.benchmark.fw.model.TestUnit;
import com.scene.mesh.benchmark.fw.model.TestUnitResult;
import com.scene.mesh.benchmark.fw.sdk.model.TerminalAction;
import com.scene.mesh.benchmark.fw.spec.IResultEvaluator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public class DefaultResultEvaluator implements IResultEvaluator {
    
    private final ScoreThresholdConfig scoreThresholdConfig;

    // 存储所有测试单元结果，key为testUnitId
    private final Map<String, TestUnitResult> testUnitResults = new ConcurrentHashMap<>();

    // 存储所有测试套件报告，key为testSuiteId
    private final Map<String, TestSuiteReport> testSuiteReports = new ConcurrentHashMap<>();

    // 测试单元到测试套件的映射关系，key为testUnitId，value为testSuiteId
    private final Map<String, String> unitToSuiteMapping = new ConcurrentHashMap<>();


    @Override
    public TestUnitResult evaluateTestUnit(TestUnit testUnit, List<TerminalAction> receivedActions) {
        // 创建 TestUnitResult
        TestUnitResult result = new TestUnitResult(testUnit);

        // 设置接收到的动作
        result.setReceivedActions(receivedActions);

        // 评估测试结果，返回详细的评估结果
        EvaluationResult evaluationResult = evaluateTestResult(testUnit, receivedActions);

        // 完成测试执行（使用新的带分数的方法）
        result.complete(evaluationResult.getScore(), receivedActions,
                evaluationResult.getMatchedCount(), evaluationResult.getExpectedCount(),
                evaluationResult.getMissingActions(), evaluationResult.getUnexpectedActions());

        // 生成评估详情
        String details = generateEvaluationDetails(testUnit, receivedActions, evaluationResult);
        result.setEvaluationDetails(details);

        // 存储结果
        testUnitResults.put(testUnit.getId(), result);

        // 建立映射关系
        String testSuiteId = extractTestSuiteId(testUnit);
        if (testSuiteId != null) {
            unitToSuiteMapping.put(testUnit.getId(), testSuiteId);
        }
        return result;
    }

    @Override
    public TestSuiteReport evaluateTestSuite(TestSuite testSuite) {
        // 获取该测试套件的所有测试单元结果
        List<TestUnitResult> suiteTestUnitResults = getTestUnitResultsBySuiteId(testSuite.getId());

        TestSuiteReport report = new TestSuiteReport();
        report.setTestSuiteId(testSuite.getId());
        report.setTestSuiteName(testSuite.getName());
        report.setProductId(testSuite.getProduct().getId());
        report.setProductName(testSuite.getProduct().getName());
        report.setReportTime(Instant.now());

        // 计算时间范围
        if (!suiteTestUnitResults.isEmpty()) {
            Instant startTime = suiteTestUnitResults.stream()
                    .map(TestUnitResult::getStartTime)
                    .min(Instant::compareTo)
                    .orElse(Instant.now());
            Instant endTime = suiteTestUnitResults.stream()
                    .map(TestUnitResult::getEndTime)
                    .max(Instant::compareTo)
                    .orElse(Instant.now());

            report.setStartTime(startTime);
            report.setEndTime(endTime);
            report.setTotalDurationMs(endTime.toEpochMilli() - startTime.toEpochMilli());
        }

        // 生成统计信息
        TestSuiteReport.TestStatistics statistics = generateStatistics(suiteTestUnitResults);
        report.setStatistics(statistics);

        // 存储报告
        testSuiteReports.put(testSuite.getId(), report);

        return report;
    }

    private EvaluationResult evaluateTestResult(TestUnit testUnit, List<TerminalAction> receivedActions) {
        List<String> expectedActionIds = testUnit.getExpectedActionIds();
        List<String> receivedActionIds = receivedActions.stream()
                .map(TerminalAction::getMetaActionId)
                .collect(Collectors.toList());

        log.debug("开始评估 - 模式: {}", testUnit.getMode());
        log.debug("期望动作: {}", expectedActionIds);
        log.debug("实际接收动作: {}", receivedActionIds);

        return switch (testUnit.getMode()) {
            case POSITIVE_MATCH, BOUNDARY_CONDITION -> {
                // 对于需要接收动作的测试，如果没有收到任何动作，说明连接有问题
                if (receivedActionIds.isEmpty()) {
                    log.warn("测试单元 {} 未收到任何动作", testUnit.getId());
                    yield new EvaluationResult(0.0, false, 0, expectedActionIds != null ? expectedActionIds.size() : 0,
                            expectedActionIds, receivedActionIds, "未收到任何动作");
                }
                yield evaluatePositiveMatch(expectedActionIds, receivedActionIds);
            }
            case NEGATIVE_MATCH -> {
                // 对于 NEGATIVE_MATCH，需要确保连接正常但确实没有动作
                yield evaluateNegativeMatch(receivedActionIds);
            }
            default -> {
                log.warn("未知的测试模式: {}", testUnit.getMode());
                yield new EvaluationResult(0.0, false, 0, 0, null, receivedActionIds, "未知的测试模式");
            }
        };
    }

    private EvaluationResult evaluatePositiveMatch(List<String> expectedActionIds, List<String> receivedActionIds) {
        if (expectedActionIds == null || expectedActionIds.isEmpty()) {
            boolean passed = receivedActionIds.isEmpty();
            return new EvaluationResult(
                passed ? 1.0 : 0.0, 
                passed, 
                0, 0, 
                expectedActionIds, receivedActionIds,
                passed ? "期望无动作，实际无动作" : "期望无动作，但收到了动作"
            );
        }

        // 去重处理
        Set<String> expectedSet = new HashSet<>(expectedActionIds);
        Set<String> receivedSet = new HashSet<>(receivedActionIds);
        
        // 计算匹配的动作
        Set<String> matchedActions = new HashSet<>(expectedSet);
        matchedActions.retainAll(receivedSet);
        
        // 计算缺失的动作
        Set<String> missingActions = new HashSet<>(expectedSet);
        missingActions.removeAll(receivedSet);
        
        // 计算意外的动作
        Set<String> unexpectedActions = new HashSet<>(receivedSet);
        unexpectedActions.removeAll(expectedSet);
        
        int matchedCount = matchedActions.size();
        int expectedCount = expectedSet.size();
        
        // 计算匹配分数
        double score = (double) matchedCount / expectedCount;
        
        // 判断是否通过（可以根据需要调整阈值）
        boolean passed = score >= 1.0; // 或者可以设置阈值，比如 score >= 0.8
        
        // 生成详细信息
        StringBuilder details = new StringBuilder();
        details.append(String.format("匹配分数: %.2f (%d/%d)\n", score, matchedCount, expectedCount));
        details.append("匹配的动作: ").append(matchedActions).append("\n");
        if (!missingActions.isEmpty()) {
            details.append("缺失的动作: ").append(missingActions).append("\n");
        }
        if (!unexpectedActions.isEmpty()) {
            details.append("意外的动作: ").append(unexpectedActions).append("\n");
        }
        
        log.debug("评估结果 - 分数: {}, 通过: {}, 详情: {}", score, passed, details.toString());
        
        return new EvaluationResult(score, passed, matchedCount, expectedCount, 
                new java.util.ArrayList<>(missingActions), new java.util.ArrayList<>(unexpectedActions), details.toString());
    }

    private EvaluationResult evaluateNegativeMatch(List<String> receivedActionIds) {
        boolean passed = receivedActionIds.isEmpty();
        double score = passed ? 1.0 : 0.0;
        String details = passed ? "期望无动作，实际无动作" : "期望无动作，但收到了动作: " + receivedActionIds;
        
        return new EvaluationResult(score, passed, 0, 0, null, receivedActionIds, details);
    }

    private String generateEvaluationDetails(TestUnit testUnit, List<TerminalAction> receivedActions, EvaluationResult evaluationResult) {
        StringBuilder details = new StringBuilder();
        details.append("测试模式: ").append(testUnit.getMode()).append("\n");
        details.append("期望动作: ").append(testUnit.getExpectedActionIds()).append("\n");
        details.append("实际动作: ").append(receivedActions.stream()
                .map(TerminalAction::getMetaActionId)
                .collect(Collectors.toList())).append("\n");
        
        // 使用分数等级而不是布尔值
        TestUnitResult.ScoreThreshold level = TestUnitResult.ScoreThreshold.fromScore(evaluationResult.getScore());
        details.append("评估结果: ").append(String.format("%.2f", evaluationResult.getScore()))
               .append(" (").append(level.getDescription()).append(")").append("\n");
        details.append("匹配详情: ").append(evaluationResult.getDetails());

        return details.toString();
    }

    private TestSuiteReport.TestStatistics generateStatistics(List<TestUnitResult> testUnitResults) {
        TestSuiteReport.TestStatistics statistics = new TestSuiteReport.TestStatistics();

        // 基础统计
        statistics.setTotalTestUnits(testUnitResults.size());
        
        // 基于分数阈值计算通过/失败统计
        int passedTestUnits = (int) testUnitResults.stream()
                .filter(result -> {
                    if (result.getTestUnit() != null) {
                        double threshold = scoreThresholdConfig.getSuccessThreshold(result.getTestUnit().getMode().name());
                        return result.isSuccessful(threshold);
                    }
                    return result.isSuccessful(); // 回退到默认阈值
                })
                .count();
        statistics.setPassedTestUnits(passedTestUnits);
        statistics.setFailedTestUnits(statistics.getTotalTestUnits() - statistics.getPassedTestUnits());

        // 计算通过率和失败率
        if (statistics.getTotalTestUnits() > 0) {
            statistics.setPassRate((double) statistics.getPassedTestUnits() / statistics.getTotalTestUnits());
            statistics.setFailureRate((double) statistics.getFailedTestUnits() / statistics.getTotalTestUnits());
        }

        // 计算平均执行时间
        double avgTime = testUnitResults.stream()
                .mapToLong(TestUnitResult::getDurationMs)
                .average()
                .orElse(0.0);
        statistics.setAverageExecutionTimeMs(avgTime);

        // 计算分数统计
        List<Double> scores = testUnitResults.stream()
                .map(TestUnitResult::getMatchScore)
                .filter(score -> score != null)
                .collect(Collectors.toList());
        
        if (!scores.isEmpty()) {
            statistics.setAverageMatchScore(scores.stream().mapToDouble(Double::doubleValue).average().orElse(0.0));
            statistics.setMaxMatchScore(scores.stream().mapToDouble(Double::doubleValue).max().orElse(0.0));
            statistics.setMinMatchScore(scores.stream().mapToDouble(Double::doubleValue).min().orElse(0.0));
        }

        // 计算分数分布
        TestSuiteReport.ScoreDistribution scoreDistribution = calculateScoreDistribution(testUnitResults);
        statistics.setScoreDistribution(scoreDistribution);
        
        // 计算整体质量等级
        if (statistics.getAverageMatchScore() > 0) {
            TestUnitResult.ScoreThreshold overallLevel = TestUnitResult.ScoreThreshold.fromScore(statistics.getAverageMatchScore());
            statistics.setOverallQualityLevel(overallLevel.getDescription());
        } else {
            statistics.setOverallQualityLevel("未评估");
        }

        // 按测试模式分组统计
        Map<String, TestSuiteReport.ModeStatistics> modeStats = testUnitResults.stream()
                .collect(Collectors.groupingBy(
                        result -> result.getTestUnit().getMode().name(),
                        Collectors.collectingAndThen(
                                Collectors.toList(),
                                results -> {
                                    TestSuiteReport.ModeStatistics modeStat = new TestSuiteReport.ModeStatistics();
                                    modeStat.setTotal(results.size());
                                    
                                    // 使用该模式的特定阈值
                                    String modeName = results.get(0).getTestUnit().getMode().name();
                                    double modeThreshold = scoreThresholdConfig.getSuccessThreshold(modeName);
                                    
                                    modeStat.setPassed((int) results.stream()
                                            .filter(result -> result.isSuccessful(modeThreshold))
                                            .count());
                                    modeStat.setFailed(modeStat.getTotal() - modeStat.getPassed());
                                    if (modeStat.getTotal() > 0) {
                                        modeStat.setPassRate((double) modeStat.getPassed() / modeStat.getTotal());
                                    }
                                    
                                    // 计算该模式的分数统计
                                    List<Double> modeScores = results.stream()
                                            .map(TestUnitResult::getMatchScore)
                                            .filter(score -> score != null)
                                            .collect(Collectors.toList());
                                    
                                    if (!modeScores.isEmpty()) {
                                        modeStat.setAverageMatchScore(modeScores.stream().mapToDouble(Double::doubleValue).average().orElse(0.0));
                                        modeStat.setMaxMatchScore(modeScores.stream().mapToDouble(Double::doubleValue).max().orElse(0.0));
                                        modeStat.setMinMatchScore(modeScores.stream().mapToDouble(Double::doubleValue).min().orElse(0.0));
                                    }
                                    
                                    return modeStat;
                                }
                        )
                ));
        statistics.setModeStatistics(modeStats);

        return statistics;
    }

    private TestSuiteReport.ScoreDistribution calculateScoreDistribution(List<TestUnitResult> testUnitResults) {
        TestSuiteReport.ScoreDistribution distribution = new TestSuiteReport.ScoreDistribution();
        
        for (TestUnitResult result : testUnitResults) {
            Double score = result.getMatchScore();
            if (score == null) {
                distribution.setNoScores(distribution.getNoScores() + 1);
            } else if (score >= 1.0) {
                distribution.setPerfectScores(distribution.getPerfectScores() + 1);
            } else if (score >= 0.8) {
                distribution.setHighScores(distribution.getHighScores() + 1);
            } else if (score >= 0.6) {
                distribution.setMediumScores(distribution.getMediumScores() + 1);
            } else if (score >= 0.4) {
                distribution.setLowScores(distribution.getLowScores() + 1);
            } else {
                distribution.setVeryLowScores(distribution.getVeryLowScores() + 1);
            }
        }
        
        return distribution;
    }

    private String extractTestSuiteId(TestUnit testUnit) {
        try {
            return testUnit.getScene().getProductId();
        } catch (Exception e) {
            log.warn("无法提取测试套件ID: {}", e.getMessage());
            return null;
        }
    }

    private List<TestUnitResult> getTestUnitResultsBySuiteId(String testSuiteId) {
        return testUnitResults.entrySet().stream()
                .filter(entry -> testSuiteId.equals(unitToSuiteMapping.get(entry.getKey())))
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
    }

    /**
     * 评估结果内部类
     */
    private static class EvaluationResult {
        private final double score;
        private final boolean passed;
        private final int matchedCount;
        private final int expectedCount;
        private final List<String> missingActions;
        private final List<String> unexpectedActions;
        private final String details;
        
        public EvaluationResult(double score, boolean passed, int matchedCount, int expectedCount,
                               List<String> missingActions, List<String> unexpectedActions, String details) {
            this.score = score;
            this.passed = passed;
            this.matchedCount = matchedCount;
            this.expectedCount = expectedCount;
            this.missingActions = missingActions;
            this.unexpectedActions = unexpectedActions;
            this.details = details;
        }
        
        public double getScore() { return score; }
        public boolean isPassed() { return passed; }
        public int getMatchedCount() { return matchedCount; }
        public int getExpectedCount() { return expectedCount; }
        public List<String> getMissingActions() { return missingActions; }
        public List<String> getUnexpectedActions() { return unexpectedActions; }
        public String getDetails() { return details; }
    }
}
