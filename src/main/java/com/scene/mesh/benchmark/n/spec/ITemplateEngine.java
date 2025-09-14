package com.scene.mesh.benchmark.n.spec;

import java.util.Map;

/**
 * 模板引擎接口
 * 支持多种模板引擎实现，如 Aviator、FreeMarker、Velocity 等
 * 开发时可替换，运行时使用单一引擎
 */
public interface ITemplateEngine {

    /**
     * 处理模板字符串
     * @param template 模板字符串
     * @param context 模板上下文变量
     * @return 处理后的字符串
     */
    String processTemplate(String template, Map<String, Object> context);

    /**
     * 获取模板引擎名称
     * @return 引擎名称
     */
    String getEngineName();
}