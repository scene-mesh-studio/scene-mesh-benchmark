package com.scene.mesh.benchmark.fw.model;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Data
@Slf4j
public class TestSuiteReport {
    
    /**
     * 测试套件ID
     */
    private String testSuiteId;
    
    /**
     * 测试套件名称
     */
    private String testSuiteName;
    
    /**
     * 产品ID
     */
    private String productId;
    
    /**
     * 产品名称
     */
    private String productName;
    
    /**
     * 报告生成时间
     */
    private Instant reportTime;
    
    /**
     * 测试开始时间
     */
    private Instant startTime;
    
    /**
     * 测试结束时间
     */
    private Instant endTime;
    
    /**
     * 总执行时长（毫秒）
     */
    private Long totalDurationMs;
    
    /**
     * 统计信息
     */
    private TestStatistics statistics;
    
    /**
     * 测试统计信息
     */
    @Data
    public static class TestStatistics {
        /**
         * 总测试单元数
         */
        private int totalTestUnits;
        
        /**
         * 通过的测试单元数（基于分数阈值）
         */
        private int passedTestUnits;
        
        /**
         * 失败的测试单元数
         */
        private int failedTestUnits;
        
        /**
         * 通过率
         */
        private double passRate;
        
        /**
         * 失败率
         */
        private double failureRate;
        
        /**
         * 整体质量等级
         */
        private String overallQualityLevel;
        
        /**
         * 平均执行时间（毫秒）
         */
        private double averageExecutionTimeMs;
        
        /**
         * 平均匹配分数
         */
        private double averageMatchScore;
        
        /**
         * 最高匹配分数
         */
        private double maxMatchScore;
        
        /**
         * 最低匹配分数
         */
        private double minMatchScore;
        
        /**
         * 分数分布统计
         */
        private ScoreDistribution scoreDistribution;
        
        /**
         * 按测试模式分组的统计
         */
        private Map<String, ModeStatistics> modeStatistics;
        
        /**
         * 按测试场景分组的统计
         */
        private Map<String, ScenarioStatistics> scenarioStatistics;
    }
    
    /**
     * 测试模式统计
     */
    @Data
    public static class ModeStatistics {
        private int total;
        private int passed;
        private int failed;
        private double passRate;
        private double averageMatchScore;
        private double maxMatchScore;
        private double minMatchScore;
    }
    
    /**
     * 测试场景统计
     */
    @Data
    public static class ScenarioStatistics {
        private String scenarioId;
        private String scenarioName;
        private String sceneId;
        private String sceneName;
        private int totalTestUnits;
        private int passedTestUnits;
        private int failedTestUnits;
        private double passRate;
        private double averageExecutionTimeMs;
        private double averageMatchScore;
    }
    
    /**
     * 分数分布统计
     */
    @Data
    public static class ScoreDistribution {
        /**
         * 满分(1.0)的测试数量
         */
        private int perfectScores;
        
        /**
         * 高分(0.8-0.99)的测试数量
         */
        private int highScores;
        
        /**
         * 中等分数(0.6-0.79)的测试数量
         */
        private int mediumScores;
        
        /**
         * 低分(0.4-0.59)的测试数量
         */
        private int lowScores;
        
        /**
         * 极低分(0.0-0.39)的测试数量
         */
        private int veryLowScores;
        
        /**
         * 无分数(未完成或失败)的测试数量
         */
        private int noScores;
    }
    
    /**
     * 输出完整的测试报告到日志
     */
    public void logReport() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        
        log.info("=".repeat(80));
        log.info("测试套件报告");
        log.info("=".repeat(80));
        
        // 基本信息
        log.info("📋 基本信息:");
        log.info("  测试套件ID: {}", testSuiteId);
        log.info("  测试套件名称: {}", testSuiteName);
        log.info("  产品ID: {}", productId);
        log.info("  产品名称: {}", productName);
        
        // 时间信息
        log.info("⏰ 时间信息:");
        if (startTime != null) {
            log.info("  开始时间: {}", startTime.toString());
        }
        if (endTime != null) {
            log.info("  结束时间: {}", endTime.toString());
        }
        if (totalDurationMs != null) {
            Duration duration = Duration.ofMillis(totalDurationMs);
            log.info("  总执行时长: {} 秒", duration.getSeconds());
        }
        if (reportTime != null) {
            log.info("  报告生成时间: {}", reportTime.toString());
        }
        
