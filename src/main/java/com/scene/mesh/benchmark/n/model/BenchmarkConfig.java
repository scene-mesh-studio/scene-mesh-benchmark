package com.scene.mesh.benchmark.n.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 基准测试配置类
 * 对应 benchmark-config.json 文件结构
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BenchmarkConfig {

    /**
     * 测试套件ID
     */
    private String id;

    /**
     * 测试套件名称
     */
    private String name;

    /**
     * 产品ID
     */
    @JsonProperty("productId")
    private String productId;

    /**
     * 密钥
     */
    @JsonProperty("secretKey")
    private String secretKey;

    @JsonProperty("protocol")
    private String protocol;

    /**
     * 事件组列表
     */
    @JsonProperty("eventGroups")
    private List<EventGroup> eventGroups;

    /**
     * 等待接受 actions 的时间
     */
    private Long durationOfWaitingActions;

    /**
     * 期望的动作ID列表
     */
    @JsonProperty("expectedActionIds")
    private List<String> expectedActionIds;

    /**
     * 事件组
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class EventGroup {

        /**
         * 事件组ID
         */
        @JsonProperty("groupId")
        private String groupId;

        /**
         * 事件组名称
         */
        private String name;

        /**
         * 测试模式
         */
        private TestMode mode;

        /**
         * 是否自动生成
         */
        @JsonProperty("autoGenerate")
        private boolean autoGenerate;

        /**
         * 生成数量（仅当 autoGenerate=true 时使用）
         */
        private Integer count;

        /**
         * 输入事件列表（仅当 autoGenerate=false 时使用）
         */
        @JsonProperty("inputEvents")
        private List<InputEvent> inputEvents;

        /**
         * 事件模板（仅当 autoGenerate=true 时使用）
         */
        @JsonProperty("eventTemplate")
        private EventTemplate eventTemplate;
    }

    /**
     * 输入事件
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class InputEvent {

        /**
         * 事件类型
         */
        private String type;

        /**
         * payload
         */
        private Map<String, Object> payload;
    }

    /**
     * 事件模板
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class EventTemplate {

        /**
         * 事件类型选择器
         */
        @JsonProperty("typeSelector")
        private List<String> typeSelector;

        /**
         * payload模板
         */
        @JsonProperty("payloadTemplates")
        private Map<String, Map<String, Object>> payloadTemplates;
    }
}