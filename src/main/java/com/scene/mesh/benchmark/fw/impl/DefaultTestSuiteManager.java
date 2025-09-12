package com.scene.mesh.benchmark.fw.impl;

import com.scene.mesh.benchmark.fw.model.TestCase;
import com.scene.mesh.benchmark.fw.model.TestScenario;
import com.scene.mesh.benchmark.fw.model.TestSuite;
import com.scene.mesh.benchmark.fw.spec.ITestSuiteManager;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DefaultTestSuiteManager implements ITestSuiteManager {

    private Map<String, TestSuite> testSuites;
    private Map<String, TestScenario> testScenarios;
    private Map<String, TestCase> testCases;

    public DefaultTestSuiteManager() {
        this.testSuites = new HashMap<>();
        this.testScenarios = new HashMap<>();
        this.testCases = new HashMap<>();
    }

    @Override
    public void registerTestCases(List<TestCase> testCases) {
        if (testCases != null) {
            for (TestCase testCase : testCases) {
                this.testCases.put(testCase.getId(), testCase);
            }
        }
    }

    @Override
    public void registerTestScenarios(List<TestScenario> testScenarios) {
        if (testScenarios != null) {
            for (TestScenario testScenario : testScenarios) {
                this.testScenarios.put(testScenario.getId(), testScenario);
            }
        }
    }

    @Override
    public void registerTestSuite(List<TestSuite> testSuites) {
        if (testSuites != null) {
            for (TestSuite testSuite : testSuites) {
                this.testSuites.put(testSuite.getId(), testSuite);
            }
        }
    }

    @Override
    public List<TestSuite> getTestSuites() {
        return new ArrayList<>(this.testSuites.values());
    }

}
