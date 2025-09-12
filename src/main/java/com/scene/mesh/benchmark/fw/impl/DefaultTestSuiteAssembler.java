package com.scene.mesh.benchmark.fw.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scene.mesh.benchmark.fw.model.*;
import com.scene.mesh.benchmark.fw.spec.IConfigLoader;
import com.scene.mesh.benchmark.fw.spec.ITestSuiteAssembler;
import com.scene.mesh.benchmark.fw.spec.ITestSuiteManager;
import com.scene.mesh.model.event.Event;
import com.scene.mesh.model.event.IMetaEvent;
import com.scene.mesh.model.product.Product;
import com.scene.mesh.model.scene.Scene;
import com.scene.mesh.model.scene.WhenThen;
import com.scene.mesh.service.spec.ai.chat.IChatClientFactory;
import com.scene.mesh.service.spec.ai.chat.IPromptService;
import org.apache.flink.cep.dynamic.impl.json.spec.GraphSpec;
import org.apache.flink.cep.dynamic.impl.json.util.CepJsonUtils;
import org.apache.flink.cep.pattern.Pattern;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class DefaultTestSuiteAssembler implements ITestSuiteAssembler {

    @Autowired
    private IConfigLoader configLoader;

    @Autowired
    private ITestSuiteManager testSuiteManager;

    @Autowired
    private IChatClientFactory chatClientFactory;

    @Autowired
    private IPromptService promptService;

    private final Resource cepAgentPromptTemplate = new ClassPathResource("cep_agent_prompt_template.av");
    private final Resource userMessageTemplate = new ClassPathResource("user_message_template.av");

    @Override
    public List<TestSuite> assembler() {
        //发现所有产品
        List<Product> products =  this.configLoader.getProducts();
        if (products.isEmpty()) {
            return null;
        }

        List<TestSuite> testSuites = new ArrayList<>();
        //发现所有场景
        products.forEach(product -> {
            List<Scene> scenes = this.configLoader.getScenesByProduct(product.getId());
            List<TestScenario> testScenarios = new ArrayList<>();
            scenes.forEach(scene -> {
                List<TestCase> tcs = assembleTestCase(product,scene);
                //注册 test case
                this.testSuiteManager.registerTestCases(tcs);
                TestScenario ts = new TestScenario(product, scene);
                ts.setTestCases(tcs);
                testScenarios.add(ts);
            });

            //注册 test scenario
            this.testSuiteManager.registerTestScenarios(testScenarios);
            TestSuite testSuite = new TestSuite(product);
            testSuite.setScenarios(testScenarios);
            testSuites.add(testSuite);
        });

        this.testSuiteManager.registerTestSuite(testSuites);
        return testSuites;
    }

    private List<TestCase> assembleTestCase(Product product, Scene scene) {
        List<WhenThen> whenThenList = scene.getWhenThenList();
        if (whenThenList.isEmpty()) {
            return null;
        }

        List<IMetaEvent> metaEvents = this.configLoader.getMetaEventsByProductId(scene.getProductId());

        List<TestCase> testCases = new ArrayList<>();
        whenThenList.forEach(whenThen -> {
            String when = whenThen.getWhen();
            GraphSpec graphSpec;
            try {
                graphSpec = CepJsonUtils.convertJSONStringToGraphSpec(extractCleanJson(when));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            if (graphSpec == null) {
                throw  new RuntimeException("when is illegal - when str: " + when);
            }
            WhenThen.Then then = whenThen.getThen();
            List<WhenThen.OutputAction> actions = then.getOutputActions();
            if (actions.isEmpty()) {
                return;
            }
            List<String> actionIds = new ArrayList<>();
            actions.forEach(action -> {
               actionIds.add(action.getActionId());
            });

//            // 生成正向匹配事件
//            List<Event> positiveEvents = fetchEventsFromLlm(graphSpec, metaEvents, TestMode.POSITIVE_MATCH.name(),
//                "生成完全符合CEP规则的事件序列，确保能够成功触发规则匹配。事件顺序必须严格按照规则要求，时间间隔必须在规则的时间窗口内，所有条件都必须满足。");
//            TestUnit positiveUnit = new TestUnit(TestMode.POSITIVE_MATCH);
//            positiveUnit.setInputEvents(positiveEvents);
//            positiveUnit.setExpectedActionIds(actionIds);
            
            // 生成负向匹配事件
            List<Event> negativeEvents = fetchEventsFromLlm(graphSpec, metaEvents, TestMode.NEGATIVE_MATCH.name(),
                "生成故意不符合CEP规则的事件序列，确保不会触发规则匹配。可以缺少某些关键事件、改变事件顺序、超出时间窗口或让某些条件不满足。");
            TestUnit negativeUnit = new TestUnit(TestMode.NEGATIVE_MATCH);
            negativeUnit.setScene(scene);
            negativeUnit.setProduct(product);
            negativeUnit.setInputEvents(negativeEvents);
            negativeUnit.setExpectedActionIds(null);
            
            // 生成边界条件事件
            List<Event> boundaryEvents = fetchEventsFromLlm(graphSpec, metaEvents, TestMode.BOUNDARY_CONDITION.name(),
                "生成临界情况的事件序列，测试规则的边界条件。事件数量刚好满足最小要求，时间间隔接近时间窗口边界，条件值接近临界值。");
            TestUnit boundaryUnit = new TestUnit(TestMode.BOUNDARY_CONDITION);
            boundaryUnit.setScene(scene);
            boundaryUnit.setProduct(product);
            boundaryUnit.setInputEvents(boundaryEvents);
            boundaryUnit.setExpectedActionIds(actionIds);


            TestCase tc = new TestCase();
            tc.setId(then.getId());
            tc.setScene(scene);
            tc.setProduct(product);
            tc.setName(scene.getName()+ ":" +then.getName());
            tc.setWhenThen(whenThen);
            tc.setTestUnits(List.of(negativeUnit,boundaryUnit));
            testCases.add(tc);
        });

        return testCases;
    }

    private List<Event> fetchEventsFromLlm(GraphSpec graphSpec, List<IMetaEvent> metaEvents, 
                                         String testMode, String testModeDescription) {

        ChatClient chatClient = chatClientFactory.getChatClient("智谱 AI","glm-4.5");

        BeanOutputConverter<List<Event>> outputConverter = new BeanOutputConverter<>(new ParameterizedTypeReference<>() {});

        Prompt prompt = Prompt.builder()
                .messages(new UserMessage(withUserMessageTemplate(graphSpec, metaEvents, testMode, testModeDescription)))
                .build()
                .augmentSystemMessage(withCepPromptTemplate());

        ChatResponse response = chatClient
                .prompt(prompt)
                .call()
                .chatClientResponse()
                .chatResponse();

        try {
            List<Event> events = outputConverter.convert(response.getResult().getOutput().getText());
            events.forEach(event -> {
                System.out.println("[" + testMode + "] " + event.toString());
            });
            return events != null ? events : new ArrayList<>();
        } catch (Exception e) {
            System.err.println("生成" + testMode + "事件失败: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    private String withCepPromptTemplate() {

        String cepPrompt = this.promptService.assemblePrompt(cepAgentPromptTemplate, Map.of());
        return cepPrompt;
    }

    private String withUserMessageTemplate(GraphSpec graphSpec, List<IMetaEvent> metaEvents, 
                                         String testMode, String testModeDescription) {
        String cepRule;
        try {
            cepRule = CepJsonUtils.convertGraphSpecToJSONString(graphSpec);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        if (cepRule == null) {
            throw  new RuntimeException("cepRule cannot be null.");
        }

        Map<String,Object> variables = Map.of(
                "cepRule", cepRule,
                "metaEvents", metaEvents,
                "testMode", testMode,
                "testModeDescription", testModeDescription
        );

        String userMessage = this.promptService.assemblePrompt(userMessageTemplate, variables);
        return userMessage;
    }

    private String extractCleanJson(String escapedJson) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            String jsonString = mapper.readValue(escapedJson, String.class);
            Object jsonObject = mapper.readValue(jsonString, Object.class);

            // 重新序列化为干净的JSON
            return mapper.writeValueAsString(jsonObject);
        } catch (Exception e) {
            e.printStackTrace();
            return escapedJson;
        }
    }


}
