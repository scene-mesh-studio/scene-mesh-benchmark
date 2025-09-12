package com.scene.mesh.benchmark.fw.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.scene.mesh.model.action.Action;
import com.scene.mesh.model.event.Event;
import com.scene.mesh.model.product.Product;
import com.scene.mesh.model.scene.Scene;
import com.scene.mesh.model.scene.WhenThen;
import lombok.Data;

import java.util.List;

/**
 * when then
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TestCase {
    /**
     * 测试用例ID
     */
    private String id;

    private Product product;

    private Scene scene;
    /**
     * 测试用例名称
     */
    private String name;

    private WhenThen whenThen;

    private List<TestUnit> testUnits;

    public TestCase() {
    }
}
