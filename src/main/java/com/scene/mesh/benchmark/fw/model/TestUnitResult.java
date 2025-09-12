package com.scene.mesh.benchmark.fw.model;

import com.scene.mesh.benchmark.fw.sdk.model.TerminalAction;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Collectors;

@Data
public class TestUnitResult {

    /**
     * 测试单元
     */
    private TestUnit testUnit;

    /**
     * 开始时间
     */
    private Instant startTime;

    /**
     * 结束时间
     */
    private Instant endTime;

    /**
     * 执行时长（毫秒）
     */
    private Long durationMs;

    /**
     * 分数阈值枚举
     */
    public enum ScoreThreshold {
        EXCELLENT(0.95, "优秀"),
        GOOD(0.85, "良好"), 
        ACCEPTABLE(0.75, "可接受"),
        POOR(0.60, "较差"),
        FAILED(0.0, "失败");
        
        private final double threshold;
        private final String description;
        
        ScoreThreshold(double threshold, String description) {
            this.threshold = threshold;
            this.description = description;
        }
        
        public double getThreshold() { return threshold; }
        public String getDescription() { return description; }
        
        public static ScoreThreshold fromScore(double score) {
            if (score >= EXCELLENT.threshold) return EXCELLENT;
            if (score >= GOOD.threshold) return GOOD;
            if (score >= ACCEPTABLE.threshold) return ACCEPTABLE;
            if (score >= POOR.threshold) return POOR;
            return FAILED;
        }
    }

    /**
     * 接收到的动作列表
     */
    private List<TerminalAction> receivedActions;

    /**
     * 错误信息（如果有）
     */
    private String errorMessage;

    /**
     * 评估详情
     */
    private String evaluationDetails;

    /**
     * 测试执行状态
     */
    private TestExecutionStatus status;

    /**
     * 匹配分数 (0.0 - 1.0)
     */
    private Double matchScore;

    /**
     * 匹配的动作数量
     */
    private Integer matchedActionCount;

    /**
     * 期望的动作数量
     */
    private Integer expectedActionCount;

    /**
     * 缺失的动作ID列表
     */
    private List<String> missingActionIds;

    /**
     * 意外的动作ID列表
     */
    private List<String> unexpectedActionIds;

    /**
     * 测试执行状态枚举
     */
    public enum TestExecutionStatus {
        /**
         * 执行中
         */
        RUNNING,

        /**
         * 执行完成
         */
        COMPLETED,

        /**
         * 执行超时
         */
        TIMEOUT,

        /**
         * 执行失败
         */
        FAILED,

        /**
         * 执行中断
         */
        INTERRUPTED
    }

    /**
     * 构造函数
     */
    public TestUnitResult() {
        this.status = TestExecutionStatus.RUNNING;
        this.startTime = Instant.now();
    }

    /**
     * 构造函数
     * @param testUnit 测试单元
     */
    public TestUnitResult(TestUnit testUnit) {
        this();
        this.testUnit = testUnit;
    }

    /**
     * 完成测试执行
     * @param receivedActions 接收到的动作
     */
    public void complete(List<TerminalAction> receivedActions) {
        this.endTime = Instant.now();
        this.receivedActions = receivedActions;
        this.status = TestExecutionStatus.COMPLETED;

        if (this.startTime != null) {
            this.durationMs = this.endTime.toEpochMilli() - this.startTime.toEpochMilli();
        }
    }

    /**
     * 完成测试执行（带分数信息）
     * @param matchScore 匹配分数
     * @param receivedActions 接收到的动作
     * @param matchedCount 匹配的动作数量
     * @param expectedCount 期望的动作数量
     * @param missingActions 缺失的动作ID列表
     * @param unexpectedActions 意外的动作ID列表
     */
    public void complete(double matchScore, List<TerminalAction> receivedActions, 
                        int matchedCount, int expectedCount, List<String> missingActions, 
                        List<String> unexpectedActions) {
        this.endTime = Instant.now();
        this.matchScore = matchScore;
        this.receivedActions = receivedActions;
        this.matchedActionCount = matchedCount;
        this.expectedActionCount = expectedCount;
        this.missingActionIds = missingActions;
        this.unexpectedActionIds = unexpectedActions;
        this.status = TestExecutionStatus.COMPLETED;

        if (this.startTime != null) {
            this.durationMs = this.endTime.toEpochMilli() - this.startTime.toEpochMilli();
        }
    }

