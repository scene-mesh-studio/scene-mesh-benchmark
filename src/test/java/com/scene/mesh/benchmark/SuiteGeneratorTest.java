package com.scene.mesh.benchmark;

import com.scene.mesh.benchmark.n.impl.AviatorTemplateEngine;
import com.scene.mesh.benchmark.n.impl.DefaultTemplateProcessor;
import com.scene.mesh.benchmark.n.impl.DefaultTestSuiteGenerator;
import com.scene.mesh.benchmark.n.model.BenchmarkConfig;
import com.scene.mesh.benchmark.n.model.TestMode;
import com.scene.mesh.benchmark.n.model.TestSuite;
import com.scene.mesh.benchmark.n.spec.ITemplateEngine;
import com.scene.mesh.benchmark.n.spec.ITemplateProcessor;
import com.scene.mesh.benchmark.n.spec.ITestSuiteGenerator;
import com.scene.mesh.sdk.model.TerminalEvent;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@ExtendWith(SpringExtension.class)
@SpringBootTest
public class SuiteGeneratorTest {

    private ITestSuiteGenerator testSuiteGenerator;
    private ITemplateProcessor templateProcessor;
    private ITemplateEngine templateEngine;

    @BeforeEach
    void setUp() {
        // 初始化依赖
        templateEngine = new AviatorTemplateEngine();
        templateProcessor = new DefaultTemplateProcessor(templateEngine);
        testSuiteGenerator = new DefaultTestSuiteGenerator(templateProcessor);
    }

    @Test
    void testGenerateTestSuite_WithManualInputEvents() {
        // 准备测试数据 - 手动输入事件模式
        BenchmarkConfig config = createManualInputConfig();

        // 执行测试
        TestSuite result = testSuiteGenerator.generateTestSuite(config);

        // 验证结果
        assertNotNull(result);
        assertEquals("test-suite-001", result.getId());
        assertEquals("智能家居场景测试", result.getName());
        assertEquals("product-001", result.getProductId());
        assertEquals("", result.getSecretKey());

        // 验证终端事件
        List<TerminalEvent> terminalEvents = result.getTerminalEvents();
        assertNotNull(terminalEvents);
        assertEquals(1, terminalEvents.size());

        TerminalEvent event = terminalEvents.get(0);
        assertEquals("chat_text", event.getType());
        assertEquals("你好，请帮我开灯", event.getPayload().get("text"));

        // 验证期望动作ID
        List<String> expectedActionIds = result.getExpectedActionIds();
        assertNotNull(expectedActionIds);
        assertEquals(2, expectedActionIds.size());
        assertTrue(expectedActionIds.contains("action-light-on-001"));
        assertTrue(expectedActionIds.contains("action-response-001"));

        log.info("手动输入事件测试通过，生成的事件: {}", event);
    }

    @Test
    void testGenerateTestSuite_WithAutoGenerateEvents() {
        // 准备测试数据 - 自动生成事件模式
        BenchmarkConfig config = createAutoGenerateConfig();

        // 执行测试
        TestSuite result = testSuiteGenerator.generateTestSuite(config);

        // 验证结果
        assertNotNull(result);
        assertEquals("test-suite-002", result.getId());
        assertEquals("自动生成测试", result.getName());

        // 验证终端事件
        List<TerminalEvent> terminalEvents = result.getTerminalEvents();
        assertNotNull(terminalEvents);
        assertEquals(5, terminalEvents.size()); // count = 5

        // 验证每个事件都有正确的类型和payload
        for (TerminalEvent event : terminalEvents) {
            assertNotNull(event.getType());
            assertNotNull(event.getPayload());
            assertTrue(event.getType().equals("eventA") || event.getType().equals("sensor_event") || event.getType().equals("button_press"));

            // 验证payload内容
            if ("eventA".equals(event.getType())) {
                assertTrue(event.getPayload().containsKey("text"));
                String text = (String) event.getPayload().get("text");
                assertNotNull(text);
                assertTrue(text.length() > 0);
                log.info("生成的 eventA 文本: {}", text);
            } else if ("sensor_event".equals(event.getType())) {
                assertTrue(event.getPayload().containsKey("sensorType"));
                assertTrue(event.getPayload().containsKey("value"));
                assertTrue(event.getPayload().containsKey("unit"));
                log.info("生成的传感器事件: {}", event.getPayload());
            } else if ("button_press".equals(event.getType())) {
                assertTrue(event.getPayload().containsKey("buttonId"));
                assertTrue(event.getPayload().containsKey("duration"));
                log.info("生成的按钮事件: {}", event.getPayload());
            }
        }

        log.info("自动生成事件测试通过，生成了 {} 个事件", terminalEvents.size());
    }

