package com.example.langchain4jdeepseek.config;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
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
    public ChatModel deepSeekChatModel() {
        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(apiUrl)
                .modelName(modelName)
                .logRequests(true)
                .logResponses(true)
                .maxRetries(3)
                .temperature(0.7)
                .build();
    }
    
    @Bean
    public StreamingChatModel deepSeekStreamingChatModel() {
        return OpenAiStreamingChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(apiUrl)
                .modelName(modelName)
                .logRequests(true)
                .logResponses(true)
                .temperature(0.7)
                .build();
    }
}