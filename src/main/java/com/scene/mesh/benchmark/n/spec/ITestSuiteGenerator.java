package com.scene.mesh.benchmark.n.spec;

import com.scene.mesh.benchmark.n.model.TestSuite;
import com.scene.mesh.benchmark.n.model.BenchmarkConfig;

public interface ITestSuiteGenerator {

    TestSuite generateTestSuite(BenchmarkConfig benchmarkConfig);
}
