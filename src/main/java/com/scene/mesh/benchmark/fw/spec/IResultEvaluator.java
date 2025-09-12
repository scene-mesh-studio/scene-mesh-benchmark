package com.scene.mesh.benchmark.fw.spec;

import com.scene.mesh.benchmark.fw.model.TestSuite;
import com.scene.mesh.benchmark.fw.model.TestSuiteReport;
import com.scene.mesh.benchmark.fw.model.TestUnit;
import com.scene.mesh.benchmark.fw.model.TestUnitResult;
import com.scene.mesh.benchmark.fw.sdk.model.TerminalAction;

import java.util.List;

public interface IResultEvaluator {

    /**
     * 评估单个测试单元结果并存储
     * @param testUnit 测试单元
     * @param receivedActions 接收到的动作列表
     * @return 测试单元结果
     */
    TestUnitResult evaluateTestUnit(TestUnit testUnit, List<TerminalAction> receivedActions);

    /**
     * 根据已存储的测试单元结果生成测试套件报告并存储
     * @param testSuite 测试套件
     * @return 测试套件报告
     */
    TestSuiteReport evaluateTestSuite(TestSuite testSuite);

}
