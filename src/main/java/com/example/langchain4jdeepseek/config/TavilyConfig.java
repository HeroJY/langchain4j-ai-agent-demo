package com.example.langchain4jdeepseek.config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TavilyConfig {
    
    @Value("${tavily.api.key}")
    public String apiKey;
    
    @Value("${tavily.api.url}")
    public String apiUrl;
}