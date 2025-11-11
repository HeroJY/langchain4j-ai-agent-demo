package com.example.langchain4jdeepseek.service;

import com.example.langchain4jdeepseek.tools.CommandExecutionTool;
import com.example.langchain4jdeepseek.tools.TavilySearchTool;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.service.AiServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import com.example.langchain4jdeepseek.service.StreamingResponseHandler;

// 定义Assistant接口，用于AiServices构建
interface Assistant {
    AiMessage chat(List<dev.langchain4j.data.message.ChatMessage> messages);
}

// 定义流式Assistant接口
    interface StreamingAssistant {
        TokenStream chat(List<dev.langchain4j.data.message.ChatMessage> messages);
    }



@Service
public class ChatService {

    private static final Logger logger = LoggerFactory.getLogger(ChatService.class);

    private final ChatModel chatLanguageModel;
    
    private final StreamingChatModel streamingChatModel;
    
    private final ChatMemory chatMemory;
    
    private final CommandExecutionTool commandExecutionTool;
    
    private final TavilySearchTool tavilySearchTool;
    
    private final SystemPromptManager systemPromptManager;
    
    // 当前使用的场景
    private String currentScenario = "default";
    
    // 存储流式响应会话
    private final Map<String, StringBuilder> streamingSessions = new ConcurrentHashMap<>();

    @Autowired
    public ChatService(ChatModel chatLanguageModel, 
                      StreamingChatModel streamingChatModel,
                      CommandExecutionTool commandExecutionTool, 
                      TavilySearchTool tavilySearchTool,
                      SystemPromptManager systemPromptManager) {
        this.chatLanguageModel = chatLanguageModel;
        this.streamingChatModel = streamingChatModel;
        this.commandExecutionTool = commandExecutionTool;
        this.tavilySearchTool = tavilySearchTool;
        this.systemPromptManager = systemPromptManager;
        this.chatMemory = MessageWindowChatMemory.withMaxMessages(10);
        
        // 初始化时添加系统提示词
        initializeSystemMessage();
    }
    
    /**
     * 初始化系统消息
     */
    private void initializeSystemMessage() {
        String systemPrompt = systemPromptManager.getSystemPrompt(currentScenario);
        chatMemory.add(new SystemMessage(systemPrompt));
    }

    public String chat(String userMessage) {
        return chatWithScenario(userMessage, null);
    }
    
    /**
     * 使用指定场景进行聊天
     * @param userMessage 用户消息
     * @param scenario 场景标识，如果为null则使用当前场景
     * @return AI响应
     */
    public String chatWithScenario(String userMessage, String scenario) {
        try {
            logger.info("Received chat message: {} with scenario: {}", userMessage, scenario);
            
            // 如果指定了新场景，则更新系统提示词
            if (scenario != null && !scenario.equals(currentScenario)) {
                updateSystemPrompt(scenario);
            }
            
            // 将用户消息添加到聊天历史中
            chatMemory.add(UserMessage.from(userMessage));
            
            // 创建带工具的AI服务
            var assistant = AiServices.builder(Assistant.class)
                    .chatModel(chatLanguageModel)
                    .tools(commandExecutionTool, tavilySearchTool)
                    .build();
            
            // 使用模型生成响应
            AiMessage aiMessage = assistant.chat(chatMemory.messages());
            
            // 将AI消息添加到聊天历史中
            chatMemory.add(aiMessage);
            
            logger.info("Generated AI response: {}", aiMessage.text());
            
            return aiMessage.text();
        } catch (Exception e) {
            logger.error("Error occurred while generating response for message: {}", userMessage, e);
            throw new RuntimeException("Failed to generate response: " + e.getMessage(), e);
        }
    }
    
