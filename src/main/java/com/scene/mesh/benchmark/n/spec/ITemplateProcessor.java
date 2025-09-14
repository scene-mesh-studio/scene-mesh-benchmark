package com.scene.mesh.benchmark.n.spec;

import java.util.Map;

/**
 * 模板处理器接口
 * 负责将数据模板转换为实际数据
 */
public interface ITemplateProcessor {

    /**
     * 处理数据模板
     * @param template 数据模板
     * @return 处理后的数据
     */
    Map<String, Object> processDataTemplate(Map<String, Object> template);

    /**
     * 处理单个模板字符串
     * @param template 模板字符串
     * @return 处理后的字符串
     */
    String processStringTemplate(String template);
}