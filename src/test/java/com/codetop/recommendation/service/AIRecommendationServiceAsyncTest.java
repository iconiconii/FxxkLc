package com.codetop.recommendation.service;

import com.codetop.recommendation.chain.ProviderChain;
import com.codetop.recommendation.config.LlmProperties;
import com.codetop.recommendation.dto.AIRecommendationResponse;
import com.codetop.recommendation.provider.LlmProvider;
import com.codetop.recommendation.provider.impl.DefaultProvider;
import com.codetop.recommendation.provider.impl.OpenAiProvider;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class AIRecommendationServiceAsyncTest {

    @Test
    void getRecommendationsAsync_returnsFsrsFallback_whenLlmUnavailable() throws Exception {
        LlmProperties props = new LlmProperties();
        props.setEnabled(true);

        LlmProperties.OpenAi openAi = props.getOpenai();
        openAi.setApiKeyEnv("NON_EXISTENT_TEST_KEY_ASYNC");

        LlmProperties.Chain chain = new LlmProperties.Chain();
        List<LlmProperties.Node> nodes = new ArrayList<>();
        LlmProperties.Node node = new LlmProperties.Node();
        node.setName("openai");
        node.setEnabled(true);
        nodes.add(node);
        chain.setNodes(nodes);
        chain.setDefaultProvider(new LlmProperties.DefaultProvider());
        props.setChain(chain);

        LlmProvider openAiProvider = new OpenAiProvider(openAi);
        LlmProvider defaultProvider = new DefaultProvider(chain.getDefaultProvider());
        ProviderChain pc = new ProviderChain(List.of(openAiProvider), props, defaultProvider, null, null);

        AIRecommendationService service = new AIRecommendationService(pc);
        AIRecommendationResponse resp = service.getRecommendationsAsync(999L, 5).get(3, TimeUnit.SECONDS);

        assertNotNull(resp);
        assertNotNull(resp.getItems());
        assertEquals(5, resp.getItems().size());
        assertEquals("FSRS", resp.getItems().get(0).getSource());
        assertEquals("fsrs", resp.getItems().get(0).getStrategy());
    }
}

