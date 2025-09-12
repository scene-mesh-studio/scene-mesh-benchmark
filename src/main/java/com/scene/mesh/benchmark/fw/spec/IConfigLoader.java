package com.scene.mesh.benchmark.fw.spec;

import com.scene.mesh.model.action.IMetaAction;
import com.scene.mesh.model.event.IMetaEvent;
import com.scene.mesh.model.product.Product;
import com.scene.mesh.model.scene.Scene;

import java.util.List;

public interface IConfigLoader {

    List<Product> getProducts();

    Product getProduct(String productId);

    List<Scene> getScenesByProduct(String productId);

    IMetaEvent getMetaEventById(String metaEventId);

    IMetaAction getMetaActionById(String metaActionId);

    List<IMetaEvent> getMetaEventsByProductId(String productId);
}
