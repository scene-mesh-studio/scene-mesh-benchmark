package com.scene.mesh.benchmark.fw.config;

import com.scene.mesh.benchmark.fw.impl.*;
import com.scene.mesh.benchmark.fw.spec.*;
import com.scene.mesh.foundation.impl.cache.RedisCache;
import com.scene.mesh.foundation.spec.cache.ICache;
import com.scene.mesh.service.impl.action.DefaultMetaActionService;
import com.scene.mesh.service.impl.ai.chat.AviatorPromptService;
import com.scene.mesh.service.impl.ai.chat.DefaultChatClientFactory;
import com.scene.mesh.service.impl.ai.config.DefaultLLmConfigService;
import com.scene.mesh.service.impl.event.DefaultMetaEventService;
import com.scene.mesh.service.impl.product.DefaultProductService;
import com.scene.mesh.service.impl.scene.DefaultSceneService;
import com.scene.mesh.service.spec.action.IMetaActionService;
import com.scene.mesh.service.spec.ai.chat.IChatClientFactory;
import com.scene.mesh.service.spec.ai.chat.IPromptService;
import com.scene.mesh.service.spec.ai.config.ILLmConfigService;
import com.scene.mesh.service.spec.cache.MutableCacheService;
import com.scene.mesh.service.spec.event.IMetaEventService;
import com.scene.mesh.service.spec.product.IProductService;
import com.scene.mesh.service.spec.scene.ISceneService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TestConfig {

    @Value("${scene-mesh.infrastructure.redis.host}")
    private String redisHost;

    @Value("${scene-mesh.infrastructure.redis.port}")
    private int redisPort;

    @Value("${scene-mesh.facade.url.mqtt}")
    private String mqttUrl;

    @Value("${scene-mesh.facade.url.websocket}")
    private String webSocketUrl;

    @Bean
    public IConfigLoader getConfigLoader(IProductService productService, ISceneService sceneService, IMetaActionService metaActionService, IMetaEventService metaEventService) {
        return new DefaultConfigLoader(productService, sceneService,metaEventService,metaActionService);
    }

    @Bean
    public ITestSuiteManager testSuiteManager(){
        return new DefaultTestSuiteManager();
    }

    @Bean
    public ITestExecutor testExecutor(ITestSuiteManager testSuiteManager,IResultEvaluator resultEvaluator,ScoreThresholdConfig scoreThresholdConfig){
        return new DefaultTestExecutor(testSuiteManager,resultEvaluator,scoreThresholdConfig,mqttUrl,webSocketUrl);
    }

    @Bean
    public ITestSuiteAssembler testSuiteAssembler(){
        return new DefaultTestSuiteAssembler();
    }

    @Bean
    public IResultEvaluator resultEvaluator(ScoreThresholdConfig scoreThresholdConfig){
        return new DefaultResultEvaluator(scoreThresholdConfig);
    }

    @Bean
    public RedisCache iCache() {
        return new RedisCache(redisHost, redisPort);
    }

    @Bean
    public MutableCacheService mutableCacheService(ICache cache) {
        return new MutableCacheService(cache,null);
    }

    @Bean
    public ISceneService sceneService(MutableCacheService mutableCacheService){
        return new DefaultSceneService(mutableCacheService);
    }

    @Bean
    public IProductService productService(MutableCacheService mutableCacheService){
        return new DefaultProductService(mutableCacheService);
    }

    @Bean
    public IMetaEventService metaEventService(MutableCacheService cacheService){
        return new DefaultMetaEventService(cacheService);
    }

    @Bean
    public IMetaActionService metaActionService(MutableCacheService cacheService){
        return new DefaultMetaActionService(cacheService);
    }

    @Bean
    public ILLmConfigService llmConfigService(MutableCacheService cacheService){
        return new DefaultLLmConfigService(cacheService);
    }

    @Bean
    public IChatClientFactory chatClientFactory(ILLmConfigService llmConfigService){
        return new DefaultChatClientFactory(llmConfigService);
    }

    @Bean
    public IPromptService promptService(){
        return new AviatorPromptService();
    }

}
