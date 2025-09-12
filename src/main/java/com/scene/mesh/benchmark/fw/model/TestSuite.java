package com.scene.mesh.benchmark.fw.model;

import com.scene.mesh.model.product.Product;
import lombok.Data;

import java.util.List;

/**
 * product
 */
@Data
public class TestSuite {
    /**
     * 测试套件ID
     */
    private String id;

    /**
     * 测试套件名称
     */
    private String name;

    /**
     * 测试套件描述
     */
    private String description;

    private Product product;

    /**
     * 测试场景列表
     */
    private List<TestScenario> scenarios;

    public TestSuite(Product product) {
        this.product = product;
        this.id = product.getId();
        this.name = product.getName();
    }

    public TestSuite() {
    }
}
