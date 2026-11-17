package com.smartdesk.ai_triage_service.service;

import com.smartdesk.ai_triage_service.dto.response.AiTriageResponse;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class GroqResilientGateway {

    private final ChatClient chatClient;

    public GroqResilientGateway(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @CircuitBreaker(name = "groq", fallbackMethod = "chatFallback")
    @Retry(name = "groq")
    @RateLimiter(name = "groq")
    @Bulkhead(name = "groq", type = Bulkhead.Type.SEMAPHORE)
    public AiTriageResponse generateClassification(
            String systemPrompt,
            String userPrompt
    ) {

        return chatClient.prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .call()
                .entity(
                        AiTriageResponse.class,
                        spec -> spec.validateSchema()
                );
    }

    public AiTriageResponse chatFallback(
            String systemPrompt,
            String userPrompt,
            Throwable exception
    ) {
        throw new AiProviderUnavailableException(
                "AI provider is temporarily unavailable. Please try again later.",
                exception
        );
    }
}