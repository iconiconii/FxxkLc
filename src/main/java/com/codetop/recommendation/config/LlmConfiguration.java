package com.codetop.recommendation.config;

import com.codetop.recommendation.chain.ProviderChain;
import com.codetop.recommendation.provider.LlmProvider;
import com.codetop.recommendation.provider.impl.DefaultProvider;
import com.codetop.recommendation.provider.impl.MockProvider;
import com.codetop.recommendation.provider.impl.OpenAiProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.ArrayList;
import java.util.List;

@Configuration
@EnableConfigurationProperties(LlmProperties.class)
public class LlmConfiguration {

    @Bean
    public LlmProvider openAiProvider(LlmProperties props) {
        return new OpenAiProvider(props.getOpenai());
    }

    @Bean
    public LlmProvider mockProvider() {
        return new MockProvider();
    }

    @Bean
    public LlmProvider defaultProvider(LlmProperties props) {
        return new DefaultProvider(props.getChain().getDefaultProvider());
    }

    @Bean
    public ProviderChain providerChain(List<LlmProvider> providers, LlmProperties props, LlmProvider defaultProvider,
                                       io.github.resilience4j.ratelimiter.RateLimiterRegistry rateLimiterRegistry,
                                       io.github.resilience4j.retry.RetryRegistry retryRegistry) {
        // Build provider list from catalog order; ProviderChain will select based on props.chain.nodes
        List<LlmProvider> catalog = new ArrayList<>(providers);
        return new ProviderChain(catalog, props, defaultProvider, rateLimiterRegistry, retryRegistry);
    }
}
