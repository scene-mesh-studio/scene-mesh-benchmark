package com.scene.mesh.benchmark;

import com.scene.mesh.benchmark.n.model.BenchmarkConfig;
import com.scene.mesh.benchmark.n.model.TestSuite;
import com.scene.mesh.benchmark.n.model.TestSuiteReport;
import com.scene.mesh.benchmark.n.spec.IConfigLoader;
import com.scene.mesh.benchmark.n.spec.ITestSuiteExecutor;
import com.scene.mesh.benchmark.n.spec.ITestSuiteGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.io.ClassPathResource;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 基准测试示例应用
 */
@Slf4j
@SpringBootApplication
public class SceneMeshBenchmarkApplication implements CommandLineRunner {

    @Autowired
    private IConfigLoader configLoader;

    @Autowired
    private ITestSuiteGenerator testSuiteGenerator;

    @Autowired
    private ITestSuiteExecutor testSuiteExecutor;

    public static void main(String[] args) {
        SpringApplication.run(SceneMeshBenchmarkApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("Starting Scene Mesh Benchmark ...");

        try {
            //1. 加载 test suite config
            BenchmarkConfig testSuiteConfig = configLoader.loadTestSuiteConfig(
                    new ClassPathResource("benchmark-config.json"));

            //2. 生成test suite
            TestSuite testSuite = testSuiteGenerator.generateTestSuite(testSuiteConfig);

            //3. 执行 testSuite
            TestSuiteReport report = testSuiteExecutor.execute(testSuite);

            //4. 输出执行报告
            printReport(report);

        } catch (Exception e) {
            log.error("Error running benchmark example", e);
        }

        log.info("Scene Mesh Benchmark Example completed.");
    }

    /**
     * 打印美观的测试报告
     */
    private void printReport(TestSuiteReport report) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        System.out.println("\n" + "=".repeat(80));
        System.out.println("                    Scene Mesh 测试套件执行报告");
        System.out.println("=".repeat(80));

        // 基本信息
        System.out.println("ℹ️ 基本信息");
        System.out.println("  • 测试套件: " + report.getTestSuiteName());
        System.out.println("  • 套件ID: " + report.getTestSuiteId());
        System.out.println("  • 产品ID: " + report.getProductId());
        System.out.println("  • 执行状态: " + getStatusDisplay(report.getStatus()));
        System.out.println("  • 是否通过: " + (report.isPassed() ? "✅ 是" : "❌ 否"));
        System.out.println();

        // 时间信息
        System.out.println("⏰ 执行时间");
        System.out.println("  • 开始时间: " + report.getStartTime().format(formatter));
        System.out.println("  • 结束时间: " + report.getEndTime().format(formatter));
        System.out.println("  • 执行时长: " + formatDuration(report.getExecutionTimeMs()));
        System.out.println();

        // 事件统计
        System.out.println("📊 事件统计");
        System.out.println("  • 总发送数: " + report.getTotalEventsSent());
        System.out.println("  • 成功发送: " + report.getSuccessfulEvents() + " ✅");
        System.out.println("  • 发送失败: " + report.getFailedEvents() + " ❌");
        if (report.getTotalEventsSent() > 0) {
            double successRate = (double) report.getSuccessfulEvents() / report.getTotalEventsSent() * 100;
            System.out.println("  • 发送成功率: " + String.format("%.1f%%", successRate));
        }
        System.out.println();

        // 动作匹配结果
        System.out.println("🎯 动作匹配结果");
        System.out.println("  • 匹配分数: " + String.format("%.2f", report.getMatchScore()) +
                " (" + getScoreLevelDisplay(report.getScoreLevel()) + ")");

        if (report.getExpectedActions() != null && !report.getExpectedActions().isEmpty()) {
            System.out.println("  • 期望动作: " + report.getExpectedActions().size() + " 个");
            System.out.println("    " + report.getExpectedActions());
        }

        if (report.getActualActions() != null && !report.getActualActions().isEmpty()) {
            System.out.println("  • 实际动作: " + report.getActualActions().size() + " 个");
            System.out.println("    " + report.getActualActions());
        }

        if (report.getMatchedActions() != null && !report.getMatchedActions().isEmpty()) {
            System.out.println("  • 匹配动作: " + report.getMatchedActions().size() + " 个 ✅");
            System.out.println("    " + report.getMatchedActions());
        }

        if (report.getMissedActions() != null && !report.getMissedActions().isEmpty()) {
            System.out.println("  • 遗漏动作: " + report.getMissedActions().size() + " 个 ⚠️");
            System.out.println("    " + report.getMissedActions());
        }

        if (report.getUnexpectedActions() != null && !report.getUnexpectedActions().isEmpty()) {
            System.out.println("  • 意外动作: " + report.getUnexpectedActions().size() + " 个 ⚠️");
            System.out.println("    " + report.getUnexpectedActions());
        }
        System.out.println();

        // 错误信息
        if (report.getErrorMessage() != null && !report.getErrorMessage().isEmpty()) {
            System.out.println("❌ 错误信息");
            System.out.println("  " + report.getErrorMessage());
            System.out.println();
        }

        // 警告信息
        if (report.getWarnings() != null && !report.getWarnings().isEmpty()) {
            System.out.println("⚠️ 警告信息");
            for (String warning : report.getWarnings()) {
                System.out.println("  • " + warning);
            }
            System.out.println();
        }

        // 总结
        System.out.println("📊 总结");
        System.out.println("  • 整体评估: " + getOverallAssessment(report));
        if (!report.isPassed()) {
            System.out.println("  • 建议: " + getRecommendation(report));
        }

        System.out.println("=".repeat(80));
    }

