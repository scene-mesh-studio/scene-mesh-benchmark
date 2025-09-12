package com.scene.mesh.benchmark.fw.impl;

import com.scene.mesh.benchmark.fw.spec.IConfigLoader;
import com.scene.mesh.model.action.IMetaAction;
import com.scene.mesh.model.event.IMetaEvent;
import com.scene.mesh.model.product.Product;
import com.scene.mesh.model.scene.Scene;
import com.scene.mesh.service.spec.action.IMetaActionService;
import com.scene.mesh.service.spec.event.IMetaEventService;
import com.scene.mesh.service.spec.product.IProductService;
import com.scene.mesh.service.spec.scene.ISceneService;

import java.util.ArrayList;
import java.util.List;

public class DefaultConfigLoader implements IConfigLoader {

    private final IProductService productService;

    private final ISceneService sceneService;

    private final IMetaEventService metaEventService;

    private final IMetaActionService metaActionService;

    public DefaultConfigLoader(IProductService productService,
                               ISceneService sceneService,
                               IMetaEventService metaEventService,
                               IMetaActionService metaActionService) {
        this.productService = productService;
        this.sceneService = sceneService;
        this.metaEventService = metaEventService;
        this.metaActionService = metaActionService;
    }

    @Override
    public List<Product> getProducts() {
        return this.productService.getProducts();
    }

    @Override
    public Product getProduct(String productId) {
        return this.productService.getProduct(productId);
    }

    @Override
    public List<Scene> getScenesByProduct(String productId) {
        List<Scene> scenes = this.sceneService.getAllScenes();
        if (scenes == null || scenes.isEmpty()) {
            return new ArrayList<>();
        }

        List<Scene> result = new ArrayList<>();
        scenes.forEach(scene -> {
            if (scene.getProductId().equals(productId)) {
                result.add(scene);
            }
        });

        return result;
    }

    @Override
    public IMetaEvent getMetaEventById(String metaEventId) {
        return this.metaEventService.getIMetaEvent(metaEventId);
    }

    @Override
    public IMetaAction getMetaActionById(String metaActionId) {
        return this.metaActionService.getIMetaAction(metaActionId);
    }

    @Override
    public List<IMetaEvent> getMetaEventsByProductId(String productId) {
        List<IMetaEvent> metaEvents = this.metaEventService.getAllMetaEvents();
        if (metaEvents == null || metaEvents.isEmpty()) {
            return new ArrayList<>();
        }
        List<IMetaEvent> result = new ArrayList<>();
        metaEvents.forEach(metaEvent -> {
            if (metaEvent.getProductId().equals(productId)) {
                result.add(metaEvent);
            }
        });
        return result;
    }
}
