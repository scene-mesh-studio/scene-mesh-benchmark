package com.scene.mesh.benchmark;

import com.fasterxml.jackson.core.type.TypeReference;
import com.scene.mesh.benchmark.fw.model.TestSuite;
import com.scene.mesh.benchmark.fw.spec.*;
import com.scene.mesh.sdk.util.MessageUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.Charset;
import java.util.List;

/**
 * 基准测试示例应用
 */
@Slf4j
@SpringBootApplication
public class SceneMeshBenchmarkApplication implements CommandLineRunner {

    @Autowired
    private IResultEvaluator resultEvaluator;

    @Autowired
    private ITestExecutor testExecutor;

    @Autowired
    private ITestSuiteAssembler testSuiteAssembler;

    public static void main(String[] args) {
        SpringApplication.run(SceneMeshBenchmarkApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("Starting Scene Mesh Benchmark ...");

        try {
            //2. 将产品组装成为 TestSuite
//            List<TestSuite> suites = this.testSuiteAssembler.assembler();
            String json = new ClassPathResource("test-suites.json").getContentAsString(Charset.defaultCharset());
            List<TestSuite> suites = MessageUtils.fromJson(json,new TypeReference<>() {});
//            System.out.println(SimpleObjectHelper.objectData2json(suites));
            if (suites == null) {
                log.info("没有产品需要测试");
                return;
            }
            //3. 查询 TestSuiteManager 执行 TestSuite、 TestScenario、TestCase
            this.testExecutor.executeAllSuites(suites);
//            //4. 将执行结果交给评估器进行评估
//            TestSuiteReport repost = this.resultEvaluator.evaluate(caseResults);
//            //5. 输出评估报告
//            System.out.println(repost.toString());

        } catch (Exception e) {
            log.error("Error running benchmark example", e);
        }

        log.info("Scene Mesh Benchmark Example completed.");
    }
}