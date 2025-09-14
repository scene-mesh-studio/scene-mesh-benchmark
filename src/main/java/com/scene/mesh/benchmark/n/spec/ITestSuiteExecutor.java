package com.scene.mesh.benchmark.n.spec;

import com.scene.mesh.benchmark.n.model.TestSuite;
import com.scene.mesh.benchmark.n.model.TestSuiteReport;

public interface ITestSuiteExecutor {

    TestSuiteReport execute(TestSuite testSuite);

}
