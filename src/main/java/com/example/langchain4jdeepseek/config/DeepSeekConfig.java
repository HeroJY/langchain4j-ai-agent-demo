package com.example.langchain4jdeepseek.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DeepSeekConfig {

    @Value("${deepseek.api.key}")
    private String apiKey;

    @Value("${deepseek.api.url}")
    private String apiUrl;

    @Value("${deepseek.api.model.name}")
    private String modelName;

    @Bean
    public ChatLanguageModel deepSeekChatModel() {
        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(apiUrl)
                .modelName(modelName)
                .logRequests(true)
                .logResponses(true)
                .build();
    }
}