package com.scene.mesh.benchmark.fw.model;

import com.scene.mesh.model.product.Product;
import com.scene.mesh.model.scene.Scene;
import lombok.Data;

import java.util.List;

/**
 * scene
 */
@Data
public class TestScenario {

    private String id;

    private String name;

    private String description;

    private Product product;
    /**
     * 测试场景
     */
    private Scene scene;

    /**
     * 测试用例列表 - 该场景下的所有测试用例
     */
    private List<TestCase> testCases;

    public TestScenario(Product product, Scene scene) {
        this.product = product;
        this.scene = scene;
        this.id = scene.getId();
        this.name = product.getName() + ":" + scene.getName();
    }

    public TestScenario() {
    }
}
