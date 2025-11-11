package com.example.langchain4jdeepseek.service;

import com.example.langchain4jdeepseek.tools.CommandExecutionTool;
import com.example.langchain4jdeepseek.tools.TavilySearchTool;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.service.AiServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ChatService {

    private static final Logger logger = LoggerFactory.getLogger(ChatService.class);

    private final ChatLanguageModel chatModel;
    private final StreamingChatLanguageModel streamingChatModel;
    private final TavilySearchTool tavilySearchTool;
    private final CommandExecutionTool commandExecutionTool;
    
    // 存储系统提示词的映射
    private final Map<String, String> promptTemplates = new HashMap<>();
    
    // 存储动态变量的映射
    private final Map<String, String> dynamicVariables = new HashMap<>();
    
    // 存储流式会话内容的映射
    private final Map<String, StringBuilder> streamingSessions = new ConcurrentHashMap<>();
    
    // 当前使用的场景
    private String currentScenario = "default";

    @Value("${system.prompt.file:classpath:system-prompts/default.prompt}")
    private String defaultPromptFile;

    @Autowired
    public ChatService(
            ChatLanguageModel chatModel,
            StreamingChatLanguageModel streamingChatModel,
            TavilySearchTool tavilySearchTool,
            CommandExecutionTool commandExecutionTool) {
        this.chatModel = chatModel;
        this.streamingChatModel = streamingChatModel;
        this.tavilySearchTool = tavilySearchTool;
        this.commandExecutionTool = commandExecutionTool;
        
        // 初始化默认提示词
        loadDefaultPrompt();
        
        // 初始化内置场景提示词
        loadBuiltInScenarios();
        
        // 初始化默认动态变量
        initializeDefaultVariables();
    }
    
    /**
     * 加载默认系统提示词
     */
    private void loadDefaultPrompt() {
        try {
            Resource resource = new ClassPathResource("system-prompts/default.prompt");
            String defaultPrompt = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
            promptTemplates.put("default", defaultPrompt);
            logger.info("Loaded default system prompt");
        } catch (IOException e) {
            logger.error("Failed to load default system prompt", e);
            // 使用硬编码的默认提示词作为后备
            promptTemplates.put("default", "你是一个专业的AI助手，请用简洁明了的语言回答问题。");
        }
    }
    
    /**
     * 加载内置场景提示词
     */
    private void loadBuiltInScenarios() {
        // 定义内置场景列表
        String[] scenarios = {"code_reviewer", "customer_support", "technical_writer", "translator"};
        
        for (String scenario : scenarios) {
            try {
                Resource resource = new ClassPathResource("system-prompts/" + scenario + ".prompt");
                String prompt = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
                promptTemplates.put(scenario, prompt);
                logger.info("Loaded built-in scenario: {}", scenario);
            } catch (IOException e) {
                logger.warn("Failed to load built-in scenario: {}", scenario, e);
                // 使用硬编码的提示词作为后备
                loadFallbackScenario(scenario);
            }
        }
    }
    
    /**
     * 加载后备场景提示词
     */
    private void loadFallbackScenario(String scenario) {
        switch (scenario) {
            case "code_reviewer":
                promptTemplates.put(scenario, "你是一个专业的代码审查员，请对提供的代码进行详细的审查，包括代码质量、性能、安全性和最佳实践。");
                break;
            case "customer_support":
                promptTemplates.put(scenario, "你是一个专业的客户支持代理，请耐心解答用户的问题，提供有用的解决方案，并保持友好和专业的态度。");
                break;
            case "technical_writer":
                promptTemplates.put(scenario, "你是一个专业的技术文档编写员，请用清晰、准确的语言编写技术文档，确保内容易于理解且符合技术写作规范。");
                break;
            case "translator":
                promptTemplates.put(scenario, "你是一个专业的翻译专家，请准确翻译文本内容，保持原文的含义和风格，并确保翻译结果符合目标语言的表达习惯。");
                break;
            default:
                promptTemplates.put(scenario, "你是一个专业的AI助手，请用简洁明了的语言回答问题。");
                break;
        }
    }
    
    /**
     * 初始化默认动态变量
     */
    private void initializeDefaultVariables() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        
        dynamicVariables.put("current_date", now.format(dateFormatter));
        dynamicVariables.put("current_time", now.format(timeFormatter));
        dynamicVariables.put("current_datetime", now.format(dateTimeFormatter));
        
        logger.info("Initialized default dynamic variables");
    }
    
    /**
     * 替换提示词中的变量
     */
    private String replaceVariables(String template) {
        String result = template;
        
        // 替换动态变量
        for (Map.Entry<String, String> entry : dynamicVariables.entrySet()) {
            String placeholder = "${" + entry.getKey() + "}";
            result = result.replace(placeholder, entry.getValue());
        }
        
        return result;
    }
    
    /**
     * 设置动态变量
     */
    public void setDynamicVariables(Map<String, String> variables) {
        if (variables != null) {
            dynamicVariables.putAll(variables);
            logger.info("Updated dynamic variables: {}", variables.keySet());
        }
    }
    
    /**
     * 获取当前系统提示词
     */
    public String getCurrentSystemPrompt() {
        String template = promptTemplates.get(currentScenario);
        if (template == null) {
            template = promptTemplates.get("default");
        }
        return replaceVariables(template);
    }
    
    /**
     * 普通聊天方法
     */
    public String chat(String message) {
        return chatWithScenario(message, "default");
    }
    
    /**
     * 带场景的聊天方法
     */
    public String chatWithScenario(String message, String scenario) {
        // 设置当前场景
        this.currentScenario = scenario;
        
        // 获取场景对应的系统提示词
        String systemPrompt = promptTemplates.get(scenario);
        if (systemPrompt == null) {
            systemPrompt = promptTemplates.get("default");
            logger.warn("Scenario '{}' not found, using default scenario", scenario);
        }
        
        // 替换变量
        systemPrompt = replaceVariables(systemPrompt);
        
        // 创建AI服务
        Assistant assistant = AiServices.create(Assistant.class, chatModel)
                .systemMessageProvider(ctx -> systemPrompt)
                .tools(tavilySearchTool, commandExecutionTool)
                .build();
        
        // 处理用户消息
        UserMessage userMessage = UserMessage.from(message);
        
        try {
            ChatResponse response = assistant.chat(userMessage);
            logger.info("Chat response received for scenario: {}", scenario);
            return response.aiMessage().text();
        } catch (Exception e) {
            logger.error("Error during chat with scenario: {}", scenario, e);
            return "抱歉，处理您的请求时出现错误：" + e.getMessage();
        }
    }
    
    /**
     * 流式聊天方法
     */
    public void streamChat(String message, String scenario, String sessionId, StreamingResponseHandler<AiMessage> handler) {
        // 设置当前场景
        this.currentScenario = scenario;
        
        // 获取场景对应的系统提示词
        String systemPrompt = promptTemplates.get(scenario);
        if (systemPrompt == null) {
            systemPrompt = promptTemplates.get("default");
            logger.warn("Scenario '{}' not found, using default scenario", scenario);
        }
        
        // 替换变量
        systemPrompt = replaceVariables(systemPrompt);
        
        // 创建AI服务
        StreamingAssistant assistant = AiServices.create(StreamingAssistant.class, streamingChatModel)
                .systemMessageProvider(ctx -> systemPrompt)
                .tools(tavilySearchTool, commandExecutionTool)
                .build();
        
        // 初始化会话内容
        streamingSessions.put(sessionId, new StringBuilder());
        
        // 处理用户消息
        UserMessage userMessage = UserMessage.from(message);
        
        try {
            assistant.chat(userMessage)
                    .onNext(token -> {
                        // 将token添加到会话内容
                        streamingSessions.get(sessionId).append(token);
                        // 发送token给处理器
                        handler.onNext(token);
                    })
                    .onComplete(response -> {
                        logger.info("Streaming chat completed for scenario: {}", scenario);
                        handler.onComplete(response);
                    })
                    .onError(error -> {
                        logger.error("Error during streaming chat with scenario: {}", scenario, error);
                        handler.onError(error);
                    })
                    .start();
        } catch (Exception e) {
            logger.error("Error during streaming chat with scenario: {}", scenario, e);
            handler.onError(e);
        }
    }
    
    /**
     * 获取流式会话的当前内容
     */
    public String getStreamingSessionContent(String sessionId) {
        StringBuilder content = streamingSessions.get(sessionId);
        return content != null ? content.toString() : null;
    }
    
    /**
     * 添加提示词模板
     */
    public void addPromptTemplate(String scenario, String template) {
        promptTemplates.put(scenario, template);
        logger.info("Added new prompt template for scenario: {}", scenario);
    }
    
    /**
     * 获取可用场景列表
     */
    public List<String> getAvailableScenarios() {
        return List.copyOf(promptTemplates.keySet());
    }
    
    /**
     * 获取当前场景
     */
    public String getCurrentScenario() {
        return currentScenario;
    }
    
    /**
     * 助手接口
     */
    interface Assistant {
        ChatResponse chat(UserMessage message);
    }
    
    /**
     * 流式助手接口
     */
    interface StreamingAssistant {
        dev.langchain4j.model.chat.ChatResponseStreamer chat(UserMessage message);
    }
}