    /**
     * 标记为超时
     * @param receivedActions 接收到的动作
     */
    public void timeout(List<TerminalAction> receivedActions) {
        this.endTime = Instant.now();
        this.matchScore = 0.0; // 超时视为失败，分数为0
        this.receivedActions = receivedActions;
        this.status = TestExecutionStatus.TIMEOUT;
        this.errorMessage = "测试执行超时";

        if (this.startTime != null) {
            this.durationMs = this.endTime.toEpochMilli() - this.startTime.toEpochMilli();
        }
    }

    /**
     * 标记为失败
     * @param errorMessage 错误信息
     */
    public void fail(String errorMessage) {
        this.endTime = Instant.now();
        this.matchScore = 0.0; // 失败视为分数为0
        this.status = TestExecutionStatus.FAILED;
        this.errorMessage = errorMessage;

        if (this.startTime != null) {
            this.durationMs = this.endTime.toEpochMilli() - this.startTime.toEpochMilli();
        }
    }

    /**
     * 标记为中断
     */
    public void interrupt() {
        this.endTime = Instant.now();
        this.matchScore = 0.0; // 中断视为分数为0
        this.status = TestExecutionStatus.INTERRUPTED;
        this.errorMessage = "测试执行被中断";

        if (this.startTime != null) {
            this.durationMs = this.endTime.toEpochMilli() - this.startTime.toEpochMilli();
        }
    }

    /**
     * 获取测试单元ID
     * @return 测试单元ID
     */
    public String getTestUnitId() {
        return testUnit != null ? testUnit.getId() : null;
    }

    /**
     * 获取测试模式
     * @return 测试模式
     */
    public TestMode getTestMode() {
        return testUnit != null ? testUnit.getMode() : null;
    }

    /**
     * 获取期望动作ID列表
     * @return 期望动作ID列表
     */
    public List<String> getExpectedActionIds() {
        return testUnit != null ? testUnit.getExpectedActionIds() : null;
    }

    /**
     * 获取实际接收到的动作ID列表
     * @return 实际接收到的动作ID列表
     */
    public List<String> getReceivedActionIds() {
        if (receivedActions == null || receivedActions.isEmpty()) {
            return null;
        }
        return receivedActions.stream()
                .map(TerminalAction::getMetaActionId)
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * 是否执行完成（无论成功失败）
     * @return 是否执行完成
     */
    public boolean isCompleted() {
        return status == TestExecutionStatus.COMPLETED ||
                status == TestExecutionStatus.TIMEOUT ||
                status == TestExecutionStatus.FAILED ||
                status == TestExecutionStatus.INTERRUPTED;
    }

    /**
     * 是否执行成功（基于分数阈值）
     * @return 是否执行成功
     */
    public boolean isSuccessful() {
        return status == TestExecutionStatus.COMPLETED && 
               matchScore != null && matchScore >= ScoreThreshold.ACCEPTABLE.getThreshold();
    }
    
    /**
     * 是否执行成功（基于自定义阈值）
     * @param threshold 自定义阈值
     * @return 是否执行成功
     */
    public boolean isSuccessful(double threshold) {
        return status == TestExecutionStatus.COMPLETED && 
               matchScore != null && matchScore >= threshold;
    }

    /**
     * 是否执行失败
     * @return 是否执行失败
     */
    public boolean isFailed() {
        return !isSuccessful();
    }

    /**
     * 获取分数等级
     * @return 分数等级
     */
    public ScoreThreshold getScoreLevel() {
        if (matchScore == null) {
            return ScoreThreshold.FAILED;
        }
        return ScoreThreshold.fromScore(matchScore);
    }

    /**
     * 是否达到指定分数阈值
     * @param threshold 分数阈值
     * @return 是否达到阈值
     */
    public boolean meetsThreshold(ScoreThreshold threshold) {
        return matchScore != null && matchScore >= threshold.getThreshold();
    }

    /**
     * 生成简化的评估摘要
     * @return 评估摘要
     */
    public String getSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("测试单元: ").append(getTestUnitId()).append("\n");
        summary.append("模式: ").append(getTestMode()).append("\n");
        summary.append("状态: ").append(status).append("\n");
        
        if (matchScore != null) {
            ScoreThreshold level = getScoreLevel();
            summary.append("分数: ").append(String.format("%.2f", matchScore))
                   .append(" (").append(level.getDescription()).append(")").append("\n");
        } else {
            summary.append("分数: 未评估").append("\n");
        }
        
        summary.append("执行时间: ").append(durationMs != null ? durationMs + "ms" : "未知").append("\n");

        if (errorMessage != null) {
            summary.append("错误: ").append(errorMessage).append("\n");
        }

        return summary.toString();
    }
}