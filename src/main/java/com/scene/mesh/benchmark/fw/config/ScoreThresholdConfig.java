package com.scene.mesh.benchmark.fw.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 分数阈值配置
 * 用于配置不同测试模式的分数阈值
 */
@Data
@Component
@ConfigurationProperties(prefix = "benchmark.score-threshold")
public class ScoreThresholdConfig {
    
    /**
     * 默认的成功阈值（可接受的最低分数）
     */
    private double defaultSuccessThreshold = 0.8;
    
    /**
     * 优秀分数阈值
     */
    private double excellentThreshold = 0.95;
    
    /**
     * 良好分数阈值
     */
    private double goodThreshold = 0.85;
    
    /**
     * 可接受分数阈值
     */
    private double acceptableThreshold = 0.75;
    
    /**
     * 较差分数阈值
     */
    private double poorThreshold = 0.60;
    
    /**
     * 按测试模式配置的阈值
     */
    private ModeThresholds modeThresholds = new ModeThresholds();
    
    @Data
    public static class ModeThresholds {
        /**
         * POSITIVE_MATCH 模式的成功阈值
         */
        private double positiveMatch = 0.80;
        
        /**
         * NEGATIVE_MATCH 模式的成功阈值
         */
        private double negativeMatch = 0.90;
        
        /**
         * BOUNDARY_CONDITION 模式的成功阈值
         */
        private double boundaryCondition = 0.75;
    }
    
    /**
     * 获取指定测试模式的成功阈值
     * @param mode 测试模式
     * @return 成功阈值
     */
    public double getSuccessThreshold(String mode) {
        if (modeThresholds == null) {
            return defaultSuccessThreshold;
        }
        
        return switch (mode.toUpperCase()) {
            case "POSITIVE_MATCH" -> modeThresholds.getPositiveMatch();
            case "NEGATIVE_MATCH" -> modeThresholds.getNegativeMatch();
            case "BOUNDARY_CONDITION" -> modeThresholds.getBoundaryCondition();
            default -> defaultSuccessThreshold;
        };
    }
    
    /**
     * 判断分数是否达到成功阈值
     * @param score 分数
     * @param mode 测试模式
     * @return 是否成功
     */
    public boolean isSuccessful(double score, String mode) {
        return score >= getSuccessThreshold(mode);
    }
}
