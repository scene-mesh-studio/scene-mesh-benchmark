package com.scene.mesh.benchmark.fw.spec;

import com.scene.mesh.benchmark.fw.model.TestSuite;

import java.util.List;

public interface ITestExecutor {

    void executeAllSuites();

    void executeAllSuites(List<TestSuite> testSuites);
}
