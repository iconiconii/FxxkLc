package com.codetop.recommendation.chain;

import com.codetop.recommendation.config.LlmProperties;
import com.codetop.recommendation.provider.LlmProvider;
import com.codetop.recommendation.provider.impl.DefaultProvider;
import com.codetop.recommendation.provider.impl.OpenAiProvider;
import com.codetop.recommendation.service.RequestContext;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class ProviderChainAsyncTest {

    @Test
    void executeAsync_fallsThroughAndDefaults_whenNoSuccess() throws Exception {
        LlmProperties props = new LlmProperties();
        props.setEnabled(true);

        LlmProperties.Chain chain = new LlmProperties.Chain();
        List<LlmProperties.Node> nodes = new ArrayList<>();
        LlmProperties.Node node = new LlmProperties.Node();
        node.setName("openai");
        node.setEnabled(true);
        nodes.add(node);
        chain.setNodes(nodes);
        chain.setDefaultProvider(new LlmProperties.DefaultProvider());
        props.setChain(chain);

        LlmProperties.OpenAi openAi = props.getOpenai();
        openAi.setApiKeyEnv("NON_EXISTENT_KEY");
        LlmProvider openAiProvider = new OpenAiProvider(openAi);
        LlmProvider defaultProvider = new DefaultProvider(chain.getDefaultProvider());

        ProviderChain pc = new ProviderChain(List.of(openAiProvider), props, defaultProvider, null, null);

        ProviderChain.Result res = pc.executeAsync(new RequestContext(), List.of(), new LlmProvider.PromptOptions())
                .get(2, TimeUnit.SECONDS);

        assertNotNull(res);
        assertFalse(res.success);
        assertNotNull(res.hops);
        assertTrue(res.hops.contains("openai"));
        assertTrue(res.hops.contains("default"));
    }
}