        // 统计信息
        if (statistics != null) {
            log.info("📊 统计信息:");
            log.info("  总测试单元数: {}", statistics.getTotalTestUnits());
            log.info("  通过测试单元数: {}", statistics.getPassedTestUnits());
            log.info("  失败测试单元数: {}", statistics.getFailedTestUnits());
            log.info("  通过率: {}%", statistics.getPassRate() * 100);
            log.info("  失败率: {}%", statistics.getFailureRate() * 100);
            log.info("  整体质量等级: {}", statistics.getOverallQualityLevel());
            
            // 执行时间统计
            log.info("⏱️ 执行时间统计:");
            log.info("  平均执行时间: {} 毫秒", statistics.getAverageExecutionTimeMs());
            
            // 分数统计
            log.info("🎯 分数统计:");
            log.info("  平均匹配分数: {}", statistics.getAverageMatchScore());
            log.info("  最高匹配分数: {}", statistics.getMaxMatchScore());
            log.info("  最低匹配分数: {}", statistics.getMinMatchScore());
            
            // 分数分布
            if (statistics.getScoreDistribution() != null) {
                ScoreDistribution dist = statistics.getScoreDistribution();
                log.info("📈 分数分布:");
                log.info("  满分(1.0): {} 个", dist.getPerfectScores());
                log.info("  高分(0.8-0.99): {} 个", dist.getHighScores());
                log.info("  中等分数(0.6-0.79): {} 个", dist.getMediumScores());
                log.info("  低分(0.4-0.59): {} 个", dist.getLowScores());
                log.info("  极低分(0.0-0.39): {} 个", dist.getVeryLowScores());
                log.info("  无分数: {} 个", dist.getNoScores());
            }
            
            // 按测试模式分组的统计
            if (statistics.getModeStatistics() != null && !statistics.getModeStatistics().isEmpty()) {
                log.info("🔧 按测试模式分组统计:");
                statistics.getModeStatistics().forEach((mode, modeStats) -> {
                    log.info("  模式: {}", mode);
                    log.info("    总数: {}, 通过: {}, 失败: {}", 
                             modeStats.getTotal(), modeStats.getPassed(), modeStats.getFailed());
                    log.info("    通过率: {}%", modeStats.getPassRate() * 100);
                    log.info("    平均分数: {}", modeStats.getAverageMatchScore());
                    log.info("    分数范围: {} - {}", 
                             modeStats.getMinMatchScore(), modeStats.getMaxMatchScore());
                });
            }
            
            // 按测试场景分组的统计
            if (statistics.getScenarioStatistics() != null && !statistics.getScenarioStatistics().isEmpty()) {
                log.info("🎬 按测试场景分组统计:");
                statistics.getScenarioStatistics().forEach((scenarioId, scenarioStats) -> {
                    log.info("  场景ID: {}", scenarioId);
                    log.info("    场景名称: {}", scenarioStats.getScenarioName());
                    log.info("    场景ID: {}", scenarioStats.getSceneId());
                    log.info("    场景名称: {}", scenarioStats.getSceneName());
                    log.info("    测试单元数: {}", scenarioStats.getTotalTestUnits());
                    log.info("    通过数: {}, 失败数: {}", 
                             scenarioStats.getPassedTestUnits(), scenarioStats.getFailedTestUnits());
                    log.info("    通过率: {}%", scenarioStats.getPassRate() * 100);
                    log.info("    平均执行时间: {} 毫秒", scenarioStats.getAverageExecutionTimeMs());
                    log.info("    平均匹配分数: {}", scenarioStats.getAverageMatchScore());
                });
            }
        }
        
        log.info("=".repeat(80));
        log.info("报告输出完成");
        log.info("=".repeat(80));
    }
    
    /**
     * 输出简化的测试报告到日志
     */
    public void logSummary() {
        if (statistics != null) {
            log.info("📊 测试套件 [{}] 执行完成: 总数={}, 通过={}, 失败={}, 通过率={}%, 平均分数={}", 
                     testSuiteName, 
                     statistics.getTotalTestUnits(),
                     statistics.getPassedTestUnits(),
                     statistics.getFailedTestUnits(),
                     statistics.getPassRate() * 100,
                     statistics.getAverageMatchScore());
        }
    }
}