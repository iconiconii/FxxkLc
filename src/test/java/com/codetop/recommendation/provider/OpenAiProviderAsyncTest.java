package com.codetop.recommendation.provider;

import com.codetop.recommendation.config.LlmProperties;
import com.codetop.recommendation.provider.impl.OpenAiProvider;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class OpenAiProviderAsyncTest {

    @Test
    void rankCandidatesAsync_returnsMissingKeyEarly() throws Exception {
        LlmProperties.OpenAi cfg = new LlmProperties.OpenAi();
        // Ensure env var is not set
        cfg.setApiKeyEnv("NON_EXISTENT_" + UUID.randomUUID());

        OpenAiProvider provider = new OpenAiProvider(cfg);
        LlmProvider.PromptOptions opts = new LlmProvider.PromptOptions();
        opts.limit = 5;

        LlmProvider.LlmResult res = provider
                .rankCandidatesAsync(null, Collections.emptyList(), opts)
                .get(2, TimeUnit.SECONDS);

        assertNotNull(res);
        assertFalse(res.success);
        assertEquals("API_KEY_MISSING", res.error);
        assertNotNull(res.items);
        assertTrue(res.items.isEmpty());
    }
}

