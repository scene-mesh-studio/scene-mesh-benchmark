package com.scene.mesh.benchmark.n.spec;

import com.scene.mesh.benchmark.n.model.BenchmarkConfig;
import org.springframework.core.io.Resource;

public interface IConfigLoader {
    /**
     * 从资源加载
     * @param resource 指定资源
     * @return TestSuiteConfig
     */
    BenchmarkConfig loadTestSuiteConfig(Resource resource);
}
