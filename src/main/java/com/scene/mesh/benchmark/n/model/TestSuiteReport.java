package com.scene.mesh.benchmark.n.model;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 测试套件执行报告
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestSuiteReport {

    /**
     * 测试套件基本信息
     */
    private String testSuiteId;
    private String testSuiteName;
    private String productId;

    /**
     * 执行状态
     */
    private ExecutionStatus status;

    /**
     * 执行时间信息
     */
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private long executionTimeMs;

    /**
     * 事件统计信息
     */
    private int totalEventsSent;     // 发送的事件总数
    private int successfulEvents;    // 成功发送的事件数
    private int failedEvents;        // 发送失败的事件数

    /**
     * 动作匹配结果（TestSuite级别）
     */
    private List<String> expectedActions;    // 期望的动作列表
    private List<String> actualActions;      // 实际接收到的动作列表
    private List<String> matchedActions;     // 匹配的动作列表
    private List<String> missedActions;      // 遗漏的动作列表
    private List<String> unexpectedActions;  // 意外的动作列表

    /**
     * 分数信息
     */
    private double matchScore;       // 匹配度分数 0.0-1.0
    private ScoreLevel scoreLevel;   // 分数等级
    private boolean passed;          // 是否通过（基于阈值判断）

    /**
     * 错误信息
     */
    private String errorMessage;
    private List<String> warnings;

    /**
     * 自定义指标
     */
    private Map<String, Object> metrics;

    /**
     * 执行状态枚举
     */
    public enum ExecutionStatus {
        SUCCESS,        // 完全成功
        PARTIAL_SUCCESS, // 部分成功
        FAILED,         // 完全失败
        SKIPPED         // 跳过执行
    }

    /**
     * 分数等级枚举
     */
    public enum ScoreLevel {
        EXCELLENT,      // 优秀 (>= 0.95)
        GOOD,           // 良好 (>= 0.85)
        ACCEPTABLE,     // 可接受 (>= 0.75)
        POOR,           // 较差 (>= 0.60)
        FAILED          // 失败 (< 0.60)
    }
}