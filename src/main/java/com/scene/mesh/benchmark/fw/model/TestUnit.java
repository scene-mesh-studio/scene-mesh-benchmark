package com.scene.mesh.benchmark.fw.model;

import com.scene.mesh.model.event.Event;
import com.scene.mesh.model.product.Product;
import com.scene.mesh.model.scene.Scene;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class TestUnit {

    private String id;

    private TestMode mode;

    private Product product;

    private Scene scene;

    private List<Event> inputEvents;

    private List<String> expectedActionIds;

    private Long timeoutMs = 50000L;

    public TestUnit(TestMode mode) {
        this.mode = mode;
        this.id = UUID.randomUUID().toString();
    }

    public TestUnit() {
    }
}