    @Test
    void testGenerateTestSuite_WithMixedEventGroups() {
        // 准备测试数据 - 混合模式（手动 + 自动生成）
        BenchmarkConfig config = createMixedConfig();

        // 执行测试
        TestSuite result = testSuiteGenerator.generateTestSuite(config);

        // 验证结果
        assertNotNull(result);
        List<TerminalEvent> terminalEvents = result.getTerminalEvents();
        assertNotNull(terminalEvents);
        assertEquals(3, terminalEvents.size()); // 1个手动 + 2个自动生成

        // 验证第一个事件是手动输入的
        TerminalEvent manualEvent = terminalEvents.get(0);
        assertEquals("chat_text", manualEvent.getType());
        assertEquals("你好，请帮我开灯", manualEvent.getPayload().get("text"));

        // 验证后面的事件是自动生成的
        for (int i = 1; i < terminalEvents.size(); i++) {
            TerminalEvent autoEvent = terminalEvents.get(i);
            assertNotNull(autoEvent.getType());
            assertNotNull(autoEvent.getPayload());
            log.info("自动生成的事件 {}: {}", i, autoEvent);
        }

        log.info("混合模式测试通过，生成了 {} 个事件", terminalEvents.size());
    }

    @Test
    void testGenerateTestSuite_WithEmptyEventGroups() {
        // 准备测试数据 - 空事件组
        BenchmarkConfig config = createEmptyConfig();

        // 执行测试
        TestSuite result = testSuiteGenerator.generateTestSuite(config);

        // 验证结果
        assertNotNull(result);
        assertEquals("test-suite-empty", result.getId());

        List<TerminalEvent> terminalEvents = result.getTerminalEvents();
        assertNotNull(terminalEvents);
        assertEquals(0, terminalEvents.size());

        log.info("空事件组测试通过");
    }

    @Test
    void testTemplateProcessing() {
        // 测试模板处理能力
        Map<String, Object> template = new HashMap<>();
        template.put("text", "#{randomString(5, 'chinese')}");
        template.put("number", "#{randomRange(1, 100)}");
        template.put("choice", "#{randomFrom(['option1','option2','option3'])}");

        // 执行模板处理
        Map<String, Object> result = templateProcessor.processDataTemplate(template);

        // 验证结果
        assertNotNull(result);
        assertTrue(result.containsKey("text"));
        assertTrue(result.containsKey("number"));
        assertTrue(result.containsKey("choice"));

        String text = (String) result.get("text");
        assertNotNull(text);
        assertTrue(text.length() == 5);
        log.info("生成的文本: {}", text);

        String number = result.get("number").toString();
        assertNotNull(number);
        int num = Integer.parseInt(number);
        assertTrue(num >= 1 && num <= 100);
        log.info("生成的数字: {}", num);

        String choice = (String) result.get("choice");
        assertNotNull(choice);
        assertTrue(Arrays.asList("option1", "option2", "option3").contains(choice));
        log.info("选择的选项: {}", choice);

        log.info("模板处理测试通过");
    }

    // 辅助方法：创建手动输入配置
    private BenchmarkConfig createManualInputConfig() {
        BenchmarkConfig config = new BenchmarkConfig();
        config.setId("test-suite-001");
        config.setName("智能家居场景测试");
        config.setProductId("product-001");
        config.setSecretKey("");

        // 创建事件组
        BenchmarkConfig.EventGroup eventGroup = new BenchmarkConfig.EventGroup();
        eventGroup.setGroupId("group-001");
        eventGroup.setName("正向测试事件组");
        eventGroup.setMode(TestMode.POSITIVE_MATCH);
        eventGroup.setAutoGenerate(false);

        // 创建输入事件
        BenchmarkConfig.InputEvent inputEvent = new BenchmarkConfig.InputEvent();
        inputEvent.setType("chat_text");
        Map<String, Object> payload = new HashMap<>();
        payload.put("text", "你好，请帮我开灯");
        inputEvent.setPayload(payload);

        eventGroup.setInputEvents(Arrays.asList(inputEvent));
        config.setEventGroups(Arrays.asList(eventGroup));

        // 设置期望动作ID
        config.setExpectedActionIds(Arrays.asList("action-light-on-001", "action-response-001"));

        return config;
    }

