package com.example.langchain4jdeepseek.controller;

import com.example.langchain4jdeepseek.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*")
public class ChatController {

    private final ChatService chatService;

    @Autowired
    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping
    public Map<String, String> chat(@RequestBody Map<String, String> request) {
        String userMessage = request.get("message");
        String scenario = request.get("scenario");
        
        String response;
        if (scenario != null && !scenario.isEmpty()) {
            response = chatService.chatWithScenario(userMessage, scenario);
        } else {
            response = chatService.chat(userMessage);
        }
        
        Map<String, String> result = new HashMap<>();
        result.put("response", response);
        return result;
    }
    
    @PostMapping("/with-variables")
    public Map<String, String> chatWithVariables(@RequestBody Map<String, Object> request) {
        String userMessage = (String) request.get("message");
        String scenario = (String) request.get("scenario");
        Map<String, String> variables = (Map<String, String>) request.get("variables");
        
        // 设置动态变量
        if (variables != null && !variables.isEmpty()) {
            chatService.setDynamicVariables(variables);
        }
        
        String response;
        if (scenario != null && !scenario.isEmpty()) {
            response = chatService.chatWithScenario(userMessage, scenario);
        } else {
            response = chatService.chat(userMessage);
        }
        
        Map<String, String> result = new HashMap<>();
        result.put("response", response);
        return result;
    }
    
    @GetMapping("/scenarios")
    public List<String> getAvailableScenarios() {
        return chatService.getAvailableScenarios();
    }
    
    @GetMapping("/current-scenario")
    public Map<String, String> getCurrentScenario() {
        Map<String, String> result = new HashMap<>();
        result.put("scenario", chatService.getCurrentScenario());
        return result;
    }
    
    @PostMapping("/add-scenario")
    public Map<String, String> addScenario(@RequestBody Map<String, String> request) {
        String scenario = request.get("scenario");
        String template = request.get("template");
        
        if (scenario != null && template != null) {
            chatService.addPromptTemplate(scenario, template);
            Map<String, String> result = new HashMap<>();
            result.put("message", "Scenario added successfully");
            return result;
        } else {
            throw new IllegalArgumentException("Both scenario and template are required");
        }
    }
}