    /**
     * 流式聊天方法
     * @param userMessage 用户消息
     * @param scenario 场景
     * @param sessionId 会话ID
     * @param responseHandler 响应处理器
     */
    public void streamChat(String userMessage, String scenario, String sessionId, 
                           StreamingResponseHandler<AiMessage> responseHandler) {
        try {
            logger.info("Received streaming chat message: {} with scenario: {} and sessionId: {}", 
                       userMessage, scenario, sessionId);
            
            // 初始化会话存储
            streamingSessions.put(sessionId, new StringBuilder());
            
            // 如果指定了新场景，则更新系统提示词
            if (scenario != null && !scenario.equals(currentScenario)) {
                updateSystemPrompt(scenario);
            }
            
            // 创建新的聊天内存用于流式处理
            ChatMemory streamChatMemory = MessageWindowChatMemory.withMaxMessages(10);
            // 复制当前聊天内存的内容
            streamChatMemory.messages().addAll(chatMemory.messages());
            
            // 添加当前用户消息
            streamChatMemory.add(UserMessage.from(userMessage));
            
            // 创建流式AI服务
            var streamingAssistant = AiServices.builder(StreamingAssistant.class)
                    .streamingChatModel(streamingChatModel)
                    .tools(commandExecutionTool, tavilySearchTool)
                    .build();
            
            // 使用TokenStream进行流式处理
            TokenStream tokenStream = streamingAssistant.chat(streamChatMemory.messages());
            
            tokenStream.onPartialResponse(token -> {
                // 将token添加到会话存储
                streamingSessions.get(sessionId).append(token);
                // 转发给原始响应处理器
                responseHandler.onNext(token);
            })
            .onCompleteResponse(response -> {
                // 将完整响应添加到原始聊天内存
                chatMemory.add(UserMessage.from(userMessage));
                chatMemory.add(response.aiMessage());
                
                // 清理会话存储
                streamingSessions.remove(sessionId);
                
                logger.info("Completed streaming response for session: {}", sessionId);
                responseHandler.onComplete(response);
            })
            .onError(error -> {
                // 清理会话存储
                streamingSessions.remove(sessionId);
                
                logger.error("Error in streaming response for session: {}", sessionId, error);
                responseHandler.onError(error);
            })
            .start();
            
        } catch (Exception e) {
            // 清理会话存储
            streamingSessions.remove(sessionId);
            
            logger.error("Error occurred while setting up streaming response for message: {} and session: {}", 
                        userMessage, sessionId, e);
            responseHandler.onError(e);
        }
    }
    
    /**
     * 获取流式会话的当前内容
     * @param sessionId 会话ID
     * @return 当前会话内容，如果会话不存在则返回null
     */
    public String getStreamingSessionContent(String sessionId) {
        StringBuilder sessionContent = streamingSessions.get(sessionId);
        return sessionContent != null ? sessionContent.toString() : null;
    }
    
    /**
     * 更新系统提示词
     * @param scenario 新的场景
     */
    private void updateSystemPrompt(String scenario) {
        // 移除旧的系统消息（如果存在）
        chatMemory.messages().removeIf(message -> message instanceof SystemMessage);
        
        // 设置新场景
        currentScenario = scenario;
        
        // 添加新的系统消息
        String systemPrompt = systemPromptManager.getSystemPrompt(currentScenario);
        chatMemory.add(new SystemMessage(systemPrompt));
        
        logger.info("Updated system prompt to scenario: {}", scenario);
    }
    
    /**
     * 设置动态变量
     * @param key 变量名
     * @param value 变量值
     */
    public void setDynamicVariable(String key, String value) {
        systemPromptManager.setDynamicVariable(key, value);
        // 更新当前系统提示词以反映变量变化
        updateSystemPrompt(currentScenario);
    }
    
    /**
     * 批量设置动态变量
     * @param variables 变量映射
     */
    public void setDynamicVariables(Map<String, String> variables) {
        systemPromptManager.setDynamicVariables(variables);
        // 更新当前系统提示词以反映变量变化
        updateSystemPrompt(currentScenario);
    }
    
    /**
     * 获取当前场景
     * @return 当前场景标识
     */
    public String getCurrentScenario() {
        return currentScenario;
    }
    
    /**
     * 获取所有可用场景
     * @return 场景列表
     */
    public List<String> getAvailableScenarios() {
        return systemPromptManager.getAvailableScenarios();
    }
    
    /**
     * 添加新的提示词模板
     * @param scenario 场景标识
     * @param template 提示词模板
     */
    public void addPromptTemplate(String scenario, String template) {
        systemPromptManager.addPromptTemplate(scenario, template);
    }
}