    // 辅助方法：创建自动生成配置
    private BenchmarkConfig createAutoGenerateConfig() {
        BenchmarkConfig config = new BenchmarkConfig();
        config.setId("test-suite-002");
        config.setName("自动生成测试");
        config.setProductId("product-002");
        config.setSecretKey("test-secret");

        // 创建事件组
        BenchmarkConfig.EventGroup eventGroup = new BenchmarkConfig.EventGroup();
        eventGroup.setGroupId("group-002");
        eventGroup.setName("自动生成事件组");
        eventGroup.setMode(TestMode.NEGATIVE_MATCH);
        eventGroup.setAutoGenerate(true);
        eventGroup.setCount(5);

        // 创建事件模板
        BenchmarkConfig.EventTemplate eventTemplate = new BenchmarkConfig.EventTemplate();
        eventTemplate.setTypeSelector(Arrays.asList("eventA", "sensor_event", "button_press"));

        Map<String, Map<String, Object>> payloadTemplates = new HashMap<>();

        // eventA 模板
        Map<String, Object> eventATemplate = new HashMap<>();
        eventATemplate.put("text", "#{randomString(10, 'chinese')}");
        payloadTemplates.put("eventA", eventATemplate);

        // sensor_event 模板
        Map<String, Object> sensorTemplate = new HashMap<>();
        sensorTemplate.put("sensorType", "#{randomFrom(['temperature','humidity','light'])}");
        sensorTemplate.put("value", "#{randomRange(0,100)}");
        sensorTemplate.put("unit", "#{randomFrom(['celsius','percent','lux'])}");
        payloadTemplates.put("sensor_event", sensorTemplate);

        // button_press 模板
        Map<String, Object> buttonTemplate = new HashMap<>();
        buttonTemplate.put("buttonId", "#{randomFrom(['btn1','btn2','btn3'])}");
        buttonTemplate.put("duration", "#{randomRange(100,2000)}");
        payloadTemplates.put("button_press", buttonTemplate);

        eventTemplate.setPayloadTemplates(payloadTemplates);
        eventGroup.setEventTemplate(eventTemplate);

        config.setEventGroups(Arrays.asList(eventGroup));
        config.setExpectedActionIds(Arrays.asList("action-001"));

        return config;
    }

    // 辅助方法：创建混合配置
    private BenchmarkConfig createMixedConfig() {
        BenchmarkConfig config = new BenchmarkConfig();
        config.setId("test-suite-003");
        config.setName("混合模式测试");
        config.setProductId("product-003");
        config.setSecretKey("");

        List<BenchmarkConfig.EventGroup> eventGroups = new ArrayList<>();

        // 手动输入事件组
        BenchmarkConfig.EventGroup manualGroup = new BenchmarkConfig.EventGroup();
        manualGroup.setGroupId("group-manual");
        manualGroup.setName("手动输入组");
        manualGroup.setMode(TestMode.POSITIVE_MATCH);
        manualGroup.setAutoGenerate(false);

        BenchmarkConfig.InputEvent inputEvent = new BenchmarkConfig.InputEvent();
        inputEvent.setType("chat_text");
        Map<String, Object> payload = new HashMap<>();
        payload.put("text", "你好，请帮我开灯");
        inputEvent.setPayload(payload);
        manualGroup.setInputEvents(Arrays.asList(inputEvent));

        // 自动生成事件组
        BenchmarkConfig.EventGroup autoGroup = new BenchmarkConfig.EventGroup();
        autoGroup.setGroupId("group-auto");
        autoGroup.setName("自动生成组");
        autoGroup.setMode(TestMode.NEGATIVE_MATCH);
        autoGroup.setAutoGenerate(true);
        autoGroup.setCount(2);

        BenchmarkConfig.EventTemplate eventTemplate = new BenchmarkConfig.EventTemplate();
        eventTemplate.setTypeSelector(Arrays.asList("eventA"));

        Map<String, Map<String, Object>> payloadTemplates = new HashMap<>();
        Map<String, Object> eventATemplate = new HashMap<>();
        eventATemplate.put("text", "#{randomString(8, 'english')}");
        payloadTemplates.put("eventA", eventATemplate);
        eventTemplate.setPayloadTemplates(payloadTemplates);
        autoGroup.setEventTemplate(eventTemplate);

        eventGroups.add(manualGroup);
        eventGroups.add(autoGroup);
        config.setEventGroups(eventGroups);
        config.setExpectedActionIds(Arrays.asList("action-001", "action-002"));

        return config;
    }

    // 辅助方法：创建空配置
    private BenchmarkConfig createEmptyConfig() {
        BenchmarkConfig config = new BenchmarkConfig();
        config.setId("test-suite-empty");
        config.setName("空事件组测试");
        config.setProductId("product-empty");
        config.setSecretKey("");
        config.setEventGroups(new ArrayList<>());
        config.setExpectedActionIds(Arrays.asList("action-001"));

        return config;
    }
}