    /**
     * 获取状态显示文本
     */
    private String getStatusDisplay(TestSuiteReport.ExecutionStatus status) {
        return switch (status) {
            case SUCCESS -> "✓ 成功";
            case PARTIAL_SUCCESS -> "⚠ 部分成功";
            case FAILED -> "✗ 失败";
            case SKIPPED -> "⏭ 跳过";
        };
    }

    /**
     * 获取分数等级显示文本
     */
    private String getScoreLevelDisplay(TestSuiteReport.ScoreLevel scoreLevel) {
        return switch (scoreLevel) {
            case EXCELLENT -> "🟢 优秀";
            case GOOD -> "🟡 良好";
            case ACCEPTABLE -> "🟠 可接受";
            case POOR -> "🔴 较差";
            case FAILED -> "⚫ 失败";
        };
    }

    /**
     * 格式化持续时间
     */
    private String formatDuration(long milliseconds) {
        if (milliseconds < 1000) {
            return milliseconds + "ms";
        } else if (milliseconds < 60000) {
            return String.format("%.1fs", milliseconds / 1000.0);
        } else {
            long minutes = milliseconds / 60000;
            long seconds = (milliseconds % 60000) / 1000;
            return String.format("%dm %ds", minutes, seconds);
        }
    }

    /**
     * 获取整体评估
     */
    private String getOverallAssessment(TestSuiteReport report) {
        if (report.isPassed()) {
            return "测试通过，系统表现良好";
        } else if (report.getStatus() == TestSuiteReport.ExecutionStatus.PARTIAL_SUCCESS) {
            return "测试部分通过，需要关注遗漏或意外的动作";
        } else if (report.getStatus() == TestSuiteReport.ExecutionStatus.FAILED) {
            return "测试失败，需要检查系统配置和连接状态";
        } else {
            return "测试跳过，未执行";
        }
    }

    /**
     * 获取建议
     */
    private String getRecommendation(TestSuiteReport report) {
        if (report.isPassed()) {
            return "无";
        } else if (report.getFailedEvents() > 0) {
            return "检查网络连接和事件格式，确保事件能够正确发送";
        } else if (report.getMissedActions() != null && !report.getMissedActions().isEmpty()) {
            return "检查产品配置或调整大模型配置，确保期望的动作能够正确触发";
        } else if (report.getUnexpectedActions() != null && !report.getUnexpectedActions().isEmpty()) {
            return "检查产品配置或调整大模型配置，避免触发不必要的动作";
        } else {
            return "检查系统配置和日志，定位具体问题";
        }
    }
}