package com.example.langchain4jdeepseek.controller;

import com.example.langchain4jdeepseek.service.ChatService;
import com.example.langchain4jdeepseek.service.StreamingResponseHandler;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

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
    
    /**
     * 流式聊天端点
     * @param request 请求体，包含message和scenario
     * @return SseEmitter 用于服务器发送事件
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChat(@RequestBody Map<String, String> request) {
        String userMessage = request.get("message");
        String scenario = request.get("scenario");
        
        // 生成唯一会话ID
        String sessionId = UUID.randomUUID().toString();
        
        // 设置SSE超时时间为30分钟
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);
        
        // 发送会话ID给客户端
        try {
            emitter.send(SseEmitter.event()
                    .name("session-id")
                    .data(sessionId));
        } catch (IOException e) {
            emitter.completeWithError(e);
            return emitter;
        }
        
        // 异步处理流式响应
        CompletableFuture.runAsync(() -> {
            try {
                chatService.streamChat(userMessage, scenario, sessionId, 
                    new StreamingResponseHandler<AiMessage>() {
                        @Override
                        public void onNext(String token) {
                            try {
                                emitter.send(SseEmitter.event()
                                        .name("token")
                                        .data(token));
                            } catch (IOException e) {
                                emitter.completeWithError(e);
                            }
                        }
                        
                        @Override
                        public void onComplete(ChatResponse response) {
                            try {
                                emitter.send(SseEmitter.event()
                                        .name("complete")
                                        .data("Stream completed"));
                                emitter.complete();
                            } catch (IOException e) {
                                emitter.completeWithError(e);
                            }
                        }
                        
                        @Override
                        public void onError(Throwable error) {
                            try {
                                emitter.send(SseEmitter.event()
                                        .name("error")
                                        .data("Error: " + error.getMessage()));
                            } catch (IOException e) {
                                // 忽略发送错误事件的异常，直接完成
                            } finally {
                                emitter.completeWithError(error);
                            }
                        }
                    });
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        });
        
        // 设置完成和超时处理
        emitter.onCompletion(() -> {
            // 清理资源
        });
        
        emitter.onTimeout(() -> {
            emitter.complete();
        });
        
        return emitter;
    }
    
    /**
     * 获取流式会话的当前内容
     * @param sessionId 会话ID
     * @return 会话内容
     */
    @GetMapping("/stream/{sessionId}")
    public Map<String, String> getStreamingSessionContent(@PathVariable String sessionId) {
        String content = chatService.getStreamingSessionContent(sessionId);
        
        Map<String, String> result = new HashMap<>();
        if (content != null) {
            result.put("content", content);
            result.put("status", "active");
        } else {
            result.put("content", "");
            result.put("status", "not_found");
        }
        
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