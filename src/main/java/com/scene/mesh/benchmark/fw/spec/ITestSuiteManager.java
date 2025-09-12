package com.scene.mesh.benchmark.fw.spec;

import com.scene.mesh.benchmark.fw.model.TestCase;
import com.scene.mesh.benchmark.fw.model.TestScenario;
import com.scene.mesh.benchmark.fw.model.TestSuite;

import java.util.List;

public interface ITestSuiteManager {
    void registerTestCases(List<TestCase> testCases);

    void registerTestScenarios(List<TestScenario> testScenarios);

    void registerTestSuite(List<TestSuite> testSuites);

    List<TestSuite> getTestSuites();
}
